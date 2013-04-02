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

package org.mobicents.smsc.slee.services.persistence;

import java.io.Serializable;
import java.util.Collection;
import java.util.Date;


import javolution.util.FastList;

import com.cloudhopper.smpp.pdu.Pdu;
import com.cloudhopper.smpp.tlv.Tlv;
import com.cloudhopper.smpp.tlv.TlvConvertException;
import com.eaio.uuid.UUID;

/**
 * 
 * @author amit bhayani
 * @author sergey vetyutnev
 *
 */
public class Sms implements Serializable {

	private static final long serialVersionUID = 6893251312588274520L;

	private SmsSet smsSet;

	private UUID dbId;

	private int sourceAddrTon;
	private int sourceAddrNpi;
	private String sourceAddr;

	private long messageId;

	private String origSystemId;
	private String origEsmeId;

	private Date submitDate;
	private Date deliverDate;

	private String serviceType;
	private int esmClass;

	private int protocolId; // not present in data_sm
	private int priority; // not present in data_sm

	// TODO: was protected ?
	private int registeredDelivery;
	private int replaceIfPresent; // not present in data_sm

	// TODO: was protected ?
	private int dataCoding;
	private int defaultMsgId; // not present in data_sm, not used in deliver_sm

	private byte[] shortMessage;

	private Date scheduleDeliveryTime; // not present in data_sm
	private Date validityPeriod; // not present in data_sm

	private FastList<Tlv> optionalParameters;


	public Sms() {
	}


	/**
	 * ID field for storing into a database
	 */
	public UUID getDbId() {
		return dbId;
	}

	public void setDbId(UUID dbId) {
		this.dbId = dbId;
	}

	/**
	 * DeliveringActivity link
	 */
	public SmsSet getSmsSet() {
		return smsSet;
	}

	public void setSmsSet(SmsSet deliveringActivity) {
		this.smsSet = deliveringActivity;
	}

	/**
	 * smpp style type of number
	 */
	public int getSourceAddrTon() {
		return sourceAddrTon;
	}

	public void setSourceAddrTon(int sourceAddrTon) {
		this.sourceAddrTon = sourceAddrTon;
	}

	/**
	 * smpp style type of numbering plan indicator
	 */
	public int getSourceAddrNpi() {
		return sourceAddrNpi;
	}

	public void setSourceAddrNpi(int sourceAddrNpi) {
		this.sourceAddrNpi = sourceAddrNpi;
	}

	/**
	 * origination address
	 */
	public String getSourceAddr() {
		return sourceAddr;
	}

	public void setSourceAddr(String sourceAddr) {
		this.sourceAddr = sourceAddr;
	}

	/**
	 * Unique message ID assigned by SMSC (since SMSC started)
	 */
	public long getMessageId() {
		return messageId;
	}

	public void setMessageId(long messageId) {
		this.messageId = messageId;
	}

	/**
	 * SMPP name of origination esme (“” for MO messages)
	 */
	public String getOrigSystemId() {
		return origSystemId;
	}

	public void setOrigSystemId(String systemId) {
		this.origSystemId = systemId;
	}

	/**
	 * SMSC internal name of origination esme (“” for MO messages)
	 */
	public String getOrigEsmeId() {
		return origEsmeId;
	}

	public void setOrigEsmeId(String origEsmeId) {
		this.origEsmeId = origEsmeId;
	}

	/**
	 * time when a message was received by SMSC
	 */
	public Date getSubmitDate() {
		return submitDate;
	}

	public void setSubmitDate(Date submitDate) {
		this.submitDate = submitDate;
	}

	/**
	 * time when a message was sent from SMSC (null (?) if message failed to deliver)
	 */
	public Date getDeliverDate() {
		return deliverDate;
	}

	public void setDeliverDate(Date deliverDate) {
		this.deliverDate = deliverDate;
	}

	/**
	 * service_type smpp param for esme originated messages
	 */
	public String getServiceType() {
		return serviceType;
	}

	public void setServiceType(String serviceType) {
		this.serviceType = serviceType;
	}

	/**
	 * Indicates Message Mode and Message Type
	 */
	public int getEsmClass() {
		return esmClass;
	}

	public void setEsmClass(int esmClass) {
		this.esmClass = esmClass;
	}

	/**
	 * Protocol Identifier SMPP parameter (TP-Protocol-Identifier files for GSM)
	 */
	public int getProtocolId() {
		return protocolId;
	}

	public void setProtocolId(int protocolId) {
		this.protocolId = protocolId;
	}

	/**
	 * priority_flag smpp parameter
	 */
	public int getPriority() {
		return priority;
	}

	public void setPriority(int priority) {
		this.priority = priority;
	}

	/**
	 * registered_delivery smpp parameter
	 */
	public int getRegisteredDelivery() {
		return registeredDelivery;
	}

	public void setRegisteredDelivery(int registeredDelivery) {
		this.registeredDelivery = registeredDelivery;
	}

	/**
	 * replace_if_present_flag smpp parameter
	 */
	public int getReplaceIfPresent() {
		return replaceIfPresent;
	}

	public void setReplaceIfPresent(int replaceIfPresent) {
		this.replaceIfPresent = replaceIfPresent;
	}

	/**
	 * data_coding scheme
	 */
	public int getDataCoding() {
		return dataCoding;
	}

	public void setDataCoding(int dataCoding) {
		this.dataCoding = dataCoding;
	}

	/**
	 * sm_default_msg_id smpp parameter
	 */
	public int getDefaultMsgId() {
		return defaultMsgId;
	}

	public void setDefaultMsgId(int defaultMsgId) {
		this.defaultMsgId = defaultMsgId;
	}

	/**
	 * Message text in source style that has been received from EMSE or from MS
	 */
	public byte[] getShortMessage() {
		return shortMessage;
	}

	public void setShortMessage(byte[] shortMessage) {
		this.shortMessage = shortMessage;
	}

	/**
	 * schedule_delivery_time smpp parameter time when SMSC should strat a delivery (may be null – immediate message delivery)
	 */
	public Date getScheduleDeliveryTime() {
		return scheduleDeliveryTime;
	}

	public void setScheduleDeliveryTime(Date scheduleDeliveryTime) {
		this.scheduleDeliveryTime = scheduleDeliveryTime;
	}

	/**
	 * The validity period of this message (if ESME have not defined or for MO messages this field is filled by default SMSC settings)
	 */
	public Date getValidityPeriod() {
		return validityPeriod;
	}

	public void setValidityPeriod(Date validityPeriod) {
		this.validityPeriod = validityPeriod;
	}

	// Optional parameters

	public int getOptionalParameterCount() {
		if (this.optionalParameters == null) {
			return 0;
		}
		return this.optionalParameters.size();
	}

	/**
	 * Gets the current list of optional parameters. If no parameters have been
	 * added, this will return null.
	 * 
	 * @return Null if no parameters added yet, or the list of optional
	 *         parameters.
	 */
	public FastList<Tlv> getOptionalParameters() {
		return this.optionalParameters;
	}

	/**
	 * Adds an optional parameter to this PDU. Does not check if the TLV has
	 * already been added (allows duplicates).
	 * 
	 * @param tlv
	 *            The TLV to add
	 * @see Pdu#setOptionalParameter(com.cloudhopper.smpp.tlv.Tlv)
	 */
	public void addOptionalParameter(Tlv tlv) {
		if (this.optionalParameters == null) {
			this.optionalParameters = new FastList<Tlv>();
		}
		this.optionalParameters.add(tlv);
	}

	/**
	 * Removes an optional parameter by tag. Will only remove the first matching
	 * tag.
	 * 
	 * @param tag
	 *            That tag to remove
	 * @return Null if no TLV removed, or the TLV removed.
	 */
	public Tlv removeOptionalParameter(short tag) {
		// does this parameter exist?
		int i = this.findOptionalParameter(tag);
		if (i < 0) {
			return null;
		} else {
			return this.optionalParameters.remove(i);
		}
	}

	/**
	 * Sets an optional parameter by checking if the tag already exists in our
	 * list of optional parameters. If it already exists, will replace the old
	 * value with the new value.
	 * 
	 * @param tlv
	 *            The TLV to add/set
	 * @return Null if no TLV was replaced, or the TLV replaced.
	 */
	public Tlv setOptionalParameter(Tlv tlv) {
		// does this parameter already exist?
		int i = this.findOptionalParameter(tlv.getTag());
		if (i < 0) {
			// parameter does not yet exist, add it, not replaced
			this.addOptionalParameter(tlv);
			return null;
		} else {
			// this parameter already exists, replace it, return old
			return this.optionalParameters.set(i, tlv);
		}
	}

	/**
	 * Checks if an optional parameter by tag exists.
	 * 
	 * @param tag
	 *            The TLV to search for
	 * @return True if exists, otherwise false
	 */
	public boolean hasOptionalParameter(short tag) {
		return (this.findOptionalParameter(tag) >= 0);
	}

	protected int findOptionalParameter(short tag) {
		if (this.optionalParameters == null) {
			return -1;
		}
		int i = 0;
		for (FastList.Node<Tlv> node = this.optionalParameters.head(), end = this.optionalParameters.tail(); (node = node
				.getNext()) != end;) {
			Tlv tlv = node.getValue();
			if (tlv.getTag() == tag) {
				return i;
			}
			i++;
		}
		// if we get here, we didn't find the parameter by tag
		return -1;
	}

	/**
	 * Gets a TLV by tag.
	 * 
	 * @param tag
	 *            The TLV tag to search for
	 * @return The first matching TLV by tag
	 */
	public Tlv getOptionalParameter(short tag) {
		if (this.optionalParameters == null) {
			return null;
		}
		// try to find this parameter's index
		int i = this.findOptionalParameter(tag);
		if (i < 0) {
			return null;
		}
		return this.optionalParameters.get(i);
	}
	
	
	
	public void addAllOptionalParameter(Collection<Tlv> tlvs) {
		if (this.optionalParameters == null) {
			this.optionalParameters = new FastList<Tlv>();
		}
		this.optionalParameters.addAll(tlvs);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();

		sb.append("SmsEvent [deliveringActivity=");
		sb.append(smsSet);
		sb.append(", dbId=");
		sb.append(dbId);
		sb.append(", sourceAddrTon=");
		sb.append(sourceAddrTon);
		sb.append(", sourceAddrNpi=");
		sb.append(sourceAddrNpi);
		sb.append(", sourceAddr=");
		sb.append(sourceAddr);
		sb.append(", messageId=");
		sb.append(messageId);
		sb.append(", origSystemId=");
		sb.append(origSystemId);
		sb.append(", origEsmeId=");
		sb.append(origEsmeId);
		sb.append(", submitDate=");
		sb.append(submitDate);
		sb.append(", deliverDate=");
		sb.append(deliverDate);
		sb.append(", serviceType=");
		sb.append(serviceType);
		sb.append(", esmClass=");
		sb.append(esmClass);
		sb.append(", protocolId=");
		sb.append(protocolId);
		sb.append(", priority=");
		sb.append(priority);
		sb.append(", registeredDelivery=");
		sb.append(registeredDelivery);
		sb.append(", replaceIfPresent=");
		sb.append(replaceIfPresent);
		sb.append(", dataCoding=");
		sb.append(dataCoding);
		sb.append(", defaultMsgId=");
		sb.append(defaultMsgId);
		sb.append(", scheduleDeliveryTime=");
		sb.append(scheduleDeliveryTime);
		sb.append(", validityPeriod=");
		sb.append(validityPeriod);

		if (this.optionalParameters != null) {
			for (Tlv tlv : this.optionalParameters) {
				sb.append(", ");
				sb.append(tlv.getTagName());
				sb.append("=");
				try {
					sb.append(tlv.getValueAsString());
				} catch (TlvConvertException e) {
				}
				sb.append(validityPeriod);
			}
		}

		// TODO: add printing shortMessage parameter

		sb.append("]");

		return sb.toString();
	}

	
    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
		if (dbId != null)
			return dbId.hashCode();
		else
			return 0;

//    	final int prime = 31;
//        int result = 1;
//
//        result = prime * result + dataCoding;
//        result = prime * result + defaultMsgId;
//        result = prime * result + ((destAddr == null) ? 0 : destAddr.hashCode());
//        result = prime * result + destAddrNpi;
//        result = prime * result + destAddrTon;
//        result = prime * result + esmClass;
//        result = prime * result + ((messageId == null) ? 0 : messageId.hashCode());
//        result = prime * result + ((optionalParameters == null) ? 0 : optionalParameters.hashCode());
//        result = prime * result + priority;
//        result = prime * result + protocolId;
//        result = prime * result + registeredDelivery;
//        result = prime * result + replaceIfPresent;
//        result = prime * result + ((scheduleDeliveryTime == null) ? 0 : scheduleDeliveryTime.hashCode());
//        result = prime * result + Arrays.hashCode(shortMessage);
//        result = prime * result + ((sourceAddr == null) ? 0 : sourceAddr.hashCode());
//        result = prime * result + sourceAddrNpi;
//        result = prime * result + sourceAddrTon;
//        result = prime * result + ((submitDate == null) ? 0 : submitDate.hashCode());
//        result = prime * result + ((origSystemId == null) ? 0 : origSystemId.hashCode());
//        result = prime * result + ((validityPeriod == null) ? 0 : validityPeriod.hashCode());
//        return result;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Sms other = (Sms) obj;

		if (this.dbId == null || other.dbId == null)
			return false;
		if (this.dbId.equals(other.dbId))
			return true;
		else
			return false;

//    	if (dbId != null)
//			return dbId.hashCode();
//		else
//			return 0;

    	
//        if (dataCoding != other.dataCoding)
//            return false;
//        if (defaultMsgId != other.defaultMsgId)
//            return false;
//        if (destAddr == null) {
//            if (other.destAddr != null)
//                return false;
//        } else if (!destAddr.equals(other.destAddr))
//            return false;
//        if (destAddrNpi != other.destAddrNpi)
//            return false;
//        if (destAddrTon != other.destAddrTon)
//            return false;
//        if (esmClass != other.esmClass)
//            return false;
//        if (messageId == null) {
//            if (other.messageId != null)
//                return false;
//        } else if (!messageId.equals(other.messageId))
//            return false;
//        if (optionalParameters == null) {
//            if (other.optionalParameters != null)
//                return false;
//        } else if (!optionalParameters.equals(other.optionalParameters))
//            return false;
//        if (priority != other.priority)
//            return false;
//        if (protocolId != other.protocolId)
//            return false;
//        if (registeredDelivery != other.registeredDelivery)
//            return false;
//        if (replaceIfPresent != other.replaceIfPresent)
//            return false;
//        if (scheduleDeliveryTime == null) {
//            if (other.scheduleDeliveryTime != null)
//                return false;
//        } else if (!scheduleDeliveryTime.equals(other.scheduleDeliveryTime))
//            return false;
//        if (!Arrays.equals(shortMessage, other.shortMessage))
//            return false;
//        if (sourceAddr == null) {
//            if (other.sourceAddr != null)
//                return false;
//        } else if (!sourceAddr.equals(other.sourceAddr))
//            return false;
//        if (sourceAddrNpi != other.sourceAddrNpi)
//            return false;
//        if (sourceAddrTon != other.sourceAddrTon)
//            return false;
//        if (submitDate == null) {
//            if (other.submitDate != null)
//                return false;
//        } else if (!submitDate.equals(other.submitDate))
//            return false;
//        if (origSystemId == null) {
//            if (other.origSystemId != null)
//                return false;
//        } else if (!origSystemId.equals(other.origSystemId))
//            return false;
//        if (validityPeriod == null) {
//            if (other.validityPeriod != null)
//                return false;
//        } else if (!validityPeriod.equals(other.validityPeriod))
//            return false;
//        return true;
    }

}
