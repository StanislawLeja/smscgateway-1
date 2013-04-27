/*
 * TeleStax, Open Source Cloud Communications  
 * Copyright 2012, Telestax Inc and individual contributors
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

package org.mobicents.smsc.slee.services.smpp.server.tx;

import java.sql.Timestamp;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.slee.ActivityContextInterface;
import javax.slee.CreateException;
import javax.slee.EventContext;
import javax.slee.RolledBackContext;
import javax.slee.SLEEException;
import javax.slee.Sbb;
import javax.slee.SbbContext;
import javax.slee.TransactionRequiredLocalException;
import javax.slee.facilities.Tracer;

import org.mobicents.protocols.ss7.map.api.errors.MAPErrorCode;
import org.mobicents.slee.ChildRelationExt;
import org.mobicents.slee.SbbContextExt;
import org.mobicents.smsc.slee.resources.smpp.server.SmppSessions;
import org.mobicents.smsc.slee.resources.smpp.server.SmppTransaction;
import org.mobicents.smsc.slee.resources.smpp.server.SmppTransactionACIFactory;
import org.mobicents.smsc.slee.resources.smpp.server.events.PduRequestTimeout;
import org.mobicents.smsc.slee.services.persistence.MessageUtil;
import org.mobicents.smsc.slee.services.persistence.Persistence;
import org.mobicents.smsc.slee.services.persistence.PersistenceException;
import org.mobicents.smsc.slee.services.persistence.SmppExtraConstants;
import org.mobicents.smsc.slee.services.persistence.Sms;
import org.mobicents.smsc.slee.services.persistence.SmsSet;
import org.mobicents.smsc.slee.services.persistence.SmscPocessingException;
import org.mobicents.smsc.slee.services.persistence.TargetAddress;
import org.mobicents.smsc.smpp.Esme;
import org.mobicents.smsc.smpp.SmscPropertiesManagement;

import com.cloudhopper.smpp.SmppConstants;
import com.cloudhopper.smpp.pdu.BaseSm;
import com.cloudhopper.smpp.pdu.DataSmResp;
import com.cloudhopper.smpp.pdu.SubmitSmResp;
import com.cloudhopper.smpp.tlv.Tlv;
import com.cloudhopper.smpp.tlv.TlvConvertException;
import com.cloudhopper.smpp.type.RecoverablePduException;
import com.cloudhopper.smpp.util.TlvUtil;

/**
 * 
 * @author amit bhayani
 * @author servey vetyutnev
 * 
 */
public abstract class TxSmppServerSbb implements Sbb {
    private static final SmscPropertiesManagement smscPropertiesManagement = SmscPropertiesManagement.getInstance();

	protected Tracer logger;
	private SbbContextExt sbbContext;

	private SmppTransactionACIFactory smppServerTransactionACIFactory = null;
	protected SmppSessions smppServerSessions = null;

	public TxSmppServerSbb() {
		// TODO Auto-generated constructor stub
	}

	// -------------------------------------------------------------
    // Child relations
    // -------------------------------------------------------------
	public abstract ChildRelationExt getStoreSbb();


	public Persistence getStore() throws TransactionRequiredLocalException, SLEEException, CreateException {
		ChildRelationExt childRelation = getStoreSbb();
		Persistence persistence = (Persistence) childRelation.get(ChildRelationExt.DEFAULT_CHILD_NAME);
		if (persistence == null) {
			persistence = (Persistence) childRelation.create(ChildRelationExt.DEFAULT_CHILD_NAME);
		}
		return persistence;
	}

	/**
	 * Event Handlers
	 */

	public void onSubmitSm(com.cloudhopper.smpp.pdu.SubmitSm event, ActivityContextInterface aci) {
		SmppTransaction smppServerTransaction = (SmppTransaction) aci.getActivity();
		Esme esme = smppServerTransaction.getEsme();
		String esmeName = esme.getName();

		if (this.logger.isInfoEnabled()) {
			this.logger.info("Received SUBMIT_SM = " + event + " from Esme name=" + esmeName);
		}

		Sms sms;
		try {
			TargetAddress ta = createDestTargetAddress(event);
			Persistence store = obtainStore(ta);
			TargetAddress lock = store.obtainSynchroObject(ta);

			try {
				synchronized (lock) {
					sms = this.createSmsEvent(event, esme, ta, store);
					this.processSms(sms, store);
				}
			} finally {
				store.releaseSynchroObject(lock);
			}
		} catch (SmscPocessingException e1) {
			this.logger.severe(e1.getMessage(), e1);

			SubmitSmResp response = event.createResponse();
			response.setCommandStatus(e1.getSmppErrorCode());
			String s = e1.getMessage();
			if (s != null) {
				if (s.length() > 255)
					s = s.substring(0, 255);
				Tlv tlv;
				try {
					tlv = TlvUtil.createNullTerminatedStringTlv(SmppConstants.TAG_ADD_STATUS_INFO, s);
					response.addOptionalParameter(tlv);
				} catch (TlvConvertException e) {
					this.logger.severe("TlvConvertException while storing TAG_ADD_STATUS_INFO Tlv parameter", e);
				}
			}

			// Lets send the Response with error here
			try {
				this.smppServerSessions.sendResponsePdu(esme, event, response);
			} catch (Exception e) {
				this.logger.severe("Error while trying to send SubmitSmResponse=" + response, e);
			}

			return;
		} catch (Throwable e1) {
			String s = "Exception when processing SubmitSm message: " + e1.getMessage();
			this.logger.severe(s, e1);

			SubmitSmResp response = event.createResponse();
			response.setCommandStatus(SmppConstants.STATUS_SYSERR);
			if (s.length() > 255)
				s = s.substring(0, 255);
			Tlv tlv;
			try {
				tlv = TlvUtil.createNullTerminatedStringTlv(SmppConstants.TAG_ADD_STATUS_INFO, s);
				response.addOptionalParameter(tlv);
			} catch (TlvConvertException e) {
				this.logger.severe("TlvConvertException while storing TAG_ADD_STATUS_INFO Tlv parameter", e);
			}

			// Lets send the Response with error here
			try {
				this.smppServerSessions.sendResponsePdu(esme, event, response);
			} catch (Exception e) {
				this.logger.severe("Error while trying to send SubmitSmResponse=" + response, e);
			}
			
			return;
		}

		SubmitSmResp response = event.createResponse();
		response.setMessageId(((Long) sms.getMessageId()).toString());

		// Lets send the Response with success here
		try {
			this.smppServerSessions.sendResponsePdu(esme, event, response);
		} catch (Throwable e) {
			this.logger.severe("Error while trying to send SubmitSmResponse=" + response, e);
		}
	}

	private Persistence obtainStore(TargetAddress ta) throws SmscPocessingException {
		Persistence store;
		try {
			store = this.getStore();
		} catch (TransactionRequiredLocalException e1) {
			throw new SmscPocessingException("TransactionRequiredLocalException when getting PersistenceSbb: " + ta.toString() + "\n" + e1.getMessage(),
					SmppConstants.STATUS_SYSERR, MAPErrorCode.systemFailure, null, e1);
		} catch (SLEEException e1) {
			throw new SmscPocessingException("SLEEException when reading SmsSet when getting PersistenceSbb: " + ta.toString() + "\n" + e1.getMessage(),
					SmppConstants.STATUS_SYSERR, MAPErrorCode.systemFailure, null, e1);
		} catch (CreateException e1) {
			throw new SmscPocessingException("CreateException when reading SmsSet when getting PersistenceSbb: " + ta.toString() + "\n" + e1.getMessage(),
					SmppConstants.STATUS_SYSERR, MAPErrorCode.systemFailure, null, e1);
		}
		return store;
	}

	public void onDataSm(com.cloudhopper.smpp.pdu.DataSm event, ActivityContextInterface aci) {
		SmppTransaction smppServerTransaction = (SmppTransaction) aci.getActivity();
		Esme esme = smppServerTransaction.getEsme();
		String esmeName = esme.getName();

		if (this.logger.isInfoEnabled()) {
			this.logger.info("Received DATA_SM = " + event + " from Esme name=" + esmeName);
		}

		Sms sms;
		try {
			TargetAddress ta = createDestTargetAddress(event);
			Persistence store = obtainStore(ta);
			TargetAddress lock = store.obtainSynchroObject(ta);

			try {
				synchronized (lock) {
					sms = this.createSmsEvent(event, esme, ta, store);
					this.processSms(sms, store);
				}
			} finally {
				store.releaseSynchroObject(lock);
			}
		} catch (SmscPocessingException e1) {
			this.logger.severe(e1.getMessage(), e1);

			DataSmResp response = event.createResponse();
			response.setCommandStatus(e1.getSmppErrorCode());
			String s = e1.getMessage();
			if (s != null) {
				if (s.length() > 255)
					s = s.substring(0, 255);
				Tlv tlv;
				try {
					tlv = TlvUtil.createNullTerminatedStringTlv(SmppConstants.TAG_ADD_STATUS_INFO, s);
					response.addOptionalParameter(tlv);
				} catch (TlvConvertException e) {
					this.logger.severe("TlvConvertException while storing TAG_ADD_STATUS_INFO Tlv parameter", e);
				}
			}

			// Lets send the Response with error here
			try {
				this.smppServerSessions.sendResponsePdu(esme, event, response);
			} catch (Exception e) {
				this.logger.severe("Error while trying to send DataSmResponse=" + response, e);
			}
			
			return;
		} catch (Throwable e1) {
			String s = "Exception when processing dataSm message: " + e1.getMessage();
			this.logger.severe(s, e1);

			DataSmResp response = event.createResponse();
			response.setCommandStatus(SmppConstants.STATUS_SYSERR);
			if (s.length() > 255)
				s = s.substring(0, 255);
			Tlv tlv;
			try {
				tlv = TlvUtil.createNullTerminatedStringTlv(SmppConstants.TAG_ADD_STATUS_INFO, s);
				response.addOptionalParameter(tlv);
			} catch (TlvConvertException e) {
				this.logger.severe("TlvConvertException while storing TAG_ADD_STATUS_INFO Tlv parameter", e);
			}

			// Lets send the Response with error here
			try {
				this.smppServerSessions.sendResponsePdu(esme, event, response);
			} catch (Exception e) {
				this.logger.severe("Error while trying to send SubmitSmResponse=" + response, e);
			}
			
			return;
		}

		DataSmResp response = event.createResponse();
		response.setMessageId(((Long) sms.getMessageId()).toString());

		// Lets send the Response with success here
		try {
			this.smppServerSessions.sendResponsePdu(esme, event, response);
		} catch (Exception e) {
			this.logger.severe("Error while trying to send DataSmResponse=" + response, e);
		}
	}

	private TargetAddress createDestTargetAddress(BaseSm event) throws SmscPocessingException {
		if (event.getDestAddress() == null || event.getDestAddress().getAddress() == null || event.getDestAddress().getAddress().isEmpty()) {
			throw new SmscPocessingException("DestAddress digits are absent", SmppConstants.STATUS_INVDSTADR, MAPErrorCode.systemFailure, null);
		}
		int destTon, destNpi;
		switch (event.getDestAddress().getTon()) {
		case SmppConstants.TON_UNKNOWN:
			destTon = smscPropertiesManagement.getDefaultTon();
			break;
		case SmppConstants.TON_INTERNATIONAL:
			destTon = event.getDestAddress().getTon();
			break;
		default:
			throw new SmscPocessingException("DestAddress TON not supported: " + event.getDestAddress().getTon(), SmppConstants.STATUS_INVDSTTON,
					MAPErrorCode.systemFailure, null);
		}
		switch (event.getDestAddress().getNpi()) {
		case SmppConstants.NPI_UNKNOWN:
			destNpi = smscPropertiesManagement.getDefaultNpi();
			break;
		case SmppConstants.NPI_E164:
			destNpi = event.getDestAddress().getNpi();
			break;
		default:
			throw new SmscPocessingException("DestAddress NPI not supported: " + event.getDestAddress().getNpi(), SmppConstants.STATUS_INVDSTNPI,
					MAPErrorCode.systemFailure, null);
		}

		TargetAddress ta = new TargetAddress(destTon, destNpi, event.getDestAddress().getAddress());
		return ta;
	}

	public void onDeliverSm(com.cloudhopper.smpp.pdu.DeliverSm event, ActivityContextInterface aci) {
		logger.severe(String.format("onDeliverSm : this must not be", event));

		SmppTransaction smppServerTransaction = (SmppTransaction) aci.getActivity();
		Esme esme = smppServerTransaction.getEsme();
		String esmeName = esme.getName();

		if (this.logger.isInfoEnabled()) {
			this.logger.info("Received DELIVER_SM = " + event + " from Esme name=" + esmeName);
		}

//		SmsEvent smsEvent = this.createSmsEvent(event);
//		this.processSms(smsEvent);
//
//		DeliverSmResp response = event.createResponse();
//		response.setMessageId(smsEvent.getMessageId());
//		// Lets send the Response here
//		try {
//			this.smppServerSessions.sendResponsePdu(esme, event, response);
//		} catch (Exception e) {
//			this.logger.severe("Error while trying to send DeliverSmResp=" + response, e);
//		}
	}

	public void onPduRequestTimeout(PduRequestTimeout event, ActivityContextInterface aci, EventContext eventContext) {
		logger.severe(String.format("onPduRequestTimeout : PduRequestTimeout=%s", event));
		// TODO : Handle this
	}

	public void onRecoverablePduException(RecoverablePduException event, ActivityContextInterface aci,
			EventContext eventContext) {
		logger.severe(String.format("onRecoverablePduException : RecoverablePduException=%s", event));
		// TODO : Handle this
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
		} catch (Exception ne) {
			logger.severe("Could not set SBB context:", ne);
		}
	}

	@Override
	public void unsetSbbContext() {
		// TODO Auto-generated method stub

	}

	private Sms createSmsEvent(BaseSm event, Esme origEsme, TargetAddress ta, Persistence store) throws SmscPocessingException {

		Sms sms = new Sms();
		sms.setDbId(UUID.randomUUID());

		// checking parameters first
		if (event.getSourceAddress() == null || event.getSourceAddress().getAddress() == null || event.getDestAddress().getAddress().isEmpty()) {
			throw new SmscPocessingException("SourceAddress digits are absent", SmppConstants.STATUS_INVSRCADR, MAPErrorCode.systemFailure, null);
		}
		sms.setSourceAddr(event.getSourceAddress().getAddress());
		switch(event.getSourceAddress().getTon()){
		case SmppConstants.TON_UNKNOWN:
			sms.setSourceAddrTon(smscPropertiesManagement.getDefaultTon());
			break;
		case SmppConstants.TON_INTERNATIONAL:
			sms.setSourceAddrTon(event.getSourceAddress().getTon());
			break;
		default:
			throw new SmscPocessingException("SourceAddress TON not supported: " + event.getSourceAddress().getTon(), SmppConstants.STATUS_INVSRCTON,
					MAPErrorCode.systemFailure, null);
		}
		switch (event.getSourceAddress().getNpi()) {
		case SmppConstants.NPI_UNKNOWN:
			sms.setSourceAddrNpi(smscPropertiesManagement.getDefaultNpi());
			break;
		case SmppConstants.NPI_E164:
			sms.setSourceAddrNpi(event.getSourceAddress().getNpi());
			break;
		default:
			throw new SmscPocessingException("SourceAddress NPI not supported: " + event.getSourceAddress().getNpi(), SmppConstants.STATUS_INVSRCNPI,
					MAPErrorCode.systemFailure, null);
		}

		int dcs = event.getDataCoding();
		if (dcs != 0 && dcs != 8) {
			throw new SmscPocessingException("DataCoding scheme does not supported (only 0 an 8 is supported): " + dcs, SmppExtraConstants.ESME_RINVDCS,
					MAPErrorCode.systemFailure, null);
		}
		sms.setDataCoding(dcs);

		sms.setOrigSystemId(origEsme.getSystemId());
		sms.setOrigEsmeName(origEsme.getName());

		sms.setSubmitDate(new Timestamp(System.currentTimeMillis()));

		sms.setServiceType(event.getServiceType());
		sms.setEsmClass(event.getEsmClass());
		sms.setProtocolId(event.getProtocolId());
		sms.setPriority(event.getPriority());
		sms.setRegisteredDelivery(event.getRegisteredDelivery());
		sms.setReplaceIfPresent(event.getReplaceIfPresent());
		sms.setDefaultMsgId(event.getDefaultMsgId());

		// short message data
		sms.setShortMessage(event.getShortMessage());
		if (event.getShortMessageLength() == 0) {
			// Probably the message_payload Optional Parameter is being used
			Tlv messagePaylod = event.getOptionalParameter(SmppConstants.TAG_MESSAGE_PAYLOAD);
			if (messagePaylod != null) {
				sms.setShortMessage(messagePaylod.getValue());
			}
		}

		// ValidityPeriod processing
		Tlv tlvQosTimeToLive = sms.getTlvSet().getOptionalParameter(SmppConstants.TAG_QOS_TIME_TO_LIVE);
		Date validityPeriod;
		if (tlvQosTimeToLive != null) {
			long valTime;
			try {
				valTime = (new Date()).getTime() + tlvQosTimeToLive.getValueAsInt();
			} catch (TlvConvertException e) {
				throw new SmscPocessingException("TlvConvertException when getting TAG_QOS_TIME_TO_LIVE tlv field: " + e.getMessage(),
						SmppConstants.STATUS_INVOPTPARAMVAL,MAPErrorCode.systemFailure, null, e);
			}
			validityPeriod = new Date(valTime);
		} else {
			try {
				validityPeriod = MessageUtil.parseSmppDate(event.getValidityPeriod());
			} catch (ParseException e) {
				throw new SmscPocessingException("ParseException when parsing ValidityPeriod field: " + e.getMessage(),
						SmppConstants.STATUS_INVEXPIRY,MAPErrorCode.systemFailure, null, e);
			}
		}
		MessageUtil.applyValidityPeriod(sms, validityPeriod, true);

		// ScheduleDeliveryTime processing
		Date scheduleDeliveryTime;
		try {
			scheduleDeliveryTime = MessageUtil.parseSmppDate(event.getScheduleDeliveryTime());
		} catch (ParseException e) {
			throw new SmscPocessingException("ParseException when parsing ScheduleDeliveryTime field: " + e.getMessage(),
					SmppConstants.STATUS_INVSCHED,MAPErrorCode.systemFailure, null, e);
		}
		MessageUtil.applyScheduleDeliveryTime(sms, scheduleDeliveryTime);

		// storing additional parameters
		ArrayList<Tlv> optionalParameters = event.getOptionalParameters();
		if (optionalParameters != null && optionalParameters.size() > 0) {
			for (Tlv tlv : optionalParameters) {
				if (tlv.getTag() != SmppConstants.TAG_MESSAGE_PAYLOAD) {
					sms.getTlvSet().addOptionalParameter(tlv);
				}
			}
		}

		SmsSet smsSet;
		try {
			smsSet = store.obtainSmsSet(ta);
		} catch (PersistenceException e1) {
			throw new SmscPocessingException("PersistenceException when reading SmsSet from a database: " + ta.toString() + "\n" + e1.getMessage(),
					SmppConstants.STATUS_SUBMITFAIL, MAPErrorCode.systemFailure, null, e1);
		}
		sms.setSmsSet(smsSet);

		long messageId = this.smppServerSessions.getNextMessageId();
		sms.setMessageId(messageId);

		// TODO: process case when event.getReplaceIfPresent()==true: we need remove old message with same MessageId ?

		return sms;
	}

	private void processSms(Sms sms, Persistence store) throws SmscPocessingException {
		try {
			store.createLiveSms(sms);
			if (sms.getScheduleDeliveryTime() == null)
				store.setNewMessageScheduled(sms.getSmsSet(), MessageUtil.computeDueDate(MessageUtil.computeFirstDueDelay()));
			else
				store.setNewMessageScheduled(sms.getSmsSet(), sms.getScheduleDeliveryTime());
		} catch (PersistenceException e) {
			throw new SmscPocessingException("PersistenceException when storing LIVE_SMS : " + e.getMessage(), SmppConstants.STATUS_SUBMITFAIL,
					MAPErrorCode.systemFailure, null, e);
		}
	}
}

