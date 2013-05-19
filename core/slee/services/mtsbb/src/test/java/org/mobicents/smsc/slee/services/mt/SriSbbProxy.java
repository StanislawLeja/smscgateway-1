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
import org.mobicents.protocols.ss7.map.api.MAPApplicationContextVersion;
import org.mobicents.protocols.ss7.map.api.errors.MAPErrorMessage;
import org.mobicents.protocols.ss7.map.api.service.sms.SendRoutingInfoForSMResponse;
import org.mobicents.protocols.ss7.map.service.sms.SendRoutingInfoForSMResponseImpl;
import org.mobicents.slee.ChildRelationExt;
import org.mobicents.slee.SbbLocalObjectExt;
import org.mobicents.smsc.slee.resources.persistence.MAPProviderProxy;
import org.mobicents.smsc.slee.resources.persistence.PersistenceRAInterfaceProxy;
import org.mobicents.smsc.slee.resources.persistence.TraceProxy;

/**
 * 
 * @author sergey vetyutnev
 * 
 */
public class SriSbbProxy extends SriSbb implements ChildRelation, SbbLocalObjectExt, SriSbbLocalObject {

	private PersistenceRAInterfaceProxy cassandraSbb;
	private MtSbbProxy mtSbb;
	private RsdsSbbProxy rsdsSbb;

	public SriSbbProxy(PersistenceRAInterfaceProxy pers, MtSbbProxy mtSbb, RsdsSbbProxy rsdsSbb) {
		this.cassandraSbb = pers;
		this.mtSbb = mtSbb;
		this.rsdsSbb = rsdsSbb;
		this.logger = new TraceProxy();

		this.mapProvider = new MAPProviderProxy();
		this.mapParameterFactory = new MAPParameterFactoryImpl();
		this.maxMAPApplicationContextVersion = MAPApplicationContextVersion.getInstance(smscPropertiesManagement.getMaxMapVersion());
		this.mapAcif = new MAPContextInterfaceFactoryProxy();
		this.sbbContext = new SbbContextExtProxy(this);
	}

	@Override
	public PersistenceRAInterfaceProxy getStore() {
		return cassandraSbb;
	}

	@Override
	public ChildRelationExt getMtSbb() {
		return this.mtSbb;
	}

	@Override
	public ChildRelationExt getRsdsSbb() {
		return rsdsSbb;
	}

	
	private SendRoutingInfoForSMResponse sendRoutingInfoForSMResponse;
	private int sriMapVersion;
	private MAPErrorMessage errorContainer;
	
	@Override
	public void setSendRoutingInfoForSMResponse(SendRoutingInfoForSMResponse sendRoutingInfoForSMResponse) {
		this.sendRoutingInfoForSMResponse = sendRoutingInfoForSMResponse;
	}

	@Override
	public SendRoutingInfoForSMResponse getSendRoutingInfoForSMResponse() {
		return this.sendRoutingInfoForSMResponse;
	}

	@Override
	public void setSriMapVersion(int sriMapVersion) {
		this.sriMapVersion = sriMapVersion;
	}

	@Override
	public int getSriMapVersion() {
		return this.sriMapVersion;
	}

	@Override
	public void setErrorResponse(MAPErrorMessage errorContainer) {
		this.errorContainer = errorContainer;
	}

	@Override
	public MAPErrorMessage getErrorResponse() {
		return this.errorContainer;
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
		// TODO Auto-generated method stub
		return null;
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

}
