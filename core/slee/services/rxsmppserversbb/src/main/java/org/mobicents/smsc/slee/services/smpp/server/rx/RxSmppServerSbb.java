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

package org.mobicents.smsc.slee.services.smpp.server.rx;

import java.util.Date;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.slee.ActivityContextInterface;
import javax.slee.CreateException;
import javax.slee.EventContext;
import javax.slee.RolledBackContext;
import javax.slee.Sbb;
import javax.slee.SbbContext;
import javax.slee.facilities.Tracer;
import javax.slee.resource.ResourceAdaptorTypeID;

import org.mobicents.slee.SbbContextExt;
import org.mobicents.smsc.slee.resources.persistence.ErrorCode;
import org.mobicents.smsc.slee.resources.persistence.MessageUtil;
import org.mobicents.smsc.slee.resources.persistence.PersistenceException;
import org.mobicents.smsc.slee.resources.persistence.PersistenceRAInterface;
import org.mobicents.smsc.slee.resources.persistence.Sms;
import org.mobicents.smsc.slee.resources.persistence.SmsSet;
import org.mobicents.smsc.slee.resources.persistence.SmscProcessingException;
import org.mobicents.smsc.slee.resources.persistence.TargetAddress;
import org.mobicents.smsc.slee.resources.scheduler.SchedulerRaSbbInterface;
import org.mobicents.smsc.slee.resources.smpp.server.SmppSessions;
import org.mobicents.smsc.slee.resources.smpp.server.SmppTransaction;
import org.mobicents.smsc.slee.resources.smpp.server.SmppTransactionACIFactory;
import org.mobicents.smsc.slee.resources.smpp.server.events.PduRequestTimeout;
import org.mobicents.smsc.slee.services.smpp.server.events.SmsSetEvent;
import org.mobicents.smsc.smpp.Esme;
import org.mobicents.smsc.smpp.EsmeManagement;

import com.cloudhopper.smpp.SmppConstants;
import com.cloudhopper.smpp.pdu.DeliverSm;
import com.cloudhopper.smpp.pdu.DeliverSmResp;
import com.cloudhopper.smpp.tlv.Tlv;
import com.cloudhopper.smpp.type.Address;
import com.cloudhopper.smpp.type.RecoverablePduException;

/**
 * 
 * @author amit bhayani
 * @author sergey vetyutnev
 * 
 */
public abstract class RxSmppServerSbb implements Sbb {

    private static final ResourceAdaptorTypeID PERSISTENCE_ID = new ResourceAdaptorTypeID("PersistenceResourceAdaptorType", "org.mobicents", "1.0");
    private static final ResourceAdaptorTypeID SCHEDULE_ID = new ResourceAdaptorTypeID("SchedulerResourceAdaptorType", "org.mobicents", "1.0");
    private static final String PERSISTENCE_LINK = "PersistenceResourceAdaptor";
    private static final String SCHEDULE_LINK = "SchedulerResourceAdaptor";

	private Tracer logger;
	private SbbContextExt sbbContext;

	private SmppTransactionACIFactory smppServerTransactionACIFactory = null;
	private SmppSessions smppServerSessions = null;

	private PersistenceRAInterface persistence;
	private SchedulerRaSbbInterface scheduler;

	public RxSmppServerSbb() {
		// TODO Auto-generated constructor stub
	}

	public PersistenceRAInterface getStore() {
		return this.persistence;
	}

    public SchedulerRaSbbInterface getScheduler() {
        return this.scheduler;
    }


	public void onDeliverSm(SmsSetEvent event, ActivityContextInterface aci, EventContext eventContext) {

		if (this.logger.isInfoEnabled()) {
			this.logger.info("\nReceived Deliver SMS. event= " + event + "this=" + this);
		}

		SmsSet smsSet = event.getSmsSet();
		int curMsg = 0;
		Sms sms = smsSet.getSms(curMsg);
		if (sms != null) {
			this.startMessageDelivery(sms);
		}

		this.setCurrentMsgNum(curMsg);
		SmsDeliveryData smsDeliveryData = new SmsDeliveryData();
		smsDeliveryData.setSmsSet(smsSet);
		this.setSmsDeliveryData(smsDeliveryData);

		try {
			this.sendDeliverSm();
		} catch (SmscProcessingException e) {
			String s = "SmscProcessingException when sending initial sendDeliverSm()=" + e.getMessage() + ", Message=" + sms;
			logger.severe(s, e);
			this.onDeliveryError(ErrorAction.temporaryFailure, ErrorCode.SC_SYSTEM_ERROR, s);
		}
	}

	public void onDeliverSmResp(DeliverSmResp event, ActivityContextInterface aci, EventContext eventContext) {
		if (logger.isInfoEnabled()) {
			logger.info(String.format("\nonDeliverSmResp : DeliverSmResp=%s", event));
		}

//		event.getCommandId();
//		event.getSequenceNumber();
		int status = event.getCommandStatus();
		if (status == 0) {

			// current message is sent
			// pushing current message into an archive
			SmsDeliveryData smsDeliveryData = this.getSmsDeliveryData();
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
			int currentMsgNum = this.getCurrentMsgNum();
			Sms sms = smsSet.getSms(currentMsgNum);

			PersistenceRAInterface pers = this.getStore();

			Date deliveryDate = new Date();
			try {
//				generateCdr(sms, CdrGenerator.CDR_SUCCESS, MtCommonSbb.CDR_SUCCESS_NO_REASON);
				pers.archiveDeliveredSms(sms, deliveryDate);
			} catch (PersistenceException e1) {
				this.logger.severe("PersistenceException when archiveDeliveredSms() in RxSmppServerSbb.onDeliverSmResp(): " + e1.getMessage(), e1);
				// we do not "return" here because even if storing into archive database is failed 
				// we will continue delivering process
			}

			// now we are trying to sent other messages
			if (currentMsgNum < smsSet.getSmsCount() - 1) {
				// there are more messages to send in cache
				currentMsgNum++;
				this.setCurrentMsgNum(currentMsgNum);
				sms = smsSet.getSms(currentMsgNum);
				if (sms != null) {
					this.startMessageDelivery(sms);
				}

				try {
					this.sendDeliverSm();
					return;
				} catch (SmscProcessingException e) {
					String s = "SmscProcessingException when sending initial sendDeliverSm()=" + e.getMessage() + ", Message=" + sms;
					logger.severe(s, e);
				}
			}

			// no more messages are in cache now - lets check if there are more messages in a database
			try {
				pers.fetchSchedulableSms(smsSet, false);
			} catch (PersistenceException e1) {
				this.logger.severe("PersistenceException when invoking fetchSchedulableSms(smsSet) from RxSmppServerSbb.onDeliverSmResp(): " + e1.toString(), e1);
			}
			if (smsSet.getSmsCount() > 0) {
				// there are more messages in a database - start delivering of those messages
				currentMsgNum = 0;
				this.setCurrentMsgNum(currentMsgNum);

				try {
					this.sendDeliverSm();
					return;
				} catch (SmscProcessingException e) {
					String s = "SmscProcessingException when sending initial sendDeliverSm()=" + e.getMessage() + ", Message=" + sms;
					logger.severe(s, e);
				}
			}

			// no more messages to send - remove smsSet
			this.freeSmsSetSucceded(smsSet, pers);
		} else {
			this.onDeliveryError(ErrorAction.temporaryFailure, ErrorCode.SC_SYSTEM_ERROR, "DeliverSm response has a bad status: " + status);
		}
	}

	public void onPduRequestTimeout(PduRequestTimeout event, ActivityContextInterface aci, EventContext eventContext) {
		logger.severe(String.format("\nonPduRequestTimeout : PduRequestTimeout=%s", event));

		this.onDeliveryError(ErrorAction.temporaryFailure, ErrorCode.SC_SYSTEM_ERROR, "PduRequestTimeout: ");
	}

	public void onRecoverablePduException(RecoverablePduException event, ActivityContextInterface aci,
			EventContext eventContext) {
		logger.severe(String.format("\nonRecoverablePduException : RecoverablePduException=%s", event));

		this.onDeliveryError(ErrorAction.temporaryFailure, ErrorCode.SC_SYSTEM_ERROR, "RecoverablePduException: ");
	}


	/**
	 * CMPs
	 */
	public abstract void setSmsDeliveryData(SmsDeliveryData smsDeliveryData);

	public abstract SmsDeliveryData getSmsDeliveryData();

	public abstract void setCurrentMsgNum(int currentMsgNum);

	public abstract int getCurrentMsgNum();


	/**
	 * Private methods
	 */

	private void sendDeliverSm() throws SmscProcessingException {

		// TODO: let make here a special check if ESME in a good state
		// if not - skip sending and set temporary error

		SmsDeliveryData smsDeliveryData = this.getSmsDeliveryData();
		if (smsDeliveryData == null) {
			throw new SmscProcessingException("RxSmppServerSbb.sendDeliverSm(): SmsDeliveryData CMP missed", 0, 0, null);
		}
		SmsSet smsSet = smsDeliveryData.getSmsSet();
		if (smsSet == null) {
			throw new SmscProcessingException("RxSmppServerSbb.sendDeliverSm(): In SmsDeliveryData CMP smsSet is missed", 0, 0, null);
		}
		int currentMsgNum = this.getCurrentMsgNum();
		Sms sms = smsSet.getSms(currentMsgNum);

		try {
			EsmeManagement esmeManagement = EsmeManagement.getInstance();
			Esme esme = esmeManagement.getEsmeByClusterName(smsSet.getDestClusterName());
			if (esme == null) {
				String s = "\nRxSmppServerSbb.sendDeliverSm(): Received DELIVER_SM SmsEvent but no Esme found for destClusterName: "
						+ smsSet.getDestClusterName() + ", Message=" + sms;
				logger.warning(s);
				this.onDeliveryError(ErrorAction.temporaryFailure, ErrorCode.SC_SYSTEM_ERROR, s);
				return;
			}

			smsSet.setDestSystemId(esme.getSystemId());
			smsSet.setDestEsmeName(esme.getName());

			DeliverSm deliverSm = new DeliverSm();
			deliverSm.setSourceAddress(new Address((byte) sms.getSourceAddrTon(), (byte) sms.getSourceAddrNpi(), sms.getSourceAddr()));
			deliverSm.setDestAddress(new Address((byte) sms.getSmsSet().getDestAddrTon(), (byte) sms.getSmsSet().getDestAddrNpi(), sms.getSmsSet()
					.getDestAddr()));
			deliverSm.setEsmClass((byte) sms.getEsmClass());
			deliverSm.setProtocolId((byte) sms.getProtocolId());
			deliverSm.setPriority((byte) sms.getPriority());
			if (sms.getScheduleDeliveryTime() != null) {
				deliverSm.setScheduleDeliveryTime(MessageUtil.printSmppAbsoluteDate(sms.getScheduleDeliveryTime(), -(new Date()).getTimezoneOffset()));
			}
			if (sms.getValidityPeriod() != null) {
				deliverSm.setValidityPeriod(MessageUtil.printSmppAbsoluteDate(sms.getValidityPeriod(), -(new Date()).getTimezoneOffset()));
			}
			deliverSm.setRegisteredDelivery((byte) sms.getRegisteredDelivery());
			deliverSm.setReplaceIfPresent((byte) sms.getReplaceIfPresent());
			deliverSm.setDataCoding((byte) sms.getDataCoding());

			if (sms.getShortMessage() != null) {
				if (sms.getShortMessage().length <= 255) {
					deliverSm.setShortMessage(sms.getShortMessage());
				} else {
					Tlv tlv = new Tlv(SmppConstants.TAG_MESSAGE_PAYLOAD, sms.getShortMessage(), null);
					deliverSm.addOptionalParameter(tlv);
				}
			}			

			// TODO : waiting for 2 secs for window to accept our request, is it
			// good? Should time be more here?
			SmppTransaction smppServerTransaction = this.smppServerSessions.sendRequestPdu(esme, deliverSm, 2000);
			if (logger.isInfoEnabled()) {
				logger.info(String.format("\nsent deliverSm to ESME: ", deliverSm));
			}

			ActivityContextInterface smppTxaci = this.smppServerTransactionACIFactory.getActivityContextInterface(smppServerTransaction);
			smppTxaci.attach(this.sbbContext.getSbbLocalObject());

		} catch (Exception e) {
			throw new SmscProcessingException("RxSmppServerSbb.sendDeliverSm(): Exception while trying to send DELIVERY Report for received SmsEvent="
					+ e.getMessage() + "\nMessage: " + sms, 0, 0, null, e);
		} finally {
//			NullActivity nullActivity = (NullActivity) aci.getActivity();
//			nullActivity.endActivity();
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

	private void onDeliveryError(ErrorAction errorAction, ErrorCode smStatus, String reason) {

		SmsDeliveryData smsDeliveryData = this.getSmsDeliveryData();
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
		int currentMsgNum = this.getCurrentMsgNum();
		Sms smsa = smsSet.getSms(currentMsgNum);
//		if (smsa != null)
//			this.generateCdr(smsa, CdrGenerator.CDR_FAILED, reason);

		PersistenceRAInterface pers = this.getStore();

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
					for (int i1 = currentMsgNum; i1 < smsCnt; i1++) {
						Sms sms = smsSet.getSms(currentMsgNum);
						if (sms != null) {
							if (sms.getValidityPeriod().before(curDate)) {
								pers.archiveFailuredSms(sms);
							} else {
								goodMsgCnt++;
							}
						}
					}

					if (goodMsgCnt == 0) {
						// no more messages to send
						// firstly we search for new uploaded message
						pers.fetchSchedulableSms(smsSet, false);
						if (smsSet.getSmsCount() == 0)
							errorAction = ErrorAction.permanentFailure;
					}

					switch (errorAction) {
					case temporaryFailure:
						this.rescheduleSmsSet(smsSet, pers);
						break;

					case permanentFailure:
						this.freeSmsSetFailured(smsSet, pers);
						break;
					}

				} catch (PersistenceException e) {
					this.logger.severe("PersistenceException when RxSmppServerSbb.onDeliveryError()" + e.getMessage(), e);
				}

			} finally {
				pers.releaseSynchroObject(lock);
			}
		}
	}

	/**
	 * Mark a message that its delivery has been started
	 * 
	 * @param sms
	 */
	protected void startMessageDelivery(Sms sms) {

		try {
			this.getStore().setDeliveryStart(sms);
		} catch (PersistenceException e) {
			this.logger.severe("PersistenceException when RxSmppServerSbb.setDeliveryStart(sms)" + e.getMessage(), e);
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
					this.logger.severe("PersistenceException when RxSmppServerSbb.freeSmsSetFailured(SmsSet smsSet)" + e.getMessage(), e);
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
	protected void rescheduleSmsSet(SmsSet smsSet, PersistenceRAInterface pers) {

		TargetAddress lock = pers.obtainSynchroObject(new TargetAddress(smsSet));
		try {
			synchronized (lock) {

				try {
					int prevDueDelay = smsSet.getDueDelay();
					int newDueDelay = MessageUtil.computeNextDueDelay(prevDueDelay);

					Date newDueDate = new Date(new Date().getTime() + newDueDelay * 1000);
					pers.setDeliveringProcessScheduled(smsSet, newDueDate, newDueDelay);
				} catch (PersistenceException e) {
					this.logger.severe("PersistenceException when RxSmppServerSbb.rescheduleSmsSet(SmsSet smsSet)" + e.getMessage(), e);
				}
			}
		} finally {
			pers.releaseSynchroObject(lock);
		}
	}

	
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

			this.smppServerTransactionACIFactory = (SmppTransactionACIFactory) ctx
					.lookup("slee/resources/smppp/server/1.0/acifactory");
			this.smppServerSessions = (SmppSessions) ctx.lookup("slee/resources/smpp/server/1.0/provider");

			this.logger = this.sbbContext.getTracer(getClass().getSimpleName());

			this.persistence = (PersistenceRAInterface) this.sbbContext.getResourceAdaptorInterface(PERSISTENCE_ID, PERSISTENCE_LINK);
            this.scheduler = (SchedulerRaSbbInterface) this.sbbContext.getResourceAdaptorInterface(SCHEDULE_ID, SCHEDULE_LINK);
		} catch (Exception ne) {
			logger.severe("Could not set SBB context:", ne);
		}
	}

	@Override
	public void unsetSbbContext() {
		// TODO Auto-generated method stub

	}

    private void decrementDeliveryActivityCount() {
        if (this.scheduler != null)
            this.scheduler.decrementDeliveryActivityCount();
    }

	public enum ErrorAction {
		temporaryFailure,
		permanentFailure,
	}
}
