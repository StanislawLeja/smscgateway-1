package org.mobicents.smsc.slee.services.mo;

import java.sql.Timestamp;

import javax.slee.ActivityContextInterface;
import javax.slee.InitialEventSelector;
import javax.slee.facilities.ActivityContextNamingFacility;
import javax.slee.facilities.NameAlreadyBoundException;
import javax.slee.nullactivity.NullActivity;

import org.mobicents.protocols.ss7.map.api.MAPApplicationContextName;
import org.mobicents.protocols.ss7.map.api.MAPDialog;
import org.mobicents.protocols.ss7.map.api.MAPException;
import org.mobicents.protocols.ss7.map.api.primitives.AddressString;
import org.mobicents.protocols.ss7.map.api.service.sms.ForwardShortMessageRequest;
import org.mobicents.protocols.ss7.map.api.service.sms.ForwardShortMessageResponse;
import org.mobicents.protocols.ss7.map.api.service.sms.MAPDialogSms;
import org.mobicents.protocols.ss7.map.api.service.sms.MoForwardShortMessageRequest;
import org.mobicents.protocols.ss7.map.api.service.sms.MoForwardShortMessageResponse;
import org.mobicents.protocols.ss7.map.api.service.sms.SM_RP_OA;
import org.mobicents.protocols.ss7.map.api.service.sms.SmsSignalInfo;
import org.mobicents.protocols.ss7.map.api.smstpdu.AddressField;
import org.mobicents.protocols.ss7.map.api.smstpdu.DataCodingScheme;
import org.mobicents.protocols.ss7.map.api.smstpdu.SmsDeliverTpdu;
import org.mobicents.protocols.ss7.map.api.smstpdu.SmsSubmitTpdu;
import org.mobicents.protocols.ss7.map.api.smstpdu.SmsTpdu;
import org.mobicents.protocols.ss7.map.api.smstpdu.UserData;
import org.mobicents.protocols.ss7.map.api.smstpdu.UserDataHeader;
import org.mobicents.slee.resource.map.events.DialogDelimiter;
import org.mobicents.slee.resource.map.events.DialogNotice;
import org.mobicents.slee.resource.map.events.DialogProviderAbort;
import org.mobicents.slee.resource.map.events.DialogReject;
import org.mobicents.slee.resource.map.events.DialogRequest;
import org.mobicents.slee.resource.map.events.DialogTimeout;
import org.mobicents.slee.resource.map.events.DialogUserAbort;
import org.mobicents.slee.resource.map.events.ErrorComponent;
import org.mobicents.slee.resource.map.events.RejectComponent;
import org.mobicents.smsc.slee.resources.smpp.server.SmppServerSession;
import org.mobicents.smsc.slee.services.smpp.server.events.SmsEvent;

import com.cloudhopper.commons.charset.CharsetUtil;
import com.cloudhopper.smpp.SmppConstants;

public abstract class MoSbb extends MoCommonSbb {

	private static final String className = "MoSbb";

	public MoSbb() {
		super(className);
	}

	public void onDialogRequest(DialogRequest evt, ActivityContextInterface aci) {
		super.onDialogRequest(evt, aci);

		this.setProcessingState(MoProcessingState.OnlyRequestRecieved);
	}

	public void onDialogDelimiter(DialogDelimiter evt, ActivityContextInterface aci) {
		super.onDialogDelimiter(evt, aci);

		if (this.getProcessingState() == MoProcessingState.OnlyRequestRecieved) {
			this.setProcessingState(null);
			if (this.logger.isInfoEnabled())
				this.logger.info("MoSBB: onDialogDelimiter - sending empty TC-CONTINUE for " + evt);
			evt.getMAPDialog();
			MAPDialog dialog = evt.getMAPDialog();

			try {
				dialog.send();
			} catch (MAPException e) {
				logger.severe("Error while sending Continue", e);
			}
		}
	}

	public void onErrorComponent(ErrorComponent event, ActivityContextInterface aci) {
		super.onErrorComponent(event, aci);

		this.setProcessingState(MoProcessingState.OtherDataRecieved);
	}

	public void onRejectComponent(RejectComponent event, ActivityContextInterface aci) {
		super.onRejectComponent(event, aci);

		this.setProcessingState(MoProcessingState.OtherDataRecieved);
	}

	public void onDialogReject(DialogReject evt, ActivityContextInterface aci) {
		super.onDialogReject(evt, aci);

		this.setProcessingState(MoProcessingState.OtherDataRecieved);
	}

	public void onDialogUserAbort(DialogUserAbort evt, ActivityContextInterface aci) {
		super.onDialogUserAbort(evt, aci);

		this.setProcessingState(MoProcessingState.OtherDataRecieved);
	}

	public void onDialogProviderAbort(DialogProviderAbort evt, ActivityContextInterface aci) {
		super.onDialogProviderAbort(evt, aci);

		this.setProcessingState(MoProcessingState.OtherDataRecieved);
	}

	public void onDialogNotice(DialogNotice evt, ActivityContextInterface aci) {
		super.onDialogNotice(evt, aci);

		this.setProcessingState(MoProcessingState.OtherDataRecieved);
	}

	public void onDialogTimeout(DialogTimeout evt, ActivityContextInterface aci) {
		super.onDialogTimeout(evt, aci);

		this.setProcessingState(MoProcessingState.OtherDataRecieved);
	}

	/**
	 * SMS Event Handlers
	 */
	/**
	 * Received incoming SMS for ACN v3. Send back ack
	 * 
	 * @param evt
	 * @param aci
	 */
	public void onMoForwardShortMessageRequest(MoForwardShortMessageRequest evt, ActivityContextInterface aci) {
		if (this.logger.isInfoEnabled()) {
			this.logger.info("Received MO_FORWARD_SHORT_MESSAGE_REQUEST = " + evt);
		}

		this.setProcessingState(MoProcessingState.OtherDataRecieved);

		SmsSignalInfo smsSignalInfo = evt.getSM_RP_UI();
		SM_RP_OA smRPOA = evt.getSM_RP_OA();

		AddressString callingPartyAddress = smRPOA.getMsisdn();
		if (callingPartyAddress == null) {
			callingPartyAddress = smRPOA.getServiceCentreAddressOA();
		}

		SmsTpdu smsTpdu = null;

		try {
			smsTpdu = smsSignalInfo.decodeTpdu(true);

			switch (smsTpdu.getSmsTpduType()) {
			case SMS_SUBMIT:
				SmsSubmitTpdu smsSubmitTpdu = (SmsSubmitTpdu) smsTpdu;
				if (this.logger.isInfoEnabled()) {
					this.logger.info("Received SMS_SUBMIT = " + smsSubmitTpdu);
				}
				this.handleSmsSubmitTpdu(smsSubmitTpdu, callingPartyAddress);
				break;
			case SMS_DELIVER:
				SmsDeliverTpdu smsDeliverTpdu = (SmsDeliverTpdu) smsTpdu;
				this.logger.severe("Received SMS_DELIVER = " + smsDeliverTpdu);
				break;
			default:
				this.logger.severe("Received non SMS_SUBMIT or SMS_DELIVER = " + smsTpdu);
				break;
			}
		} catch (MAPException e1) {
			logger.severe("Error while decoding SmsSignalInfo ", e1);
		}

		MAPDialogSms dialog = evt.getMAPDialog();

		try {
			dialog.addMoForwardShortMessageResponse(evt.getInvokeId(), null, null);
			dialog.close(false);
		} catch (MAPException e) {
			logger.severe("Error while sending ForwardShortMessageResponse ", e);
		}

	}

	/**
	 * Received Ack for MO SMS. But this is error we should never receive this
	 * 
	 * @param evt
	 * @param aci
	 */
	public void onMoForwardShortMessageResponse(MoForwardShortMessageResponse evt, ActivityContextInterface aci) {
		this.logger.severe("Received MO_FORWARD_SHORT_MESSAGE_RESPONSE = " + evt);
	}

	public void onForwardShortMessageRequest(ForwardShortMessageRequest evt, ActivityContextInterface aci) {
		if (this.logger.isInfoEnabled()) {
			this.logger.info("Received FORWARD_SHORT_MESSAGE_REQUEST = " + evt);
		}
	}

	/**
	 * Received Ack for MO SMS. But this is error we should never receive this
	 * 
	 * @param evt
	 * @param aci
	 */
	public void onForwardShortMessageResponse(ForwardShortMessageResponse evt, ActivityContextInterface aci) {
		this.logger.severe("Received FORWARD_SHORT_MESSAGE_RESPONSE = " + evt);
	}

	/**
	 * Initial event selector method to check if the Event should initalize the
	 */
	public InitialEventSelector initialEventSelect(InitialEventSelector ies) {
		Object event = ies.getEvent();
		DialogRequest dialogRequest = null;

		if (event instanceof DialogRequest) {
			dialogRequest = (DialogRequest) event;

			if (MAPApplicationContextName.shortMsgMORelayContext == dialogRequest.getMAPDialog()
					.getApplicationContext().getApplicationContextName()) {
				ies.setInitialEvent(true);
				ies.setActivityContextSelected(true);
			} else {
				ies.setInitialEvent(false);
			}
		}

		return ies;
	}

	/**
	 * Fire the SUBMIT_SM event to be consumed by MtSbb to send it to Mobile
	 * 
	 * @param event
	 * @param aci
	 * @param address
	 */
	public abstract void fireSubmitSm(SmsEvent event, ActivityContextInterface aci, javax.slee.Address address);

	/**
	 * Fire DELIVER_SM event to be consumed by RxSmppServerSbb to send it to
	 * ESME
	 * 
	 * @param event
	 * @param aci
	 * @param address
	 */
	public abstract void fireDeliverSm(SmsEvent event, ActivityContextInterface aci, javax.slee.Address address);

	/**
	 * Private Methods
	 * 
	 * @throws MAPException
	 */

	private void handleSmsSubmitTpdu(SmsSubmitTpdu smsSubmitTpdu, AddressString callingPartyAddress)
			throws MAPException {

		AddressField destinationAddress = smsSubmitTpdu.getDestinationAddress();

		UserData userData = smsSubmitTpdu.getUserData();

		userData.decode();

		// TODO : Is decoding correct? May be we should send the raw data
		// userData.decode();
		// String decodedMessage = userData.getDecodedMessage();
		//
		// if (this.logger.isInfoEnabled()) {
		// this.logger.info("decodedMessage SMS_SUBMIT = " + decodedMessage);
		// }

		SmsEvent rxSMS = new SmsEvent();
		rxSMS.setSourceAddr(callingPartyAddress.getAddress());
		rxSMS.setSourceAddrNpi((byte) callingPartyAddress.getNumberingPlan().getIndicator());
		rxSMS.setSourceAddrTon((byte) callingPartyAddress.getAddressNature().getIndicator());

		rxSMS.setDestAddr(destinationAddress.getAddressValue());
		rxSMS.setDestAddrNpi((byte) destinationAddress.getNumberingPlanIdentification().getCode());
		rxSMS.setDestAddrTon((byte) destinationAddress.getTypeOfNumber().getCode());
		//
		// deliveryReport.setSystemId(original.getSystemId());
		//
		rxSMS.setSubmitDate(new Timestamp(System.currentTimeMillis()));

		DataCodingScheme dataCodingScheme = userData.getDataCodingScheme();
		byte[] smsPayload = null;

		switch (dataCodingScheme.getCharacterSet()) {
		case GSM7:
			UserDataHeader udh = userData.getDecodedUserDataHeader();
			if (udh != null) {
				byte[] buf1 = udh.getEncodedData();
				if (buf1 != null) {
					byte[] buf2 = CharsetUtil.encode(userData.getDecodedMessage(), CharsetUtil.CHARSET_GSM);
					smsPayload = new byte[buf1.length + buf2.length];
					System.arraycopy(buf1, 0, smsPayload, 0, buf1.length);
					System.arraycopy(buf2, 0, smsPayload, buf1.length, buf2.length);
				} else {
					smsPayload = CharsetUtil.encode(userData.getDecodedMessage(), CharsetUtil.CHARSET_GSM);
				}
			} else {
				smsPayload = CharsetUtil.encode(userData.getDecodedMessage(), CharsetUtil.CHARSET_GSM);
			}
			break;
		default:
			smsPayload = userData.getEncodedData();
			break;

//		case GSM8:
//			// TODO : Is this correct?
//			smsPayload = CharsetUtil.encode(userData.getDecodedMessage(), CharsetUtil.CHARSET_GSM);
//			break;
//		case UCS2:
//			smsPayload = userData.getEncodedData();
//			break;
//		default:
//			logger.warning("Received unrecognized DataCodingScheme " + dataCodingScheme);
//			smsPayload = userData.getEncodedData();
//			break;
		}

		rxSMS.setShortMessage(smsPayload);

		if (smsSubmitTpdu.getStatusReportRequest()) {
			rxSMS.setRegisteredDelivery(SmppConstants.REGISTERED_DELIVERY_SMSC_RECEIPT_REQUESTED);
		}

		rxSMS.setEsmClass((byte) (userData.getEncodedUserDataHeaderIndicator() ? 0x40 : 0));

		rxSMS.setDataCoding((byte) dataCodingScheme.getCode());

		// TODO More parameters

		SmppServerSession smppSession = smppServerSessions.getSmppSession(rxSMS.getDestAddrTon(),
				rxSMS.getDestAddrNpi(), rxSMS.getDestAddr());

		if (smppSession == null) {
			if (this.logger.isInfoEnabled()) {
				this.logger.info(String.format("No SmppServerSession for MoSMS=%s Will send to to Mt module", rxSMS));
			}

			this.processSubmitSM(rxSMS);

		} else if (!smppSession.isBound()) {
			this.logger.severe(String.format("Received MoSMS=%s but SmppSession=%s is not BOUND", rxSMS,
					smppSession.getSystemId()));
			// TODO : Add to SnF module
		} else {
			rxSMS.setSystemId(smppSession.getSystemId());

			NullActivity nullActivity = this.sbbContext.getNullActivityFactory().createNullActivity();
			ActivityContextInterface nullActivityContextInterface = this.sbbContext
					.getNullActivityContextInterfaceFactory().getActivityContextInterface(nullActivity);

			this.fireDeliverSm(rxSMS, nullActivityContextInterface, null);
		}
	}

	private void processSubmitSM(SmsEvent event) {

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

		MoActivityContextInterface txSmppServerSbbActivityContextInterface = this
				.asSbbActivityContextInterface(nullActivityContextInterface);
		int pendingEventsOnNullActivity = txSmppServerSbbActivityContextInterface.getPendingEventsOnNullActivity();
		pendingEventsOnNullActivity = pendingEventsOnNullActivity + 1;

		if (this.logger.isInfoEnabled()) {
			this.logger.info(String.format("pendingEventsOnNullActivity = %d", pendingEventsOnNullActivity));
		}

		txSmppServerSbbActivityContextInterface.setPendingEventsOnNullActivity(pendingEventsOnNullActivity);
		// We have NullActivityContextInterface, lets fire SmsEvent on this
		this.fireSubmitSm(event, nullActivityContextInterface, null);

	}

	public abstract void setProcessingState(MoProcessingState processingState);

	public abstract MoProcessingState getProcessingState();

	public enum MoProcessingState {
		OnlyRequestRecieved,
		OtherDataRecieved,
	}
}
