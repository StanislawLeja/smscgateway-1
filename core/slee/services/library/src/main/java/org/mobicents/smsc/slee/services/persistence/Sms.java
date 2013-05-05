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
import java.util.Date;
import java.util.UUID;

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
	private int moMessageRef;

	private String origSystemId;
	private String origEsmeName;

	private Date submitDate;
	private Date deliveryDate;

	private String serviceType;
	private int esmClass;

	private int protocolId; // not present in data_sm
	private int priority; // not present in data_sm

	private int registeredDelivery;
	private int replaceIfPresent; // not present in data_sm

	private int dataCoding;
	private int defaultMsgId; // not present in data_sm, not used in deliver_sm

	private byte[] shortMessage;

	private Date scheduleDeliveryTime; // not present in data_sm
	private Date validityPeriod; // not present in data_sm

    private int deliveryCount;

	private TlvSet tlvSet = new TlvSet();

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

	public void setSmsSet(SmsSet smsSet) {
		this.smsSet = smsSet;
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
	 * MO SMS-SUBMIT TP-Message-Reference field value 
	 */
	public int getMoMessageRef() {
		return moMessageRef;
	}

	public void setMoMessageRef(int moMessageRef) {
		this.moMessageRef = moMessageRef;
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
	public String getOrigEsmeName() {
		return origEsmeName;
	}

	public void setOrigEsmeName(String origEsmeName) {
		this.origEsmeName = origEsmeName;
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
		return deliveryDate;
	}

	public void setDeliveryDate(Date deliveryDate) {
		this.deliveryDate = deliveryDate;
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
	 * schedule_delivery_time smpp parameter time when SMSC should start a delivery (may be null – immediate message delivery)
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

	/**
	 * delivery tries count
	 */
	public int getDeliveryCount() {
		return deliveryCount;
	}

	public void setDeliveryCount(int deliveryCount) {
		this.deliveryCount = deliveryCount;
	}

	// Optional parameters

	/**
	 * List of tlv parameters
	 */
	public TlvSet getTlvSet() {
		return tlvSet;
	}

	public void setTlvSet(TlvSet tlvSet) {
		this.tlvSet = tlvSet;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();

		sb.append("SmsEvent [SmsSet=");
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
		sb.append(", moMessageRef=");
		sb.append(moMessageRef);
		sb.append(", origSystemId=");
		sb.append(origSystemId);
		sb.append(", origEsmeId=");
		sb.append(origEsmeName);
		sb.append(", submitDate=");
		sb.append(submitDate);
		sb.append(", deliverDate=");
		sb.append(deliveryDate);
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
		sb.append(", deliveryCount=");
		sb.append(deliveryCount);

		if (this.tlvSet != null) {
			sb.append(", tlvSet=");
			sb.append(this.tlvSet.toString());
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
    }

}
