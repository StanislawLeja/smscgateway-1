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

import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Logger;
import org.mobicents.ss7.management.console.ShellExecutor;

import com.cloudhopper.smpp.SmppBindType;
import com.cloudhopper.smpp.SmppConstants;
import com.cloudhopper.smpp.SmppSession;
import com.cloudhopper.smpp.type.Address;

/**
 * @author amit bhayani
 * 
 */
public class SMSCShellExecutor implements ShellExecutor {

	private static final Logger logger = Logger.getLogger(SMSCShellExecutor.class);

	private SmscManagement smscManagement;

	private static final SmscPropertiesManagement smscPropertiesManagement = SmscPropertiesManagement.getInstance();

	public SMSCShellExecutor() {

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
		byte esmeTonType = 0;
		byte esmeNpiType = 0;
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
			} else {
				return SMSCOAMMessages.INVALID_COMMAND;
			}

		}

		if ((SmppBindType.TRANSCEIVER == smppBindType || SmppBindType.RECEIVER == smppBindType)
				&& esmeAddrRange == null) {
			return SMSCOAMMessages.NULL_ESME_ADDRESS_RANGE;
		}
		Address address = new Address(esmeTonType, esmeNpiType, esmeAddrRange);
		Esme esme = this.smscManagement.getEsmeManagement().createEsme(name, systemId, password, host, intPort,
				smppBindType, systemType, smppVersionType, address, smppSessionType, windowSize, connectTimeout,
				requestExpiryTimeout, windowMonitorInterval, windowWaitTimeout, clusterName, countersEnabled,
				enquireLinkDelay);
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
			} else if (args[1].equals("set")) {
				return this.manageSet(args);
			} else if (args[1].equals("get")) {
				return this.manageGet(args);
			}
			return SMSCOAMMessages.INVALID_COMMAND;
		} catch (Exception e) {
			logger.error(String.format("Error while executing comand %s", Arrays.toString(args)), e);
			return e.getMessage();
		}
	}

	private String manageSet(String[] options) throws Exception {
		if (options.length < 4) {
			return SMSCOAMMessages.INVALID_COMMAND;
		}

		String parName = options[2].toLowerCase();
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
		} else {
			return SMSCOAMMessages.INVALID_COMMAND;
		}

		return SMSCOAMMessages.PARAMETER_SUCCESSFULLY_SET;
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

}
