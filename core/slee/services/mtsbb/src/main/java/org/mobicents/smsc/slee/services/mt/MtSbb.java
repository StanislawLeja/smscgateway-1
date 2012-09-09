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

import java.sql.Timestamp;

import javax.slee.ActivityContextInterface;
import javax.slee.EventContext;

import org.mobicents.protocols.ss7.indicator.NatureOfAddress;
import org.mobicents.protocols.ss7.indicator.NumberingPlan;
import org.mobicents.protocols.ss7.indicator.RoutingIndicator;
import org.mobicents.protocols.ss7.map.api.MAPApplicationContext;
import org.mobicents.protocols.ss7.map.api.MAPApplicationContextName;
import org.mobicents.protocols.ss7.map.api.MAPException;
import org.mobicents.protocols.ss7.map.api.dialog.MAPRefuseReason;
import org.mobicents.protocols.ss7.map.api.primitives.IMSI;
import org.mobicents.protocols.ss7.map.api.primitives.ISDNAddressString;
import org.mobicents.protocols.ss7.map.api.service.sms.ForwardShortMessageRequest;
import org.mobicents.protocols.ss7.map.api.service.sms.ForwardShortMessageResponse;
import org.mobicents.protocols.ss7.map.api.service.sms.MAPDialogSms;
import org.mobicents.protocols.ss7.map.api.service.sms.MtForwardShortMessageRequest;
import org.mobicents.protocols.ss7.map.api.service.sms.MtForwardShortMessageResponse;
import org.mobicents.protocols.ss7.map.api.service.sms.SM_RP_DA;
import org.mobicents.protocols.ss7.map.api.service.sms.SM_RP_OA;
import org.mobicents.protocols.ss7.map.api.smstpdu.AbsoluteTimeStamp;
import org.mobicents.protocols.ss7.map.api.smstpdu.AddressField;
import org.mobicents.protocols.ss7.map.api.smstpdu.DataCodingScheme;
import org.mobicents.protocols.ss7.map.api.smstpdu.NumberingPlanIdentification;
import org.mobicents.protocols.ss7.map.api.smstpdu.TypeOfNumber;
import org.mobicents.protocols.ss7.map.api.smstpdu.UserDataHeader;
import org.mobicents.protocols.ss7.map.api.smstpdu.UserDataHeaderElement;
import org.mobicents.protocols.ss7.map.service.sms.SmsSignalInfoImpl;
import org.mobicents.protocols.ss7.map.smstpdu.AbsoluteTimeStampImpl;
import org.mobicents.protocols.ss7.map.smstpdu.AddressFieldImpl;
import org.mobicents.protocols.ss7.map.smstpdu.ProtocolIdentifierImpl;
import org.mobicents.protocols.ss7.map.smstpdu.SmsDeliverTpduImpl;
import org.mobicents.protocols.ss7.map.smstpdu.UserDataImpl;
import org.mobicents.protocols.ss7.sccp.parameter.GT0100;
import org.mobicents.protocols.ss7.sccp.parameter.SccpAddress;
import org.mobicents.protocols.ss7.tcap.asn.ApplicationContextName;
import org.mobicents.slee.resource.map.events.DialogProviderAbort;
import org.mobicents.slee.resource.map.events.DialogReject;
import org.mobicents.slee.resource.map.events.DialogTimeout;
import org.mobicents.slee.resource.map.events.ErrorComponent;
import org.mobicents.smsc.slee.services.smpp.server.events.SmsEvent;

import com.cloudhopper.smpp.SmppConstants;
import com.cloudhopper.smpp.tlv.Tlv;
import com.cloudhopper.smpp.tlv.TlvConvertException;

/**
 * 
 * @author amit bhayani
 * 
 */
public abstract class MtSbb extends MtCommonSbb implements MtForwardSmsInterface {

	private static final String className = "MtSbb";

	private MAPApplicationContext mtFoSMSMAPApplicationContext = null;

	public MtSbb() {
		super(className);
	}

	/**
	 * Components Events override from MtCommonSbb that we care
	 */

	@Override
	public void onErrorComponent(ErrorComponent event, ActivityContextInterface aci) {
		super.onErrorComponent(event, aci);
		// TODO : Take care of error condition and Store and Forward

		// TODO : Its possible to receive two MAP Components in same TCAP
		// Dialog, one error and other informServiceCenter. Look at packet 28 of
		// wiresharktrace smsc_sv01apsmsc01.pcap. Handle this situation
	}

	/**
	 * Dialog Events override from MtCommonSbb that we care
	 */
	@Override
	public void onDialogProviderAbort(DialogProviderAbort evt, ActivityContextInterface aci) {
		super.onDialogProviderAbort(evt, aci);

		// TODO : SmsEvent should now be handed to StoreAndForwardSbb to store
		// this event.

		// TODO : Set flag for this MSISDN so no more Mt process is tried,
		// rather handed to Mt directly
	}

	@Override
	public void onDialogTimeout(DialogTimeout evt, ActivityContextInterface aci) {
		super.onDialogTimeout(evt, aci);

		// TODO : SmsEvent should now be handed to StoreAndForwardSbb to store
		// this event.

		// TODO : Set flag for this MSISDN so no more Mt process is tried,
		// rather handed to Mt directly
	}

	@Override
	public void onDialogReject(DialogReject evt, ActivityContextInterface aci) {
		MAPRefuseReason mapRefuseReason = evt.getRefuseReason();

		// If ACN not supported, lets use the new one suggested
		if (mapRefuseReason == MAPRefuseReason.ApplicationContextNotSupported) {
			// lets detach so we don't get onDialogRelease() which will start
			// delivering SMS waiting in queue for same MSISDN
			aci.detach(this.sbbContext.getSbbLocalObject());

			// Now send new SRI with supported ACN
			ApplicationContextName tcapApplicationContextName = evt.getAlternativeApplicationContext();
			MAPApplicationContext supportedMAPApplicationContext = MAPApplicationContext
					.getInstance(tcapApplicationContextName.getOid());

			SmsEvent event = this.getOriginalSmsEvent();

			this.sendMtSms(event, supportedMAPApplicationContext);

		} else {
			super.onDialogReject(evt, aci);
		}
	}

	/**
	 * SMS Event Handlers
	 */

	/**
	 * Received MT SMS. This is error we should never receive this
	 * 
	 * @param evt
	 * @param aci
	 */
	public void onForwardShortMessageRequest(ForwardShortMessageRequest evt, ActivityContextInterface aci) {
		this.logger.severe("Received FORWARD_SHORT_MESSAGE_REQUEST = " + evt);
	}

	/**
	 * Received ACK for MT Forward SMS sent earlier
	 * 
	 * @param evt
	 * @param aci
	 */
	public void onForwardShortMessageResponse(ForwardShortMessageResponse evt, ActivityContextInterface aci) {
		if (this.logger.isInfoEnabled()) {
			this.logger.info("Received FORWARD_SHORT_MESSAGE_RESPONSE = " + evt);
		}
		this.handleSmsResponse(evt.getMAPDialog(), aci);
	}

	/**
	 * Received MT SMS. This is error we should never receive this
	 * 
	 * @param evt
	 * @param aci
	 */
	public void onMtForwardShortMessageRequest(MtForwardShortMessageRequest evt, ActivityContextInterface aci) {
		this.logger.severe("Received MT_FORWARD_SHORT_MESSAGE_REQUEST = " + evt);
	}

	/**
	 * Received ACK for MT Forward SMS sent earlier
	 * 
	 * @param evt
	 * @param aci
	 */
	public void onMtForwardShortMessageResponse(MtForwardShortMessageResponse evt, ActivityContextInterface aci) {
		if (this.logger.isInfoEnabled()) {
			this.logger.info("Received MT_FORWARD_SHORT_MESSAGE_RESPONSE = " + evt);
		}

		this.handleSmsResponse(evt.getMAPDialog(), aci);
	}

	/**
	 * SBB Local Object Methods
	 * 
	 * @throws MAPException
	 */
	@Override
	public void setupMtForwardShortMessageRequest(ISDNAddressString networkNode, IMSI imsi,
			EventContext nullActivityEventContext) {
		if (this.logger.isInfoEnabled()) {
			this.logger.info("Received setupMtForwardShortMessageRequestIndication ISDNAddressString= " + networkNode
					+ " nullActivityEventContext" + nullActivityEventContext);
		}

		SccpAddress networkNodeSccpAddress = this.getMSCSccpAddress(networkNode);

		SM_RP_DA sm_RP_DA = this.mapParameterFactory.createSM_RP_DA(imsi);

		SM_RP_OA sm_RP_OA = this.mapParameterFactory.createSM_RP_OA_ServiceCentreAddressOA(this
				.getServiceCenterAddressString());

		this.setNullActivityEventContext(nullActivityEventContext);
		this.setNetworkNode(networkNodeSccpAddress);
		this.setSmRpDa(sm_RP_DA);
		this.setSmRpOa(sm_RP_OA);

		SmsEvent smsEvent = (SmsEvent) nullActivityEventContext.getEvent();

		this.sendMtSms(smsEvent, this.getMtFoSMSMAPApplicationContext());
	}

	/**
	 * CMPs
	 */

	/**
	 * Set the ISDNAddressString of network node where Mt SMS is to be submitted
	 * 
	 * @param networkNode
	 */
	public abstract void setNetworkNode(SccpAddress sccpAddress);

	public abstract SccpAddress getNetworkNode();

	/**
	 * Set the IMSI of destination MSISDN
	 * 
	 * @param imsi
	 */
	public abstract void setServiceCentreTimeStamp(AbsoluteTimeStamp serviceCentreTimeStamp);

	public abstract AbsoluteTimeStamp getServiceCentreTimeStamp();

	/**
	 * Set the message count as how many sliced SMS for this one message
	 * 
	 * @param mesageSegmentCount
	 */
	public abstract void setMessageSegmentCount(int mesageSegmentCount);

	public abstract int getMessageSegmentCount();

	/**
	 * Set the counter as which SMS is sent. Max sending can be equal to
	 * messageSegmentCount
	 * 
	 * @param mesageSegmentNumber
	 */
	public abstract void setMessageSegmentNumber(int mesageSegmentNumber);

	public abstract int getMessageSegmentNumber();

	public abstract void setMessageReferenceNumber(int mesageReferenceNumber);

	public abstract int getMessageReferenceNumber();

	/**
	 * Destination Address
	 * 
	 * @param sm_rp_da
	 */
	public abstract void setSmRpDa(SM_RP_DA sm_rp_da);

	public abstract SM_RP_DA getSmRpDa();

	/**
	 * Originating Address
	 * 
	 * @param sm_rp_oa
	 */
	public abstract void setSmRpOa(SM_RP_OA sm_rp_oa);

	public abstract SM_RP_OA getSmRpOa();

	/**
	 * DataCodingScheme of current SMS, only GSM 7bit, 8bit and UCS2 is
	 * supported
	 * 
	 * @param dataCodingScheme
	 */
	public abstract void setDataCodingScheme(DataCodingScheme dataCodingScheme);

	public abstract DataCodingScheme getDataCodingScheme();

	/**
	 * Private Methods
	 */

	private void handleSmsResponse(MAPDialogSms mapDialogSms, ActivityContextInterface aci) {
		SmsEvent smsEvent = this.getOriginalSmsEvent();

		int mesageSegmentCount = this.getMessageSegmentCount();
		int mesageSegmentNumber = this.getMessageSegmentNumber();
		if (mesageSegmentCount != 0 && (mesageSegmentCount >= mesageSegmentNumber)) {
			// we have more messages to be sent yet
			// try {
			this.sendMtSms(smsEvent, mapDialogSms.getApplicationContext());

			// detach from aci so we don't get back onDialogReleased
			aci.detach(this.sbbContext.getSbbLocalObject());
			// } catch (MAPException e) {
			// this.logger.severe(String.format("Exception while trying to send slice SMS for SmsEvent=%s",
			// smsEvent),
			// e);
			//
			// // Lets send back failure delivery report
			// try {
			// if (smsEvent.getSystemId() != null) {
			// sendFailureDeliverSmToEsms(smsEvent);
			// } else {
			// // TODO : This is destined for Mobile user, send
			// // SMS-STATUS-REPORT
			// }
			// } catch (Exception ex) {
			// this.logger.severe(String.format(
			// "Exception while trying to send failure Delivery Report for SmsEvent=%s",
			// smsEvent), ex);
			// }
			//
			// }
		} else {

			if (smsEvent != null) {
				try {
					if (smsEvent.getSystemId() != null) {
						sendSuccessDeliverSmToEsms(smsEvent);
					} else {
						// TODO : This is destined for Mobile user, send
						// SMS-STATUS-REPORT
					}
				} catch (Exception e) {
					this.logger.severe(String.format(
							"Exception while trying to send Success Delivery Report for SmsEvent=%s", smsEvent), e);
				}
			}
		}
	}

	private byte[] sliceMessage(byte[] shortMessage, DataCodingScheme dataCodingScheme, int mesageSegmentNumber) {
		byte[] slicedMessage = null;
		int srcPos = 0;
		int destPos = 0;
		int length = 0;
		switch (dataCodingScheme.getCharacterSet()) {
		case GSM7:
			srcPos = (mesageSegmentNumber - 1) * 152;
			length = Math.min(152, (shortMessage.length - srcPos));
			break;
		case GSM8:
			srcPos = (mesageSegmentNumber - 1) * 134;
			length = Math.min(134, (shortMessage.length - srcPos));
			break;
		case UCS2:
			srcPos = (mesageSegmentNumber - 1) * 134;
			length = Math.min(134, (shortMessage.length - srcPos));
			break;
		}
		slicedMessage = new byte[length];
		System.arraycopy(shortMessage, srcPos, slicedMessage, destPos, length);
		return slicedMessage;
	}

	private void sendPartMtSms(MAPDialogSms mapDialogSms, SmsEvent smsEvent, SM_RP_DA sm_RP_DA, SM_RP_OA sm_RP_OA,
			DataCodingScheme dataCodingScheme, AbsoluteTimeStamp serviceCentreTimeStamp, int mesageSegmentCount,
			int mesageSegmentNumber, int messageReferenceNumber) throws MAPException {

		MAPApplicationContext mapApplicationContext = mapDialogSms.getApplicationContext();
		byte[] shortMessage = smsEvent.getShortMessage();

		// TODO : Can this be constant?
		ProtocolIdentifierImpl pi = new ProtocolIdentifierImpl(0);
		UserDataHeader userDataHeader = null;
		boolean moreMessagesToSend = false;

		if (mesageSegmentCount != 0) {
			if (mesageSegmentCount > mesageSegmentNumber) {
				// Check if we have more messages to be sent
				moreMessagesToSend = true;
			}

			userDataHeader = this.mapSmsTpduParameterFactory.createUserDataHeader();
			UserDataHeaderElement concatenatedShortMessagesIdentifier = this.mapSmsTpduParameterFactory
					.createConcatenatedShortMessagesIdentifier(false, messageReferenceNumber, mesageSegmentCount,
							mesageSegmentNumber);
			userDataHeader.addInformationElement(concatenatedShortMessagesIdentifier);

			// Now slice message
			shortMessage = this.sliceMessage(shortMessage, dataCodingScheme, mesageSegmentNumber);
		}

		UserDataImpl ud = new UserDataImpl(new String(shortMessage), dataCodingScheme, userDataHeader, null);

		SmsDeliverTpduImpl smsDeliverTpduImpl = new SmsDeliverTpduImpl(moreMessagesToSend, false, false, true,
				this.getSmsTpduOriginatingAddress(smsEvent.getSourceAddrTon(), smsEvent.getSourceAddrNpi(),
						smsEvent.getSourceAddr()), pi, serviceCentreTimeStamp, ud);

		SmsSignalInfoImpl SmsSignalInfoImpl = new SmsSignalInfoImpl(smsDeliverTpduImpl, null);

		switch (mapApplicationContext.getApplicationContextVersion()) {
		case version3:
			mapDialogSms.addMtForwardShortMessageRequest(sm_RP_DA, sm_RP_OA, SmsSignalInfoImpl, false, null);
			break;
		case version2:
			mapDialogSms.addForwardShortMessageRequest(sm_RP_DA, sm_RP_OA, SmsSignalInfoImpl, false);
			break;
		default:
			// TODO take care of this, but should this ever happen?
			logger.severe(String.format("Trying to send Mt SMS with version=%d. This is serious!!",
					mapApplicationContext.getApplicationContextVersion().getVersion()));
			break;
		}

		// Lets increase the mesageSegmentNumber for next cycle
		this.setMessageSegmentNumber(mesageSegmentNumber + 1);

		ActivityContextInterface mtFOSmsDialogACI = this.mapAcif.getActivityContextInterface(mapDialogSms);
		mtFOSmsDialogACI.attach(this.sbbContext.getSbbLocalObject());

		mapDialogSms.send();
	}

	private void sendMtSms(SmsEvent smsEvent, MAPApplicationContext mapApplicationContext) {
		MAPDialogSms mapDialogSms = null;
		try {
			mapDialogSms = this.mapProvider.getMAPServiceSms().createNewDialog(mapApplicationContext,
					this.getServiceCenterSccpAddress(), null, this.getNetworkNode(), null);

			SM_RP_DA sm_RP_DA = this.getSmRpDa();

			SM_RP_OA sm_RP_OA = this.getSmRpOa();

			DataCodingScheme dataCodingScheme = this.getDataCodingScheme();
			if (dataCodingScheme == null) {
				dataCodingScheme = this.mapSmsTpduParameterFactory.createDataCodingScheme(smsEvent.getDataCoding());
				this.setDataCodingScheme(dataCodingScheme);
			}

			// TODO : Should this be SubmitDate or currentDate?
			AbsoluteTimeStamp serviceCentreTimeStamp = this.getServiceCentreTimeStamp();

			if (serviceCentreTimeStamp == null) {
				Timestamp submitDate = smsEvent.getSubmitDate();

				// TODO : TimeZone should be configurable
				serviceCentreTimeStamp = new AbsoluteTimeStampImpl((submitDate.getYear() % 100),
						(submitDate.getMonth() + 1), submitDate.getDate(), submitDate.getHours(),
						submitDate.getMinutes(), submitDate.getSeconds(), (submitDate.getTimezoneOffset() / 15));

				this.setServiceCentreTimeStamp(serviceCentreTimeStamp);
			}

			byte[] shortMessage = smsEvent.getShortMessage();

			// How many sliced SMS to be sent
			int mesageSegmentCount = this.getMessageSegmentCount();
			int mesageSegmentNumber = this.getMessageSegmentNumber();
			int messageReferenceNumber = this.getMessageReferenceNumber();

			if (mesageSegmentCount == 0) {

				// Counter of which SMS is to be delivered now. Starts from 1
				mesageSegmentNumber = 1;

				// TODO messageReferenceNumber should be generated
				messageReferenceNumber = 1;

				Tlv sarMsgRefNum = smsEvent.getOptionalParameter(SmppConstants.TAG_SAR_MSG_REF_NUM);
				Tlv sarTotalSegments = smsEvent.getOptionalParameter(SmppConstants.TAG_SAR_TOTAL_SEGMENTS);
				Tlv sarSegmentSeqnum = smsEvent.getOptionalParameter(SmppConstants.TAG_SAR_SEGMENT_SEQNUM);

				if (sarMsgRefNum != null && sarTotalSegments != null && sarSegmentSeqnum != null) {
					mesageSegmentCount = sarTotalSegments.getValueAsUnsignedByte();
					mesageSegmentNumber = sarSegmentSeqnum.getValueAsUnsignedByte();
					messageReferenceNumber = sarMsgRefNum.getValueAsUnsignedShort();
				} else {

					switch (dataCodingScheme.getCharacterSet()) {
					case GSM7:
						if (shortMessage.length > 160) {
							// we need concatination. Max 152
							mesageSegmentCount = shortMessage.length / 152;
							if (shortMessage.length % 152 != 0) {
								mesageSegmentCount++;
							}
						}
						break;
					case GSM8:
						if (shortMessage.length > 140) {
							// we need concatination. Max 134
							if (shortMessage.length % 134 != 0) {
								mesageSegmentCount++;
							}
						}
						break;
					case UCS2:
						if (shortMessage.length > 140) {
							// we need concatination. Max 134
							if (shortMessage.length % 134 != 0) {
								mesageSegmentCount++;
							}
						}
						break;
					}
				}

				this.setMessageSegmentCount(mesageSegmentCount);
				this.setMessageSegmentNumber(mesageSegmentNumber);
				this.setMessageReferenceNumber(messageReferenceNumber);
			}

			// TODO : Take care of esm_class to include UDHI. See SMPP specs

			this.sendPartMtSms(mapDialogSms, smsEvent, sm_RP_DA, sm_RP_OA, dataCodingScheme, serviceCentreTimeStamp,
					mesageSegmentCount, mesageSegmentNumber, messageReferenceNumber);

		} catch (MAPException e) {
			// TODO : Take care of error
			logger.severe("Error while trying to send MtForwardShortMessageRequestIndication", e);
			// something horrible, release MAPDialog and free resources
			if (mapDialogSms != null) {
				mapDialogSms.release();
			}

			MtActivityContextInterface mtSbbActivityContextInterface = this.asSbbActivityContextInterface(this
					.getNullActivityEventContext().getActivityContextInterface());
			this.resumeNullActivityEventDelivery(mtSbbActivityContextInterface, this.getNullActivityEventContext());
		} catch (TlvConvertException e) {
			// TODO : Take care of error
			logger.severe("TlvConvertException while trying to send MtForwardShortMessageRequestIndication", e);
			// something horrible, release MAPDialog and free resources
			if (mapDialogSms != null) {
				mapDialogSms.release();
			}

			MtActivityContextInterface mtSbbActivityContextInterface = this.asSbbActivityContextInterface(this
					.getNullActivityEventContext().getActivityContextInterface());
			this.resumeNullActivityEventDelivery(mtSbbActivityContextInterface, this.getNullActivityEventContext());
		}
	}

	private MAPApplicationContext getMtFoSMSMAPApplicationContext() {
		if (this.mtFoSMSMAPApplicationContext == null) {
			this.mtFoSMSMAPApplicationContext = MAPApplicationContext.getInstance(
					MAPApplicationContextName.shortMsgMTRelayContext, this.maxMAPApplicationContextVersion);
		}
		return this.mtFoSMSMAPApplicationContext;
	}

	private SccpAddress getMSCSccpAddress(ISDNAddressString networkNodeNumber) {

		// TODO : use the networkNodeNumber also to derive if its
		// International / ISDN?
		GT0100 gt = new GT0100(0, NumberingPlan.ISDN_TELEPHONY, NatureOfAddress.INTERNATIONAL,
				networkNodeNumber.getAddress());
		return new SccpAddress(RoutingIndicator.ROUTING_BASED_ON_GLOBAL_TITLE, 0, gt,
				smscPropertiesManagement.getMscSsn());
	}

	private AddressField getSmsTpduOriginatingAddress(byte ton, byte npi, String address) {
		return new AddressFieldImpl(TypeOfNumber.getInstance(ton), NumberingPlanIdentification.getInstance(npi),
				address);
	}

}
