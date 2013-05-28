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
import javax.slee.Sbb;
import javax.slee.SbbContext;
import javax.slee.facilities.Tracer;
import javax.slee.resource.ResourceAdaptorTypeID;

import org.mobicents.protocols.ss7.indicator.NatureOfAddress;
import org.mobicents.protocols.ss7.indicator.NumberingPlan;
import org.mobicents.protocols.ss7.indicator.RoutingIndicator;
import org.mobicents.protocols.ss7.map.api.MAPApplicationContextVersion;
import org.mobicents.protocols.ss7.map.api.MAPParameterFactory;
import org.mobicents.protocols.ss7.map.api.MAPProvider;
import org.mobicents.protocols.ss7.map.api.MAPSmsTpduParameterFactory;
import org.mobicents.protocols.ss7.map.api.dialog.MAPUserAbortChoice;
import org.mobicents.protocols.ss7.map.api.dialog.ProcedureCancellationReason;
import org.mobicents.protocols.ss7.map.api.dialog.ResourceUnavailableReason;
import org.mobicents.protocols.ss7.map.api.primitives.AddressNature;
import org.mobicents.protocols.ss7.map.api.primitives.AddressString;
import org.mobicents.protocols.ss7.map.api.primitives.ISDNAddressString;
import org.mobicents.protocols.ss7.map.api.service.sms.SMDeliveryOutcome;
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
import org.mobicents.smsc.slee.resources.persistence.ErrorCode;
import org.mobicents.smsc.slee.resources.persistence.MessageUtil;
import org.mobicents.smsc.slee.resources.persistence.PersistenceException;
import org.mobicents.smsc.slee.resources.persistence.PersistenceRAInterface;
import org.mobicents.smsc.slee.resources.persistence.Sms;
import org.mobicents.smsc.slee.resources.persistence.SmsSet;
import org.mobicents.smsc.slee.resources.persistence.SmsSubmitData;
import org.mobicents.smsc.slee.resources.persistence.TargetAddress;
import org.mobicents.smsc.slee.resources.scheduler.SchedulerRaSbbInterface;
import org.mobicents.smsc.smpp.SmscPropertiesManagement;

/**
 * 
 * @author amit bhayani
 * @author sergey vetyutnev
 * 
 */
public abstract class MtCommonSbb implements Sbb, ReportSMDeliveryStatusInterface2 {

    private static final ResourceAdaptorTypeID PERSISTENCE_ID = new ResourceAdaptorTypeID("PersistenceResourceAdaptorType", "org.mobicents", "1.0");
    private static final ResourceAdaptorTypeID SCHEDULE_ID = new ResourceAdaptorTypeID("SchedulerResourceAdaptorType", "org.mobicents", "1.0");
    private static final String PERSISTENCE_LINK = "PersistenceResourceAdaptor";
    private static final String SCHEDULE_LINK = "SchedulerResourceAdaptor";
    
	protected static final String MAP_USER_ABORT_CHOICE_USER_SPECIFIC_REASON = "userSpecificReason";
	protected static final String MAP_USER_ABORT_CHOICE_USER_RESOURCE_LIMITATION = "userResourceLimitation";
	protected static final String MAP_USER_ABORT_CHOICE_UNKNOWN = "DialogUserAbort_Unknown";

	protected static final String CDR_SUCCESS_NO_REASON = "";

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

    protected PersistenceRAInterface persistence;
    protected SchedulerRaSbbInterface scheduler;

	public MtCommonSbb(String className) {
		this.className = className;
	}

    public PersistenceRAInterface getStore() {
        return this.persistence;
    }

    public SchedulerRaSbbInterface getScheduler() {
        return this.scheduler;
    }

	/**
	 * MAP Components Events
	 */

	public void onErrorComponent(ErrorComponent event, ActivityContextInterface aci) {
		if (this.logger.isInfoEnabled()) {
			this.logger.info("\nRx :  onErrorComponent " + event + " Dialog=" + event.getMAPDialog());
		}
	}

	public void onRejectComponent(RejectComponent event, ActivityContextInterface aci) {
		this.logger.severe("\nRx :  onRejectComponent" + event);
	}

	protected String getRejectComponentReason(RejectComponent event) {
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

		return reason;
	}

	public void onInvokeTimeout(InvokeTimeout evt, ActivityContextInterface aci) {
		if (logger.isWarningEnabled()) {
			this.logger.warning("\nRx : onInvokeTimeout=" + evt);
		}
	}

	/**
	 * Dialog Events
	 */

	public void onDialogReject(DialogReject evt, ActivityContextInterface aci) {
		if (logger.isWarningEnabled()) {
			this.logger.warning("\nRx : onDialogReject=" + evt);
		}
	}

	public void onDialogProviderAbort(DialogProviderAbort evt, ActivityContextInterface aci) {
		if (logger.isWarningEnabled()) {
			this.logger.warning("\nRx :  onDialogProviderAbort=" + evt);
		}
	}

	public void onDialogUserAbort(DialogUserAbort evt, ActivityContextInterface aci) {
		if (logger.isWarningEnabled()) {
			this.logger.warning("\nRx :  onDialogUserAbort=" + evt);
		}
	}

	protected String getUserAbortReason(DialogUserAbort evt) {
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
		return reason;
	}

	public void onDialogTimeout(DialogTimeout evt, ActivityContextInterface aci) {
		if (logger.isWarningEnabled()) {
			this.logger.warning("\nRx :  onDialogTimeout=" + evt);
		}
	}

	public void onDialogDelimiter(DialogDelimiter evt, ActivityContextInterface aci) {
		if (logger.isFineEnabled()) {
			this.logger.fine("\nRx :  onDialogDelimiter=" + evt);
		}
	}

	public void onDialogAccept(DialogAccept evt, ActivityContextInterface aci) {
		if (logger.isFineEnabled()) {
			this.logger.fine("\nRx :  onDialogAccept=" + evt);
		}
	}

	public void onDialogClose(DialogClose evt, ActivityContextInterface aci) {
		if (logger.isFineEnabled()) {
			this.logger.fine("\nRx :  onDialogClose=" + evt);
		}
	}

	public void onDialogNotice(DialogNotice evt, ActivityContextInterface aci) {
		if (logger.isWarningEnabled()) {
			this.logger.warning("\nRx :  onDialogNotice" + evt);
		}
	}

	public void onDialogRequest(DialogRequest evt, ActivityContextInterface aci) {
		if (logger.isFineEnabled()) {
			this.logger.fine("\nRx :  onDialogRequest=" + evt);
		}
	}

	public void onDialogRelease(DialogRelease evt, ActivityContextInterface aci) {
		if (logger.isInfoEnabled()) {
			this.logger.info("\nRx :  DialogRelease=" + evt);
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
            this.persistence = (PersistenceRAInterface) this.sbbContext.getResourceAdaptorInterface(PERSISTENCE_ID, PERSISTENCE_LINK);
            this.scheduler = (SchedulerRaSbbInterface) this.sbbContext.getResourceAdaptorInterface(SCHEDULE_ID, SCHEDULE_LINK);
		
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
		return this.mapParameterFactory.createISDNAddressString(AddressNature.getInstance(ton),
				org.mobicents.protocols.ss7.map.api.primitives.NumberingPlan.getInstance(npi), destinationAddress);
	}

	protected void onDeliveryError(ErrorAction errorAction, ErrorCode smStatus, String reason) {

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
		int currentMsgNum = this.doGetCurrentMsgNum();
		Sms smsa = smsSet.getSms(currentMsgNum);
		if (smsa != null)
			this.generateCdr(smsa, CdrGenerator.CDR_FAILED, reason);

		StringBuilder sb = new StringBuilder();
		sb.append("onDeliveryError: errorAction=");
		sb.append(errorAction);
		sb.append(", smStatus=");
		sb.append(smStatus);
		sb.append(", smsSet=");
		sb.append(smsSet);
		sb.append(", reason=");
		sb.append(", reason=");
		if (this.logger.isInfoEnabled())
			this.logger.info(sb.toString());

		PersistenceRAInterface pers = this.getStore();

		SMDeliveryOutcome smDeliveryOutcome = null;
		TargetAddress lock = pers.obtainSynchroObject(new TargetAddress(smsSet));
		synchronized (lock) {
			try {
				Date curDate = new Date();
				try {
					pers.setDeliveryFailure(smsSet, smStatus, curDate);
                    this.decrementDeliveryActivityCount();                  

					// first of all we are removing messages that delivery
					// period is over
					int smsCnt = smsSet.getSmsCount();
					int goodMsgCnt = 0;
					int removedMsgCnt = 0;
					for (int i1 = currentMsgNum; i1 < smsCnt; i1++) {
						Sms sms = smsSet.getSms(currentMsgNum);
						if (sms != null) {
							if (sms.getValidityPeriod().before(curDate)) {
								pers.archiveFailuredSms(sms);
								removedMsgCnt++;
							} else {
								goodMsgCnt++;
							}
						}
					}

					if (goodMsgCnt == 0 || removedMsgCnt > 0) {
						// no more messages to send
						// firstly we search for new uploaded message
						pers.fetchSchedulableSms(smsSet, false);
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

					case temporaryFailure:
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
			this.setupReportSMDeliveryStatusRequest(smsSet.getDestAddr(), smsSet.getDestAddrTon(), smsSet.getDestAddrNpi(), smDeliveryOutcome, smsSet.getTargetId());
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

	public abstract void doSetSmsSubmitData(SmsSubmitData smsDeliveryData);

	public abstract SmsSubmitData doGetSmsSubmitData();

	public abstract void doSetCurrentMsgNum(int currentMsgNum);

	public abstract int doGetCurrentMsgNum();

	public abstract void doSetInformServiceCenterContainer(InformServiceCenterContainer informServiceCenterContainer);

	public abstract InformServiceCenterContainer doGetInformServiceCenterContainer();


	/**
	 * Mark a message that its delivery has been started
	 * 
	 * @param sms
	 */
	protected void startMessageDelivery(Sms sms) {

		try {
			this.getStore().setDeliveryStart(sms);
		} catch (PersistenceException e) {
			this.logger.severe("PersistenceException when setDeliveryStart(sms)" + e.getMessage(), e);
		}
	}

	/**
	 * remove smsSet from LIVE database after all messages has been delivered
	 * 
	 * @param smsSet
	 */
	protected void freeSmsSetSucceded(SmsSet smsSet, PersistenceRAInterface pers) {

		TargetAddress lock = pers.obtainSynchroObject(new TargetAddress(smsSet));
		try {
			synchronized (lock) {
				try {
					Date lastDelivery = new Date();
					pers.setDeliverySuccess(smsSet, lastDelivery);
					this.decrementDeliveryActivityCount();					

					if (!pers.deleteSmsSet(smsSet)) {
						Date newDueDate = MessageUtil.computeDueDate(MessageUtil.computeFirstDueDelay());
						pers.fetchSchedulableSms(smsSet, false);
						newDueDate = MessageUtil.checkScheduleDeliveryTime(smsSet, newDueDate);
						pers.setNewMessageScheduled(smsSet, newDueDate);
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
	protected void freeSmsSetFailured(SmsSet smsSet, PersistenceRAInterface pers) {

		TargetAddress lock = pers.obtainSynchroObject(new TargetAddress(smsSet));
		try {
			synchronized (lock) {
				try {
					pers.fetchSchedulableSms(smsSet, false);
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
	 * make new schedule time for smsSet after temporary failure
	 * 
	 * @param smsSet
	 */
	protected void rescheduleSmsSet(SmsSet smsSet, boolean busySuscriber, PersistenceRAInterface pers) {

		TargetAddress lock = pers.obtainSynchroObject(new TargetAddress(smsSet));
		try {
			synchronized (lock) {

				try {
					int prevDueDelay = smsSet.getDueDelay();
					int newDueDelay;
					if (busySuscriber) {
						newDueDelay = MessageUtil.computeDueDelaySubscriberBusy();
					} else {
						newDueDelay = MessageUtil.computeNextDueDelay(prevDueDelay);
					}

					Date newDueDate = new Date(new Date().getTime() + newDueDelay * 1000);

					newDueDate = MessageUtil.checkScheduleDeliveryTime(smsSet, newDueDate);
					pers.setDeliveringProcessScheduled(smsSet, newDueDate, newDueDelay);
				} catch (PersistenceException e) {
					this.logger.severe("PersistenceException when rescheduleSmsSet(SmsSet smsSet)" + e.getMessage(), e);
				}
			}
		} finally {
			pers.releaseSynchroObject(lock);
		}
	}

    private void decrementDeliveryActivityCount() {
        if (this.scheduler != null)
            this.scheduler.decrementDeliveryActivityCount();
    }

	public enum ErrorAction {
		subscriberBusy, 
		memoryCapacityExceededFlag, // MNRF 
		mobileNotReachableFlag, // MNRF
		notReachableForGprs, // MNRG
		permanentFailure,
		temporaryFailure,
	}

}

