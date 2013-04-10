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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;

import javolution.text.TextBuilder;
import javolution.xml.XMLBinding;
import javolution.xml.XMLObjectReader;
import javolution.xml.XMLObjectWriter;
import javolution.xml.stream.XMLStreamException;

import org.apache.log4j.Logger;

import com.cloudhopper.smpp.SmppConstants;
import com.cloudhopper.smpp.SmppServerConfiguration;
import com.cloudhopper.smpp.impl.DefaultSmppServer;

/**
 * 
 * @author Amit Bhayani
 * 
 */
public class SmppServerManagement implements SmppServerManagementMBean {

	private static final Logger logger = Logger.getLogger(SmppServerManagement.class);

	private static final String PORT = "port";
	private static final String BIND_TIMEOUT = "bindTimeout";
	private static final String SYSTEM_ID = "systemId";
	private static final String AUTO_NEGOTIATION_VERSION = "autoNegotiateInterfaceVersion";
	private static final String INTERFACE_VERSION = "interfaceVersion";
	private static final String MAX_CONNECTION_SIZE = "maxConnectionSize";
	private static final String DEFAULT_WINDOW_SIZE = "defaultWindowSize";
	private static final String DEFAULT_WINDOW_WAIT_TIMEOUT = "defaultWindowWaitTimeout";
	private static final String DEFAULT_REQUEST_EXPIRY_TIMEOUT = "defaultRequestExpiryTimeout";
	private static final String DEFAULT_WINDOW_MONITOR_INTERVAL = "defaultWindowMonitorInterval";
	private static final String DEFAULT_SESSION_COUNTERS_ENABLED = "defaultSessionCountersEnabled";

	private static final String TAB_INDENT = "\t";
	private static final String CLASS_ATTRIBUTE = "type";
	private static final XMLBinding binding = new XMLBinding();
	private static final String PERSIST_FILE_NAME = "smppserver.xml";

	private final TextBuilder persistFile = TextBuilder.newInstance();

	private String persistDir = null;

	final private String name;
	private final ThreadPoolExecutor executor;
	private final ScheduledThreadPoolExecutor monitorExecutor;
	private final EsmeManagement esmeManagement;

	private int port = 2776;
	// length of time to wait for a bind request
	private long bindTimeout = 5000;
	private String systemId = "TelscaleSMSC";
	// if true, <= 3.3 for interface version normalizes to version 3.3
	// if true, >= 3.4 for interface version normalizes to version 3.4 and
	// optional sc_interface_version is set to 3.4
	private boolean autoNegotiateInterfaceVersion = true;
	// smpp version the server supports
	private double interfaceVersion = 3.4;
	// max number of connections/sessions this server will expect to handle
	// this number corrosponds to the number of worker threads handling reading
	// data from sockets and the thread things will be processed under
	private int maxConnectionSize = SmppConstants.DEFAULT_SERVER_MAX_CONNECTION_SIZE;

	private int defaultWindowSize = 100;
	private long defaultWindowWaitTimeout = 30000;
	private long defaultRequestExpiryTimeout = 30000;
	private long defaultWindowMonitorInterval = 15000;
	private boolean defaultSessionCountersEnabled = true;

	private DefaultSmppServer defaultSmppServer = null;

	private SmppSessionHandlerInterface smppSessionHandlerInterface;

	public SmppServerManagement(String name, EsmeManagement esmeManagement,
			SmppSessionHandlerInterface smppSessionHandlerInterface, ThreadPoolExecutor executor,
			ScheduledThreadPoolExecutor monitorExecutor) {
		this.name = name;
		this.executor = executor;
		this.monitorExecutor = monitorExecutor;
		this.esmeManagement = esmeManagement;
		this.smppSessionHandlerInterface = smppSessionHandlerInterface;

		binding.setClassAttribute(CLASS_ATTRIBUTE);
	}

	public String getName() {
		return name;
	}

	public String getPersistDir() {
		return persistDir;
	}

	public void setPersistDir(String persistDir) {
		this.persistDir = persistDir;
	}

	public void setBindTimeout(long bindTimeout) {
		this.bindTimeout = bindTimeout;
		this.store();
	}

	public void setSystemId(String systemId) {
		this.systemId = systemId;
		this.store();
	}

	public void setAutoNegotiateInterfaceVersion(boolean autoNegotiateInterfaceVersion) {
		this.autoNegotiateInterfaceVersion = autoNegotiateInterfaceVersion;
		this.store();
	}

	public void setInterfaceVersion(double interfaceVersion) {
		this.interfaceVersion = interfaceVersion;
		this.store();
	}

	public void setMaxConnectionSize(int maxConnectionSize) {
		this.maxConnectionSize = maxConnectionSize;
		this.store();
	}

	public void setDefaultWindowSize(int defaultWindowSize) {
		this.defaultWindowSize = defaultWindowSize;
		this.store();
	}

	public void setDefaultWindowWaitTimeout(long defaultWindowWaitTimeout) {
		this.defaultWindowWaitTimeout = defaultWindowWaitTimeout;
		this.store();
	}

	public void setDefaultRequestExpiryTimeout(long defaultRequestExpiryTimeout) {
		this.defaultRequestExpiryTimeout = defaultRequestExpiryTimeout;
		this.store();
	}

	public void setDefaultWindowMonitorInterval(long defaultWindowMonitorInterval) {
		this.defaultWindowMonitorInterval = defaultWindowMonitorInterval;
		this.store();
	}

	public void setDefaultSessionCountersEnabled(boolean defaultSessionCountersEnabled) {
		this.defaultSessionCountersEnabled = defaultSessionCountersEnabled;
		this.store();
	}

	public void start() throws Exception {

		if (this.smppSessionHandlerInterface == null) {
			throw new Exception("SmppSessionHandlerInterface is not set!");
		}

		this.persistFile.clear();

		if (persistDir != null) {
			this.persistFile.append(persistDir).append(File.separator).append(this.name).append("_")
					.append(PERSIST_FILE_NAME);
		} else {
			persistFile
					.append(System.getProperty(SmscManagement.SMSC_PERSIST_DIR_KEY,
							System.getProperty(SmscManagement.USER_DIR_KEY))).append(File.separator).append(this.name)
					.append("_").append(PERSIST_FILE_NAME);
		}

		logger.info(String.format("Loading SMPP Server Properties from %s", persistFile.toString()));

		try {
			this.load();
		} catch (FileNotFoundException e) {
			logger.warn(String.format("Failed to load the SMSC configuration file. \n%s", e.getMessage()));
		}

		// create a server configuration
		SmppServerConfiguration configuration = new SmppServerConfiguration();
		configuration.setName(this.name);
		configuration.setPort(this.port);
		configuration.setBindTimeout(this.bindTimeout);
		configuration.setSystemId(this.systemId);
		configuration.setAutoNegotiateInterfaceVersion(this.autoNegotiateInterfaceVersion);
		if (this.interfaceVersion == 3.4) {
			configuration.setInterfaceVersion(SmppConstants.VERSION_3_4);
		} else if (this.interfaceVersion == 3.3) {
			configuration.setInterfaceVersion(SmppConstants.VERSION_3_3);
		}
		configuration.setMaxConnectionSize(this.maxConnectionSize);
		configuration.setNonBlockingSocketsEnabled(true);

		// SMPP Request sent would wait for 30000 milli seconds before throwing
		// exception
		configuration.setDefaultRequestExpiryTimeout(this.defaultRequestExpiryTimeout);
		configuration.setDefaultWindowMonitorInterval(this.defaultWindowMonitorInterval);

		// The "window" is the amount of unacknowledged requests that are
		// permitted to be outstanding/unacknowledged at any given time.
		configuration.setDefaultWindowSize(this.defaultWindowSize);

		// Set the amount of time to wait until a slot opens up in the
		// sendWindow.
		configuration.setDefaultWindowWaitTimeout(this.defaultWindowWaitTimeout);
		configuration.setDefaultSessionCountersEnabled(this.defaultSessionCountersEnabled);

		// We bind to JBoss MBean
		configuration.setJmxEnabled(false);

		// create a server, start it up
		this.defaultSmppServer = new DefaultSmppServer(configuration, new DefaultSmppServerHandler(esmeManagement,
				this.smppSessionHandlerInterface), executor, monitorExecutor);
		logger.info("Starting SMPP server...");
		this.defaultSmppServer.start();
		logger.info("SMPP server started");

	}

	public void stop() throws Exception {
		logger.info("Stopping SMPP server...");
		this.defaultSmppServer.stop();
		logger.info("SMPP server stopped");
		logger.info(String.format("Server counters: %s", this.defaultSmppServer.getCounters()));

		this.store();
	}

	@Override
	public int getBindPort() {
		return this.port;
	}

	@Override
	public void setBindPort(int port) {
		this.port = port;
		this.store();
	}

	@Override
	public long getBindTimeout() {
		return this.bindTimeout;
	}

	@Override
	public String getSystemId() {
		return this.systemId;
	}

	@Override
	public boolean isAutoNegotiateInterfaceVersion() {
		return this.autoNegotiateInterfaceVersion;
	}

	@Override
	public double getInterfaceVersion() {
		return this.interfaceVersion;
	}

	@Override
	public int getMaxConnectionSize() {
		return this.maxConnectionSize;
	}

	@Override
	public int getDefaultWindowSize() {
		return this.defaultWindowSize;
	}

	@Override
	public long getDefaultWindowWaitTimeout() {
		return this.defaultWindowWaitTimeout;
	}

	@Override
	public long getDefaultRequestExpiryTimeout() {
		return this.defaultRequestExpiryTimeout;
	}

	@Override
	public long getDefaultWindowMonitorInterval() {
		return this.defaultWindowMonitorInterval;
	}

	@Override
	public boolean isDefaultSessionCountersEnabled() {
		return this.defaultSessionCountersEnabled;
	}

	@Override
	public boolean isStarted() {
		return (this.defaultSmppServer != null && this.defaultSmppServer.isStarted());
	}

	@Override
	public void resetCounters() {
		if (this.defaultSmppServer != null) {
			this.defaultSmppServer.resetCounters();
		}
	}

	@Override
	public int getSessionSize() {
		if (this.defaultSmppServer != null) {
			return this.defaultSmppServer.getSessionSize();
		}
		return 0;
	}

	@Override
	public int getTransceiverSessionSize() {
		if (this.defaultSmppServer != null) {
			return this.defaultSmppServer.getTransceiverSessionSize();
		}
		return 0;
	}

	@Override
	public int getTransmitterSessionSize() {
		if (this.defaultSmppServer != null) {
			return this.defaultSmppServer.getTransmitterSessionSize();
		}
		return 0;
	}

	@Override
	public int getReceiverSessionSize() {
		if (this.defaultSmppServer != null) {
			return this.defaultSmppServer.getReceiverSessionSize();
		}
		return 0;
	}

	@Override
	public int getConnectionSize() {
		if (this.defaultSmppServer != null) {
			return this.defaultSmppServer.getConnectionSize();
		}
		return 0;
	}

	@Override
	public boolean isNonBlockingSocketsEnabled() {
		if (this.defaultSmppServer != null) {
			return this.defaultSmppServer.isNonBlockingSocketsEnabled();
		}
		return false;
	}

	@Override
	public boolean isReuseAddress() {
		if (this.defaultSmppServer != null) {
			return this.defaultSmppServer.isReuseAddress();
		}
		return false;
	}

	@Override
	public int getChannelConnects() {
		if (this.defaultSmppServer != null) {
			return this.defaultSmppServer.getChannelConnects();
		}
		return 0;
	}

	@Override
	public int getChannelDisconnects() {
		if (this.defaultSmppServer != null) {
			return this.defaultSmppServer.getChannelDisconnects();
		}
		return 0;
	}

	@Override
	public int getBindTimeouts() {
		if (this.defaultSmppServer != null) {
			return this.defaultSmppServer.getBindTimeouts();
		}
		return 0;
	}

	@Override
	public int getBindRequested() {
		if (this.defaultSmppServer != null) {
			return this.defaultSmppServer.getBindRequested();
		}
		return 0;
	}

	@Override
	public int getSessionCreated() {
		if (this.defaultSmppServer != null) {
			return this.defaultSmppServer.getSessionCreated();
		}
		return 0;
	}

	@Override
	public int getSessionDestroyed() {
		if (this.defaultSmppServer != null) {
			return this.defaultSmppServer.getSessionDestroyed();
		}
		return 0;
	}

	/**
	 * Persist
	 */
	public void store() {

		// TODO : Should we keep reference to Objects rather than recreating
		// everytime?
		try {
			XMLObjectWriter writer = XMLObjectWriter.newInstance(new FileOutputStream(persistFile.toString()));
			writer.setBinding(binding);
			// Enables cross-references.
			// writer.setReferenceResolver(new XMLReferenceResolver());
			writer.setIndentation(TAB_INDENT);

			writer.write(this.port, PORT, Integer.class);
			writer.write(this.bindTimeout, BIND_TIMEOUT, Long.class);
			writer.write(this.systemId, SYSTEM_ID, String.class);
			writer.write(this.autoNegotiateInterfaceVersion, AUTO_NEGOTIATION_VERSION, Boolean.class);
			writer.write(this.interfaceVersion, INTERFACE_VERSION, Double.class);
			writer.write(this.maxConnectionSize, MAX_CONNECTION_SIZE, Integer.class);
			writer.write(this.defaultWindowSize, DEFAULT_WINDOW_SIZE, Integer.class);
			writer.write(this.defaultWindowWaitTimeout, DEFAULT_WINDOW_WAIT_TIMEOUT, Long.class);
			writer.write(this.defaultRequestExpiryTimeout, DEFAULT_REQUEST_EXPIRY_TIMEOUT, Long.class);
			writer.write(this.defaultWindowMonitorInterval, DEFAULT_WINDOW_MONITOR_INTERVAL, Long.class);
			writer.write(this.defaultSessionCountersEnabled, DEFAULT_SESSION_COUNTERS_ENABLED, Boolean.class);

			writer.close();
		} catch (Exception e) {
			logger.error("Error while persisting the Rule state in file", e);
		}
	}

	/**
	 * Load and create LinkSets and Link from persisted file
	 * 
	 * @throws Exception
	 */
	public void load() throws FileNotFoundException {

		XMLObjectReader reader = null;
		try {
			reader = XMLObjectReader.newInstance(new FileInputStream(persistFile.toString()));

			reader.setBinding(binding);
			this.port = reader.read(PORT, Integer.class);
			this.bindTimeout = reader.read(BIND_TIMEOUT, Long.class);
			this.systemId = reader.read(SYSTEM_ID, String.class);
			this.autoNegotiateInterfaceVersion = reader.read(AUTO_NEGOTIATION_VERSION, Boolean.class);
			this.interfaceVersion = reader.read(INTERFACE_VERSION, Double.class);
			this.maxConnectionSize = reader.read(MAX_CONNECTION_SIZE, Integer.class);
			this.defaultWindowSize = reader.read(DEFAULT_WINDOW_SIZE, Integer.class);
			this.defaultWindowWaitTimeout = reader.read(DEFAULT_WINDOW_WAIT_TIMEOUT, Integer.class);
			this.defaultRequestExpiryTimeout = reader.read(DEFAULT_REQUEST_EXPIRY_TIMEOUT, Integer.class);
			this.defaultWindowMonitorInterval = reader.read(DEFAULT_WINDOW_MONITOR_INTERVAL, Integer.class);
			this.defaultSessionCountersEnabled = reader.read(DEFAULT_SESSION_COUNTERS_ENABLED, Boolean.class);

			reader.close();
		} catch (XMLStreamException ex) {
			// this.logger.info(
			// "Error while re-creating Linksets from persisted file", ex);
		}
	}
}
