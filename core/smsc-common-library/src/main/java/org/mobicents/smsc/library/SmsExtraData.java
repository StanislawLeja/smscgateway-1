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

package org.mobicents.smsc.library;

import javolution.xml.XMLFormat;
import javolution.xml.stream.XMLStreamException;

/**
 *
 * @author sergey vetyutnev
 *
 */
public class SmsExtraData {
    public static final String MPROC_NOTES = "mprocNotes";
    public static final String ORIGINATION_TYPE = "originationType";
    public static final String RECEIPT_LOCAL_MESSAGEID = "receiptLocalMessageId";
    
    //timestamps used for DetailedCDR
    public static final String TIMESTAMP_A = "timestampA";
    public static final String TIMESTAMP_B = "timestampB";
    public static final String TIMESTAMP_C = "timestampC";

    public static final String GW_INC_START = "gwIncStart";
    public static final String GW_INC_STOP = "gwIncStop";
    public static final String GW_QUE_START = "gwQueStart";
    public static final String GW_QUE_STOP = "gwQueStop";
    public static final String GW_OUT_START = "gwOutStart";
    public static final String GW_OUT_STOP = "gwOutStop";
    public static final String OC_DIA_START = "ocDiaStart";
    public static final String OC_DIA_STOP = "ocDiaStop";
    public static final String DEST_IP = "destIP";
    public static final String DEST_PORT = "destPort";
    public static final String SOURCE_IP = "sourceIP";
    public static final String SOURCE_PORT = "sourcePort";


    // global title and translation type for MT message
    public static final String MT_LOCAL_SCCP_GT = "mtLocalSccpGt";
    public static final String MT_REMOTE_SCCP_TT = "mtRemoteSccpTt";

    public static final String ZERO_STRING = null;

    private String mprocNotes;
    private OriginationType originationType;
    private Long receiptLocalMessageId;
    
    private long timestampA;
    private long timestampB;
    private long timestampC;

    private long gwIncStart;
    private long gwIncStop;

    private long gwQueStart;
    private long gwQueStop;
    private long gwOutStart;
    private long gwOutStop;
    private long ocDiaStart;
    private long ocDiaStop;


    private String destIP;
    private String destPort;
    private String sourceIP;
    private String sourcePort;

    private String mtLocalSccpGt;
    private Integer mtRemoteSccpTt;


    public boolean isEmpty() {
        if (this.mprocNotes != null || this.originationType != null || this.receiptLocalMessageId != null 
        		|| this.timestampA != 0 || this.timestampB != 0 || this.timestampC != 0 || this.gwIncStart != 0
                || this.gwIncStop != 0 || this.gwQueStart != 0 || this.gwQueStop != 0 || this.gwOutStart != 0
                || this.gwOutStop != 0 || this.ocDiaStart != 0 || this.ocDiaStop != 0 || this.destIP != null
                || this.destPort != null || this.sourceIP != null || this.sourcePort != null
                || this.mtLocalSccpGt != null || this.mtRemoteSccpTt != null)

            return false;
        else
            return true;
    }

    public void clear() {
        mprocNotes = null;
        originationType = null;
        receiptLocalMessageId = null;
        timestampA = 0;
        timestampB = 0;
        timestampC = 0;
        gwIncStart = 0;
        gwIncStop = 0;
        gwQueStart = 0;
        gwQueStop = 0;
        gwOutStart = 0;
        gwOutStop = 0;
        ocDiaStart = 0;
        ocDiaStop = 0;
        destIP = null;
        destPort = null;
        sourceIP = null;
        sourcePort = null;
        mtLocalSccpGt = null;
        mtRemoteSccpTt = null;
    }

    public String getMprocNotes() {
        return mprocNotes;
    }

    public void setMprocNotes(String mprocNotes) {
        this.mprocNotes = mprocNotes;
    }

    public OriginationType getOriginationType() {
        return originationType;
    }

    public void setOriginationType(OriginationType originationType) {
        this.originationType = originationType;
    }

    public Long getReceiptLocalMessageId() {
        return receiptLocalMessageId;
    }

    public void setReceiptLocalMessageId(Long receiptLocalMessageId) {
        this.receiptLocalMessageId = receiptLocalMessageId;
    }
    
    public long getTimestampA() {
		return timestampA;
	}

	public void setTimestampA(long timestampA) {
        this.timestampA = timestampA;
	}

	public long getTimestampB() {
		return timestampB;

	}

	public void setTimestampB(long timestampB) {
		this.timestampB = timestampB;
	}

	public long getTimestampC() {
		return timestampC;
	}

    public void setTimestampC(long timestampC) {
        this.timestampC = timestampC;
    }
    
    public void setMtLocalSccpGt(String mtLocalSccpGt) {
        this.mtLocalSccpGt = mtLocalSccpGt;
    }
    
    public String getMtLocalSccpGt() {
        return mtLocalSccpGt;
    }
    
    public void setMtRemoteSccpTt(Integer mtRemoteSccpTt) {
        this.mtRemoteSccpTt = mtRemoteSccpTt;
    }
    
    public Integer getMtRemoteSccpTt() {
        return mtRemoteSccpTt;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("SmsExtraData [");
        if (mprocNotes != null) {
            sb.append("mprocNotes=");
            sb.append(mprocNotes);
            sb.append(", ");
        }
        if (originationType != null) {
            sb.append("originationType=");
            sb.append(originationType);
            sb.append(", ");
        }
        if (receiptLocalMessageId != null) {
            sb.append("receiptLocalMessageId=");
            sb.append(receiptLocalMessageId);
            sb.append(", ");
        }
        if (timestampA != 0) {
        	sb.append("timestampA=");
        	sb.append(timestampA);
        	sb.append(", ");
        }
        if (timestampB != 0) {
        	sb.append("timestampB=");
        	sb.append(timestampB);
        	sb.append(", ");
        }
        if (timestampC != 0) {
        	sb.append("timestampC=");
        	sb.append(timestampC);
        	sb.append(", ");
        }

        if (gwIncStart != 0) {
            sb.append("gwIncStart=");
            sb.append(gwIncStart);
            sb.append(", ");
        }
        if (gwIncStop != 0) {
            sb.append("gwIncStop=");
            sb.append(gwIncStop);
            sb.append(", ");
        }
        if (gwQueStart != 0) {
            sb.append("gwQueStart=");
            sb.append(gwQueStart);
            sb.append(", ");
        }
        if (gwQueStop != 0) {
            sb.append("gwQueStop=");
            sb.append(gwQueStop);
            sb.append(", ");
        }
        if (gwOutStart != 0) {
            sb.append("gwOutStart=");
            sb.append(gwOutStart);
            sb.append(", ");
        }
        if (gwOutStop != 0) {
            sb.append("gwOutStop=");
            sb.append(gwOutStop);
            sb.append(", ");
        }if (ocDiaStart != 0) {
            sb.append("ocDiaStart=");
            sb.append(ocDiaStart);
            sb.append(", ");
        }
        if (ocDiaStop != 0) {
            sb.append("ocDiaStop=");
            sb.append(ocDiaStop);
            sb.append(", ");
        }
        if (destIP != null) {
            sb.append("destIP=");
            sb.append(destIP);
            sb.append(", ");
        }
        if (destPort != null) {
            sb.append("destPort=");
            sb.append(destPort);
            sb.append(", ");
        }
        if (sourceIP != null) {
            sb.append("sourceIP=");
            sb.append(sourceIP);
            sb.append(", ");
        }
        if (sourcePort != null) {
            sb.append("sourcePort=");
            sb.append(sourcePort);
        }

        if (mtLocalSccpGt != null) {
            sb.append("mtLocalSccpGt=");
            sb.append(mtLocalSccpGt);
            sb.append(", ");
        }

        if (mtRemoteSccpTt != null) {
            sb.append("mtRemoteSccpTt");
            sb.append(mtRemoteSccpTt);
            sb.append(", ");
        }
        sb.append("]");

        return sb.toString();
    }


        protected static final XMLFormat<SmsExtraData> SMS_EXTRA_DATA_XML = new XMLFormat<SmsExtraData>(SmsExtraData.class) {

        @Override
        public void read(javolution.xml.XMLFormat.InputElement xml, SmsExtraData extraData) throws XMLStreamException {
            extraData.clear();

            String valS = xml.getAttribute(ORIGINATION_TYPE, ZERO_STRING);
            if (valS != null) {
                try {
                    extraData.originationType = Enum.valueOf(OriginationType.class, valS);
                } catch (IllegalArgumentException e) {
                }
            }

            extraData.mprocNotes = xml.get(MPROC_NOTES, String.class);
            extraData.receiptLocalMessageId = xml.get(RECEIPT_LOCAL_MESSAGEID, Long.class);
            extraData.gwIncStart = xml.get(GW_INC_START,Long.class);
            extraData.gwIncStop = xml.get(GW_INC_STOP,Long.class);
            extraData.gwQueStart = xml.get(GW_QUE_START,Long.class);
            extraData.gwQueStop = xml.get(GW_QUE_STOP,Long.class);
            extraData.gwOutStart = xml.get(GW_OUT_START,Long.class);
            extraData.gwOutStop = xml.get(GW_OUT_STOP,Long.class);
            extraData.ocDiaStart = xml.get(OC_DIA_START,Long.class);
            extraData.ocDiaStop = xml.get(OC_DIA_STOP,Long.class);
            extraData.destIP = xml.get(DEST_IP,String.class);
            extraData.destPort = xml.get(DEST_PORT,String.class);
            extraData.sourceIP = xml.get(SOURCE_IP,String.class);
            extraData.sourcePort = xml.get(SOURCE_PORT,String.class);

            extraData.receiptLocalMessageId = xml.get(RECEIPT_LOCAL_MESSAGEID, Long.class);

            extraData.mtLocalSccpGt = xml.get(MT_LOCAL_SCCP_GT, String.class);
            extraData.mtRemoteSccpTt = xml.get(MT_REMOTE_SCCP_TT, Integer.class);

        }

        @Override
        public void write(SmsExtraData extraData, javolution.xml.XMLFormat.OutputElement xml) throws XMLStreamException {
            if (extraData.originationType != null) {
                xml.setAttribute(ORIGINATION_TYPE, extraData.originationType.toString());
            }

            if (extraData.mprocNotes != null) {
                xml.add(extraData.mprocNotes, MPROC_NOTES, String.class);
            }
            if (extraData.receiptLocalMessageId != null) {
                xml.add(extraData.receiptLocalMessageId, RECEIPT_LOCAL_MESSAGEID, Long.class);
            }

           // if (extraData.gwIncStart != 0){
                xml.add(extraData.gwIncStart, GW_INC_START, Long.class);
           // }

          //  if (extraData.gwIncStop != 0){
                xml.add(extraData.gwIncStop, GW_INC_STOP, Long.class);
           // }

           // if (extraData.gwQueStart != 0){
                xml.add(extraData.gwQueStart, GW_QUE_START, Long.class);
          //  }

          //  if (extraData.gwQueStop != 0){
                xml.add(extraData.gwQueStop, GW_QUE_STOP, Long.class);
         //   }

         //   if (extraData.gwOutStart != 0){
                xml.add(extraData.gwOutStart, GW_OUT_START, Long.class);
         //   }

         //   if (extraData.gwOutStop != 0){
                xml.add(extraData.gwOutStop, GW_OUT_STOP, Long.class);
         //   }

         //   if (extraData.ocDiaStart != 0){
                xml.add(extraData.ocDiaStart, OC_DIA_START, Long.class);
         //   }

         //   if (extraData.ocDiaStop != 0){
                xml.add(extraData.ocDiaStop, OC_DIA_STOP, Long.class);
         //   }
            //   if (extraData.ocDiaStop != 0){
                xml.add(extraData.destIP, DEST_IP, String.class);
            //   }
            //   if (extraData.ocDiaStop != 0){
                xml.add(extraData.destPort, DEST_PORT, String.class);
            //   }
            //   if (extraData.ocDiaStop != 0){
                xml.add(extraData.sourceIP, SOURCE_IP, String.class);
            //   }
            //   if (extraData.ocDiaStop != 0){
                xml.add(extraData.sourcePort, SOURCE_PORT, String.class);
            //   }

            if (extraData.mtLocalSccpGt != null) {
                xml.add(extraData.mtLocalSccpGt, MT_LOCAL_SCCP_GT, String.class);
            }
            if (extraData.mtRemoteSccpTt != null) {
                xml.add(extraData.mtRemoteSccpTt, MT_REMOTE_SCCP_TT, Integer.class);
            }
        }
    };

    public long getGwIncStart() {
        return gwIncStart;
    }

    public void setGwIncStart(long gwIncStart) {
        this.gwIncStart = gwIncStart;
    }

    public long getGwIncStop() {
        return gwIncStop;
    }

    public void setGwIncStop(long gwIncStop) {
        this.gwIncStop = gwIncStop;
    }

    public long getGwQueStart() {
        return gwQueStart;
    }

    public void setGwQueStart(long gwQueStart) {
        this.gwQueStart = gwQueStart;
    }

    public long getGwQueStop() {
        return gwQueStop;
    }

    public void setGwQueStop(long gwQueStop) {
        this.gwQueStop = gwQueStop;
    }

    public long getGwOutStart() {
        return gwOutStart;
    }

    public void setGwOutStart(long gwOutStart) {
        this.gwOutStart = gwOutStart;
    }

    public long getGwOutStop() {
        return gwOutStop;
    }

    public void setGwOutStop(long gwOutStop) {
        this.gwOutStop = gwOutStop;
    }

    public long getOcDiaStart() {
        return ocDiaStart;
    }

    public void setOcDiaStart(long ocDiaStart) {
        this.ocDiaStart = ocDiaStart;
    }

    public long getOcDiaStop() {
        return ocDiaStop;
    }

    public void setOcDiaStop(long ocDiaStop) {
        this.ocDiaStop = ocDiaStop;
    }

    public String getDestIP() {
        return destIP;
    }

    public void setDestIP(String destIP) {
        this.destIP = destIP;
    }

    public String getDestPort() {
        return destPort;
    }

    public void setDestPort(String destPort) {
        this.destPort = destPort;
    }

    public String getSourceIP() {
        return sourceIP;
    }

    public void setSourceIP(String sourceIP) {
        this.sourceIP = sourceIP;
    }

    public String getSourcePort() {
        return sourcePort;
    }

    public void setSourcePort(String sourcePort) {
        this.sourcePort = sourcePort;
    }
}
