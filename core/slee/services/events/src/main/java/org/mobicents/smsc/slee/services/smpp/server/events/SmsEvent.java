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
package org.mobicents.smsc.slee.services.smpp.server.events;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Collection;

import javolution.util.FastList;

import com.cloudhopper.smpp.pdu.Pdu;
import com.cloudhopper.smpp.tlv.Tlv;

/**
 * 
 * @author amit bhayani
 *
 */
public class SmsEvent implements Serializable {

	/**
	 * Mobicents SMSC variables
	 */

	private String messageId;

	/**
	 * System ID is the ESME System ID. Used only when SMS is coming from ESME
	 */
	private String systemId;

	/**
	 * Time when this SMS was received
	 */
	private Timestamp submitDate;

	/**
	 * From SUBMIT_SM
	 */

	private byte sourceAddrTon;
	private byte sourceAddrNpi;
	private String sourceAddr;

	private byte destAddrTon;
	private byte destAddrNpi;
	private String destAddr;

	private byte esmClass;

	private byte protocolId; // not present in data_sm
	private byte priority; // not present in data_sm

	private String scheduleDeliveryTime; // not present in data_sm
	private String validityPeriod; // not present in data_sm

	protected byte registeredDelivery;

	private byte replaceIfPresent; // not present in data_sm

	protected byte dataCoding;

	private byte defaultMsgId; // not present in data_sm, not used in deliver_sm

	private byte[] shortMessage; // not present in data_sm

	private FastList<Tlv> optionalParameters;

	public SmsEvent() {
	}

	public String getMessageId() {
		return messageId;
	}

	public void setMessageId(String messageId) {
		this.messageId = messageId;
	}

	public String getSystemId() {
		return systemId;
	}

	public void setSystemId(String systemId) {
		this.systemId = systemId;
	}

	public byte getSourceAddrTon() {
		return sourceAddrTon;
	}

	public void setSourceAddrTon(byte sourceAddrTon) {
		this.sourceAddrTon = sourceAddrTon;
	}

	public byte getSourceAddrNpi() {
		return sourceAddrNpi;
	}

	public void setSourceAddrNpi(byte sourceAddrNpi) {
		this.sourceAddrNpi = sourceAddrNpi;
	}

	public String getSourceAddr() {
		return sourceAddr;
	}

	public void setSourceAddr(String sourceAddr) {
		this.sourceAddr = sourceAddr;
	}

	public byte getDestAddrTon() {
		return destAddrTon;
	}

	public void setDestAddrTon(byte destAddrTon) {
		this.destAddrTon = destAddrTon;
	}

	public byte getDestAddrNpi() {
		return destAddrNpi;
	}

	public void setDestAddrNpi(byte destAddrNpi) {
		this.destAddrNpi = destAddrNpi;
	}

	public String getDestAddr() {
		return destAddr;
	}

	public void setDestAddr(String destAddr) {
		this.destAddr = destAddr;
	}

	public byte getEsmClass() {
		return esmClass;
	}

	public void setEsmClass(byte esmClass) {
		this.esmClass = esmClass;
	}

	public byte getProtocolId() {
		return protocolId;
	}

	public void setProtocolId(byte protocolId) {
		this.protocolId = protocolId;
	}

	public byte getPriority() {
		return priority;
	}

	public void setPriority(byte priority) {
		this.priority = priority;
	}

	public String getScheduleDeliveryTime() {
		return scheduleDeliveryTime;
	}

	public void setScheduleDeliveryTime(String scheduleDeliveryTime) {
		this.scheduleDeliveryTime = scheduleDeliveryTime;
	}

	public String getValidityPeriod() {
		return validityPeriod;
	}

	public void setValidityPeriod(String validityPeriod) {
		this.validityPeriod = validityPeriod;
	}

	public byte getRegisteredDelivery() {
		return registeredDelivery;
	}

	public void setRegisteredDelivery(byte registeredDelivery) {
		this.registeredDelivery = registeredDelivery;
	}

	public byte getReplaceIfPresent() {
		return replaceIfPresent;
	}

	public void setReplaceIfPresent(byte replaceIfPresent) {
		this.replaceIfPresent = replaceIfPresent;
	}

	public byte getDataCoding() {
		return dataCoding;
	}

	public void setDataCoding(byte dataCoding) {
		this.dataCoding = dataCoding;
	}

	public byte getDefaultMsgId() {
		return defaultMsgId;
	}

	public void setDefaultMsgId(byte defaultMsgId) {
		this.defaultMsgId = defaultMsgId;
	}

	public byte[] getShortMessage() {
		return shortMessage;
	}

	public void setShortMessage(byte[] shortMessage) {
		this.shortMessage = shortMessage;
	}

	public Timestamp getSubmitDate() {
		return submitDate;
	}

	public void setSubmitDate(Timestamp submitDate) {
		this.submitDate = submitDate;
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
		return "SmsEvent [messageId=" + messageId + ", systemId=" + systemId + ", sourceAddrTon=" + sourceAddrTon
				+ ", sourceAddrNpi=" + sourceAddrNpi + ", sourceAddr=" + sourceAddr + ", destAddrTon=" + destAddrTon
				+ ", destAddrNpi=" + destAddrNpi + ", destAddr=" + destAddr + ", esmClass=" + esmClass
				+ ", protocolId=" + protocolId + ", priority=" + priority + ", scheduleDeliveryTime="
				+ scheduleDeliveryTime + ", validityPeriod=" + validityPeriod + ", registeredDelivery="
				+ registeredDelivery + ", replaceIfPresent=" + replaceIfPresent + ", dataCoding=" + dataCoding
				+ ", defaultMsgId=" + defaultMsgId + "]";
	}

	// @Override
	// public int hashCode() {
	// final int prime = 71;
	// int result = 1;
	// result = prime * result + (int) (messageId ^ (messageId >>> 32));
	// return result;
	// }
	//
	// @Override
	// public boolean equals(Object obj) {
	// if (this == obj)
	// return true;
	// if (obj == null)
	// return false;
	// if (getClass() != obj.getClass())
	// return false;
	// SmsEvent other = (SmsEvent) obj;
	// if (messageId != other.messageId)
	// return false;
	// return true;
	// }

}
