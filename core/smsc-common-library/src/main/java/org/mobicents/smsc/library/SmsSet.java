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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import org.mobicents.protocols.ss7.map.api.service.sms.LocationInfoWithLMSI;


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
    private int networkId;

	// destination info - not saved in LIVE table
	private String destClusterName;
	private String destSystemId;
	private String destEsmeName;

	// routing info - not saved in LIVE table
	private String imsi;
	private LocationInfoWithLMSI locationInfoWithLMSI;
	// correlationId for homeRouting mode
    private String correlationId;

	private Date dueDate;
	// last interval between delivering (sec)
	private int dueDelay;
	private int inSystem;
	private Date inSystemDate;

	private ErrorCode status;
    private SmType type;

    private Date lastDelivery;
    private boolean alertingSupported;

    private boolean processingStarted = false;

	private List<Sms> smsList = new ArrayList<Sms>();

    private Date creationTime = new Date();

    public SmsSet() {
	}

	public String getTargetId() {
		StringBuilder sb = new StringBuilder();
		sb.append(this.destAddr);
		sb.append("_");
		sb.append(this.destAddrTon);
        sb.append("_");
        sb.append(this.destAddrNpi);
        sb.append("_");
        sb.append(this.networkId);
		return sb.toString();
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
     * networkId
     */
    public int getNetworkId() {
        return networkId;
    }

    public void setNetworkId(int networkId) {
        this.networkId = networkId;
    }

	/**
	 * name of cluster for destination ESME terminated massages (�� for MT messages)
	 */
	public String getDestClusterName() {
		return destClusterName;
	}

	public void setDestClusterName(String destClusterName) {
		this.destClusterName = destClusterName;
	}

	/**
	 * SMPP name of destination esme (�� for MT messages)
	 */
	public String getDestSystemId() {
		return destSystemId;
	}

	public void setDestSystemId(String destSystemId) {
		this.destSystemId = destSystemId;
	}

	/**
	 * SMSC internal name of destination esme (�� for MT messages)
	 */
	public String getDestEsmeName() {
		return destEsmeName;
	}

	public void setDestEsmeName(String destEsmeName) {
		this.destEsmeName = destEsmeName;
	}

	/**
	 * RoutingInfo: imsi
	 */
	public String getImsi() {
		return imsi;
	}

	public void setImsi(String imsi) {
		this.imsi = imsi;
	}

	/**
	 * RoutingInfo: locationInfoWithLMSI
	 */
	public LocationInfoWithLMSI getLocationInfoWithLMSI() {
		return locationInfoWithLMSI;
	}

	public void setLocationInfoWithLMSI(LocationInfoWithLMSI locationInfoWithLMSI) {
		this.locationInfoWithLMSI = locationInfoWithLMSI;
	}

    /**
     * correlationId for homeRouting mode
     */
    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

	/**
	 * last interval between delivering (sec)
	 */
	public int getDueDelay() {
		return dueDelay;
	}

    public void setDueDelay(int dueDelay) {
        this.dueDelay = dueDelay;
    }

    public void updateDueDelay(int dueDelay) {
        if (this.dueDelay > dueDelay)
            this.dueDelay = dueDelay;
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
	 * 0-subscriber target not schedulered by deuDate, 1-schedulered by deuDate, 2-delivering in processed
	 */
	public int getInSystem() {
		return inSystem;
	}

	public void setInSystem(int inSystem) {
		this.inSystem = inSystem;
	}

	/**
	 * Time when SmsSet went to IN_SYSTEM state (when delivering process has been started)
	 */
	public Date getInSystemDate() {
		return inSystemDate;
	}

	public void setInSystemDate(Date inSystemDate) {
		this.inSystemDate = inSystemDate;
	}

	/**
	 * ErrorCode value will be put here for last attempt (0==success / no attempts yet, !=0 � ErrorCode of the last attempt)
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
	 * time of last delivery attempt (null if it were not attempts)
	 */
	public Date getLastDelivery() {
		return lastDelivery;
	}

	public void setLastDelivery(Date lastDelivery) {
		this.lastDelivery = lastDelivery;
	}

	/**
	 * true if after SMSC was successfully registered at HLR after delivery failure
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

	public void resortSms() {
		Collections.sort(this.smsList, new SmsComparator());
	}

	public Sms getSms(int index) {
		if (index >= 0 && index < this.smsList.size())
			return this.smsList.get(index);
		else
			return null;
	}

	public int getSmsCount() {
		return this.smsList.size();
	}

	public List<Sms> getRawList(){
	    return new ArrayList<Sms>(this.smsList);
	}

    public boolean isProcessingStarted() {
        return processingStarted;
    }

    public void setProcessingStarted() {
        processingStarted = true;
    }

    public Date getCreationTime() {
        return creationTime;
    }

    public boolean checkSmsPresent(Sms sms) {
        for (Sms smsa : smsList) {
            if (smsa.getDbId().equals(sms.getDbId())) {
                return true;
            }
        }
        return false;
    }

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();

		sb.append("SmsSet [destAddrTon=");
		sb.append(destAddrTon);
		sb.append(", destAddrNpi=");
		sb.append(destAddrNpi);
        sb.append(", destAddr=");
        sb.append(destAddr);
        sb.append(", networkId=");
        sb.append(networkId);
		sb.append(", destClusterName=");
		sb.append(destClusterName);
		sb.append(", destSystemId=");
		sb.append(destSystemId);
		sb.append(", destEsmeId=");
		sb.append(destEsmeName);
		sb.append(", imsi=");
		sb.append(imsi);
		sb.append(", locationInfoWithLMSI=");
		sb.append(locationInfoWithLMSI);
        sb.append(", correlationId=");
        sb.append(correlationId);
		sb.append(", inSystem=");
		sb.append(inSystem);
		sb.append(", inSystemDate=");
		sb.append(inSystemDate);
		sb.append(", dueDate=");
		sb.append(dueDate);
		sb.append(", dueDelay=");
		sb.append(dueDelay);
		sb.append(", status=");
		sb.append(status);
		sb.append(", type=");
		sb.append(type);
		sb.append(", lastDelivery=");
		sb.append(lastDelivery);
		sb.append(", alertingSupported=");
		sb.append(alertingSupported);

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
        result = prime * result + networkId;
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
        if (this.networkId != other.networkId)
            return false;

		if (destAddr == null) {
			if (other.destAddr != null)
				return false;
		} else if (!destAddr.equals(other.destAddr))
			return false;

		return true;
    }

	public class SmsComparator implements Comparator<Sms> {

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
