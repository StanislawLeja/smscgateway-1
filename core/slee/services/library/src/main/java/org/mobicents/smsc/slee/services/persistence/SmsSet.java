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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;


/**
 * 
 * @author amit bhayani
 * @author sergey vetyutnev
 *
 */
public class SmsSet implements Serializable {

	private static final long serialVersionUID = -627234093407300864L;

	private int destAddrTon;
	private int destAddrNpi;
	private String destAddr;
	
	private String destClusterName;

	private Date dueDate;

	private ErrorCode status;
    private SmType type;
	
    private int deliveryCount;

    private Date lastDelivery;
    private boolean alertingSupported;

	private List<Sms> smsList = new ArrayList<Sms>();
	private int messageIndex = 0;

    public SmsSet() {
	}


    /**
     * smpp style type of number
     */
    public int getDestAddrTon() {
		return destAddrTon;
	}

	public void setDestAddrTon(int destAddrTon) {
		this.destAddrTon = destAddrTon;
	}

    /**
     * smpp style type of numbering plan indicator
     */
	public int getDestAddrNpi() {
		return destAddrNpi;
	}

	public void setDestAddrNpi(int destAddrNpi) {
		this.destAddrNpi = destAddrNpi;
	}

    /**
     * destination address
     */
	public String getDestAddr() {
		return destAddr;
	}

	public void setDestAddr(String destAddr) {
		this.destAddr = destAddr;
	}

	/**
	 * name of cluster for destination ESME terminated massages (“” for MT messages)
	 */
	public String getDestClusterName() {
		return destClusterName;
	}

	public void setDestClusterName(String destClusterName) {
		this.destClusterName = destClusterName;
	}

	/**
	 * time when next delivery attempts must be done
	 */
	public Date getDueDate() {
		return dueDate;
	}

	public void setDueDate(Date dueDate) {
		this.dueDate = dueDate;
	}

	/**
	 * ErrorCode value will be put here for last attempt (0==success / no attempts yet, !=0 – ErrorCode of the last attempt)
	 */
	public ErrorCode getStatus() {
		return status;
	}

	public void setStatus(ErrorCode status) {
		this.status = status;
	}

	/**
	 * 0-esme terminated, 1-MT
	 */
	public SmType getType() {
		return type;
	}

	public void setType(SmType type) {
		this.type = type;
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

	/**
	 * time of last delivery attempt (null if it were not attempts)
	 */
	private Date getLastDelivery() {
		return lastDelivery;
	}

	private void setLastDelivery(Date lastDelivery) {
		this.lastDelivery = lastDelivery;
	}

	/**
	 * true if after SMSC was successfully registerd at HLR after delivery failure
	 */
	public boolean isAlertingSupported() {
		return alertingSupported;
	}

	public void setAlertingSupported(boolean alertingSupported) {
		this.alertingSupported = alertingSupported;
	}

	// managing of SMS list
	public void clearSmsList() {
		this.smsList.clear();
	}

	public void addSms(Sms sms) {
		sms.setSmsSet(this);
		this.smsList.add(sms);
	}

	public Sms getFirstSms() {
		Collections.sort(this.smsList, new SmsComparator());
		this.messageIndex = 0;
		return getNextSms();
	}

	public Sms getNextSms() {
		if (this.messageIndex >= this.smsList.size())
			return null;
		else
			return this.smsList.get(this.messageIndex++);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();

		sb.append("DeliveringActivity [destAddrTon=");
		sb.append(destAddrTon);
		sb.append(", destAddrNpi=");
		sb.append(destAddrNpi);
		sb.append(", destAddr=");
		sb.append(destAddr);
		sb.append(", destClusterName=");
		sb.append(destClusterName);
		sb.append(", dueDate=");
		sb.append(dueDate);
		sb.append(", messageId=");
		sb.append(status);
		sb.append(", status=");
		sb.append(type);
		sb.append(", type=");
		sb.append(deliveryCount);
		sb.append(", deliveryCount=");
		sb.append(lastDelivery);
		sb.append(", lastDelivery=");
		sb.append(alertingSupported);
		sb.append(", alertingSupported=");

		sb.append("]");

		return sb.toString();
	}
	
    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
    	final int prime = 31;
        int result = 1;

        result = prime * result + destAddrTon;
        result = prime * result + destAddrNpi;
        result = prime * result + ((destAddr == null) ? 0 : destAddr.hashCode());
        return result;
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
		SmsSet other = (SmsSet) obj;

		if (this.destAddrTon != other.destAddrTon)
			return false;
		if (this.destAddrNpi != other.destAddrNpi)
			return false;

		if (destAddr == null) {
			if (other.destAddr != null)
				return false;
		} else if (!destAddr.equals(other.destAddr))
			return false;

		return true;
    }

	private class SmsComparator implements Comparator<Sms> {

		@Override
		public int compare(Sms a1, Sms a2) {
			if (a1.getMessageId() == a2.getMessageId())
				return 0;
			else {
				if (a1.getMessageId() < a2.getMessageId())
					return -1;
				else
					return 1;
			}
		}

	}
}
