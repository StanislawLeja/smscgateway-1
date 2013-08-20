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
import java.util.List;

import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import javax.management.StandardMBean;

import javolution.text.TextBuilder;
import javolution.util.FastList;
import javolution.util.FastMap;
import javolution.xml.XMLBinding;
import javolution.xml.XMLObjectReader;
import javolution.xml.XMLObjectWriter;
import javolution.xml.stream.XMLStreamException;

import org.apache.log4j.Logger;
import org.jboss.mx.util.MBeanServerLocator;
import org.mobicents.smsc.cassandra.DbSmsRoutingRule;

import com.cloudhopper.smpp.SmppBindType;
import com.cloudhopper.smpp.SmppSession;
import com.cloudhopper.smpp.type.Address;

/**
 * 
 * @author amit bhayani
 * 
 */
public class EsmeManagement implements EsmeManagementMBean {

	private static final Logger logger = Logger.getLogger(EsmeManagement.class);

	private static final String ESME_LIST = "esmeList";
	private static final String TAB_INDENT = "\t";
	private static final String CLASS_ATTRIBUTE = "type";
	private static final XMLBinding binding = new XMLBinding();
	private static final String PERSIST_FILE_NAME = "esme.xml";

	private final String name;

	private String persistDir = null;

	protected FastList<Esme> esmes = new FastList<Esme>();

	protected FastMap<String, EsmeCluster> esmeClusters = new FastMap<String, EsmeCluster>();

	private final TextBuilder persistFile = TextBuilder.newInstance();

	private SmppClientManagement smppClient = null;

	private MBeanServer mbeanServer = null;

	private static EsmeManagement instance = null;

	private EsmeManagement(String name) {
		this.name = name;

		binding.setClassAttribute(CLASS_ATTRIBUTE);
		binding.setAlias(Esme.class, "esme");
	}

	protected static EsmeManagement getInstance(String name) {
		if (instance == null) {
			instance = new EsmeManagement(name);
		}
		return instance;
	}

	public static EsmeManagement getInstance() {
		return instance;
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

	/**
	 * @param smppClient
	 *            the smppClient to set
	 */
	protected void setSmppClient(SmppClientManagement smppClient) {
		this.smppClient = smppClient;
	}

	@Override
	public List<Esme> getEsmes() {
		return esmes.unmodifiable();
	}

	@Override
	public Esme getEsmeByName(String esmeName) {
		for (FastList.Node<Esme> n = esmes.head(), end = esmes.tail(); (n = n.getNext()) != end;) {
			Esme esme = n.getValue();
			if (esme.getName().equals(esmeName)) {
				return esme;
			}
		}
		return null;
	}

	@Override
	public Esme getEsmeByClusterName(String esmeClusterName) {
		EsmeCluster esmeCluster = this.esmeClusters.get(esmeClusterName);
		if (esmeCluster != null) {
			return esmeCluster.getNextEsme();
		}
		return null;
	}

	protected Esme getEsmeByPrimaryKey(String SystemId, String host, int port, SmppBindType smppBindType) {

		Esme discoveredEsme = null;

		// Check for actual SystemId, host and port
		for (FastList.Node<Esme> n = esmes.head(), end = esmes.tail(); (n = n.getNext()) != end;) {
			Esme esme = n.getValue();

			if (esme.getSystemId().equals(SystemId) && esme.getSmppBindType() == smppBindType) {
				discoveredEsme = esme;
				if (esme.getHost().equals(host) && esme.getPort() == port) {
					// exact match found
					return esme;
				}

				if (esme.getHost().equals(host) && esme.getPort() == -1) {
					// Hosts match but port is any
					if (esme.getStateName().equals(com.cloudhopper.smpp.SmppSession.STATES[SmppSession.STATE_CLOSED])) {
						return esme;
					}
				}

				if (esme.getHost().equals("-1") & esme.getPort() == port) {
					// Host is any but port matches
					if (esme.getStateName().equals(com.cloudhopper.smpp.SmppSession.STATES[SmppSession.STATE_CLOSED])) {
						return esme;
					}
				}

				if (esme.getHost().equals("-1") & esme.getPort() == -1) {
					// Host is any and port is also any
					if (esme.getStateName().equals(com.cloudhopper.smpp.SmppSession.STATES[SmppSession.STATE_CLOSED])) {
						return esme;
					}
				}

			}// esme.getSystemId().equals(SystemId) && esme.getSmppBindType() ==
				// smppBindType
		}

		return discoveredEsme;
	}

	public Esme createEsme(String name, String systemId, String password, String host, int port, String smppBindType,
			String systemType, String smppIntVersion, byte ton, byte npi, String address, String smppSessionType,
			int windowSize, long connectTimeout, long requestExpiryTimeout, long windowMonitorInterval,
			long windowWaitTimeout, String clusterName, boolean countersEnabled, int enquireLinkDelay) throws Exception {
		SmppBindType smppBindTypeOb = SmppBindType.valueOf(smppBindType);
		SmppInterfaceVersionType smppInterfaceVersionTypeObj = SmppInterfaceVersionType
				.getInterfaceVersionType(smppIntVersion);
		Address addressObj = new Address(ton, npi, address);
		SmppSession.Type smppSessionTypeObj = SmppSession.Type.valueOf(smppSessionType);

		return this.createEsme(name, systemId, password, host, port, smppBindTypeOb, systemType,
				smppInterfaceVersionTypeObj, addressObj, smppSessionTypeObj, windowSize, connectTimeout,
				requestExpiryTimeout, windowMonitorInterval, windowWaitTimeout, clusterName, countersEnabled,
				enquireLinkDelay);

	}

	/**
	 * <p>
	 * Create new {@link Esme}
	 * </p>
	 * <p>
	 * Command is smsc esme create <name> <systemId> <Specify password>
	 * <host-ip> <port> <SmppBindType> <SmppSession.Type> system-type <sms | vms
	 * | ota > interface-version <3.3 | 3.4 | 5.0> esme-ton <esme address ton>
	 * esme-npi <esme address npi> esme-range <esme address range> cluster-name
	 * <cluster-name>
	 * </p>
	 * <p>
	 * where system-type, interface-version, esme-ton, esme-npi, esme-range are
	 * optional, by default interface-version is 3.4.
	 * 
	 * </p>
	 * 
	 * @param args
	 * @return
	 * @throws Exception
	 */
	@Override
	public synchronized Esme createEsme(String name, String systemId, String password, String host, int port,
			SmppBindType smppBindType, String systemType, SmppInterfaceVersionType smppIntVersion, Address address,
			SmppSession.Type smppSessionType, int windowSize, long connectTimeout, long requestExpiryTimeout,
			long windowMonitorInterval, long windowWaitTimeout, String clusterName, boolean countersEnabled,
			int enquireLinkDelay) throws Exception {

		if (smppSessionType == SmppSession.Type.CLIENT) {
			if (port < 1) {
				throw new Exception(SMSCOAMMessages.CREATE_EMSE_FAIL_PORT_CANNOT_BE_LESS_THAN_ZERO);
			}

			if (host == null || host.equals("-1")) {
				throw new Exception(SMSCOAMMessages.CREATE_EMSE_FAIL_HOST_CANNOT_BE_ANONYMOUS);
			}
		}

		for (FastList.Node<Esme> n = esmes.head(), end = esmes.tail(); (n = n.getNext()) != end;) {
			Esme esme = n.getValue();

			// Name should be unique
			if (esme.getName().equals(name)) {
				throw new Exception(String.format(SMSCOAMMessages.CREATE_EMSE_FAIL_ALREADY_EXIST, name));
			}

			// SystemId:IP:Port:SmppBindType combination should be unique for
			// CLIENT. For SERVER it accepts multiple incoming binds as far as
			// host is anonymous (-1) and/or port is -1
			String primaryKey = systemId + smppBindType.name();
			String existingPrimaryKey = esme.getSystemId() + esme.getSmppBindType().name();

			if (smppSessionType == SmppSession.Type.SERVER) {
				if (!host.equals("-1") && port != -1) {
					primaryKey = primaryKey + host + port;
					existingPrimaryKey = existingPrimaryKey + esme.getHost() + esme.getPort();
				} else {
					//Let the ESME be created
					primaryKey = "X";
					existingPrimaryKey = "Y";
				}
			} else {
				primaryKey = primaryKey + host + port;
				existingPrimaryKey = existingPrimaryKey + esme.getHost() + esme.getPort();
			}

			if (primaryKey.equals(existingPrimaryKey)) {
				throw new Exception(String.format(SMSCOAMMessages.CREATE_EMSE_FAIL_PRIMARY_KEY_ALREADY_EXIST, systemId,
						host, port, smppBindType));
			}
		}// for loop

		if (smppIntVersion == null) {
			smppIntVersion = SmppInterfaceVersionType.SMPP34;
		}

		if (clusterName == null) {
			clusterName = name;
		}

		Esme esme = null;
		if (smppSessionType.equals(SmppSession.Type.SERVER)) {
			esme = new Esme(name, systemId, password, host, port, smppBindType, systemType, smppIntVersion, address,
					clusterName, countersEnabled);
		} else {
			esme = new Esme(name, systemId, password, host, port, systemType, smppIntVersion, address, smppBindType,
					smppSessionType, windowSize, connectTimeout, requestExpiryTimeout, windowMonitorInterval,
					windowWaitTimeout, clusterName, countersEnabled, enquireLinkDelay);
		}
		esmes.add(esme);

		EsmeCluster esmeCluster = this.esmeClusters.get(clusterName);
		if (esmeCluster == null) {
			esmeCluster = new EsmeCluster(clusterName);
			this.esmeClusters.put(clusterName, esmeCluster);
		}

		esmeCluster.addEsme(esme);

		this.store();

		this.registerEsmeMbean(esme);

		return esme;
	}

	public Esme destroyEsme(String esmeName) throws Exception {
		Esme esme = this.getEsmeByName(esmeName);
		if (esme == null) {
			throw new Exception(String.format(SMSCOAMMessages.DELETE_ESME_FAILED_NO_ESME_FOUND, esmeName));
		}

		if (esme.isStarted()) {
			throw new Exception(String.format(SMSCOAMMessages.DELETE_ESME_FAILED_ESME_STARTED));
		}

		esmes.remove(esme);

		EsmeCluster esmeCluster = this.esmeClusters.get(esme.getClusterName());
		esmeCluster.removeEsme(esme);

		if (!esmeCluster.hasMoreEsmes()) {
			this.esmeClusters.remove(esme.getClusterName());
		}

		this.store();

		this.unregisterEsmeMbean(esme.getName());

		return esme;
	}

	@Override
	public void startEsme(String esmeName) throws Exception {
		Esme esme = this.getEsmeByName(esmeName);
		if (esme == null) {
			throw new Exception(String.format(SMSCOAMMessages.DELETE_ESME_FAILED_NO_ESME_FOUND, esmeName));
		}

		if (esme.isStarted()) {
			throw new Exception(String.format(SMSCOAMMessages.START_ESME_FAILED_ALREADY_STARTED, esmeName));
		}

		esme.setStarted(true);
		this.store();

		if (esme.getSmppSessionType().equals(SmppSession.Type.CLIENT)) {
			this.smppClient.startSmppClientSession(esme);
		}

	}

	@Override
	public void stopEsme(String esmeName) throws Exception {
		Esme esme = this.getEsmeByName(esmeName);
		if (esme == null) {
			throw new Exception(String.format(SMSCOAMMessages.DELETE_ESME_FAILED_NO_ESME_FOUND, esmeName));
		}

		esme.setStarted(false);
		this.store();

		this.stopWrappedSession(esme);
	}

	private void stopWrappedSession(Esme esme) {
		if (esme.getSmppSessionType().equals(SmppSession.Type.SERVER)) {
			SmppSession smppSession = esme.getSmppSession();

			if (smppSession != null) {
				// TODO can server side send UNBIND?
				// smppSession.unbind(5000);
				smppSession.close();
				smppSession.destroy();
			}
		} else {
			if (this.smppClient != null) {
				this.smppClient.stopSmppClientSession(esme);
			}
		}
	}

	private DatabaseSmsRoutingRule getDatabaseSmsRoutingRule() {
		SmsRoutingRule smsRoutingRule = SmsRouteManagement.getInstance().getSmsRoutingRule();
		if (smsRoutingRule != null && (smsRoutingRule instanceof DatabaseSmsRoutingRule)) {
			return (DatabaseSmsRoutingRule) smsRoutingRule;
		} else {
			return null;
		}
	}

	@Override
	public void updateDatabaseRule(String address, String systemId) throws Exception {
		DatabaseSmsRoutingRule smsRoutingRule = this.getDatabaseSmsRoutingRule();
		if (smsRoutingRule == null)
			throw new Exception("DatabaseSmsRoutingRule is not defined in the system");

		smsRoutingRule.updateDbSmsRoutingRule(address, systemId);
	}

	@Override
	public void deleteDatabaseRule(String address) throws Exception {
		DatabaseSmsRoutingRule smsRoutingRule = this.getDatabaseSmsRoutingRule();
		if (smsRoutingRule == null)
			throw new Exception("DatabaseSmsRoutingRule is not defined in the system");

		smsRoutingRule.deleteDbSmsRoutingRule(address);
	}

	@Override
	public String getDatabaseRule(String address) throws Exception {
		DatabaseSmsRoutingRule smsRoutingRule = this.getDatabaseSmsRoutingRule();
		if (smsRoutingRule == null)
			throw new Exception("DatabaseSmsRoutingRule is not defined in the system");

		DbSmsRoutingRule rr = smsRoutingRule.getSmsRoutingRule(address);
		if (rr != null)
			return rr.toString();
		else
			return "Record not found for: " + address;
	}

	@Override
	public String getDatabaseRulesRange() throws Exception {
		DatabaseSmsRoutingRule smsRoutingRule = this.getDatabaseSmsRoutingRule();
		if (smsRoutingRule == null)
			throw new Exception("DatabaseSmsRoutingRule is not defined in the system");

		List<DbSmsRoutingRule> rrr = smsRoutingRule.getSmsRoutingRulesRange();
		StringBuilder sb = new StringBuilder();
		int i1 = 0;
		for (DbSmsRoutingRule rr : rrr) {
			if (i1 == 0)
				i1 = 1;
			else
				sb.append("\n");
			sb.append(rr.toString());
		}
		return sb.toString();

	}

	@Override
	public String getDatabaseRulesRange(String lastAdress) throws Exception {
		DatabaseSmsRoutingRule smsRoutingRule = this.getDatabaseSmsRoutingRule();
		if (smsRoutingRule == null)
			throw new Exception("DatabaseSmsRoutingRule is not defined in the system");

		DbSmsRoutingRule rr0 = smsRoutingRule.getSmsRoutingRule(lastAdress);
		if (rr0 == null)
			return "Record not found for a key: " + lastAdress;

		List<DbSmsRoutingRule> rrr = smsRoutingRule.getSmsRoutingRulesRange(lastAdress);
		StringBuilder sb = new StringBuilder();
		int i1 = 0;
		for (DbSmsRoutingRule rr : rrr) {
			if (i1 == 0)
				i1 = 1;
			else
				sb.append("\n");
			sb.append(rr.toString());
		}
		return sb.toString();
	}

	public void start() throws Exception {

		this.mbeanServer = MBeanServerLocator.locateJBoss();

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

		logger.info(String.format("Loading ESME configuration from %s", persistFile.toString()));

		try {
			this.load();
		} catch (FileNotFoundException e) {
			logger.warn(String.format("Failed to load the SS7 configuration file. \n%s", e.getMessage()));
		}

		for (FastList.Node<Esme> n = esmes.head(), end = esmes.tail(); (n = n.getNext()) != end;) {
			Esme esme = n.getValue();
			this.registerEsmeMbean(esme);
		}

	}

	public void stop() throws Exception {
		this.store();

		for (FastList.Node<Esme> n = esmes.head(), end = esmes.tail(); (n = n.getNext()) != end;) {
			Esme esme = n.getValue();
			this.stopWrappedSession(esme);
			this.unregisterEsmeMbean(esme.getName());
		}
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
			writer.write(esmes, ESME_LIST, FastList.class);

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
			this.esmes = reader.read(ESME_LIST, FastList.class);

			// Populate cluster
			for (FastList.Node<Esme> n = this.esmes.head(), end = this.esmes.tail(); (n = n.getNext()) != end;) {
				Esme esme = n.getValue();
				String esmeClusterName = esme.getClusterName();
				EsmeCluster esmeCluster = this.esmeClusters.get(esmeClusterName);
				if (esmeCluster == null) {
					esmeCluster = new EsmeCluster(esmeClusterName);
					this.esmeClusters.put(esmeClusterName, esmeCluster);
				}
				esmeCluster.addEsme(esme);
			}

			reader.close();
		} catch (XMLStreamException ex) {
			// this.logger.info(
			// "Error while re-creating Linksets from persisted file", ex);
		}
	}

	private void registerEsmeMbean(Esme esme) {
		try {
			ObjectName esmeObjNname = new ObjectName(SmscManagement.JMX_DOMAIN + ":layer=Esme,name=" + esme.getName());
			StandardMBean esmeMxBean = new StandardMBean(esme, EsmeMBean.class, true);

			this.mbeanServer.registerMBean(esmeMxBean, esmeObjNname);
		} catch (InstanceAlreadyExistsException e) {
			logger.error(String.format("Error while registering MBean for ESME %s", esme.getName()), e);
		} catch (MBeanRegistrationException e) {
			logger.error(String.format("Error while registering MBean for ESME %s", esme.getName()), e);
		} catch (NotCompliantMBeanException e) {
			logger.error(String.format("Error while registering MBean for ESME %s", esme.getName()), e);
		} catch (MalformedObjectNameException e) {
			logger.error(String.format("Error while registering MBean for ESME %s", esme.getName()), e);
		}
	}

	private void unregisterEsmeMbean(String esmeName) {

		try {
			ObjectName esmeObjNname = new ObjectName(SmscManagement.JMX_DOMAIN + ":layer=Esme,name=" + esmeName);
			this.mbeanServer.unregisterMBean(esmeObjNname);
		} catch (MBeanRegistrationException e) {
			logger.error(String.format("Error while unregistering MBean for ESME %s", esmeName), e);
		} catch (InstanceNotFoundException e) {
			logger.error(String.format("Error while unregistering MBean for ESME %s", esmeName), e);
		} catch (MalformedObjectNameException e) {
			logger.error(String.format("Error while unregistering MBean for ESME %s", esmeName), e);
		}
	}

}
