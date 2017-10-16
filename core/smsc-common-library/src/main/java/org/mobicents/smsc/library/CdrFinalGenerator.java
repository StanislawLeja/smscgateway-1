package org.mobicents.smsc.library;

import org.apache.log4j.Logger;
import org.mobicents.smsc.mproc.DeliveryReceiptData;
import org.mobicents.smsc.utils.SplitMessageData;
import org.restcomm.smpp.parameter.TlvSet;

import java.text.SimpleDateFormat;
import java.util.Date;

public class CdrFinalGenerator {
    private static final Logger logger = Logger.getLogger(CdrFinalGenerator.class);


    //DetailedCDR
    public static final String CDR_EMPTY = "";
    public static final String CDR_SEPARATOR = ",";

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");


    public static void generateFinalCdr(String message) {
        logger.debug(message);
    }

    public static void generateFinalCdr(Sms smsEvent, String status, String reason, boolean generateReceiptCdr,
                                        boolean messageIsSplitted, boolean lastSegment,
                                        boolean calculateMsgPartsLenCdr, boolean delayParametersInCdr,
                                        String sourceAddrAndPort, String destAddrAndPort,
                                        boolean generateFinalCdr) {
        generateFinalCdr(smsEvent,status,reason,generateReceiptCdr,messageIsSplitted,lastSegment,
                calculateMsgPartsLenCdr,delayParametersInCdr, smsEvent.isMcDeliveryReceipt(), smsEvent.getTlvSet(),
                smsEvent.getOrigEsmeName(), sourceAddrAndPort, destAddrAndPort, generateFinalCdr);
    }

    public static void generateFinalCdr(Sms smsEvent, String status, String reason, boolean generateReceiptCdr,
                                        boolean messageIsSplitted, boolean lastSegment, boolean calculateMsgPartsLenCdr, boolean delayParametersInCdr,
                                        boolean mcDeliveryReceipt, TlvSet tlvSet, String origEsmeName, String sourceAddrAndPort, String destAddrAndPort, boolean generateFinalCdr) {

        if (!generateFinalCdr) {
            return;
        }

        if (!generateReceiptCdr && mcDeliveryReceipt) {
            // we do not generate CDR's for receipt if generateReceiptCdr option is off
            return;
        }

        int charNumbers = 0;
        if (calculateMsgPartsLenCdr) {
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

        String destIP = null, destPort = null;
        if (destAddrAndPort != null) {
            String[] parts = destAddrAndPort.split(":");
            destIP = parts[0];
            destPort = parts[1];
        }else{
            destIP = smsEvent.getDestIP();
            destPort = smsEvent.getDestPort();
        }

        String sourceIP = null, sourcePort = null;
        if (sourceAddrAndPort != null) {
            String[] parts = sourceAddrAndPort.split(":");
            sourceIP = parts[0];
            sourcePort = parts[1];
        }else{
            sourceIP = smsEvent.getSourceIP();
            sourcePort = smsEvent.getSourcePort();
        }

        SmsExposureLayerData objSmsExposureLayerData = null;
        boolean exposureLayerDataSet = false;
        if(smsEvent.getExposureLayerData() != null){
            objSmsExposureLayerData = new SmsExposureLayerData(smsEvent.getExposureLayerData());
            exposureLayerDataSet = true;
        }

        long el_api_start;
        long el_que_start ;
        long el_que_stop;

        if(exposureLayerDataSet == true){
            el_api_start = objSmsExposureLayerData.getElApiStart();
            el_que_start = objSmsExposureLayerData.getElQueStart();
            el_que_stop = objSmsExposureLayerData.getElQueStop();
        }else{
            el_api_start = 0;
            el_que_start = 0;
            el_que_stop = 0;
        }

        long gw_inc_start = smsEvent.getGwIncStart();
        long gw_inc_stop = smsEvent.getGwIncStop();
        long gw_que_start = smsEvent.getGwQueStart();
        long gw_que_stop = smsEvent.getGwQueStop();
        long gw_out_start = smsEvent.getGwOutStart();
        long gw_out_stop = smsEvent.getGwOutStop();
        long oc_dia_start = smsEvent.getOcDiaStart();
        long oc_dia_stop = smsEvent.getOcDiaStop();

        long T1,T3,T4,T5,T8,T9,T10,T11,T12;

        if(gw_que_start == 0 || gw_que_stop == 0){
            gw_que_start = 0;
            gw_que_stop = 0;
        }
        if(el_que_start == 0 || el_que_stop == 0){
            el_que_start = 0;
            el_que_stop = 0;
        }
        if(gw_out_stop == 0){
            T1 = 0;
            T3 = 0;
            T8 = 0;
            T12 = 0;
        }else{
            T3 = gw_out_stop - gw_out_start;
            if(gw_inc_start == 0){
                if(exposureLayerDataSet == true){
                    T1 = gw_out_stop - el_api_start;
                    T8 = gw_out_stop - el_api_start - (gw_que_stop - gw_que_start) - (el_que_stop - el_que_start);
                }else {
                    T1 = 0;
                    T8 = 0;
                }
                T12 = 0;
            }else{
                if(exposureLayerDataSet == true){
                    T1 = gw_out_stop - el_api_start;
                    T8 = gw_out_stop - el_api_start - (gw_que_stop - gw_que_start) - (el_que_stop - el_que_start);
                }else {
                    T1 = gw_out_stop - gw_inc_start;
                    T8 = gw_out_stop - gw_inc_start - (gw_que_stop - gw_que_start);
                }
                T12 = gw_out_stop - gw_inc_start - (gw_que_stop - gw_que_start);
            }
        }

        if(oc_dia_stop == 0 || oc_dia_start == 0){
            T4 = 0;
        }else{
            T4 = oc_dia_stop - oc_dia_start;
        }
        if(gw_inc_stop == 0 || gw_inc_start == 0){
            T5 = 0;
        }else {
            T5 = gw_inc_stop - gw_inc_start;
        }
        T9 = el_que_stop - el_que_start + gw_que_stop - gw_que_start;
        T10 = el_que_stop - el_que_start;
        T11  = gw_que_stop - gw_que_start;



        SplitMessageData splitMessageData = MessageUtil.parseSplitMessageData(smsEvent);

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
                .append(smsEvent.getOrigNetworkId())
                .append(CdrFinalGenerator.CDR_SEPARATOR)
                .append(smsEvent.getSmsSet().getNetworkId())
                .append(CdrFinalGenerator.CDR_SEPARATOR)
                .append(smsEvent.getMprocNotes())
                .append(CdrFinalGenerator.CDR_SEPARATOR)
                .append(charNumbers)
                .append(CdrFinalGenerator.CDR_SEPARATOR)
                .append(delayParametersInCdr ? getProcessingTime(smsEvent.getSubmitDate()) : CDR_EMPTY)
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
                .append(CdrFinalGenerator.CDR_SEPARATOR)//Begining of CdrDetailed
                .append(origEsmeName)
                .append(CdrFinalGenerator.CDR_SEPARATOR)
                .append(sourceIP)
                .append(CdrFinalGenerator.CDR_SEPARATOR)
                .append(sourcePort)
                .append(CdrFinalGenerator.CDR_SEPARATOR)
                .append(destIP)
                .append(CdrFinalGenerator.CDR_SEPARATOR)
                .append(destPort)
                .append(CdrFinalGenerator.CDR_SEPARATOR)//Begining of only final CDR (GW)
                .append(smsEvent.getReroutingCount())
                .append(CdrFinalGenerator.CDR_SEPARATOR)
                .append(exposureLayerDataSet == true ? objSmsExposureLayerData.getUserId() : CdrFinalGenerator.CDR_EMPTY)//ELUserId
                .append(CdrFinalGenerator.CDR_SEPARATOR)
                .append(CdrFinalGenerator.CDR_EMPTY)//ELProductId
                //// TODO: 21.09.17  Insert ELProductId from ExposureLayereData
                .append(CdrFinalGenerator.CDR_SEPARATOR)
                .append(exposureLayerDataSet == true ? objSmsExposureLayerData.getMessageId() : CdrFinalGenerator.CDR_EMPTY)
                .append(CdrFinalGenerator.CDR_SEPARATOR)
                .append(exposureLayerDataSet == true ? objSmsExposureLayerData.getCorrelationId() : CdrFinalGenerator.CDR_EMPTY)
                .append(CdrFinalGenerator.CDR_SEPARATOR)
                .append(splitMessageData.isMsgSplitInUse() ? splitMessageData.getSplitedMessageID(): CdrGenerator.CDR_EMPTY)
                .append(CdrFinalGenerator.CDR_SEPARATOR)
                .append(splitMessageData.isMsgSplitInUse() ? splitMessageData.getSplitedMessageParts() : 1)
                .append(CdrFinalGenerator.CDR_SEPARATOR)
                .append(splitMessageData.isMsgSplitInUse() ? splitMessageData.getSplitedMessagePartNumber(): CdrGenerator.CDR_EMPTY)
                .append(CdrFinalGenerator.CDR_SEPARATOR)
                .append(smsEvent.getGwIncStart() >0 ? DATE_FORMAT.format(smsEvent.getGwIncStart()) : CdrFinalGenerator.CDR_EMPTY)
                .append(CdrFinalGenerator.CDR_SEPARATOR)
                .append(smsEvent.getGwIncStop() >0 ? DATE_FORMAT.format(smsEvent.getGwIncStop()) : CdrFinalGenerator.CDR_EMPTY)
                .append(CdrFinalGenerator.CDR_SEPARATOR)
                .append(smsEvent.getGwQueStart() >0 ? DATE_FORMAT.format(smsEvent.getGwQueStart()) : CdrFinalGenerator.CDR_EMPTY)
                .append(CdrFinalGenerator.CDR_SEPARATOR)
                .append(smsEvent.getGwQueStop() >0 ? DATE_FORMAT.format(smsEvent.getGwQueStop()) : CdrFinalGenerator.CDR_EMPTY)
                .append(CdrFinalGenerator.CDR_SEPARATOR)
                .append(smsEvent.getGwOutStart() >0 ? DATE_FORMAT.format(smsEvent.getGwOutStart()) : CdrFinalGenerator.CDR_EMPTY)
                .append(CdrFinalGenerator.CDR_SEPARATOR)
                .append(smsEvent.getGwOutStop() >0 ? DATE_FORMAT.format(smsEvent.getGwOutStop()) : CdrFinalGenerator.CDR_EMPTY)
                .append(CdrFinalGenerator.CDR_SEPARATOR)
                .append(oc_dia_start >0 ? DATE_FORMAT.format(oc_dia_start) : CdrFinalGenerator.CDR_EMPTY)
                .append(CdrFinalGenerator.CDR_SEPARATOR)
                .append(oc_dia_stop >0 ? DATE_FORMAT.format(oc_dia_stop) : CdrFinalGenerator.CDR_EMPTY)
                .append(CdrFinalGenerator.CDR_SEPARATOR)
                .append(exposureLayerDataSet == true ? DATE_FORMAT.format(objSmsExposureLayerData.getElApiStart()) : CdrFinalGenerator.CDR_EMPTY)
                .append(CdrFinalGenerator.CDR_SEPARATOR)
                .append(exposureLayerDataSet == true ? DATE_FORMAT.format(objSmsExposureLayerData.getElQueStart()) : CdrFinalGenerator.CDR_EMPTY)
                .append(CdrFinalGenerator.CDR_SEPARATOR)
                .append(exposureLayerDataSet == true ? DATE_FORMAT.format(objSmsExposureLayerData.getElQueStop()) : CdrFinalGenerator.CDR_EMPTY)
                .append(CdrFinalGenerator.CDR_SEPARATOR)
                .append(T1 != 0 ? T1 : CdrFinalGenerator.CDR_EMPTY)
                .append(CdrFinalGenerator.CDR_SEPARATOR)
                .append(T3 != 0 ? T3 : CdrFinalGenerator.CDR_EMPTY)
                .append(CdrFinalGenerator.CDR_SEPARATOR)
                .append(T4 != 0 ? T4 : CdrFinalGenerator.CDR_EMPTY)
                .append(CdrFinalGenerator.CDR_SEPARATOR)
                .append(T5 != 0 ? T5 : CdrFinalGenerator.CDR_EMPTY)
                .append(CdrFinalGenerator.CDR_SEPARATOR)
                .append(T8 != 0 ? T8 : CdrFinalGenerator.CDR_EMPTY)
                .append(CdrFinalGenerator.CDR_SEPARATOR)
                .append(T9 != 0 ? T9 : CdrFinalGenerator.CDR_EMPTY)
                .append(CdrFinalGenerator.CDR_SEPARATOR)
                .append(T10 != 0 ? T10 : CdrFinalGenerator.CDR_EMPTY)
                .append(CdrFinalGenerator.CDR_SEPARATOR)
                .append(T11 != 0 ? T11 : CdrFinalGenerator.CDR_EMPTY)
                .append(CdrFinalGenerator.CDR_SEPARATOR)
                .append(T12 != 0 ? T12 : CdrFinalGenerator.CDR_EMPTY)
                .append(CdrFinalGenerator.CDR_SEPARATOR);


        CdrFinalGenerator.generateFinalCdr(sb.toString());
    }



    public static void generateFinalCdr(String sourceAddr, int sourceAddressTon, int sourceAddrNpi, String destAddr, int destAddrTon,
                                        int destAddrNpi, OriginationType originationType, String origSystemId, String imsi,
                                        String originatorSccpAddress, int origNetworkId, int networkId, Date scheduleDeliveryTime,
                                        int deliveryCount, String message, String status, String reason, boolean generateReceiptCdr,
                                        boolean lastSegment, boolean calculateMsgPartsLenCdr, boolean delayParametersInCdr,
                                        String sourceAddrAndPort, String destAddrAndPort,String origEsmeName,Long receiptLocalMessageId,
                                        DeliveryReceiptData deliveryReceiptData, boolean generateFinalCdr) {

        if (!generateFinalCdr || !generateReceiptCdr)
            return;

        int msgParts = 0;
        int charNumbers = 0;

        String st = null;
        int tlvMessageState = -1;
        int err = -1;

        if (deliveryReceiptData != null) {
            st = deliveryReceiptData.getStatus();
            tlvMessageState = deliveryReceiptData.getTlvMessageState() == null ? -1 : deliveryReceiptData.getTlvMessageState();
            err = deliveryReceiptData.getError();
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

        long gw_inc_start = 0;
        long gw_inc_stop = 0;
        long gw_que_start = 0;
        long gw_que_stop = 0;
        long gw_out_start = 0;
        long gw_out_stop = 0;
        long oc_dia_start = 0;
        long oc_dia_stop = 0;

        long el_api_start = 0;
        long el_que_start = 0;
        long el_que_stop = 0;


        long T1,T3,T4,T5,T8,T9,T10,T11,T12;

        if(gw_que_start == 0 || gw_que_stop == 0){
            gw_que_start = 0;
            gw_que_stop = 0;
        }
        if(el_que_start == 0 || el_que_stop == 0){
            el_que_start = 0;
            el_que_stop = 0;
        }
        if(gw_out_stop == 0){
            T1 = 0;
            T3 = 0;
            T8 = 0;
            T12 = 0;
        }else{
            T3 = gw_out_stop - gw_out_start;
            if(gw_inc_start == 0){
                T1 = 0;
                T8 = 0;
                T12 = 0;
            }else{
                T1 = gw_out_stop - gw_inc_start;
                T8 = gw_out_stop - gw_inc_start - (gw_que_stop - gw_que_start);
                T12 = gw_out_stop - gw_inc_start - (gw_que_stop - gw_que_start);
            }
        }

        if(oc_dia_stop == 0 || oc_dia_start == 0){
            T4 = 0;
        }else{
            T4 = oc_dia_stop - oc_dia_start;
        }
        if(gw_inc_stop == 0 || gw_inc_start == 0){
            T5 = 0;
        }else {
            T5 = gw_inc_stop - gw_inc_start;
        }
        T9 = el_que_stop - el_que_start + gw_que_stop - gw_que_start;
        T10 = el_que_stop - el_que_start;
        T11  = gw_que_stop - gw_que_start;

        //SplitMessageData splitMessageData = MessageUtil.parseSplitMessageData(smsEvent); todo handle it

        Date submitDate = new Date();

        StringBuffer sb = new StringBuffer();
        sb.append(DATE_FORMAT.format(submitDate))
                .append(CdrGenerator.CDR_SEPARATOR)
                .append(sourceAddr)
                .append(CdrGenerator.CDR_SEPARATOR)
                .append(sourceAddressTon)
                .append(CdrGenerator.CDR_SEPARATOR)
                .append(sourceAddrNpi)
                .append(CdrGenerator.CDR_SEPARATOR)
                .append(destAddr)
                .append(CdrGenerator.CDR_SEPARATOR)
                .append(destAddrTon)
                .append(CdrGenerator.CDR_SEPARATOR)
                .append(destAddrNpi)
                .append(CdrGenerator.CDR_SEPARATOR)
                .append(status)
                .append(CdrGenerator.CDR_SEPARATOR)
                .append(originationType)
                .append(CdrGenerator.CDR_SEPARATOR)
                .append("message")
                .append(CdrGenerator.CDR_SEPARATOR)
                .append(origSystemId)
                .append(CdrGenerator.CDR_SEPARATOR)
                .append(CDR_EMPTY)//??
                .append(CdrGenerator.CDR_SEPARATOR)
                .append(CDR_EMPTY)
                .append(CdrGenerator.CDR_SEPARATOR)
                .append(CDR_EMPTY)
                .append(CdrGenerator.CDR_SEPARATOR)
                .append(origNetworkId)
                .append(CdrGenerator.CDR_SEPARATOR)
                .append(networkId)
                .append(CdrGenerator.CDR_SEPARATOR)
                .append(CDR_EMPTY)
                .append(CdrGenerator.CDR_SEPARATOR)
                .append(msgParts != 0 ? msgParts : msgParts)
                .append(CdrGenerator.CDR_SEPARATOR)
                .append(charNumbers != 0 ? msgParts : charNumbers)
                .append(CdrGenerator.CDR_SEPARATOR)
                .append(delayParametersInCdr ? getProcessingTime(submitDate) : CDR_EMPTY)
                .append(CdrGenerator.CDR_SEPARATOR)
                .append(delayParametersInCdr ? deliveryCount : CDR_EMPTY)
                .append(CdrGenerator.CDR_SEPARATOR)
                .append("\"")
                .append(getEscapedString(getFirst20CharOfSMS(message)))
                .append("\"")
                .append(CdrGenerator.CDR_SEPARATOR)
                .append("\"")
                .append(getEscapedString(reason))//Reason_For_Failure
                .append("\"")
                .append(CdrFinalGenerator.CDR_SEPARATOR)
                .append(st != null ? st : CdrFinalGenerator.CDR_EMPTY)//DeliveryState
                .append(CdrFinalGenerator.CDR_SEPARATOR)
                .append(tlvMessageState != -1 ? tlvMessageState : CdrFinalGenerator.CDR_EMPTY)
                .append(CdrFinalGenerator.CDR_SEPARATOR)
                .append(err != -1 ? err : CdrFinalGenerator.CDR_EMPTY)// "DeliveryErrorCode"
                .append(CdrFinalGenerator.CDR_SEPARATOR)//Begining of CdrDetailed
                .append(origEsmeName)
                .append(CdrFinalGenerator.CDR_SEPARATOR)
                .append(sourceAddrAndPort != null ? sourceIP :CdrFinalGenerator.CDR_EMPTY)
                .append(CdrFinalGenerator.CDR_SEPARATOR)
                .append(sourceAddrAndPort != null ? sourcePort :CdrFinalGenerator.CDR_EMPTY)
                .append(CdrFinalGenerator.CDR_SEPARATOR)
                .append(sourceAddrAndPort != null ? destIP :CdrFinalGenerator.CDR_EMPTY)
                .append(CdrFinalGenerator.CDR_SEPARATOR)
                .append(sourceAddrAndPort != null ? destPort :CdrFinalGenerator.CDR_EMPTY)
                .append(CdrFinalGenerator.CDR_SEPARATOR)//Begining of only final CDR (GW)
                .append(CdrFinalGenerator.CDR_EMPTY)//smsEvent.getReroutingCount()
                .append(CdrFinalGenerator.CDR_SEPARATOR)
                .append(CdrFinalGenerator.CDR_EMPTY)//objSmsExposureLayerData.getUserId()
                .append(CdrFinalGenerator.CDR_SEPARATOR)
                .append(CdrFinalGenerator.CDR_EMPTY)//ELProductId
                //// TODO: 21.09.17  Insert ELProductId from ExposureLayereData
                .append(CdrFinalGenerator.CDR_SEPARATOR)
                .append(CdrFinalGenerator.CDR_EMPTY)//objSmsExposureLayerData.getMessageId()
                .append(CdrFinalGenerator.CDR_SEPARATOR)
                .append(CdrFinalGenerator.CDR_EMPTY)//objSmsExposureLayerData.getCorrelationId()
                .append(CdrFinalGenerator.CDR_SEPARATOR)
                .append(CdrGenerator.CDR_EMPTY)
                .append(CdrFinalGenerator.CDR_SEPARATOR)
                .append(CdrGenerator.CDR_EMPTY)
                .append(CdrFinalGenerator.CDR_SEPARATOR)
                .append(CdrGenerator.CDR_EMPTY)
                .append(CdrFinalGenerator.CDR_SEPARATOR)
                .append(gw_inc_start >0 ? DATE_FORMAT.format(gw_inc_start) : CdrFinalGenerator.CDR_EMPTY)
                .append(CdrFinalGenerator.CDR_SEPARATOR)
                .append(gw_inc_stop >0 ? DATE_FORMAT.format(gw_inc_stop) : CdrFinalGenerator.CDR_EMPTY)
                .append(CdrFinalGenerator.CDR_SEPARATOR)
                .append(gw_que_start >0 ? DATE_FORMAT.format(gw_que_start) : CdrFinalGenerator.CDR_EMPTY)
                .append(CdrFinalGenerator.CDR_SEPARATOR)
                .append(gw_que_stop >0 ? DATE_FORMAT.format(gw_que_stop) : CdrFinalGenerator.CDR_EMPTY)
                .append(CdrFinalGenerator.CDR_SEPARATOR)
                .append(gw_out_start >0 ? DATE_FORMAT.format(gw_out_start) : CdrFinalGenerator.CDR_EMPTY)
                .append(CdrFinalGenerator.CDR_SEPARATOR)
                .append(gw_out_stop >0 ? DATE_FORMAT.format(gw_out_stop) : CdrFinalGenerator.CDR_EMPTY)
                .append(CdrFinalGenerator.CDR_SEPARATOR)
                .append(oc_dia_start >0 ? DATE_FORMAT.format(oc_dia_start) : CdrFinalGenerator.CDR_EMPTY)
                .append(CdrFinalGenerator.CDR_SEPARATOR)
                .append(oc_dia_stop >0 ? DATE_FORMAT.format(oc_dia_stop) : CdrFinalGenerator.CDR_EMPTY)
                .append(CdrFinalGenerator.CDR_SEPARATOR)
                .append(el_api_start >0 ? DATE_FORMAT.format(el_api_start) : CdrFinalGenerator.CDR_EMPTY)//objSmsExposureLayerData.getElApiStart()
                .append(CdrFinalGenerator.CDR_SEPARATOR)
                .append(el_que_start >0? DATE_FORMAT.format(el_que_start) : CdrFinalGenerator.CDR_EMPTY)//objSmsExposureLayerData.getElQueStart()
                .append(CdrFinalGenerator.CDR_SEPARATOR)
                .append(el_que_stop >0 ? DATE_FORMAT.format(el_que_stop) : CdrFinalGenerator.CDR_EMPTY)//objSmsExposureLayerData.getElQueStop()
                .append(CdrFinalGenerator.CDR_SEPARATOR)
                .append(T1 != 0 ? T1 : CdrFinalGenerator.CDR_EMPTY)
                .append(CdrFinalGenerator.CDR_SEPARATOR)
                .append(T3 != 0 ? T3 : CdrFinalGenerator.CDR_EMPTY)
                .append(CdrFinalGenerator.CDR_SEPARATOR)
                .append(T4 != 0 ? T4 : CdrFinalGenerator.CDR_EMPTY)
                .append(CdrFinalGenerator.CDR_SEPARATOR)
                .append(T5 != 0 ? T5 : CdrFinalGenerator.CDR_EMPTY)
                .append(CdrFinalGenerator.CDR_SEPARATOR)
                .append(T8 != 0 ? T8 : CdrFinalGenerator.CDR_EMPTY)
                .append(CdrFinalGenerator.CDR_SEPARATOR)
                .append(T9 != 0 ? T9 : CdrFinalGenerator.CDR_EMPTY)
                .append(CdrFinalGenerator.CDR_SEPARATOR)
                .append(T10 != 0 ? T10 : CdrFinalGenerator.CDR_EMPTY)
                .append(CdrFinalGenerator.CDR_SEPARATOR)
                .append(T11 != 0 ? T11 : CdrFinalGenerator.CDR_EMPTY)
                .append(CdrFinalGenerator.CDR_SEPARATOR)
                .append(T12 != 0 ? T12 : CdrFinalGenerator.CDR_EMPTY)
                .append(CdrFinalGenerator.CDR_SEPARATOR);

        CdrFinalGenerator.generateFinalCdr(sb.toString());
    }

    private static String getFirst20CharOfSMS(String first20CharOfSms) {
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

}
