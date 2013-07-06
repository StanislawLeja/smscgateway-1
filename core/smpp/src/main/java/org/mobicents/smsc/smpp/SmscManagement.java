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
import java.io.FileNotFoundException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;

import javolution.text.TextBuilder;
import javolution.xml.XMLBinding;

import org.apache.log4j.Logger;
import org.jboss.mx.util.MBeanServerLocator;

/**
 * @author Amit Bhayani
 * 
 */
public class SmscManagement implements SmscManagementMBean {
	private static final Logger logger = Logger.getLogger(SmscManagement.class);

	public static final String JMX_DOMAIN = "com.telscale.smsc";
	public static final String JMX_LAYER_SMSC_MANAGEMENT = "SmscManagement";
    public static final String JMX_LAYER_ESME_MANAGEMENT = "EsmeManagement";
    public static final String JMX_LAYER_ARCHIVE_SMS = "ArchiveSms";
	public static final String JMX_LAYER_SMSC_PROPERTIES_MANAGEMENT = "SmscPropertiesManagement";
	public static final String JMX_LAYER_SMPP_SERVER_MANAGEMENT = "SmppServerManagement";
	public static final String JMX_LAYER_SMPP_CLIENT_MANAGEMENT = "SmppClientManagement";

	protected static final String SMSC_PERSIST_DIR_KEY = "smsc.persist.dir";
	protected static final String USER_DIR_KEY = "user.dir";

	private static final String PERSIST_FILE_NAME = "smsc.xml";

	private static final XMLBinding binding = new XMLBinding();
	private static final String TAB_INDENT = "\t";
	private static final String CLASS_ATTRIBUTE = "type";

	private final TextBuilder persistFile = TextBuilder.newInstance();

	private final String name;

	private String persistDir = null;

	private SmppServerManagement smppServerManagement = null;
	private SmppClientManagement smppClientManagement = null;

	private ThreadPoolExecutor executor = null;
	private ScheduledThreadPoolExecutor monitorExecutor = null;

	private EsmeManagement esmeManagement = null;
	private SmscPropertiesManagement smscPropertiesManagement = null;
	private ArchiveSms archiveSms;

	private MBeanServer mbeanServer = null;

	private String smsRoutingRuleClass;

	private SmppSessionHandlerInterface smppSessionHandlerInterface = null;

	private boolean isStarted = false;

	private static SmscManagement instance = null;

	private SmscManagement(String name) {
		this.name = name;
		binding.setClassAttribute(CLASS_ATTRIBUTE);
		binding.setAlias(Esme.class, "esme");
	}

	public static SmscManagement getInstance(String name) {
		if (instance == null) {
			instance = new SmscManagement(name);
		}
		return instance;
	}

	public static SmscManagement getInstance() {
		return instance;
	}

	public String getName() {
		return name;
	}

	public void setSmppSessionHandlerInterface(SmppSessionHandlerInterface smppSessionHandlerInterface) {
		this.smppSessionHandlerInterface = smppSessionHandlerInterface;
	}

	public String getPersistDir() {
		return persistDir;
	}

	public void setPersistDir(String persistDir) {
		this.persistDir = persistDir;
	}

	public SmppServerManagement getSmppServerManagement() {
		return smppServerManagement;
	}

    public EsmeManagement getEsmeManagement() {
        return esmeManagement;
    }

    public ArchiveSms getArchiveSms() {
        return archiveSms;
    }

	/**
	 * @return the smsRoutingRuleClass
	 */
	public String getSmsRoutingRuleClass() {
		return smsRoutingRuleClass;
	}

	/**
	 * @param smsRoutingRuleClass
	 *            the smsRoutingRuleClass to set
	 */
	public void setSmsRoutingRuleClass(String smsRoutingRuleClass) {
		this.smsRoutingRuleClass = smsRoutingRuleClass;
	}

	public void startSmscManagement() throws Exception {

		// Step 1 Get the MBeanServer
		this.mbeanServer = MBeanServerLocator.locateJBoss();

		// Step 2 Setup ESME
		this.esmeManagement = EsmeManagement.getInstance(this.name);
		this.esmeManagement.setPersistDir(this.persistDir);
		this.esmeManagement.start();

        // Step 3 Setup SMSC Properties
        this.smscPropertiesManagement = SmscPropertiesManagement.getInstance(this.name);
        this.smscPropertiesManagement.setPersistDir(this.persistDir);
        this.smscPropertiesManagement.start();

		// Step 4 Set Routing Rule class
		SmsRoutingRule smsRoutingRule = null;
		if (this.smsRoutingRuleClass != null) {
			smsRoutingRule = (SmsRoutingRule) Class.forName(this.smsRoutingRuleClass).newInstance();
		} else {
			smsRoutingRule = new DefaultSmsRoutingRule();
		}
        smsRoutingRule.setEsmeManagement(esmeManagement);
        smsRoutingRule.setSmscPropertiesManagement(smscPropertiesManagement);
		SmsRouteManagement.getInstance().setSmsRoutingRule(smsRoutingRule);

        // Step 5 Setup ArchiveSms
        this.archiveSms = ArchiveSms.getInstance(this.name);
        this.archiveSms.start();

        ObjectName esmeObjNname = new ObjectName(SmscManagement.JMX_DOMAIN + ":layer=" + JMX_LAYER_ESME_MANAGEMENT + ",name=" + this.getName());
        this.registerMBean(this.esmeManagement, EsmeManagementMBean.class, false, esmeObjNname);

        ObjectName smscObjNname = new ObjectName(SmscManagement.JMX_DOMAIN + ":layer=" + JMX_LAYER_SMSC_PROPERTIES_MANAGEMENT + ",name=" + this.getName());
        this.registerMBean(this.smscPropertiesManagement, SmscPropertiesManagementMBean.class, true, smscObjNname);

        ObjectName arhiveObjNname = new ObjectName(SmscManagement.JMX_DOMAIN + ":layer=" + JMX_LAYER_ARCHIVE_SMS + ",name=" + this.getName());
        this.registerMBean(this.archiveSms, ArchiveSmsMBean.class, false, arhiveObjNname);

		this.persistFile.clear();

		if (persistDir != null) {
			this.persistFile.append(persistDir).append(File.separator).append(this.name).append("_")
					.append(PERSIST_FILE_NAME);
		} else {
			persistFile.append(System.getProperty(SMSC_PERSIST_DIR_KEY, System.getProperty(USER_DIR_KEY)))
					.append(File.separator).append(this.name).append("_").append(PERSIST_FILE_NAME);
		}

		logger.info(String.format("SMSC configuration file path %s", persistFile.toString()));

		try {
			this.load();
		} catch (FileNotFoundException e) {
			logger.warn(String.format("Failed to load the SS7 configuration file. \n%s", e.getMessage()));
		}

		// Step 5. Get DefaultSmppServerHandler

		// for monitoring thread use, it's preferable to create your own
		// instance of an executor and cast it to a ThreadPoolExecutor from
		// Executors.newCachedThreadPool() this permits exposing thinks like
		// executor.getActiveCount() via JMX possible no point renaming the
		// threads in a factory since underlying Netty framework does not easily
		// allow you to customize your thread names
		this.executor = (ThreadPoolExecutor) Executors.newCachedThreadPool();

		// to enable automatic expiration of requests, a second scheduled
		// executor
		// is required which is what a monitor task will be executed with - this
		// is probably a thread pool that can be shared with between all client
		// bootstraps
		this.monitorExecutor = (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(1, new ThreadFactory() {
			private AtomicInteger sequence = new AtomicInteger(0);

			@Override
			public Thread newThread(Runnable r) {
				Thread t = new Thread(r);
				t.setName("SmppSessionWindowMonitorPool-" + sequence.getAndIncrement());
				return t;
			}
		});

		// Step 6 Start SMPP Server
		this.smppServerManagement = new SmppServerManagement(this.name, this.esmeManagement,
				this.smppSessionHandlerInterface, this.executor, this.monitorExecutor);
		this.smppServerManagement.setPersistDir(this.persistDir);
		this.smppServerManagement.start();

		ObjectName smppServerManaObjName = new ObjectName(SmscManagement.JMX_DOMAIN + ":layer="
				+ JMX_LAYER_SMPP_SERVER_MANAGEMENT + ",name=" + this.getName());
		this.registerMBean(this.smppServerManagement, SmppServerManagementMBean.class, true, smppServerManaObjName);

		// Ste 7 Start SMPP Clients
		this.smppClientManagement = new SmppClientManagement(this.name, this.esmeManagement,
				this.smppSessionHandlerInterface, this.executor, this.monitorExecutor);

		this.esmeManagement.setSmppClient(this.smppClientManagement);
		this.smppClientManagement.start();

		ObjectName smppClientManaObjName = new ObjectName(SmscManagement.JMX_DOMAIN + ":layer="
				+ JMX_LAYER_SMPP_CLIENT_MANAGEMENT + ",name=" + this.getName());
		this.registerMBean(this.smppClientManagement, SmppClientManagementMBean.class, true, smppClientManaObjName);

		this.isStarted = true;
		logger.info("Started SmscManagement");
	}

	public void stopSmscManagement() throws Exception {
        this.archiveSms.stop();
        ObjectName arhiveObjNname = new ObjectName(SmscManagement.JMX_DOMAIN + ":layer=" + JMX_LAYER_ARCHIVE_SMS + ",name=" + this.getName());
        this.unregisterMbean(arhiveObjNname);

        this.esmeManagement.stop();
		ObjectName esmeObjNname = new ObjectName(SmscManagement.JMX_DOMAIN + ":layer=" + JMX_LAYER_ESME_MANAGEMENT
				+ ",name=" + this.getName());
		this.unregisterMbean(esmeObjNname);

		this.smscPropertiesManagement.stop();
		ObjectName smscObjNname = new ObjectName(SmscManagement.JMX_DOMAIN + ":layer="
				+ JMX_LAYER_SMSC_PROPERTIES_MANAGEMENT + ",name=" + this.getName());
		this.unregisterMbean(smscObjNname);

		this.smppServerManagement.stop();
		ObjectName smppServerManaObjName = new ObjectName(SmscManagement.JMX_DOMAIN + ":layer="
				+ JMX_LAYER_SMPP_SERVER_MANAGEMENT + ",name=" + this.getName());
		this.unregisterMbean(smppServerManaObjName);

		this.smppClientManagement.stop();
		ObjectName smppClientManaObjName = new ObjectName(SmscManagement.JMX_DOMAIN + ":layer="
				+ JMX_LAYER_SMPP_CLIENT_MANAGEMENT + ",name=" + this.getName());
		this.unregisterMbean(smppClientManaObjName);

		this.executor.shutdownNow();
		this.monitorExecutor.shutdownNow();

		this.isStarted = false;
		
		this.store();
	}

	/**
	 * Persist
	 */
	public void store() {

	}

	/**
	 * Load and create LinkSets and Link from persisted file
	 * 
	 * @throws Exception
	 */
	public void load() throws FileNotFoundException {

	}

	@Override
	public boolean isStarted() {
		return this.isStarted;
	}

	protected <T> void registerMBean(T implementation, Class<T> mbeanInterface, boolean isMXBean, ObjectName name) {
		try {
			this.mbeanServer.registerMBean(implementation, name);
		} catch (InstanceAlreadyExistsException e) {
			logger.error(String.format("Error while registering MBean %s", mbeanInterface.getName()), e);
		} catch (MBeanRegistrationException e) {
			logger.error(String.format("Error while registering MBean %s", mbeanInterface.getName()), e);
		} catch (NotCompliantMBeanException e) {
			logger.error(String.format("Error while registering MBean %s", mbeanInterface.getName()), e);
		}
	}

	protected void unregisterMbean(ObjectName name) {

		try {
			this.mbeanServer.unregisterMBean(name);
		} catch (MBeanRegistrationException e) {
			logger.error(String.format("Error while unregistering MBean %s", name), e);
		} catch (InstanceNotFoundException e) {
			logger.error(String.format("Error while unregistering MBean %s", name), e);
		}
	}
}
