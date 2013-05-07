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
import javax.slee.SLEEException;
import javax.slee.SbbLocalObject;
import javax.slee.TransactionRequiredLocalException;

import org.mobicents.protocols.ss7.map.MAPParameterFactoryImpl;
import org.mobicents.protocols.ss7.map.api.MAPApplicationContextVersion;
import org.mobicents.protocols.ss7.map.api.MAPParameterFactory;
import org.mobicents.protocols.ss7.map.api.MAPProvider;
import org.mobicents.protocols.ss7.map.api.errors.MAPErrorMessage;
import org.mobicents.protocols.ss7.map.service.sms.SendRoutingInfoForSMResponseImpl;
import org.mobicents.slee.ChildRelationExt;
import org.mobicents.slee.SbbContextExt;
import org.mobicents.smsc.slee.services.persistence.CassandraPersistenceSbbProxy;
import org.mobicents.smsc.slee.services.persistence.MAPProviderProxy;
import org.mobicents.smsc.slee.services.persistence.Persistence;
import org.mobicents.smsc.slee.services.persistence.TraceProxy;

/**
 * 
 * @author sergey vetyutnev
 * 
 */
public class SriSbbProxy extends SriSbb implements ChildRelation {

	private CassandraPersistenceSbbProxy cassandraSbb;
	private MtSbbProxy mtSbb;

	public SriSbbProxy(CassandraPersistenceSbbProxy pers, MtSbbProxy mtSbb) {
		this.cassandraSbb = pers;
		this.mtSbb = mtSbb;
		this.logger = new TraceProxy();

		this.mapProvider = new MAPProviderProxy();
		this.mapParameterFactory = new MAPParameterFactoryImpl();
		this.maxMAPApplicationContextVersion = MAPApplicationContextVersion.getInstance(smscPropertiesManagement.getMaxMapVersion());
		this.mapAcif = new MAPContextInterfaceFactoryProxy();
		this.sbbContext = new SbbContextExtProxy();
	}

	@Override
	public Persistence getStore() {
		return cassandraSbb;
	}

	@Override
	public ChildRelation getMtSbb() {
		return this.mtSbb;
	}

	@Override
	public ChildRelationExt getStoreSbb() {
		// TODO Auto-generated method stub
		return null;
	}

	
	private SendRoutingInfoForSMResponseImpl sendRoutingInfoForSMResponse;
	private int sriMapVersion;
	private MAPErrorMessage errorContainer;
	
	@Override
	public void setSendRoutingInfoForSMResponse(SendRoutingInfoForSMResponseImpl sendRoutingInfoForSMResponse) {
		this.sendRoutingInfoForSMResponse = sendRoutingInfoForSMResponse;
	}

	@Override
	public SendRoutingInfoForSMResponseImpl getSendRoutingInfoForSMResponse() {
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
	public void setErrorContainer(MAPErrorMessage errorContainer) {
		this.errorContainer = errorContainer;
	}

	@Override
	public MAPErrorMessage getErrorContainer() {
		return this.errorContainer;
	}


	@Override
	public ChildRelation getRsdsSbb() {
		// TODO Auto-generated method stub
		return null;
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

}
