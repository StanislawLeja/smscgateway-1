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

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Date;

import javax.slee.ActivityContextInterface;

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
import org.mobicents.protocols.ss7.map.api.errors.MAPErrorMessageAbsentSubscriberSM;
import org.mobicents.protocols.ss7.map.api.errors.MAPErrorMessageExtensionContainer;
import org.mobicents.protocols.ss7.map.api.errors.MAPErrorMessageFacilityNotSup;
import org.mobicents.protocols.ss7.map.api.errors.MAPErrorMessageSMDeliveryFailure;
import org.mobicents.protocols.ss7.map.api.errors.MAPErrorMessageSubscriberBusyForMtSms;
import org.mobicents.protocols.ss7.map.api.errors.MAPErrorMessageSystemFailure;
import org.mobicents.protocols.ss7.map.api.errors.SMEnumeratedDeliveryFailureCause;
import org.mobicents.protocols.ss7.map.api.primitives.IMSI;
import org.mobicents.protocols.ss7.map.api.primitives.ISDNAddressString;
import org.mobicents.protocols.ss7.map.api.primitives.LMSI;
import org.mobicents.protocols.ss7.map.api.service.sms.ForwardShortMessageRequest;
import org.mobicents.protocols.ss7.map.api.service.sms.ForwardShortMessageResponse;
import org.mobicents.protocols.ss7.map.api.service.sms.MAPDialogSms;
import org.mobicents.protocols.ss7.map.api.service.sms.MtForwardShortMessageRequest;
import org.mobicents.protocols.ss7.map.api.service.sms.MtForwardShortMessageResponse;
import org.mobicents.protocols.ss7.map.api.service.sms.SMDeliveryOutcome;
import org.mobicents.protocols.ss7.map.api.service.sms.SM_RP_DA;
import org.mobicents.protocols.ss7.map.api.service.sms.SM_RP_OA;
import org.mobicents.protocols.ss7.map.api.service.sms.SmsSignalInfo;
import org.mobicents.protocols.ss7.map.api.smstpdu.AbsoluteTimeStamp;
import org.mobicents.protocols.ss7.map.api.smstpdu.AddressField;
import org.mobicents.protocols.ss7.map.api.smstpdu.DataCodingScheme;
import org.mobicents.protocols.ss7.map.api.smstpdu.NumberingPlanIdentification;
import org.mobicents.protocols.ss7.map.api.smstpdu.TypeOfNumber;
import org.mobicents.protocols.ss7.map.api.smstpdu.UserDataHeader;
import org.mobicents.protocols.ss7.map.api.smstpdu.UserDataHeaderElement;
import org.mobicents.protocols.ss7.map.service.sms.SmsSignalInfoImpl;
import org.mobicents.protocols.ss7.map.smstpdu.AbsoluteTimeStampImpl;
import org.mobicents.protocols.ss7.map.smstpdu.AddressFieldImpl;
import org.mobicents.protocols.ss7.map.smstpdu.DataCodingSchemeImpl;
import org.mobicents.protocols.ss7.map.smstpdu.SmsDeliverTpduImpl;
import org.mobicents.protocols.ss7.map.smstpdu.UserDataHeaderImpl;
import org.mobicents.protocols.ss7.map.smstpdu.UserDataImpl;
import org.mobicents.protocols.ss7.sccp.parameter.GT0100;
import org.mobicents.protocols.ss7.sccp.parameter.SccpAddress;
import org.mobicents.protocols.ss7.tcap.asn.ApplicationContextName;
import org.mobicents.slee.SbbLocalObjectExt;
import org.mobicents.slee.resource.map.events.DialogDelimiter;
import org.mobicents.slee.resource.map.events.DialogProviderAbort;
import org.mobicents.slee.resource.map.events.DialogReject;
import org.mobicents.slee.resource.map.events.DialogTimeout;
import org.mobicents.slee.resource.map.events.DialogUserAbort;
import org.mobicents.slee.resource.map.events.ErrorComponent;
import org.mobicents.slee.resource.map.events.RejectComponent;
import org.mobicents.smsc.slee.resources.persistence.ErrorCode;
import org.mobicents.smsc.slee.resources.persistence.MessageUtil;
import org.mobicents.smsc.slee.resources.persistence.PersistenceException;
import org.mobicents.smsc.slee.resources.persistence.PersistenceRAInterface;
import org.mobicents.smsc.slee.resources.persistence.Sms;
import org.mobicents.smsc.slee.resources.persistence.SmsSet;
import org.mobicents.smsc.slee.resources.persistence.SmsSubmitData;
import org.mobicents.smsc.slee.resources.persistence.SmscProcessingException;


import com.cloudhopper.smpp.SmppConstants;
import com.cloudhopper.smpp.tlv.Tlv;
import com.cloudhopper.smpp.tlv.TlvConvertException;

/**
 * 
 * @author amit bhayani
 * @author sergey vetyutnev
 * 
 */
public abstract class MtSbb extends MtCommonSbb implements MtForwardSmsInterface {

	private static final String className = "MtSbb";

	public MtSbb() {
		super(className);
	}

	/**
	 * Components Events override from MtCommonSbb that we care
	 */

	@Override
	public void onErrorComponent(ErrorComponent event, ActivityContextInterface aci) {
		super.onErrorComponent(event, aci);

		MAPErrorMessage mapErrorMessage = event.getMAPErrorMessage();
		if (mapErrorMessage.isEmSubscriberBusyForMtSms()) {
			MAPErrorMessageSubscriberBusyForMtSms subscriberBusyForMtSms = mapErrorMessage.getEmSubscriberBusyForMtSms();
			this.onDeliveryError(ErrorAction.subscriberBusy, ErrorCode.USER_BUSY, "Error subscriberBusyForMtSms after MtForwardSM Request: "
					+ subscriberBusyForMtSms.toString());
		} else if (mapErrorMessage.isEmAbsentSubscriber()) {
			MAPErrorMessageAbsentSubscriber absentSubscriber = mapErrorMessage.getEmAbsentSubscriber();
			this.onDeliveryError(ErrorAction.mobileNotReachableFlag, ErrorCode.ABSENT_SUBSCRIBER, "Error absentSubscriber after MtForwardSM Request: "
					+ absentSubscriber.toString());
		} else if (mapErrorMessage.isEmAbsentSubscriberSM()) {
			MAPErrorMessageAbsentSubscriberSM absentSubscriber = mapErrorMessage.getEmAbsentSubscriberSM();
			this.onDeliveryError(ErrorAction.mobileNotReachableFlag, ErrorCode.ABSENT_SUBSCRIBER, "Error absentSubscriberSM after MtForwardSM Request: "
					+ absentSubscriber.toString());
		} else if (mapErrorMessage.isEmSMDeliveryFailure()) {
			MAPErrorMessageSMDeliveryFailure smDeliveryFailure = mapErrorMessage.getEmSMDeliveryFailure();
			if (smDeliveryFailure.getSMEnumeratedDeliveryFailureCause() == SMEnumeratedDeliveryFailureCause.memoryCapacityExceeded) {
				this.onDeliveryError(ErrorAction.memoryCapacityExceededFlag, ErrorCode.MESSAGE_QUEUE_FULL, "Error smDeliveryFailure after MtForwardSM Request: "
						+ smDeliveryFailure.toString());
			} else {
				this.onDeliveryError(ErrorAction.permanentFailure, ErrorCode.SENDING_SM_FAILED, "Error smDeliveryFailure after MtForwardSM Request: "
						+ smDeliveryFailure.toString());
			}
		} else if (mapErrorMessage.isEmSystemFailure()) {
			// TODO: may be it is not a permanent case ???
			MAPErrorMessageSystemFailure systemFailure = mapErrorMessage.getEmSystemFailure();
			this.onDeliveryError(ErrorAction.permanentFailure, ErrorCode.SYSTEM_FAILURE,
					"Error systemFailure after MtForwardSM Request: " + systemFailure.toString());
		} else if (mapErrorMessage.isEmFacilityNotSup()) {
			MAPErrorMessageFacilityNotSup facilityNotSup = mapErrorMessage.getEmFacilityNotSup();
			this.onDeliveryError(ErrorAction.permanentFailure, ErrorCode.SYSTEM_FAILURE,
					"Error facilityNotSup after MtForwardSM Request: " + facilityNotSup.toString());
		} else if (mapErrorMessage.isEmExtensionContainer()) {
			MAPErrorMessageExtensionContainer extensionContainer = mapErrorMessage.getEmExtensionContainer();
			switch ((int) (long) extensionContainer.getErrorCode()) {
			case MAPErrorCode.dataMissing:
				this.onDeliveryError(ErrorAction.permanentFailure, ErrorCode.DATA_MISSING, "Error after MtForwardSM Request: " + extensionContainer.toString());
				break;
			case MAPErrorCode.unexpectedDataValue:
				this.onDeliveryError(ErrorAction.permanentFailure, ErrorCode.UNEXPECTED_DATA, "Error after MtForwardSM Request: " + extensionContainer.toString());
				break;
			case MAPErrorCode.facilityNotSupported:
				this.onDeliveryError(ErrorAction.permanentFailure, ErrorCode.FACILITY_NOT_SUPPORTED, "Error after MtForwardSM Request: " + extensionContainer.toString());
				break;
			case MAPErrorCode.unidentifiedSubscriber:
				this.onDeliveryError(ErrorAction.permanentFailure, ErrorCode.UNDEFINED_SUBSCRIBER, "Error after MtForwardSM Request: " + extensionContainer.toString());
				break;
			case MAPErrorCode.illegalSubscriber:
				this.onDeliveryError(ErrorAction.permanentFailure, ErrorCode.ILLEGAL_SUBSCRIBER, "Error after MtForwardSM Request: " + extensionContainer.toString());
				break;
			case MAPErrorCode.illegalEquipment:
				this.onDeliveryError(ErrorAction.permanentFailure, ErrorCode.ILLEGAL_EQUIPMENT, "Error after MtForwardSM Request: " + extensionContainer.toString());
				break;
			default:
				this.onDeliveryError(ErrorAction.permanentFailure, ErrorCode.SYSTEM_FAILURE, "Error after MtForwardSM Request: " + extensionContainer.toString());
				break;
			}
		} else {
			this.onDeliveryError(ErrorAction.permanentFailure, ErrorCode.SYSTEM_FAILURE, "Error after MtForwardSM Request");
		}
	}

	@Override
	public void onRejectComponent(RejectComponent event, ActivityContextInterface aci) {
		super.onRejectComponent(event, aci);

		String reason = this.getRejectComponentReason(event);

		this.onDeliveryError(ErrorAction.permanentFailure, ErrorCode.HLR_REJECT_AFTER_ROUTING_INFO,
				"onRejectComponent after MtForwardSM Request: " + reason != null ? reason.toString() : "");
	}

	/**
	 * Dialog Events override from MtCommonSbb that we care
	 */

	@Override
	public void onDialogReject(DialogReject evt, ActivityContextInterface aci) {

		MAPRefuseReason mapRefuseReason = evt.getRefuseReason();

		if (mapRefuseReason == MAPRefuseReason.PotentialVersionIncompatibility
				&& evt.getMAPDialog().getApplicationContext().getApplicationContextVersion() != MAPApplicationContextVersion.version1) {
			if (logger.isWarningEnabled()) {
				this.logger.warning("Rx : Mt onDialogReject / PotentialVersionIncompatibility=" + evt);
			}
			// possible a peer supports only MAP V1
			// Now send new SRI with supported ACN (MAP V1)
			try {
				this.sendMtSms(this.getMtFoSMSMAPApplicationContext(MAPApplicationContextVersion.version1),
						MessageProcessingState.resendAfterMapProtocolNegotiation);
				return;
			} catch (SmscProcessingException e) {
				String reason = "SmscPocessingException when invoking sendMtSms() from onDialogReject()-resendAfterMapProtocolNegotiation: " + e.toString();
				this.logger.severe(reason, e);
				ErrorCode smStatus = ErrorCode.SC_SYSTEM_ERROR;
				try {
					smStatus = ErrorCode.fromInt(e.getSmppErrorCode());
				} catch (IllegalArgumentException e1) {
				}
				this.onDeliveryError(ErrorAction.permanentFailure, smStatus, reason);
				return;
			} catch (Throwable e) {
				String reason = "Exception when invoking sendMtSms() from onDialogReject()-resendAfterMapProtocolNegotiation: " + e.toString();
				this.logger.severe(reason, e);
				ErrorCode smStatus = ErrorCode.SC_SYSTEM_ERROR;
				this.onDeliveryError(ErrorAction.permanentFailure, smStatus, reason);
				return;
			}
		}

		// If ACN not supported, lets use the new one suggested
		if (mapRefuseReason == MAPRefuseReason.ApplicationContextNotSupported) {
			if (logger.isWarningEnabled()) {
				this.logger.warning("Rx : Mt onDialogReject / ApplicationContextNotSupported=" + evt);
			}

			// Now send new SRI with supported ACN
			ApplicationContextName tcapApplicationContextName = evt.getAlternativeApplicationContext();
			MAPApplicationContext supportedMAPApplicationContext = MAPApplicationContext.getInstance(tcapApplicationContextName.getOid());

			try {
				this.sendMtSms(this.getMtFoSMSMAPApplicationContext(supportedMAPApplicationContext.getApplicationContextVersion()),
						MessageProcessingState.resendAfterMapProtocolNegotiation);
				return;
			} catch (SmscProcessingException e) {
				String reason = "SmscPocessingException when invoking sendMtSms() from onDialogReject()-resendAfterMapProtocolNegotiation: " + e.toString();
				this.logger.severe(reason, e);
				ErrorCode smStatus = ErrorCode.SC_SYSTEM_ERROR;
				try {
					smStatus = ErrorCode.fromInt(e.getSmppErrorCode());
				} catch (IllegalArgumentException e1) {
				}
				this.onDeliveryError(ErrorAction.permanentFailure, smStatus, reason);
				return;
			} catch (Throwable e) {
				String reason = "Exception when invoking sendMtSms() from onDialogReject()-resendAfterMapProtocolNegotiation: " + e.toString();
				this.logger.severe(reason, e);
				ErrorCode smStatus = ErrorCode.SC_SYSTEM_ERROR;
				this.onDeliveryError(ErrorAction.permanentFailure, smStatus, reason);
				return;
			}
		}

		super.onDialogReject(evt, aci);

		this.onDeliveryError(ErrorAction.permanentFailure, ErrorCode.MSC_REFUSES_SM,
				"onDialogReject after MT Request: " + mapRefuseReason != null ? mapRefuseReason.toString() : "");

	}

	@Override
	public void onDialogProviderAbort(DialogProviderAbort evt, ActivityContextInterface aci) {
		super.onDialogProviderAbort(evt, aci);

		MAPAbortProviderReason abortProviderReason = evt.getAbortProviderReason();

		this.onDeliveryError(ErrorAction.permanentFailure, ErrorCode.MSC_REFUSES_SM,
				"onDialogProviderAbort after MtForwardSM Request: " + abortProviderReason != null ? abortProviderReason.toString() : "");
	}

	@Override
	public void onDialogUserAbort(DialogUserAbort evt, ActivityContextInterface aci) {
		super.onDialogUserAbort(evt, aci);

		String reason = getUserAbortReason(evt);

		this.onDeliveryError(ErrorAction.permanentFailure, ErrorCode.MSC_REFUSES_SM,
				"onDialogUserAbort after MtForwardSM Request: " + reason != null ? reason.toString() : "");
	}

	@Override
	public void onDialogTimeout(DialogTimeout evt, ActivityContextInterface aci) {
		// TODO: may be it is not a permanent failure case ???

		super.onDialogTimeout(evt, aci);

		this.onDeliveryError(ErrorAction.temporaryFailure, ErrorCode.MSC_REFUSES_SM, "onDialogTimeout after MtForwardSM Request");
	}

	@Override
	public void onDialogDelimiter(DialogDelimiter evt, ActivityContextInterface aci) {
		super.onDialogDelimiter(evt, aci);

		if (this.getTcEmptySent() != 0) {
			// Empty TC-BEGIN has been sent
			// We are sending MtForwardSM
			this.setTcEmptySent(0);

			SmsSignalInfo[] segments = this.getSegments();
			int messageSegmentNumber = this.getMessageSegmentNumber();
			if (messageSegmentNumber >= 0 && segments != null && messageSegmentNumber < segments.length) {
				SmsSignalInfo si = segments[messageSegmentNumber];
				if (si != null) {
					try {
						MAPDialogSms mapDialogSms = (MAPDialogSms) evt.getMAPDialog();
						SM_RP_DA sm_RP_DA = this.getSmRpDa();
						SM_RP_OA sm_RP_OA = this.getSmRpOa();

						boolean moreMessagesToSend = false;
						if (messageSegmentNumber < segments.length - 1) {
							moreMessagesToSend = true;
						}
						if (this.doGetCurrentMsgNum() < this.doGetSmsSubmitData().getSmsSet().getSmsCount() - 1) {
							moreMessagesToSend = true;
						}

						switch (mapDialogSms.getApplicationContext().getApplicationContextVersion()) {
						case version3:
							mapDialogSms.addMtForwardShortMessageRequest(sm_RP_DA, sm_RP_OA, si, moreMessagesToSend, null);
							if (this.logger.isInfoEnabled()) {
								this.logger.info("\nSending: MtForwardShortMessageRequest: sm_RP_DA=" + sm_RP_DA + ", sm_RP_OA=" + sm_RP_OA + ", si=" + si
										+ ", moreMessagesToSend=" + moreMessagesToSend);
							}
							break;
						case version2:
						case version1:
							mapDialogSms.addForwardShortMessageRequest(sm_RP_DA, sm_RP_OA, si, moreMessagesToSend);
							if (this.logger.isInfoEnabled()) {
								this.logger.info("\nSending: ForwardShortMessageRequest: sm_RP_DA=" + sm_RP_DA + ", sm_RP_OA=" + sm_RP_OA + ", si=" + si
										+ ", moreMessagesToSend=" + moreMessagesToSend);
							}
							break;
						default:
							break;
						}

						mapDialogSms.send();
					} catch (MAPException e) {
						logger.severe("Error while trying to send MtForwardShortMessageRequest", e);
					}
				}
			}
		}
	}

	/**
	 * SMS Event Handlers
	 */

	/**
	 * Received MT SMS. This is error we should never receive this
	 * 
	 * @param evt
	 * @param aci
	 */
	public void onForwardShortMessageRequest(ForwardShortMessageRequest evt, ActivityContextInterface aci) {
		this.logger.severe("\nReceived FORWARD_SHORT_MESSAGE_REQUEST = " + evt);
	}

	/**
	 * Received ACK for MT Forward SMS sent earlier
	 * 
	 * @param evt
	 * @param aci
	 */
	public void onForwardShortMessageResponse(ForwardShortMessageResponse evt, ActivityContextInterface aci) {
		if (this.logger.isInfoEnabled()) {
			this.logger.info("\nReceived FORWARD_SHORT_MESSAGE_RESPONSE = " + evt);
		}
		this.handleSmsResponse(evt.getMAPDialog(), aci);
	}

	/**
	 * Received MT SMS. This is error we should never receive this
	 * 
	 * @param evt
	 * @param aci
	 */
	public void onMtForwardShortMessageRequest(MtForwardShortMessageRequest evt, ActivityContextInterface aci) {
		this.logger.severe("\nReceived MT_FORWARD_SHORT_MESSAGE_REQUEST = " + evt);
	}

	/**
	 * Received ACK for MT Forward SMS sent earlier
	 * 
	 * @param evt
	 * @param aci
	 */
	public void onMtForwardShortMessageResponse(MtForwardShortMessageResponse evt, ActivityContextInterface aci) {
		if (this.logger.isInfoEnabled()) {
			this.logger.info("\nReceived MT_FORWARD_SHORT_MESSAGE_RESPONSE = " + evt);
		}

		this.handleSmsResponse(evt.getMAPDialog(), aci);
	}

	/**
	 * SBB Local Object Methods
	 * 
	 */

	@Override
	public void setupMtForwardShortMessageRequest(ISDNAddressString networkNode, IMSI imsi, LMSI lmsi) {
		if (this.logger.isInfoEnabled()) {
			this.logger.info("\nmperforming setupMtForwardShortMessageRequest ISDNAddressString= " + networkNode);
		}

		SccpAddress networkNodeSccpAddress = this.getMSCSccpAddress(networkNode);

		SM_RP_DA sm_RP_DA = this.mapParameterFactory.createSM_RP_DA(imsi);
		SM_RP_OA sm_RP_OA = this.mapParameterFactory.createSM_RP_OA_ServiceCentreAddressOA(this.getServiceCenterAddressString());

		this.setNetworkNode(networkNodeSccpAddress);
		this.setSmRpDa(sm_RP_DA);
		this.setSmRpOa(sm_RP_OA);

		try {
			this.sendMtSms(this.getMtFoSMSMAPApplicationContext(this.maxMAPApplicationContextVersion), MessageProcessingState.firstMessageSending);
		} catch (SmscProcessingException e) {
			String reason = "SmscPocessingException when invoking sendMtSms() from setupMtForwardShortMessageRequest()-firstMessageSending: " + e.toString();
			this.logger.severe(reason, e);
			ErrorCode smStatus = ErrorCode.SC_SYSTEM_ERROR;
			try {
				smStatus = ErrorCode.fromInt(e.getSmppErrorCode());
			} catch (IllegalArgumentException e1) {
			}
			this.onDeliveryError(ErrorAction.permanentFailure, smStatus, reason);
		} catch (Throwable e) {
			String reason = "Exception when invoking sendMtSms() from setupMtForwardShortMessageRequest()-firstMessageSending: " + e.toString();
			this.logger.severe(reason, e);
			ErrorCode smStatus = ErrorCode.SC_SYSTEM_ERROR;
			this.onDeliveryError(ErrorAction.permanentFailure, smStatus, reason);
		}
	}

	public void setupReportSMDeliveryStatusRequest(String destinationAddress, int ton, int npi, SMDeliveryOutcome sMDeliveryOutcome, String targetId) {

		SbbLocalObjectExt sbbLocalObject = this.sbbContext.getSbbLocalObject().getParent();
		SriSbbLocalObject sriSbb = (SriSbbLocalObject) sbbLocalObject;
		sriSbb.setupReportSMDeliveryStatusRequest(destinationAddress, ton, npi, sMDeliveryOutcome, targetId);
	}

	/**
	 * CMPs
	 */
	public abstract void setSmsSubmitData(SmsSubmitData smsDeliveryData);

	public abstract SmsSubmitData getSmsSubmitData();

	public abstract void setCurrentMsgNum(int currentMsgNum);

	public abstract int getCurrentMsgNum();

	public abstract void setInformServiceCenterContainer(InformServiceCenterContainer informServiceCenterContainer);

	public abstract InformServiceCenterContainer getInformServiceCenterContainer();

	public abstract void setTcEmptySent(int tcEmptySent);

	public abstract int getTcEmptySent();



	public void doSetSmsSubmitData(SmsSubmitData smsDeliveryData) {
		this.setSmsSubmitData(smsDeliveryData);
	}

	public SmsSubmitData doGetSmsSubmitData() {
		return this.getSmsSubmitData();
	}

	public void doSetCurrentMsgNum(int currentMsgNum) {
		this.setCurrentMsgNum(currentMsgNum);
	}

	public int doGetCurrentMsgNum() {
		return this.getCurrentMsgNum();
	}

	public void doSetInformServiceCenterContainer(InformServiceCenterContainer informServiceCenterContainer) {
		this.setInformServiceCenterContainer(informServiceCenterContainer);
	}
										
	public InformServiceCenterContainer doGetInformServiceCenterContainer() {
		return this.getInformServiceCenterContainer();
	}

	/**
	 * Set the ISDNAddressString of network node where Mt SMS is to be submitted
	 * 
	 * @param networkNode
	 */
	public abstract void setNetworkNode(SccpAddress sccpAddress);

	public abstract SccpAddress getNetworkNode(); 

	/**
	 * Set the counter as which SMS is sent. Max sending can be equal to
	 * messageSegmentCount
	 * 
	 * @param mesageSegmentNumber
	 */
	public abstract void setMessageSegmentNumber(int mesageSegmentNumber);

	public abstract int getMessageSegmentNumber();

	public abstract void setSegments(SmsSignalInfo[] segments);

	public abstract SmsSignalInfo[] getSegments();

	/**
	 * Destination Address
	 * 
	 * @param sm_rp_da
	 */
	public abstract void setSmRpDa(SM_RP_DA sm_rp_da);

	public abstract SM_RP_DA getSmRpDa();

	/**
	 * Originating Address
	 * 
	 * @param sm_rp_oa
	 */
	public abstract void setSmRpOa(SM_RP_OA sm_rp_oa);

	public abstract SM_RP_OA getSmRpOa();

	/**
	 * Private Methods
	 */

	private void handleSmsResponse(MAPDialogSms mapDialogSms, ActivityContextInterface aci) {

		// checking if there are yet message segments
		int messageSegmentNumber = this.getMessageSegmentNumber();
		SmsSignalInfo[] segments = this.getSegments();
		if (segments != null && messageSegmentNumber < segments.length - 1) {
			// we have more message parts to be sent yet
			messageSegmentNumber++;
			this.setMessageSegmentNumber(messageSegmentNumber);
			try {
				this.sendMtSms(mapDialogSms.getApplicationContext(), MessageProcessingState.nextSegmentSending);
				return;
			} catch (SmscProcessingException e) {
				this.logger.severe("SmscPocessingException when invoking sendMtSms() from handleSmsResponse()-nextSegmentSending: " + e.toString(), e);
			}
		}

		// current message is sent
		// pushing current message into an archive
		SmsSubmitData smsDeliveryData = this.doGetSmsSubmitData();
		if (smsDeliveryData == null) {
			if (this.logger.isInfoEnabled())
				this.logger.info("SmsDeliveryData CMP missed");
			
			return;
		}
		SmsSet smsSet = smsDeliveryData.getSmsSet();
		if (smsSet == null) {
			this.logger.severe("In SmsDeliveryData CMP smsSet is missed");
			return;
		}
		PersistenceRAInterface pers = this.getStore();
		
		int currentMsgNum = this.doGetCurrentMsgNum();
		Sms sms = smsSet.getSms(currentMsgNum);
		Date deliveryDate = new Date();
		try {
			generateCdr(sms, CdrGenerator.CDR_SUCCESS, MtCommonSbb.CDR_SUCCESS_NO_REASON);
			pers.archiveDeliveredSms(sms, deliveryDate);
		} catch (PersistenceException e1) {
			this.logger.severe("PersistenceException when archiveDeliveredSms() in handleSmsResponse(): " + e1.getMessage(), e1);
			// we do not "return" here because even if storing into archive database is failed 
			// we will continue delivering process
		}

		// now we are trying to sent other messages
		if (currentMsgNum < smsSet.getSmsCount() - 1) {
			// there are more messages to send in cache
			currentMsgNum++;
			this.doSetCurrentMsgNum(currentMsgNum);
			sms = smsSet.getSms(currentMsgNum);
			if (sms != null) {
				this.startMessageDelivery(sms);
			}

			try {
				this.sendMtSms(mapDialogSms.getApplicationContext(), MessageProcessingState.firstMessageSending);
				return;
			} catch (SmscProcessingException e) {
				this.logger.severe("SmscPocessingException when invoking sendMtSms() from handleSmsResponse()-firstMessageSending: " + e.toString(), e);
			}
		}

		// no more messages are in cache now - lets check if there are more messages in a database
		try {
			pers.fetchSchedulableSms(smsSet, true);
		} catch (PersistenceException e1) {
			this.logger.severe("PersistenceException when invoking fetchSchedulableSms(smsSet) from handleSmsResponse(): " + e1.toString(), e1);
		}
		if (smsSet.getSmsCount() > 0) {
			// there are more messages in a database - start delivering of those messages
			currentMsgNum = 0;
			this.doSetCurrentMsgNum(currentMsgNum);

			try {
				this.sendMtSms(mapDialogSms.getApplicationContext(), MessageProcessingState.firstMessageSending);
				return;
			} catch (SmscProcessingException e) {
				this.logger.severe("SmscPocessingException when invoking sendMtSms() from handleSmsResponse()-firstMessageSending: " + e.toString(), e);
			}
		}

		// no more messages to send - remove smsSet
		this.freeSmsSetSucceded(smsSet, pers);

		InformServiceCenterContainer informServiceCenterContainer = this.getInformServiceCenterContainer();
		if (informServiceCenterContainer != null && informServiceCenterContainer.getMwStatus() != null
				&& informServiceCenterContainer.getMwStatus().getScAddressNotIncluded() == false
				&& mapDialogSms.getApplicationContext().getApplicationContextVersion() != MAPApplicationContextVersion.version1) {
			// sending a report to HLR of a success delivery
			this.setupReportSMDeliveryStatusRequest(smsSet.getDestAddr(), smsSet.getDestAddrTon(), smsSet.getDestAddrNpi(),
					SMDeliveryOutcome.successfulTransfer, smsSet.getTargetId());
		}
	}

	private ArrayList<byte[]> sliceMessage(byte[] shortMessage, DataCodingScheme dataCodingScheme) {

		// TODO: if we use extended character tables we will need more sophisticated algorithm
		// for calculating real message length (for GSM7)

		int lenSolid = MessageUtil.getMaxSolidMessageBytesLength(dataCodingScheme);
		int lenSegmented = MessageUtil.getMaxSegmentedMessageBytesLength(dataCodingScheme);

		ArrayList<byte[]> res = new ArrayList<byte[]>();
		if (shortMessage.length <= lenSolid) {
			res.add(shortMessage);
		} else {
			int segmCnt = (shortMessage.length - 1) / lenSegmented + 1;
			for (int i1 = 0; i1 < segmCnt; i1++) {
				byte[] buf;
				if (i1 == segmCnt - 1) {
					buf = new byte[shortMessage.length - i1 * lenSegmented];
				} else {
					buf = new byte[lenSegmented];
				}
				System.arraycopy(shortMessage, i1 * lenSegmented, buf, 0, buf.length);
				res.add(buf);
			}
		}

		return res;
	}

	private SmsSignalInfo createSignalInfo(Sms sms, byte[] shortMessage, boolean moreMessagesToSend, int messageReferenceNumber, int messageSegmentCount,
			int messageSegmentNumber, DataCodingScheme dataCodingScheme, boolean udhExists) throws MAPException {

		UserDataImpl ud;
		byte[] textPart = shortMessage;
		UserDataHeader userDataHeader = null;
		if (udhExists && shortMessage.length > 2) {
			// UDH exists
			int udhLen = (shortMessage[0] & 0xFF) + 1;
			if (udhLen <= shortMessage.length) {
				textPart = new byte[shortMessage.length - udhLen];
				byte[] udhData = new byte[udhLen];
				System.arraycopy(shortMessage, udhLen, textPart, 0, textPart.length);
				System.arraycopy(shortMessage, 0, udhData, 0, udhLen);
				userDataHeader = new UserDataHeaderImpl(udhData);
			}
		}

		String msg = "";
		switch (sms.getDataCoding()) {
		case 0:
			msg = new String(textPart);
			break;
		case 8:
			Charset ucs2Charset = Charset.forName("UTF-16BE");
			ByteBuffer bb = ByteBuffer.wrap(textPart);
			CharBuffer bf = ucs2Charset.decode(bb);
			msg = bf.toString();
			break;
		default:
			// we do not support this yet
			break;
		}

		if (messageSegmentCount > 1) {
			userDataHeader = this.mapSmsTpduParameterFactory.createUserDataHeader();
			UserDataHeaderElement concatenatedShortMessagesIdentifier = this.mapSmsTpduParameterFactory.createConcatenatedShortMessagesIdentifier(
					messageReferenceNumber > 255, messageReferenceNumber, messageSegmentCount, messageSegmentNumber);
			userDataHeader.addInformationElement(concatenatedShortMessagesIdentifier);
		}

		ud = new UserDataImpl(msg, dataCodingScheme, userDataHeader, null);

		Date submitDate = sms.getSubmitDate();

		// TODO : TimeZone should be configurable
		AbsoluteTimeStamp serviceCentreTimeStamp = new AbsoluteTimeStampImpl((submitDate.getYear() % 100), (submitDate.getMonth() + 1), submitDate.getDate(),
				submitDate.getHours(), submitDate.getMinutes(), submitDate.getSeconds(), (submitDate.getTimezoneOffset() / 15));

		SmsDeliverTpduImpl smsDeliverTpduImpl = new SmsDeliverTpduImpl(moreMessagesToSend, false,
				((sms.getEsmClass() & SmppConstants.ESM_CLASS_REPLY_PATH_MASK) != 0), false, this.getSmsTpduOriginatingAddress(sms.getSourceAddrTon(),
						sms.getSourceAddrNpi(), sms.getSourceAddr()), this.mapSmsTpduParameterFactory.createProtocolIdentifier(sms.getProtocolId()),
				serviceCentreTimeStamp, ud);

		SmsSignalInfoImpl smsSignalInfo = new SmsSignalInfoImpl(smsDeliverTpduImpl, null);

		return smsSignalInfo;
	}

	private void sendMtSms(MAPApplicationContext mapApplicationContext, MessageProcessingState messageProcessingState) throws SmscProcessingException {
		SmsSubmitData smsDeliveryData = this.doGetSmsSubmitData();
		if (smsDeliveryData == null) {
			throw new SmscProcessingException("SmsDeliveryData CMP missed",-1,-1,null);
		}
		SmsSet smsSet = smsDeliveryData.getSmsSet();
		if (smsSet == null) {
			throw new SmscProcessingException("SmsDeliveryData-SmsSet CMP missed",-1,-1,null);
		}
		int msgNum = this.doGetCurrentMsgNum();
		Sms sms = smsSet.getSms(msgNum);

		if (sms == null) {
			throw new SmscProcessingException("SmsDeliveryData-sms is missed in CMP",-1,-1,null);
		}
		boolean moreMessagesToSend = false;
		if (msgNum < smsSet.getSmsCount() - 1) {
			moreMessagesToSend = true;
		}

		MAPDialogSms mapDialogSms = null;
		try {
			mapDialogSms = this.mapProvider.getMAPServiceSms().createNewDialog(mapApplicationContext, this.getServiceCenterSccpAddress(), null,
					this.getNetworkNode(), null);

			ActivityContextInterface mtFOSmsDialogACI = this.mapAcif.getActivityContextInterface(mapDialogSms);
			mtFOSmsDialogACI.attach(this.sbbContext.getSbbLocalObject());

			SM_RP_DA sm_RP_DA = this.getSmRpDa();
			SM_RP_OA sm_RP_OA = this.getSmRpOa();

			SmsSignalInfo smsSignalInfo;
			if (messageProcessingState == MessageProcessingState.firstMessageSending) {

				DataCodingScheme dataCodingScheme = new DataCodingSchemeImpl(sms.getDataCoding());
				Tlv sarMsgRefNum = sms.getTlvSet().getOptionalParameter(SmppConstants.TAG_SAR_MSG_REF_NUM);
				Tlv sarTotalSegments = sms.getTlvSet().getOptionalParameter(SmppConstants.TAG_SAR_TOTAL_SEGMENTS);
				Tlv sarSegmentSeqnum = sms.getTlvSet().getOptionalParameter(SmppConstants.TAG_SAR_SEGMENT_SEQNUM);
				SmsSignalInfo[] segments;
				if ((sms.getEsmClass() & SmppConstants.ESM_CLASS_UDHI_MASK) != 0) {
					// message already contains UDH - we can not slice it
					segments = new SmsSignalInfo[1];
					segments[0] = this.createSignalInfo(sms, sms.getShortMessage(), moreMessagesToSend, 0, 1, 1, dataCodingScheme, true);
				} else if (sarMsgRefNum != null && sarTotalSegments != null && sarSegmentSeqnum != null) {
					// we have tlv's that define message count/number/reference  
					int messageSegmentCount = sarTotalSegments.getValueAsUnsignedByte();
					int messageSegmentNumber = sarSegmentSeqnum.getValueAsUnsignedByte();
					int messageReferenceNumber = sarMsgRefNum.getValueAsUnsignedShort();
					segments = new SmsSignalInfo[1];
					segments[0] = this.createSignalInfo(sms, sms.getShortMessage(), moreMessagesToSend, messageReferenceNumber, messageSegmentCount,
							messageSegmentNumber, dataCodingScheme, false);
				} else {
					// possible a big message and segmentation
					ArrayList<byte[]> segmentsByte;
					segmentsByte = this.sliceMessage(sms.getShortMessage(), dataCodingScheme);
					segments = new SmsSignalInfo[segmentsByte.size()];

					// TODO messageReferenceNumber should be generated
					int messageReferenceNumber = msgNum + 1;

					for (int i1 = 0; i1 < segmentsByte.size(); i1++) {
						segments[i1] = this.createSignalInfo(sms, segmentsByte.get(i1), (i1 < segmentsByte.size() - 1 ? true : moreMessagesToSend),
								messageReferenceNumber, segmentsByte.size(), i1 + 1, dataCodingScheme, false);
					}
				}

				this.setSegments(segments);
				smsSignalInfo = segments[0];
				this.setMessageSegmentNumber(0);
				if (segments.length > 1)
					moreMessagesToSend = true;
			} else {
				int messageSegmentNumber = this.getMessageSegmentNumber();
				SmsSignalInfo[] segments = this.getSegments();
				smsSignalInfo = segments[messageSegmentNumber];
				if (messageSegmentNumber < segments.length - 1)
					moreMessagesToSend = true;
			}

			long invokeId = 0;
			switch (mapDialogSms.getApplicationContext().getApplicationContextVersion()) {
			case version3:
				invokeId = mapDialogSms.addMtForwardShortMessageRequest(sm_RP_DA, sm_RP_OA, smsSignalInfo, moreMessagesToSend, null);
				if (this.logger.isInfoEnabled()) {
					this.logger.info("\nSending: MtForwardShortMessageRequest: sm_RP_DA=" + sm_RP_DA + ", sm_RP_OA=" + sm_RP_OA + ", si=" + smsSignalInfo
							+ ", moreMessagesToSend=" + moreMessagesToSend);
				}
				break;
			case version2:
			case version1:
				invokeId = mapDialogSms.addForwardShortMessageRequest(sm_RP_DA, sm_RP_OA, smsSignalInfo, moreMessagesToSend);
				if (this.logger.isInfoEnabled()) {
					this.logger.info("\nSending: ForwardShortMessageRequest: sm_RP_DA=" + sm_RP_DA + ", sm_RP_OA=" + sm_RP_OA + ", si=" + smsSignalInfo
							+ ", moreMessagesToSend=" + moreMessagesToSend);
				}
				break;
			default:
				break;
			}

			if (mapDialogSms.getApplicationContext().getApplicationContextVersion() != MAPApplicationContextVersion.version1
					&& mapDialogSms.getMessageUserDataLengthOnSend() >= mapDialogSms.getMaxUserDataLength()) {
				mapDialogSms.cancelInvocation(invokeId);
				this.setTcEmptySent(1);
			} else {
				this.setTcEmptySent(0);
			}

			mapDialogSms.send();

		} catch (MAPException e) {
			if (mapDialogSms != null)
				mapDialogSms.release();
			throw new SmscProcessingException("MAPException when sending MtForwardSM", -1, -1, null, e);
		} catch (TlvConvertException e) {
			if (mapDialogSms != null)
				mapDialogSms.release();
			throw new SmscProcessingException("TlvConvertException when sending MtForwardSM", -1, -1, null, e);
		}
	}


	private MAPApplicationContext getMtFoSMSMAPApplicationContext(MAPApplicationContextVersion mapApplicationContextVersion) {

		if (mapApplicationContextVersion == MAPApplicationContextVersion.version1) {
			return MAPApplicationContext.getInstance(MAPApplicationContextName.shortMsgMORelayContext, mapApplicationContextVersion);
		} else {
			return MAPApplicationContext.getInstance(MAPApplicationContextName.shortMsgMTRelayContext, mapApplicationContextVersion);
		}
	}

	private SccpAddress getMSCSccpAddress(ISDNAddressString networkNodeNumber) {
		NumberingPlan np = MessageUtil.getSccpNumberingPlan(networkNodeNumber.getNumberingPlan().getIndicator());
		NatureOfAddress na = MessageUtil.getSccpNatureOfAddress(networkNodeNumber.getAddressNature().getIndicator());
		GT0100 gt = new GT0100(0, np, na, networkNodeNumber.getAddress());
		return new SccpAddress(RoutingIndicator.ROUTING_BASED_ON_GLOBAL_TITLE, 0, gt, smscPropertiesManagement.getMscSsn());
	}

	private AddressField getSmsTpduOriginatingAddress(int ton, int npi, String address) {
		return new AddressFieldImpl(TypeOfNumber.getInstance(ton), NumberingPlanIdentification.getInstance(npi),
				address);
	}

	public enum MessageProcessingState {
		firstMessageSending,
		nextSegmentSending,
		resendAfterMapProtocolNegotiation,
	}
}
