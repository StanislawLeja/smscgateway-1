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

package org.mobicents.smsc.slee.services.sip.server.rx;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.sip.ClientTransaction;
import javax.sip.ListeningPoint;
import javax.sip.address.AddressFactory;
import javax.sip.address.SipURI;
import javax.sip.header.CSeqHeader;
import javax.sip.header.CallIdHeader;
import javax.sip.header.ContentTypeHeader;
import javax.sip.header.FromHeader;
import javax.sip.header.HeaderFactory;
import javax.sip.header.MaxForwardsHeader;
import javax.sip.header.ToHeader;
import javax.sip.header.ViaHeader;
import javax.sip.message.MessageFactory;
import javax.sip.message.Request;
import javax.slee.ActivityContextInterface;
import javax.slee.CreateException;
import javax.slee.EventContext;
import javax.slee.RolledBackContext;
import javax.slee.Sbb;
import javax.slee.SbbContext;
import javax.slee.facilities.Tracer;
import javax.slee.resource.ResourceAdaptorTypeID;

import net.java.slee.resource.sip.SipActivityContextInterfaceFactory;
import net.java.slee.resource.sip.SleeSipProvider;

import org.mobicents.protocols.ss7.map.api.smstpdu.CharacterSet;
import org.mobicents.protocols.ss7.map.api.smstpdu.DataCodingScheme;
import org.mobicents.protocols.ss7.map.smstpdu.DataCodingSchemeImpl;
import org.mobicents.slee.SbbContextExt;
import org.mobicents.smsc.cassandra.CdrGenerator;
import org.mobicents.smsc.cassandra.DBOperations_C2;
import org.mobicents.smsc.cassandra.DatabaseType;
import org.mobicents.smsc.cassandra.ErrorAction;
import org.mobicents.smsc.cassandra.ErrorCode;
import org.mobicents.smsc.cassandra.PersistenceException;
import org.mobicents.smsc.cassandra.Sms;
import org.mobicents.smsc.cassandra.SmsSet;
import org.mobicents.smsc.cassandra.SmsSetCashe;
import org.mobicents.smsc.cassandra.TargetAddress;
import org.mobicents.smsc.slee.resources.persistence.MessageUtil;
import org.mobicents.smsc.slee.resources.persistence.PersistenceRAInterface;
import org.mobicents.smsc.slee.resources.persistence.SmscProcessingException;
import org.mobicents.smsc.slee.resources.scheduler.SchedulerActivity;
import org.mobicents.smsc.slee.resources.scheduler.SchedulerRaSbbInterface;
import org.mobicents.smsc.slee.services.smpp.server.events.SmsSetEvent;
import org.mobicents.smsc.smpp.Sip;
import org.mobicents.smsc.smpp.SipManagement;
import org.mobicents.smsc.smpp.SmppEncodingForUCS2;
import org.mobicents.smsc.smpp.SmscPropertiesManagement;

import com.cloudhopper.smpp.SmppConstants;

/**
 * 
 * @author amit bhayani
 * 
 */
public abstract class RxSipServerSbb implements Sbb {
	protected static SmscPropertiesManagement smscPropertiesManagement = SmscPropertiesManagement.getInstance();

	private static final ResourceAdaptorTypeID PERSISTENCE_ID = new ResourceAdaptorTypeID(
			"PersistenceResourceAdaptorType", "org.mobicents", "1.0");
	private static final ResourceAdaptorTypeID SCHEDULE_ID = new ResourceAdaptorTypeID("SchedulerResourceAdaptorType",
			"org.mobicents", "1.0");
	private static final String PERSISTENCE_LINK = "PersistenceResourceAdaptor";
	private static final String SCHEDULE_LINK = "SchedulerResourceAdaptor";

	// SIP RA
	private static final ResourceAdaptorTypeID SIP_RA_TYPE_ID = new ResourceAdaptorTypeID("JAIN SIP", "javax.sip",
			"1.2");
	private static final String SIP_RA_LINK = "SipRA";
	private SleeSipProvider sipRA;

	private MessageFactory messageFactory;
	private AddressFactory addressFactory;
	private HeaderFactory headerFactory;

	private SipActivityContextInterfaceFactory sipACIFactory = null;

	private Tracer logger;
	private SbbContextExt sbbContext;

	private PersistenceRAInterface persistence;
	private SchedulerRaSbbInterface scheduler;

	private static final SipManagement sipManagement = SipManagement.getInstance();

	public RxSipServerSbb() {
		// TODO Auto-generated constructor stub
	}

	public PersistenceRAInterface getStore() {
		return this.persistence;
	}

	public SchedulerRaSbbInterface getScheduler() {
		return this.scheduler;
	}

	public void onSipSm(SmsSetEvent event, ActivityContextInterface aci, EventContext eventContext) {

		try {
			if (this.logger.isFineEnabled()) {
				this.logger.fine("\nReceived SIP SMS. event= " + event + "this=" + this);
			}

			SmsSet smsSet = event.getSmsSet();

			if (smscPropertiesManagement.getDatabaseType() == DatabaseType.Cassandra_1) {
				try {
					this.getStore().fetchSchedulableSms(smsSet, false);
				} catch (PersistenceException e) {
					this.onDeliveryError(smsSet, ErrorAction.temporaryFailure, ErrorCode.SC_SYSTEM_ERROR,
							"PersistenceException when fetchSchedulableSms(): " + e.getMessage());
					return;
				}
			} else {
				// TODO?
			}

			int curMsg = 0;
			Sms sms = smsSet.getSms(curMsg);
			if (sms != null) {
				if (smscPropertiesManagement.getDatabaseType() == DatabaseType.Cassandra_1) {
					this.startMessageDelivery(sms);
				} else {
					sms.setDeliveryCount(sms.getDeliveryCount() + 1);
				}
			}

			this.setCurrentMsgNum(curMsg);
			this.setTargetId(smsSet.getTargetId());

			try {
				this.sendMessage(smsSet);
			} catch (SmscProcessingException e) {
				String s = "SmscProcessingException when sending SIP MESSAGE=" + e.getMessage() + ", Message=" + sms;
				logger.severe(s, e);
				this.onDeliveryError(smsSet, ErrorAction.temporaryFailure, ErrorCode.SC_SYSTEM_ERROR, s);
			}
		} catch (Throwable e1) {
			logger.severe(
					"Exception in RxSmppServerSbb.onDeliverSm() when fetching records and issuing events: "
							+ e1.getMessage(), e1);
		}
	}

	public void onCLIENT_ERROR(javax.sip.ResponseEvent event, ActivityContextInterface aci) {
		this.logger.warning("onCLIENT_ERROR " + event);

		String targetId = this.getTargetId();
		if (targetId == null) {
			this.logger.severe("onCLIENT_ERROR but there is no TargetId CMP!");
			return;
		}
		SmsSet smsSet = SmsSetCashe.getInstance().getProcessingSmsSet(targetId);

		if (smsSet == null) {
			logger.severe("onCLIENT_ERROR but CMP smsSet is missed, targetId=" + targetId);
			return;
		}

		// TODO : Is CLIENT ERROR temporary?
		this.onDeliveryError(smsSet, ErrorAction.temporaryFailure, ErrorCode.SC_SYSTEM_ERROR,
				"SIP Exception CLIENT_ERROR received. Reason : " + event.getResponse().getReasonPhrase()
						+ " Status Code : " + event.getResponse().getStatusCode());
	}

	public void onSERVER_ERROR(javax.sip.ResponseEvent event, ActivityContextInterface aci) {
		this.logger.severe("onSERVER_ERROR " + event);

		String targetId = this.getTargetId();
		if (targetId == null) {
			this.logger.severe("onSERVER_ERROR but there is no TargetId CMP!");
			return;
		}
		SmsSet smsSet = SmsSetCashe.getInstance().getProcessingSmsSet(targetId);

		if (smsSet == null) {
			logger.severe("onSERVER_ERROR but CMP smsSet is missed, targetId=" + targetId);
			return;
		}

		// TODO : Is SERVER ERROR permanent?
		this.onDeliveryError(smsSet, ErrorAction.permanentFailure, ErrorCode.SC_SYSTEM_ERROR,
				"SIP Exception SERVER_ERROR received. Reason : " + event.getResponse().getReasonPhrase()
						+ " Status Code : " + event.getResponse().getStatusCode());
	}

	public void onSUCCESS(javax.sip.ResponseEvent event, ActivityContextInterface aci) {
		if (this.logger.isFineEnabled()) {
			this.logger.fine("onSUCCESS " + event);
		}

		try {

			String targetId = this.getTargetId();
			if (targetId == null) {
				logger.severe("RxSmppServerSbb.sendDeliverSm(): onDeliverSmResp CMP missed");
				return;
			}
			SmsSet smsSet = SmsSetCashe.getInstance().getProcessingSmsSet(targetId);
			if (smsSet == null) {
				logger.severe("RxSmppServerSbb.sendDeliverSm(): In onDeliverSmResp CMP smsSet is missed, targetId="
						+ targetId);
				return;
			}

			// current message is sent pushing current message into an archive
			int currentMsgNum = this.getCurrentMsgNum();
			Sms sms = smsSet.getSms(currentMsgNum);

			PersistenceRAInterface pers = this.getStore();

			Date deliveryDate = new Date();
			try {

				// we need to find if it is the last or single segment
				boolean isPartial = MessageUtil.isSmsNotLastSegment(sms);
				CdrGenerator.generateCdr(sms, isPartial ? CdrGenerator.CDR_PARTIAL_SIP : CdrGenerator.CDR_SUCCESS_SIP,
						CdrGenerator.CDR_SUCCESS_NO_REASON, smscPropertiesManagement.getGenerateReceiptCdr());

				if (smscPropertiesManagement.getDatabaseType() == DatabaseType.Cassandra_1) {
					pers.archiveDeliveredSms(sms, deliveryDate);
				} else {
					if (sms.getStored()) {
						pers.c2_updateInSystem(sms, DBOperations_C2.IN_SYSTEM_SENT);
						sms.setDeliveryDate(deliveryDate);
						sms.getSmsSet().setStatus(ErrorCode.SUCCESS);
						pers.c2_createRecordArchive(sms);
					}
				}

				// adding a success receipt if it is needed
				if (sms.getStored()) {
					int registeredDelivery = sms.getRegisteredDelivery();
					if (MessageUtil.isReceiptOnSuccess(registeredDelivery)) {
						TargetAddress ta = new TargetAddress(sms.getSourceAddrTon(), sms.getSourceAddrNpi(),
								sms.getSourceAddr());
						TargetAddress lock = SmsSetCashe.getInstance().addSmsSet(ta);
						try {
							synchronized (lock) {
								Sms receipt;
								if (smscPropertiesManagement.getDatabaseType() == DatabaseType.Cassandra_1) {
									receipt = MessageUtil.createReceiptSms(sms, true);
									SmsSet backSmsSet = pers.obtainSmsSet(ta);
									receipt.setSmsSet(backSmsSet);
									pers.createLiveSms(receipt);
									pers.setNewMessageScheduled(receipt.getSmsSet(),
											MessageUtil.computeDueDate(MessageUtil.computeFirstDueDelay()));
									this.logger.info("Adding a delivery receipt: source=" + receipt.getSourceAddr()
											+ ", dest=" + receipt.getSmsSet().getDestAddr());
								} else {
									receipt = MessageUtil.createReceiptSms(sms, true);
									SmsSet backSmsSet = new SmsSet();
									backSmsSet.setDestAddr(ta.getAddr());
									backSmsSet.setDestAddrNpi(ta.getAddrNpi());
									backSmsSet.setDestAddrTon(ta.getAddrTon());
									receipt.setSmsSet(backSmsSet);
									receipt.setStored(true);
									pers.c2_scheduleMessage(receipt);
								}
							}
						} finally {
							SmsSetCashe.getInstance().removeSmsSet(lock);
						}
					}
				}
			} catch (PersistenceException e1) {
				this.logger.severe(
						"PersistenceException when archiveDeliveredSms() in RxSmppServerSbb.onDeliverSmResp(): "
								+ e1.getMessage(), e1);
				// we do not "return" here because even if storing into
				// archive database is failed
				// we will continue delivering process
			}

			TargetAddress lock = pers.obtainSynchroObject(new TargetAddress(smsSet));
			try {
				synchronized (lock) {
					// now we are trying to sent other messages
					if (currentMsgNum < smsSet.getSmsCount() - 1) {
						// there are more messages to send in cache
						currentMsgNum++;
						this.setCurrentMsgNum(currentMsgNum);
						sms = smsSet.getSms(currentMsgNum);
						if (sms != null) {
							if (smscPropertiesManagement.getDatabaseType() == DatabaseType.Cassandra_1) {
								this.startMessageDelivery(sms);
							} else {
								sms.setDeliveryCount(sms.getDeliveryCount() + 1);
							}
						}

						try {
							this.sendMessage(smsSet);
							return;
						} catch (SmscProcessingException e) {
							String s = "SmscProcessingException when sending initial sendDeliverSm()=" + e.getMessage()
									+ ", Message=" + sms;
							logger.severe(s, e);
						}
					}

					// no more messages are in cache now - lets check if
					// there
					// are more messages in a database
					if (smscPropertiesManagement.getDatabaseType() == DatabaseType.Cassandra_1) {
						try {
							pers.fetchSchedulableSms(smsSet, false);
						} catch (PersistenceException e1) {
							this.logger.severe(
									"PersistenceException when invoking fetchSchedulableSms(smsSet) from RxSmppServerSbb.onDeliverSmResp(): "
											+ e1.toString(), e1);
						}
						if (smsSet.getSmsCount() > 0) {
							// there are more messages in a database - start
							// delivering of those messages
							currentMsgNum = 0;
							this.setCurrentMsgNum(currentMsgNum);

							try {
								this.sendMessage(smsSet);
								return;
							} catch (SmscProcessingException e) {
								String s = "SmscProcessingException when sending initial sendDeliverSm()="
										+ e.getMessage() + ", Message=" + sms;
								logger.severe(s, e);
							}
						}
					} else {
					}

					// no more messages to send - remove smsSet
					this.freeSmsSetSucceded(smsSet, pers);
				}
			} finally {
				pers.releaseSynchroObject(lock);
			}

		} catch (Throwable e1) {
			logger.severe("Exception in RxSmppServerSbb.onDeliverSmResp() when fetching records and issuing events: "
					+ e1.getMessage(), e1);
		}
	}

	public void onTRYING(javax.sip.ResponseEvent event, ActivityContextInterface aci) {
		if (this.logger.isFineEnabled()) {
			this.logger.fine("onTRYING " + event);
		}
	}

	public void onPROVISIONAL(javax.sip.ResponseEvent event, ActivityContextInterface aci) {
		if (this.logger.isFineEnabled()) {
			this.logger.fine("onPROVISIONAL " + event);
		}
	}

	public void onREDIRECT(javax.sip.ResponseEvent event, ActivityContextInterface aci) {
		this.logger.warning("onREDIRECT " + event);
	}

	public void onGLOBAL_FAILURE(javax.sip.ResponseEvent event, ActivityContextInterface aci) {
		this.logger.severe("onGLOBAL_FAILURE " + event);

		String targetId = this.getTargetId();
		if (targetId == null) {
			this.logger.severe("onGLOBAL_FAILURE but there is no TargetId CMP!");
			return;
		}
		SmsSet smsSet = SmsSetCashe.getInstance().getProcessingSmsSet(targetId);

		if (smsSet == null) {
			logger.severe("onGLOBAL_FAILURE but CMP smsSet is missed, targetId=" + targetId);
			return;
		}

		// TODO : Is GLOBAL FAILURE PERMANENT?
		this.onDeliveryError(smsSet, ErrorAction.permanentFailure, ErrorCode.SC_SYSTEM_ERROR,
				"SIP Exception GLOBAL_FAILURE received. Reason : " + event.getResponse().getReasonPhrase()
						+ " Status Code : " + event.getResponse().getStatusCode());
	}

	public void onTRANSACTION_TIMEOUT(javax.sip.TimeoutEvent event, ActivityContextInterface aci) {
		this.logger.warning("onTRANSACTION_TIMEOUT " + event);

		String targetId = this.getTargetId();
		if (targetId == null) {
			this.logger.severe("onTRANSACTION_TIMEOUT but there is no TargetId CMP!");
			return;
		}
		SmsSet smsSet = SmsSetCashe.getInstance().getProcessingSmsSet(targetId);

		if (smsSet == null) {
			logger.severe("onTRANSACTION_TIMEOUT but CMP smsSet is missed, targetId=" + targetId);
			return;
		}

		this.onDeliveryError(smsSet, ErrorAction.temporaryFailure, ErrorCode.SC_SYSTEM_ERROR,
				"SIP Exception TRANSACTION_TIMEOUT received.");
	}

	/**
	 * CMPs
	 */
	public abstract void setTargetId(String targetId);

	public abstract String getTargetId();

	public abstract void setCurrentMsgNum(int currentMsgNum);

	public abstract int getCurrentMsgNum();

	/**
	 * Private methods
	 */
	private void sendMessage(SmsSet smsSet) throws SmscProcessingException {

		int currentMsgNum = this.getCurrentMsgNum();
		Sms sms = smsSet.getSms(currentMsgNum);

		try {

			// TODO: let make here a special check if SIP is in a good state
			// if not - skip sending and set temporary error

			String fromAddressStr = sms.getSourceAddr();
			String toAddressStr = smsSet.getDestAddr();

			Sip sip = sipManagement.getSipByName(SipManagement.SIP_NAME);

			ListeningPoint listeningPoint = sipRA.getListeningPoints()[0];

			SipURI fromAddressUri = addressFactory.createSipURI(fromAddressStr, listeningPoint.getIPAddress() + ":"
					+ listeningPoint.getPort());
			javax.sip.address.Address fromAddress = addressFactory.createAddress(fromAddressUri);
			FromHeader fromHeader = headerFactory.createFromHeader(fromAddress, null);

			SipURI toAddressUri = addressFactory.createSipURI(toAddressStr, sip.getSipAddress());
			javax.sip.address.Address toAddress = addressFactory.createAddress(toAddressUri);
			ToHeader toHeader = headerFactory.createToHeader(toAddress, null);

			List<ViaHeader> viaHeaders = new ArrayList<ViaHeader>(1);

			ViaHeader viaHeader = headerFactory.createViaHeader(listeningPoint.getIPAddress(),
					listeningPoint.getPort(), listeningPoint.getTransport(), null);
			viaHeaders.add(viaHeader);

			ContentTypeHeader contentTypeHeader = headerFactory.createContentTypeHeader("text", "plain");
			CSeqHeader cSeqHeader = headerFactory.createCSeqHeader(2L, Request.MESSAGE);
			MaxForwardsHeader maxForwardsHeader = headerFactory.createMaxForwardsHeader(70);

			CallIdHeader callId = this.sipRA.getNewCallId();

			// create request
			Request request = messageFactory.createRequest(toAddressUri, Request.MESSAGE, callId, cSeqHeader,
					fromHeader, toHeader, viaHeaders, maxForwardsHeader, contentTypeHeader, sms.getShortMessage());

			// create client transaction and send request
			ClientTransaction clientTransaction = sipRA.getNewClientTransaction(request);

			ActivityContextInterface sipClientTxaci = this.sipACIFactory.getActivityContextInterface(clientTransaction);
			sipClientTxaci.attach(this.sbbContext.getSbbLocalObject());

			clientTransaction.sendRequest();
		} catch (Exception e) {
			throw new SmscProcessingException(
					"RxSipServerSbb.sendMessage(): Exception while trying to send SIP Message =" + e.getMessage()
							+ "\nMessage: " + sms, 0, 0, null, e);
		}
	}

	private byte[] recodeShortMessage(int esmeClass, int dataCoding, byte[] msg) {
		DataCodingScheme dataCodingScheme = new DataCodingSchemeImpl(dataCoding);
		boolean udhPresent = (esmeClass & SmppConstants.ESM_CLASS_UDHI_MASK) != 0;
		if (smscPropertiesManagement.getSmppEncodingForUCS2() == SmppEncodingForUCS2.Utf8
				&& dataCodingScheme.getCharacterSet() == CharacterSet.UCS2) {
			byte[] textPart = msg;
			byte[] udhData = null;
			if (udhPresent && msg.length > 2) {
				// UDH exists
				int udhLen = (msg[0] & 0xFF) + 1;
				if (udhLen <= msg.length) {
					textPart = new byte[msg.length - udhLen];
					udhData = new byte[udhLen];
					System.arraycopy(msg, udhLen, textPart, 0, textPart.length);
					System.arraycopy(msg, 0, udhData, 0, udhLen);
				}
			}

			Charset ucs2Charset = Charset.forName("UTF-16BE");
			ByteBuffer bb = ByteBuffer.wrap(textPart);
			CharBuffer cb = ucs2Charset.decode(bb);
			Charset utf8Charset = Charset.forName("UTF-8");
			ByteBuffer bf2 = utf8Charset.encode(cb);
			if (udhData != null) {
				msg = new byte[udhData.length + bf2.limit()];
				System.arraycopy(udhData, 0, msg, 0, udhData.length);
				bf2.get(msg, udhData.length, bf2.limit());
			} else {
				msg = new byte[bf2.limit()];
				bf2.get(msg);
			}
		}
		return msg;
	}

	/**
	 * remove smsSet from LIVE database after all messages has been delivered
	 * 
	 * @param smsSet
	 */
	private void freeSmsSetSucceded(SmsSet smsSet, PersistenceRAInterface pers) {

		try {
			if (smscPropertiesManagement.getDatabaseType() == DatabaseType.Cassandra_1) {
				Date lastDelivery = new Date();
				pers.setDeliverySuccess(smsSet, lastDelivery);

				if (!pers.deleteSmsSet(smsSet)) {
					pers.setNewMessageScheduled(smsSet, MessageUtil.computeDueDate(MessageUtil.computeFirstDueDelay()));
				}
			} else {
				smsSet.setStatus(ErrorCode.SUCCESS);
				SmsSetCashe.getInstance().removeProcessingSmsSet(smsSet.getTargetId());
			}
		} catch (PersistenceException e) {
			this.logger.severe("PersistenceException when freeSmsSetSucceded(SmsSet smsSet)" + e.getMessage(), e);
		}

		this.decrementDeliveryActivityCount();
	}

	private void onDeliveryError(SmsSet smsSet, ErrorAction errorAction, ErrorCode smStatus, String reason) {
		int currentMsgNum = this.getCurrentMsgNum();
		Sms smsa = smsSet.getSms(currentMsgNum);
		if (smsa != null) {
			String s1 = reason.replace("\n", "\t");
			CdrGenerator.generateCdr(smsa, CdrGenerator.CDR_TEMP_FAILED_SIP, s1,
					smscPropertiesManagement.getGenerateReceiptCdr());
		}

		PersistenceRAInterface pers = this.getStore();
		ArrayList<Sms> lstFailured = new ArrayList<Sms>();

		TargetAddress lock = pers.obtainSynchroObject(new TargetAddress(smsSet));
		synchronized (lock) {
			try {
				Date curDate = new Date();
				try {
					if (smscPropertiesManagement.getDatabaseType() == DatabaseType.Cassandra_1) {
						pers.setDeliveryFailure(smsSet, smStatus, curDate);
					} else {
						smsSet.setStatus(smStatus);
						SmsSetCashe.getInstance().removeProcessingSmsSet(smsSet.getTargetId());
					}
					this.decrementDeliveryActivityCount();

					// first of all we are removing messages that delivery
					// period is over
					if (smscPropertiesManagement.getDatabaseType() == DatabaseType.Cassandra_1) {
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
					} else {
					}

					switch (errorAction) {
					case temporaryFailure:
						this.rescheduleSmsSet(smsSet, pers, currentMsgNum, lstFailured);
						break;

					case permanentFailure:
						int smsCnt = smsSet.getSmsCount();
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
					this.logger.severe("PersistenceException when RxSmppServerSbb.onDeliveryError()" + e.getMessage(),
							e);
				}

			} finally {
				pers.releaseSynchroObject(lock);
			}
		}

		for (Sms sms : lstFailured) {
			CdrGenerator.generateCdr(sms, CdrGenerator.CDR_FAILED_SIP, reason,
					smscPropertiesManagement.getGenerateReceiptCdr());

			// adding an error receipt if it is needed
			if (sms.getStored()) {
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
									pers.setNewMessageScheduled(receipt.getSmsSet(),
											MessageUtil.computeDueDate(MessageUtil.computeFirstDueDelay()));
								} else {
									receipt = MessageUtil.createReceiptSms(sms, false);
									SmsSet backSmsSet = new SmsSet();
									backSmsSet.setDestAddr(ta.getAddr());
									backSmsSet.setDestAddrNpi(ta.getAddrNpi());
									backSmsSet.setDestAddrTon(ta.getAddrTon());
									receipt.setSmsSet(backSmsSet);
									receipt.setStored(true);
									pers.c2_scheduleMessage(receipt);
								}
								this.logger.info("Adding an error receipt: source=" + receipt.getSourceAddr()
										+ ", dest=" + receipt.getSmsSet().getDestAddr());
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
		}
	}

	/**
	 * Mark a message that its delivery has been started
	 * 
	 * @param sms
	 */
	private void startMessageDelivery(Sms sms) {

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
	private void freeSmsSetFailured(SmsSet smsSet, PersistenceRAInterface pers, int currentMsgNum) {

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
					this.logger.severe("PersistenceException when RxSmppServerSbb.freeSmsSetFailured(SmsSet smsSet)"
							+ e.getMessage(), e);
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
	private void rescheduleSmsSet(SmsSet smsSet, PersistenceRAInterface pers, int currentMsgNum,
			ArrayList<Sms> lstFailured) {

		TargetAddress lock = pers.obtainSynchroObject(new TargetAddress(smsSet));
		try {
			synchronized (lock) {

				try {
					int prevDueDelay = smsSet.getDueDelay();
					int newDueDelay = MessageUtil.computeNextDueDelay(prevDueDelay);

					Date newDueDate = new Date(new Date().getTime() + newDueDelay * 1000);

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
								pers.c2_scheduleMessage(sms, dueSlot, lstFailured);
							}
						}
					}
				} catch (PersistenceException e) {
					this.logger.severe(
							"PersistenceException when RxSmppServerSbb.rescheduleSmsSet(SmsSet smsSet)"
									+ e.getMessage(), e);
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
			this.logger = this.sbbContext.getTracer(getClass().getSimpleName());

			this.persistence = (PersistenceRAInterface) this.sbbContext.getResourceAdaptorInterface(PERSISTENCE_ID,
					PERSISTENCE_LINK);
			this.scheduler = (SchedulerRaSbbInterface) this.sbbContext.getResourceAdaptorInterface(SCHEDULE_ID,
					SCHEDULE_LINK);

			// get SIP stuff
			this.sipRA = (SleeSipProvider) this.sbbContext.getResourceAdaptorInterface(SIP_RA_TYPE_ID, SIP_RA_LINK);
			this.sipACIFactory = (SipActivityContextInterfaceFactory) this.sbbContext
					.getActivityContextInterfaceFactory(SIP_RA_TYPE_ID);

			this.messageFactory = this.sipRA.getMessageFactory();
			this.headerFactory = this.sipRA.getHeaderFactory();
			this.addressFactory = this.sipRA.getAddressFactory();

		} catch (Exception ne) {
			logger.severe("Could not set SBB context:", ne);
		}
	}

	@Override
	public void unsetSbbContext() {
		// TODO Auto-generated method stub

	}

	private void decrementDeliveryActivityCount() {
		try {
			this.getSchedulerActivity().endActivity();
		} catch (Exception e) {
			this.logger.severe("Error while decrementing DeliveryActivityCount", e);
		}
	}

	private SchedulerActivity getSchedulerActivity() {
		ActivityContextInterface[] acis = this.sbbContext.getActivities();
		for (int count = 0; count < acis.length; count++) {
			ActivityContextInterface aci = acis[count];
			Object activity = aci.getActivity();
			if (activity instanceof SchedulerActivity) {
				return (SchedulerActivity) activity;
			}
		}

		return null;
	}

}