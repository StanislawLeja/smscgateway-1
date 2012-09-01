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
package org.mobicents.smsc.smpp;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.StandardMBean;

import org.apache.log4j.Logger;
import org.jboss.mx.util.MBeanServerLocator;

import com.cloudhopper.smpp.SmppConstants;
import com.cloudhopper.smpp.SmppServerConfiguration;
import com.cloudhopper.smpp.SmppServerHandler;
import com.cloudhopper.smpp.impl.DefaultSmppServer;
import com.cloudhopper.smpp.jmx.DefaultSmppServerMXBean;
import com.cloudhopper.smpp.type.SmppChannelException;

public class SmppServer {
	
	private static final Logger logger = Logger.getLogger(SmppServer.class);

	private String name = "SmppServer";
	private int port = 2775;
	// length of time to wait for a bind request
	private long bindTimeout = 5000;
	private String systemId = "MobicentsSMSC";
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

	private SmppServerHandler smppServerHandler = null;

	private MBeanServer mbeanServer = null;
	
	private EsmeManagement esmeManagement = null;

	public SmppServer() {

	}

	public void setEsmeManagement(EsmeManagement esmeManagement) {
		this.esmeManagement = esmeManagement;
		((DefaultSmppServerHandler)this.smppServerHandler).setEsmeManagement(esmeManagement);
	}

	public void setName(String name) {
		this.name = name;
	}

	protected String getName() {
		return name;
	}

	public void setPort(int port) {
		this.port = port;
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

	public void setInterfaceVersion(double interfaceVersion) throws Exception {
		if (interfaceVersion != 3.4 && interfaceVersion != 3.3) {
			throw new Exception("Only SMPP version 3.4 or 3.3 is supported");
		}
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

	public void start() throws SmppChannelException {
		// for monitoring thread use, it's preferable to create your own
		// instance of an executor and cast it to a ThreadPoolExecutor from
		// Executors.newCachedThreadPool() this permits exposing thinks like
		// executor.getActiveCount() via JMX possible no point renaming the
		// threads in a factory since underlying Netty framework does not easily
		// allow you to customize your thread names
		ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newCachedThreadPool();

		// to enable automatic expiration of requests, a second scheduled
		// executor
		// is required which is what a monitor task will be executed with - this
		// is probably a thread pool that can be shared with between all client
		// bootstraps
		ScheduledThreadPoolExecutor monitorExecutor = (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(1,
				new ThreadFactory() {
					private AtomicInteger sequence = new AtomicInteger(0);

					@Override
					public Thread newThread(Runnable r) {
						Thread t = new Thread(r);
						t.setName("SmppServerSessionWindowMonitorPool-" + sequence.getAndIncrement());
						return t;
					}
				});

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

		this.smppServerHandler = new DefaultSmppServerHandler();

		// create a server, start it up
		this.defaultSmppServer = new DefaultSmppServer(configuration, smppServerHandler, executor, monitorExecutor);
		this.registerMBean();
		logger.info("Starting SMPP server...");
		this.defaultSmppServer.start();
		logger.info("SMPP server started");
	}

	public void stop() {
		logger.info("Stopping SMPP server...");
		this.defaultSmppServer.stop();
		logger.info("SMPP server stopped");
		logger.info(String.format("Server counters: %s", this.defaultSmppServer.getCounters()));
	}

	public void destroy() {
		this.unregisterMBean();
	}

	private void registerMBean() {

		try {
			this.mbeanServer = MBeanServerLocator.locateJBoss();
			ObjectName name = new ObjectName(SmscManagement.JMX_DOMAIN + ":name=" + this.name);
			final StandardMBean mxBean = new StandardMBean(this.defaultSmppServer, DefaultSmppServerMXBean.class, true);
			this.mbeanServer.registerMBean(mxBean, name);

		} catch (Exception e) {
			// log the error, but don't throw an exception for this datasource
			logger.error(String.format("Unable to register DefaultSmppServerMXBean %s", this.name), e);
		}
	}

	private void unregisterMBean() {
		try {
			if (this.mbeanServer != null) {
				ObjectName name = new ObjectName(SmscManagement.JMX_DOMAIN + ":name=" + this.name);
				this.mbeanServer.unregisterMBean(name);
			}
		} catch (Exception e) {
			logger.error(String.format("Unable to unregister DefaultSmppServerMXBean %s", this.name), e);
		}
	}

	public DefaultSmppServerHandler getDefaultSmppServerHandler() {
		return (DefaultSmppServerHandler) smppServerHandler;
	}

}
