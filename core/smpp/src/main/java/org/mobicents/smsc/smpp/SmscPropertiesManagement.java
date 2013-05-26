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

import javolution.text.TextBuilder;
import javolution.xml.XMLBinding;
import javolution.xml.XMLObjectReader;
import javolution.xml.XMLObjectWriter;
import javolution.xml.stream.XMLStreamException;

import org.apache.log4j.Logger;

/**
 * 
 * @author Amit Bhayani
 * @author sergey vetyutnev
 *
 */
public class SmscPropertiesManagement implements SmscPropertiesManagementMBean {
	private static final Logger logger = Logger.getLogger(SmscPropertiesManagement.class);

	private static final String SC_GT = "scgt";
	private static final String SC_SSN = "scssn";
	private static final String HLR_SSN = "hlrssn";
	private static final String MSC_SSN = "mscssn";
	private static final String MAX_MAP_VERSION = "maxmapv";
	private static final String DEFAULT_VALIDITY_PERIOD_HOURS = "defaultValidityPeriodHours";
	private static final String MAX_VALIDITY_PERIOD_HOURS = "maxValidityPeriodHours";
	private static final String DEFAULT_TON = "defaultTon";
	private static final String DEFAULT_NPI = "defaultNpi";
	private static final String SUBSCRIBER_BUSY_DUE_DELAY = "subscriberBusyDueDelay";
	private static final String FIRST_DUE_DELAY = "firstDueDelay";
	private static final String SECOND_DUE_DELAY = "secondDueDelay";
	private static final String MAX_DUE_DELAY = "maxDueDelay";
	private static final String DUE_DELAY_MULTIPLICATOR = "dueDelayMultiplicator";
	private static final String MAX_MESSAGE_LENGTH_REDUCER = "maxMessageLengthReducer";

	private static final String TAB_INDENT = "\t";
	private static final String CLASS_ATTRIBUTE = "type";
	private static final XMLBinding binding = new XMLBinding();
	private static final String PERSIST_FILE_NAME = "smscproperties.xml";
	
	private static SmscPropertiesManagement instance;
	
	private final String name;

	private String persistDir = null;

	private final TextBuilder persistFile = TextBuilder.newInstance();

	private String serviceCenterGt = null;
	private int serviceCenterSsn = -1;
	private int hlrSsn = -1;
	private int mscSsn = -1;
	private int maxMapVersion = 3;

	private int defaultValidityPeriodHours = 3 * 24;
	private int maxValidityPeriodHours = 10 * 24;
	private int defaultTon = 1;
	private int defaultNpi = 1;
	// delay after delivering failure with cause "subscriber busy" (sec)
	private int subscriberBusyDueDelay = 60 * 2;
	// delay before first a delivering try after incoming message receiving (sec) 
	private int firstDueDelay = 60;
	// delay after first delivering failure (sec)
	private int secondDueDelay = 60 * 5;
	// max possible delay between  delivering failure (sec)
	private int maxDueDelay = 3600 * 24;
	// next delay (after failure will be calculated as "prevDueDelay * dueDelayMultiplicator / 100")
	private int dueDelayMultiplicator = 200;
	// 	
	private int maxMessageLengthReducer = 6;

	private SmscPropertiesManagement(String name) {
		this.name = name;
		binding.setClassAttribute(CLASS_ATTRIBUTE);
	}
	
	public static SmscPropertiesManagement getInstance(String name){
		if(instance == null){
			instance = new SmscPropertiesManagement(name);
		}
		return instance;
	}
	
	public static SmscPropertiesManagement getInstance(){
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

	public String getServiceCenterGt() {
		return serviceCenterGt;
	}

	public void setServiceCenterGt(String serviceCenterGt) {
		this.serviceCenterGt = serviceCenterGt;
		this.store();
	}

	public int getServiceCenterSsn() {
		return serviceCenterSsn;
	}

	public void setServiceCenterSsn(int serviceCenterSsn) {
		this.serviceCenterSsn = serviceCenterSsn;
		this.store();
	}

	public int getHlrSsn() {
		return hlrSsn;
	}

	public void setHlrSsn(int hlrSsn) {
		this.hlrSsn = hlrSsn;
		this.store();
	}

	public int getMscSsn() {
		return mscSsn;
	}

	public void setMscSsn(int mscSsn) {
		this.mscSsn = mscSsn;
		this.store();
	}

	public int getMaxMapVersion() {
		return maxMapVersion;
	}

	public void setMaxMapVersion(int maxMapVersion) {
		this.maxMapVersion = maxMapVersion;
		this.store();
	}

	public int getDefaultValidityPeriodHours() {
		return defaultValidityPeriodHours;
	}

	public void setDefaultValidityPeriodHours(int defaultValidityPeriodHours) {
		this.defaultValidityPeriodHours = defaultValidityPeriodHours;
	}

	public int getMaxValidityPeriodHours() {
		return maxValidityPeriodHours;
	}

	public void setMaxValidityPeriodHours(int maxValidityPeriodHours) {
		this.maxValidityPeriodHours = maxValidityPeriodHours;
	}

	public int getDefaultTon() {
		return defaultTon;
	}

	public void setDefaultTon(int defaultTon) {
		this.defaultTon = defaultTon;
	}

	public int getDefaultNpi() {
		return defaultNpi;
	}

	public void setDefaultNpi(int defaultNpi) {
		this.defaultNpi = defaultNpi;
	}

	public int getSubscriberBusyDueDelay() {
		return subscriberBusyDueDelay;
	}

	public void setSubscriberBusyDueDelay(int subscriberBusyDueDelay) {
		this.subscriberBusyDueDelay = subscriberBusyDueDelay;
	}

	public int getFirstDueDelay() {
		return firstDueDelay;
	}

	public void setFirstDueDelay(int firstDueDelay) {
		this.firstDueDelay = firstDueDelay;
	}

	public int getSecondDueDelay() {
		return secondDueDelay;
	}

	public void setSecondDueDelay(int secondDueDelay) {
		this.secondDueDelay = secondDueDelay;
	}

	public int getMaxDueDelay() {
		return maxDueDelay;
	}

	public void setMaxDueDelay(int maxDueDelay) {
		this.maxDueDelay = maxDueDelay;
	}

	public int getDueDelayMultiplicator() {
		return dueDelayMultiplicator;
	}

	public void setDueDelayMultiplicator(int dueDelayMultiplicator) {
		this.dueDelayMultiplicator = dueDelayMultiplicator;
	}

	@Override
	public int getMaxMessageLengthReducer() {
		return maxMessageLengthReducer;
	}

	@Override
	public void setMaxMessageLengthReducer(int maxMessageLengReducer) {
		this.maxMessageLengthReducer = maxMessageLengReducer;
	}


	public void start() throws Exception {

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

		logger.info(String.format("Loading SMSC Properties from %s", persistFile.toString()));

		try {
			this.load();
		} catch (FileNotFoundException e) {
			logger.warn(String.format("Failed to load the SMSC configuration file. \n%s", e.getMessage()));
		}

	}

	public void stop() throws Exception {
		this.store();
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

			writer.write(this.serviceCenterGt, SC_GT, String.class);
			writer.write(this.serviceCenterSsn, SC_SSN, Integer.class);
			writer.write(this.hlrSsn, HLR_SSN, Integer.class);
			writer.write(this.mscSsn, MSC_SSN, Integer.class);
			writer.write(this.maxMapVersion, MAX_MAP_VERSION, Integer.class);
			writer.write(this.defaultValidityPeriodHours, DEFAULT_VALIDITY_PERIOD_HOURS, Integer.class);
			writer.write(this.maxValidityPeriodHours, MAX_VALIDITY_PERIOD_HOURS, Integer.class);
			writer.write(this.defaultTon, DEFAULT_TON, Integer.class);
			writer.write(this.defaultNpi, DEFAULT_NPI, Integer.class);

			writer.write(this.subscriberBusyDueDelay, SUBSCRIBER_BUSY_DUE_DELAY, Integer.class);
			writer.write(this.firstDueDelay, FIRST_DUE_DELAY, Integer.class);
			writer.write(this.secondDueDelay, SECOND_DUE_DELAY, Integer.class);
			writer.write(this.maxDueDelay, MAX_DUE_DELAY, Integer.class);
			writer.write(this.dueDelayMultiplicator, DUE_DELAY_MULTIPLICATOR, Integer.class);
			writer.write(this.maxMessageLengthReducer, MAX_MESSAGE_LENGTH_REDUCER, Integer.class);

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
			this.serviceCenterGt = reader.read(SC_GT, String.class);
			this.serviceCenterSsn = reader.read(SC_SSN, Integer.class);
			this.hlrSsn = reader.read(HLR_SSN, Integer.class);
			this.mscSsn = reader.read(MSC_SSN, Integer.class);
			this.maxMapVersion = reader.read(MAX_MAP_VERSION, Integer.class);
			Integer dvp = reader.read(DEFAULT_VALIDITY_PERIOD_HOURS, Integer.class);
			if (dvp != null)
				this.defaultValidityPeriodHours = dvp;
			Integer mvp = reader.read(MAX_VALIDITY_PERIOD_HOURS, Integer.class);
			if (mvp != null)
				this.maxValidityPeriodHours = mvp;
			Integer dTon = reader.read(DEFAULT_TON, Integer.class);
			if (dTon != null)
				this.defaultTon = dTon;
			Integer dNpi = reader.read(DEFAULT_NPI, Integer.class);
			if (dNpi != null)
				this.defaultNpi = dNpi;
			Integer val = reader.read(SUBSCRIBER_BUSY_DUE_DELAY, Integer.class);
			if (val != null)
				this.subscriberBusyDueDelay = val;
			val = reader.read(FIRST_DUE_DELAY, Integer.class);
			if (val != null)
				this.firstDueDelay = val;
			val = reader.read(SECOND_DUE_DELAY, Integer.class);
			if (val != null)
				this.secondDueDelay = val;
			val = reader.read(MAX_DUE_DELAY, Integer.class);
			if (val != null)
				this.maxDueDelay = val;
			val = reader.read(DUE_DELAY_MULTIPLICATOR, Integer.class);
			if (val != null)
				this.dueDelayMultiplicator = val;
			val = reader.read(MAX_MESSAGE_LENGTH_REDUCER, Integer.class);
			if (val != null)
				this.maxMessageLengthReducer = val;
			
			reader.close();
		} catch (XMLStreamException ex) {
			// this.logger.info(
			// "Error while re-creating Linksets from persisted file", ex);
		}
	}
}
