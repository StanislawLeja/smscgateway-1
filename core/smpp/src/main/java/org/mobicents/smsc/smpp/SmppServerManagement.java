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

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;

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

	final private String name;
	private final ThreadPoolExecutor executor;
	private final ScheduledThreadPoolExecutor monitorExecutor;
	private final EsmeManagement esmeManagement;

	private int port = 2775;
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

	private int defaultWindowSize = SmppConstants.DEFAULT_WINDOW_SIZE;
	private long defaultWindowWaitTimeout = SmppConstants.DEFAULT_WINDOW_WAIT_TIMEOUT;
	private long defaultRequestExpiryTimeout = SmppConstants.DEFAULT_REQUEST_EXPIRY_TIMEOUT;
	private long defaultWindowMonitorInterval = SmppConstants.DEFAULT_WINDOW_MONITOR_INTERVAL;
	private boolean defaultSessionCountersEnabled = false;

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
	}

	public String getName() {
		return name;
	}

	public void setBindTimeout(long bindTimeout) {
		this.bindTimeout = bindTimeout;
	}

	public void setSystemId(String systemId) {
		this.systemId = systemId;
	}

	public void setAutoNegotiateInterfaceVersion(boolean autoNegotiateInterfaceVersion) {
		this.autoNegotiateInterfaceVersion = autoNegotiateInterfaceVersion;
	}

	public void setInterfaceVersion(double interfaceVersion) {
		this.interfaceVersion = interfaceVersion;
	}

	public void setMaxConnectionSize(int maxConnectionSize) {
		this.maxConnectionSize = maxConnectionSize;
	}

	public void setDefaultWindowSize(int defaultWindowSize) {
		this.defaultWindowSize = defaultWindowSize;
	}

	public void setDefaultWindowWaitTimeout(long defaultWindowWaitTimeout) {
		this.defaultWindowWaitTimeout = defaultWindowWaitTimeout;
	}

	public void setDefaultRequestExpiryTimeout(long defaultRequestExpiryTimeout) {
		this.defaultRequestExpiryTimeout = defaultRequestExpiryTimeout;
	}

	public void setDefaultWindowMonitorInterval(long defaultWindowMonitorInterval) {
		this.defaultWindowMonitorInterval = defaultWindowMonitorInterval;
	}

	public void setDefaultSessionCountersEnabled(boolean defaultSessionCountersEnabled) {
		this.defaultSessionCountersEnabled = defaultSessionCountersEnabled;
	}

	public void start() throws Exception {

		if (this.smppSessionHandlerInterface == null) {
			throw new Exception("SmppSessionHandlerInterface is not set!");
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
		this.defaultSmppServer.destroy();
		logger.info("SMPP server stopped");
		logger.info(String.format("Server counters: %s", this.defaultSmppServer.getCounters()));
	}

	@Override
	public int getBindPort() {
		return this.port;
	}

	@Override
	public void setBindPort(int port) {
		this.port = port;
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
}
