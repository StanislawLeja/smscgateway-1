package org.mobicents.smsc.slee.services.mo;

import javax.slee.ActivityContextInterface;
import javax.slee.InitialEventSelector;

import org.mobicents.protocols.ss7.map.api.MAPApplicationContextName;
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
import org.mobicents.protocols.ss7.map.api.smstpdu.SmsDeliverTpdu;
import org.mobicents.protocols.ss7.map.api.smstpdu.SmsSubmitTpdu;
import org.mobicents.protocols.ss7.map.api.smstpdu.SmsTpdu;
import org.mobicents.protocols.ss7.map.api.smstpdu.UserData;
import org.mobicents.slee.resource.map.events.DialogRequest;

public abstract class MoSbb extends MoCommonSbb {

	private static final String className = "MoSbb";

	public MoSbb() {
		super(className);
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
	 * Private Methods
	 * 
	 * @throws MAPException
	 */

	private void handleSmsSubmitTpdu(SmsSubmitTpdu smsSubmitTpdu, AddressString callingPartyAddress)
			throws MAPException {

		AddressField destinationAddress = smsSubmitTpdu.getDestinationAddress();

		UserData userData = smsSubmitTpdu.getUserData();

		// TODO handle SMS
	}

}
