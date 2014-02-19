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

	Address getAddress();

	void setAddress(Address address);

	boolean isCountersEnabled();

	void setCountersEnabled(boolean countersEnabled);

	boolean isChargingEnabled();

	void setChargingEnabled(boolean chargingEnabled);

}
