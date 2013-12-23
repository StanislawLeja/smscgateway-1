package org.mobicents.smsc.slee.resources.smpp.server;

import java.util.Date;
import java.util.concurrent.atomic.AtomicLong;

import javax.slee.SLEEException;
import javax.slee.facilities.Tracer;
import javax.slee.resource.ActivityAlreadyExistsException;
import javax.slee.resource.StartActivityException;

import org.mobicents.smsc.slee.resources.smpp.server.events.EventsType;
import org.mobicents.smsc.slee.resources.smpp.server.events.PduRequestTimeout;
import org.mobicents.smsc.smpp.Esme;
import org.mobicents.smsc.smpp.SmppSessionHandlerInterface;
import org.mobicents.smsc.smpp.SmsRouteManagement;

import com.cloudhopper.commons.util.windowing.WindowFuture;
import com.cloudhopper.smpp.PduAsyncResponse;
import com.cloudhopper.smpp.SmppConstants;
import com.cloudhopper.smpp.SmppSessionHandler;
import com.cloudhopper.smpp.impl.DefaultSmppSession;
import com.cloudhopper.smpp.pdu.DataSm;
import com.cloudhopper.smpp.pdu.DataSmResp;
import com.cloudhopper.smpp.pdu.DeliverSm;
import com.cloudhopper.smpp.pdu.DeliverSmResp;
import com.cloudhopper.smpp.pdu.Pdu;
import com.cloudhopper.smpp.pdu.PduRequest;
import com.cloudhopper.smpp.pdu.PduResponse;
import com.cloudhopper.smpp.pdu.SubmitSm;
import com.cloudhopper.smpp.pdu.SubmitSmResp;
import com.cloudhopper.smpp.type.RecoverablePduException;
import com.cloudhopper.smpp.type.SmppChannelException;
import com.cloudhopper.smpp.type.SmppTimeoutException;
import com.cloudhopper.smpp.type.UnrecoverablePduException;

/**
 * 
 * @author Amit Bhayani
 * 
 */
public class SmppSessionsImpl implements SmppSessions {

	private static Tracer tracer;

	private SmppServerResourceAdaptor smppServerResourceAdaptor = null;

	protected SmppSessionHandlerInterfaceImpl smppSessionHandlerInterfaceImpl = null;

	private final AtomicLong messageIdGenerator = new AtomicLong(0);

	public SmppSessionsImpl(SmppServerResourceAdaptor smppServerResourceAdaptor) {
		this.smppServerResourceAdaptor = smppServerResourceAdaptor;
		if (tracer == null) {
			tracer = this.smppServerResourceAdaptor.getRAContext().getTracer(
					SmppSessionHandlerInterfaceImpl.class.getSimpleName());
		}
		this.smppSessionHandlerInterfaceImpl = new SmppSessionHandlerInterfaceImpl();

	}

	protected SmppSessionHandlerInterface getSmppSessionHandlerInterface() {
		return this.smppSessionHandlerInterfaceImpl;
	}

	@Override
	public SmppTransaction sendRequestPdu(Esme esme, PduRequest request, long timeoutMillis)
			throws RecoverablePduException, UnrecoverablePduException, SmppTimeoutException, SmppChannelException,
			InterruptedException, ActivityAlreadyExistsException, NullPointerException, IllegalStateException,
			SLEEException, StartActivityException {

		DefaultSmppSession defaultSmppSession = esme.getSmppSession();

		if (defaultSmppSession == null) {
			throw new NullPointerException("Underlying SmppSession is Null!");
		}

		if (!request.hasSequenceNumberAssigned()) {
			// assign the next PDU sequence # if its not yet assigned
			request.setSequenceNumber(defaultSmppSession.getSequenceNumber().next());
		}

		SmppTransactionHandle smppServerTransactionHandle = new SmppTransactionHandle(esme.getName(),
				request.getSequenceNumber(), SmppTransactionType.OUTGOING);

		SmppTransactionImpl smppServerTransaction = new SmppTransactionImpl(request, esme, smppServerTransactionHandle,
				smppServerResourceAdaptor);

		smppServerResourceAdaptor.startNewSmppTransactionSuspendedActivity(smppServerTransaction);

		try {
			WindowFuture<Integer, PduRequest, PduResponse> windowFuture = defaultSmppSession.sendRequestPdu(request,
					timeoutMillis, false);
		} catch (RecoverablePduException e) {
			this.smppServerResourceAdaptor.endActivity(smppServerTransaction);
			throw e;
		} catch (UnrecoverablePduException e) {
			this.smppServerResourceAdaptor.endActivity(smppServerTransaction);
			throw e;
		} catch (SmppTimeoutException e) {
			this.smppServerResourceAdaptor.endActivity(smppServerTransaction);
			throw e;
		} catch (SmppChannelException e) {
			this.smppServerResourceAdaptor.endActivity(smppServerTransaction);
			throw e;
		} catch (InterruptedException e) {
			this.smppServerResourceAdaptor.endActivity(smppServerTransaction);
			throw e;
		}

		return smppServerTransaction;
	}

	@Override
	public void sendResponsePdu(Esme esme, PduRequest request, PduResponse response) throws RecoverablePduException,
			UnrecoverablePduException, SmppChannelException, InterruptedException {

		SmppTransactionImpl smppServerTransactionImpl = (SmppTransactionImpl) request.getReferenceObject();

		try {
			DefaultSmppSession defaultSmppSession = esme.getSmppSession();

			if (defaultSmppSession == null) {
				throw new NullPointerException("Underlying SmppSession is Null!");
			}

			if (request.getSequenceNumber() != response.getSequenceNumber()) {
				throw new UnrecoverablePduException("Sequence number of response is not same as request");
			}
			defaultSmppSession.sendResponsePdu(response);
		} finally {
			if (smppServerTransactionImpl == null) {
				tracer.severe(String.format("SmppTransactionImpl Activity is null while trying to send PduResponse=%s",
						response));
			} else {
				this.smppServerResourceAdaptor.endActivity(smppServerTransactionImpl);
			}
		}
		
		//TODO Should it catch UnrecoverablePduException and SmppChannelException and close underlying SmppSession?
	}

	protected class SmppSessionHandlerInterfaceImpl implements SmppSessionHandlerInterface {

		public SmppSessionHandlerInterfaceImpl() {

		}

		@Override
		public SmppSessionHandler createNewSmppSessionHandler(Esme esme) {
			return new SmppSessionHandlerImpl(esme);
		}
	}

	protected class SmppSessionHandlerImpl implements SmppSessionHandler {
		private Esme esme;

		public SmppSessionHandlerImpl(Esme esme) {
			this.esme = esme;
		}

		@Override
		public PduResponse firePduRequestReceived(PduRequest pduRequest) {

			PduResponse response = pduRequest.createResponse();
			try {
				SmppTransactionImpl smppServerTransaction = null;
				SmppTransactionHandle smppServerTransactionHandle = null;
				switch (pduRequest.getCommandId()) {
				case SmppConstants.CMD_ID_ENQUIRE_LINK:
					break;
				case SmppConstants.CMD_ID_UNBIND:
					break;
				case SmppConstants.CMD_ID_SUBMIT_SM:
//                    // TODO remove it ...........................
//				    SubmitSm submitSm = (SubmitSm) pduRequest;
//				    Date dt = new Date();
//				    submitSm.setServiceType(dt.toGMTString());
//                    // TODO remove it ...........................

                    
                    smppServerTransactionHandle = new SmppTransactionHandle(this.esme.getName(),
							pduRequest.getSequenceNumber(), SmppTransactionType.INCOMING);
					smppServerTransaction = new SmppTransactionImpl(pduRequest, this.esme, smppServerTransactionHandle,
							smppServerResourceAdaptor);

					smppServerResourceAdaptor.startNewSmppServerTransactionActivity(smppServerTransaction);
					smppServerResourceAdaptor.fireEvent(EventsType.SUBMIT_SM,
							smppServerTransaction.getActivityHandle(), (SubmitSm) pduRequest);

					// Return null. Let SBB send response back
					return null;
				case SmppConstants.CMD_ID_DATA_SM:
					smppServerTransactionHandle = new SmppTransactionHandle(this.esme.getName(),
							pduRequest.getSequenceNumber(), SmppTransactionType.INCOMING);
					smppServerTransaction = new SmppTransactionImpl(pduRequest, this.esme, smppServerTransactionHandle,
							smppServerResourceAdaptor);
					smppServerResourceAdaptor.startNewSmppServerTransactionActivity(smppServerTransaction);
					smppServerResourceAdaptor.fireEvent(EventsType.DATA_SM, smppServerTransaction.getActivityHandle(),
							(DataSm) pduRequest);

					// Return null. Let SBB send response back
					return null;
				case SmppConstants.CMD_ID_DELIVER_SM:
					smppServerTransactionHandle = new SmppTransactionHandle(this.esme.getName(),
							pduRequest.getSequenceNumber(), SmppTransactionType.INCOMING);
					smppServerTransaction = new SmppTransactionImpl(pduRequest, this.esme, smppServerTransactionHandle,
							smppServerResourceAdaptor);
					smppServerResourceAdaptor.startNewSmppServerTransactionActivity(smppServerTransaction);
					smppServerResourceAdaptor.fireEvent(EventsType.DELIVER_SM,
							smppServerTransaction.getActivityHandle(), (DeliverSm) pduRequest);
					return null;
				default:
					tracer.severe(String.format("Rx : Non supported PduRequest=%s. Will not fire event", pduRequest));
					break;
				}
			} catch (Exception e) {
				tracer.severe(String.format("Error while processing PduRequest=%s", pduRequest), e);
				response.setCommandStatus(SmppConstants.STATUS_SYSERR);
			}

			return response;
		}

		@Override
		public String lookupResultMessage(int arg0) {
			return null;
		}

		@Override
		public String lookupTlvTagName(short arg0) {
			return null;
		}

		@Override
		public void fireChannelUnexpectedlyClosed() {
			tracer.severe(String
					.format("Rx : fireChannelUnexpectedlyClosed for SmppSessionImpl=%s Default handling is to discard an unexpected channel closed",
							this.esme.getName()));
		}

		@Override
		public void fireExpectedPduResponseReceived(PduAsyncResponse pduAsyncResponse) {

			PduRequest pduRequest = pduAsyncResponse.getRequest();
			PduResponse pduResponse = pduAsyncResponse.getResponse();

			SmppTransactionImpl smppServerTransaction = (SmppTransactionImpl) pduRequest.getReferenceObject();

			if (smppServerTransaction == null) {
				tracer.severe(String
						.format("Rx : fireExpectedPduResponseReceived for SmppSessionImpl=%s PduAsyncResponse=%s but SmppTransactionImpl is null",
								this.esme.getName(), pduAsyncResponse));
				return;
			}

			try {
				switch (pduResponse.getCommandId()) {
				case SmppConstants.CMD_ID_DELIVER_SM_RESP:
					smppServerResourceAdaptor.fireEvent(EventsType.DELIVER_SM_RESP,
							smppServerTransaction.getActivityHandle(), (DeliverSmResp) pduResponse);
					break;
				case SmppConstants.CMD_ID_DATA_SM_RESP:
					smppServerResourceAdaptor.fireEvent(EventsType.DATA_SM_RESP,
							smppServerTransaction.getActivityHandle(), (DataSmResp) pduResponse);
					break;
				case SmppConstants.CMD_ID_SUBMIT_SM_RESP:
					smppServerResourceAdaptor.fireEvent(EventsType.SUBMIT_SM_RESP,
							smppServerTransaction.getActivityHandle(), (SubmitSmResp) pduResponse);
					break;
				default:
					tracer.severe(String
							.format("Rx : fireExpectedPduResponseReceived for SmppSessionImpl=%s PduAsyncResponse=%s but PduResponse is unidentified. Event will not be fired ",
									this.esme.getName(), pduAsyncResponse));
					break;
				}

			} catch (Exception e) {
				tracer.severe(String.format("Error while processing PduAsyncResponse=%s", pduAsyncResponse), e);
			} finally {
				if (smppServerTransaction != null) {
					smppServerResourceAdaptor.endActivity(smppServerTransaction);
				}
			}
		}

		@Override
		public void firePduRequestExpired(PduRequest pduRequest) {
			tracer.warning(String.format("PduRequestExpired=%s", pduRequest));

			SmppTransactionImpl smppServerTransaction = (SmppTransactionImpl) pduRequest.getReferenceObject();

			if (smppServerTransaction == null) {
				tracer.severe(String
						.format("Rx : firePduRequestExpired for SmppSessionImpl=%s PduRequest=%s but SmppTransactionImpl is null",
								this.esme.getName(), pduRequest));
				return;
			}

			PduRequestTimeout event = new PduRequestTimeout(pduRequest, this.esme.getName());

			try {
				smppServerResourceAdaptor.fireEvent(EventsType.REQUEST_TIMEOUT,
						smppServerTransaction.getActivityHandle(), event);
			} catch (Exception e) {
				tracer.severe(String.format("Received firePduRequestExpired. Error while processing PduRequest=%s",
						pduRequest), e);
			} finally {
				if (smppServerTransaction != null) {
					smppServerResourceAdaptor.endActivity(smppServerTransaction);
				}
			}
		}

		@Override
		public void fireRecoverablePduException(RecoverablePduException recoverablePduException) {
			tracer.warning("Received fireRecoverablePduException", recoverablePduException);

			Pdu partialPdu = recoverablePduException.getPartialPdu();

			SmppTransactionImpl smppServerTransaction = (SmppTransactionImpl) partialPdu.getReferenceObject();

			if (smppServerTransaction == null) {
				tracer.severe(String.format(
						"Rx : fireRecoverablePduException for SmppSessionImpl=%s but SmppTransactionImpl is null",
						this.esme.getName()), recoverablePduException);
				return;
			}

			try {
				smppServerResourceAdaptor.fireEvent(EventsType.RECOVERABLE_PDU_EXCEPTION,
						smppServerTransaction.getActivityHandle(), recoverablePduException);
			} catch (Exception e) {
				tracer.severe(String.format(
						"Received fireRecoverablePduException. Error while processing RecoverablePduException=%s",
						recoverablePduException), e);
			} finally {
				if (smppServerTransaction != null) {
					smppServerResourceAdaptor.endActivity(smppServerTransaction);
				}
			}

		}

		@Override
		public void fireUnrecoverablePduException(UnrecoverablePduException unrecoverablePduException) {
			tracer.severe("Received fireUnrecoverablePduException", unrecoverablePduException);

			// TODO : recommendation is to close session
		}

		@Override
		public void fireUnexpectedPduResponseReceived(PduResponse pduResponse) {
			tracer.severe("Received fireUnexpectedPduResponseReceived PduResponse=" + pduResponse);
		}

		@Override
		public void fireUnknownThrowable(Throwable throwable) {
			tracer.severe("Received fireUnknownThrowable", throwable);
			// TODO what here?
		}

	}

	@Override
	public long getNextMessageId() {
		// return Long.toString(this.messageIdGenerator.incrementAndGet());
		return this.messageIdGenerator.incrementAndGet();
	}

}
