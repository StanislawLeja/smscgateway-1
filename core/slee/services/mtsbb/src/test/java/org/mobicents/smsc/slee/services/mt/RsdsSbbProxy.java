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

import org.mobicents.protocols.ss7.map.api.service.sms.SMDeliveryOutcome;
import org.mobicents.slee.ChildRelationExt;
import org.mobicents.slee.SbbLocalObjectExt;
import org.mobicents.smsc.slee.services.persistence.CassandraPersistenceSbbProxy;
import org.mobicents.smsc.slee.services.persistence.MAPProviderProxy;
import org.mobicents.smsc.slee.services.persistence.Persistence;
import org.mobicents.smsc.slee.services.persistence.TraceProxy;

/**
 * 
 * @author sergey vetyutnev
 * 
 */
public class RsdsSbbProxy extends RsdsSbb implements ChildRelation, SbbLocalObject, RsdsSbbLocalObject, SbbLocalObjectExt {

	private CassandraPersistenceSbbProxy pers;
	private String targetId;
	private SMDeliveryOutcome smDeliveryOutcome;

	public RsdsSbbProxy(CassandraPersistenceSbbProxy pers) {
		this.pers = pers;
		this.logger = new TraceProxy();

		this.mapProvider = new MAPProviderProxy();
		this.mapAcif = new MAPContextInterfaceFactoryProxy();
		this.sbbContext = new SbbContextExtProxy(this);
	}

	@Override
	public Persistence getStore() {
		return pers;
	}

	@Override
	public void setupReportSMDeliveryStatusRequest(String destinationAddress, int ton, int npi, SMDeliveryOutcome sMDeliveryOutcome, String targetId) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setTargetId(String targetId) {
		this.targetId = targetId;
	}

	@Override
	public String getTargetId() {
		return this.targetId;
	}

	@Override
	public ChildRelationExt getStoreSbb() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void doSetSmsDeliveryData(SmsDeliveryData smsDeliveryData) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public SmsDeliveryData doGetSmsDeliveryData() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void doSetCurrentMsgNum(int currentMsgNum) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public int doGetCurrentMsgNum() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void doSetInformServiceCenterContainer(InformServiceCenterContainer informServiceCenterContainer) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public InformServiceCenterContainer doGetInformServiceCenterContainer() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean add(Object e) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean addAll(Collection c) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void clear() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean contains(Object o) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean containsAll(Collection c) {
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
	public boolean remove(Object o) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean removeAll(Collection c) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean retainAll(Collection c) {
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
	public Object[] toArray(Object[] a) {
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
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setSmDeliveryOutcome(SMDeliveryOutcome smDeliveryOutcome) {
		this.smDeliveryOutcome = smDeliveryOutcome;
	}

	@Override
	public SMDeliveryOutcome getSmDeliveryOutcome() {
		return this.smDeliveryOutcome;
	}

}
