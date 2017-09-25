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
    public static final String TIMESTAMP_A = "timestampA";
    public static final String TIMESTAMP_B = "timestampB";
    public static final String TIMESTAMP_C = "timestampC";
    public static final String GW_INC_START = "gw_inc_start";
    public static final String GW_INC_STOP = "gw_inc_stop";
    public static final String GW_QUE_START = "gw_que_stop";
    public static final String GW_QUE_STOP = "gw_que_stop";
    public static final String GW_OUT_START = "gw_out_start";
    public static final String GW_OUT_STOP = "gw_out_stop";
    public static final String OC_DIA_START = "gw_dia_start";
    public static final String OC_DIA_STOP = "gw_dia_stop";


    public static final String ZERO_STRING = null;

    private String mprocNotes;
    private OriginationType originationType;
    private Long receiptLocalMessageId;
    
    private long timestampA;
    private long timestampB;
    private long timestampC;

    private long gw_inc_start;
    private long gw_inc_stop;

    private long gw_que_start;
    private long gw_que_stop;
    private long gw_out_start;
    private long gw_out_stop;
    private long oc_dia_start;
    private long oc_dia_stop;

    public boolean isEmpty() {
        if (this.mprocNotes != null || this.originationType != null || this.receiptLocalMessageId != null 
        		|| this.timestampA != 0 || this.timestampB != 0 || this.timestampC != 0 || this.gw_inc_start != 0
                || this.gw_inc_stop != 0 || this.gw_que_start != 0 || this.getGw_que_stop() != 0 || this.gw_out_start != 0
                || this.gw_out_stop != 0 || this.oc_dia_start != 0 || this.oc_dia_stop != 0 )
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
        gw_inc_start = 0;
        gw_inc_stop = 0;
        gw_que_start = 0;
        gw_que_stop = 0;
        gw_out_start = 0;
        gw_out_stop = 0;
        oc_dia_start = 0;
        oc_dia_stop = 0;
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
        setGw_inc_stop(timestampB);
	}

	public long getTimestampC() {
		return timestampC;
	}

	public void setTimestampC(long timestampC) {
		this.timestampC = timestampC;
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
        if (gw_inc_start != 0) {
            sb.append("gw_inc_start=");
            sb.append(gw_inc_start);
            sb.append(", ");
        }
        if (gw_inc_stop != 0) {
            sb.append("gw_inc_stop=");
            sb.append(gw_inc_stop);
            sb.append(", ");
        }
        if (gw_que_start != 0) {
            sb.append("gw_que_start=");
            sb.append(gw_que_start);
            sb.append(", ");
        }
        if (gw_que_stop != 0) {
            sb.append("gw_que_stop=");
            sb.append(gw_que_stop);
            sb.append(", ");
        }
        if (gw_out_start != 0) {
            sb.append("gw_out_start=");
            sb.append(gw_out_start);
            sb.append(", ");
        }
        if (gw_out_stop != 0) {
            sb.append("gw_out_stop=");
            sb.append(gw_out_stop);
            sb.append(", ");
        }if (oc_dia_start != 0) {
            sb.append("oc_dia_start=");
            sb.append(oc_dia_start);
            sb.append(", ");
        }if (oc_dia_stop != 0) {
            sb.append("oc_dia_stop=");
            sb.append(oc_dia_stop);
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
            extraData.gw_inc_start = xml.get(GW_INC_START,Long.class);
            extraData.gw_inc_stop = xml.get(GW_INC_STOP,Long.class);
            extraData.gw_que_start = xml.get(GW_QUE_START,Long.class);
            extraData.gw_que_stop = xml.get(GW_QUE_STOP,Long.class);
            extraData.gw_out_start = xml.get(GW_OUT_START,Long.class);
            extraData.gw_out_stop = xml.get(GW_OUT_STOP,Long.class);
            extraData.oc_dia_start = xml.get(OC_DIA_START,Long.class);
            extraData.oc_dia_stop = xml.get(OC_DIA_STOP,Long.class);
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

           // if (extraData.gw_inc_start != 0){
                xml.add(extraData.gw_inc_start, GW_INC_START, Long.class);
           // }

          //  if (extraData.gw_inc_stop != 0){
                xml.add(extraData.gw_inc_stop, GW_INC_STOP, Long.class);
           // }

           // if (extraData.gw_que_start != 0){
                xml.add(extraData.gw_que_start, GW_QUE_START, Long.class);
          //  }

          //  if (extraData.gw_que_stop != 0){
                xml.add(extraData.gw_que_stop, GW_QUE_STOP, Long.class);
         //   }

         //   if (extraData.gw_out_start != 0){
                xml.add(extraData.gw_out_start, GW_OUT_START, Long.class);
         //   }

         //   if (extraData.gw_out_stop != 0){
                xml.add(extraData.gw_out_stop, GW_OUT_STOP, Long.class);
         //   }

         //   if (extraData.oc_dia_start != 0){
                xml.add(extraData.oc_dia_start, OC_DIA_START, Long.class);
         //   }

         //   if (extraData.oc_dia_stop != 0){
                xml.add(extraData.oc_dia_stop, OC_DIA_STOP, Long.class);
         //   }
        }
    };

    public long getGw_inc_start() {
        return gw_inc_start;
    }

    public void setGw_inc_start(long gw_inc_start) {
        this.gw_inc_start = gw_inc_start;
    }

    public long getGw_inc_stop() {
        return gw_inc_stop;
    }

    public void setGw_inc_stop(long gw_inc_stop) {
        this.gw_inc_stop = gw_inc_stop;
    }

    public long getGw_que_start() {
        return gw_que_start;
    }

    public void setGw_que_start(long gw_que_start) {
        this.gw_que_start = gw_que_start;
    }

    public long getGw_que_stop() {
        return gw_que_stop;
    }

    public void setGw_que_stop(long gw_que_stop) {
        this.gw_que_stop = gw_que_stop;
    }

    public long getGw_out_start() {
        return gw_out_start;
    }

    public void setGw_out_start(long gw_out_start) {
        this.gw_out_start = gw_out_start;
    }

    public long getGw_out_stop() {
        return gw_out_stop;
    }

    public void setGw_out_stop(long gw_out_stop) {
        this.gw_out_stop = gw_out_stop;
    }

    public long getOc_dia_start() {
        return oc_dia_start;
    }

    public void setOc_dia_start(long oc_dia_start) {
        this.oc_dia_start = oc_dia_start;
    }

    public long getOc_dia_stop() {
        return oc_dia_stop;
    }

    public void setOc_dia_stop(long oc_dia_stop) {
        this.oc_dia_stop = oc_dia_stop;
    }
}
