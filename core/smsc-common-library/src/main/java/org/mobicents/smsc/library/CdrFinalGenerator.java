package org.mobicents.smsc.library;

import org.apache.log4j.Logger;
import org.mobicents.protocols.ss7.map.api.smstpdu.DataCodingScheme;
import org.mobicents.protocols.ss7.map.smstpdu.DataCodingSchemeImpl;
import org.mobicents.smsc.mproc.DeliveryReceiptData;
import org.restcomm.smpp.parameter.TlvSet;

import java.text.SimpleDateFormat;
import java.util.Date;

public class CdrFinalGenerator {
    private static final Logger logger = Logger.getLogger(CdrFinalGenerator.class);


    //CDR
    public static final String CDR_SUBMIT_FAILED_MO = "submit_failed_mo";
    public static final String CDR_SUBMIT_FAILED_HR = "submit_failed_hr";
    public static final String CDR_SUBMIT_FAILED_ESME = "submit_failed_esme";
    public static final String CDR_SUBMIT_FAILED_SIP = "submit_failed_sip";
    public static final String CDR_SUBMIT_FAILED_HTTP = "submit_failed_http";
    public static final String CDR_SUBMIT_FAILED_CHARGING = "submit_failed_charging";

    public static final String CDR_SUCCESS_ESME = "success_esme";
    public static final String CDR_PARTIAL_ESME = "partial_esme";
    public static final String CDR_FAILED_ESME = "failed_esme";
    public static final String CDR_TEMP_FAILED_ESME = "temp_failed_esme";

    public static final String CDR_SUCCESS_SIP = "success_sip";
    public static final String CDR_PARTIAL_SIP = "partial_sip";
    public static final String CDR_FAILED_SIP = "failed_sip";
    public static final String CDR_TEMP_FAILED_SIP = "temp_failed_sip";


    //DetailedCDR
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

    public static void generateFinalCdr(Sms smsEvent, String status, String reason, boolean generateReceiptCdr,
                                        boolean generateCdr, boolean messageIsSplitted, boolean lastSegment,
                                        boolean calculateMsgPartsLenCdr, boolean delayParametersInCdr, Sms sms,
                                        EventType eventType, ErrorCode errorCode, String messageType, long statusCode,
                                        int mprocRuleId, String sourceAddrAndPort, String destAddrAndPort, int seqNumber,
                                        boolean generateDetailedCdr) {

        generateFinalCdr(smsEvent,status,reason,generateReceiptCdr,generateCdr,messageIsSplitted,lastSegment,
                calculateMsgPartsLenCdr,delayParametersInCdr, sms.isMcDeliveryReceipt(), sms.getShortMessageText(),
                sms.getTlvSet(), sms.getTimestampA(), sms.getTimestampB(), sms.getTimestampC(), sms.getMessageId(),
                sms.getOrigEsmeName(), eventType, errorCode, messageType, statusCode, mprocRuleId, sourceAddrAndPort,
                destAddrAndPort, seqNumber, generateDetailedCdr);
    }

    public static void generateFinalCdr(
            Sms smsEvent, String status, String reason, boolean generateReceiptCdr, boolean generateCdr,
                                        boolean messageIsSplitted, boolean lastSegment, boolean calculateMsgPartsLenCdr, boolean delayParametersInCdr,
                                        boolean mcDeliveryReceipt, String shortMessageText, TlvSet tlvSet, Long tsA,
            Long tsB, Long tsC, Long messageId, String origEsmeName, EventType eventType, ErrorCode errorCode,
            String messageType, long statusCode, int mprocRuleId, String sourceAddrAndPort, String destAddrAndPort,
            int seqNumber, boolean generateDetailedCdr) {
        // Format is
        // CDR recording timestamp, Event type, ErrorCode (status), MessageType, Status code, CorrelationId, OrigCorrelationId
        // DlrStatus, mprocRuleId, ESME name, Timestamp A, Timestamp B, Timestamp C, Source IP, Source port, Dest IP, Dest port,
        // SequenceNumber

        /*if (!generateDetailedCdr) {
            return;
        }*/

        /*if (!generateReceiptCdr && mcDeliveryReceipt) {
            // we do not generate CDR's for receipt if generateReceiptCdr option is off
            return;
        }*/

        //CDR
        int msgParts = 0, charNumbers = 0;
        if (calculateMsgPartsLenCdr) {
            if (messageIsSplitted) {
                msgParts = 1;
            } else {
                DataCodingScheme dataCodingScheme = new DataCodingSchemeImpl(smsEvent.getDataCoding());
                msgParts = MessageUtil.calculateMsgParts(smsEvent.getShortMessageText(), dataCodingScheme,
                        smsEvent.getNationalLanguageLockingShift(), smsEvent.getNationalLanguageSingleShift());
            }
            if (lastSegment) {
                charNumbers = smsEvent.getShortMessageText().length();
            } else {
                charNumbers = 0;
            }
        }

        Long receiptLocalMessageId = smsEvent.getReceiptLocalMessageId();

        DeliveryReceiptData deliveryReceiptData = MessageUtil.parseDeliveryReceipt(smsEvent.getShortMessageText(),
                smsEvent.getTlvSet());
        String st = null;
        int tlvMessageState = -1;
        int err = -1;

        if (deliveryReceiptData != null) {
            st = deliveryReceiptData.getStatus();
            tlvMessageState = deliveryReceiptData.getTlvMessageState() == null ? -1 : deliveryReceiptData.getTlvMessageState();
            err = deliveryReceiptData.getError();
        }


        //DetailedCDR
        String timestamp = DATE_FORMAT.format(new Date());

        String dlrStatus = null;
        String origMessageID = null;
        deliveryReceiptData = null;
        if (shortMessageText != null && tlvSet != null) {
            deliveryReceiptData = MessageUtil.parseDeliveryReceipt(shortMessageText, tlvSet);
            if (deliveryReceiptData != null) {
                dlrStatus = deliveryReceiptData.getStatus();
                if (deliveryReceiptData.getTlvMessageState() != null) {
                    tlvMessageState = deliveryReceiptData.getTlvMessageState();
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

       /* String timestampA = null, timestampB = null, timestampC = null;
        if (tsA != null && tsA != 0)
            timestampA = DATE_FORMAT.format(tsA);
        if (tsB != null && tsB != 0)
            timestampB = DATE_FORMAT.format(tsB);
        if (tsC != null && tsC != 0)
            timestampC = DATE_FORMAT.format(tsC);*/

        SmsExposureLayerData objSmsExposureLayerData = null;
        boolean exposureLayerDataSet = false;
        String expLayerData = smsEvent.getExposureLayerData();
        System.out.println("expLayerData: "+expLayerData);
        if(smsEvent.getExposureLayerData() != null){
            objSmsExposureLayerData = new SmsExposureLayerData(smsEvent.getExposureLayerData());
            exposureLayerDataSet = true;
        }

        long el_api_start;
        long el_api_stop ;
        long el_que_start ;
        long el_que_stop;

        if(exposureLayerDataSet == true){
            el_api_start = objSmsExposureLayerData.getElApiStart();
            el_api_stop = objSmsExposureLayerData.getElApiStop();
            el_que_start = objSmsExposureLayerData.getElQueStart();
            el_que_stop = objSmsExposureLayerData.getElQueStop();
        }else{
            el_api_start = 0;
            el_api_stop = 0;
            el_que_start = 0;
            el_que_stop = 0;
        }

        long gw_inc_start = smsEvent.getGw_inc_start();
        long gw_inc_stop = smsEvent.getGw_inc_stop();
        long gw_que_start = smsEvent.getGw_que_start();
        long gw_que_stop = smsEvent.getGw_que_stop();
        long gw_out_start = smsEvent.getGw_out_start();
        long gw_out_stop = smsEvent.getGw_out_stop();
        long oc_dia_start = 0;
        long oc_dia_stop = 0;


        long T1 = el_api_start - gw_out_stop - gw_out_stop + gw_out_start;
        long T2 = el_api_stop - el_api_start;
        long T3 = gw_out_stop - gw_out_start;
        long T4 = oc_dia_stop - oc_dia_start;
        long T5 = gw_inc_stop - gw_inc_start;
        long T6 = el_api_stop - el_api_start;
        long T7 = el_api_start - gw_out_stop - gw_out_stop + gw_out_start - el_que_stop + el_que_start - gw_que_stop + gw_que_start;
        long T8 = T1 - T7;
        long T9 = el_que_stop - el_que_start;
        long T10 = gw_out_stop - gw_que_start;


        StringBuffer sb = new StringBuffer();
        sb.append(DATE_FORMAT.format(smsEvent.getSubmitDate()))
                .append(CdrFinalGenerator.CDR_SEPARATOR)
                .append(smsEvent.getSourceAddr())
                .append(CdrFinalGenerator.CDR_SEPARATOR)
                .append(smsEvent.getSourceAddrTon())
                .append(CdrFinalGenerator.CDR_SEPARATOR)
                .append(smsEvent.getSourceAddrNpi())
                .append(CdrFinalGenerator.CDR_SEPARATOR)
                .append(smsEvent.getSmsSet().getDestAddr())
                .append(CdrFinalGenerator.CDR_SEPARATOR)
                .append(smsEvent.getSmsSet().getDestAddrTon())
                .append(CdrFinalGenerator.CDR_SEPARATOR)
                .append(smsEvent.getSmsSet().getDestAddrNpi())
                .append(CdrFinalGenerator.CDR_SEPARATOR)
                .append(status)
                .append(CdrFinalGenerator.CDR_SEPARATOR)
                .append(smsEvent.getOriginationType())
                .append(CdrFinalGenerator.CDR_SEPARATOR)
                .append(smsEvent.getReceiptLocalMessageId() == null ? "message" : "dlr")
                .append(CdrFinalGenerator.CDR_SEPARATOR)
                .append(smsEvent.getOrigSystemId())
                .append(CdrFinalGenerator.CDR_SEPARATOR)
                .append(smsEvent.getMessageId())
                .append(CdrFinalGenerator.CDR_SEPARATOR)
                .append(smsEvent.getDlvMessageId())
                .append(CdrFinalGenerator.CDR_SEPARATOR)
                .append((receiptLocalMessageId != null && receiptLocalMessageId == -1) ? "xxxx" : smsEvent.getReceiptLocalMessageId())
                .append(CdrFinalGenerator.CDR_SEPARATOR)
                //.append(smsEvent.getSmsSet().getLocationInfoWithLMSI() != null ? smsEvent.getSmsSet().getLocationInfoWithLMSI()
                 //       .getNetworkNodeNumber().getAddress() : null)
                //.append(CdrFinalGenerator.CDR_SEPARATOR)
                //.append(smsEvent.getSmsSet().getImsi())
                //.append(CdrFinalGenerator.CDR_SEPARATOR)
               // .append(smsEvent.getSmsSet().getCorrelationId())
               // .append(CdrFinalGenerator.CDR_SEPARATOR)
                //.append(smsEvent.getOriginatorSccpAddress())
                //.append(CdrFinalGenerator.CDR_SEPARATOR)
                //.append(smsEvent.getMtServiceCenterAddress())
                //.append(CdrFinalGenerator.CDR_SEPARATOR)
                .append(smsEvent.getOrigNetworkId())
                .append(CdrFinalGenerator.CDR_SEPARATOR)
                .append(smsEvent.getSmsSet().getNetworkId())
                .append(CdrFinalGenerator.CDR_SEPARATOR)
                .append(smsEvent.getMprocNotes())
                .append(CdrFinalGenerator.CDR_SEPARATOR)
                .append(msgParts)
                .append(CdrFinalGenerator.CDR_SEPARATOR)
                .append(charNumbers)
                .append(CdrFinalGenerator.CDR_SEPARATOR)
                .append(delayParametersInCdr ? getProcessingTime(smsEvent.getSubmitDate()) : CDR_EMPTY)
                //.append(CdrFinalGenerator.CDR_SEPARATOR)
                //.append(delayParametersInCdr ? getScheduleDeliveryDelayMilis(smsEvent.getSubmitDate(), smsEvent.getScheduleDeliveryTime()) : CDR_EMPTY)
                .append(CdrFinalGenerator.CDR_SEPARATOR)
                .append(delayParametersInCdr ? smsEvent.getDeliveryCount() : CDR_EMPTY)
                .append(CdrFinalGenerator.CDR_SEPARATOR)
                .append("\"")
                .append(getEscapedString(getFirst20CharOfSMS(smsEvent.getShortMessageText())))
                .append("\"")
                .append(CdrFinalGenerator.CDR_SEPARATOR)
                .append("\"")
                .append(getEscapedString(reason))//Reason_For_Failure
                .append("\"")
                .append(CdrFinalGenerator.CDR_SEPARATOR)
                .append(st != null ? st : CdrFinalGenerator.CDR_EMPTY)//DeliveryState
                .append(CdrFinalGenerator.CDR_SEPARATOR)
                .append(tlvMessageState != -1 ? tlvMessageState : CdrFinalGenerator.CDR_EMPTY)
                .append(CdrFinalGenerator.CDR_SEPARATOR)
                .append(err != -1 ? err : CdrFinalGenerator.CDR_EMPTY)// "DeliveryErrorCode"
                .append(CdrFinalGenerator.CDR_SEPARATOR)
                .append(smsEvent.getGw_inc_start() >0 ? 1 : CdrFinalGenerator.CDR_EMPTY)
                .append(CdrFinalGenerator.CDR_SEPARATOR)
                .append(smsEvent.getGw_inc_stop() >0 ? 1 : CdrFinalGenerator.CDR_EMPTY)
                .append(CdrFinalGenerator.CDR_SEPARATOR)
                .append(smsEvent.getGw_out_start() >0 ? 1 : CdrFinalGenerator.CDR_EMPTY)
                .append(CdrFinalGenerator.CDR_SEPARATOR)
                .append(smsEvent.getGw_out_stop() >0 ? 1 : CdrFinalGenerator.CDR_EMPTY)
                .append(CdrFinalGenerator.CDR_SEPARATOR)
                .append(smsEvent.getGw_que_start() >0 ? 1 : CdrFinalGenerator.CDR_EMPTY)
                .append(CdrFinalGenerator.CDR_SEPARATOR)
                .append(smsEvent.getGw_que_stop() >0 ? 1 : CdrFinalGenerator.CDR_EMPTY)
                .append(CdrFinalGenerator.CDR_SEPARATOR)//Begining of CdrDetailed
                .append(timestamp)
                .append(CdrFinalGenerator.CDR_SEPARATOR)
                //.append(eventType)
                //.append(CdrFinalGenerator.CDR_SEPARATOR)
                //.append(errorCode)
                //.append(CdrFinalGenerator.CDR_SEPARATOR)
                //.append(messageType)
                //.append(CdrFinalGenerator.CDR_SEPARATOR)
                //.append(statusCode)
                //.append(CdrFinalGenerator.CDR_SEPARATOR)
                //.append(messageId)
                //.append(CdrFinalGenerator.CDR_SEPARATOR)
                //.append(origMessageID)
                //.append(CdrFinalGenerator.CDR_SEPARATOR)
                //.append(dlrStatus)
                //.append(CdrFinalGenerator.CDR_SEPARATOR)
                //.append(Long.valueOf(mprocRuleId))
                //.append(CdrFinalGenerator.CDR_SEPARATOR)
                .append(origEsmeName)
                .append(CdrFinalGenerator.CDR_SEPARATOR)
                //.append(timestampA)
                //.append(CdrFinalGenerator.CDR_SEPARATOR)
                //.append(timestampB)
                //.append(CdrFinalGenerator.CDR_SEPARATOR)
                //.append(timestampC)
                //.append(CdrFinalGenerator.CDR_SEPARATOR)
                .append(sourceIP)
                .append(CdrFinalGenerator.CDR_SEPARATOR)
                .append(sourcePort)
                .append(CdrFinalGenerator.CDR_SEPARATOR)
                .append(destIP)
                .append(CdrFinalGenerator.CDR_SEPARATOR)
                .append(destPort)
                .append(CdrFinalGenerator.CDR_SEPARATOR)
                .append(Long.valueOf(seqNumber))
                .append(CdrFinalGenerator.CDR_SEPARATOR)//Begining of only final CDR (GW)
                .append(smsEvent.getReroutingCount())//ReroutingCount
                .append(CdrFinalGenerator.CDR_SEPARATOR)
                .append(CdrFinalGenerator.CDR_EMPTY)//ELUserId
                .append(CdrFinalGenerator.CDR_SEPARATOR)
                .append(CdrFinalGenerator.CDR_EMPTY)//ELProductId
                .append(CdrFinalGenerator.CDR_SEPARATOR)
                .append(exposureLayerDataSet == true ? objSmsExposureLayerData.getMessageId() : CdrFinalGenerator.CDR_EMPTY)//ELMessageId
                .append(CdrFinalGenerator.CDR_SEPARATOR)
                .append(exposureLayerDataSet == true ? objSmsExposureLayerData.getCorrelationId() : CdrFinalGenerator.CDR_EMPTY)//ELCorrelationId
                .append(CdrFinalGenerator.CDR_SEPARATOR)
                .append(0)//UDHFirstMessageId
                .append(CdrFinalGenerator.CDR_SEPARATOR)
                .append(0)//UDHPartsCount
                .append(CdrFinalGenerator.CDR_SEPARATOR)
                .append(0)//UDHCurrentPartNumber
                .append(CdrFinalGenerator.CDR_SEPARATOR)
                .append(0)//UDHMessagePartText
                .append(CdrFinalGenerator.CDR_SEPARATOR)
                .append(smsEvent.getGw_inc_start() >0 ? 1 : CdrFinalGenerator.CDR_EMPTY)//GWIncStart
                .append(CdrFinalGenerator.CDR_SEPARATOR)
                .append(smsEvent.getGw_inc_stop() >0 ? 1 : CdrFinalGenerator.CDR_EMPTY)//GWIncStop
                .append(CdrFinalGenerator.CDR_SEPARATOR)
                .append(smsEvent.getGw_que_start() >0 ? 1 : CdrFinalGenerator.CDR_EMPTY)//GWQueStart
                .append(CdrFinalGenerator.CDR_SEPARATOR)
                .append(smsEvent.getGw_que_stop() >0 ? 1 : CdrFinalGenerator.CDR_EMPTY)//GWQueStop
                .append(CdrFinalGenerator.CDR_SEPARATOR)
                .append(smsEvent.getGw_out_start() >0 ? 1 : CdrFinalGenerator.CDR_EMPTY)//GWOutStart
                .append(CdrFinalGenerator.CDR_SEPARATOR)
                .append(smsEvent.getGw_out_stop() >0 ? 1 : CdrFinalGenerator.CDR_EMPTY)//GWOutStop
                .append(CdrFinalGenerator.CDR_SEPARATOR)
                .append(0)//OCDiaStart
                .append(CdrFinalGenerator.CDR_SEPARATOR)
                .append(0)//OCDiaStop
                .append(CdrFinalGenerator.CDR_SEPARATOR)
                .append(exposureLayerDataSet == true ? objSmsExposureLayerData.getElApiStart() : CdrFinalGenerator.CDR_EMPTY)//ELApiStart
                .append(CdrFinalGenerator.CDR_SEPARATOR)
                .append(exposureLayerDataSet == true ? objSmsExposureLayerData.getElApiStop() : CdrFinalGenerator.CDR_EMPTY)//ELApiStop
                .append(CdrFinalGenerator.CDR_SEPARATOR)
                .append(exposureLayerDataSet == true ? objSmsExposureLayerData.getElQueStart() : CdrFinalGenerator.CDR_EMPTY)//ELQueStart
                .append(CdrFinalGenerator.CDR_SEPARATOR)
                .append(exposureLayerDataSet == true ? objSmsExposureLayerData.getElQueStop() : CdrFinalGenerator.CDR_EMPTY)//ELQueStop
                .append(CdrFinalGenerator.CDR_SEPARATOR)
                .append(T1)
                .append(CdrFinalGenerator.CDR_SEPARATOR)
                .append(T2)
                .append(CdrFinalGenerator.CDR_SEPARATOR)
                .append(T3)
                .append(CdrFinalGenerator.CDR_SEPARATOR)
                .append(T4)
                .append(CdrFinalGenerator.CDR_SEPARATOR)
                .append(T5)
                .append(CdrFinalGenerator.CDR_SEPARATOR)
                .append(T6)
                .append(CdrFinalGenerator.CDR_SEPARATOR)
                .append(T7)
                .append(CdrFinalGenerator.CDR_SEPARATOR)
                .append(T8)
                .append(CdrFinalGenerator.CDR_SEPARATOR)
                .append(T9)
                .append(CdrFinalGenerator.CDR_SEPARATOR)
                .append(T10)
                .append(CdrFinalGenerator.CDR_SEPARATOR);



        CdrFinalGenerator.generateDetailedCdr(sb.toString());
    }

    // 14:23:08.421,IN_SMPP_REJECT_FORBIDDEN,REJECT_INCOMING,SubmitSm,,,,-1,test,2017-07-15
    // 14:23:08.415,,,127.0.0.1,null45542,,,2

    private static void appendObject(StringBuilder sb, Object obj) {
        sb.append(obj != null ? obj : CdrFinalGenerator.CDR_EMPTY);
        sb.append(CdrFinalGenerator.CDR_SEPARATOR);
    }

    private static void appendNumber(StringBuilder sb, Long num) {
        sb.append(num != null ? num != -1 ? num : CdrFinalGenerator.CDR_EMPTY : CdrFinalGenerator.CDR_EMPTY);
        sb.append(CdrFinalGenerator.CDR_SEPARATOR);
    }

    //    private static String getFirst20CharOfSMS(byte[] rawSms) {
    private static String getFirst20CharOfSMS(String first20CharOfSms) {
//        String first20CharOfSms = new String(rawSms);
        if (first20CharOfSms == null)
            return "";
        if (first20CharOfSms.length() > 20) {
            first20CharOfSms = first20CharOfSms.substring(0, 20);
        }
        return first20CharOfSms;
    }

    private static String getEscapedString(final String aValue) {
        return aValue.replaceAll("\n", "n").replaceAll(",", " ").replace("\"", "'").replace('\u0000', '?').replace('\u0006', '?');
    }

    private static String getProcessingTime(final Date aSubmitDate) {
        if (aSubmitDate == null) {
            return CDR_EMPTY;
        }
        return String.valueOf(System.currentTimeMillis() - aSubmitDate.getTime());
    }

    private static String getScheduleDeliveryDelayMilis(final Date aSubmitDate, final Date aScheduleDeliveryDate) {
        if (aSubmitDate == null) {
            return CDR_EMPTY;
        }
        if (aScheduleDeliveryDate == null) {
            return CDR_EMPTY;
        }
        return String.valueOf(aScheduleDeliveryDate.getTime() - aSubmitDate.getTime());
    }

}
