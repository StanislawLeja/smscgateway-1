/*
 * TeleStax, Open Source Cloud Communications  
 * Copyright 2012, Telestax Inc and individual contributors
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

package org.mobicents.smsc.smpp;

import java.util.regex.Pattern;

import javolution.xml.XMLFormat;
import javolution.xml.XMLSerializable;
import javolution.xml.stream.XMLStreamException;

import org.apache.log4j.Logger;

import com.cloudhopper.smpp.SmppBindType;
import com.cloudhopper.smpp.SmppSession;
import com.cloudhopper.smpp.SmppSession.Type;
import com.cloudhopper.smpp.impl.DefaultSmppSession;
import com.cloudhopper.smpp.type.Address;

/**
 * @author amit bhayani
 * 
 */
public class Esme implements XMLSerializable, EsmeMBean {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private static final Logger logger = Logger.getLogger(Esme.class);

	private static final String ESME_NAME = "name";

	private static final String ESME_CLUSTER_NAME = "clusterName";

	private static final String ESME_SYSTEM_ID = "systemId";
	private static final String ESME_PASSWORD = "password";
	private static final String REMOTE_HOST_IP = "host";
	private static final String REMOTE_HOST_PORT = "port";
	private static final String SMPP_BIND_TYPE = "smppBindType";

	private static final String SMPP_SESSION_TYPE = "smppSessionType";

	private static final String ESME_SYSTEM_TYPE = "systemType";
	private static final String ESME_INTERFACE_VERSION = "smppVersion";
	private static final String ESME_TON = "ton";
	private static final String ESME_NPI = "npi";
	private static final String ESME_ADDRESS_RANGE = "addressRange";

	private static final String WINDOW_SIZE = "windowSize";
	private static final String CONNECT_TIMEOUT = "connectTimeout";
	private static final String REQUEST_EXPIRY_TIMEOUT = "requestExpiryTimeout";
	private static final String WINDOW_MONITOR_INTERVAL = "windowMonitorInterval";
	private static final String WINDOW_WAIT_TIMEOUT = "windowWaitTimeout";

	private static final String ENQUIRE_LINK_DELAY = "enquireLinkDelay";
	private static final String COUNTERS_ENABLED = "countersEnabled";

	private static final String STARTED = "started";

	private String name;
	private String clusterName;
	private String systemId;
	private String password;
	private String host;
	private int port;
	private String systemType;
	private SmppInterfaceVersionType smppVersion = null;
	private transient Address address = null;
	private SmppBindType smppBindType;

	private boolean countersEnabled = true;

	private int enquireLinkDelay = 30000;

	// Default Server
	private SmppSession.Type smppSessionType = SmppSession.Type.SERVER;

	// Client side config. Defaul 100
	private int windowSize;
	private long connectTimeout;

	/**
	 * Set the amount of time(ms) to wait for an endpoint to respond to a
	 * request before it expires. Defaults to disabled (-1).
	 */
	private long requestExpiryTimeout;

	/**
	 * Sets the amount of time between executions of monitoring the window for
	 * requests that expire. It's recommended that this generally either matches
	 * or is half the value of requestExpiryTimeout. Therefore, at worst a
	 * request would could take up 1.5X the requestExpiryTimeout to clear out.
	 */
	private long windowMonitorInterval;

	/**
	 * Set the amount of time to wait until a slot opens up in the sendWindow.
	 * Defaults to 60000.
	 */
	private long windowWaitTimeout;

	protected transient SmscManagement smscManagement = null;

	private boolean started = false;

	private String state = SmppSession.STATES[SmppSession.STATE_CLOSED];

	private transient DefaultSmppSession defaultSmppSession = null;

	private Pattern pattern;

	public Esme() {

	}

	public Esme(String name, String systemId, String pwd, String host, int port, SmppBindType smppBindType,
			String systemType, SmppInterfaceVersionType version, Address address, String clusterName,
			boolean countersEnabled) {
		this.name = name;

		this.systemId = systemId;
		this.password = pwd;
		this.host = host;
		this.port = port;
		this.systemType = systemType;
		this.smppVersion = version;
		this.address = address;
		this.smppBindType = smppBindType;

		if (this.address != null && this.address.getAddress() != null) {
			this.pattern = Pattern.compile(this.address.getAddress());
		}

		this.clusterName = clusterName;
	}

	/**
	 * @param systemId
	 * @param password
	 * @param host
	 * @param port
	 * @param systemType
	 * @param smppVersion
	 * @param address
	 * @param smppBindType
	 * @param smppSessionType
	 * @param smscManagement
	 * @param state
	 */
	public Esme(String name, String systemId, String password, String host, int port, String systemType,
			SmppInterfaceVersionType smppVersion, Address address, SmppBindType smppBindType, Type smppSessionType,
			int windowSize, long connectTimeout, long requestExpiryTimeout, long windowMonitorInterval,
			long windowWaitTimeout, String clusterName, boolean countersEnabled, int enquireLinkDelay) {
		this(name, systemId, password, host, port, smppBindType, systemType, smppVersion, address, clusterName,
				countersEnabled);

		this.smppSessionType = smppSessionType;

		this.windowSize = windowSize;
		this.connectTimeout = connectTimeout;
		this.requestExpiryTimeout = requestExpiryTimeout;
		this.windowMonitorInterval = windowMonitorInterval;
		this.windowWaitTimeout = windowWaitTimeout;

	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return the clusterName
	 */
	public String getClusterName() {
		return clusterName;
	}

	/**
	 * @param clusterName
	 *            the clusterName to set
	 */
	protected void setClusterName(String clusterName) {
		this.clusterName = clusterName;
	}

	/**
	 * Every As has unique name
	 * 
	 * @return String name of this As
	 */
	public String getSystemId() {
		return this.systemId;
	}

	/**
	 * @param systemId
	 *            the systemId to set
	 */
	public void setSystemId(String systemId) {
		this.systemId = systemId;
	}

	/**
	 * @return the password
	 */
	public String getPassword() {
		return password;
	}

	/**
	 * @param password
	 *            the password to set
	 */
	public void setPassword(String password) {
		this.password = password;
	}

	/**
	 * @return the host
	 */
	public String getHost() {
		return host;
	}

	/**
	 * @param host
	 *            the host to set
	 */
	public void setHost(String host) {
		this.host = host;
	}

	/**
	 * @return the port
	 */
	public int getPort() {
		return port;
	}

	/**
	 * @param port
	 *            the port to set
	 */
	public void setPort(int port) {
		this.port = port;
	}

	protected SmppBindType getSmppBindType() {
		return smppBindType;
	}

	protected void setSmppBindType(SmppBindType smppBindType) {
		this.smppBindType = smppBindType;
	}

	/**
	 * @return the systemType
	 */
	public String getSystemType() {
		return systemType;
	}

	/**
	 * @param systemType
	 *            the systemType to set
	 */
	public void setSystemType(String systemType) {
		this.systemType = systemType;
	}

	/**
	 * @return the smppVersion
	 */
	public SmppInterfaceVersionType getSmppVersion() {
		return smppVersion;
	}

	/**
	 * @param smppVersion
	 *            the smppVersion to set
	 */
	public void setSmppVersion(SmppInterfaceVersionType smppVersion) {
		this.smppVersion = smppVersion;
	}

	public Address getAddress() {
		return address;
	}

	public void setAddress(Address address) {
		this.address = address;
	}

	/**
	 * @return the smppSessionType
	 */
	public SmppSession.Type getSmppSessionType() {
		return smppSessionType;
	}

	/**
	 * @param smppSessionType
	 *            the smppSessionType to set
	 */
	public void setSmppSessionType(SmppSession.Type smppSessionType) {
		this.smppSessionType = smppSessionType;
	}

	/**
	 * @return the windowSize
	 */
	public int getWindowSize() {
		return windowSize;
	}

	/**
	 * @param windowSize
	 *            the windowSize to set
	 */
	public void setWindowSize(int windowSize) {
		this.windowSize = windowSize;
	}

	/**
	 * @return the connectTimeout
	 */
	public long getConnectTimeout() {
		return connectTimeout;
	}

	/**
	 * @param connectTimeout
	 *            the connectTimeout to set
	 */
	public void setConnectTimeout(int connectTimeout) {
		this.connectTimeout = connectTimeout;
	}

	/**
	 * @return the requestExpiryTimeout
	 */
	public long getRequestExpiryTimeout() {
		return requestExpiryTimeout;
	}

	/**
	 * @param requestExpiryTimeout
	 *            the requestExpiryTimeout to set
	 */
	public void setRequestExpiryTimeout(long requestExpiryTimeout) {
		this.requestExpiryTimeout = requestExpiryTimeout;
	}

	/**
	 * @return the windowMonitorInterval
	 */
	public long getWindowMonitorInterval() {
		return windowMonitorInterval;
	}

	/**
	 * @param windowMonitorInterval
	 *            the windowMonitorInterval to set
	 */
	public void setWindowMonitorInterval(long windowMonitorInterval) {
		this.windowMonitorInterval = windowMonitorInterval;
	}

	/**
	 * @return the windowWaitTimeout
	 */
	public long getWindowWaitTimeout() {
		return windowWaitTimeout;
	}

	/**
	 * @param windowWaitTimeout
	 *            the windowWaitTimeout to set
	 */
	public void setWindowWaitTimeout(long windowWaitTimeout) {
		this.windowWaitTimeout = windowWaitTimeout;
	}

	/**
	 * @return the started
	 */
	public boolean isStarted() {
		return started;
	}

	/**
	 * @param started
	 *            the started to set
	 */
	public void setStarted(boolean started) {
		this.started = started;
	}

	/**
	 * @return the smppSession
	 */
	public DefaultSmppSession getSmppSession() {
		return defaultSmppSession;
	}

	/**
	 * @param smppSession
	 *            the smppSession to set
	 */
	public void setSmppSession(DefaultSmppSession smppSession) {
		this.defaultSmppSession = smppSession;
	}

	/**
	 * @return the pattern
	 */
	protected Pattern getAddressRangePattern() {
		return pattern;
	}

	public int getEnquireLinkDelay() {
		return enquireLinkDelay;
	}

	public void setEnquireLinkDelay(int enquireLinkDelay) {
		this.enquireLinkDelay = enquireLinkDelay;
	}

	public boolean isCountersEnabled() {
		return countersEnabled;
	}

	/**
	 * XML Serialization/Deserialization
	 */
	protected static final XMLFormat<Esme> ESME_XML = new XMLFormat<Esme>(Esme.class) {

		@Override
		public void read(javolution.xml.XMLFormat.InputElement xml, Esme esme) throws XMLStreamException {
			esme.name = xml.getAttribute(ESME_NAME, "");
			esme.clusterName = xml.getAttribute(ESME_CLUSTER_NAME, "");
			esme.systemId = xml.getAttribute(ESME_SYSTEM_ID, "");
			esme.password = xml.getAttribute(ESME_PASSWORD, "");
			esme.host = xml.getAttribute(REMOTE_HOST_IP, "");
			esme.port = xml.getAttribute(REMOTE_HOST_PORT, -1);

			String smppBindTypeStr = xml.getAttribute(SMPP_BIND_TYPE, "TRANSCEIVER");

			if (SmppBindType.TRANSCEIVER.toString().equals(smppBindTypeStr)) {
				esme.smppBindType = SmppBindType.TRANSCEIVER;
			} else if (SmppBindType.TRANSMITTER.toString().equals(smppBindTypeStr)) {
				esme.smppBindType = SmppBindType.TRANSMITTER;
			} else if (SmppBindType.RECEIVER.toString().equals(smppBindTypeStr)) {
				esme.smppBindType = SmppBindType.RECEIVER;
			}

			String smppSessionTypeStr = xml.getAttribute(SMPP_SESSION_TYPE, "SERVER");
			esme.smppSessionType = SmppSession.Type.valueOf(smppSessionTypeStr);

			esme.started = xml.getAttribute(STARTED, false);

			esme.systemType = xml.getAttribute(ESME_SYSTEM_TYPE, "");
			esme.smppVersion = SmppInterfaceVersionType.getInterfaceVersionType(xml.getAttribute(
					ESME_INTERFACE_VERSION, ""));

			byte ton = xml.getAttribute(ESME_TON, (byte) 0);
			byte npi = xml.getAttribute(ESME_NPI, (byte) 0);
			String addressRange = xml.getAttribute(ESME_ADDRESS_RANGE, null);

			esme.address = new Address(ton, npi, addressRange);

			esme.windowSize = xml.getAttribute(WINDOW_SIZE, 0);
			esme.connectTimeout = xml.getAttribute(CONNECT_TIMEOUT, 0);
			esme.requestExpiryTimeout = xml.getAttribute(REQUEST_EXPIRY_TIMEOUT, 0);
			esme.windowMonitorInterval = xml.getAttribute(WINDOW_MONITOR_INTERVAL, 0);
			esme.windowWaitTimeout = xml.getAttribute(WINDOW_WAIT_TIMEOUT, 0);
			esme.countersEnabled = xml.getAttribute(COUNTERS_ENABLED, true);
			esme.enquireLinkDelay = xml.getAttribute(ENQUIRE_LINK_DELAY, 30000);
			

		}

		@Override
		public void write(Esme esme, javolution.xml.XMLFormat.OutputElement xml) throws XMLStreamException {
			xml.setAttribute(ESME_NAME, esme.name);
			xml.setAttribute(ESME_CLUSTER_NAME, esme.clusterName);
			xml.setAttribute(ESME_SYSTEM_ID, esme.systemId);
			xml.setAttribute(ESME_PASSWORD, esme.password);
			xml.setAttribute(REMOTE_HOST_IP, esme.host);
			xml.setAttribute(REMOTE_HOST_PORT, esme.port);
			xml.setAttribute(SMPP_BIND_TYPE, esme.smppBindType.toString());
			xml.setAttribute(SMPP_SESSION_TYPE, esme.smppSessionType.toString());

			xml.setAttribute(STARTED, esme.started);

			xml.setAttribute(ESME_INTERFACE_VERSION, esme.smppVersion.getType());
			if (esme.systemType != null) {
				xml.setAttribute(ESME_SYSTEM_TYPE, esme.systemType);
			}
			xml.setAttribute(ESME_TON, esme.address.getTon());
			xml.setAttribute(ESME_NPI, esme.address.getNpi());
			xml.setAttribute(ESME_ADDRESS_RANGE, esme.address.getAddress());

			xml.setAttribute(WINDOW_SIZE, esme.windowSize);
			xml.setAttribute(CONNECT_TIMEOUT, esme.connectTimeout);
			xml.setAttribute(REQUEST_EXPIRY_TIMEOUT, esme.requestExpiryTimeout);
			xml.setAttribute(WINDOW_MONITOR_INTERVAL, esme.windowMonitorInterval);
			xml.setAttribute(WINDOW_WAIT_TIMEOUT, esme.windowWaitTimeout);
			xml.setAttribute(COUNTERS_ENABLED, esme.countersEnabled);
			xml.setAttribute(ENQUIRE_LINK_DELAY, esme.enquireLinkDelay);
		}
	};

	public void show(StringBuffer sb) {
		sb.append(SMSCOAMMessages.SHOW_ESME_NAME).append(this.name).append(SMSCOAMMessages.SHOW_ESME_SYSTEM_ID)
				.append(this.systemId).append(SMSCOAMMessages.SHOW_ESME_STATE).append(this.getStateName())
				.append(SMSCOAMMessages.SHOW_ESME_PASSWORD).append(this.password)
				.append(SMSCOAMMessages.SHOW_ESME_HOST).append(this.host).append(SMSCOAMMessages.SHOW_ESME_PORT)
				.append(this.port).append(SMSCOAMMessages.SHOW_ESME_BIND_TYPE).append(this.smppBindType)
				.append(SMSCOAMMessages.SHOW_ESME_SYSTEM_TYPE).append(this.systemType)
				.append(SMSCOAMMessages.SHOW_ESME_INTERFACE_VERSION).append(this.smppVersion)
				.append(SMSCOAMMessages.SHOW_ADDRESS).append(this.address).append(SMSCOAMMessages.SHOW_CLUSTER_NAME)
				.append(this.clusterName);

		sb.append(SMSCOAMMessages.NEW_LINE);
	}

	@Override
	public void close() {
		if (this.defaultSmppSession != null) {
			this.defaultSmppSession.close();
		}
	}

	@Override
	public void close(long arg0) {
		if (this.defaultSmppSession != null) {
			this.defaultSmppSession.close(arg0);
		}
	}

	@Override
	public void destroy() {
		if (this.defaultSmppSession != null) {
			this.defaultSmppSession.destroy();
		}
	}

	@Override
	public void disableLogBytes() {
		if (this.defaultSmppSession != null) {
			this.defaultSmppSession.disableLogBytes();
		}
	}

	@Override
	public void disableLogPdu() {
		if (this.defaultSmppSession != null) {
			this.defaultSmppSession.disableLogPdu();
		}
	}

	@Override
	public String[] dumpWindow() {
		if (this.defaultSmppSession != null) {
			return this.defaultSmppSession.dumpWindow();
		}

		return null;
	}

	@Override
	public void enableLogBytes() {
		if (this.defaultSmppSession != null) {
			this.defaultSmppSession.enableLogBytes();
		}
	}

	@Override
	public void enableLogPdu() {
		if (this.defaultSmppSession != null) {
			this.defaultSmppSession.enableLogPdu();
		}
	}

	@Override
	public String getBindTypeName() {
		if (this.defaultSmppSession != null) {
			return this.defaultSmppSession.getBindTypeName();
		}
		return null;
	}

	@Override
	public String getBoundDuration() {
		if (this.defaultSmppSession != null) {
			return this.defaultSmppSession.getBoundDuration();
		}
		return null;
	}

	@Override
	public String getInterfaceVersionName() {
		if (this.defaultSmppSession != null) {
			return this.defaultSmppSession.getInterfaceVersionName();
		}
		return null;
	}

	@Override
	public String getLocalAddressAndPort() {
		if (this.defaultSmppSession != null) {
			return this.defaultSmppSession.getLocalAddressAndPort();
		}
		return null;
	}

	@Override
	public String getLocalTypeName() {
		if (this.defaultSmppSession != null) {
			return this.defaultSmppSession.getLocalTypeName();
		}
		return this.smppSessionType.toString();
	}

	@Override
	public int getMaxWindowSize() {
		if (this.defaultSmppSession != null) {
			return this.defaultSmppSession.getMaxWindowSize();
		}
		return 0;
	}

	@Override
	public int getNextSequenceNumber() {
		if (this.defaultSmppSession != null) {
			return this.defaultSmppSession.getNextSequenceNumber();
		}
		return 0;
	}

	@Override
	public String getRemoteAddressAndPort() {
		if (this.defaultSmppSession != null) {
			return this.defaultSmppSession.getRemoteAddressAndPort();
		}
		return null;
	}

	@Override
	public String getRemoteTypeName() {
		if (this.defaultSmppSession != null) {
			return this.defaultSmppSession.getRemoteTypeName();
		}
		return null;
	}

	@Override
	public String getRxDataSMCounter() {
		if (this.defaultSmppSession != null) {
			return this.defaultSmppSession.getRxDataSMCounter();
		}
		return null;
	}

	@Override
	public String getRxDeliverSMCounter() {
		if (this.defaultSmppSession != null) {
			return this.defaultSmppSession.getRxDeliverSMCounter();
		}
		return null;
	}

	@Override
	public String getRxEnquireLinkCounter() {
		if (this.defaultSmppSession != null) {
			return this.defaultSmppSession.getRxEnquireLinkCounter();
		}
		return null;
	}

	@Override
	public String getRxSubmitSMCounter() {
		if (this.defaultSmppSession != null) {
			return this.defaultSmppSession.getRxSubmitSMCounter();
		}
		return null;
	}

	@Override
	public String getStateName() {
		if (this.defaultSmppSession != null) {
			return this.defaultSmppSession.getStateName();
		}
		return this.state;
	}

	@Override
	public String getTxDataSMCounter() {
		if (this.defaultSmppSession != null) {
			return this.defaultSmppSession.getTxDataSMCounter();
		}
		return null;
	}

	@Override
	public String getTxDeliverSMCounter() {
		if (this.defaultSmppSession != null) {
			return this.defaultSmppSession.getTxDeliverSMCounter();
		}
		return null;
	}

	@Override
	public String getTxEnquireLinkCounter() {
		if (this.defaultSmppSession != null) {
			return this.defaultSmppSession.getTxEnquireLinkCounter();
		}
		return null;
	}

	@Override
	public String getTxSubmitSMCounter() {
		if (this.defaultSmppSession != null) {
			return this.defaultSmppSession.getTxSubmitSMCounter();
		}
		return null;
	}

	@Override
	public boolean isBinding() {
		if (this.defaultSmppSession != null) {
			return this.defaultSmppSession.isBinding();
		}
		return false;
	}

	@Override
	public boolean isBound() {
		if (this.defaultSmppSession != null) {
			return this.defaultSmppSession.isBound();
		}
		return false;
	}

	@Override
	public boolean isClosed() {
		if (this.defaultSmppSession != null) {
			return this.defaultSmppSession.isClosed();
		}
		return true;
	}

	@Override
	public boolean isOpen() {
		if (this.defaultSmppSession != null) {
			return this.defaultSmppSession.isOpen();
		}
		return false;
	}

	@Override
	public boolean isUnbinding() {
		if (this.defaultSmppSession != null) {
			return this.defaultSmppSession.isUnbinding();
		}
		return false;
	}

	@Override
	public boolean isWindowMonitorEnabled() {
		if (this.defaultSmppSession != null) {
			return this.defaultSmppSession.isWindowMonitorEnabled();
		}
		return false;
	}

	@Override
	public void resetCounters() {
		if (this.defaultSmppSession != null) {
			this.defaultSmppSession.resetCounters();
		}
	}

	@Override
	public void unbind(long arg0) {
		if (this.defaultSmppSession != null) {
			this.defaultSmppSession.unbind(arg0);
		}

	}
}
