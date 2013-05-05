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

import org.mobicents.protocols.ss7.map.api.service.sms.SM_RP_DA;
import org.mobicents.protocols.ss7.map.api.service.sms.SM_RP_OA;
import org.mobicents.protocols.ss7.map.api.service.sms.SmsSignalInfo;
import org.mobicents.protocols.ss7.map.api.smstpdu.AbsoluteTimeStamp;
import org.mobicents.protocols.ss7.map.api.smstpdu.DataCodingScheme;
import org.mobicents.protocols.ss7.sccp.parameter.SccpAddress;
import org.mobicents.slee.ChildRelationExt;
import org.mobicents.smsc.slee.services.persistence.CassandraPersistenceSbbProxy;

/**
 * 
 * @author sergey vetyutnev
 * 
 */
public class MtSbbProxy extends MtSbb {

	public MtSbbProxy(CassandraPersistenceSbbProxy pers) {
		this.persistence = pers;
	}

	@Override
	public void setSmsDeliveryData(SmsDeliveryData smsDeliveryData) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public SmsDeliveryData getSmsDeliveryData() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setNetworkNode(SccpAddress sccpAddress) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public SccpAddress getNetworkNode() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setServiceCentreTimeStamp(AbsoluteTimeStamp serviceCentreTimeStamp) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public AbsoluteTimeStamp getServiceCentreTimeStamp() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setMessageSegmentNumber(int mesageSegmentNumber) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public int getMessageSegmentNumber() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void setSmRpDa(SM_RP_DA sm_rp_da) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public SM_RP_DA getSmRpDa() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setSmRpOa(SM_RP_OA sm_rp_oa) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public SM_RP_OA getSmRpOa() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setDataCodingScheme(DataCodingScheme dataCodingScheme) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public DataCodingScheme getDataCodingScheme() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ChildRelationExt getStoreSbb() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setCurrentMsgNum(int currentMsgNum) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setInformServiceCenterContainer(InformServiceCenterContainer informServiceCenterContainer) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public InformServiceCenterContainer getInformServiceCenterContainer() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setTcEmptySent(int tcEmptySent) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public int getTcEmptySent() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void setSegments(SmsSignalInfo[] segments) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public SmsSignalInfo[] getSegments() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public InformServiceCenterContainer doGetInformServiceCenterContainer() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getCurrentMsgNum() {
		// TODO Auto-generated method stub
		return 0;
	}

}
