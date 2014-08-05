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

import java.nio.charset.Charset;
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
import javax.slee.Sbb;
import javax.slee.SbbContext;
import javax.slee.facilities.Tracer;
import javax.slee.resource.ResourceAdaptorTypeID;

import org.mobicents.protocols.ss7.map.api.errors.MAPErrorCode;
import org.mobicents.protocols.ss7.map.api.smstpdu.CharacterSet;
import org.mobicents.protocols.ss7.map.api.smstpdu.DataCodingScheme;
import org.mobicents.protocols.ss7.map.smstpdu.DataCodingSchemeImpl;
import org.mobicents.slee.ChildRelationExt;
import org.mobicents.slee.SbbContextExt;
import org.mobicents.smsc.cassandra.DatabaseType;
import org.mobicents.smsc.cassandra.PersistenceException;
import org.mobicents.smsc.domain.SmscPropertiesManagement;
import org.mobicents.smsc.domain.SmscStatAggregator;
import org.mobicents.smsc.domain.SmscStatProvider;
import org.mobicents.smsc.library.MessageUtil;
import org.mobicents.smsc.library.Sms;
import org.mobicents.smsc.library.SmsSet;
import org.mobicents.smsc.library.SmscProcessingException;
import org.mobicents.smsc.library.TargetAddress;
import org.mobicents.smsc.slee.resources.persistence.PersistenceRAInterface;
import org.mobicents.smsc.slee.resources.persistence.SmppExtraConstants;
import org.mobicents.smsc.slee.resources.scheduler.SchedulerRaSbbInterface;
import org.mobicents.smsc.slee.resources.smpp.server.SmppSessions;
import org.mobicents.smsc.slee.resources.smpp.server.SmppTransaction;
import org.mobicents.smsc.slee.resources.smpp.server.SmppTransactionACIFactory;
import org.mobicents.smsc.slee.resources.smpp.server.events.PduRequestTimeout;
import org.mobicents.smsc.slee.services.charging.ChargingSbbLocalObject;
import org.mobicents.smsc.slee.services.charging.ChargingMedium;
import org.mobicents.smsc.smpp.Esme;
import org.mobicents.smsc.smpp.SmppEncoding;

import com.cloudhopper.smpp.SmppConstants;
import com.cloudhopper.smpp.pdu.BaseSm;
import com.cloudhopper.smpp.pdu.DataSm;
import com.cloudhopper.smpp.pdu.DataSmResp;
import com.cloudhopper.smpp.pdu.DeliverSmResp;
import com.cloudhopper.smpp.pdu.SubmitSm;
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
	protected static SmscPropertiesManagement smscPropertiesManagement = SmscPropertiesManagement.getInstance();

    private static final ResourceAdaptorTypeID PERSISTENCE_ID = new ResourceAdaptorTypeID(
            "PersistenceResourceAdaptorType", "org.mobicents", "1.0");
    private static final String PERSISTENCE_LINK = "PersistenceResourceAdaptor";
    private static final ResourceAdaptorTypeID SCHEDULER_ID = new ResourceAdaptorTypeID(
            "SchedulerResourceAdaptorType", "org.mobicents", "1.0");
    private static final String SCHEDULER_LINK = "SchedulerResourceAdaptor";

	protected Tracer logger;
	private SbbContextExt sbbContext;

	private SmppTransactionACIFactory smppServerTransactionACIFactory = null;
	protected SmppSessions smppServerSessions = null;
	protected PersistenceRAInterface persistence = null;
	protected SchedulerRaSbbInterface scheduler = null;
	private SmscStatAggregator smscStatAggregator = SmscStatAggregator.getInstance();

	private static Charset utf8Charset = Charset.forName("UTF-8");
	private static Charset ucs2Charset = Charset.forName("UTF-16BE");
    private static Charset isoCharset = Charset.forName("ISO-8859-1");

	public TxSmppServerSbb() {
		// TODO Auto-generated constructor stub
	}

	public PersistenceRAInterface getStore() {
		return this.persistence;
	}

	/**
	 * Event Handlers
	 */

	public void onSubmitSm(com.cloudhopper.smpp.pdu.SubmitSm event, ActivityContextInterface aci) {
		// TODO remove it ...........................
		// long l2 = Date.parse(event.getServiceType());
		// Date dt0 = new Date(l2);
		Date dt0 = new Date();
		Date dt1 = new Date();
		// TODO remove it ...........................

		SmppTransaction smppServerTransaction = (SmppTransaction) aci.getActivity();
		Esme esme = smppServerTransaction.getEsme();
		String esmeName = esme.getName();

		if (this.logger.isFineEnabled()) {
			this.logger.fine("\nReceived SUBMIT_SM = " + event + " from Esme name=" + esmeName);
		}

		Sms sms;
		try {
			TargetAddress ta = createDestTargetAddress(event);
			PersistenceRAInterface store = getStore();
			TargetAddress lock = store.obtainSynchroObject(ta);

			try {
				synchronized (lock) {
					sms = this.createSmsEvent(event, esme, ta, store);
					this.processSms(sms, store, esme, event, null);
				}
			} finally {
				store.releaseSynchroObject(lock);
			}
		} catch (SmscProcessingException e1) {
            if (!e1.isSkipErrorLogging()) {
                this.logger.severe(e1.getMessage(), e1);
                smscStatAggregator.updateMsgInFailedAll();
            }

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
            smscStatAggregator.updateMsgInFailedAll();

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
            if (sms.getMessageDeliveryResultResponse() == null) {
                this.smppServerSessions.sendResponsePdu(esme, event, response);
            }
		} catch (Throwable e) {
			this.logger.severe("Error while trying to send SubmitSmResponse=" + response, e);
		}

		// TODO remove it ...........................
		Date dt3 = new Date();
		SmscStatProvider.getInstance().setParam1((int) (dt3.getTime() - dt0.getTime()));
		SmscStatProvider.getInstance().setParam2((int) (dt3.getTime() - dt1.getTime()));
		// TODO remove it ...........................

	}

	public void onDataSm(com.cloudhopper.smpp.pdu.DataSm event, ActivityContextInterface aci) {
		SmppTransaction smppServerTransaction = (SmppTransaction) aci.getActivity();
		Esme esme = smppServerTransaction.getEsme();
		String esmeName = esme.getName();

		if (this.logger.isFineEnabled()) {
			this.logger.fine("Received DATA_SM = " + event + " from Esme name=" + esmeName);
		}

		Sms sms;
		try {
			TargetAddress ta = createDestTargetAddress(event);
			PersistenceRAInterface store = getStore();
			TargetAddress lock = store.obtainSynchroObject(ta);

			try {
				synchronized (lock) {
					sms = this.createSmsEvent(event, esme, ta, store);
					this.processSms(sms, store, esme, null, event);
				}
			} finally {
				store.releaseSynchroObject(lock);
			}
		} catch (SmscProcessingException e1) {
            if (!e1.isSkipErrorLogging()) {
                this.logger.severe(e1.getMessage(), e1);
                smscStatAggregator.updateMsgInFailedAll();
            }

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
            smscStatAggregator.updateMsgInFailedAll();

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
            if (sms.getMessageDeliveryResultResponse() == null) {
                this.smppServerSessions.sendResponsePdu(esme, event, response);
            }
		} catch (Exception e) {
			this.logger.severe("Error while trying to send DataSmResponse=" + response, e);
		}
	}

	private TargetAddress createDestTargetAddress(BaseSm event) throws SmscProcessingException {
		if (event.getDestAddress() == null || event.getDestAddress().getAddress() == null
				|| event.getDestAddress().getAddress().isEmpty()) {
			throw new SmscProcessingException("DestAddress digits are absent", SmppConstants.STATUS_INVDSTADR,
					MAPErrorCode.systemFailure, null);
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
			throw new SmscProcessingException("DestAddress TON not supported: " + event.getDestAddress().getTon(),
					SmppConstants.STATUS_INVDSTTON, MAPErrorCode.systemFailure, null);
		}
		switch (event.getDestAddress().getNpi()) {
		case SmppConstants.NPI_UNKNOWN:
			destNpi = smscPropertiesManagement.getDefaultNpi();
			break;
		case SmppConstants.NPI_E164:
			destNpi = event.getDestAddress().getNpi();
			break;
		default:
			throw new SmscProcessingException("DestAddress NPI not supported: " + event.getDestAddress().getNpi(),
					SmppConstants.STATUS_INVDSTNPI, MAPErrorCode.systemFailure, null);
		}

		TargetAddress ta = new TargetAddress(destTon, destNpi, event.getDestAddress().getAddress());
		return ta;
	}

	public void onDeliverSm(com.cloudhopper.smpp.pdu.DeliverSm event, ActivityContextInterface aci) {
		SmppTransaction smppServerTransaction = (SmppTransaction) aci.getActivity();
		Esme esme = smppServerTransaction.getEsme();
		String esmeName = esme.getName();

		if (this.logger.isFineEnabled()) {
			this.logger.fine("\nReceived DELIVER_SM = " + event + " from Esme name=" + esmeName);
		}

		Sms sms;
		try {
			TargetAddress ta = createDestTargetAddress(event);
			PersistenceRAInterface store = getStore();
			TargetAddress lock = store.obtainSynchroObject(ta);

			try {
				synchronized (lock) {
					sms = this.createSmsEvent(event, esme, ta, store);
					this.processSms(sms, store, esme, null, null);
				}
			} finally {
				store.releaseSynchroObject(lock);
			}
		} catch (SmscProcessingException e1) {
            if (!e1.isSkipErrorLogging()) {
                this.logger.severe(e1.getMessage(), e1);
                smscStatAggregator.updateMsgInFailedAll();
            }

			DeliverSmResp response = event.createResponse();
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
            smscStatAggregator.updateMsgInFailedAll();

			DeliverSmResp response = event.createResponse();
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

		DeliverSmResp response = event.createResponse();
		response.setMessageId(((Long) sms.getMessageId()).toString());

		// Lets send the Response with success here
		try {
			this.smppServerSessions.sendResponsePdu(esme, event, response);
		} catch (Throwable e) {
			this.logger.severe("Error while trying to send SubmitSmResponse=" + response, e);
		}
	}

	public void onPduRequestTimeout(PduRequestTimeout event, ActivityContextInterface aci, EventContext eventContext) {
		logger.severe(String.format("\nonPduRequestTimeout : PduRequestTimeout=%s", event));
		// TODO : Handle this
	}

	public void onRecoverablePduException(RecoverablePduException event, ActivityContextInterface aci,
			EventContext eventContext) {
		logger.severe(String.format("\nonRecoverablePduException : RecoverablePduException=%s", event));
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

            this.persistence = (PersistenceRAInterface) this.sbbContext.getResourceAdaptorInterface(PERSISTENCE_ID, PERSISTENCE_LINK);
            this.scheduler = (SchedulerRaSbbInterface) this.sbbContext.getResourceAdaptorInterface(SCHEDULER_ID, SCHEDULER_LINK);
		} catch (Exception ne) {
			logger.severe("Could not set SBB context:", ne);
		}
	}

	@Override
	public void unsetSbbContext() {
		// TODO Auto-generated method stub

	}

	protected Sms createSmsEvent(BaseSm event, Esme origEsme, TargetAddress ta, PersistenceRAInterface store)
			throws SmscProcessingException {

		Sms sms = new Sms();
		sms.setDbId(UUID.randomUUID());
        sms.setOriginationType(Sms.OriginationType.SMPP);

		// checking parameters first
		if (event.getSourceAddress() == null || event.getSourceAddress().getAddress() == null
				|| event.getDestAddress().getAddress().isEmpty()) {
			throw new SmscProcessingException("SourceAddress digits are absent", SmppConstants.STATUS_INVSRCADR,
					MAPErrorCode.systemFailure, null);
		}
		sms.setSourceAddr(event.getSourceAddress().getAddress());
		switch (event.getSourceAddress().getTon()) {
		case SmppConstants.TON_UNKNOWN:
			sms.setSourceAddrTon(smscPropertiesManagement.getDefaultTon());
			break;
		case SmppConstants.TON_INTERNATIONAL:
			sms.setSourceAddrTon(event.getSourceAddress().getTon());
			break;
		case SmppConstants.TON_ALPHANUMERIC:
			sms.setSourceAddrTon(event.getSourceAddress().getTon());
			break;
		default:
			throw new SmscProcessingException("SourceAddress TON not supported: " + event.getSourceAddress().getTon(),
					SmppConstants.STATUS_INVSRCTON, MAPErrorCode.systemFailure, null);
		}
		if (event.getSourceAddress().getTon() == SmppConstants.TON_ALPHANUMERIC) {
			// TODO: when alphanumerical orig address (TON_ALPHANUMERIC) - which
			// should we NPI select
			// sms.setSourceAddrNpi(SmppConstants.NPI_UNKNOWN);
		} else {
			switch (event.getSourceAddress().getNpi()) {
			case SmppConstants.NPI_UNKNOWN:
				sms.setSourceAddrNpi(smscPropertiesManagement.getDefaultNpi());
				break;
			case SmppConstants.NPI_E164:
				sms.setSourceAddrNpi(event.getSourceAddress().getNpi());
				break;
			default:
				throw new SmscProcessingException("SourceAddress NPI not supported: "
						+ event.getSourceAddress().getNpi(), SmppConstants.STATUS_INVSRCNPI,
						MAPErrorCode.systemFailure, null);
			}
		}

		int dcs = event.getDataCoding();
		String err = MessageUtil.checkDataCodingSchemeSupport(dcs);
		if (err != null) {
			throw new SmscProcessingException("TxSmpp DataCoding scheme does not supported: " + dcs + " - " + err,
					SmppExtraConstants.ESME_RINVDCS, MAPErrorCode.systemFailure, null);
		}
		DataCodingScheme dataCodingScheme = new DataCodingSchemeImpl(dcs);
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

		boolean udhPresent = (event.getEsmClass() & SmppConstants.ESM_CLASS_UDHI_MASK) != 0;
		Tlv sarMsgRefNum = event.getOptionalParameter(SmppConstants.TAG_SAR_MSG_REF_NUM);
		Tlv sarTotalSegments = event.getOptionalParameter(SmppConstants.TAG_SAR_TOTAL_SEGMENTS);
		Tlv sarSegmentSeqnum = event.getOptionalParameter(SmppConstants.TAG_SAR_SEGMENT_SEQNUM);
		boolean segmentTlvFlag = (sarMsgRefNum != null && sarTotalSegments != null && sarSegmentSeqnum != null);

        // short message data
        byte[] data = event.getShortMessage();
        if (event.getShortMessageLength() == 0) {
            // Probably the message_payload Optional Parameter is being used
            Tlv messagePaylod = event.getOptionalParameter(SmppConstants.TAG_MESSAGE_PAYLOAD);
            if (messagePaylod != null) {
                data = messagePaylod.getValue();
            }
        }
        if (data == null) {
            data = new byte[0];
        }

        byte[] udhData;
        byte[] textPart;
        String msg;
        int messageLen;
        udhData = null;
        textPart = data;
        if (udhPresent && data.length > 2) {
            // UDH exists
            int udhLen = (textPart[0] & 0xFF) + 1;
            if (udhLen <= textPart.length) {
                textPart = new byte[textPart.length - udhLen];
                udhData = new byte[udhLen];
                System.arraycopy(data, udhLen, textPart, 0, textPart.length);
                System.arraycopy(data, 0, udhData, 0, udhLen);
            }
        }

        if (dataCodingScheme.getCharacterSet() == CharacterSet.GSM8) {
            msg = new String(textPart, isoCharset);
        } else if (dataCodingScheme.getCharacterSet() == CharacterSet.GSM7) {
            if (smscPropertiesManagement.getSmppEncodingForGsm7() == SmppEncoding.Utf8) {
                msg = new String(textPart, utf8Charset);
            } else {
                msg = new String(textPart, ucs2Charset);
            }
        } else {
            if (smscPropertiesManagement.getSmppEncodingForUCS2() == SmppEncoding.Utf8) {
                msg = new String(textPart, utf8Charset);
            } else {
                msg = new String(textPart, ucs2Charset);
            }
        }

        messageLen = MessageUtil.getMessageLengthInBytes(dataCodingScheme, msg.length());
        if (udhData != null)
            messageLen += udhData.length;

        sms.setShortMessageText(msg);
        sms.setShortMessageBin(udhData);

		// checking max message length
        int lenSolid = MessageUtil.getMaxSolidMessageBytesLength();
        int lenSegmented = MessageUtil.getMaxSegmentedMessageBytesLength();
        if (udhPresent || segmentTlvFlag) {
			// here splitting by SMSC is not supported
			if (messageLen > lenSolid) {
				throw new SmscProcessingException("Message length in bytes is too big for solid message: "
						+ messageLen + ">" + lenSolid, SmppConstants.STATUS_INVPARLEN,
						MAPErrorCode.systemFailure, null);
			}
		} else {
			// here splitting by SMSC is supported
			if (messageLen > lenSegmented * 255) {
				throw new SmscProcessingException("Message length in bytes is too big for segmented message: "
						+ messageLen + ">" + lenSegmented, SmppConstants.STATUS_INVPARLEN,
						MAPErrorCode.systemFailure, null);
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
				throw new SmscProcessingException("TlvConvertException when getting TAG_QOS_TIME_TO_LIVE tlv field: "
						+ e.getMessage(), SmppConstants.STATUS_INVOPTPARAMVAL, MAPErrorCode.systemFailure, null, e);
			}
			validityPeriod = new Date(valTime);
		} else {
			try {
				validityPeriod = MessageUtil.parseSmppDate(event.getValidityPeriod());
			} catch (ParseException e) {
				throw new SmscProcessingException(
						"ParseException when parsing ValidityPeriod field: " + e.getMessage(),
						SmppConstants.STATUS_INVEXPIRY, MAPErrorCode.systemFailure, null, e);
			}
		}
        MessageUtil.applyValidityPeriod(sms, validityPeriod, true, smscPropertiesManagement.getMaxValidityPeriodHours(),
                smscPropertiesManagement.getDefaultValidityPeriodHours());

		// ScheduleDeliveryTime processing
		Date scheduleDeliveryTime;
		try {
			scheduleDeliveryTime = MessageUtil.parseSmppDate(event.getScheduleDeliveryTime());
		} catch (ParseException e) {
			throw new SmscProcessingException("ParseException when parsing ScheduleDeliveryTime field: "
					+ e.getMessage(), SmppConstants.STATUS_INVSCHED, MAPErrorCode.systemFailure, null, e);
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
		if (smscPropertiesManagement.getDatabaseType() == DatabaseType.Cassandra_1) {
			try {
				smsSet = store.obtainSmsSet(ta);
			} catch (PersistenceException e1) {
				throw new SmscProcessingException("PersistenceException when reading SmsSet from a database: "
						+ ta.toString() + "\n" + e1.getMessage(), SmppConstants.STATUS_SUBMITFAIL,
						MAPErrorCode.systemFailure, null, e1);
			}
		} else {
			smsSet = new SmsSet();
			smsSet.setDestAddr(ta.getAddr());
			smsSet.setDestAddrNpi(ta.getAddrNpi());
			smsSet.setDestAddrTon(ta.getAddrTon());
			smsSet.addSms(sms);
		}
		sms.setSmsSet(smsSet);

		// long messageId = this.smppServerSessions.getNextMessageId();
		long messageId = store.c2_getNextMessageId();
		SmscStatProvider.getInstance().setCurrentMessageId(messageId);
		sms.setMessageId(messageId);

		// TODO: process case when event.getReplaceIfPresent()==true: we need
		// remove old message with same MessageId ?

		return sms;
	}

    private void processSms(Sms sms, PersistenceRAInterface store, Esme esme, SubmitSm eventSubmit, DataSm eventData) throws SmscProcessingException {
        // checking if SMSC is paused
        if (smscPropertiesManagement.isDeliveryPause() && !MessageUtil.isStoreAndForward(sms)) {
            SmscProcessingException e = new SmscProcessingException("SMSC is paused", SmppConstants.STATUS_SYSERR, 0, null);
            e.setSkipErrorLogging(true);
            throw e;
        }

		boolean withCharging = false;
		switch (smscPropertiesManagement.getTxSmppChargingType()) {
		case Selected:
			withCharging = esme.isChargingEnabled();
			break;
		case All:
			withCharging = true;
			break;
		}

        // transactional mode
        if ((eventSubmit != null || eventData != null) && MessageUtil.isTransactional(sms)) {
            MessageDeliveryResultResponseSmpp messageDeliveryResultResponse = new MessageDeliveryResultResponseSmpp(this.smppServerSessions, esme, eventSubmit,
                    eventData, sms.getMessageId());
            sms.setMessageDeliveryResultResponse(messageDeliveryResultResponse);
        }

		if (withCharging) {
			ChargingSbbLocalObject chargingSbb = getChargingSbbObject();
			chargingSbb.setupChargingRequestInterface(ChargingMedium.TxSmppOrig, sms);
		} else {
            boolean storeAndForwMode = MessageUtil.isStoreAndForward(sms);
			if (!storeAndForwMode) {
			    try {
                    this.scheduler.injectSmsOnFly(sms.getSmsSet());
                } catch (Exception e) {
                    throw new SmscProcessingException("Exception when runnung injectSmsOnFly(): " + e.getMessage(), SmppConstants.STATUS_SYSERR,
                            MAPErrorCode.systemFailure, e);
                }
			} else {
				// store and forward
				try {
					sms.setStored(true);
					if (smscPropertiesManagement.getDatabaseType() == DatabaseType.Cassandra_1) {
						store.createLiveSms(sms);
						if (sms.getScheduleDeliveryTime() == null)
                            store.setNewMessageScheduled(sms.getSmsSet(),
                                    MessageUtil.computeDueDate(MessageUtil.computeFirstDueDelay(smscPropertiesManagement.getFirstDueDelay())));
						else
							store.setNewMessageScheduled(sms.getSmsSet(), sms.getScheduleDeliveryTime());
					} else {
						store.c2_scheduleMessage(sms);
					}
				} catch (PersistenceException e) {
					throw new SmscProcessingException("PersistenceException when storing LIVE_SMS : " + e.getMessage(),
							SmppConstants.STATUS_SUBMITFAIL, MAPErrorCode.systemFailure, null, e);
				}
			}
            smscStatAggregator.updateMsgInReceivedAll();
            smscStatAggregator.updateMsgInReceivedSmpp();
		}
	}

	/**
	 * Get child ChargingSBB
	 * 
	 * @return
	 */
	public abstract ChildRelationExt getChargingSbb();

	private ChargingSbbLocalObject getChargingSbbObject() {
		ChildRelationExt relation = getChargingSbb();

		ChargingSbbLocalObject ret = (ChargingSbbLocalObject) relation.get(ChildRelationExt.DEFAULT_CHILD_NAME);
		if (ret == null) {
			try {
				ret = (ChargingSbbLocalObject) relation.create(ChildRelationExt.DEFAULT_CHILD_NAME);
			} catch (Exception e) {
				if (this.logger.isSevereEnabled()) {
					this.logger.severe("Exception while trying to creat ChargingSbb child", e);
				}
			}
		}
		return ret;
	}
}
