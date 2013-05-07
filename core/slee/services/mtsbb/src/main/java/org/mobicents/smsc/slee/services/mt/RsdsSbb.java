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

import javax.slee.ActivityContextInterface;
import javax.slee.CreateException;
import javax.slee.SLEEException;
import javax.slee.TransactionRequiredLocalException;

import org.mobicents.protocols.ss7.map.api.MAPApplicationContext;
import org.mobicents.protocols.ss7.map.api.MAPException;
import org.mobicents.protocols.ss7.map.api.primitives.AddressString;
import org.mobicents.protocols.ss7.map.api.primitives.ISDNAddressString;
import org.mobicents.protocols.ss7.map.api.service.sms.MAPDialogSms;
import org.mobicents.protocols.ss7.map.api.service.sms.SMDeliveryOutcome;
import org.mobicents.protocols.ss7.map.api.service.sms.SendRoutingInfoForSMResponse;
import org.mobicents.protocols.ss7.sccp.parameter.SccpAddress;
import org.mobicents.smsc.slee.services.persistence.Persistence;
import org.mobicents.smsc.slee.services.persistence.PersistenceException;

/**
 * 
 * @author sergey vetyutnev
 * 
 */
public abstract class RsdsSbb extends MtCommonSbb implements ReportSMDeliveryStatusInterface {

	private static final String className = "RsdsSbb";

	public RsdsSbb() {
		super(className);
	}

	/**
	 * Components Events override from MtCommonSbb that we care
	 */

	/**
	 * Dialog Events override from MtCommonSbb that we care
	 */

	/**
	 * SMS Event Handlers
	 */

	public void onSendRoutingInfoForSMResponse(SendRoutingInfoForSMResponse evt, ActivityContextInterface aci) {
		if (this.logger.isInfoEnabled()) {
			this.logger.info("Received SEND_ROUTING_INFO_FOR_SM_RESPONSE = " + evt);
		}

		Persistence pers;
		try {
			pers = this.getStore();
			pers.setAlertingSupported(this.getTargetId(), true);
		} catch (TransactionRequiredLocalException e1) {
			this.logger.severe("TransactionRequiredLocalException when getting Persistence object in onSendRoutingInfoForSMResponse(): " + e1.getMessage(), e1);
		} catch (SLEEException e1) {
			this.logger.severe("SLEEException when getting Persistence object in onSendRoutingInfoForSMResponse(): " + e1.getMessage(), e1);
		} catch (CreateException e1) {
			this.logger.severe("CreateException when getting Persistence object in onSendRoutingInfoForSMResponse(): " + e1.getMessage(), e1);
		} catch (PersistenceException e1) {
			this.logger.severe("PersistenceException when setAlertingSupported() in onSendRoutingInfoForSMResponse(): " + e1.getMessage(), e1);
		}
	}


	/**
	 * SBB Local Object Methods
	 * 
	 * @throws MAPException
	 */
	@Override
	public void setupReportSMDeliveryStatusRequest(ISDNAddressString msisdn, AddressString serviceCentreAddress, SMDeliveryOutcome sMDeliveryOutcome,
			SccpAddress destAddress, MAPApplicationContext mapApplicationContext, String targetId) {
		if (this.logger.isInfoEnabled()) {
			this.logger.info("Received setupReportSMDeliveryStatus request msisdn= " + msisdn + ", serviceCentreAddress=" + serviceCentreAddress
					+ ", sMDeliveryOutcome=" + sMDeliveryOutcome + ", mapApplicationContext=" + mapApplicationContext);
		}

		this.setTargetId(targetId);

		MAPDialogSms mapDialogSms;
		try {
			mapDialogSms = this.mapProvider.getMAPServiceSms().createNewDialog(mapApplicationContext, this.getServiceCenterSccpAddress(), null, destAddress,
					null);

			ActivityContextInterface mtFOSmsDialogACI = this.mapAcif.getActivityContextInterface(mapDialogSms);
			mtFOSmsDialogACI.attach(this.sbbContext.getSbbLocalObject());

			mapDialogSms.addReportSMDeliveryStatusRequest(msisdn, serviceCentreAddress, sMDeliveryOutcome, null, null, false, false, null, null);
			mapDialogSms.send();
		} catch (MAPException e) {
			this.logger.severe("MAPException when sending reportSMDeliveryStatusRequest: " + e.getMessage(), e);
		}
	}

	/**
	 * CMPs
	 */
	public abstract void setTargetId(String targetId);

	public abstract String getTargetId();

}

