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

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
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
import org.mobicents.smsc.cassandra.Sms;
import org.mobicents.smsc.cassandra.SmsSet;
import org.mobicents.smsc.cassandra.TargetAddress;
import org.mobicents.smsc.slee.resources.smpp.server.SmppSessions;
import org.mobicents.smsc.slee.resources.smpp.server.SmppTransaction;
import org.mobicents.smsc.slee.resources.smpp.server.SmppTransactionACIFactory;
import org.mobicents.smsc.slee.resources.smpp.server.events.PduRequestTimeout;

import org.mobicents.smsc.slee.resources.persistence.MessageUtil;
import org.mobicents.smsc.slee.resources.persistence.PersistenceRAInterface;
import org.mobicents.smsc.slee.resources.persistence.SmppExtraConstants;
import org.mobicents.smsc.slee.resources.persistence.SmscProcessingException;
import org.mobicents.smsc.slee.services.charging.ChargingSbbLocalObject;
import org.mobicents.smsc.slee.services.charging.ChargingType;
import org.mobicents.smsc.smpp.Esme;
import org.mobicents.smsc.smpp.SmppEncodingForUCS2;
import org.mobicents.smsc.smpp.SmscPropertiesManagement;
import org.mobicents.smsc.smpp.SmscStatProvider;

import com.cloudhopper.smpp.SmppConstants;
import com.cloudhopper.smpp.pdu.BaseSm;
import com.cloudhopper.smpp.pdu.DataSmResp;
import com.cloudhopper.smpp.pdu.DeliverSmResp;
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

    private static final ResourceAdaptorTypeID PERSISTENCE_ID = new ResourceAdaptorTypeID("PersistenceResourceAdaptorType", "org.mobicents", "1.0");
    private static final String LINK = "PersistenceResourceAdaptor";
    
	protected Tracer logger;
	private SbbContextExt sbbContext;

	private SmppTransactionACIFactory smppServerTransactionACIFactory = null;
	protected SmppSessions smppServerSessions = null;
	protected PersistenceRAInterface persistence = null;

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
//        long l2 = Date.parse(event.getServiceType());
//        Date dt0 = new Date(l2);
        Date dt0 = new Date();
        Date dt1 = new Date();
        // TODO remove it ...........................

        SmppTransaction smppServerTransaction = (SmppTransaction) aci.getActivity();
		Esme esme = smppServerTransaction.getEsme();
		String esmeName = esme.getName();

		if (this.logger.isInfoEnabled()) {
			this.logger.info("\nReceived SUBMIT_SM = " + event + " from Esme name=" + esmeName);
		}

		Sms sms;
		try {
			TargetAddress ta = createDestTargetAddress(event);
			PersistenceRAInterface store = getStore();
			TargetAddress lock = store.obtainSynchroObject(ta);

			try {
				synchronized (lock) {
                    sms = this.createSmsEvent(event, esme, ta, store);
					this.processSms(sms, store);
				}
			} finally {
				store.releaseSynchroObject(lock);
			}
		} catch (SmscProcessingException e1) {
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

		if (this.logger.isInfoEnabled()) {
			this.logger.info("Received DATA_SM = " + event + " from Esme name=" + esmeName);
		}

		Sms sms;
		try {
			TargetAddress ta = createDestTargetAddress(event);
			PersistenceRAInterface store = getStore();
			TargetAddress lock = store.obtainSynchroObject(ta);

			try {
				synchronized (lock) {
					sms = this.createSmsEvent(event, esme, ta, store);
					this.processSms(sms, store);
				}
			} finally {
				store.releaseSynchroObject(lock);
			}
		} catch (SmscProcessingException e1) {
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

	private TargetAddress createDestTargetAddress(BaseSm event) throws SmscProcessingException {
		if (event.getDestAddress() == null || event.getDestAddress().getAddress() == null || event.getDestAddress().getAddress().isEmpty()) {
			throw new SmscProcessingException("DestAddress digits are absent", SmppConstants.STATUS_INVDSTADR, MAPErrorCode.systemFailure, null);
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
			throw new SmscProcessingException("DestAddress TON not supported: " + event.getDestAddress().getTon(), SmppConstants.STATUS_INVDSTTON,
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
			throw new SmscProcessingException("DestAddress NPI not supported: " + event.getDestAddress().getNpi(), SmppConstants.STATUS_INVDSTNPI,
					MAPErrorCode.systemFailure, null);
		}

		TargetAddress ta = new TargetAddress(destTon, destNpi, event.getDestAddress().getAddress());
		return ta;
	}

	public void onDeliverSm(com.cloudhopper.smpp.pdu.DeliverSm event, ActivityContextInterface aci) {
//		logger.severe(String.format("onDeliverSm : this must not be", event));
//
//		SmppTransaction smppServerTransaction = (SmppTransaction) aci.getActivity();
//		Esme esme = smppServerTransaction.getEsme();
//		String esmeName = esme.getName();
//
//		if (this.logger.isInfoEnabled()) {
//			this.logger.info("Received DELIVER_SM = " + event + " from Esme name=" + esmeName);
//		}

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

		SmppTransaction smppServerTransaction = (SmppTransaction) aci.getActivity();
        Esme esme = smppServerTransaction.getEsme();
        String esmeName = esme.getName();

        if (this.logger.isInfoEnabled()) {
            this.logger.info("\nReceived DELIVER_SM = " + event + " from Esme name=" + esmeName);
        }

        Sms sms;
        try {
            TargetAddress ta = createDestTargetAddress(event);
            PersistenceRAInterface store = getStore();
            TargetAddress lock = store.obtainSynchroObject(ta);

            try {
                synchronized (lock) {
                    sms = this.createSmsEvent(event, esme, ta, store);
                    this.processSms(sms, store);
                }
            } finally {
                store.releaseSynchroObject(lock);
            }
        } catch (SmscProcessingException e1) {
            this.logger.severe(e1.getMessage(), e1);

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
			
			this.persistence = (PersistenceRAInterface) this.sbbContext.getResourceAdaptorInterface(PERSISTENCE_ID, LINK);
		} catch (Exception ne) {
			logger.severe("Could not set SBB context:", ne);
		}
	}

	@Override
	public void unsetSbbContext() {
		// TODO Auto-generated method stub

	}

	protected Sms createSmsEvent(BaseSm event, Esme origEsme, TargetAddress ta, PersistenceRAInterface store) throws SmscProcessingException {

		Sms sms = new Sms();
		sms.setDbId(UUID.randomUUID());

		// checking parameters first
		if (event.getSourceAddress() == null || event.getSourceAddress().getAddress() == null || event.getDestAddress().getAddress().isEmpty()) {
			throw new SmscProcessingException("SourceAddress digits are absent", SmppConstants.STATUS_INVSRCADR, MAPErrorCode.systemFailure, null);
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
			throw new SmscProcessingException("SourceAddress TON not supported: " + event.getSourceAddress().getTon(), SmppConstants.STATUS_INVSRCTON,
					MAPErrorCode.systemFailure, null);
		}
        if (event.getSourceAddress().getTon() == SmppConstants.TON_ALPHANUMERIC) {
            // TODO: when alphanumerical orig address (TON_ALPHANUMERIC) - which should we NPI select
//            sms.setSourceAddrNpi(SmppConstants.NPI_UNKNOWN);
        } else {
            switch (event.getSourceAddress().getNpi()) {
            case SmppConstants.NPI_UNKNOWN:
                sms.setSourceAddrNpi(smscPropertiesManagement.getDefaultNpi());
                break;
            case SmppConstants.NPI_E164:
                sms.setSourceAddrNpi(event.getSourceAddress().getNpi());
                break;
            default:
                throw new SmscProcessingException("SourceAddress NPI not supported: " + event.getSourceAddress().getNpi(), SmppConstants.STATUS_INVSRCNPI,
                        MAPErrorCode.systemFailure, null);
            }
        }

        int dcs = event.getDataCoding();
        String err = MessageUtil.chechDataCodingSchemeSupport(dcs);
        if (err != null) {
            throw new SmscProcessingException("TxSmpp DataCoding scheme does not supported: " + dcs + " - " + err, SmppExtraConstants.ESME_RINVDCS,
                    MAPErrorCode.systemFailure, null);
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

		// short message data
		sms.setShortMessage(event.getShortMessage());

		if (event.getShortMessageLength() == 0) {
			// Probably the message_payload Optional Parameter is being used
			Tlv messagePaylod = event.getOptionalParameter(SmppConstants.TAG_MESSAGE_PAYLOAD);
			if (messagePaylod != null) {
				sms.setShortMessage(messagePaylod.getValue());
			}
		}
        if (sms.getShortMessage() == null) {
            sms.setShortMessage(new byte[0]);
        }

        int lenSolid = MessageUtil.getMaxSolidMessageBytesLength(dataCodingScheme);
        int lenSegmented = MessageUtil.getMaxSegmentedMessageBytesLength(dataCodingScheme);
        boolean udhPresent = (event.getEsmClass() & SmppConstants.ESM_CLASS_UDHI_MASK) != 0;
        Tlv sarMsgRefNum = event.getOptionalParameter(SmppConstants.TAG_SAR_MSG_REF_NUM);
        Tlv sarTotalSegments = event.getOptionalParameter(SmppConstants.TAG_SAR_TOTAL_SEGMENTS);
        Tlv sarSegmentSeqnum = event.getOptionalParameter(SmppConstants.TAG_SAR_SEGMENT_SEQNUM);
        boolean segmentTlvFlag = (sarMsgRefNum != null && sarTotalSegments != null && sarSegmentSeqnum != null);

        if (smscPropertiesManagement.getSmppEncodingForUCS2() == SmppEncodingForUCS2.Utf8 && dataCodingScheme.getCharacterSet() == CharacterSet.UCS2) {
            // for UCS2 encoding we have to recode UTF-8 -> UCS2 here

            byte[] udhData = null;
            byte[] textPart = sms.getShortMessage();
            if (udhPresent && sms.getShortMessage().length > 2) {
                // UDH exists
                int udhLen = (textPart[0] & 0xFF) + 1;
                if (udhLen <= textPart.length) {
                    textPart = new byte[textPart.length - udhLen];
                    udhData = new byte[udhLen];
                    System.arraycopy(sms.getShortMessage(), udhLen, textPart, 0, textPart.length);
                    System.arraycopy(sms.getShortMessage(), 0, udhData, 0, udhLen);
                }
            }
            Charset utf8Charset = Charset.forName("UTF-8");
            ByteBuffer bb = ByteBuffer.wrap(textPart);
            CharBuffer cb = utf8Charset.decode(bb);
            Charset ucs2Charset = Charset.forName("UTF-16BE");
            ByteBuffer bf2 = ucs2Charset.encode(cb);
            byte[] buf2;
            if (udhData != null) {
                buf2 = new byte[udhData.length + bf2.limit()];
                bf2.get(buf2, udhData.length, bf2.limit());
                System.arraycopy(udhData, 0, buf2, 0, udhData.length);
            } else {
                buf2 = new byte[bf2.limit()];
                bf2.get(buf2);
            }
            sms.setShortMessage(buf2);
        }

		// checking max message length 
		if (udhPresent || segmentTlvFlag) {
			// here splitting by SMSC is not supported
			if (sms.getShortMessage().length > lenSolid) {
				throw new SmscProcessingException("Message length in bytes is too big for solid message: " + sms.getShortMessage().length + ">" + lenSolid,
						SmppConstants.STATUS_INVPARLEN, MAPErrorCode.systemFailure, null);
			}
		} else {
			// here splitting by SMSC is supported
			if (sms.getShortMessage().length > lenSegmented * 255) {
				throw new SmscProcessingException("Message length in bytes is too big for segmented message: " + sms.getShortMessage().length + ">" + lenSolid,
						SmppConstants.STATUS_INVPARLEN, MAPErrorCode.systemFailure, null);
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
				throw new SmscProcessingException("TlvConvertException when getting TAG_QOS_TIME_TO_LIVE tlv field: " + e.getMessage(),
						SmppConstants.STATUS_INVOPTPARAMVAL,MAPErrorCode.systemFailure, null, e);
			}
			validityPeriod = new Date(valTime);
		} else {
			try {
				validityPeriod = MessageUtil.parseSmppDate(event.getValidityPeriod());
			} catch (ParseException e) {
				throw new SmscProcessingException("ParseException when parsing ValidityPeriod field: " + e.getMessage(),
						SmppConstants.STATUS_INVEXPIRY,MAPErrorCode.systemFailure, null, e);
			}
		}
		MessageUtil.applyValidityPeriod(sms, validityPeriod, true);

		// ScheduleDeliveryTime processing
		Date scheduleDeliveryTime;
		try {
			scheduleDeliveryTime = MessageUtil.parseSmppDate(event.getScheduleDeliveryTime());
		} catch (ParseException e) {
			throw new SmscProcessingException("ParseException when parsing ScheduleDeliveryTime field: " + e.getMessage(),
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
        if (smscPropertiesManagement.getDatabaseType() == DatabaseType.Cassandra_1) {
            try {
                smsSet = store.obtainSmsSet(ta);
            } catch (PersistenceException e1) {
                throw new SmscProcessingException("PersistenceException when reading SmsSet from a database: " + ta.toString() + "\n" + e1.getMessage(),
                        SmppConstants.STATUS_SUBMITFAIL, MAPErrorCode.systemFailure, null, e1);
            }
        } else {
            smsSet = new SmsSet();
            smsSet.setDestAddr(ta.getAddr());
            smsSet.setDestAddrNpi(ta.getAddrNpi());
            smsSet.setDestAddrTon(ta.getAddrTon());
        }
        sms.setSmsSet(smsSet);

//		long messageId = this.smppServerSessions.getNextMessageId();
        long messageId = store.c2_getNextMessageId();
        SmscStatProvider.getInstance().setCurrentMessageId(messageId);
		sms.setMessageId(messageId);

		// TODO: process case when event.getReplaceIfPresent()==true: we need remove old message with same MessageId ?

		return sms;
	}

	private void processSms(Sms sms, PersistenceRAInterface store) throws SmscProcessingException {

        if (smscPropertiesManagement.isTxSmppCharging()) {
            ChargingSbbLocalObject chargingSbb = getChargingSbbObject();
            chargingSbb.setupChargingRequestInterface(ChargingType.TxSmppOrig, sms);
        } else {
            boolean storeAndForwMode = (sms.getEsmClass() & 0x03) == 0x03;

            // TODO ...................... direct launch
            storeAndForwMode = true;
            // TODO ...................... direct launch
            if (!storeAndForwMode) {
                // TODO ...................... direct launch

            } else {
                // store and forward
                try {
                    // TODO: we can make this some check will we send this
                    // message
                    // or not

                    sms.setStored(true);
                    if (smscPropertiesManagement.getDatabaseType() == DatabaseType.Cassandra_1) {
                        store.createLiveSms(sms);
                        if (sms.getScheduleDeliveryTime() == null)
                            store.setNewMessageScheduled(sms.getSmsSet(), MessageUtil.computeDueDate(MessageUtil.computeFirstDueDelay()));
                        else
                            store.setNewMessageScheduled(sms.getSmsSet(), sms.getScheduleDeliveryTime());
                    } else {
                        sms.setStored(true);
                        store.c2_scheduleMessage(sms);
                    }
                } catch (PersistenceException e) {
                    throw new SmscProcessingException("PersistenceException when storing LIVE_SMS : " + e.getMessage(), SmppConstants.STATUS_SUBMITFAIL,
                            MAPErrorCode.systemFailure, null, e);
                }
            }
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

