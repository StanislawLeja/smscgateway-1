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

import javax.slee.ChildRelation;
import org.mobicents.protocols.ss7.map.service.sms.SendRoutingInfoForSMResponseImpl;
import org.mobicents.slee.ChildRelationExt;
import org.mobicents.smsc.slee.services.persistence.CassandraPersistenceSbbProxy;

/**
 * 
 * @author sergey vetyutnev
 * 
 */
public class SriSbbProxy extends SriSbb {

	public SriSbbProxy(CassandraPersistenceSbbProxy pers) {
		this.persistence = pers;
	}

	@Override
	public ChildRelation getMtSbb() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ChildRelationExt getStoreSbb() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setSendRoutingInfoForSMResponse(SendRoutingInfoForSMResponseImpl sendRoutingInfoForSMResponse) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public SendRoutingInfoForSMResponseImpl getSendRoutingInfoForSMResponse() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setErrorContainer(ErrorContainer errorContainer) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public ErrorContainer getErrorContainer() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public InformServiceCenterContainer doGetInformServiceCenterContainer() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setSriMapVersion(int sriMapVersion) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public int getSriMapVersion() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public ChildRelation getRsdsSbb() {
		// TODO Auto-generated method stub
		return null;
	}

}
