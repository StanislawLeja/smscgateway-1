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

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javolution.util.FastMap;

import org.apache.log4j.Logger;
import org.mobicents.protocols.ss7.map.api.MAPApplicationContextVersion;
import org.mobicents.smsc.cassandra.DbSmsRoutingRule;
import org.mobicents.smsc.cassandra.SmsRoutingRuleType;
import org.mobicents.ss7.management.console.ShellExecutor;

import com.cloudhopper.smpp.SmppBindType;
import com.cloudhopper.smpp.SmppConstants;
import com.cloudhopper.smpp.SmppSession;
import com.cloudhopper.smpp.type.Address;

/**
 * @author amit bhayani
 * @author sergey vetyutnev
 * 
 */
public class SMSCShellExecutor implements ShellExecutor {

	private static final Logger logger = Logger.getLogger(SMSCShellExecutor.class);

	private SmscManagement smscManagement;

	private static SmscPropertiesManagement smscPropertiesManagement;

	private static final MapVersionCache mapVersionCache = MapVersionCache.getInstance();

	private static final String LINE_SEPARATOR = System.getProperty("line.separator");
	private static final String MAP_CACHE_KEY_VALUE_SEPARATOR = " : ";

	public SMSCShellExecutor() {

	}

	public void start() throws Exception {
		smscPropertiesManagement = SmscPropertiesManagement.getInstance(this.getSmscManagement().getName());
		if (logger.isInfoEnabled()) {
			logger.info("Started SMSCShellExecutor " + this.getSmscManagement().getName());
		}
	}

	/**
	 * @return the m3uaManagement
	 */
	public SmscManagement getSmscManagement() {
		return smscManagement;
	}

	/**
	 * @param m3uaManagement
	 *            the m3uaManagement to set
	 */
	public void setSmscManagement(SmscManagement smscManagement) {
		this.smscManagement = smscManagement;
	}

	private String showSip() {
		SipManagement sipManagement = SipManagement.getInstance();
		List<Sip> sips = sipManagement.getSips();
		if (sips.size() == 0) {
			return SMSCOAMMessages.NO_SIP_DEFINED_YET;
		}

		StringBuffer sb = new StringBuffer();
		for (Sip sip : sips) {
			sb.append(SMSCOAMMessages.NEW_LINE);
			sip.show(sb);
		}
		return sb.toString();
	}

	private String modifySip(String[] args) throws Exception {
		if (args.length < 6 || args.length > 20) {
			return SMSCOAMMessages.INVALID_COMMAND;
		}

		// modify existing SIP
		String name = args[3];
		if (name == null) {
			return SMSCOAMMessages.INVALID_COMMAND;
		}

		SipManagement sipManagement = SipManagement.getInstance();
		Sip sip = sipManagement.getSipByName(name);
		if (sip == null) {
			return String.format(SMSCOAMMessages.SIP_NOT_FOUND, name);
		}

		int count = 4;
		String command;

		boolean success = false;
		while (count < (args.length - 1) && ((command = args[count++]) != null)) {
			String value = args[count++];
			if (command.equals("cluster-name")) {
				sip.setClusterName(value);
				success = true;
			} else if (command.equals("host")) {
				sip.setHost(value);
				success = true;
			} else if (command.equals("port")) {
				sip.setPort(Integer.parseInt(value));
				success = true;
			} else if (command.equals("ton")) {
				sip.setAddressTon(Byte.parseByte(value));
				success = true;
			} else if (command.equals("npi")) {
				sip.setAddressNpi(Byte.parseByte(value));
				success = true;
			} else if (command.equals("range")) {
				sip.setAddressRange(value);
				success = true;
			} else if (command.equals("counters-enabled")) {
				sip.setCountersEnabled(Boolean.parseBoolean(value));
				success = true;
			} else if (command.equals("charging-enabled")) {
				sip.setChargingEnabled(Boolean.parseBoolean(value));
				success = true;
			}
		}// while

		if (!success) {
			return SMSCOAMMessages.INVALID_COMMAND;
		}

		return String.format(SMSCOAMMessages.SIP_MODIFY_SUCCESS, name);
	}

	/**
	 * Command is smsc esme create name <systemId> <Specify password> <host-ip>
	 * <port> <SmppBindType> <SmppSession.Type> system-type <sms | vms | ota >
	 * interface-version <3.3 | 3.4 | 5.0> esme-ton <esme address ton> esme-npi
	 * <esme address npi> esme-range <esme address range> cluster-name
	 * <clusterName> window-size <windowSize> connect-timeout <connectTimeout>
	 * request-expiry-timeout <requestExpiryTimeout> window-monitor-interval
	 * <windowMonitorInterval> window-wait-timeout <windowWaitTimeout>
	 * counters-enabled <true | false> enquire-link-delay <30000>
	 * 
	 * @param args
	 * @return
	 */
	private String createEsme(String[] args) throws Exception {
		if (args.length < 10 || args.length > 36) {
			return SMSCOAMMessages.INVALID_COMMAND;
		}

		// Create new Rem ESME
		String name = args[3];
		if (name == null) {
			return SMSCOAMMessages.INVALID_COMMAND;
		}

		String systemId = args[4];
		if (systemId == null) {
			return SMSCOAMMessages.INVALID_COMMAND;
		}
		String password = args[5];
		if (password == null) {
			return SMSCOAMMessages.INVALID_COMMAND;
		}
		String host = args[6];
		if (host == null) {
			return SMSCOAMMessages.INVALID_COMMAND;
		}
		String strPort = args[7];
		int intPort = -1;
		if (strPort == null) {
			return SMSCOAMMessages.INVALID_COMMAND;
		} else {
			try {
				intPort = Integer.parseInt(strPort);
			} catch (Exception e) {
				return SMSCOAMMessages.INVALID_COMMAND;
			}
		}

		SmppBindType smppBindType = null;
		String smppBindTypeStr = args[8];

		if (smppBindTypeStr == null) {
			return SMSCOAMMessages.INVALID_COMMAND;
		}

		if (SmppBindType.TRANSCEIVER.toString().equals(smppBindTypeStr)) {
			smppBindType = SmppBindType.TRANSCEIVER;
		} else if (SmppBindType.TRANSMITTER.toString().equals(smppBindTypeStr)) {
			smppBindType = SmppBindType.TRANSMITTER;
		} else if (SmppBindType.RECEIVER.toString().equals(smppBindTypeStr)) {
			smppBindType = SmppBindType.RECEIVER;
		} else {
			return SMSCOAMMessages.INVALID_COMMAND;
		}

		String smppSessionTypeStr = args[9];
		if (smppBindTypeStr == null) {
			return SMSCOAMMessages.INVALID_COMMAND;
		}

		SmppSession.Type smppSessionType = SmppSession.Type.valueOf(smppSessionTypeStr);

		String systemType = null;
		SmppInterfaceVersionType smppVersionType = null;
		byte esmeTonType = (byte) smscPropertiesManagement.getDefaultTon();
		byte esmeNpiType = (byte) smscPropertiesManagement.getDefaultNpi();
		String esmeAddrRange = null;
		String clusterName = name;

		int count = 10;

		int windowSize = SmppConstants.DEFAULT_WINDOW_SIZE;
		long connectTimeout = SmppConstants.DEFAULT_CONNECT_TIMEOUT;
		long requestExpiryTimeout = SmppConstants.DEFAULT_REQUEST_EXPIRY_TIMEOUT;
		long windowMonitorInterval = SmppConstants.DEFAULT_WINDOW_MONITOR_INTERVAL;
		long windowWaitTimeout = SmppConstants.DEFAULT_WINDOW_WAIT_TIMEOUT;

		boolean countersEnabled = true;
		int enquireLinkDelay = 30000;
		boolean chargingEnabled = false;

		while (count < args.length) {
			// These are all optional parameters for a Tx/Rx/Trx binds
			String key = args[count++];
			if (key == null) {
				return SMSCOAMMessages.INVALID_COMMAND;
			}

			if (key.equals("system-type")) {
				systemType = args[count++];
			} else if (key.equals("interface-version")) {
				smppVersionType = SmppInterfaceVersionType.getInterfaceVersionType(args[count++]);
				if (smppVersionType == null) {
					smppVersionType = SmppInterfaceVersionType.SMPP34;
				}
			} else if (key.equals("esme-ton")) {
				esmeTonType = Byte.parseByte(args[count++]);
			} else if (key.equals("esme-npi")) {
				esmeNpiType = Byte.parseByte(args[count++]);
			} else if (key.equals("esme-range")) {
				esmeAddrRange = /* Regex */args[count++];
			} else if (key.equals("window-size")) {
				windowSize = Integer.parseInt(args[count++]);
			} else if (key.equals("connect-timeout")) {
				connectTimeout = Long.parseLong(args[count++]);
			} else if (key.equals("request-expiry-timeout")) {
				requestExpiryTimeout = Long.parseLong(args[count++]);
			} else if (key.equals("window-monitor-interval")) {
				windowMonitorInterval = Long.parseLong(args[count++]);
			} else if (key.equals("window-wait-timeout")) {
				windowWaitTimeout = Long.parseLong(args[count++]);
			} else if (key.equals("cluster-name")) {
				clusterName = args[count++];
			} else if (key.equals("counters-enabled")) {
				countersEnabled = Boolean.parseBoolean(args[count++]);
			} else if (key.equals("enquire-link-delay")) {
				enquireLinkDelay = Integer.parseInt(args[count++]);
			} else if (key.equals("charging-enabled")) {
				chargingEnabled = true;
			} else {
				return SMSCOAMMessages.INVALID_COMMAND;
			}

		}

		Address address = new Address(esmeTonType, esmeNpiType, esmeAddrRange);
		Esme esme = this.smscManagement.getEsmeManagement().createEsme(name, systemId, password, host, intPort,
				chargingEnabled, smppBindType, systemType, smppVersionType, address, smppSessionType, windowSize,
				connectTimeout, requestExpiryTimeout, windowMonitorInterval, windowWaitTimeout, clusterName,
				countersEnabled, enquireLinkDelay);
		return String.format(SMSCOAMMessages.CREATE_ESME_SUCCESSFULL, esme.getSystemId());
	}

	/**
	 * smsc esme destroy <esmeName>
	 * 
	 * @param args
	 * @return
	 * @throws Exception
	 */
	private String destroyEsme(String[] args) throws Exception {
		if (args.length < 4) {
			return SMSCOAMMessages.INVALID_COMMAND;
		}

		String esmeName = args[3];
		if (esmeName == null) {
			return SMSCOAMMessages.INVALID_COMMAND;
		}

		Esme esme = this.smscManagement.getEsmeManagement().destroyEsme(esmeName);

		return String.format(SMSCOAMMessages.DELETE_ESME_SUCCESSFUL, esmeName);
	}

	private String showEsme() {
		List<Esme> esmes = this.smscManagement.getEsmeManagement().getEsmes();
		if (esmes.size() == 0) {
			return SMSCOAMMessages.NO_ESME_DEFINED_YET;
		}
		StringBuffer sb = new StringBuffer();
		for (Esme esme : esmes) {
			sb.append(SMSCOAMMessages.NEW_LINE);
			esme.show(sb);
		}
		return sb.toString();
	}

	private String executeSmsc(String[] args) {
		try {
			if (args.length < 2 || args.length > 20) {
				// any command will have atleast 3 args
				return SMSCOAMMessages.INVALID_COMMAND;
			}

			if (args[1] == null) {
				return SMSCOAMMessages.INVALID_COMMAND;
			}

			if (args[1].equals("esme")) {
				String rasCmd = args[2];
				if (rasCmd == null) {
					return SMSCOAMMessages.INVALID_COMMAND;
				}

				if (rasCmd.equals("create")) {
					return this.createEsme(args);
				} else if (rasCmd.equals("delete")) {
					return this.destroyEsme(args);
				} else if (rasCmd.equals("show")) {
					return this.showEsme();
				} else if (rasCmd.equals("start")) {
					return this.startEsme(args);
				} else if (rasCmd.equals("stop")) {
					return this.stopEsme(args);
				}
				return SMSCOAMMessages.INVALID_COMMAND;
			} else if (args[1].equals("sip")) {
				String rasCmd = args[2];
				if (rasCmd == null) {
					return SMSCOAMMessages.INVALID_COMMAND;
				}

				if (rasCmd.equals("modify")) {
					return this.modifySip(args);
				} else if (rasCmd.equals("show")) {
					return this.showSip();
				}

				return SMSCOAMMessages.INVALID_COMMAND;

			} else if (args[1].equals("set")) {
				return this.manageSet(args);
			} else if (args[1].equals("get")) {
				return this.manageGet(args);
			} else if (args[1].equals("remove")) {
				return this.manageRemove(args);
			} else if (args[1].equals("smppserver")) {
				String rasCmd = args[2];
				if (rasCmd == null) {
					return SMSCOAMMessages.INVALID_COMMAND;
				}

				if (rasCmd.equals("set")) {
					return this.manageSmppServerSet(args);
				} else if (rasCmd.equals("get")) {
					return this.manageSmppServerGet(args);
				}

				return SMSCOAMMessages.INVALID_COMMAND;
			} else if (args[1].toLowerCase().equals("databaserule")) {
				String rasCmd = args[2];
				if (rasCmd == null) {
					return SMSCOAMMessages.INVALID_COMMAND;
				}

				SmsRoutingRule smsRoutingRule = this.smscManagement.getSmsRoutingRule();
				if (!(smsRoutingRule instanceof DatabaseSmsRoutingRule)) {
					return SMSCOAMMessages.NO_DATABASE_SMS_ROUTING_RULE;
				}

				if (rasCmd.equals("update")) {
					return this.databaseRuleUpdate(args);
				} else if (rasCmd.equals("delete")) {
					return this.databaseRuleDelete(args);
				} else if (rasCmd.equals("get")) {
					return this.databaseRuleGet(args);
				} else if (rasCmd.toLowerCase().equals("getrange")) {
					return this.databaseRuleGetRange(args);
				}

				return SMSCOAMMessages.INVALID_COMMAND;
			} else if (args[1].equals("archive")) {
				String rasCmd = args[2];
				if (rasCmd == null) {
					return SMSCOAMMessages.INVALID_COMMAND;
				}

				if (rasCmd.equals("generatecdr")) {
					return this.archiveGenerateCdr(args);
				}

				return SMSCOAMMessages.INVALID_COMMAND;
			} else if (args[1].toLowerCase().equals("mapcache")) {
				String rasCmd = args[2];

				if (rasCmd == null) {
					return SMSCOAMMessages.INVALID_COMMAND;
				}

				if (rasCmd.equals("get")) {
					return this.getMapVersionCache(args);
				} else if (rasCmd.equals("set")) {
					return this.setMapVersionCache(args);
				} else if (rasCmd.equals("clear")) {
					return this.clearMapVersionCache(args);
				}

				return SMSCOAMMessages.INVALID_COMMAND;
			} else if (args[1].toLowerCase().equals("stat")) {
				String rasCmd = args[2];

				if (rasCmd == null) {
					return SMSCOAMMessages.INVALID_COMMAND;
				}

				if (rasCmd.equals("get")) {
					return this.getStat(args);
				}

				return SMSCOAMMessages.INVALID_COMMAND;
			}

			return SMSCOAMMessages.INVALID_COMMAND;
		} catch (Exception e) {
			logger.error(String.format("Error while executing comand %s", Arrays.toString(args)), e);
			return e.getMessage();
		}
	}

	/**
	 * smsc mapcache get <msisdn>
	 * 
	 * msisdn is optional
	 * 
	 * @param args
	 * @return
	 */
	private String getMapVersionCache(String[] args) throws Exception {
		if (args.length < 3 || args.length > 4) {
			return SMSCOAMMessages.INVALID_COMMAND;
		}

		if (args.length == 4) {
			String msisdn = args[3];

			MAPApplicationContextVersion mapApplicationContextVersion = mapVersionCache
					.getMAPApplicationContextVersion(msisdn);

			if (mapApplicationContextVersion != null) {
				return mapApplicationContextVersion.toString();
			} else {
				return SMSCOAMMessages.MAP_VERSION_CACHE_NOT_FOUND;
			}
		}

		FastMap<String, MAPApplicationContextVersion> cache = mapVersionCache.getMAPApplicationContextVersionCache();
		if (cache.size() == 0) {
			return SMSCOAMMessages.MAP_VERSION_CACHE_NOT_FOUND;
		}

		StringBuffer sb = new StringBuffer();
		for (FastMap.Entry<String, MAPApplicationContextVersion> e = cache.head(), end = cache.tail(); (e = e.getNext()) != end;) {
			sb.append(e.getKey()).append(MAP_CACHE_KEY_VALUE_SEPARATOR).append(e.getValue()).append(LINE_SEPARATOR);
		}

		return sb.toString();
	}

	/**
	 * smsc mapcache clear
	 * 
	 * msisdn is optional
	 * 
	 * @param args
	 * @return
	 */
	private String clearMapVersionCache(String[] args) throws Exception {
		if (args.length != 3) {
			return SMSCOAMMessages.INVALID_COMMAND;
		}

		mapVersionCache.forceClear();

		return SMSCOAMMessages.MAP_VERSION_CACHE_SUCCESSFULLY_CLEARED;
	}

	/**
	 * smsc mapcache set <msisdn> <version>
	 * 
	 * msisdn is optional
	 * 
	 * @param args
	 * @return
	 */
	private String setMapVersionCache(String[] args) throws Exception {
		if (args.length != 5) {
			return SMSCOAMMessages.INVALID_COMMAND;
		}

		String msisdn = args[3];
		String version = args[4];

		MAPApplicationContextVersion mapApplicationContextVersion = MAPApplicationContextVersion.getInstance(Long
				.parseLong(version));

		if (mapApplicationContextVersion == null
				|| mapApplicationContextVersion == MAPApplicationContextVersion.version4) {
			return SMSCOAMMessages.MAP_VERSION_CACHE_INVALID_VERSION;

		}

		mapVersionCache.setMAPApplicationContextVersion(msisdn, mapApplicationContextVersion);

		return SMSCOAMMessages.MAP_VERSION_CACHE_SUCCESSFULLY_SET;
	}

	/**
	 * Command is smsc smppserver set <variable> <value>
	 * 
	 * @param options
	 * @return
	 * @throws Exception
	 */
	private String manageSmppServerSet(String[] options) throws Exception {
		if (options.length != 5) {
			return SMSCOAMMessages.INVALID_COMMAND;
		}

		SmppServerManagement smppServerManagement = this.smscManagement.getSmppServerManagement();

		String parName = options[3].toLowerCase();
		if (parName.equals("port")) {
			smppServerManagement.setBindPort(Integer.parseInt(options[4]));
		} else if (parName.equals("bindtimeout")) {
			int val = Integer.parseInt(options[4]);
			smppServerManagement.setBindTimeout(val);
		} else if (parName.equals("systemid")) {
			smppServerManagement.setSystemId(options[4]);
		} else if (parName.equals("autonegotiateversion")) {
			boolean val = Boolean.parseBoolean(options[4]);
			smppServerManagement.setAutoNegotiateInterfaceVersion(val);
		} else if (parName.equals("interfaceversion")) {
			double val = Double.parseDouble(options[4]);
			smppServerManagement.setInterfaceVersion(val);
		} else if (parName.equals("maxconnectionsize")) {
			int val = Integer.parseInt(options[4]);
			smppServerManagement.setMaxConnectionSize(val);
		} else if (parName.equals("defaultwindowsize")) {
			int val = Integer.parseInt(options[4]);
			smppServerManagement.setDefaultWindowSize(val);
		} else if (parName.equals("defaultwindowwaittimeout")) {
			int val = Integer.parseInt(options[4]);
			smppServerManagement.setDefaultWindowWaitTimeout(val);
		} else if (parName.equals("defaultrequestexpirytimeout")) {
			int val = Integer.parseInt(options[4]);
			smppServerManagement.setDefaultRequestExpiryTimeout(val);
		} else if (parName.equals("defaultwindowmonitorinterval")) {
			int val = Integer.parseInt(options[4]);
			smppServerManagement.setDefaultWindowMonitorInterval(val);
		} else if (parName.equals("defaultsessioncountersenabled")) {
			boolean val = Boolean.parseBoolean(options[4]);
			smppServerManagement.setDefaultSessionCountersEnabled(val);
		} else {
			return SMSCOAMMessages.INVALID_COMMAND;
		}

		return SMSCOAMMessages.SMPP_SERVER_PARAMETER_SUCCESSFULLY_SET;
	}

	private String manageSet(String[] options) throws Exception {
		if (options.length < 4) {
			return SMSCOAMMessages.INVALID_COMMAND;
		}

		String parName = options[2].toLowerCase();
		try {
			if (parName.equals("scgt")) {
				smscPropertiesManagement.setServiceCenterGt(options[3]);
			} else if (parName.equals("scssn")) {
				int val = Integer.parseInt(options[3]);
				smscPropertiesManagement.setServiceCenterSsn(val);
			} else if (parName.equals("hlrssn")) {
				int val = Integer.parseInt(options[3]);
				smscPropertiesManagement.setHlrSsn(val);
			} else if (parName.equals("mscssn")) {
				int val = Integer.parseInt(options[3]);
				smscPropertiesManagement.setMscSsn(val);
			} else if (parName.equals("maxmapv")) {
				int val = Integer.parseInt(options[3]);
				smscPropertiesManagement.setMaxMapVersion(val);

			} else if (parName.equals("defaultvalidityperiodhours")) {
				int val = Integer.parseInt(options[3]);
				smscPropertiesManagement.setDefaultValidityPeriodHours(val);
			} else if (parName.equals("maxvalidityperiodhours")) {
				int val = Integer.parseInt(options[3]);
				smscPropertiesManagement.setMaxValidityPeriodHours(val);
			} else if (parName.equals("defaultton")) {
				int val = Integer.parseInt(options[3]);
				smscPropertiesManagement.setDefaultTon(val);
			} else if (parName.equals("defaultnpi")) {
				int val = Integer.parseInt(options[3]);
				smscPropertiesManagement.setDefaultNpi(val);
			} else if (parName.equals("subscriberbusyduedelay")) {
				int val = Integer.parseInt(options[3]);
				smscPropertiesManagement.setSubscriberBusyDueDelay(val);
			} else if (parName.equals("firstduedelay")) {
				int val = Integer.parseInt(options[3]);
				smscPropertiesManagement.setFirstDueDelay(val);
			} else if (parName.equals("secondduedelay")) {
				int val = Integer.parseInt(options[3]);
				smscPropertiesManagement.setSecondDueDelay(val);
			} else if (parName.equals("maxduedelay")) {
				int val = Integer.parseInt(options[3]);
				smscPropertiesManagement.setMaxDueDelay(val);
			} else if (parName.equals("duedelaymultiplicator")) {
				int val = Integer.parseInt(options[3]);
				smscPropertiesManagement.setDueDelayMultiplicator(val);
			} else if (parName.equals("maxmessagelengthreducer")) {
				int val = Integer.parseInt(options[3]);
				smscPropertiesManagement.setMaxMessageLengthReducer(val);
			} else if (parName.equals("smppencodingforucs2")) {
				String s1 = options[3].toLowerCase();
				if (s1.equals("utf8")) {
					smscPropertiesManagement.setSmppEncodingForUCS2(SmppEncodingForUCS2.Utf8);
				} else if (s1.equals("unicode")) {
					smscPropertiesManagement.setSmppEncodingForUCS2(SmppEncodingForUCS2.Unicode);
				} else {
					return String.format(SMSCOAMMessages.ILLEGAL_ARGUMENT, "SmppEncodingForUCS2 value",
							"UTF8 or UNICODE are possible");
				}
			} else if (parName.equals("hosts")) {
				String val = options[3];
				smscPropertiesManagement.setHosts(val);
			} else if (parName.equals("keyspacename")) {
				String val = options[3];
				smscPropertiesManagement.setKeyspaceName(val);
			} else if (parName.equals("clustername")) {
				String val = options[3];
				smscPropertiesManagement.setClusterName(val);
			} else if (parName.equals("fetchperiod")) {
				long val = Long.parseLong(options[3]);
				smscPropertiesManagement.setFetchPeriod(val);
			} else if (parName.equals("fetchmaxrows")) {
				int val = Integer.parseInt(options[3]);
				smscPropertiesManagement.setFetchMaxRows(val);
			} else if (parName.equals("maxactivitycount")) {
				int val = Integer.parseInt(options[3]);
				smscPropertiesManagement.setMaxActivityCount(val);
				// } else if (parName.equals("cdrdatabaseexportduration")) {
				// int val = Integer.parseInt(options[3]);
				// smscPropertiesManagement.setCdrDatabaseExportDuration(val);
			} else if (parName.equals("esmedefaultcluster")) {
				smscPropertiesManagement.setEsmeDefaultClusterName(options[3]);
			} else if (parName.equals("smshomerouting")) {
				smscPropertiesManagement.setSMSHomeRouting(Boolean.parseBoolean(options[3]));
			} else if (parName.equals("revisesecondsonsmscstart")) {
				int val = Integer.parseInt(options[3]);
				smscPropertiesManagement.setReviseSecondsOnSmscStart(val);
			} else if (parName.equals("processingsmssettimeout")) {
				int val = Integer.parseInt(options[3]);
				smscPropertiesManagement.setProcessingSmsSetTimeout(val);
			} else if (parName.equals("generatereceiptcdr")) {
				smscPropertiesManagement.setGenerateReceiptCdr(Boolean.parseBoolean(options[3]));

			} else if (parName.equals("mocharging")) {
				smscPropertiesManagement.setMoCharging(Boolean.parseBoolean(options[3]));
			} else if (parName.equals("txsmppcharging")) {
				smscPropertiesManagement.setTxSmppChargingType(Enum.valueOf(ChargingType.class, options[3]));
			} else if (parName.equals("txsipcharging")) {
				smscPropertiesManagement.setTxSipChargingType(Enum.valueOf(ChargingType.class, options[3]));
			} else if (parName.equals("diameterdestrealm")) {
				String val = options[3];
				smscPropertiesManagement.setDiameterDestRealm(val);
			} else if (parName.equals("diameterdesthost")) {
				String val = options[3];
				smscPropertiesManagement.setDiameterDestHost(val);
			} else if (parName.equals("diameterdestport")) {
				int val = Integer.parseInt(options[3]);
				smscPropertiesManagement.setDiameterDestPort(val);
			} else if (parName.equals("diameterusername")) {
				String val = options[3];
				smscPropertiesManagement.setDiameterUserName(val);

			} else {
				return SMSCOAMMessages.INVALID_COMMAND;
			}
		} catch (IllegalArgumentException e) {
			return String.format(SMSCOAMMessages.ILLEGAL_ARGUMENT, parName, e.getMessage());
		}

		return SMSCOAMMessages.PARAMETER_SUCCESSFULLY_SET;
	}

	private String manageRemove(String[] options) throws Exception {
		if (options.length < 3) {
			return SMSCOAMMessages.INVALID_COMMAND;
		}

		String parName = options[2].toLowerCase();
		try {
			if (parName.equals("esmedefaultcluster")) {
				smscPropertiesManagement.setEsmeDefaultClusterName(null);

			} else {
				return SMSCOAMMessages.INVALID_COMMAND;
			}
		} catch (IllegalArgumentException e) {
			return String.format(SMSCOAMMessages.ILLEGAL_ARGUMENT, parName, e.getMessage());
		}

		return SMSCOAMMessages.PARAMETER_SUCCESSFULLY_REMOVED;
	}

	/**
	 * Command is smsc smppserver get <variable>
	 * 
	 * @param options
	 * @return
	 * @throws Exception
	 */
	private String manageSmppServerGet(String[] options) throws Exception {

		SmppServerManagement smppServerManagement = this.smscManagement.getSmppServerManagement();

		if (options.length == 4) {
			String parName = options[3].toLowerCase();

			StringBuilder sb = new StringBuilder();
			sb.append(options[3]);
			sb.append(" = ");
			if (parName.equals("port")) {
				sb.append(smppServerManagement.getBindPort());
			} else if (parName.equals("bindtimeout")) {
				sb.append(smppServerManagement.getBindTimeout());
			} else if (parName.equals("systemid")) {
				sb.append(smppServerManagement.getSystemId());
			} else if (parName.equals("autonegotiateversion")) {
				sb.append(smppServerManagement.isAutoNegotiateInterfaceVersion());
			} else if (parName.equals("interfaceversion")) {
				sb.append(smppServerManagement.getInterfaceVersion());
			} else if (parName.equals("maxconnectionsize")) {
				sb.append(smppServerManagement.getMaxConnectionSize());
			} else if (parName.equals("defaultwindowsize")) {
				sb.append(smppServerManagement.getDefaultWindowSize());
			} else if (parName.equals("defaultwindowwaittimeout")) {
				sb.append(smppServerManagement.getDefaultWindowWaitTimeout());
			} else if (parName.equals("defaultrequestexpirytimeout")) {
				sb.append(smppServerManagement.getDefaultRequestExpiryTimeout());
			} else if (parName.equals("defaultwindowmonitorinterval")) {
				sb.append(smppServerManagement.getDefaultWindowMonitorInterval());
			} else if (parName.equals("defaultsessioncountersenabled")) {
				sb.append(smppServerManagement.isDefaultSessionCountersEnabled());
			} else {
				return SMSCOAMMessages.INVALID_COMMAND;
			}

			return sb.toString();
		} else {
			StringBuilder sb = new StringBuilder();
			sb.append("port = ");
			sb.append(smppServerManagement.getBindPort());
			sb.append("\n");

			sb.append("bind-timeout = ");
			sb.append(smppServerManagement.getBindTimeout());
			sb.append("\n");

			sb.append("system-id = ");
			sb.append(smppServerManagement.getSystemId());
			sb.append("\n");

			sb.append("auto-negotiate-version = ");
			sb.append(smppServerManagement.isAutoNegotiateInterfaceVersion());
			sb.append("\n");

			sb.append("interface-version = ");
			sb.append(smppServerManagement.getInterfaceVersion());
			sb.append("\n");

			sb.append("max-connection-size = ");
			sb.append(smppServerManagement.getMaxConnectionSize());
			sb.append("\n");

			sb.append("default-window-size = ");
			sb.append(smppServerManagement.getDefaultWindowSize());
			sb.append("\n");

			sb.append("default-window-wait-timeout = ");
			sb.append(smppServerManagement.getDefaultWindowWaitTimeout());
			sb.append("\n");

			sb.append("default-request-expiry-timeout = ");
			sb.append(smppServerManagement.getDefaultRequestExpiryTimeout());
			sb.append("\n");

			sb.append("default-window-monitor-interval = ");
			sb.append(smppServerManagement.getDefaultWindowMonitorInterval());
			sb.append("\n");

			sb.append("default-session-counters-enabled = ");
			sb.append(smppServerManagement.isDefaultSessionCountersEnabled());
			sb.append("\n");

			return sb.toString();
		}
	}

	private String manageGet(String[] options) throws Exception {
		if (options.length == 3) {
			String parName = options[2].toLowerCase();

			StringBuilder sb = new StringBuilder();
			sb.append(options[2]);
			sb.append(" = ");
			if (parName.equals("scgt")) {
				sb.append(smscPropertiesManagement.getServiceCenterGt());
			} else if (parName.equals("scssn")) {
				sb.append(smscPropertiesManagement.getServiceCenterSsn());
			} else if (parName.equals("hlrssn")) {
				sb.append(smscPropertiesManagement.getHlrSsn());
			} else if (parName.equals("mscssn")) {
				sb.append(smscPropertiesManagement.getMscSsn());
			} else if (parName.equals("maxmapv")) {
				sb.append(smscPropertiesManagement.getMaxMapVersion());
			} else if (parName.equals("defaultvalidityperiodhours")) {
				sb.append(smscPropertiesManagement.getDefaultValidityPeriodHours());
			} else if (parName.equals("maxvalidityperiodhours")) {
				sb.append(smscPropertiesManagement.getMaxValidityPeriodHours());
			} else if (parName.equals("defaultton")) {
				sb.append(smscPropertiesManagement.getDefaultTon());
			} else if (parName.equals("defaultnpi")) {
				sb.append(smscPropertiesManagement.getDefaultNpi());
			} else if (parName.equals("subscriberbusyduedelay")) {
				sb.append(smscPropertiesManagement.getSubscriberBusyDueDelay());
			} else if (parName.equals("firstduedelay")) {
				sb.append(smscPropertiesManagement.getFirstDueDelay());
			} else if (parName.equals("secondduedelay")) {
				sb.append(smscPropertiesManagement.getSecondDueDelay());
			} else if (parName.equals("maxduedelay")) {
				sb.append(smscPropertiesManagement.getMaxDueDelay());
			} else if (parName.equals("duedelaymultiplicator")) {
				sb.append(smscPropertiesManagement.getDueDelayMultiplicator());
			} else if (parName.equals("maxmessagelengthreducer")) {
				sb.append(smscPropertiesManagement.getMaxMessageLengthReducer());
			} else if (parName.equals("smppencodingforucs2")) {
				sb.append(smscPropertiesManagement.getSmppEncodingForUCS2());
			} else if (parName.equals("hosts")) {
				sb.append(smscPropertiesManagement.getHosts());
			} else if (parName.equals("keyspacename")) {
				sb.append(smscPropertiesManagement.getKeyspaceName());
			} else if (parName.equals("clustername")) {
				sb.append(smscPropertiesManagement.getClusterName());
			} else if (parName.equals("fetchperiod")) {
				sb.append(smscPropertiesManagement.getFetchPeriod());
			} else if (parName.equals("fetchmaxrows")) {
				sb.append(smscPropertiesManagement.getFetchMaxRows());
			} else if (parName.equals("maxactivitycount")) {
				sb.append(smscPropertiesManagement.getMaxActivityCount());
				// } else if (parName.equals("cdrdatabaseexportduration")) {
				// sb.append(smscPropertiesManagement.getCdrDatabaseExportDuration());
			} else if (parName.equals("esmedefaultcluster")) {
				sb.append(smscPropertiesManagement.getEsmeDefaultClusterName());
			} else if (parName.equals("smshomerouting")) {
				sb.append(smscPropertiesManagement.getSMSHomeRouting());
			} else if (parName.equals("revisesecondsonsmscstart")) {
				sb.append(smscPropertiesManagement.getReviseSecondsOnSmscStart());
			} else if (parName.equals("processingsmssettimeout")) {
				sb.append(smscPropertiesManagement.getProcessingSmsSetTimeout());
			} else if (parName.equals("generatereceiptcdr")) {
				sb.append(smscPropertiesManagement.getGenerateReceiptCdr());

			} else if (parName.equals("mocharging")) {
				sb.append(smscPropertiesManagement.isMoCharging());
			} else if (parName.equals("txsmppcharging")) {
				sb.append(smscPropertiesManagement.getTxSmppChargingType());
			} else if (parName.equals("txsipcharging")) {
				sb.append(smscPropertiesManagement.getTxSipChargingType());
			} else if (parName.equals("diameterdestrealm")) {
				sb.append(smscPropertiesManagement.getDiameterDestRealm());
			} else if (parName.equals("diameterdesthost")) {
				sb.append(smscPropertiesManagement.getDiameterDestHost());
			} else if (parName.equals("diameterdestport")) {
				sb.append(smscPropertiesManagement.getDiameterDestPort());
			} else if (parName.equals("diameterusername")) {
				sb.append(smscPropertiesManagement.getDiameterUserName());

			} else {
				return SMSCOAMMessages.INVALID_COMMAND;
			}

			return sb.toString();
		} else {
			StringBuilder sb = new StringBuilder();
			sb.append("scgt = ");
			sb.append(smscPropertiesManagement.getServiceCenterGt());
			sb.append("\n");

			sb.append("scssn = ");
			sb.append(smscPropertiesManagement.getServiceCenterSsn());
			sb.append("\n");

			sb.append("hlrssn = ");
			sb.append(smscPropertiesManagement.getHlrSsn());
			sb.append("\n");

			sb.append("mscssn = ");
			sb.append(smscPropertiesManagement.getMscSsn());
			sb.append("\n");

			sb.append("maxmapv = ");
			sb.append(smscPropertiesManagement.getMaxMapVersion());
			sb.append("\n");

			sb.append("defaultValidityPeriodHours = ");
			sb.append(smscPropertiesManagement.getDefaultValidityPeriodHours());
			sb.append("\n");

			sb.append("maxValidityPeriodHours = ");
			sb.append(smscPropertiesManagement.getMaxValidityPeriodHours());
			sb.append("\n");

			sb.append("defaultTon = ");
			sb.append(smscPropertiesManagement.getDefaultTon());
			sb.append("\n");

			sb.append("defaultNpi = ");
			sb.append(smscPropertiesManagement.getDefaultNpi());
			sb.append("\n");

			sb.append("subscriberBusyDueDelay = ");
			sb.append(smscPropertiesManagement.getSubscriberBusyDueDelay());
			sb.append("\n");

			sb.append("firstDueDelay = ");
			sb.append(smscPropertiesManagement.getFirstDueDelay());
			sb.append("\n");

			sb.append("secondDueDelay = ");
			sb.append(smscPropertiesManagement.getSecondDueDelay());
			sb.append("\n");

			sb.append("maxDueDelay = ");
			sb.append(smscPropertiesManagement.getMaxDueDelay());
			sb.append("\n");

			sb.append("dueDelayMultiplicator = ");
			sb.append(smscPropertiesManagement.getDueDelayMultiplicator());
			sb.append("\n");

			sb.append("maxMessageLengthReducer = ");
			sb.append(smscPropertiesManagement.getMaxMessageLengthReducer());
			sb.append("\n");

			sb.append("smppEncodingForUCS2 = ");
			sb.append(smscPropertiesManagement.getSmppEncodingForUCS2());
			sb.append("\n");

			sb.append("hosts = ");
			sb.append(smscPropertiesManagement.getHosts());
			sb.append("\n");

			sb.append("keyspaceName = ");
			sb.append(smscPropertiesManagement.getKeyspaceName());
			sb.append("\n");

			sb.append("maxActivityCount = ");
			sb.append(smscPropertiesManagement.getMaxActivityCount());
			sb.append("\n");

			sb.append("fetchPeriod = ");
			sb.append(smscPropertiesManagement.getFetchPeriod());
			sb.append("\n");

			sb.append("fetchMaxRows = ");
			sb.append(smscPropertiesManagement.getFetchMaxRows());
			sb.append("\n");

			sb.append("maxActivityCount = ");
			sb.append(smscPropertiesManagement.getMaxActivityCount());
			sb.append("\n");

			// sb.append("cdrDatabaseExportDuration = ");
			// sb.append(smscPropertiesManagement.getCdrDatabaseExportDuration());
			// sb.append("\n");

			sb.append("esmedefaultcluster = ");
			sb.append(smscPropertiesManagement.getEsmeDefaultClusterName());
			sb.append("\n");

			sb.append("smshomerouting = ");
			sb.append(smscPropertiesManagement.getSMSHomeRouting());
			sb.append("\n");

			sb.append("revisesecondsonsmscstart = ");
			sb.append(smscPropertiesManagement.getReviseSecondsOnSmscStart());
			sb.append("\n");

			sb.append("processingsmssettimeout = ");
			sb.append(smscPropertiesManagement.getProcessingSmsSetTimeout());
			sb.append("\n");

			sb.append("generatereceiptcdr = ");
			sb.append(smscPropertiesManagement.getGenerateReceiptCdr());
			sb.append("\n");

			sb.append("mocharging = ");
			sb.append(smscPropertiesManagement.isMoCharging());
			sb.append("\n");

			sb.append("txsmppcharging = ");
			sb.append(smscPropertiesManagement.getTxSmppChargingType());
			sb.append("\n");

			sb.append("txsipcharging = ");
			sb.append(smscPropertiesManagement.getTxSipChargingType());
			sb.append("\n");

			sb.append("diameterdestrealm = ");
			sb.append(smscPropertiesManagement.getDiameterDestRealm());
			sb.append("\n");

			sb.append("diameterdesthost = ");
			sb.append(smscPropertiesManagement.getDiameterDestHost());
			sb.append("\n");

			sb.append("diameterdestport = ");
			sb.append(smscPropertiesManagement.getDiameterDestPort());
			sb.append("\n");

			sb.append("diameterusername = ");
			sb.append(smscPropertiesManagement.getDiameterUserName());
			sb.append("\n");

			// private int defaultValidityPeriodHours = 3 * 24;
			// private int maxValidityPeriodHours = 10 * 24;
			// private int defaultTon = 1;
			// private int defaultNpi = 1;

			// private int subscriberBusyDueDelay = 60 * 2;
			// private int firstDueDelay = 60;
			// private int secondDueDelay = 60 * 5;
			// private int maxDueDelay = 3600 * 24;
			// private int dueDelayMultiplicator = 200;
			// private int maxMessageLengthReducer = 6;

			// private String hosts = "127.0.0.1:9160";
			// private String keyspaceName = "TelestaxSMSC";
			// private String clusterName = "TelestaxSMSC";

			// private long fetchPeriod = 5000;
			// private int fetchMaxRows = 100;
			// private int maxActivityCount = 500;

			return sb.toString();
		}
	}

	/**
	 * Command is smsc esme start <name>
	 * 
	 * @param args
	 * @return
	 * @throws Exception
	 */
	private String startEsme(String[] args) throws Exception {
		if (args.length != 4) {
			return SMSCOAMMessages.INVALID_COMMAND;
		}

		SmppBindType smppBindType = SmppBindType.TRANSCEIVER;

		if (args.length == 5) {
			smppBindType = SmppBindType.valueOf(args[4]);
		}

		if (smppBindType == null) {
			throw new Exception(String.format(SMSCOAMMessages.INVALID_SMPP_BIND_TYPE, args[4]));
		}

		this.smscManagement.getEsmeManagement().startEsme(args[3]);

		return String.format(SMSCOAMMessages.ESME_START_SUCCESSFULL, args[3]);
	}

	/**
	 * Command is smsc esme stop <name>
	 * 
	 * @param args
	 * @return
	 * @throws Exception
	 */
	private String stopEsme(String[] args) throws Exception {
		if (args.length != 4) {
			return SMSCOAMMessages.INVALID_COMMAND;
		}

		this.smscManagement.getEsmeManagement().stopEsme(args[3]);

		return String.format(SMSCOAMMessages.ESME_STOP_SUCCESSFULL, args[3]);
	}

	/**
	 * smsc databaseRule update <address> <systemId> <SMPP|SIP>
	 * 
	 * @param args
	 * @return
	 */
	private String databaseRuleUpdate(String[] args) throws Exception {
		DatabaseSmsRoutingRule smsRoutingRule = (DatabaseSmsRoutingRule) this.smscManagement.getSmsRoutingRule();

		if (args.length < 5 || args.length > 6) {
			return SMSCOAMMessages.INVALID_COMMAND;
		}

		String address = args[3];
		if (address == null) {
			return SMSCOAMMessages.INVALID_COMMAND;
		}

		String systemId = args[4];
		if (systemId == null) {
			return SMSCOAMMessages.INVALID_COMMAND;
		}

		SmsRoutingRuleType smsRoutingRuleType = SmsRoutingRuleType.SMPP;
		if (args.length == 6) {
			smsRoutingRuleType = SmsRoutingRuleType.valueOf(args[5]);
		}

		if (smsRoutingRuleType == null) {
			return SMSCOAMMessages.INVALID_COMMAND;
		}
		smsRoutingRule.updateDbSmsRoutingRule(smsRoutingRuleType, address, systemId);
		return String.format(SMSCOAMMessages.UPDATE_DATABASE_RULE_SUCCESSFULL, address);
	}

	/**
	 * smsc databaseRule delete <address> <SMPP|SIP>
	 * 
	 * @param args
	 * @return
	 */
	private String databaseRuleDelete(String[] args) throws Exception {
		DatabaseSmsRoutingRule smsRoutingRule = (DatabaseSmsRoutingRule) this.smscManagement.getSmsRoutingRule();

		if (args.length < 4 || args.length > 5) {
			return SMSCOAMMessages.INVALID_COMMAND;
		}

		String address = args[3];
		if (address == null) {
			return SMSCOAMMessages.INVALID_COMMAND;
		}

		SmsRoutingRuleType smsRoutingRuleType = SmsRoutingRuleType.SMPP;
		if (args.length == 5) {
			smsRoutingRuleType = SmsRoutingRuleType.valueOf(args[4]);
		}

		if (smsRoutingRuleType == null) {
			return SMSCOAMMessages.INVALID_COMMAND;
		}

		smsRoutingRule.deleteDbSmsRoutingRule(smsRoutingRuleType, address);
		return String.format(SMSCOAMMessages.DELETE_DATABASE_RULE_SUCCESSFULL, address);
	}

	/**
	 * smsc databaseRule get <address> <SMPP|SIP>
	 * 
	 * @param args
	 * @return
	 */
	private String databaseRuleGet(String[] args) throws Exception {
		DatabaseSmsRoutingRule smsRoutingRule = (DatabaseSmsRoutingRule) this.smscManagement.getSmsRoutingRule();

		if (args.length < 4 || args.length > 5) {
			return SMSCOAMMessages.INVALID_COMMAND;
		}

		String address = args[3];
		if (address == null) {
			return SMSCOAMMessages.INVALID_COMMAND;
		}

		SmsRoutingRuleType smsRoutingRuleType = SmsRoutingRuleType.SMPP;
		if (args.length == 5) {
			smsRoutingRuleType = SmsRoutingRuleType.valueOf(args[4]);
		}

		if (smsRoutingRuleType == null) {
			return SMSCOAMMessages.INVALID_COMMAND;
		}

		DbSmsRoutingRule res = smsRoutingRule.getSmsRoutingRule(smsRoutingRuleType, address);
		if (res == null) {
			return String.format(SMSCOAMMessages.NO_ROUTING_RULE_DEFINED_YET, address, smsRoutingRuleType.name());
		}
		return res.toString();
	}

	/**
	 * smsc databaseRule getRange <SMPP|SIP> <address> or smsc databaseRule
	 * getRange <SMPP|SIP>
	 * 
	 * @param args
	 * @return
	 */
	private String databaseRuleGetRange(String[] args) throws Exception {
		DatabaseSmsRoutingRule smsRoutingRule = (DatabaseSmsRoutingRule) this.smscManagement.getSmsRoutingRule();

		if (args.length < 4 || args.length > 5) {
			return SMSCOAMMessages.INVALID_COMMAND;
		}

		SmsRoutingRuleType smsRoutingRuleType = SmsRoutingRuleType.valueOf(args[3]);

		if (smsRoutingRuleType == null) {
			return SMSCOAMMessages.INVALID_COMMAND;
		}

		String address = null;
		if (args.length == 5) {
			address = args[4];
			if (address == null) {
				return SMSCOAMMessages.INVALID_COMMAND;
			}
		}

		List<DbSmsRoutingRule> res = smsRoutingRule.getSmsRoutingRulesRange(smsRoutingRuleType, address);

		StringBuilder sb = new StringBuilder();
		int i1 = 0;
		for (DbSmsRoutingRule rr : res) {
			if (i1 == 0)
				i1 = 1;
			else
				sb.append("\n");
			sb.append(rr.toString());
		}
		return sb.toString();

	}

	/**
	 * smsc archive generateCdr <timeFrom> <timeTo>
	 * 
	 * @param args
	 * @return
	 */
	private String archiveGenerateCdr(String[] args) throws Exception {
		if (args.length < 5 || args.length > 5) {
			return SMSCOAMMessages.INVALID_COMMAND;
		}

		String timeFromS = args[3];
		String timeToS = args[4];
		if (timeFromS == null || timeToS == null) {
			return SMSCOAMMessages.INVALID_COMMAND;
		}

		SimpleDateFormat df = new SimpleDateFormat();
		Date timeFrom = df.parse(timeFromS);
		if (timeFrom == null)
			return SMSCOAMMessages.BAD_FORMATTED_FROM_FIELD;
		Date timeTo = df.parse(timeToS);
		if (timeTo == null)
			return SMSCOAMMessages.BAD_FORMATTED_TO_FIELD;

		ArchiveSms.getInstance().makeCdrDatabaseManualExport(timeFrom, timeTo);
		return SMSCOAMMessages.ACCEPTED_ARCHIVE_GENERATE_CDR_SUCCESSFULL;
	}

	public String getStat(String[] args) {
		StringBuilder sb = new StringBuilder();

		SmscStatProvider smscStatProvider = SmscStatProvider.getInstance();
		sb.append("Stat: ");
		sb.append("Time: ");
		sb.append(new Date());
		sb.append(", MessageInProcess: ");
		sb.append(smscStatProvider.getMessageInProcess());
		sb.append(", MessageId: ");
		sb.append(smscStatProvider.getCurrentMessageId());
		sb.append(", MessageScheduledTotal: ");
		sb.append(smscStatProvider.getMessageScheduledTotal());
		sb.append(", DueSlotProcessingLag: ");
		sb.append(smscStatProvider.getDueSlotProcessingLag());
		sb.append(", Param1: ");
		sb.append(smscStatProvider.getParam1());
		sb.append(", Param2: ");
		sb.append(smscStatProvider.getParam2());
		sb.append(", SmscStartTime: ");
		sb.append(smscStatProvider.getSmscStartTime());

		return sb.toString();
	}

	public String execute(String[] args) {
		if (args[0].equals("smsc")) {
			return this.executeSmsc(args);
		}
		return SMSCOAMMessages.INVALID_COMMAND;
	}

	@Override
	public boolean handles(String command) {
		return "smsc".equals(command);
	}

	public static void main(String[] args) throws Exception {
		String command = "smsc mapcache get 1234567";
		SMSCShellExecutor exec = new SMSCShellExecutor();
		exec.getMapVersionCache(command.split(" "));
	}

}
