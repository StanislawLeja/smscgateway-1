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
import org.mobicents.protocols.ss7.map.api.dialog.MAPUserAbortChoice;
import org.mobicents.protocols.ss7.map.api.dialog.ProcedureCancellationReason;
import org.mobicents.protocols.ss7.map.api.dialog.ResourceUnavailableReason;
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
import org.mobicents.protocols.ss7.map.errors.MAPErrorMessageAbsentSubscriberImpl;
import org.mobicents.protocols.ss7.map.errors.MAPErrorMessageAbsentSubscriberSMImpl;
import org.mobicents.protocols.ss7.map.errors.MAPErrorMessageCallBarredImpl;
import org.mobicents.protocols.ss7.map.errors.MAPErrorMessageExtensionContainerImpl;
import org.mobicents.protocols.ss7.map.errors.MAPErrorMessageFacilityNotSupImpl;
import org.mobicents.protocols.ss7.map.errors.MAPErrorMessageSystemFailureImpl;
import org.mobicents.protocols.ss7.map.errors.MAPErrorMessageUnknownSubscriberImpl;
import org.mobicents.protocols.ss7.map.service.sms.SendRoutingInfoForSMResponseImpl;
import org.mobicents.protocols.ss7.sccp.parameter.GT0100;
import org.mobicents.protocols.ss7.sccp.parameter.SccpAddress;
import org.mobicents.protocols.ss7.tcap.asn.ApplicationContextName;
import org.mobicents.protocols.ss7.tcap.asn.comp.Problem;
import org.mobicents.slee.resource.map.events.DialogClose;
import org.mobicents.slee.resource.map.events.DialogDelimiter;
import org.mobicents.slee.resource.map.events.DialogProviderAbort;
import org.mobicents.slee.resource.map.events.DialogReject;
import org.mobicents.slee.resource.map.events.DialogTimeout;
import org.mobicents.slee.resource.map.events.DialogUserAbort;
import org.mobicents.slee.resource.map.events.ErrorComponent;
import org.mobicents.slee.resource.map.events.RejectComponent;
import org.mobicents.smsc.slee.services.persistence.ErrorCode;
import org.mobicents.smsc.slee.services.persistence.Sms;
import org.mobicents.smsc.slee.services.persistence.SmsSet;
import org.mobicents.smsc.slee.services.smpp.server.events.SmsSetEvent;

import com.cloudhopper.smpp.SmppConstants;

/**
 * 
 * @author amit bhayani
 * @author sergey vetyutnev
 *
 */
public abstract class SriSbb extends MtCommonSbb {

	private static final String className = "SriSbb";

	// Keep timeout for event suspend to be maximum
//	private static final int EVENT_SUSPEND_TIMEOUT = 1000 * 60 * 3;

	public SriSbb() {
		super(className);
	}

	/**
	 * Event Handlers
	 */

	public void onSms(SmsSetEvent event, ActivityContextInterface aci, EventContext eventContext) {

		// Reduce the events pending to be fired on this ACI
//		MtActivityContextInterface mtSbbActivityContextInterface = this.asSbbActivityContextInterface(aci);
//		int pendingEventsOnNullActivity = mtSbbActivityContextInterface.getPendingEventsOnNullActivity();

		if (this.logger.isInfoEnabled()) {
			this.logger.info("Received SMS. event= " + event + "this=" + this);
		}

//		pendingEventsOnNullActivity = pendingEventsOnNullActivity - 1;
//		mtSbbActivityContextInterface.setPendingEventsOnNullActivity(pendingEventsOnNullActivity);

		// Suspend the delivery of event till unsuspended by other
		// event-handlers
//		eventContext.suspendDelivery(EVENT_SUSPEND_TIMEOUT);
//		this.setNullActivityEventContext(eventContext);

		// TODO: Some mechanism to check if this MSISDN is not available in
		// which case persist this even in database

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

		this.setSendRoutingInfoForSMResponse(null);
		this.setErrorContainer(null);

		this.sendSRI(smsSet.getDestAddr(), smsSet.getDestAddrTon(), smsSet.getDestAddrNpi(),
				this.getSRIMAPApplicationContext(this.maxMAPApplicationContextVersion));
	}

	/**
	 * Components Events override from MtCommonSbb that we care
	 */

	@Override
	public void onErrorComponent(ErrorComponent event, ActivityContextInterface aci) {
//		super.onErrorComponent(event, aci);

		if (this.logger.isInfoEnabled()) {
			this.logger.info("Rx :  onErrorComponent " + event + " Dialog=" + event.getMAPDialog());
		}

		// we store error into CMP
		ErrorContainer cont = new ErrorContainer();
		MAPErrorMessage mapErrorMessage = event.getMAPErrorMessage();
		cont.setAbsentSubscriber((MAPErrorMessageAbsentSubscriberImpl) mapErrorMessage.getEmAbsentSubscriber());
		cont.setAbsentSubscriberSM((MAPErrorMessageAbsentSubscriberSMImpl) mapErrorMessage.getEmAbsentSubscriberSM());
		cont.setCallBarred((MAPErrorMessageCallBarredImpl) mapErrorMessage.getEmCallBarred());
		cont.setErrorsExtCont((MAPErrorMessageExtensionContainerImpl) mapErrorMessage.getEmExtensionContainer());
		cont.setFacilityNotSupported((MAPErrorMessageFacilityNotSupImpl) mapErrorMessage.getEmFacilityNotSup());
		cont.setSystemFailure((MAPErrorMessageSystemFailureImpl) mapErrorMessage.getEmSystemFailure());
		cont.setUnknownSubscriber((MAPErrorMessageUnknownSubscriberImpl) mapErrorMessage.getEmUnknownSubscriber());
		this.setErrorContainer(cont);

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

	/**
	 * Dialog Events override from MtCommonSbb that we care
	 */
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

			// lets detach so we don't get onDialogRelease() which will start
			// delivering SMS waiting in queue for same MSISDN
//			aci.detach(this.sbbContext.getSbbLocalObject());

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

		if (logger.isWarningEnabled()) {
			this.logger.warning("Rx : Sri onDialogReject=" + evt);
		}

		this.onDeliveryError(ErrorAction.permanentFailure, ErrorCode.HLR_REJECT_AFTER_ROUTING_INFO,
				"onDialogReject after SRI Request: " + mapRefuseReason != null ? mapRefuseReason.toString() : "");

//		super.onDialogReject(evt, aci);
	}

	public void onDialogProviderAbort(DialogProviderAbort evt, ActivityContextInterface aci) {
		this.logger.severe("Rx :  onDialogProviderAbort=" + evt);

		MAPAbortProviderReason abortProviderReason = evt.getAbortProviderReason();

		this.onDeliveryError(ErrorAction.permanentFailure, ErrorCode.HLR_REJECT_AFTER_ROUTING_INFO, "onDialogProviderAbort after SRI Request: "
				+ abortProviderReason != null ? abortProviderReason.toString() : "");
	}

	@Override
	public void onRejectComponent(RejectComponent event, ActivityContextInterface aci) {
		this.logger.severe("Rx :  onRejectComponent" + event);

		Problem problem = event.getProblem();
		String reason = null;
		switch (problem.getType()) {
		case General:
			reason = problem.getGeneralProblemType().toString();
			break;
		case Invoke:
			reason = problem.getInvokeProblemType().toString();
			break;
		case ReturnResult:
			reason = problem.getReturnResultProblemType().toString();
			break;
		case ReturnError:
			reason = problem.getReturnErrorProblemType().toString();
			break;
		default:
			reason = "RejectComponent_unknown_" + problem.getType();
			break;
		}

		try {
			event.getMAPDialog().close(false);
		} catch (Exception e) {
		}

		this.onDeliveryError(ErrorAction.permanentFailure, ErrorCode.HLR_REJECT_AFTER_ROUTING_INFO,
				"onRejectComponent after SRI Request: " + reason != null ? reason.toString() : "");
	}

	public void onDialogUserAbort(DialogUserAbort evt, ActivityContextInterface aci) {
		this.logger.severe("Rx :  onDialogUserAbort=" + evt);

		MAPUserAbortChoice userReason = evt.getUserReason();
		String reason = null;
		if (userReason.isUserSpecificReason()) {
			reason = MAP_USER_ABORT_CHOICE_USER_SPECIFIC_REASON;
		} else if (userReason.isUserResourceLimitation()) {
			reason = MAP_USER_ABORT_CHOICE_USER_RESOURCE_LIMITATION;
		} else if (userReason.isResourceUnavailableReason()) {
			ResourceUnavailableReason resourceUnavailableReason = userReason.getResourceUnavailableReason();
			reason = resourceUnavailableReason.toString();
		} else if (userReason.isProcedureCancellationReason()) {
			ProcedureCancellationReason procedureCancellationReason = userReason.getProcedureCancellationReason();
			reason = procedureCancellationReason.toString();
		} else {
			reason = MAP_USER_ABORT_CHOICE_UNKNOWN;
		}

		this.onDeliveryError(ErrorAction.permanentFailure, ErrorCode.HLR_REJECT_AFTER_ROUTING_INFO,
				"onDialogUserAbort after SRI Request: " + reason != null ? reason.toString() : "");
	}

	public void onDialogTimeout(DialogTimeout evt, ActivityContextInterface aci) {
		// TODO: may be it is not a permanent failure case ???

		this.logger.severe("Rx :  onDialogTimeout=" + evt);

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

		// lets detach so we don't get onDialogRelease() which will start
		// delivering SMS waiting in queue for same MSISDN
//		aci.detach(this.sbbContext.getSbbLocalObject());

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

	private void onSriFullResponse() {

		SendRoutingInfoForSMResponse sendRoutingInfoForSMResponse = this.getSendRoutingInfoForSMResponse();
		ErrorContainer errorContainer = this.getErrorContainer();
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
		} else if (errorContainer != null) {
			// we have a negative response
			if (errorContainer.getAbsentSubscriber() != null) {
				this.onDeliveryError(ErrorAction.mobileNotReachableFlag, ErrorCode.ABSENT_SUBSCRIBER,
						"AbsentSubscriber response from HLR: " + errorContainer.getAbsentSubscriber());
			} else if (errorContainer.getAbsentSubscriberSM() != null) {
				this.onDeliveryError(ErrorAction.mobileNotReachableFlag, ErrorCode.ABSENT_SUBSCRIBER,
						"AbsentSubscriber response from HLR: " + errorContainer.getAbsentSubscriberSM());
			} else if (errorContainer.getCallBarred() != null) {
				this.onDeliveryError(ErrorAction.permanentFailure, ErrorCode.CALL_BARRED, "CallBarred response from HLR: " + errorContainer.getCallBarred());
			} else if (errorContainer.getFacilityNotSupported() != null) {
				this.onDeliveryError(ErrorAction.permanentFailure, ErrorCode.FACILITY_NOT_SUPPORTED,
						"CallBarred response from HLR: " + errorContainer.getFacilityNotSupported());
			} else if (errorContainer.getSystemFailure() != null) {
				// TODO: may be systemFailure is not a permanent error case ?
				this.onDeliveryError(ErrorAction.permanentFailure, ErrorCode.SYSTEM_FAILURE,
						"SystemFailure response from HLR: " + errorContainer.getSystemFailure());
			} else if (errorContainer.getUnknownSubscriber() != null) {
				this.onDeliveryError(ErrorAction.permanentFailure, ErrorCode.UNKNOWN_SUBSCRIBER,
						"UnknownSubscriber response from HLR: " + errorContainer.getUnknownSubscriber());
			} else if (errorContainer.getErrorsExtCont() != null) {
				if (errorContainer.getErrorsExtCont().getErrorCode() == MAPErrorCode.dataMissing) {
					this.onDeliveryError(ErrorAction.permanentFailure, ErrorCode.DATA_MISSING, "DataMissing response from HLR");
				} else if (errorContainer.getErrorsExtCont().getErrorCode() == MAPErrorCode.unexpectedDataValue) {
					this.onDeliveryError(ErrorAction.permanentFailure, ErrorCode.UNEXPECTED_DATA, "UnexpectedDataValue response from HLR");
				} else if (errorContainer.getErrorsExtCont().getErrorCode() == MAPErrorCode.teleserviceNotProvisioned) {
					this.onDeliveryError(ErrorAction.permanentFailure, ErrorCode.TELESERVICE_NOT_PROVISIONED, "TeleserviceNotProvisioned response from HLR");
				} else {
					this.onDeliveryError(ErrorAction.permanentFailure, ErrorCode.UNEXPECTED_DATA_FROM_HLR,
							"Error response from HLR: " + errorContainer.getErrorsExtCont());
				}
			}
		} else {
			// we have no responses - this is an error behaviour
			this.onDeliveryError(ErrorAction.permanentFailure, ErrorCode.HLR_REJECT_AFTER_ROUTING_INFO, "Empty response after SRI Request");
		}
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
	 * CMPs
	 */
	public abstract void setSendRoutingInfoForSMResponse(SendRoutingInfoForSMResponseImpl sendRoutingInfoForSMResponse);

	public abstract SendRoutingInfoForSMResponseImpl getSendRoutingInfoForSMResponse();

	public abstract void setErrorContainer(ErrorContainer errorContainer);

	public abstract ErrorContainer getErrorContainer();

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

			// MtActivityContextInterface mtSbbActivityContextInterface =
			// this.asSbbActivityContextInterface(this.getNullActivityEventContext()
			// .getActivityContextInterface());
			// this.resumeNullActivityEventDelivery(mtSbbActivityContextInterface,
			// this.getNullActivityEventContext());
		} catch (Exception e) {
			this.logger.severe("Exception while trying to creat MtSbb child", e);

			// MtActivityContextInterface mtSbbActivityContextInterface =
			// this.asSbbActivityContextInterface(this.getNullActivityEventContext()
			// .getActivityContextInterface());
			// this.resumeNullActivityEventDelivery(mtSbbActivityContextInterface,
			// this.getNullActivityEventContext());
		}
	}

	public SmsDeliveryData doGetSmsDeliveryData() {
		try {
			ChildRelation relation = getMtSbb();
			MtSbbLocalObject mtSbbLocalObject = (MtSbbLocalObject) relation.create();
			return mtSbbLocalObject.doGetSmsDeliveryData();
		} catch (CreateException e) {
			this.logger.severe("Could not create Child SBB", e);

			// MtActivityContextInterface mtSbbActivityContextInterface =
			// this.asSbbActivityContextInterface(this.getNullActivityEventContext()
			// .getActivityContextInterface());
			// this.resumeNullActivityEventDelivery(mtSbbActivityContextInterface,
			// this.getNullActivityEventContext());
		} catch (Exception e) {
			this.logger.severe("Exception while trying to creat MtSbb child", e);

			// MtActivityContextInterface mtSbbActivityContextInterface =
			// this.asSbbActivityContextInterface(this.getNullActivityEventContext()
			// .getActivityContextInterface());
			// this.resumeNullActivityEventDelivery(mtSbbActivityContextInterface,
			// this.getNullActivityEventContext());
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

	public InformServiceCenterContainer doGgetInformServiceCenterContainer() {
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
		// Send out SMS
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
			logger.severe("Error while trying to send SendRoutingInfoForSMRequest", e);
			// something horrible, release MAPDialog and free resources

			if (mapDialogSms != null) {
				mapDialogSms.release();
			}

//			MtActivityContextInterface mtSbbActivityContextInterface = this.asSbbActivityContextInterface(this
//					.getNullActivityEventContext().getActivityContextInterface());
//			this.resumeNullActivityEventDelivery(mtSbbActivityContextInterface, this.getNullActivityEventContext());

			// TODO : Take care of error condition
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

	public void setupReportSMDeliveryStatusRequest(String destinationAddress, int ton, int npi, SMDeliveryOutcome sMDeliveryOutcome) {
		try {
			ChildRelation relation = getRsdsSbb();
			RsdsSbbLocalObject rsdsSbbLocalObject = (RsdsSbbLocalObject) relation.create();

			ISDNAddressString isdn = this.getCalledPartyISDNAddressString(destinationAddress, ton, npi);
			AddressString serviceCentreAddress = getServiceCenterAddressString();
			SccpAddress destAddress = this.convertAddressFieldToSCCPAddress(destinationAddress, ton, npi);
			rsdsSbbLocalObject.setupReportSMDeliveryStatusRequest(isdn, serviceCentreAddress, sMDeliveryOutcome, destAddress,
					this.getSRIMAPApplicationContext(MAPApplicationContextVersion.getInstance(this.getSriMapVersion())));
		} catch (TransactionRequiredLocalException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SLEEException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (CreateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private SccpAddress convertAddressFieldToSCCPAddress(String address, int ton, int npi) {
		NumberingPlan np = NumberingPlan.ISDN_TELEPHONY;
		NatureOfAddress na = NatureOfAddress.INTERNATIONAL;
		switch (ton) {
		case SmppConstants.TON_INTERNATIONAL:
			na = NatureOfAddress.INTERNATIONAL;
			break;
		}
		switch (npi) {
		case SmppConstants.NPI_ISDN:
			np = NumberingPlan.ISDN_TELEPHONY;
			break;
		}
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
