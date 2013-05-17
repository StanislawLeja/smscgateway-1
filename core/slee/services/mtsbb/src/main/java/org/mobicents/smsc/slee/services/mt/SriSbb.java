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
import javax.slee.ChildRelation;
import javax.slee.CreateException;
import javax.slee.EventContext;
import javax.slee.SLEEException;
import javax.slee.TransactionRequiredLocalException;

import org.mobicents.protocols.ss7.indicator.NatureOfAddress;
import org.mobicents.protocols.ss7.indicator.NumberingPlan;
import org.mobicents.protocols.ss7.indicator.RoutingIndicator;
import org.mobicents.protocols.ss7.map.api.MAPApplicationContext;
import org.mobicents.protocols.ss7.map.api.MAPApplicationContextName;
import org.mobicents.protocols.ss7.map.api.MAPApplicationContextVersion;
import org.mobicents.protocols.ss7.map.api.MAPException;
import org.mobicents.protocols.ss7.map.api.dialog.MAPAbortProviderReason;
import org.mobicents.protocols.ss7.map.api.dialog.MAPRefuseReason;
import org.mobicents.protocols.ss7.map.api.errors.MAPErrorCode;
import org.mobicents.protocols.ss7.map.api.errors.MAPErrorMessage;
import org.mobicents.protocols.ss7.map.api.errors.MAPErrorMessageAbsentSubscriber;
import org.mobicents.protocols.ss7.map.api.primitives.AddressString;
import org.mobicents.protocols.ss7.map.api.primitives.ISDNAddressString;
import org.mobicents.protocols.ss7.map.api.service.sms.MAPDialogSms;
import org.mobicents.protocols.ss7.map.api.service.sms.MWStatus;
import org.mobicents.protocols.ss7.map.api.service.sms.SMDeliveryOutcome;
import org.mobicents.protocols.ss7.map.api.service.sms.SendRoutingInfoForSMRequest;
import org.mobicents.protocols.ss7.map.api.service.sms.InformServiceCentreRequest;
import org.mobicents.protocols.ss7.map.api.service.sms.SendRoutingInfoForSMResponse;
import org.mobicents.protocols.ss7.map.service.sms.SendRoutingInfoForSMResponseImpl;
import org.mobicents.protocols.ss7.sccp.parameter.GT0100;
import org.mobicents.protocols.ss7.sccp.parameter.SccpAddress;
import org.mobicents.protocols.ss7.tcap.asn.ApplicationContextName;
import org.mobicents.slee.resource.map.events.DialogClose;
import org.mobicents.slee.resource.map.events.DialogDelimiter;
import org.mobicents.slee.resource.map.events.DialogProviderAbort;
import org.mobicents.slee.resource.map.events.DialogReject;
import org.mobicents.slee.resource.map.events.DialogTimeout;
import org.mobicents.slee.resource.map.events.DialogUserAbort;
import org.mobicents.slee.resource.map.events.ErrorComponent;
import org.mobicents.slee.resource.map.events.RejectComponent;
import org.mobicents.smsc.slee.services.smpp.server.events.SmsSetEvent;
import org.mobicents.smsc.slee.resources.peristence.MessageUtil;
import org.mobicents.smsc.slee.resources.persistence.ErrorCode;
import org.mobicents.smsc.slee.resources.persistence.Sms;
import org.mobicents.smsc.slee.resources.persistence.SmsSet;

/**
 * 
 * @author amit bhayani
 * @author sergey vetyutnev
 *
 */
public abstract class SriSbb extends MtCommonSbb {

	private static final String className = "SriSbb";

	public SriSbb() {
		super(className);
	}

	/**
	 * Event Handlers
	 */

	public void onSms(SmsSetEvent event, ActivityContextInterface aci, EventContext eventContext) {

		if (this.logger.isInfoEnabled()) {
			this.logger.info("Received Submit SMS. event= " + event + "this=" + this);
		}

		SmsSet smsSet = event.getSmsSet();
		int curMsg = 0;
		Sms sms = smsSet.getSms(curMsg);
		if (sms != null) {
			this.startMessageDelivery(sms);
		}

		this.doSetCurrentMsgNum(curMsg);
		SmsDeliveryData smsDeliveryData = new SmsDeliveryData();
		smsDeliveryData.setSmsSet(smsSet);
		this.doSetSmsDeliveryData(smsDeliveryData);

//		this.setSendRoutingInfoForSMResponse(null);
//		this.setErrorContainer(null);

		this.sendSRI(smsSet.getDestAddr(), smsSet.getDestAddrTon(), smsSet.getDestAddrNpi(),
				this.getSRIMAPApplicationContext(this.maxMAPApplicationContextVersion));
	}

	/**
	 * Components Events override from MtCommonSbb that we care
	 */

	@Override
	public void onErrorComponent(ErrorComponent event, ActivityContextInterface aci) {
		super.onErrorComponent(event, aci);

		// we store error into CMP
		MAPErrorMessage mapErrorMessage = event.getMAPErrorMessage();
		this.setErrorResponse(mapErrorMessage);

		if (mapErrorMessage.isEmAbsentSubscriber()) {
			MAPErrorMessageAbsentSubscriber errAs = mapErrorMessage.getEmAbsentSubscriber();
			Boolean mwdSet = errAs.getMwdSet();
			if (mwdSet != null && mwdSet) {
				InformServiceCenterContainer informServiceCenterContainer = new InformServiceCenterContainer();
				MWStatus mwStatus = event.getMAPDialog().getService().getMAPProvider().getMAPParameterFactory().createMWStatus(false, true, false, false);
				informServiceCenterContainer.setMwStatus(mwStatus);
				this.doSetInformServiceCenterContainer(informServiceCenterContainer);
			}
		}
	}

	public void onRejectComponent(RejectComponent event, ActivityContextInterface aci) {
		super.onRejectComponent(event, aci);

		String reason = this.getRejectComponentReason(event);

		this.onDeliveryError(ErrorAction.permanentFailure, ErrorCode.HLR_REJECT_AFTER_ROUTING_INFO,
				"onRejectComponent after SRI Request: " + reason != null ? reason.toString() : "");
	}

	/**
	 * Dialog Events override from MtCommonSbb that we care
	 */

	@Override
	public void onDialogReject(DialogReject evt, ActivityContextInterface aci) {

		MAPRefuseReason mapRefuseReason = evt.getRefuseReason();
		SmsDeliveryData smsDeliveryData = this.doGetSmsDeliveryData();
		if (smsDeliveryData == null) {
			this.logger.severe("SmsDeliveryData CMP missed");
			return;
		}

		if (mapRefuseReason == MAPRefuseReason.PotentialVersionIncompatibility
				&& evt.getMAPDialog().getApplicationContext().getApplicationContextVersion() != MAPApplicationContextVersion.version1) {
			if (logger.isWarningEnabled()) {
				this.logger.warning("Rx : Sri onDialogReject / PotentialVersionIncompatibility=" + evt);
			}
			// possible a peer supports only MAP V1
			// Now send new SRI with supported ACN (MAP V1)
			if (smsDeliveryData != null) {
				SmsSet smsSet = smsDeliveryData.getSmsSet();
				this.sendSRI(smsSet.getDestAddr(), smsSet.getDestAddrTon(), smsSet.getDestAddrNpi(),
						this.getSRIMAPApplicationContext(MAPApplicationContextVersion.version1));
				return;
			}
		}

		// If ACN not supported, lets use the new one suggested
		if (mapRefuseReason == MAPRefuseReason.ApplicationContextNotSupported) {
			if (logger.isWarningEnabled()) {
				this.logger.warning("Rx : Sri onDialogReject / ApplicationContextNotSupported=" + evt);
			}

			// Now send new SRI with supported ACN
			ApplicationContextName tcapApplicationContextName = evt.getAlternativeApplicationContext();
			MAPApplicationContext supportedMAPApplicationContext = MAPApplicationContext.getInstance(tcapApplicationContextName.getOid());

			if (smsDeliveryData != null) {
				SmsSet smsSet = smsDeliveryData.getSmsSet();
				this.sendSRI(smsSet.getDestAddr(), smsSet.getDestAddrTon(), smsSet.getDestAddrNpi(),
						this.getSRIMAPApplicationContext(supportedMAPApplicationContext.getApplicationContextVersion()));
				return;
			}
		}

		super.onDialogReject(evt, aci);
		this.onDeliveryError(ErrorAction.permanentFailure, ErrorCode.HLR_REJECT_AFTER_ROUTING_INFO,
				"onDialogReject after SRI Request: " + mapRefuseReason != null ? mapRefuseReason.toString() : "");
	}

	@Override
	public void onDialogProviderAbort(DialogProviderAbort evt, ActivityContextInterface aci) {
		super.onDialogProviderAbort(evt, aci);

		MAPAbortProviderReason abortProviderReason = evt.getAbortProviderReason();

		this.onDeliveryError(ErrorAction.permanentFailure, ErrorCode.HLR_REJECT_AFTER_ROUTING_INFO, "onDialogProviderAbort after SRI Request: "
				+ abortProviderReason != null ? abortProviderReason.toString() : "");
	}

	@Override
	public void onDialogUserAbort(DialogUserAbort evt, ActivityContextInterface aci) {
		super.onDialogUserAbort(evt, aci);

		String reason = getUserAbortReason(evt);

		this.onDeliveryError(ErrorAction.permanentFailure, ErrorCode.HLR_REJECT_AFTER_ROUTING_INFO,
				"onDialogUserAbort after SRI Request: " + reason != null ? reason.toString() : "");
	}

	@Override
	public void onDialogTimeout(DialogTimeout evt, ActivityContextInterface aci) {
		// TODO: may be it is not a permanent failure case ???

		super.onDialogTimeout(evt, aci);

		this.onDeliveryError(ErrorAction.permanentFailure, ErrorCode.HLR_REJECT_AFTER_ROUTING_INFO, "onDialogTimeout after SRI Request");
	}

	/**
	 * MAP SMS Events
	 */

	/**
	 * Received SRI request. But this is error, we should never receive this
	 * request
	 * 
	 * @param evt
	 * @param aci
	 */
	public void onSendRoutingInfoForSMRequest(SendRoutingInfoForSMRequest evt, ActivityContextInterface aci) {
		this.logger.severe("Received SEND_ROUTING_INFO_FOR_SM_REQUEST = " + evt);
	}

	/**
	 * Received response for SRI sent earlier
	 * 
	 * @param evt
	 * @param aci
	 */
	public void onSendRoutingInfoForSMResponse(SendRoutingInfoForSMResponse evt, ActivityContextInterface aci) {
		if (this.logger.isInfoEnabled()) {
			this.logger.info("Received SEND_ROUTING_INFO_FOR_SM_RESPONSE = " + evt + " Dialog=" + evt.getMAPDialog());
		}

		if (evt.getMAPDialog().getApplicationContext().getApplicationContextVersion() == MAPApplicationContextVersion.version1 && evt.getMwdSet() != null
				&& evt.getMwdSet()) {
			InformServiceCenterContainer informServiceCenterContainer = new InformServiceCenterContainer();
			MWStatus mwStatus = evt.getMAPDialog().getService().getMAPProvider().getMAPParameterFactory().createMWStatus(false, true, false, false);
			informServiceCenterContainer.setMwStatus(mwStatus);
			this.doSetInformServiceCenterContainer(informServiceCenterContainer);
		}

		this.setSendRoutingInfoForSMResponse((SendRoutingInfoForSMResponseImpl)evt);
	}

	public void onInformServiceCentreRequest(InformServiceCentreRequest evt, ActivityContextInterface aci) {
		if (this.logger.isInfoEnabled()) {
			this.logger.info("Received INFORM_SERVICE_CENTER_REQUEST = " + evt + " Dialog=" + evt.getMAPDialog());
		}

		InformServiceCenterContainer informServiceCenterContainer = new InformServiceCenterContainer();
		informServiceCenterContainer.setMwStatus(evt.getMwStatus());
		this.doSetInformServiceCenterContainer(informServiceCenterContainer);
	}

	public void onDialogDelimiter(DialogDelimiter evt, ActivityContextInterface aci) {
		super.onDialogDelimiter(evt, aci);

		this.onSriFullResponse();
	}

	public void onDialogClose(DialogClose evt, ActivityContextInterface aci) {
		super.onDialogClose(evt, aci);

		this.onSriFullResponse();
	}

	/**
	 * SBB Local Object Methods
	 * 
	 */

	public void setupReportSMDeliveryStatusRequest(String destinationAddress, int ton, int npi, SMDeliveryOutcome sMDeliveryOutcome, String targetId) {
		try {
			ChildRelation relation = getRsdsSbb();
			RsdsSbbLocalObject rsdsSbbLocalObject = (RsdsSbbLocalObject) relation.create();

			ISDNAddressString isdn = this.getCalledPartyISDNAddressString(destinationAddress, ton, npi);
			AddressString serviceCentreAddress = getServiceCenterAddressString();
			SccpAddress destAddress = this.convertAddressFieldToSCCPAddress(destinationAddress, ton, npi);
			rsdsSbbLocalObject.setupReportSMDeliveryStatusRequest(isdn, serviceCentreAddress, sMDeliveryOutcome, destAddress,
					this.getSRIMAPApplicationContext(MAPApplicationContextVersion.getInstance(this.getSriMapVersion())), targetId);
		} catch (TransactionRequiredLocalException e) {
			this.logger.severe("TransactionRequiredLocalException when obtaining RsdsSbbLocalObject", e);
		} catch (SLEEException e) {
			this.logger.severe("SLEEException when obtaining RsdsSbbLocalObject", e);
		} catch (CreateException e) {
			this.logger.severe("CreateException when obtaining RsdsSbbLocalObject", e);
		}
	}

	/**
	 * CMPs
	 */
	public abstract void setSendRoutingInfoForSMResponse(SendRoutingInfoForSMResponseImpl sendRoutingInfoForSMResponse);

	public abstract SendRoutingInfoForSMResponseImpl getSendRoutingInfoForSMResponse();

	public abstract void setErrorResponse(MAPErrorMessage errorResponse);

	public abstract MAPErrorMessage getErrorResponse();

	public abstract void setSriMapVersion(int sriMapVersion);

	public abstract int getSriMapVersion();

	/**
	 * Get Mt child SBB
	 * 
	 * @return
	 */
	public abstract ChildRelation getMtSbb();

	public abstract ChildRelation getRsdsSbb();

	public void doSetSmsDeliveryData(SmsDeliveryData smsDeliveryData) {
		try {
			ChildRelation relation = getMtSbb();
			MtForwardSmsInterface mtSbbLocalObject = (MtSbbLocalObject) relation.create();
			mtSbbLocalObject.doSetSmsDeliveryData(smsDeliveryData);
		} catch (CreateException e) {
			this.logger.severe("Could not create Child SBB", e);
		} catch (Exception e) {
			this.logger.severe("Exception while trying to creat MtSbb child", e);
		}
	}

	public SmsDeliveryData doGetSmsDeliveryData() {
		try {
			ChildRelation relation = getMtSbb();
			MtSbbLocalObject mtSbbLocalObject = (MtSbbLocalObject) relation.create();
			return mtSbbLocalObject.doGetSmsDeliveryData();
		} catch (CreateException e) {
			this.logger.severe("Could not create Child SBB", e);
		} catch (Exception e) {
			this.logger.severe("Exception while trying to creat MtSbb child", e);
		}

		return null;
	}

	public void doSetCurrentMsgNum(int currentMsgNum) {
		try {
			ChildRelation relation = getMtSbb();
			MtForwardSmsInterface mtSbbLocalObject = (MtSbbLocalObject) relation.create();
			mtSbbLocalObject.doSetCurrentMsgNum(currentMsgNum);
		} catch (CreateException e) {
			this.logger.severe("Could not create Child SBB", e);
		} catch (Exception e) {
			this.logger.severe("Exception while trying to creat MtSbb child", e);
		}
	}

	public int doGetCurrentMsgNum() {
		try {
			ChildRelation relation = getMtSbb();
			MtSbbLocalObject mtSbbLocalObject = (MtSbbLocalObject) relation.create();
			return mtSbbLocalObject.doGetCurrentMsgNum();
		} catch (CreateException e) {
			this.logger.severe("Could not create Child SBB", e);
		} catch (Exception e) {
			this.logger.severe("Exception while trying to creat MtSbb child", e);
		}

		return 0;
	}

	public void doSetInformServiceCenterContainer(InformServiceCenterContainer informServiceCenterContainer) {
		try {
			ChildRelation relation = getMtSbb();
			MtForwardSmsInterface mtSbbLocalObject = (MtSbbLocalObject) relation.create();
			mtSbbLocalObject.doSetInformServiceCenterContainer(informServiceCenterContainer);
		} catch (CreateException e) {
			this.logger.severe("Could not create Child SBB", e);
		} catch (Exception e) {
			this.logger.severe("Exception while trying to creat MtSbb child", e);
		}
	}

	public InformServiceCenterContainer doGetInformServiceCenterContainer() {
		try {
			ChildRelation relation = getMtSbb();
			MtSbbLocalObject mtSbbLocalObject = (MtSbbLocalObject) relation.create();
			return mtSbbLocalObject.doGetInformServiceCenterContainer();
		} catch (CreateException e) {
			this.logger.severe("Could not create Child SBB", e);
		} catch (Exception e) {
			this.logger.severe("Exception while trying to creat MtSbb child", e);
		}

		return null;
	}


	/**
	 * Private methods
	 */

	private void sendSRI(String destinationAddress, int ton, int npi, MAPApplicationContext mapApplicationContext) {
		// Send out SRI
		MAPDialogSms mapDialogSms = null;
		try {
			// 1. Create Dialog first and add the SRI request to it
			mapDialogSms = this.setupRoutingInfoForSMRequestIndication(destinationAddress, ton, npi, mapApplicationContext);

			// 2. Create the ACI and attach this SBB
			ActivityContextInterface sriDialogACI = this.mapAcif.getActivityContextInterface(mapDialogSms);
			sriDialogACI.attach(this.sbbContext.getSbbLocalObject());

			// 3. Finally send the request
			mapDialogSms.send();
		} catch (MAPException e) {
			if (mapDialogSms != null) {
				mapDialogSms.release();
			}

			String reason = "MAPException when sending SRI from sendSRI(): " + e.toString();
			this.logger.severe(reason, e);
			ErrorCode smStatus = ErrorCode.SC_SYSTEM_ERROR;
			this.onDeliveryError(ErrorAction.permanentFailure, smStatus, reason);
		}
	}

	private MAPDialogSms setupRoutingInfoForSMRequestIndication(String destinationAddress, int ton, int npi, MAPApplicationContext mapApplicationContext)
			throws MAPException {
		// this.mapParameterFactory.creat

		SccpAddress destinationAddr = this.convertAddressFieldToSCCPAddress(destinationAddress, ton, npi);

		MAPDialogSms mapDialogSms = this.mapProvider.getMAPServiceSms().createNewDialog(mapApplicationContext, this.getServiceCenterSccpAddress(), null,
				destinationAddr, null);

		mapDialogSms.addSendRoutingInfoForSMRequest(this.getCalledPartyISDNAddressString(destinationAddress, ton, npi), true,
				this.getServiceCenterAddressString(), null, false, null, null, null);

		return mapDialogSms;
	}

	private void onSriFullResponse() {

		SendRoutingInfoForSMResponse sendRoutingInfoForSMResponse = this.getSendRoutingInfoForSMResponse();
		MAPErrorMessage errorMessage = this.getErrorResponse();
		if (sendRoutingInfoForSMResponse != null) {

			// we have positive response to SRI request - we will try to send messages
			try {
				MtSbbLocalObject mtSbbLocalObject = null;
				ChildRelation relation = getMtSbb();
				mtSbbLocalObject = (MtSbbLocalObject) relation.create();

				mtSbbLocalObject.setupMtForwardShortMessageRequest(sendRoutingInfoForSMResponse.getLocationInfoWithLMSI().getNetworkNodeNumber(),
						sendRoutingInfoForSMResponse.getIMSI(), sendRoutingInfoForSMResponse.getLocationInfoWithLMSI().getLMSI());

			} catch (CreateException e) {
				this.logger.severe("Could not create Child SBB", e);
			} catch (Exception e) {
				this.logger.severe("Exception while trying to creat MtSbb child", e);
			}
		} else if (errorMessage != null) {

			// we have a negative response
			if (errorMessage.isEmAbsentSubscriber()) {
				this.onDeliveryError(ErrorAction.mobileNotReachableFlag, ErrorCode.ABSENT_SUBSCRIBER,
						"AbsentSubscriber response from HLR: " + errorMessage.toString());
			} else if (errorMessage.isEmAbsentSubscriberSM()) {
				this.onDeliveryError(ErrorAction.mobileNotReachableFlag, ErrorCode.ABSENT_SUBSCRIBER,
						"AbsentSubscriber response from HLR: " + errorMessage.toString());
			} else if (errorMessage.isEmCallBarred()) {
				this.onDeliveryError(ErrorAction.permanentFailure, ErrorCode.CALL_BARRED, "CallBarred response from HLR: " + errorMessage.toString());
			} else if (errorMessage.isEmFacilityNotSup()) {
				this.onDeliveryError(ErrorAction.permanentFailure, ErrorCode.FACILITY_NOT_SUPPORTED,
						"CallBarred response from HLR: " + errorMessage.toString());
			} else if (errorMessage.isEmSystemFailure()) {
				// TODO: may be systemFailure is not a permanent error case ?
				this.onDeliveryError(ErrorAction.permanentFailure, ErrorCode.SYSTEM_FAILURE,
						"SystemFailure response from HLR: " + errorMessage.toString());
			} else if (errorMessage.isEmUnknownSubscriber()) {
				this.onDeliveryError(ErrorAction.permanentFailure, ErrorCode.UNKNOWN_SUBSCRIBER,
						"UnknownSubscriber response from HLR: " + errorMessage.toString());
			} else if (errorMessage.isEmExtensionContainer()) {
				if (errorMessage.getEmExtensionContainer().getErrorCode() == MAPErrorCode.dataMissing) {
					this.onDeliveryError(ErrorAction.permanentFailure, ErrorCode.DATA_MISSING, "DataMissing response from HLR");
				} else if (errorMessage.getEmExtensionContainer().getErrorCode() == MAPErrorCode.unexpectedDataValue) {
					this.onDeliveryError(ErrorAction.permanentFailure, ErrorCode.UNEXPECTED_DATA, "UnexpectedDataValue response from HLR");
				} else if (errorMessage.getEmExtensionContainer().getErrorCode() == MAPErrorCode.teleserviceNotProvisioned) {
					this.onDeliveryError(ErrorAction.permanentFailure, ErrorCode.TELESERVICE_NOT_PROVISIONED, "TeleserviceNotProvisioned response from HLR");
				} else {
					this.onDeliveryError(ErrorAction.permanentFailure, ErrorCode.UNEXPECTED_DATA_FROM_HLR,
							"Error response from HLR: " + errorMessage.toString());
				}
			}
		} else {
			// we have no responses - this is an error behaviour
			this.onDeliveryError(ErrorAction.permanentFailure, ErrorCode.HLR_REJECT_AFTER_ROUTING_INFO, "Empty response after SRI Request");
		}
	}

	private SccpAddress convertAddressFieldToSCCPAddress(String address, int ton, int npi) {
		NumberingPlan np = MessageUtil.getSccpNumberingPlan(npi);
		NatureOfAddress na = MessageUtil.getSccpNatureOfAddress(ton);
		GT0100 gt = new GT0100(0, np, na, address);
		return new SccpAddress(RoutingIndicator.ROUTING_BASED_ON_GLOBAL_TITLE, 0, gt, smscPropertiesManagement.getHlrSsn());
	}

	private MAPApplicationContext getSRIMAPApplicationContext(MAPApplicationContextVersion applicationContextVersion) {
		MAPApplicationContext mapApplicationContext = MAPApplicationContext.getInstance(MAPApplicationContextName.shortMsgGatewayContext,
				applicationContextVersion);
		this.setSriMapVersion(applicationContextVersion.getVersion());
		return mapApplicationContext;
	}

}
