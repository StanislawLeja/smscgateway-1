/*
 * TeleStax, Open Source Cloud Communications  Copyright 2012. 
 * and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.mobicents.smsc.slee.services.mt;

import java.util.Collection;
import java.util.Iterator;

import javax.slee.ChildRelation;
import javax.slee.CreateException;
import javax.slee.NoSuchObjectLocalException;
import javax.slee.SLEEException;
import javax.slee.SbbLocalObject;
import javax.slee.TransactionRequiredLocalException;
import javax.slee.TransactionRolledbackLocalException;

import org.mobicents.protocols.ss7.map.MAPParameterFactoryImpl;
import org.mobicents.protocols.ss7.map.MAPSmsTpduParameterFactoryImpl;
import org.mobicents.protocols.ss7.map.api.MAPApplicationContextVersion;
import org.mobicents.protocols.ss7.map.api.service.sms.SM_RP_DA;
import org.mobicents.protocols.ss7.map.api.service.sms.SM_RP_OA;
import org.mobicents.protocols.ss7.map.api.service.sms.SmsSignalInfo;
import org.mobicents.protocols.ss7.sccp.parameter.SccpAddress;
import org.mobicents.slee.ChildRelationExt;
import org.mobicents.slee.SbbLocalObjectExt;
import org.mobicents.smsc.slee.resources.persistence.MAPProviderProxy;
import org.mobicents.smsc.slee.resources.persistence.PersistenceRAInterfaceProxy;
import org.mobicents.smsc.slee.resources.persistence.SmsSubmitData;
import org.mobicents.smsc.slee.resources.persistence.TraceProxy;

/**
 * 
 * @author sergey vetyutnev
 * 
 */
public class MtSbbProxy extends MtSbb implements ChildRelation, MtSbbLocalObject, SbbLocalObjectExt {

	private PersistenceRAInterfaceProxy cassandraSbb;
	private SriSbbProxy sriSbb;

	public MtSbbProxy(PersistenceRAInterfaceProxy pers) {
		this.cassandraSbb = pers;
		this.logger = new TraceProxy();

		this.mapProvider = new MAPProviderProxy();
		this.mapParameterFactory = new MAPParameterFactoryImpl();
		this.maxMAPApplicationContextVersion = MAPApplicationContextVersion.getInstance(smscPropertiesManagement.getMaxMapVersion());
		this.mapSmsTpduParameterFactory = new MAPSmsTpduParameterFactoryImpl();
		this.mapAcif = new MAPContextInterfaceFactoryProxy();
		this.sbbContext = new SbbContextExtProxy(this);
	}

	public void setSriSbbProxy(SriSbbProxy sriSbb) {
		this.sriSbb = sriSbb;
	}

	@Override
	public PersistenceRAInterfaceProxy getStore() {
		return cassandraSbb;
	}


	private SmsSubmitData smsDeliveryData;
	private SccpAddress sccpAddress;
	private int mesageSegmentNumber;
	private SM_RP_DA sm_rp_da;
	private SM_RP_OA sm_rp_oa;
	private int currentMsgNum;
	private InformServiceCenterContainer informServiceCenterContainer;
	private int tcEmptySent;
	private SmsSignalInfo[] segments;

	
	@Override
	public void setSmsSubmitData(SmsSubmitData smsDeliveryData) {
		this.smsDeliveryData = smsDeliveryData;
	}

	@Override
	public SmsSubmitData getSmsSubmitData() {
		return this.smsDeliveryData;
	}

	@Override
	public void setNetworkNode(SccpAddress sccpAddress) {
		this.sccpAddress = sccpAddress;
	}

	@Override
	public SccpAddress getNetworkNode() {
		return this.sccpAddress;
	}

	@Override
	public void setMessageSegmentNumber(int mesageSegmentNumber) {
		this.mesageSegmentNumber = mesageSegmentNumber;
	}

	@Override
	public int getMessageSegmentNumber() {
		return this.mesageSegmentNumber;
	}

	@Override
	public void setSmRpDa(SM_RP_DA sm_rp_da) {
		this.sm_rp_da = sm_rp_da;
	}

	@Override
	public SM_RP_DA getSmRpDa() {
		return this.sm_rp_da;
	}
	
	@Override
	public void setSmRpOa(SM_RP_OA sm_rp_oa) {
		this.sm_rp_oa = sm_rp_oa;
	}

	@Override
	public SM_RP_OA getSmRpOa() {
		return this.sm_rp_oa;
	}
	
	@Override
	public int getCurrentMsgNum() {
		return currentMsgNum;
	}

	@Override
	public void setCurrentMsgNum(int currentMsgNum) {
		this.currentMsgNum = currentMsgNum;
	}

	@Override
	public void setInformServiceCenterContainer(InformServiceCenterContainer informServiceCenterContainer) {
		this.informServiceCenterContainer = informServiceCenterContainer;
	}

	@Override
	public InformServiceCenterContainer getInformServiceCenterContainer() {
		return this.informServiceCenterContainer;
	}

	@Override
	public void setTcEmptySent(int tcEmptySent) {
		this.tcEmptySent = tcEmptySent;
	}

	@Override
	public int getTcEmptySent() {
		return this.tcEmptySent;
	}

	@Override
	public void setSegments(SmsSignalInfo[] segments) {
		this.segments = segments;
	}

	@Override
	public SmsSignalInfo[] getSegments() {
		return this.segments;
	}

	@Override
	public boolean add(Object arg0) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean addAll(Collection arg0) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void clear() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean contains(Object arg0) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean containsAll(Collection arg0) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isEmpty() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Iterator iterator() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean remove(Object arg0) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean removeAll(Collection arg0) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean retainAll(Collection arg0) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public int size() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public Object[] toArray() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object[] toArray(Object[] arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SbbLocalObject create() throws CreateException, TransactionRequiredLocalException, SLEEException {
		return this;
	}

	@Override
	public byte getSbbPriority() throws TransactionRequiredLocalException, NoSuchObjectLocalException, SLEEException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean isIdentical(SbbLocalObject arg0) throws TransactionRequiredLocalException, SLEEException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void remove() throws TransactionRequiredLocalException, TransactionRolledbackLocalException, SLEEException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setSbbPriority(byte arg0) throws TransactionRequiredLocalException, NoSuchObjectLocalException, SLEEException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String getChildRelation() throws TransactionRequiredLocalException, SLEEException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getName() throws NoSuchObjectLocalException, TransactionRequiredLocalException, SLEEException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SbbLocalObjectExt getParent() throws NoSuchObjectLocalException, TransactionRequiredLocalException, SLEEException {
		return sriSbb;
	}

}
