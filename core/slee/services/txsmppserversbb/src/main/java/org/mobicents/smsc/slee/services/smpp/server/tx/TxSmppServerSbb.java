package org.mobicents.smsc.slee.services.smpp.server.tx;

import java.sql.Timestamp;
import java.util.ArrayList;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.slee.ActivityContextInterface;
import javax.slee.CreateException;
import javax.slee.EventContext;
import javax.slee.RolledBackContext;
import javax.slee.Sbb;
import javax.slee.SbbContext;
import javax.slee.facilities.ActivityContextNamingFacility;
import javax.slee.facilities.NameAlreadyBoundException;
import javax.slee.facilities.Tracer;
import javax.slee.nullactivity.NullActivity;

import org.mobicents.slee.SbbContextExt;
import org.mobicents.smsc.slee.resources.smpp.server.SmppSessions;
import org.mobicents.smsc.slee.resources.smpp.server.SmppTransaction;
import org.mobicents.smsc.slee.resources.smpp.server.SmppTransactionACIFactory;
import org.mobicents.smsc.slee.resources.smpp.server.events.PduRequestTimeout;
import org.mobicents.smsc.slee.services.persistence.Sms;
import org.mobicents.smsc.smpp.Esme;

import com.cloudhopper.smpp.SmppConstants;
import com.cloudhopper.smpp.pdu.DataSmResp;
import com.cloudhopper.smpp.pdu.SubmitSmResp;
import com.cloudhopper.smpp.tlv.Tlv;
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

		String messageId = this.smppServerSessions.getNextMessageId();

		Sms smsEvent = new Sms();
		smsEvent.setSubmitDate(new Timestamp(System.currentTimeMillis()));
		smsEvent.setMessageId(messageId);

		// TODO Change API to esmeName from SystemId
		smsEvent.setOrigSystemId(esmeName);

		smsEvent.setSourceAddrTon(event.getSourceAddress().getTon());
		smsEvent.setSourceAddrNpi(event.getSourceAddress().getNpi());
		smsEvent.setSourceAddr(event.getSourceAddress().getAddress());

		// TODO : Normalise Dest Address
		smsEvent.setDestAddrTon(event.getDestAddress().getTon());
		smsEvent.setDestAddrNpi(event.getDestAddress().getNpi());
		smsEvent.setDestAddr(event.getDestAddress().getAddress());

		smsEvent.setEsmClass(event.getEsmClass());
		smsEvent.setProtocolId(event.getProtocolId());
		smsEvent.setPriority(event.getPriority());

		// TODO : respect schedule delivery
		smsEvent.setScheduleDeliveryTime(event.getScheduleDeliveryTime());

		// TODO : Check for validity period. If validity period null, set SMSC
		// default validity period
		smsEvent.setValidityPeriod(event.getValidityPeriod());
		smsEvent.setRegisteredDelivery(event.getRegisteredDelivery());

		// TODO : Respect replace if present
		smsEvent.setReplaceIfPresent(event.getReplaceIfPresent());
		smsEvent.setDataCoding(event.getDataCoding());
		smsEvent.setDefaultMsgId(event.getDefaultMsgId());
		smsEvent.setShortMessage(event.getShortMessage());

		if (event.getShortMessageLength() == 0) {
			// Probably the message_payload Optional Parameter is being used
			Tlv messagePaylod = event.getOptionalParameter(SmppConstants.TAG_MESSAGE_PAYLOAD);
			if (messagePaylod != null) {
				smsEvent.setShortMessage(messagePaylod.getValue());
			}
		}

		ArrayList<Tlv> optionalParameters = event.getOptionalParameters();
		if (optionalParameters != null && optionalParameters.size() > 0) {
			// Set it for our smsEvent
			smsEvent.addAllOptionalParameter(optionalParameters);
		}

		this.processSms(smsEvent);

		// Lets send the Response here
		SubmitSmResp response = event.createResponse();
		response.setMessageId(messageId);
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

		if (this.logger.isInfoEnabled()) {
			this.logger.info("Received DATA_SM = " + event + " from Esme name=" + esmeName);
		}

		DataSmResp response = event.createResponse();
		// Lets send the Response here
		try {
			this.smppServerSessions.sendResponsePdu(esme, event, response);
		} catch (Exception e) {
			this.logger.severe("Error while trying to send DataSmResponse=" + response, e);
		}
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

	public abstract void fireSms(Sms event, ActivityContextInterface aci, javax.slee.Address address);

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
	 * Sbb ACI
	 */
	public abstract TxSmppServerSbbActivityContextInterface asSbbActivityContextInterface(ActivityContextInterface aci);

	/**
	 * Private
	 */

	public void processSms(Sms event) {

		String destAddr = event.getDestAddr();

		ActivityContextNamingFacility activityContextNamingFacility = this.sbbContext
				.getActivityContextNamingFacility();

		ActivityContextInterface nullActivityContextInterface = null;
		try {
			nullActivityContextInterface = activityContextNamingFacility.lookup(destAddr);
		} catch (Exception e) {
			logger.severe(String.format(
					"Exception while lookup NullActivityContextInterface for jndi name=%s for SmsEvent=%s", destAddr,
					event), e);
		}

		NullActivity nullActivity = null;
		if (nullActivityContextInterface == null) {
			// If null means there are no SMS handled by Mt for this destination
			// address. Lets create new NullActivity and bind it to
			// naming-facility
			if (this.logger.isInfoEnabled()) {
				this.logger.info(String
						.format("lookup of NullActivityContextInterface returned null, create new NullActivity"));
			}

			nullActivity = this.sbbContext.getNullActivityFactory().createNullActivity();
			nullActivityContextInterface = this.sbbContext.getNullActivityContextInterfaceFactory()
					.getActivityContextInterface(nullActivity);

			try {
				activityContextNamingFacility.bind(nullActivityContextInterface, destAddr);
			} catch (NameAlreadyBoundException e) {
				// Kill existing nullActivity
				nullActivity.endActivity();

				// If name already bound, we do lookup again because this is one
				// of the race conditions
				try {
					nullActivityContextInterface = activityContextNamingFacility.lookup(destAddr);
				} catch (Exception ex) {
					logger.severe(
							String.format(
									"Exception while second lookup NullActivityContextInterface for jndi name=%s for SmsEvent=%s",
									destAddr, event), ex);
					// TODO take care of error conditions.
					return;
				}

			} catch (Exception e) {
				logger.severe(String.format(
						"Exception while binding NullActivityContextInterface to jndi name=%s for SmsEvent=%s",
						destAddr, event), e);

				if (nullActivity != null) {
					nullActivity.endActivity();
				}

				// TODO take care of error conditions.
				return;

			}
		}// if (nullActivityContextInterface == null)

		TxSmppServerSbbActivityContextInterface txSmppServerSbbActivityContextInterface = this
				.asSbbActivityContextInterface(nullActivityContextInterface);
		int pendingEventsOnNullActivity = txSmppServerSbbActivityContextInterface.getPendingEventsOnNullActivity();
		pendingEventsOnNullActivity = pendingEventsOnNullActivity + 1;

		if (this.logger.isInfoEnabled()) {
			this.logger.info(String.format("pendingEventsOnNullActivity = %d", pendingEventsOnNullActivity));
		}

		txSmppServerSbbActivityContextInterface.setPendingEventsOnNullActivity(pendingEventsOnNullActivity);
		// We have NullActivityContextInterface, lets fire SmsEvent on this
		this.fireSms(event, nullActivityContextInterface, null);

	}
}
