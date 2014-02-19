/**
 * 
 */
package org.mobicents.smsc.smpp;

import java.util.regex.Pattern;

import javolution.xml.XMLFormat;
import javolution.xml.stream.XMLStreamException;

import com.cloudhopper.smpp.type.Address;

/**
 * @author Amit Bhayani
 * 
 */
public class Sip implements SipMBean {

	private static final String SIP_NAME = "name";

	private static final String SIP_CLUSTER_NAME = "clusterName";

	private static final String REMOTE_HOST_IP = "host";
	private static final String REMOTE_HOST_PORT = "port";

	private static final String SIP_TON = "ton";
	private static final String SIP_NPI = "npi";
	private static final String SIP_ADDRESS_RANGE = "addressRange";
	private static final String CHARGING_ENABLED = "chargingEnabled";
	private static final String COUNTERS_ENABLED = "countersEnabled";

	private static final String STARTED = "started";

	private String name;
	private String clusterName;
	private String host;
	private int port;
	private transient Address address = null;
	private boolean countersEnabled = true;
	private boolean chargingEnabled = false;
	private boolean isStarted = true;

	private Pattern pattern;

	private String sipAddress = null;

	public Sip() {

	}

	/**
	 * 
	 */
	public Sip(String name, String clusterName, String host, int port, boolean chargingEnabled, Address address,
			boolean countersEnabled) {
		this.name = name;
		this.clusterName = clusterName;

		this.host = host;
		this.port = port;
		
		this.resetSipAddress();
		
		this.chargingEnabled = chargingEnabled;
		this.countersEnabled = countersEnabled;

		this.address = address;

		if (this.address != null && this.address.getAddress() != null) {
			this.pattern = Pattern.compile(this.address.getAddress());
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mobicents.smsc.smpp.SipMBean#isStarted()
	 */
	@Override
	public boolean isStarted() {
		return this.isStarted;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mobicents.smsc.smpp.SipMBean#getName()
	 */
	@Override
	public String getName() {
		return this.name;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mobicents.smsc.smpp.SipMBean#getClusterName()
	 */
	@Override
	public String getClusterName() {
		return this.clusterName;
	}

	/**
	 * @return the host
	 */
	@Override
	public String getHost() {
		return host;
	}

	/**
	 * @param host
	 *            the host to set
	 */
	@Override
	public void setHost(String host) {
		this.host = host;
		this.resetSipAddress();
	}

	/**
	 * @return the port
	 */
	@Override
	public int getPort() {
		return port;
	}

	/**
	 * @param port
	 *            the port to set
	 */
	@Override
	public void setPort(int port) {
		this.port = port;
		this.resetSipAddress();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mobicents.smsc.smpp.SipMBean#getAddress()
	 */
	@Override
	public Address getAddress() {
		return this.address;
	}

	@Override
	public void setAddress(Address address) {
		this.address = address;
	}

	@Override
	public boolean isCountersEnabled() {
		return countersEnabled;
	}

	@Override
	public void setCountersEnabled(boolean countersEnabled) {
		this.countersEnabled = countersEnabled;
	}

	@Override
	public boolean isChargingEnabled() {
		return chargingEnabled;
	}

	@Override
	public void setChargingEnabled(boolean chargingEnabled) {
		this.chargingEnabled = chargingEnabled;
	}

	public String getSipAddress() {
		return this.sipAddress;
	}

	/**
	 * @return the pattern
	 */
	protected Pattern getAddressRangePattern() {
		return pattern;
	}

	/**
	 * XML Serialization/Deserialization
	 */
	protected static final XMLFormat<Sip> SIP_XML = new XMLFormat<Sip>(Sip.class) {

		@Override
		public void read(javolution.xml.XMLFormat.InputElement xml, Sip sip) throws XMLStreamException {
			sip.name = xml.getAttribute(SIP_NAME, "");
			sip.clusterName = xml.getAttribute(SIP_CLUSTER_NAME, "");

			sip.host = xml.getAttribute(REMOTE_HOST_IP, "");
			sip.port = xml.getAttribute(REMOTE_HOST_PORT, -1);

			sip.isStarted = xml.getAttribute(STARTED, false);

			byte ton = xml.getAttribute(SIP_TON, (byte) 0);
			byte npi = xml.getAttribute(SIP_NPI, (byte) 0);
			String addressRange = xml.getAttribute(SIP_ADDRESS_RANGE, null);

			sip.address = new Address(ton, npi, addressRange);

			if (addressRange != null) {
				sip.pattern = Pattern.compile(addressRange);
			}

			sip.countersEnabled = xml.getAttribute(COUNTERS_ENABLED, true);
			sip.chargingEnabled = xml.getAttribute(CHARGING_ENABLED, false);
		}

		@Override
		public void write(Sip sip, javolution.xml.XMLFormat.OutputElement xml) throws XMLStreamException {
			xml.setAttribute(SIP_NAME, sip.name);
			xml.setAttribute(SIP_CLUSTER_NAME, sip.clusterName);

			xml.setAttribute(REMOTE_HOST_IP, sip.host);
			xml.setAttribute(REMOTE_HOST_PORT, sip.port);

			xml.setAttribute(STARTED, sip.isStarted);

			xml.setAttribute(SIP_TON, sip.address.getTon());
			xml.setAttribute(SIP_NPI, sip.address.getNpi());
			xml.setAttribute(SIP_ADDRESS_RANGE, sip.address.getAddress());

			xml.setAttribute(COUNTERS_ENABLED, sip.countersEnabled);

			xml.setAttribute(CHARGING_ENABLED, sip.chargingEnabled);
		}
	};

	private void resetSipAddress() {
		this.sipAddress = host + ":" + port;
	}

}
