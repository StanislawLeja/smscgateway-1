package org.mobicents.smsc.library;

import org.apache.log4j.Logger;
import org.mobicents.smsc.mproc.DeliveryReceiptData;
import org.restcomm.smpp.parameter.TlvSet;

import java.text.SimpleDateFormat;
import java.util.Date;

public class CdrDetailedGenerator {
    private static final Logger logger = Logger.getLogger(CdrDetailedGenerator.class);

    public static final String CDR_EMPTY = "";
    public static final String CDR_SEPARATOR = ",";
    public static final String CDR_SUCCESS = "success";
    public static final String CDR_PARTIAL = "partial";
    public static final String CDR_FAILED = "failed";
    public static final String CDR_FAILED_IMSI = "failed_imsi";
    public static final String CDR_TEMP_FAILED = "temp_failed";
    public static final String CDR_OCS_REJECTED = "ocs_rejected";
    public static final String CDR_MPROC_REJECTED = "mproc_rejected";
    public static final String CDR_MPROC_DROPPED = "mproc_dropped";
    public static final String CDR_MPROC_DROP_PRE_DELIVERY = "mproc_drop_pre_delivery";

    public static final String CDR_MSG_TYPE_SUBMITSM = "SubmitSm";
    public static final String CDR_MSG_TYPE_SUBMITMULTI = "SubmitMulti";
    public static final String CDR_MSG_TYPE_DELIVERSM = "DeliverSm";
    public static final String CDR_MSG_TYPE_DATASM = "DataSm";
    public static final String CDR_MSG_TYPE_HTTP = "Http";
    public static final String CDR_MSG_TYPE_SIP = "Sip";
    public static final String CDR_MSG_TYPE_SS7 = "Ss7";

    public static final String CDR_SUCCESS_NO_REASON = "";

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

    public static void generateDetailedCdr(String message) {
        logger.debug(message);
    }

    public static void generateDetailedCdr(Sms sms, EventType eventType, ErrorCode errorCode, String messageType,
            long statusCode, int mprocRuleId, String sourceAddrAndPort, String destAddrAndPort, int seqNumber,
            boolean generateReceiptCdr, boolean generateDetailedCdr) {

        boolean mcDeliveryReceipt = sms.isMcDeliveryReceipt();
        String shortMessageText = sms.getShortMessageText();
        TlvSet tlvSet = sms.getTlvSet();
        Long tsA = sms.getTimestampA();
        Long tsB = sms.getTimestampB();
        Long tsC = sms.getTimestampC();
        Long messageId = sms.getMessageId();
        String origEsmeName = sms.getOrigEsmeName();
        String EL_messageId = "";
        String EL_correlationId = "";
        String SourceAddr = sms.getSourceAddr();
        String DestAddr = sms.getSmsSet().getDestAddr();
        int OrigNetworkId = sms.getOrigNetworkId();
        int NetworkId = sms.getOrigNetworkId();

        SmsExposureLayerData objSmsExposureLayerData = null;
        if(sms.getExposureLayerData() != null){
            objSmsExposureLayerData = new SmsExposureLayerData(sms.getExposureLayerData());
            EL_messageId = objSmsExposureLayerData.getMessageId();
            EL_correlationId = objSmsExposureLayerData.getCorrelationId();

        }

        if (!generateDetailedCdr) {
            return;
        }

        if (!generateReceiptCdr && mcDeliveryReceipt) {
            // we do not generate CDR's for receipt if generateReceiptCdr option is off
            return;
        }
        String timestamp = DATE_FORMAT.format(new Date());

        String dlrStatus = null;
        String origMessageID = null;

        DeliveryReceiptData deliveryReceiptData = null;
        if (shortMessageText != null && tlvSet != null) {
            deliveryReceiptData = MessageUtil.parseDeliveryReceipt(shortMessageText, tlvSet);
            if (deliveryReceiptData != null) {
                dlrStatus = deliveryReceiptData.getStatus();
                if (deliveryReceiptData.getTlvMessageState() != null) {
                    int tlvMessageState = deliveryReceiptData.getTlvMessageState();
                    if (tlvMessageState != 0 && dlrStatus != null)
                        if (!dlrStatus.substring(0, 4)
                                .equals(MessageState.fromInt(tlvMessageState).toString().substring(0, 5))) {
                            dlrStatus = "err";
                        }
                }
                origMessageID = deliveryReceiptData.getMessageId();
            }
        }

        String destIP = null, destPort = null;
        if (destAddrAndPort != null) {
            String[] parts = destAddrAndPort.split(":");
            destIP = parts[0];
            destPort = parts[1];
            if(sms.getDestIP() == null){
                sms.setDestIP(destIP);
            }
            if(sms.getDestPort() == null){
                sms.setDestPort(destPort);
            }
        }else{
            destIP = sms.getDestIP();
            destPort = sms.getDestPort();
        }

        String sourceIP = null, sourcePort = null;
        if (sourceAddrAndPort != null) {
            String[] parts = sourceAddrAndPort.split(":");
            sourceIP = parts[0];
            sourcePort = parts[1];
            if(sms.getSourceIP() == null){
                sms.setSourceIP(sourceIP);
            }
            if(sms.getSourcePort() == null){
                sms.setSourcePort(sourcePort);
            }
        }else{
            sourceIP = sms.getSourceIP();
            sourcePort = sms.getSourcePort();
        }

        String timestampA = null, timestampB = null, timestampC = null;
        if (tsA != null && tsA != 0)
            timestampA = DATE_FORMAT.format(tsA);
        if (tsB != null && tsB != 0)
            timestampB = DATE_FORMAT.format(tsB);
        if (tsC != null && tsC != 0)
            timestampC = DATE_FORMAT.format(tsC);

       /* SmsExposureLayerData objSmsExposureLayerData = null;
        boolean exposureLayerDataSet = false;
        if(smsEvent.getExposureLayerData() != null){
            objSmsExposureLayerData = new SmsExposureLayerData(smsEvent.getExposureLayerData());
            exposureLayerDataSet = true;
        }*/

        StringBuilder sb = new StringBuilder();
        appendObject(sb, timestamp);
        appendObject(sb, eventType);
        appendObject(sb, errorCode);
        appendObject(sb, messageType);
        appendNumber(sb, statusCode);
        appendNumber(sb, messageId);
        appendObject(sb, origMessageID);
        appendObject(sb, dlrStatus);
        appendNumber(sb, Long.valueOf(mprocRuleId));
        appendObject(sb, origEsmeName);
        appendObject(sb, timestampA);
        appendObject(sb, timestampB);
        appendObject(sb, timestampC);
        appendObject(sb, sourceIP);
        appendObject(sb, sourcePort);
        appendObject(sb, destIP);
        appendObject(sb, destPort);
        appendNumber(sb, Long.valueOf(seqNumber));
        appendObject(sb, EL_messageId);
        appendObject(sb, EL_correlationId);
        appendObject(sb, SourceAddr);
        appendObject(sb, DestAddr);
        appendObject(sb, OrigNetworkId);
        appendObjectLast(sb, NetworkId);


        CdrDetailedGenerator.generateDetailedCdr(sb.toString());



        /*generateDetailedCdr(sms.isMcDeliveryReceipt(), sms.getShortMessageText(), sms.getTlvSet(), sms.getTimestampA(),
                sms.getTimestampB(), sms.getTimestampC(), sms.getMessageId(), sms.getOrigEsmeName(), eventType, errorCode,
                messageType, statusCode, mprocRuleId, sourceAddrAndPort, destAddrAndPort, seqNumber, generateReceiptCdr,
                generateDetailedCdr);*/
    }

    public static void generateDetailedCdr(boolean mcDeliveryReceipt, String shortMessageText, TlvSet tlvSet, Long tsA,
            Long tsB, Long tsC, Long messageId, String origEsmeName, EventType eventType, ErrorCode errorCode,
            String messageType, long statusCode, int mprocRuleId, String sourceAddrAndPort, String destAddrAndPort,
            int seqNumber, boolean generateReceiptCdr, boolean generateDetailedCdr) {
        // Format is
        // CDR recording timestamp, Event type, ErrorCode (status), MessageType, Status code, CorrelationId, OrigCorrelationId
        // DlrStatus, mprocRuleId, ESME name, Timestamp A, Timestamp B, Timestamp C, Source IP, Source port, Dest IP, Dest port,
        // SequenceNumber

        if (!generateDetailedCdr) {
            return;
        }

        if (!generateReceiptCdr && mcDeliveryReceipt) {
            // we do not generate CDR's for receipt if generateReceiptCdr option is off
            return;
        }
        String timestamp = DATE_FORMAT.format(new Date());

        String dlrStatus = null;
        String origMessageID = null;

        DeliveryReceiptData deliveryReceiptData = null;
        if (shortMessageText != null && tlvSet != null) {
            deliveryReceiptData = MessageUtil.parseDeliveryReceipt(shortMessageText, tlvSet);
            if (deliveryReceiptData != null) {
                dlrStatus = deliveryReceiptData.getStatus();
                if (deliveryReceiptData.getTlvMessageState() != null) {
                    int tlvMessageState = deliveryReceiptData.getTlvMessageState();
                    if (tlvMessageState != 0 && dlrStatus != null)
                        if (!dlrStatus.substring(0, 4)
                                .equals(MessageState.fromInt(tlvMessageState).toString().substring(0, 5))) {
                            dlrStatus = "err";
                        }
                }
                origMessageID = deliveryReceiptData.getMessageId();
            }
        }

        String destIP = null, destPort = null;
        if (destAddrAndPort != null) {
            String[] parts = destAddrAndPort.split(":");
            destIP = parts[0];
            destPort = parts[1];
        }

        String sourceIP = null, sourcePort = null;
        if (sourceAddrAndPort != null) {
            String[] parts = sourceAddrAndPort.split(":");
            sourceIP = parts[0];
            sourcePort = parts[1];
        }

        String timestampA = null, timestampB = null, timestampC = null;
        if (tsA != null && tsA != 0)
            timestampA = DATE_FORMAT.format(tsA);
        if (tsB != null && tsB != 0)
            timestampB = DATE_FORMAT.format(tsB);
        if (tsC != null && tsC != 0)
            timestampC = DATE_FORMAT.format(tsC);

       /* SmsExposureLayerData objSmsExposureLayerData = null;
        boolean exposureLayerDataSet = false;
        if(smsEvent.getExposureLayerData() != null){
            objSmsExposureLayerData = new SmsExposureLayerData(smsEvent.getExposureLayerData());
            exposureLayerDataSet = true;
        }*/


        String EL_messageId = "";
        String EL_correlationId = "";
        String SourceAddr = "";
        String DestAddr = "";
        int OrigNetworkId = -1;
        int NetworkId = -1;

        StringBuilder sb = new StringBuilder();
        appendObject(sb, timestamp);
        appendObject(sb, eventType);
        appendObject(sb, errorCode);
        appendObject(sb, messageType);
        appendNumber(sb, statusCode);
        appendNumber(sb, messageId);
        appendObject(sb, origMessageID);
        appendObject(sb, dlrStatus);
        appendNumber(sb, Long.valueOf(mprocRuleId));
        appendObject(sb, origEsmeName);
        appendObject(sb, timestampA);
        appendObject(sb, timestampB);
        appendObject(sb, timestampC);
        appendObject(sb, sourceIP);
        appendObject(sb, sourcePort);
        appendObject(sb, destIP);
        appendObject(sb, destPort);
        appendNumber(sb, Long.valueOf(seqNumber));
        appendObject(sb, EL_messageId);
        appendObject(sb, EL_correlationId);
        appendObject(sb, SourceAddr);
        appendObject(sb, DestAddr);
        appendObject(sb, OrigNetworkId);
        appendObjectLast(sb, NetworkId);


        CdrDetailedGenerator.generateDetailedCdr(sb.toString());
    }

    // 14:23:08.421,IN_SMPP_REJECT_FORBIDDEN,REJECT_INCOMING,SubmitSm,,,,-1,test,2017-07-15
    // 14:23:08.415,,,127.0.0.1,null45542,,,2

    private static void appendObject(StringBuilder sb, Object obj) {
        sb.append(obj != null ? obj : CdrDetailedGenerator.CDR_EMPTY);
        sb.append(CdrGenerator.CDR_SEPARATOR);
    }

    private static void appendObjectLast(StringBuilder sb, Object obj) {
        sb.append(obj != null ? obj : CdrDetailedGenerator.CDR_EMPTY);
    }

    private static void appendNumber(StringBuilder sb, Long num) {
        sb.append(num != null ? num != -1 ? num : CdrDetailedGenerator.CDR_EMPTY : CdrDetailedGenerator.CDR_EMPTY);
        sb.append(CdrGenerator.CDR_SEPARATOR);
    }

}
