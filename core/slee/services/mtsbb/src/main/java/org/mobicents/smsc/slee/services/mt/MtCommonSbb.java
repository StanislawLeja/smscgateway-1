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

import java.util.Date;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.slee.ActivityContextInterface;
import javax.slee.CreateException;
import javax.slee.RolledBackContext;
import javax.slee.SLEEException;
import javax.slee.Sbb;
import javax.slee.SbbContext;
import javax.slee.TransactionRequiredLocalException;
import javax.slee.facilities.Tracer;

import org.mobicents.protocols.ss7.indicator.NatureOfAddress;
import org.mobicents.protocols.ss7.indicator.NumberingPlan;
import org.mobicents.protocols.ss7.indicator.RoutingIndicator;
import org.mobicents.protocols.ss7.map.api.MAPApplicationContext;
import org.mobicents.protocols.ss7.map.api.MAPApplicationContextVersion;
import org.mobicents.protocols.ss7.map.api.MAPParameterFactory;
import org.mobicents.protocols.ss7.map.api.MAPProvider;
import org.mobicents.protocols.ss7.map.api.MAPSmsTpduParameterFactory;
import org.mobicents.protocols.ss7.map.api.primitives.AddressNature;
import org.mobicents.protocols.ss7.map.api.primitives.AddressString;
import org.mobicents.protocols.ss7.map.api.primitives.ISDNAddressString;
import org.mobicents.protocols.ss7.map.api.service.sms.SMDeliveryOutcome;
import org.mobicents.protocols.ss7.sccp.parameter.GT0100;
import org.mobicents.protocols.ss7.sccp.parameter.SccpAddress;
import org.mobicents.slee.ChildRelationExt;
import org.mobicents.slee.SbbContextExt;
import org.mobicents.slee.resource.map.MAPContextInterfaceFactory;
import org.mobicents.slee.resource.map.events.DialogAccept;
import org.mobicents.slee.resource.map.events.DialogClose;
import org.mobicents.slee.resource.map.events.DialogDelimiter;
import org.mobicents.slee.resource.map.events.DialogNotice;
import org.mobicents.slee.resource.map.events.DialogRelease;
import org.mobicents.slee.resource.map.events.DialogRequest;
import org.mobicents.smsc.slee.services.persistence.ErrorCode;
import org.mobicents.smsc.slee.services.persistence.MessageUtil;
import org.mobicents.smsc.slee.services.persistence.Persistence;
import org.mobicents.smsc.slee.services.persistence.PersistenceException;
import org.mobicents.smsc.slee.services.persistence.Sms;
import org.mobicents.smsc.slee.services.persistence.SmsSet;
import org.mobicents.smsc.slee.services.persistence.TargetAddress;
import org.mobicents.smsc.smpp.SmscPropertiesManagement;

/**
 * 
 * @author amit bhayani
 * @author sergey vetyutnev
 * 
 */
public abstract class MtCommonSbb implements Sbb, ReportSMDeliveryStatusInterface2 {

	protected static final String MAP_USER_ABORT_CHOICE_USER_SPECIFIC_REASON = "userSpecificReason";
	protected static final String MAP_USER_ABORT_CHOICE_USER_RESOURCE_LIMITATION = "userResourceLimitation";
	protected static final String MAP_USER_ABORT_CHOICE_UNKNOWN = "DialogUserAbort_Unknown";

	private static final String CDR_SUCCESS_NO_REASON = "";

	protected static final SmscPropertiesManagement smscPropertiesManagement = SmscPropertiesManagement.getInstance();

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

	protected Persistence persistence;

	public MtCommonSbb(String className) {
		this.className = className;
	}

	// -------------------------------------------------------------
    // Child relations
    // -------------------------------------------------------------
	public abstract ChildRelationExt getStoreSbb();


	public Persistence getStore() throws TransactionRequiredLocalException, SLEEException, CreateException {
		if (persistence == null) {
			ChildRelationExt childRelation = getStoreSbb();
			persistence = (Persistence) childRelation.get(ChildRelationExt.DEFAULT_CHILD_NAME);
			if (persistence == null) {
				persistence = (Persistence) childRelation.create(ChildRelationExt.DEFAULT_CHILD_NAME);
			}
		}
		return persistence;
	}

	/**
	 * MAP Components Events
	 */

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
	 * Sbb ACI
	 */
//	public abstract MtActivityContextInterface asSbbActivityContextInterface(ActivityContextInterface aci);

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
			this.serviceCenterAddress = this.mapParameterFactory.createAddressString(AddressNature.international_number,
					org.mobicents.protocols.ss7.map.api.primitives.NumberingPlan.ISDN, smscPropertiesManagement.getServiceCenterGt());
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
			GT0100 gt = new GT0100(0, NumberingPlan.ISDN_TELEPHONY, NatureOfAddress.INTERNATIONAL, smscPropertiesManagement.getServiceCenterGt());
			this.serviceCenterSCCPAddress = new SccpAddress(RoutingIndicator.ROUTING_BASED_ON_GLOBAL_TITLE, 0, gt,
					smscPropertiesManagement.getServiceCenterSsn());
		}
		return this.serviceCenterSCCPAddress;
	}

	protected ISDNAddressString getCalledPartyISDNAddressString(String destinationAddress, int ton, int npi) {
		// TODO save the ISDNAddressString in CMP to avoid creation everytime?
		return this.mapParameterFactory.createISDNAddressString(AddressNature.getInstance(ton),
				org.mobicents.protocols.ss7.map.api.primitives.NumberingPlan.getInstance(npi), destinationAddress);
	}

	protected void onDeliveryError(ErrorAction errorAction, ErrorCode smStatus, String reason) {

		SmsDeliveryData smsDeliveryData = this.doGetSmsDeliveryData();
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
		Integer n = this.doGetCurrentMsgNum();
		int currentMsgNum = 0;
		if (n != null) {
			currentMsgNum = n;
		}
		Sms smsa = smsSet.getSms(currentMsgNum);
		if (smsa != null)
			this.generateCdr(smsa, CdrGenerator.CDR_FAILED, reason);

		Persistence pers;
		try {
			pers = this.getStore();
		} catch (TransactionRequiredLocalException e1) {
			this.logger.severe("TransactionRequiredLocalException when onDeliveryError()" + e1.getMessage(), e1);
			return;
		} catch (SLEEException e1) {
			this.logger.severe("SLEEException when onDeliveryError()" + e1.getMessage(), e1);
			return;
		} catch (CreateException e1) {
			this.logger.severe("CreateException when onDeliveryError()" + e1.getMessage(), e1);
			return;
		}

		SMDeliveryOutcome smDeliveryOutcome = null;
		TargetAddress lock = pers.obtainSynchroObject(new TargetAddress(smsSet));
		synchronized (lock) {
			try {
				Date curDate = new Date();
				try {
					pers.setDeliveryFailure(smsSet, smStatus, curDate);

					// first of all we are removing messages that delivery
					// period is over
					int smsCnt = smsSet.getSmsCount();
					int goodMsgCnt = 0;
					for (int i1 = currentMsgNum; i1 < smsCnt; i1++) {
						Sms sms = smsSet.getSms(currentMsgNum);
						if (sms != null) {
							if (sms.getValidityPeriod().after(curDate)) {
								pers.archiveFailuredSms(sms);
							} else {
								goodMsgCnt++;
							}
						}
					}

					if (goodMsgCnt == 0) {
						// no more messages to send
						// firstly we search for new uploaded message
						pers.fetchSchedulableSms(smsSet);
						if (smsSet.getSmsCount() == 0)
							errorAction = ErrorAction.permanentFailure;
					}

					switch (errorAction) {
					case subscriberBusy:
						this.rescheduleSmsSet(smsSet, true, pers);
						break;

					case memoryCapacityExceededFlag:
						smDeliveryOutcome = SMDeliveryOutcome.memoryCapacityExceeded;
						this.rescheduleSmsSet(smsSet, false, pers);
						break;

					case mobileNotReachableFlag:
						smDeliveryOutcome = SMDeliveryOutcome.absentSubscriber;
						this.rescheduleSmsSet(smsSet, false, pers);
						break;

					case notReachableForGprs:
						smDeliveryOutcome = SMDeliveryOutcome.absentSubscriber;
						this.rescheduleSmsSet(smsSet, false, pers);
						break;

					case permanentFailure:
						this.freeSmsSetFailured(smsSet, pers);
						break;
					}

				} catch (PersistenceException e) {
					this.logger.severe("PersistenceException when onDeliveryError()" + e.getMessage(), e);
				}

			} finally {
				pers.releaseSynchroObject(lock);
			}
		}

		if (smDeliveryOutcome != null) {
			MAPApplicationContext mapApplicationContext;
			this.setupReportSMDeliveryStatusRequest(smsSet.getDestAddr(), smsSet.getDestAddrTon(), smsSet.getDestAddrNpi(), smDeliveryOutcome);
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
				.append(CdrGenerator.CDR_SEPARATOR).append(smsEvent.getSmsSet().getDestAddr()).append(CdrGenerator.CDR_SEPARATOR)
				.append(smsEvent.getSmsSet().getDestAddrTon()).append(CdrGenerator.CDR_SEPARATOR).append(smsEvent.getSmsSet().getDestAddrNpi())
				.append(CdrGenerator.CDR_SEPARATOR).append(status).append(CdrGenerator.CDR_SEPARATOR)
				.append(smsEvent.getOrigSystemId()).append(CdrGenerator.CDR_SEPARATOR).append(smsEvent.getMessageId())
				.append(CdrGenerator.CDR_SEPARATOR).append(this.getFirst20CharOfSMS(smsEvent.getShortMessage()))
				.append(CdrGenerator.CDR_SEPARATOR).append(reason);

		CdrGenerator.generateCdr(sb.toString());
	}

//	private String getErrorComponentReason(MAPErrorMessage mapErrorMessage) {
//		String reason = null;
//		int errCode = mapErrorMessage.getErrorCode().intValue();
//		switch (errCode) {
//		case MAPErrorCode.systemFailure:
//			reason = MAP_ERR_CODE_SYSTEM_FAILURE;
//			break;
//		case MAPErrorCode.dataMissing:
//			reason = MAP_ERR_CODE_DATA_MISSING;
//			break;
//		case MAPErrorCode.unexpectedDataValue:
//			reason = MAP_ERR_CODE_UNEXPECTED_DATA_VALUE;
//			break;
//		case MAPErrorCode.facilityNotSupported:
//			reason = MAP_ERR_CODE_FACILITY_NOT_SUPPORTED;
//			break;
//		case MAPErrorCode.incompatibleTerminal:
//			reason = MAP_ERR_CODE_INCOMPATIBLE_TERMINAL;
//			break;
//		case MAPErrorCode.resourceLimitation:
//			reason = MAP_ERR_CODE_RESOURCE_LIMITATION;
//			break;
//		// case MAPErrorCode.noRoamingNumberAvailable:
//		// reason = "noRoamingNumberAvailable";
//		// break;
//		// case MAPErrorCode.absentSubscriber:
//		// reason = "absentSubscriber";
//		// break;
//		// case MAPErrorCode.busySubscriber:
//		// reason = "busySubscriber";
//		// break;
//		// case MAPErrorCode.noSubscriberReply:
//		// reason = "noSubscriberReply";
//		// break;
//		// case MAPErrorCode.callBarred:
//		// reason = "callBarred";
//		// break;
//		// case MAPErrorCode.forwardingFailed:
//		// reason = "forwardingFailed";
//		// break;
//		// case MAPErrorCode.orNotAllowed:
//		// reason = "orNotAllowed";
//		// break;
//		// case MAPErrorCode.forwardingViolation:
//		// reason = "forwardingViolation";
//		// break;
//		// case MAPErrorCode.cugReject:
//		// reason = "cugReject";
//		// break;
//		case MAPErrorCode.subscriberBusyForMTSMS:
//			reason = MAP_ERR_CODE_SUBSCRIBER_BUSY_FOR_MTSMS;
//			break;
//		case MAPErrorCode.smDeliveryFailure:
//			MAPErrorMessageSMDeliveryFailure mapErrorMessageSMDeliveryFailure = mapErrorMessage
//					.getEmSMDeliveryFailure();
//			SMEnumeratedDeliveryFailureCause smEnumeratedDeliveryFailureCause = mapErrorMessageSMDeliveryFailure
//					.getSMEnumeratedDeliveryFailureCause();
//			reason = MAP_ERR_CODE_SM_DELIVERY_FAILURE + smEnumeratedDeliveryFailureCause.toString();
//			break;
//		case MAPErrorCode.messageWaitingListFull:
//			reason = MAP_ERR_CODE_MESSAGE_WAITING_LIST_FULL;
//			break;
//		case MAPErrorCode.absentSubscriberSM:
//			reason = MAP_ERR_CODE_ABSENT_SUBSCRIBER_SM;
//			break;
//		default:
//			reason = MAP_ERR_CODE_UNKNOWN + errCode;
//		}
//		return reason;
//	}

	public abstract void doSetSmsDeliveryData(SmsDeliveryData smsDeliveryData);

	public abstract SmsDeliveryData doGetSmsDeliveryData();

	public abstract void doSetCurrentMsgNum(int currentMsgNum);

	public abstract int doGetCurrentMsgNum();

	public abstract void doSetInformServiceCenterContainer(InformServiceCenterContainer informServiceCenterContainer);

	public abstract InformServiceCenterContainer doGetInformServiceCenterContainer();


	/**
	 * Looking into database if new messages have been added into a database
	 * and load them into smsSet
	 * @param smsSet
	 */
//	protected void fetchNewerMessages(SmsSet smsSet) {
//
//		try {
//			this.getStore().fetchSchedulableSms(smsSet);
//		} catch (TransactionRequiredLocalException e) {
//			this.logger.severe("TransactionRequiredLocalException when fetchSchedulableSms(smsSet)" + e.getMessage(), e);
//		} catch (SLEEException e) {
//			this.logger.severe("SLEEException when fetchSchedulableSms(smsSet)" + e.getMessage(), e);
//		} catch (CreateException e) {
//			this.logger.severe("CreateException when fetchSchedulableSms(smsSet)" + e.getMessage(), e);
//		} catch (PersistenceException e) {
//			this.logger.severe("PersistenceException when fetchSchedulableSms(smsSet)" + e.getMessage(), e);
//		}
//	}

	/**
	 * Mark a message that its delivery has been started
	 * 
	 * @param sms
	 */
	protected void startMessageDelivery(Sms sms) {

		try {
			this.getStore().setDeliveryStart(sms);
		} catch (TransactionRequiredLocalException e) {
			this.logger.severe("TransactionRequiredLocalException when setDeliveryStart(sms)" + e.getMessage(), e);
		} catch (SLEEException e) {
			this.logger.severe("SLEEException when setDeliveryStart(sms)" + e.getMessage(), e);
		} catch (CreateException e) {
			this.logger.severe("CreateException when setDeliveryStart(sms)" + e.getMessage(), e);
		} catch (PersistenceException e) {
			this.logger.severe("PersistenceException when setDeliveryStart(sms)" + e.getMessage(), e);
		}
	}

	/**
	 * Move sms from LIVE_SMS datatbase into ARCHIVE database as delivered
	 * 
	 * @param sms
	 */
//	protected void archiveMessageAsDelivered(Sms sms) {
//		try {
//			Date deliveryDate = new Date();
//			this.getStore().archiveDeliveredSms(sms, deliveryDate);
//		} catch (TransactionRequiredLocalException e) {
//			this.logger.severe("TransactionRequiredLocalException when archiveDeliveredSms(sms)" + e.getMessage(), e);
//		} catch (SLEEException e) {
//			this.logger.severe("SLEEException when archiveDeliveredSms(sms)" + e.getMessage(), e);
//		} catch (CreateException e) {
//			this.logger.severe("CreateException when archiveDeliveredSms(sms)" + e.getMessage(), e);
//		} catch (PersistenceException e) {
//			this.logger.severe("PersistenceException when archiveDeliveredSms(sms)" + e.getMessage(), e);
//		}
//	}

	/**
	 * remove smsSet from LIVE database after all messages has been delivered
	 * 
	 * @param smsSet
	 */
	protected void freeSmsSetSucceded(SmsSet smsSet, Persistence pers) {

		TargetAddress lock = pers.obtainSynchroObject(new TargetAddress(smsSet));
		try {
			synchronized (lock) {
				try {
					Date lastDelivery = new Date();
					pers.setDeliverySuccess(smsSet, lastDelivery);

					if (!pers.deleteSmsSet(smsSet)) {
						pers.setNewMessageScheduled(smsSet, MessageUtil.computeDueDate(MessageUtil.computeFirstDueDelay()));
					}
				} catch (PersistenceException e) {
					this.logger.severe("PersistenceException when freeSmsSetSucceded(SmsSet smsSet)" + e.getMessage(), e);
				}
			}
		} finally {
			pers.releaseSynchroObject(lock);
		}
	}

	/**
	 * remove smsSet from LIVE database after permanent delivery failure
	 * 
	 * @param smsSet
	 * @param pers
	 */
	protected void freeSmsSetFailured(SmsSet smsSet, Persistence pers) {

		TargetAddress lock = pers.obtainSynchroObject(new TargetAddress(smsSet));
		try {
			synchronized (lock) {
				try {
					pers.fetchSchedulableSms(smsSet);
					int cnt = smsSet.getSmsCount();
					for (int i1 = 0; i1 < cnt; i1++) {
						Sms sms = smsSet.getSms(i1);
						pers.archiveFailuredSms(sms);
					}

					pers.deleteSmsSet(smsSet);
				} catch (PersistenceException e) {
					this.logger.severe("PersistenceException when freeSmsSetFailured(SmsSet smsSet)" + e.getMessage(), e);
				}
			}
		} finally {
			pers.releaseSynchroObject(lock);
		}
	}

	/**
	 * archive sms if it's validityPeriod is over
	 * 
	 * @param sms
	 */
//	protected void archiveFailuredSms(Sms sms) {
//
//		Persistence pers;
//		try {
//			pers = this.getStore();
//		} catch (TransactionRequiredLocalException e1) {
//			this.logger.severe("TransactionRequiredLocalException when archiveFailuredSms(Sms sms)" + e1.getMessage(), e1);
//			return;
//		} catch (SLEEException e1) {
//			this.logger.severe("SLEEException when archiveFailuredSms(Sms sms)" + e1.getMessage(), e1);
//			return;
//		} catch (CreateException e1) {
//			this.logger.severe("CreateException when archiveFailuredSms(Sms sms)" + e1.getMessage(), e1);
//			return;
//		}
//
//		try {
//			pers.archiveFailuredSms(sms);
//		} catch (PersistenceException e) {
//			this.logger.severe("PersistenceException when archiveFailuredSms(Set sms)" + e.getMessage(), e);
//		}
//	}

	/**
	 * make new schedule time for smsSet after temporary failure
	 * 
	 * @param smsSet
	 */
	protected void rescheduleSmsSet(SmsSet smsSet, boolean busySuscriber, Persistence pers) {

		TargetAddress lock = pers.obtainSynchroObject(new TargetAddress(smsSet));
		try {
			synchronized (lock) {

				try {
//					Date lastDelivery = new Date();
//					pers.setDeliveryFailure(smsSet, smStatus, lastDelivery, false);

//					SmscPropertiesManagement smscPropertiesManagement = SmscPropertiesManagement.getInstance();
					int prevDueDelay = smsSet.getDueDelay();
					int newDueDelay;
					if (busySuscriber) {
						newDueDelay = MessageUtil.computeDueDelaySubscriberBusy();
					} else {
						newDueDelay = MessageUtil.computeNextDueDelay(prevDueDelay);
//						if (prevDueDelay == 0) {
//							newDueDelay = smscPropertiesManagement.getSecondDueDelay();
//						} else {
//							newDueDelay = prevDueDelay * smscPropertiesManagement.getDueDelayMultiplicator() / 100;
//							if (newDueDelay > smscPropertiesManagement.getMaxDueDelay())
//								newDueDelay = smscPropertiesManagement.getMaxDueDelay();
//						}
					}

					Date newDueDate = new Date(new Date().getTime() + newDueDelay * 1000);
					pers.setDeliveringProcessScheduled(smsSet, newDueDate, newDueDelay);
				} catch (PersistenceException e) {
					this.logger.severe("PersistenceException when rescheduleSmsSet(SmsSet smsSet)" + e.getMessage(), e);
				}
			}
		} finally {
			pers.releaseSynchroObject(lock);
		}
	}

	public enum ErrorAction {
		subscriberBusy, 
		memoryCapacityExceededFlag, // MNRF 
		mobileNotReachableFlag, // MNRF
		notReachableForGprs, // MNRG
		permanentFailure,
	}

}

