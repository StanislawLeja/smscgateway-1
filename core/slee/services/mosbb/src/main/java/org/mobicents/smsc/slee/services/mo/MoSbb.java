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

package org.mobicents.smsc.slee.services.mo;

import java.sql.Timestamp;
import java.util.Date;
import java.util.UUID;

import javax.slee.ActivityContextInterface;
import javax.slee.InitialEventSelector;

import org.mobicents.protocols.ss7.map.api.MAPApplicationContextName;
import org.mobicents.protocols.ss7.map.api.MAPDialog;
import org.mobicents.protocols.ss7.map.api.MAPException;
import org.mobicents.protocols.ss7.map.api.errors.MAPErrorCode;
import org.mobicents.protocols.ss7.map.api.errors.MAPErrorMessage;
import org.mobicents.protocols.ss7.map.api.primitives.AddressString;
import org.mobicents.protocols.ss7.map.api.primitives.IMSI;
import org.mobicents.protocols.ss7.map.api.primitives.ISDNAddressString;
import org.mobicents.protocols.ss7.map.api.service.sms.ForwardShortMessageRequest;
import org.mobicents.protocols.ss7.map.api.service.sms.ForwardShortMessageResponse;
import org.mobicents.protocols.ss7.map.api.service.sms.MAPDialogSms;
import org.mobicents.protocols.ss7.map.api.service.sms.MoForwardShortMessageRequest;
import org.mobicents.protocols.ss7.map.api.service.sms.MoForwardShortMessageResponse;
import org.mobicents.protocols.ss7.map.api.service.sms.MtForwardShortMessageRequest;
import org.mobicents.protocols.ss7.map.api.service.sms.MtForwardShortMessageResponse;
import org.mobicents.protocols.ss7.map.api.service.sms.SM_RP_DA;
import org.mobicents.protocols.ss7.map.api.service.sms.SM_RP_OA;
import org.mobicents.protocols.ss7.map.api.service.sms.SmsSignalInfo;
import org.mobicents.protocols.ss7.map.api.smstpdu.AbsoluteTimeStamp;
import org.mobicents.protocols.ss7.map.api.smstpdu.AddressField;
import org.mobicents.protocols.ss7.map.api.smstpdu.DataCodingScheme;
import org.mobicents.protocols.ss7.map.api.smstpdu.NumberingPlanIdentification;
import org.mobicents.protocols.ss7.map.api.smstpdu.SmsCommandTpdu;
import org.mobicents.protocols.ss7.map.api.smstpdu.SmsDeliverReportTpdu;
import org.mobicents.protocols.ss7.map.api.smstpdu.SmsDeliverTpdu;
import org.mobicents.protocols.ss7.map.api.smstpdu.SmsSubmitTpdu;
import org.mobicents.protocols.ss7.map.api.smstpdu.SmsTpdu;
import org.mobicents.protocols.ss7.map.api.smstpdu.UserData;
import org.mobicents.protocols.ss7.map.api.smstpdu.UserDataHeader;
import org.mobicents.protocols.ss7.map.api.smstpdu.ValidityPeriod;
import org.mobicents.protocols.ss7.map.api.smstpdu.ValidityPeriodFormat;
import org.mobicents.slee.resource.map.events.DialogDelimiter;
import org.mobicents.slee.resource.map.events.DialogNotice;
import org.mobicents.slee.resource.map.events.DialogProviderAbort;
import org.mobicents.slee.resource.map.events.DialogReject;
import org.mobicents.slee.resource.map.events.DialogRequest;
import org.mobicents.slee.resource.map.events.DialogTimeout;
import org.mobicents.slee.resource.map.events.DialogUserAbort;
import org.mobicents.slee.resource.map.events.ErrorComponent;
import org.mobicents.slee.resource.map.events.RejectComponent;
import org.mobicents.smsc.cassandra.PersistenceException;
import org.mobicents.smsc.cassandra.Sms;
import org.mobicents.smsc.cassandra.SmsSet;
import org.mobicents.smsc.cassandra.TargetAddress;
import org.mobicents.smsc.slee.resources.persistence.MessageUtil;
import org.mobicents.smsc.slee.resources.persistence.PersistenceRAInterface;
import org.mobicents.smsc.slee.resources.persistence.SmscProcessingException;

import com.cloudhopper.commons.charset.CharsetUtil;
import com.cloudhopper.smpp.SmppConstants;

/**
 * 
 * @author amit bhayani
 * @author servey vetyutnev
 * 
 */
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
			this.logger.info("\nReceived MO_FORWARD_SHORT_MESSAGE_REQUEST = " + evt);
		}

		this.setProcessingState(MoProcessingState.OtherDataRecieved);

		MAPDialogSms dialog = evt.getMAPDialog();

		try {
			this.processMoMessage(evt.getSM_RP_OA(), evt.getSM_RP_DA(), evt.getSM_RP_UI());
		} catch (SmscProcessingException e1) {
			this.logger.severe(e1.getMessage(), e1);
			try {
				MAPErrorMessage errorMessage;
				switch (e1.getMapErrorCode()) {
				case MAPErrorCode.unexpectedDataValue:
					errorMessage = dialog.getService().getMAPProvider().getMAPErrorMessageFactory()
							.createMAPErrorMessageExtensionContainer((long) MAPErrorCode.unexpectedDataValue, null);
					break;
				case MAPErrorCode.systemFailure:
					errorMessage = dialog.getService().getMAPProvider().getMAPErrorMessageFactory()
							.createMAPErrorMessageSystemFailure(dialog.getApplicationContext().getApplicationContextVersion().getVersion(), null, null, null);
					break;
				default:
					errorMessage = dialog.getService().getMAPProvider().getMAPErrorMessageFactory()
							.createMAPErrorMessageSystemFailure(dialog.getApplicationContext().getApplicationContextVersion().getVersion(), null, null, null);
					break;
				}
				dialog.sendErrorComponent(evt.getInvokeId(), errorMessage);
				if (this.logger.isInfoEnabled()) {
					this.logger.info("\nSent ErrorComponent = " + errorMessage);
				}

				dialog.close(false);
			} catch (Throwable e) {
				logger.severe("Error while sending Error message", e);
				return;
			}
			return;
		} catch (Throwable e1) {
			this.logger.severe("Exception while processing MO message: " + e1.getMessage(), e1);
			try {
				MAPErrorMessage errorMessage = dialog.getService().getMAPProvider().getMAPErrorMessageFactory()
						.createMAPErrorMessageSystemFailure(dialog.getApplicationContext().getApplicationContextVersion().getVersion(), null, null, null);
				dialog.sendErrorComponent(evt.getInvokeId(), errorMessage);
				dialog.close(false);
			} catch (Throwable e) {
				logger.severe("Error while sending Error message", e);
				return;
			}
			return;
		}

		try {
			dialog.addMoForwardShortMessageResponse(evt.getInvokeId(), null, null);
			if (this.logger.isInfoEnabled()) {
				this.logger.info("\nSent MoForwardShortMessageResponse = " + evt);
			}

			dialog.close(false);
		} catch (Throwable e) {
			logger.severe("Error while sending MoForwardShortMessageResponse ", e);
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

		this.setProcessingState(MoProcessingState.OtherDataRecieved);

		MAPDialogSms dialog = evt.getMAPDialog();

		try {
			this.processMoMessage(evt.getSM_RP_OA(), evt.getSM_RP_DA(), evt.getSM_RP_UI());
		} catch (SmscProcessingException e1) {
			this.logger.severe(e1.getMessage(), e1);
			try {
				MAPErrorMessage errorMessage;
				switch (e1.getMapErrorCode()) {
				case MAPErrorCode.unexpectedDataValue:
					errorMessage = dialog.getService().getMAPProvider().getMAPErrorMessageFactory()
							.createMAPErrorMessageExtensionContainer((long) MAPErrorCode.unexpectedDataValue, null);
					break;
				case MAPErrorCode.systemFailure:
					errorMessage = dialog.getService().getMAPProvider().getMAPErrorMessageFactory()
							.createMAPErrorMessageSystemFailure(dialog.getApplicationContext().getApplicationContextVersion().getVersion(), null, null, null);
					break;
				default:
					errorMessage = dialog.getService().getMAPProvider().getMAPErrorMessageFactory()
							.createMAPErrorMessageSystemFailure(dialog.getApplicationContext().getApplicationContextVersion().getVersion(), null, null, null);
					break;
				}
				dialog.sendErrorComponent(evt.getInvokeId(), errorMessage);
				if (this.logger.isInfoEnabled()) {
					this.logger.info("\nSent ErrorComponent = " + errorMessage);
				}

				dialog.close(false);
			} catch (Throwable e) {
				logger.severe("Error while sending Error message", e);
				return;
			}
			return;
		} catch (Throwable e1) {
			this.logger.severe("Exception while processing MO message: " + e1.getMessage(), e1);
			try {
				MAPErrorMessage errorMessage = dialog.getService().getMAPProvider().getMAPErrorMessageFactory()
						.createMAPErrorMessageSystemFailure(dialog.getApplicationContext().getApplicationContextVersion().getVersion(), null, null, null);
				dialog.sendErrorComponent(evt.getInvokeId(), errorMessage);
				dialog.close(false);
			} catch (Throwable e) {
				logger.severe("Error while sending Error message", e);
				return;
			}
			return;
		}

		try {
			dialog.addForwardShortMessageResponse(evt.getInvokeId());
			if (this.logger.isInfoEnabled()) {
				this.logger.info("\nSent ForwardShortMessageResponse = " + evt);
			}

			dialog.close(false);
		} catch (Throwable e) {
			logger.severe("Error while sending ForwardShortMessageResponse ", e);
		}
	}
	
	/**
	 * Received MT SMS. This is error we should never receive this
	 * 
	 * @param evt
	 * @param aci
	 */
	public void onMtForwardShortMessageRequest(MtForwardShortMessageRequest evt, ActivityContextInterface aci) {
		if(this.logger.isInfoEnabled()){
			this.logger.info("\nReceived MT_FORWARD_SHORT_MESSAGE_REQUEST = " + evt);
		}
		
		this.setProcessingState(MoProcessingState.OtherDataRecieved);
		
		MAPDialogSms dialog = evt.getMAPDialog();
		
		try {
			this.processMtMessage(evt.getSM_RP_OA(), evt.getSM_RP_DA(), evt.getSM_RP_UI());
		} catch (SmscProcessingException e1) {
			this.logger.severe(e1.getMessage(), e1);
			try {
				MAPErrorMessage errorMessage;
				switch (e1.getMapErrorCode()) {
				case MAPErrorCode.unexpectedDataValue:
					errorMessage = this.mapProvider.getMAPErrorMessageFactory()
							.createMAPErrorMessageExtensionContainer((long) MAPErrorCode.unexpectedDataValue, null);
					break;
				case MAPErrorCode.systemFailure:
					errorMessage = this.mapProvider.getMAPErrorMessageFactory()
							.createMAPErrorMessageSystemFailure(dialog.getApplicationContext().getApplicationContextVersion().getVersion(), null, null, null);
					break;
				default:
					errorMessage = this.mapProvider.getMAPErrorMessageFactory()
							.createMAPErrorMessageSystemFailure(dialog.getApplicationContext().getApplicationContextVersion().getVersion(), null, null, null);
					break;
				}
				dialog.sendErrorComponent(evt.getInvokeId(), errorMessage);
				if (this.logger.isInfoEnabled()) {
					this.logger.info("\nSent ErrorComponent = " + errorMessage);
				}

				dialog.close(false);
			} catch (Throwable e) {
				logger.severe("Error while sending Error message", e);
				return;
			}
			return;
		} catch (Throwable e1) {
			this.logger.severe("Exception while processing MO message: " + e1.getMessage(), e1);
			try {
				MAPErrorMessage errorMessage = this.mapProvider.getMAPErrorMessageFactory()
						.createMAPErrorMessageSystemFailure(dialog.getApplicationContext().getApplicationContextVersion().getVersion(), null, null, null);
				dialog.sendErrorComponent(evt.getInvokeId(), errorMessage);
				dialog.close(false);
			} catch (Throwable e) {
				logger.severe("Error while sending Error message", e);
				return;
			}
			return;
		}

		try {
			dialog.addMtForwardShortMessageResponse(evt.getInvokeId(), null, null);
			if (this.logger.isInfoEnabled()) {
				this.logger.info("\nSent MtForwardShortMessageResponse = " + evt);
			}

			dialog.close(false);
		} catch (Throwable e) {
			logger.severe("Error while sending MoForwardShortMessageResponse ", e);
		}			
	}

	/**
	 * Received ACK for MT Forward SMS sent earlier
	 * 
	 * @param evt
	 * @param aci
	 */
	public void onMtForwardShortMessageResponse(MtForwardShortMessageResponse evt, ActivityContextInterface aci) {
			this.logger.severe("\nReceived MT_FORWARD_SHORT_MESSAGE_RESPONSE = " + evt);

	}
	
	private void processMtMessage(SM_RP_OA smRPOA, SM_RP_DA smRPDA, SmsSignalInfo smsSignalInfo) throws SmscProcessingException {
		IMSI destinationImsi = smRPDA.getIMSI();
		
		if (destinationImsi == null) {
			throw new SmscProcessingException("Mt DA IMSI is absent", SmppConstants.STATUS_SYSERR, MAPErrorCode.unexpectedDataValue, null);
		}
		
		SmsTpdu smsTpdu = null;
		
		try {
			smsTpdu = smsSignalInfo.decodeTpdu(false);

			switch (smsTpdu.getSmsTpduType()) {
			case SMS_DELIVER:
				SmsDeliverTpdu smsDeliverTpdu = (SmsDeliverTpdu) smsTpdu;
				if (this.logger.isInfoEnabled()) {
					this.logger.info("Received SMS_DELIVER = " + smsDeliverTpdu);
				}
//				AddressField af = smsSubmitTpdu.getDestinationAddress();
				this.handleSmsDeliverTpdu(smsDeliverTpdu, destinationImsi);
				break;
			default:
				this.logger.severe("Received non SMS_DELIVER = " + smsTpdu);
				break;
			}
		} catch (MAPException e1) {
			logger.severe("Error while decoding SmsSignalInfo ", e1);
		}
	}

	private void processMoMessage(SM_RP_OA smRPOA, SM_RP_DA smRPDA, SmsSignalInfo smsSignalInfo) throws SmscProcessingException {

		// TODO: check if smRPDA contains local SMSC address and reject messages if not equal ???
		
		ISDNAddressString callingPartyAddress = smRPOA.getMsisdn();
		if (callingPartyAddress == null) {
			throw new SmscProcessingException("MO callingPartyAddress is absent", SmppConstants.STATUS_SYSERR, MAPErrorCode.unexpectedDataValue, null);
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
//				AddressField af = smsSubmitTpdu.getDestinationAddress();
				this.handleSmsSubmitTpdu(smsSubmitTpdu, callingPartyAddress);
				break;
			case SMS_DELIVER_REPORT:
				SmsDeliverReportTpdu smsDeliverReportTpdu = (SmsDeliverReportTpdu) smsTpdu;
				if (this.logger.isInfoEnabled()) {
					this.logger.info("Received SMS_DELIVER_REPORT = " + smsDeliverReportTpdu);
				}
				// TODO: implement it - processing of SMS_DELIVER_REPORT
//				this.handleSmsDeliverReportTpdu(smsDeliverReportTpdu, callingPartyAddress);
				break;
			case SMS_COMMAND:
				SmsCommandTpdu smsCommandTpdu = (SmsCommandTpdu) smsTpdu;
				if (this.logger.isInfoEnabled()) {
					this.logger.info("Received SMS_COMMAND = " + smsCommandTpdu);
				}
				// TODO: implement it - processing of SMS_COMMAND
//				this.handleSmsDeliverReportTpdu(smsDeliverReportTpdu, callingPartyAddress);
				break;
			default:
				this.logger.severe("Received non SMS_SUBMIT or SMS_DELIVER_REPORT or SMS_COMMAND = " + smsTpdu);
				break;
			}
		} catch (MAPException e1) {
			logger.severe("Error while decoding SmsSignalInfo ", e1);
		}
	}
	private TargetAddress createDestTargetAddress(IMSI imsi) throws SmscProcessingException {
		if (imsi == null || imsi.getData() == null || imsi.getData().isEmpty()) {
			throw new SmscProcessingException("Mt DA IMSI digits are absent", SmppConstants.STATUS_SYSERR, MAPErrorCode.unexpectedDataValue, null);
		}
		
		//TODO is this correct?
		int destTon = smscPropertiesManagement.getDefaultTon();
		int destNpi =  smscPropertiesManagement.getDefaultNpi();
		
		TargetAddress ta = new TargetAddress(destTon, destNpi, imsi.getData());
		return ta;
	}
	
	private TargetAddress createDestTargetAddress(AddressField af) throws SmscProcessingException {

		if (af == null || af.getAddressValue() == null || af.getAddressValue().isEmpty()) {
			throw new SmscProcessingException("MO DestAddress digits are absent", SmppConstants.STATUS_SYSERR, MAPErrorCode.unexpectedDataValue, null);
		}

		int destTon, destNpi;
		switch (af.getTypeOfNumber()) {
		case Unknown:
			destTon = smscPropertiesManagement.getDefaultTon();
			break;
		case InternationalNumber:
			destTon = af.getTypeOfNumber().getCode();
			break;
		default:
			throw new SmscProcessingException("MO DestAddress TON not supported: " + af.getTypeOfNumber().getCode(), SmppConstants.STATUS_SYSERR,
					MAPErrorCode.unexpectedDataValue, null);
		}
		NumberingPlanIdentification npi;
		switch (af.getNumberingPlanIdentification()) {
		case Unknown:
			destNpi = smscPropertiesManagement.getDefaultNpi();
			break;
		case ISDNTelephoneNumberingPlan:
			destNpi = af.getNumberingPlanIdentification().getCode();
			break;
		default:
			throw new SmscProcessingException("MO DestAddress NPI not supported: " + af.getNumberingPlanIdentification().getCode(), SmppConstants.STATUS_SYSERR,
					MAPErrorCode.unexpectedDataValue, null);
		}

		TargetAddress ta = new TargetAddress(destTon, destNpi, af.getAddressValue());
		return ta;
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
					.getApplicationContext().getApplicationContextName() || MAPApplicationContextName.shortMsgMTRelayContext == dialogRequest.getMAPDialog()
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

	private void handleSmsSubmitTpdu(SmsSubmitTpdu smsSubmitTpdu, AddressString callingPartyAddress) throws SmscProcessingException {

		TargetAddress ta = createDestTargetAddress(smsSubmitTpdu.getDestinationAddress());
		PersistenceRAInterface store = obtainStore(ta);
		TargetAddress lock = store.obtainSynchroObject(ta);

		try {
			synchronized (lock) {
				Sms sms = this.createSmsEvent(smsSubmitTpdu, ta, store, callingPartyAddress);
				this.processSms(sms, store);
			}
		} finally {
			store.releaseSynchroObject(lock);
		}
	}
	
	private void handleSmsDeliverTpdu(SmsDeliverTpdu smsDeliverTpdu, IMSI destinationImsi) throws SmscProcessingException {
		
		AddressField tpOAAddress = smsDeliverTpdu.getOriginatingAddress();

		TargetAddress ta = createDestTargetAddress(destinationImsi);
		PersistenceRAInterface store = obtainStore(ta);
		TargetAddress lock = store.obtainSynchroObject(ta);

		try {
			synchronized (lock) {
				Sms sms = this.createSmsEvent(smsDeliverTpdu, ta, store, tpOAAddress);
				this.processSms(sms, store);
			}
		} finally {
			store.releaseSynchroObject(lock);
		}
	}	

	private Sms createSmsEvent(SmsSubmitTpdu smsSubmitTpdu, TargetAddress ta, PersistenceRAInterface store, AddressString callingPartyAddress)
			throws SmscProcessingException {

		UserData userData = smsSubmitTpdu.getUserData();
		try {
			userData.decode();
		} catch (MAPException e) {
			throw new SmscProcessingException("MO MAPException when decoding user data", SmppConstants.STATUS_SYSERR, MAPErrorCode.unexpectedDataValue, null);
		}

		Sms sms = new Sms();
		sms.setDbId(UUID.randomUUID());

		// checking parameters first
		if (callingPartyAddress == null || callingPartyAddress.getAddress() == null || callingPartyAddress.getAddress().isEmpty()) {
			throw new SmscProcessingException("MO SourceAddress digits are absent", SmppConstants.STATUS_SYSERR, MAPErrorCode.unexpectedDataValue, null);
		}
		if (callingPartyAddress.getAddressNature() == null) {
			throw new SmscProcessingException("MO SourceAddress AddressNature is absent", SmppConstants.STATUS_SYSERR, MAPErrorCode.unexpectedDataValue, null);
		}
		if (callingPartyAddress.getNumberingPlan() == null) {
			throw new SmscProcessingException("MO SourceAddress NumberingPlan is absent", SmppConstants.STATUS_SYSERR, MAPErrorCode.unexpectedDataValue, null);
		}
		sms.setSourceAddr(callingPartyAddress.getAddress());
		switch(callingPartyAddress.getAddressNature()){
		case unknown:
			sms.setSourceAddrTon(smscPropertiesManagement.getDefaultTon());
			break;
		case international_number:
			sms.setSourceAddrTon(callingPartyAddress.getAddressNature().getIndicator());
			break;
		default:
			throw new SmscProcessingException("MO SourceAddress TON not supported: " + callingPartyAddress.getAddressNature(), SmppConstants.STATUS_SYSERR,
					MAPErrorCode.unexpectedDataValue, null);
		}

		switch (callingPartyAddress.getNumberingPlan()) {
		case unknown:
			sms.setSourceAddrNpi(smscPropertiesManagement.getDefaultNpi());
			break;
		case ISDN:
			sms.setSourceAddrNpi(callingPartyAddress.getNumberingPlan().getIndicator());
			break;
		default:
			throw new SmscProcessingException("MO SourceAddress NPI not supported: " + callingPartyAddress.getNumberingPlan(), SmppConstants.STATUS_SYSERR,
					MAPErrorCode.unexpectedDataValue, null);
		}

		sms.setSubmitDate(new Timestamp(System.currentTimeMillis()));

		sms.setEsmClass(0x03 + (smsSubmitTpdu.getUserDataHeaderIndicator() ? SmppConstants.ESM_CLASS_UDHI_MASK : 0)
				+ (smsSubmitTpdu.getReplyPathExists() ? SmppConstants.ESM_CLASS_REPLY_PATH_MASK : 0));
		sms.setProtocolId(smsSubmitTpdu.getProtocolIdentifier().getCode());
		sms.setPriority(0);

		// TODO: do we need somehow care with RegisteredDelivery ?
		sms.setReplaceIfPresent(smsSubmitTpdu.getRejectDuplicates() ? 2 : 0);

		// TODO: care with smsSubmitTpdu.getStatusReportRequest() parameter sending back SMS_STATUS_REPORT tpdu ? 


		DataCodingScheme dataCodingScheme = smsSubmitTpdu.getDataCodingScheme();
		byte[] smsPayload = null;
		int dcs = dataCodingScheme.getCode();
        String err = MessageUtil.chechDataCodingSchemeSupport(dcs);
        if (err != null) {
            throw new SmscProcessingException("MO DataCoding scheme does not supported: " + dcs + " - " + err, SmppConstants.STATUS_SYSERR,
                    MAPErrorCode.unexpectedDataValue, null);
        }
        sms.setDataCoding(dcs);

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
		}

		sms.setShortMessage(smsPayload);

		// ValidityPeriod processing
		ValidityPeriod vp = smsSubmitTpdu.getValidityPeriod();
		ValidityPeriodFormat vpf = smsSubmitTpdu.getValidityPeriodFormat();
		Date validityPeriod = null;
		if (vp != null && vpf!=null && vpf!=ValidityPeriodFormat.fieldNotPresent) {
			switch(vpf){
			case fieldPresentAbsoluteFormat:
				AbsoluteTimeStamp ats = vp.getAbsoluteFormatValue();
				Date dt = new Date(ats.getYear(), ats.getMonth(), ats.getDay(), ats.getHour(), ats.getMinute(), ats.getSecond());
				int i1 = ats.getTimeZone() * 15 * 60;
				int i2 = -new Date().getTimezoneOffset() * 60;
				long i3 = (i2 - i1) * 1000;
				validityPeriod = new Date(dt.getTime() + i3);
				break;
			case fieldPresentRelativeFormat:
				validityPeriod = new Date(new Date().getTime() + (long)(vp.getRelativeFormatHours() * 3600 * 1000));
				break;
			case fieldPresentEnhancedFormat:
				this.logger.info("Recieved unsupported ValidityPeriodFormat: PresentEnhancedFormat - we skip it");
				break;
			}
		}
		MessageUtil.applyValidityPeriod(sms, validityPeriod, false);

		SmsSet smsSet;
		try {
			smsSet = store.obtainSmsSet(ta);
		} catch (PersistenceException e1) {
			throw new SmscProcessingException("PersistenceException when reading SmsSet from a database: " + ta.toString() + "\n" + e1.getMessage(),
					SmppConstants.STATUS_SYSERR, MAPErrorCode.systemFailure, null, e1);
		}
		sms.setSmsSet(smsSet);

		long messageId = this.smppServerSessions.getNextMessageId();
		sms.setMessageId(messageId);
		sms.setMoMessageRef(smsSubmitTpdu.getMessageReference());

		// TODO: process case when smsSubmitTpdu.getRejectDuplicates()==true: we need reject message with same MessageId+same source and dest addresses ?

		return sms;
	}
	
	private Sms createSmsEvent(SmsDeliverTpdu smsDeliverTpdu, TargetAddress ta, PersistenceRAInterface store, AddressField callingPartyAddress)
			throws SmscProcessingException {

		UserData userData = smsDeliverTpdu.getUserData();
		try {
			userData.decode();
		} catch (MAPException e) {
			throw new SmscProcessingException("MT MAPException when decoding user data", SmppConstants.STATUS_SYSERR, MAPErrorCode.unexpectedDataValue, null);
		}

		Sms sms = new Sms();
		sms.setDbId(UUID.randomUUID());

		// checking parameters first
		if (callingPartyAddress == null || callingPartyAddress.getAddressValue() == null || callingPartyAddress.getAddressValue().isEmpty()) {
			throw new SmscProcessingException("MT TP-OA digits are absent", SmppConstants.STATUS_SYSERR, MAPErrorCode.unexpectedDataValue, null);
		}
		if (callingPartyAddress.getTypeOfNumber() == null) {
			throw new SmscProcessingException("MT TP-OA TypeOfNumber is absent", SmppConstants.STATUS_SYSERR, MAPErrorCode.unexpectedDataValue, null);
		}
		if (callingPartyAddress.getNumberingPlanIdentification() == null) {
			throw new SmscProcessingException("MT TP-OA NumberingPlanIdentification is absent", SmppConstants.STATUS_SYSERR, MAPErrorCode.unexpectedDataValue, null);
		}
		sms.setSourceAddr(callingPartyAddress.getAddressValue());
		switch(callingPartyAddress.getTypeOfNumber()){
		case Unknown:
			sms.setSourceAddrTon(smscPropertiesManagement.getDefaultTon());
			break;
		case InternationalNumber:
			sms.setSourceAddrTon(callingPartyAddress.getTypeOfNumber().getCode());
			break;
		default:
			throw new SmscProcessingException("MT TP-OA TypeOfNumber not supported: " + callingPartyAddress.getTypeOfNumber(), SmppConstants.STATUS_SYSERR,
					MAPErrorCode.unexpectedDataValue, null);
		}

		switch (callingPartyAddress.getNumberingPlanIdentification()) {
		case Unknown:
			sms.setSourceAddrNpi(smscPropertiesManagement.getDefaultNpi());
			break;
		case ISDNTelephoneNumberingPlan:
			sms.setSourceAddrNpi(callingPartyAddress.getNumberingPlanIdentification().getCode());
			break;
		default:
			throw new SmscProcessingException("MT TP_OA NumberingPlanIdentification not supported: " + callingPartyAddress.getNumberingPlanIdentification(), SmppConstants.STATUS_SYSERR,
					MAPErrorCode.unexpectedDataValue, null);
		}

		sms.setSubmitDate(new Timestamp(System.currentTimeMillis()));

		sms.setEsmClass(0x03 + (smsDeliverTpdu.getUserDataHeaderIndicator() ? SmppConstants.ESM_CLASS_UDHI_MASK : 0)
				+ (smsDeliverTpdu.getReplyPathExists() ? SmppConstants.ESM_CLASS_REPLY_PATH_MASK : 0));
		sms.setProtocolId(smsDeliverTpdu.getProtocolIdentifier().getCode());
		sms.setPriority(0);

		// TODO: do we need somehow care with RegisteredDelivery ?
		sms.setReplaceIfPresent(0);

		// TODO: care with smsSubmitTpdu.getStatusReportRequest() parameter sending back SMS_STATUS_REPORT tpdu ? 


		DataCodingScheme dataCodingScheme = smsDeliverTpdu.getDataCodingScheme();
		byte[] smsPayload = null;
		int dcs = dataCodingScheme.getCode();
        String err = MessageUtil.chechDataCodingSchemeSupport(dcs);
        if (err != null) {
            throw new SmscProcessingException("MO DataCoding scheme does not supported: " + dcs + " - " + err, SmppConstants.STATUS_SYSERR,
                    MAPErrorCode.unexpectedDataValue, null);
        }
        sms.setDataCoding(dcs);

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
		}

		sms.setShortMessage(smsPayload);

		// ValidityPeriod processing
		MessageUtil.applyValidityPeriod(sms, null, false);

		SmsSet smsSet;
		try {
			smsSet = store.obtainSmsSet(ta);
		} catch (PersistenceException e1) {
			throw new SmscProcessingException("PersistenceException when reading SmsSet from a database: " + ta.toString() + "\n" + e1.getMessage(),
					SmppConstants.STATUS_SYSERR, MAPErrorCode.systemFailure, null, e1);
		}
		sms.setSmsSet(smsSet);

		long messageId = this.smppServerSessions.getNextMessageId();
		sms.setMessageId(messageId);
		//sms.setMoMessageRef(smsDeliverTpdu.getMessageReference());

		// TODO: process case when smsSubmitTpdu.getRejectDuplicates()==true: we need reject message with same MessageId+same source and dest addresses ?

		return sms;
	}	

	private void processSms(Sms sms, PersistenceRAInterface store) throws SmscProcessingException {
		try {
			// TODO: we can make this some check will we send this message or not

			store.createLiveSms(sms);
			store.setNewMessageScheduled(sms.getSmsSet(), MessageUtil.computeDueDate(MessageUtil.computeFirstDueDelay()));
		} catch (PersistenceException e) {
			throw new SmscProcessingException("MO PersistenceException when storing LIVE_SMS : " + e.getMessage(), SmppConstants.STATUS_SUBMITFAIL,
					MAPErrorCode.systemFailure, null, e);
		}
	}

	public enum MoProcessingState {
		OnlyRequestRecieved,
		OtherDataRecieved,
	}

	public abstract void setProcessingState(MoProcessingState processingState);

	public abstract MoProcessingState getProcessingState();

}
