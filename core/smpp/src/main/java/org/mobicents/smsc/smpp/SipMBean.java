/**
 * 
 */
package org.mobicents.smsc.smpp;

import com.cloudhopper.smpp.type.Address;

/**
 * @author Amit Bhayani
 * 
 */
public interface SipMBean {

	boolean isStarted();

	String getName();

	/**
	 * Cluster in which this SIP stack belongs. Not used as of now.
	 * 
	 * @param clusterName
	 */
	void setClusterName(String clusterName);

	String getClusterName();

	/**
	 * Address of remote host where all SIP messages should be forwarded to
	 * 
	 * @return
	 */
	String getHost();

	void setHost(String host);

	/**
	 * port of remote host where all SIP messages are sent
	 * 
	 * @return
	 */
	int getPort();

	void setPort(int port);

	byte getAddressNpi();

	/**
	 * Helper method to modify the Address numbering plan indicator
	 * 
	 * @param npi
	 */
	void setAddressNpi(byte npi);

	byte getAddressTon();

	/**
	 * Helper method to modify the Address type of number
	 * 
	 * @param ton
	 */
	void setAddressTon(byte ton);

	String getAddressRange();

	/**
	 * Helper method to modify the Address range
	 * 
	 * @param range
	 */
	void setAddressRange(String range);

	/**
	 * true if counters is enabled. Not used as of now
	 * 
	 * @return
	 */
	boolean isCountersEnabled();

	void setCountersEnabled(boolean countersEnabled);

	/**
	 * true if charging is enabled for this SIP stack
	 * 
	 * @return
	 */
	boolean isChargingEnabled();

	void setChargingEnabled(boolean chargingEnabled);

}
