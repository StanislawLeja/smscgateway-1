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

import java.sql.Timestamp;
import java.text.SimpleDateFormat;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.slee.ActivityContextInterface;
import javax.slee.CreateException;
import javax.slee.EventContext;
import javax.slee.RolledBackContext;
import javax.slee.Sbb;
import javax.slee.SbbContext;
import javax.slee.facilities.Tracer;
import javax.slee.nullactivity.NullActivity;

import org.mobicents.protocols.ss7.indicator.NatureOfAddress;
import org.mobicents.protocols.ss7.indicator.NumberingPlan;
import org.mobicents.protocols.ss7.indicator.RoutingIndicator;
import org.mobicents.protocols.ss7.map.api.MAPApplicationContextVersion;
import org.mobicents.protocols.ss7.map.api.MAPParameterFactory;
import org.mobicents.protocols.ss7.map.api.MAPProvider;
import org.mobicents.protocols.ss7.map.api.MAPSmsTpduParameterFactory;
import org.mobicents.protocols.ss7.map.api.dialog.MAPAbortProviderReason;
import org.mobicents.protocols.ss7.map.api.dialog.MAPRefuseReason;
import org.mobicents.protocols.ss7.map.api.dialog.MAPUserAbortChoice;
import org.mobicents.protocols.ss7.map.api.dialog.ProcedureCancellationReason;
import org.mobicents.protocols.ss7.map.api.dialog.ResourceUnavailableReason;
import org.mobicents.protocols.ss7.map.api.errors.MAPErrorCode;
import org.mobicents.protocols.ss7.map.api.errors.MAPErrorMessage;
import org.mobicents.protocols.ss7.map.api.errors.MAPErrorMessageSMDeliveryFailure;
import org.mobicents.protocols.ss7.map.api.errors.SMEnumeratedDeliveryFailureCause;
import org.mobicents.protocols.ss7.map.api.primitives.AddressNature;
import org.mobicents.protocols.ss7.map.api.primitives.AddressString;
import org.mobicents.protocols.ss7.sccp.parameter.GT0100;
import org.mobicents.protocols.ss7.sccp.parameter.SccpAddress;
import org.mobicents.protocols.ss7.tcap.asn.comp.Problem;
import org.mobicents.slee.SbbContextExt;
import org.mobicents.slee.resource.map.MAPContextInterfaceFactory;
import org.mobicents.slee.resource.map.events.DialogAccept;
import org.mobicents.slee.resource.map.events.DialogClose;
import org.mobicents.slee.resource.map.events.DialogDelimiter;
import org.mobicents.slee.resource.map.events.DialogNotice;
import org.mobicents.slee.resource.map.events.DialogProviderAbort;
import org.mobicents.slee.resource.map.events.DialogReject;
import org.mobicents.slee.resource.map.events.DialogRelease;
import org.mobicents.slee.resource.map.events.DialogRequest;
import org.mobicents.slee.resource.map.events.DialogTimeout;
import org.mobicents.slee.resource.map.events.DialogUserAbort;
import org.mobicents.slee.resource.map.events.ErrorComponent;
import org.mobicents.slee.resource.map.events.InvokeTimeout;
import org.mobicents.slee.resource.map.events.RejectComponent;
import org.mobicents.smsc.slee.services.persistence.Sms;
import org.mobicents.smsc.smpp.SmscPropertiesManagement;

import com.cloudhopper.commons.charset.CharsetUtil;
import com.cloudhopper.smpp.util.SmppUtil;

/**
 * 
 * @author amit bhayani
 * 
 */
public abstract class MtCommonSbb implements Sbb {

	private static final byte ESME_DELIVERY_ACK = 0x08;

	/**
	 * MAP Error Code from MAPErrorCode TODO : May be MAPErrorCode must be Enum?
	 */
	private static final String MAP_ERR_CODE_SYSTEM_FAILURE = "systemFailure";
	private static final String MAP_ERR_CODE_DATA_MISSING = "dataMissing";
	private static final String MAP_ERR_CODE_UNEXPECTED_DATA_VALUE = "unexpectedDataValue";
	private static final String MAP_ERR_CODE_FACILITY_NOT_SUPPORTED = "facilityNotSupported";
	private static final String MAP_ERR_CODE_INCOMPATIBLE_TERMINAL = "incompatibleTerminal";
	private static final String MAP_ERR_CODE_RESOURCE_LIMITATION = "resourceLimitation";

	private static final String MAP_ERR_CODE_SUBSCRIBER_BUSY_FOR_MTSMS = "subscriberBusyForMTSMS";
	private static final String MAP_ERR_CODE_SM_DELIVERY_FAILURE = "smDeliveryFailure_";
	private static final String MAP_ERR_CODE_MESSAGE_WAITING_LIST_FULL = "messageWaitingListFull";
	private static final String MAP_ERR_CODE_ABSENT_SUBSCRIBER_SM = "absentSubscriberSM";
	private static final String MAP_ERR_CODE_UNKNOWN = "errorComponent_unknown_";

	private static final String MAP_USER_ABORT_CHOICE_USER_SPECIFIC_REASON = "userSpecificReason";
	private static final String MAP_USER_ABORT_CHOICE_USER_RESOURCE_LIMITATION = "userResourceLimitation";
	private static final String MAP_USER_ABORT_CHOICE_UNKNOWN = "DialogUserAbort_Unknown";

	private static final String DIALOG_TIMEOUT = "dialogTimeout";

	private static final String CDR_SUCCESS_NO_REASON = "";

	private static final String DELIVERY_ACK_ID = "id:";
	private static final String DELIVERY_ACK_SUB = " sub:";
	private static final String DELIVERY_ACK_DLVRD = " dlvrd:";
	private static final String DELIVERY_ACK_SUBMIT_DATE = " submit date:";
	private static final String DELIVERY_ACK_DONE_DATE = " done date:";
	private static final String DELIVERY_ACK_STAT = " stat:";
	private static final String DELIVERY_ACK_ERR = " err:";
	private static final String DELIVERY_ACK_TEXT = " text:";

	private static final String DELIVERY_ACK_STATE_DELIVERED = "DELIVRD";
	private static final String DELIVERY_ACK_STATE_EXPIRED = "EXPIRED";
	private static final String DELIVERY_ACK_STATE_DELETED = "DELETED";
	private static final String DELIVERY_ACK_STATE_UNDELIVERABLE = "UNDELIV";
	private static final String DELIVERY_ACK_STATE_ACCEPTED = "ACCEPTD";
	private static final String DELIVERY_ACK_STATE_UNKNOWN = "UNKNOWN";
	private static final String DELIVERY_ACK_STATE_REJECTED = "REJECTD";

	protected static final SmscPropertiesManagement smscPropertiesManagement = SmscPropertiesManagement.getInstance();

	private final SimpleDateFormat DELIVERY_ACK_DATE_FORMAT = new SimpleDateFormat("yyMMddHHmm");

	private final String className;

	protected Tracer logger;
	protected SbbContextExt sbbContext;

	protected MAPContextInterfaceFactory mapAcif;
	protected MAPProvider mapProvider;
	protected MAPParameterFactory mapParameterFactory;
	protected MAPSmsTpduParameterFactory mapSmsTpduParameterFactory;

	private AddressString serviceCenterAddress;
	private SccpAddress serviceCenterSCCPAddress = null;

	protected MAPApplicationContextVersion maxMAPApplicationContextVersion = null;

	public MtCommonSbb(String className) {
		this.className = className;
	}

	/**
	 * MAP Components Events
	 */

	public void onInvokeTimeout(InvokeTimeout evt, ActivityContextInterface aci) {
		if (logger.isInfoEnabled()) {
			this.logger.info("Rx :  onInvokeTimeout" + evt);
		}
	}

	public void onErrorComponent(ErrorComponent event, ActivityContextInterface aci) {

		if (this.logger.isInfoEnabled()) {
			this.logger.info("Rx :  onErrorComponent " + event + " Dialog=" + event.getMAPDialog());
		}
		// if (mapErrorMessage.isEmAbsentSubscriberSM()) {
		// this.sendReportSMDeliveryStatusRequest(SMDeliveryOutcome.absentSubscriber);
		// }

		MAPErrorMessage mapErrorMessage = event.getMAPErrorMessage();
		String reason = this.getErrorComponentReason(mapErrorMessage);

		Sms original = this.getOriginalSmsEvent();

		if (original != null) {
			if (original.getOrigSystemId() != null) {
				this.sendFailureDeliverSmToEsms(original, reason);
			}
		}
	}

//	public void onProviderErrorComponent(ProviderErrorComponent event, ActivityContextInterface aci) {
//		this.logger.severe("Rx :  onProviderErrorComponent" + event);
//
//		MAPProviderError mapProviderError = event.getMAPProviderError();
//		SmsEvent original = this.getOriginalSmsEvent();
//
//		if (original != null) {
//			if (original.getSystemId() != null) {
//				this.sendFailureDeliverSmToEsms(original, mapProviderError.toString());
//			}
//		}
//	}

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

		Sms original = this.getOriginalSmsEvent();

		if (original != null) {
			if (original.getOrigSystemId() != null) {
				this.sendFailureDeliverSmToEsms(original, reason);
			}
		}
	}

	/**
	 * Dialog Events
	 */

	public void onDialogDelimiter(DialogDelimiter evt, ActivityContextInterface aci) {
		if (logger.isFineEnabled()) {
			this.logger.fine("Rx :  onDialogDelimiter=" + evt);
		}
	}

	public void onDialogAccept(DialogAccept evt, ActivityContextInterface aci) {
		if (logger.isFineEnabled()) {
			this.logger.fine("Rx :  onDialogAccept=" + evt);
		}
	}

	public void onDialogReject(DialogReject evt, ActivityContextInterface aci) {
		if (logger.isWarningEnabled()) {
			this.logger.warning("Rx :  onDialogReject=" + evt);
		}

		// TODO : Error condition. Take care

		MAPRefuseReason refuseReason = evt.getRefuseReason();

		Sms original = this.getOriginalSmsEvent();

		if (original != null) {
			if (original.getOrigSystemId() != null) {
				this.sendFailureDeliverSmToEsms(original, refuseReason.toString());
			}
		}
	}

	public void onDialogUserAbort(DialogUserAbort evt, ActivityContextInterface aci) {
		this.logger.severe("Rx :  onDialogUserAbort=" + evt);

		// TODO : Error condition. Take care
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

		Sms original = this.getOriginalSmsEvent();

		if (original != null) {
			if (original.getOrigSystemId() != null) {
				this.sendFailureDeliverSmToEsms(original, reason);
			}
		}
	}

	public void onDialogProviderAbort(DialogProviderAbort evt, ActivityContextInterface aci) {
		this.logger.severe("Rx :  onDialogProviderAbort=" + evt);

		MAPAbortProviderReason abortProviderReason = evt.getAbortProviderReason();

		Sms original = this.getOriginalSmsEvent();

		if (original != null) {
			if (original.getOrigSystemId() != null) {
				this.sendFailureDeliverSmToEsms(original, abortProviderReason.toString());
			}
		}
	}

	public void onDialogClose(DialogClose evt, ActivityContextInterface aci) {
		if (logger.isFineEnabled()) {
			this.logger.fine("Rx :  onDialogClose" + evt);
		}
	}

	public void onDialogNotice(DialogNotice evt, ActivityContextInterface aci) {
		if (logger.isInfoEnabled()) {
			this.logger.info("Rx :  onDialogNotice" + evt);
		}
	}

	public void onDialogTimeout(DialogTimeout evt, ActivityContextInterface aci) {
		this.logger.severe("Rx :  onDialogTimeout" + evt);

		Sms original = this.getOriginalSmsEvent();

		if (original != null) {
			if (original.getOrigSystemId() != null) {
				this.sendFailureDeliverSmToEsms(original, DIALOG_TIMEOUT);
			}
		}
	}

	public void onDialogRequest(DialogRequest evt, ActivityContextInterface aci) {
		if (logger.isFineEnabled()) {
			this.logger.fine("Rx :  onDialogRequest" + evt);
		}
	}

	public void onDialogRelease(DialogRelease evt, ActivityContextInterface aci) {
		if (logger.isInfoEnabled()) {
			// TODO : Should be fine
			this.logger.info("Rx :  DialogRelease" + evt);
		}

		MtActivityContextInterface mtSbbActivityContextInterface = this.asSbbActivityContextInterface(this
				.getNullActivityEventContext().getActivityContextInterface());
		this.resumeNullActivityEventDelivery(mtSbbActivityContextInterface, this.getNullActivityEventContext());
	}

	/**
	 * Life cycle methods
	 */

	@Override
	public void sbbActivate() {
		// TODO Auto-generated method stub

	}

	@Override
	public void sbbCreate() throws CreateException {
		// TODO Auto-generated method stub

	}

	@Override
	public void sbbExceptionThrown(Exception arg0, Object arg1, ActivityContextInterface arg2) {
		// TODO Auto-generated method stub

	}

	@Override
	public void sbbLoad() {
		// TODO Auto-generated method stub

	}

	@Override
	public void sbbPassivate() {
		// TODO Auto-generated method stub

	}

	@Override
	public void sbbPostCreate() throws CreateException {
		// TODO Auto-generated method stub

	}

	@Override
	public void sbbRemove() {
		// TODO Auto-generated method stub

	}

	@Override
	public void sbbRolledBack(RolledBackContext arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void sbbStore() {
		// TODO Auto-generated method stub

	}

	@Override
	public void setSbbContext(SbbContext sbbContext) {
		this.sbbContext = (SbbContextExt) sbbContext;

		try {
			Context ctx = (Context) new InitialContext().lookup("java:comp/env");
			this.mapAcif = (MAPContextInterfaceFactory) ctx.lookup("slee/resources/map/2.0/acifactory");
			this.mapProvider = (MAPProvider) ctx.lookup("slee/resources/map/2.0/provider");
			this.mapParameterFactory = this.mapProvider.getMAPParameterFactory();
			this.mapSmsTpduParameterFactory = this.mapProvider.getMAPSmsTpduParameterFactory();

			this.logger = this.sbbContext.getTracer(this.className);

			this.maxMAPApplicationContextVersion = MAPApplicationContextVersion.getInstance(smscPropertiesManagement
					.getMaxMapVersion());
		} catch (Exception ne) {
			logger.severe("Could not set SBB context:", ne);
		}
		// TODO : Handle proper error

	}

	@Override
	public void unsetSbbContext() {

	}

	/**
	 * Fire SmsEvent
	 * 
	 * @param event
	 * @param aci
	 * @param address
	 */
	public abstract void fireSendDeliveryReportSms(Sms event, ActivityContextInterface aci,
			javax.slee.Address address);

	/**
	 * CMPs
	 */
	public abstract void setNullActivityEventContext(EventContext eventContext);

	public abstract EventContext getNullActivityEventContext();

	/**
	 * Sbb ACI
	 */
	public abstract MtActivityContextInterface asSbbActivityContextInterface(ActivityContextInterface aci);

	/**
	 * TODO : This is repetitive in each Sbb. Find way to make it static
	 * probably?
	 * 
	 * This is our own number. We are Service Center.
	 * 
	 * @return
	 */
	protected AddressString getServiceCenterAddressString() {

		if (this.serviceCenterAddress == null) {
			this.serviceCenterAddress = this.mapParameterFactory.createAddressString(
					AddressNature.international_number,
					org.mobicents.protocols.ss7.map.api.primitives.NumberingPlan.ISDN,
					smscPropertiesManagement.getServiceCenterGt());
		}
		return this.serviceCenterAddress;
	}

	/**
	 * TODO: This should be configurable and static as well
	 * 
	 * This is our (Service Center) SCCP Address for GT
	 * 
	 * @return
	 */
	protected SccpAddress getServiceCenterSccpAddress() {
		if (this.serviceCenterSCCPAddress == null) {
			GT0100 gt = new GT0100(0, NumberingPlan.ISDN_TELEPHONY, NatureOfAddress.INTERNATIONAL,
					smscPropertiesManagement.getServiceCenterGt());
			this.serviceCenterSCCPAddress = new SccpAddress(RoutingIndicator.ROUTING_BASED_ON_GLOBAL_TITLE, 0, gt,
					smscPropertiesManagement.getServiceCenterSsn());
		}
		return this.serviceCenterSCCPAddress;
	}

	protected void resumeNullActivityEventDelivery(MtActivityContextInterface mtSbbActivityContextInterface,
			EventContext nullActivityEventContext) {
		if (mtSbbActivityContextInterface.getPendingEventsOnNullActivity() == 0) {
			// If no more events pending, lets end NullActivity
			NullActivity nullActivity = (NullActivity) nullActivityEventContext.getActivityContextInterface()
					.getActivity();
			nullActivity.endActivity();
			if (logger.isInfoEnabled()) {
				this.logger.info(String.format("No more events to be fired on NullActivity=%s:  Ended", nullActivity));
			}
		}
		// Resume delivery for rest of the SMS's for this MSISDN
		if (nullActivityEventContext.isSuspended()) {
			nullActivityEventContext.resumeDelivery();
		}
	}

	protected Sms getOriginalSmsEvent() {
		EventContext nullActivityEventContext = this.getNullActivityEventContext();
		Sms smsEvent = null;
		try {
			smsEvent = (Sms) nullActivityEventContext.getEvent();
		} catch (Exception e) {
			this.logger.severe(
					String.format("Exception while trying to retrieve SmsEvent from NullActivity EventContext"), e);
		}

		return smsEvent;
	}

	protected void sendFailureDeliverSmToEsms(Sms original, String reason) {
		// TODO check if SmppSession available for this SystemId, if not send to
		// SnF module

		this.generateCdr(original, CdrGenerator.CDR_FAILED, reason);

		byte registeredDelivery = original.getRegisteredDelivery();

		// Send Delivery Receipt only if requested
		if (SmppUtil.isSmscDeliveryReceiptRequested(registeredDelivery)
				|| SmppUtil.isSmscDeliveryReceiptOnFailureRequested(registeredDelivery)) {
			Sms deliveryReport = new Sms();
			deliveryReport.setSourceAddr(original.getDestAddr());
			deliveryReport.setSourceAddrNpi(original.getDestAddrNpi());
			deliveryReport.setSourceAddrTon(original.getDestAddrTon());

			deliveryReport.setDestAddr(original.getSourceAddr());
			deliveryReport.setDestAddrNpi(original.getSourceAddrNpi());
			deliveryReport.setDestAddrTon(original.getSourceAddrTon());

			// Setting SystemId as null, so RxSmppServerSbb actually tries to
			// find real SmppServerSession from Destination TON, NPI and address
			// range
			deliveryReport.setOrigSystemId(null);

			deliveryReport.setSubmitDate(original.getSubmitDate());

			deliveryReport.setMessageId(original.getMessageId());

			// TODO : Set appropriate error code in err:
			StringBuffer sb = new StringBuffer();
			sb.append(DELIVERY_ACK_ID).append(original.getMessageId()).append(DELIVERY_ACK_SUB).append("001")
					.append(DELIVERY_ACK_DLVRD).append("001").append(DELIVERY_ACK_SUBMIT_DATE)
					.append(DELIVERY_ACK_DATE_FORMAT.format(original.getSubmitDate())).append(DELIVERY_ACK_DONE_DATE)
					.append(DELIVERY_ACK_DATE_FORMAT.format(new Timestamp(System.currentTimeMillis())))
					.append(DELIVERY_ACK_STAT).append(DELIVERY_ACK_STATE_UNDELIVERABLE).append(DELIVERY_ACK_ERR)
					.append("001").append(DELIVERY_ACK_TEXT)
					.append(this.getFirst20CharOfSMS(original.getShortMessage()));

			byte[] textBytes = CharsetUtil.encode(sb.toString(), CharsetUtil.CHARSET_GSM);

			deliveryReport.setShortMessage(textBytes);
			deliveryReport.setEsmClass(ESME_DELIVERY_ACK);

			NullActivity nullActivity = this.sbbContext.getNullActivityFactory().createNullActivity();
			ActivityContextInterface nullActivityContextInterface = this.sbbContext
					.getNullActivityContextInterfaceFactory().getActivityContextInterface(nullActivity);

			this.fireSendDeliveryReportSms(deliveryReport, nullActivityContextInterface, null);
		}
	}

	protected void sendSuccessDeliverSmToEsms(Sms original) {
		// TODO check if SmppSession available for this SystemId, if not send to
		// SnF module

		this.generateCdr(original, CdrGenerator.CDR_SUCCESS, CDR_SUCCESS_NO_REASON);

		byte registeredDelivery = original.getRegisteredDelivery();

		// Send Delivery Receipt only if requested
		if (SmppUtil.isSmscDeliveryReceiptRequested(registeredDelivery)) {
			Sms deliveryReport = new Sms();
			deliveryReport.setSourceAddr(original.getDestAddr());
			deliveryReport.setSourceAddrNpi(original.getDestAddrNpi());
			deliveryReport.setSourceAddrTon(original.getDestAddrTon());

			deliveryReport.setDestAddr(original.getSourceAddr());
			deliveryReport.setDestAddrNpi(original.getSourceAddrNpi());
			deliveryReport.setDestAddrTon(original.getSourceAddrTon());

			// Setting SystemId as null, so RxSmppServerSbb actually tries to
			// find real SmppServerSession from Destination TON, NPI and address
			// range
			deliveryReport.setOrigSystemId(null);

			deliveryReport.setSubmitDate(original.getSubmitDate());

			deliveryReport.setMessageId(original.getMessageId());

			StringBuffer sb = new StringBuffer();
			sb.append(DELIVERY_ACK_ID).append(original.getMessageId()).append(DELIVERY_ACK_SUB).append("001")
					.append(DELIVERY_ACK_DLVRD).append("001").append(DELIVERY_ACK_SUBMIT_DATE)
					.append(DELIVERY_ACK_DATE_FORMAT.format(original.getSubmitDate())).append(DELIVERY_ACK_DONE_DATE)
					.append(DELIVERY_ACK_DATE_FORMAT.format(new Timestamp(System.currentTimeMillis())))
					.append(DELIVERY_ACK_STAT).append(DELIVERY_ACK_STATE_DELIVERED).append(DELIVERY_ACK_ERR)
					.append("000").append(DELIVERY_ACK_TEXT)
					.append(this.getFirst20CharOfSMS(original.getShortMessage()));

			byte[] textBytes = CharsetUtil.encode(sb.toString(), CharsetUtil.CHARSET_GSM);

			deliveryReport.setShortMessage(textBytes);
			deliveryReport.setEsmClass(ESME_DELIVERY_ACK);

			NullActivity nullActivity = this.sbbContext.getNullActivityFactory().createNullActivity();
			ActivityContextInterface nullActivityContextInterface = this.sbbContext
					.getNullActivityContextInterfaceFactory().getActivityContextInterface(nullActivity);

			this.fireSendDeliveryReportSms(deliveryReport, nullActivityContextInterface, null);
		}
	}

	private String getFirst20CharOfSMS(byte[] rawSms) {
		String first20CharOfSms = new String(rawSms);
		if (first20CharOfSms.length() > 20) {
			first20CharOfSms = first20CharOfSms.substring(0, 20);
		}
		return first20CharOfSms;
	}

	protected void generateCdr(Sms smsEvent, String status, String reason) {
		// Format is
		// SUBMIT_DATE,SOURCE_ADDRESS,SOURCE_TON,SOURCE_NPI,DESTINATION_ADDRESS,DESTINATION_TON,DESTINATION_NPI,STATUS,SYSTEM-ID,MESSAGE-ID,First
		// 20 char of SMS, REASON

		StringBuffer sb = new StringBuffer();
		sb.append(smsEvent.getSubmitDate()).append(CdrGenerator.CDR_SEPARATOR).append(smsEvent.getSourceAddr())
				.append(CdrGenerator.CDR_SEPARATOR).append(smsEvent.getSourceAddrTon())
				.append(CdrGenerator.CDR_SEPARATOR).append(smsEvent.getSourceAddrNpi())
				.append(CdrGenerator.CDR_SEPARATOR).append(smsEvent.getDestAddr()).append(CdrGenerator.CDR_SEPARATOR)
				.append(smsEvent.getDestAddrTon()).append(CdrGenerator.CDR_SEPARATOR).append(smsEvent.getDestAddrNpi())
				.append(CdrGenerator.CDR_SEPARATOR).append(status).append(CdrGenerator.CDR_SEPARATOR)
				.append(smsEvent.getOrigSystemId()).append(CdrGenerator.CDR_SEPARATOR).append(smsEvent.getMessageId())
				.append(CdrGenerator.CDR_SEPARATOR).append(this.getFirst20CharOfSMS(smsEvent.getShortMessage()))
				.append(CdrGenerator.CDR_SEPARATOR).append(reason);

		CdrGenerator.generateCdr(sb.toString());
	}

	private String getErrorComponentReason(MAPErrorMessage mapErrorMessage) {
		String reason = null;
		int errCode = mapErrorMessage.getErrorCode().intValue();
		switch (errCode) {
		case MAPErrorCode.systemFailure:
			reason = MAP_ERR_CODE_SYSTEM_FAILURE;
			break;
		case MAPErrorCode.dataMissing:
			reason = MAP_ERR_CODE_DATA_MISSING;
			break;
		case MAPErrorCode.unexpectedDataValue:
			reason = MAP_ERR_CODE_UNEXPECTED_DATA_VALUE;
			break;
		case MAPErrorCode.facilityNotSupported:
			reason = MAP_ERR_CODE_FACILITY_NOT_SUPPORTED;
			break;
		case MAPErrorCode.incompatibleTerminal:
			reason = MAP_ERR_CODE_INCOMPATIBLE_TERMINAL;
			break;
		case MAPErrorCode.resourceLimitation:
			reason = MAP_ERR_CODE_RESOURCE_LIMITATION;
			break;
		// case MAPErrorCode.noRoamingNumberAvailable:
		// reason = "noRoamingNumberAvailable";
		// break;
		// case MAPErrorCode.absentSubscriber:
		// reason = "absentSubscriber";
		// break;
		// case MAPErrorCode.busySubscriber:
		// reason = "busySubscriber";
		// break;
		// case MAPErrorCode.noSubscriberReply:
		// reason = "noSubscriberReply";
		// break;
		// case MAPErrorCode.callBarred:
		// reason = "callBarred";
		// break;
		// case MAPErrorCode.forwardingFailed:
		// reason = "forwardingFailed";
		// break;
		// case MAPErrorCode.orNotAllowed:
		// reason = "orNotAllowed";
		// break;
		// case MAPErrorCode.forwardingViolation:
		// reason = "forwardingViolation";
		// break;
		// case MAPErrorCode.cugReject:
		// reason = "cugReject";
		// break;
		case MAPErrorCode.subscriberBusyForMTSMS:
			reason = MAP_ERR_CODE_SUBSCRIBER_BUSY_FOR_MTSMS;
			break;
		case MAPErrorCode.smDeliveryFailure:
			MAPErrorMessageSMDeliveryFailure mapErrorMessageSMDeliveryFailure = mapErrorMessage
					.getEmSMDeliveryFailure();
			SMEnumeratedDeliveryFailureCause smEnumeratedDeliveryFailureCause = mapErrorMessageSMDeliveryFailure
					.getSMEnumeratedDeliveryFailureCause();
			reason = MAP_ERR_CODE_SM_DELIVERY_FAILURE + smEnumeratedDeliveryFailureCause.toString();
			break;
		case MAPErrorCode.messageWaitingListFull:
			reason = MAP_ERR_CODE_MESSAGE_WAITING_LIST_FULL;
			break;
		case MAPErrorCode.absentSubscriberSM:
			reason = MAP_ERR_CODE_ABSENT_SUBSCRIBER_SM;
			break;
		default:
			reason = MAP_ERR_CODE_UNKNOWN + errCode;
		}
		return reason;
	}

}
