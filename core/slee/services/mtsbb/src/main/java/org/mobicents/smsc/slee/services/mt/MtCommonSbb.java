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

import java.util.ArrayList;
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
import org.mobicents.smsc.cassandra.CdrGenerator;
import org.mobicents.smsc.cassandra.DBOperations_C2;
import org.mobicents.smsc.cassandra.DatabaseType;
import org.mobicents.smsc.cassandra.ErrorCode;
import org.mobicents.smsc.cassandra.PersistenceException;
import org.mobicents.smsc.cassandra.Sms;
import org.mobicents.smsc.cassandra.SmsSet;
import org.mobicents.smsc.cassandra.SmsSetCashe;
import org.mobicents.smsc.cassandra.TargetAddress;
import org.mobicents.smsc.slee.resources.persistence.MessageUtil;
import org.mobicents.smsc.slee.resources.persistence.PersistenceRAInterface;
import org.mobicents.smsc.slee.resources.persistence.SmsSubmitData;
import org.mobicents.smsc.slee.resources.scheduler.SchedulerActivity;
import org.mobicents.smsc.slee.resources.scheduler.SchedulerRaSbbInterface;
import org.mobicents.smsc.smpp.SmscPropertiesManagement;

/**
 * 
 * @author amit bhayani
 * @author sergey vetyutnev
 * 
 */
public abstract class MtCommonSbb implements Sbb, ReportSMDeliveryStatusInterface2 {

	private static final ResourceAdaptorTypeID PERSISTENCE_ID = new ResourceAdaptorTypeID(
			"PersistenceResourceAdaptorType", "org.mobicents", "1.0");
	private static final ResourceAdaptorTypeID SCHEDULE_ID = new ResourceAdaptorTypeID("SchedulerResourceAdaptorType",
			"org.mobicents", "1.0");
	private static final String PERSISTENCE_LINK = "PersistenceResourceAdaptor";
	private static final String SCHEDULE_LINK = "SchedulerResourceAdaptor";

	protected static final String MAP_USER_ABORT_CHOICE_USER_SPECIFIC_REASON = "userSpecificReason";
	protected static final String MAP_USER_ABORT_CHOICE_USER_RESOURCE_LIMITATION = "userResourceLimitation";
	protected static final String MAP_USER_ABORT_CHOICE_UNKNOWN = "DialogUserAbort_Unknown";

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
        SmsSubmitData smsDeliveryData = this.doGetSmsSubmitData();
        String targetId = null;
        if (smsDeliveryData != null) {
            targetId = smsDeliveryData.getTargetId();
        }

        if (this.logger.isInfoEnabled()) {
            this.logger.info("\nRx :  onErrorComponent " + event + " targetId=" + targetId + ", Dialog=" + event.getMAPDialog());
        }
	}

	public void onRejectComponent(RejectComponent event, ActivityContextInterface aci) {
        SmsSubmitData smsDeliveryData = this.doGetSmsSubmitData();
        String targetId = null;
        if (smsDeliveryData != null) {
            targetId = smsDeliveryData.getTargetId();
        }

		this.logger.severe("\nRx :  onRejectComponent targetId=" + targetId + ", " + event);
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
        SmsSubmitData smsDeliveryData = this.doGetSmsSubmitData();
        String targetId = null;
        if (smsDeliveryData != null) {
            targetId = smsDeliveryData.getTargetId();
        }

		if (logger.isWarningEnabled()) {
			this.logger.warning("\nRx : onInvokeTimeout targetId=" + targetId + ", " + evt);
		}
	}

	/**
	 * Dialog Events
	 */

	public void onDialogReject(DialogReject evt, ActivityContextInterface aci) {
        SmsSubmitData smsDeliveryData = this.doGetSmsSubmitData();
        String targetId = null;
        if (smsDeliveryData != null) {
            targetId = smsDeliveryData.getTargetId();
        }

		if (logger.isWarningEnabled()) {
			this.logger.warning("\nRx : onDialogReject targetId=" + targetId + ", " + evt);
		}
	}

	public void onDialogProviderAbort(DialogProviderAbort evt, ActivityContextInterface aci) {
        SmsSubmitData smsDeliveryData = this.doGetSmsSubmitData();
        String targetId = null;
        if (smsDeliveryData != null) {
            targetId = smsDeliveryData.getTargetId();
        }

		if (logger.isWarningEnabled()) {
			this.logger.warning("\nRx :  onDialogProviderAbort targetId=" + targetId + ", " + evt);
		}
	}

	public void onDialogUserAbort(DialogUserAbort evt, ActivityContextInterface aci) {
        SmsSubmitData smsDeliveryData = this.doGetSmsSubmitData();
        String targetId = null;
        if (smsDeliveryData != null) {
            targetId = smsDeliveryData.getTargetId();
        }

		if (logger.isWarningEnabled()) {
			this.logger.warning("\nRx :  onDialogUserAbort targetId=" + targetId + ", " + evt);
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
        SmsSubmitData smsDeliveryData = this.doGetSmsSubmitData();
        String targetId = null;
        if (smsDeliveryData != null) {
            targetId = smsDeliveryData.getTargetId();
        }

		if (logger.isWarningEnabled()) {
			this.logger.warning("\nRx :  onDialogTimeout targetId=" + targetId + ", " + evt);
		}
	}

	public void onDialogDelimiter(DialogDelimiter evt, ActivityContextInterface aci) {
		if (logger.isFineEnabled()) {
			this.logger.fine("\nRx :  onDialogDelimiter " + evt);
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
        SmsSubmitData smsDeliveryData = this.doGetSmsSubmitData();
        String targetId = null;
        if (smsDeliveryData != null) {
            targetId = smsDeliveryData.getTargetId();
        }

		if (logger.isWarningEnabled()) {
			this.logger.warning("\nRx :  onDialogNotice targetId=" + targetId + ", " + evt);
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

			this.persistence = (PersistenceRAInterface) this.sbbContext.getResourceAdaptorInterface(PERSISTENCE_ID,
					PERSISTENCE_LINK);
			this.scheduler = (SchedulerRaSbbInterface) this.sbbContext.getResourceAdaptorInterface(SCHEDULE_ID,
					SCHEDULE_LINK);

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
	// public abstract MtActivityContextInterface
	// asSbbActivityContextInterface(ActivityContextInterface aci);

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

	protected ISDNAddressString getCalledPartyISDNAddressString(String destinationAddress, int ton, int npi) {
		return this.mapParameterFactory.createISDNAddressString(AddressNature.getInstance(ton),
				org.mobicents.protocols.ss7.map.api.primitives.NumberingPlan.getInstance(npi), destinationAddress);
	}

    protected void onDeliveryError(SmsSet smsSet, ErrorAction errorAction, ErrorCode smStatus, String reason, boolean removeSmsSet) {
        PersistenceRAInterface pers = this.getStore();

        int currentMsgNum = this.doGetCurrentMsgNum();
		Sms smsa = smsSet.getSms(currentMsgNum);
        if (smsa != null) {
            CdrGenerator.generateCdr(smsa, CdrGenerator.CDR_TEMP_FAILED, reason);
        }

		StringBuilder sb = new StringBuilder();
		sb.append("onDeliveryError: errorAction=");
        sb.append(errorAction);
        sb.append(", smStatus=");
        sb.append(smStatus);
        sb.append(", targetId=");
        sb.append(smsSet.getTargetId());
		sb.append(", smsSet=");
		sb.append(smsSet);
        sb.append(", reason=");
        sb.append(reason);
		if (this.logger.isInfoEnabled())
			this.logger.info(sb.toString());

		ArrayList<Sms> lstFailured = new ArrayList<Sms>();

		SMDeliveryOutcome smDeliveryOutcome = null;
		TargetAddress lock = pers.obtainSynchroObject(new TargetAddress(smsSet));
		synchronized (lock) {
			try {
				Date curDate = new Date();
				try {
                    if (smscPropertiesManagement.getDatabaseType() == DatabaseType.Cassandra_1) {
                        pers.setDeliveryFailure(smsSet, smStatus, curDate);
                    } else {
                        smsSet.setStatus(smStatus);
                        if (removeSmsSet)
                            SmsSetCashe.getInstance().removeProcessingSmsSet(smsSet.getTargetId());
                    }
					this.decrementDeliveryActivityCount();

					int smsCnt;
                    if (smscPropertiesManagement.getDatabaseType() == DatabaseType.Cassandra_1) {
                        // first of all we are removing messages that delivery
                        // period is over
                        smsCnt = smsSet.getSmsCount();
                        int goodMsgCnt = 0;
                        int removedMsgCnt = 0;
                        for (int i1 = currentMsgNum; i1 < smsCnt; i1++) {
                            Sms sms = smsSet.getSms(i1);
                            if (sms != null) {
                                if (sms.getValidityPeriod().before(curDate)) {
                                    pers.archiveFailuredSms(sms);
                                    lstFailured.add(sms);
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
                    } else {
                    }

					switch (errorAction) {
					case subscriberBusy:
						this.rescheduleSmsSet(smsSet, true, pers, currentMsgNum);
						break;

					case memoryCapacityExceededFlag:
						smDeliveryOutcome = SMDeliveryOutcome.memoryCapacityExceeded;
						this.rescheduleSmsSet(smsSet, false, pers, currentMsgNum);
						break;

					case mobileNotReachableFlag:
						smDeliveryOutcome = SMDeliveryOutcome.absentSubscriber;
						this.rescheduleSmsSet(smsSet, false, pers, currentMsgNum);
						break;

					case notReachableForGprs:
						smDeliveryOutcome = SMDeliveryOutcome.absentSubscriber;
						this.rescheduleSmsSet(smsSet, false, pers, currentMsgNum);
						break;

					case temporaryFailure:
						this.rescheduleSmsSet(smsSet, false, pers, currentMsgNum);
						break;

					case permanentFailure:
						smsCnt = smsSet.getSmsCount();
						for (int i1 = currentMsgNum; i1 < smsCnt; i1++) {
							Sms sms = smsSet.getSms(currentMsgNum);
							if (sms != null) {
								lstFailured.add(sms);
							}
						}
						this.freeSmsSetFailured(smsSet, pers, currentMsgNum);
						break;
					}

				} catch (PersistenceException e) {
					this.logger.severe("PersistenceException when onDeliveryError()" + e.getMessage(), e);
				}

			} finally {
				pers.releaseSynchroObject(lock);
			}
		}

		for (Sms sms : lstFailured) {
		    CdrGenerator.generateCdr(sms, CdrGenerator.CDR_FAILED, reason);

            // adding an error receipt if it is needed
			int registeredDelivery = sms.getRegisteredDelivery();
			if (MessageUtil.isReceiptOnFailure(registeredDelivery)) {
				TargetAddress ta = new TargetAddress(sms.getSourceAddrTon(), sms.getSourceAddrNpi(),
						sms.getSourceAddr());
				lock = SmsSetCashe.getInstance().addSmsSet(ta);
				try {
					synchronized (lock) {
						try {
						    Sms receipt;
                            if (smscPropertiesManagement.getDatabaseType() == DatabaseType.Cassandra_1) {
                                receipt = MessageUtil.createReceiptSms(sms, false);
                                SmsSet backSmsSet = pers.obtainSmsSet(ta);
                                receipt.setSmsSet(backSmsSet);
                                pers.createLiveSms(receipt);
                                pers.setNewMessageScheduled(receipt.getSmsSet(), MessageUtil.computeDueDate(MessageUtil.computeFirstDueDelay()));
                            } else {
                                receipt = MessageUtil.createReceiptSms(sms, false);
                                SmsSet backSmsSet = new SmsSet();
                                backSmsSet.setDestAddr(ta.getAddr());
                                backSmsSet.setDestAddrNpi(ta.getAddrNpi());
                                backSmsSet.setDestAddrTon(ta.getAddrTon());
                                receipt.setSmsSet(backSmsSet);
                                pers.c2_createRecordCurrent(receipt);
                            }
							this.logger.info("Adding an error receipt: source=" + receipt.getSourceAddr() + ", dest="
									+ receipt.getSmsSet().getDestAddr());
						} catch (PersistenceException e) {
							this.logger.severe(
									"PersistenceException when freeSmsSetFailured(SmsSet smsSet) - adding delivery receipt"
											+ e.getMessage(), e);
						}
					}
				} finally {
					SmsSetCashe.getInstance().removeSmsSet(lock);
				}
			}
		}

		if (smDeliveryOutcome != null) {
			this.setupReportSMDeliveryStatusRequest(smsSet.getDestAddr(), smsSet.getDestAddrTon(),
					smsSet.getDestAddrNpi(), smDeliveryOutcome, smsSet.getTargetId());
		}
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
        try {
            this.decrementDeliveryActivityCount();
            if (smscPropertiesManagement.getDatabaseType() == DatabaseType.Cassandra_1) {
                Date lastDelivery = new Date();
                pers.setDeliverySuccess(smsSet, lastDelivery);

                if (!pers.deleteSmsSet(smsSet)) {
                    Date newDueDate = MessageUtil.computeDueDate(MessageUtil.computeFirstDueDelay());
                    pers.fetchSchedulableSms(smsSet, false);
                    newDueDate = MessageUtil.checkScheduleDeliveryTime(smsSet, newDueDate);
                    pers.setNewMessageScheduled(smsSet, newDueDate);
                }
            } else {
                smsSet.setStatus(ErrorCode.SUCCESS);
                SmsSetCashe.getInstance().removeProcessingSmsSet(smsSet.getTargetId());
            }
        } catch (PersistenceException e) {
            this.logger.severe("PersistenceException when freeSmsSetSucceded(SmsSet smsSet)" + e.getMessage(), e);
        }
	}

	/**
	 * remove smsSet from LIVE database after permanent delivery failure
	 * 
	 * @param smsSet
	 * @param pers
	 */
	protected void freeSmsSetFailured(SmsSet smsSet, PersistenceRAInterface pers, int currentMsgNum) {

		TargetAddress lock = pers.obtainSynchroObject(new TargetAddress(smsSet));
		try {
			synchronized (lock) {
				try {
                    if (smscPropertiesManagement.getDatabaseType() == DatabaseType.Cassandra_1) {
                        pers.fetchSchedulableSms(smsSet, false);
                        int cnt = smsSet.getSmsCount();
                        for (int i1 = 0; i1 < cnt; i1++) {
                            Sms sms = smsSet.getSms(i1);
                            pers.archiveFailuredSms(sms);
                        }

                        pers.deleteSmsSet(smsSet);
                    } else {
                        for (int i1 = currentMsgNum; i1 < smsSet.getSmsCount(); i1++) {
                            Sms sms = smsSet.getSms(i1);
                            if (sms.getStored()) {
                                pers.c2_updateInSystem(sms, DBOperations_C2.IN_SYSTEM_SENT);
                                sms.setDeliveryDate(new Date());
                                pers.c2_createRecordArchive(sms);
                            }
                        }
                    }
				} catch (PersistenceException e) {
					this.logger.severe("PersistenceException when freeSmsSetFailured(SmsSet smsSet)" + e.getMessage(),
							e);
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
	protected void rescheduleSmsSet(SmsSet smsSet, boolean busySuscriber, PersistenceRAInterface pers, int currentMsgNum) {

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
                    if (smscPropertiesManagement.getDatabaseType() == DatabaseType.Cassandra_1) {
                        pers.setDeliveringProcessScheduled(smsSet, newDueDate, newDueDelay);
                    } else {
                        smsSet.setDueDate(newDueDate);
                        smsSet.setDueDelay(newDueDelay);
                        long dueSlot = this.getStore().c2_getDueSlotForTime(newDueDate);
                        for (int i1 = currentMsgNum; i1 < smsSet.getSmsCount(); i1++) {
                            Sms sms = smsSet.getSms(i1);
                            if (sms.getStored()) {
                                pers.c2_updateInSystem(sms, DBOperations_C2.IN_SYSTEM_SENT);
                                pers.c2_updateDueSlotForTargetId_WithTableCleaning(smsSet.getTargetId(), dueSlot);
                                pers.c2_scheduleMessage(sms, dueSlot);
                            }
                        }
                    }
				} catch (PersistenceException e) {
					this.logger.severe("PersistenceException when rescheduleSmsSet(SmsSet smsSet)" + e.getMessage(), e);
				}
			}
		} finally {
			pers.releaseSynchroObject(lock);
		}
	}

	/**
	 * Get the Scheduler Activity
	 * 
	 * @return
	 */
	protected ActivityContextInterface getSchedulerActivityContextInterface() {
		ActivityContextInterface[] acis = this.sbbContext.getActivities();
		for (int count = 0; count < acis.length; count++) {
			ActivityContextInterface aci = acis[count];
			Object activity = aci.getActivity();
			if (activity instanceof SchedulerActivity) {
				return aci;
			}
		}

		return null;
	}

	private void decrementDeliveryActivityCount() {
		try {
			ActivityContextInterface schedulerActivityContextInterface = this.getSchedulerActivityContextInterface();
			SchedulerActivity schedulerActivity = (SchedulerActivity) schedulerActivityContextInterface.getActivity();

			schedulerActivity.endActivity();
		} catch (Exception e) {
			this.logger.severe("Error while decrementing DeliveryActivityCount", e);
		}
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
