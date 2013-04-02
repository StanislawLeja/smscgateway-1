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

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.slee.ActivityContextInterface;
import javax.slee.CreateException;
import javax.slee.EventContext;
import javax.slee.RolledBackContext;
import javax.slee.Sbb;
import javax.slee.SbbContext;
import javax.slee.facilities.Tracer;

import org.mobicents.slee.SbbContextExt;
import org.mobicents.smsc.slee.resources.smpp.server.SmppSessions;
import org.mobicents.smsc.slee.resources.smpp.server.SmppTransaction;
import org.mobicents.smsc.slee.resources.smpp.server.SmppTransactionACIFactory;
import org.mobicents.smsc.slee.resources.smpp.server.events.PduRequestTimeout;
import org.mobicents.smsc.slee.services.persistence.Sms;
import org.mobicents.smsc.smpp.Esme;

import com.cloudhopper.smpp.pdu.SubmitSmResp;
import com.cloudhopper.smpp.type.RecoverablePduException;

public abstract class TxSmppServerSbb implements Sbb {

	private Tracer logger;
	private SbbContextExt sbbContext;

	private SmppTransactionACIFactory smppServerTransactionACIFactory = null;
	private SmppSessions smppServerSessions = null;

	public TxSmppServerSbb() {
		// TODO Auto-generated constructor stub
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

		//SmsEvent smsEvent = this.createSmsEvent(event);
		//this.processSms(smsEvent);

		// Lets send the Response here
		SubmitSmResp response = event.createResponse();
		//response.setMessageId(smsEvent.getMessageId());
		try {
			this.smppServerSessions.sendResponsePdu(esme, event, response);
		} catch (Exception e) {
			this.logger.severe("Error while trying to send SubmitSmResponse=" + response, e);
		}
	}

	public void onDataSm(com.cloudhopper.smpp.pdu.DataSm event, ActivityContextInterface aci) {
		SmppTransaction smppServerTransaction = (SmppTransaction) aci.getActivity();
		Esme esme = smppServerTransaction.getEsme();
		String esmeName = esme.getName();

//		if (this.logger.isInfoEnabled()) {
//			this.logger.info("Received DATA_SM = " + event + " from Esme name=" + esmeName);
//		}
//
//		SmsEvent smsEvent = this.createSmsEvent(event);
//		this.processSms(smsEvent);
//
//		DataSmResp response = event.createResponse();
//		response.setMessageId(smsEvent.getMessageId());
//		// Lets send the Response here
//		try {
//			this.smppServerSessions.sendResponsePdu(esme, event, response);
//		} catch (Exception e) {
//			this.logger.severe("Error while trying to send DataSmResponse=" + response, e);
//		}
	}

	public void onDeliverSm(com.cloudhopper.smpp.pdu.DeliverSm event, ActivityContextInterface aci) {
		SmppTransaction smppServerTransaction = (SmppTransaction) aci.getActivity();
		Esme esme = smppServerTransaction.getEsme();
		String esmeName = esme.getName();

//		if (this.logger.isInfoEnabled()) {
//			this.logger.info("Received DELIVER_SM = " + event + " from Esme name=" + esmeName);
//		}
//
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

	/**
	 * Private
	 */
//	private SmsEvent createSmsEvent(BaseSm event) {
//
//		String esmeClusterName = this.smppServerSessions.getEsmeClusterName(event.getDestAddress().getTon(), event
//				.getDestAddress().getNpi(), event.getDestAddress().getAddress());
//
//		String messageId = this.smppServerSessions.getNextMessageId();
//
//		SmsEvent smsEvent = new SmsEvent();
//		smsEvent.setSubmitDate(new Timestamp(System.currentTimeMillis()));
//		smsEvent.setMessageId(messageId);
//		smsEvent.setSystemId(esmeClusterName);
//
//		smsEvent.setSourceAddrTon(event.getSourceAddress().getTon());
//		smsEvent.setSourceAddrNpi(event.getSourceAddress().getNpi());
//		smsEvent.setSourceAddr(event.getSourceAddress().getAddress());
//
//		// TODO : Normalise Dest Address
//		smsEvent.setDestAddrTon(event.getDestAddress().getTon());
//		smsEvent.setDestAddrNpi(event.getDestAddress().getNpi());
//		smsEvent.setDestAddr(event.getDestAddress().getAddress());
//
//		smsEvent.setEsmClass(event.getEsmClass());
//		smsEvent.setProtocolId(event.getProtocolId());
//		smsEvent.setPriority(event.getPriority());
//
//		// TODO : respect schedule delivery
//		smsEvent.setScheduleDeliveryTime(event.getScheduleDeliveryTime());
//
//		// TODO : Check for validity period. If validity period null, set SMSC
//		// default validity period
//		smsEvent.setValidityPeriod(event.getValidityPeriod());
//		smsEvent.setRegisteredDelivery(event.getRegisteredDelivery());
//
//		// TODO : Respect replace if present
//		smsEvent.setReplaceIfPresent(event.getReplaceIfPresent());
//		smsEvent.setDataCoding(event.getDataCoding());
//		smsEvent.setDefaultMsgId(event.getDefaultMsgId());
//		smsEvent.setShortMessage(event.getShortMessage());
//
//		if (event.getShortMessageLength() == 0) {
//			// Probably the message_payload Optional Parameter is being used
//			Tlv messagePaylod = event.getOptionalParameter(SmppConstants.TAG_MESSAGE_PAYLOAD);
//			if (messagePaylod != null) {
//				smsEvent.setShortMessage(messagePaylod.getValue());
//			}
//		}
//
//		ArrayList<Tlv> optionalParameters = event.getOptionalParameters();
//		if (optionalParameters != null && optionalParameters.size() > 0) {
//			// Set it for our smsEvent
//			smsEvent.addAllOptionalParameter(optionalParameters);
//		}
//
//		return smsEvent;
//	}

//	public void processSms(SmsEvent event) {
//		// TODO Persist in Cassandra here
//
//	}
}
