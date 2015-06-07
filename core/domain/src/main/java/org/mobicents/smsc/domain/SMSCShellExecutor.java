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

package org.mobicents.smsc.domain;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javolution.util.FastMap;

import org.apache.log4j.Logger;
import org.mobicents.protocols.ss7.indicator.GlobalTitleIndicator;
import org.mobicents.protocols.ss7.map.api.MAPApplicationContextVersion;
import org.mobicents.smsc.cassandra.SmsRoutingRuleType;
import org.mobicents.smsc.library.DbSmsRoutingRule;
import org.mobicents.smsc.library.Sms;
import org.mobicents.smsc.smpp.SmppEncoding;
import org.mobicents.ss7.management.console.ShellExecutor;

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

	/**
	 * Command is smsc sip modify name cluster-name <clusterName> host <ip> port
	 * <port> routing-ton <routing address ton> routing-npi <routing address
	 * npi> routing-range <routing address range> counters-enabled <true |
	 * false> charging-enabled <true | false> networkid <network-d>
	 * 
	 * @param args
	 * @return
	 * @throws Exception
	 */
	private String modifySip(String[] args) throws Exception {
		if (args.length < 6 || args.length > 22) {
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
            } else if (command.equals("networkid")) {
                sip.setNetworkId(Integer.parseInt(value));
                success = true;
			} else if (command.equals("routing-ton")) {
				sip.setRoutingTon(Integer.parseInt(value));
				success = true;
			} else if (command.equals("routing-npi")) {
				sip.setRoutingNpi(Integer.parseInt(value));
				success = true;
			} else if (command.equals("routing-range")) {
				sip.setRoutingAddressRange(value);
				success = true;
			} else if (command.equals("counters-enabled")) {
				sip.setCountersEnabled(Boolean.parseBoolean(value));
				success = true;
			} else if (command.equals("charging-enabled")) {
				sip.setChargingEnabled(Boolean.parseBoolean(value));
				success = true;
			} else if (command.equals("networkid")) {
				sip.setNetworkId(Integer.parseInt(value));
				success = true;
			}
		}// while

		if (!success) {
			return SMSCOAMMessages.INVALID_COMMAND;
		}

		return String.format(SMSCOAMMessages.SIP_MODIFY_SUCCESS, name);
	}

    /**
     * Command is mproc add <id> desttonmask <destination type of number>
     * destnpimask <destination numbering plan indicator> destdigmask <regular
     * expression - destination number digits mask> originatingmask <mo | hr |
     * esme | sip> networkidmask <networkId value> newnetworkid <new networkId
     * value> newdestton <new destination type of number> newdestnpi <new
     * destination numbering plan indicator> addestdigprefix <prefix> makecopy
     * <false | true>
     * 
     * @param args
     * @return
     * @throws Exception
     */
    private String addMProc(String[] args) throws Exception {
        if (args.length < 4 || args.length > 26) {
            return SMSCOAMMessages.INVALID_COMMAND;
        }

        // create new MProcRule
        int id = Integer.parseInt(args[3]);

        MProcManagement mProcManagement = MProcManagement.getInstance();
        MProcRule mProcRule = mProcManagement.getMProcRuleById(id);
        if (mProcRule != null) {
            return String.format(MProcRuleOamMessages.CREATE_MPROC_RULE_FAIL_ALREADY_EXIST, id);
        }

        int count = 4;
        String command;

        boolean success = false;
        int destTonMask = -1;
        int destNpiMask = -1;
        String destDigMask = "-1";
        String originatingMask = "-1";
        int networkIdMask = -1;
        int newNetworkId = -1;
        int newDestTon = -1;
        int newDestNpi = -1;
        String addDestDigPrefix = "-1";
        boolean makeCopy = false;
        while (count < args.length) {
            command = args[count++];
            if (count < args.length) {
                String value = args[count++];
                if (command.equals("desttonmask")) {
                    destTonMask = Integer.parseInt(value);
                } else if (command.equals("destnpimask")) {
                    destNpiMask = Integer.parseInt(value);
                } else if (command.equals("destdigmask")) {
                    destDigMask = value;
                } else if (command.equals("originatingmask")) {
                    originatingMask = value;
                } else if (command.equals("networkidmask")) {
                    networkIdMask = Integer.parseInt(value);
                } else if (command.equals("newnetworkid")) {
                    newNetworkId = Integer.parseInt(value);
                    success = true;
                } else if (command.equals("newdestton")) {
                    newDestTon = Integer.parseInt(value);
                    success = true;
                } else if (command.equals("newdestnpi")) {
                    newDestNpi = Integer.parseInt(value);
                    success = true;
                } else if (command.equals("adddestdigprefix")) {
                    addDestDigPrefix = value;
                    success = true;

                } else if (command.equals("makecopy")) {
                    makeCopy = Boolean.parseBoolean(value);
                    success = true;
                }
            }
        }// while

        if (!success) {
            return SMSCOAMMessages.INVALID_COMMAND;
        }

        mProcManagement.createMProcRule(id, destTonMask, destNpiMask, destDigMask, originatingMask, networkIdMask, newNetworkId, newDestTon, newDestNpi,
                addDestDigPrefix, makeCopy);

        return String.format(SMSCOAMMessages.MPROC_CREATE_SUCCESS, id);
    }

    /**
     * Command is mproc modify <id> desttonmask <destination type of number>
     * destnpimask <destination numbering plan indicator> destdigmask <regular
     * expression - destination number digits mask> originatingmask <mo | hr |
     * esme | sip> networkidmask <networkId value> newnetworkid <new networkId
     * value> newdestton <new destination type of number> newdestnpi <new
     * destination numbering plan indicator> addestdigprefix <prefix> makecopy
     * <false | true>
     * 
     * @param args
     * @return
     * @throws Exception
     */
    private String modifyMProc(String[] args) throws Exception {
        if (args.length < 4 || args.length > 26) {
            return SMSCOAMMessages.INVALID_COMMAND;
        }

        // modify of existing MProcRule
        int id = Integer.parseInt(args[3]);

        MProcManagement mProcManagement = MProcManagement.getInstance();
        MProcRule mProcRule = mProcManagement.getMProcRuleById(id);
        if (mProcRule == null) {
            return String.format(MProcRuleOamMessages.MODIFY_MPROC_RULE_FAIL_NOT_EXIST, id);
        }

        int count = 4;
        String command;

        boolean success = false;
        while (count < args.length) {
            command = args[count++];
            if (count < args.length) {
                String value = args[count++];
                if (command.equals("desttonmask")) {
                    int val = Integer.parseInt(value);
                    mProcRule.setDestTonMask(val);
                    success = true;
                } else if (command.equals("destnpimask")) {
                    int val = Integer.parseInt(value);
                    mProcRule.setDestNpiMask(val);
                    success = true;
                } else if (command.equals("destdigmask")) {
                    mProcRule.setDestDigMask(value);
                    success = true;
                } else if (command.equals("originatingmask")) {
                    Sms.OriginationType originatingMask = Enum.valueOf(Sms.OriginationType.class, value);
                    mProcRule.setOriginatingMask(originatingMask);
                    success = true;
                } else if (command.equals("networkidmask")) {
                    int val = Integer.parseInt(value);
                    mProcRule.setNetworkIdMask(val);
                    success = true;
                } else if (command.equals("newnetworkid")) {
                    int val = Integer.parseInt(value);
                    mProcRule.setNewNetworkId(val);
                    success = true;
                } else if (command.equals("newdestton")) {
                    int val = Integer.parseInt(value);
                    mProcRule.setNewDestTon(val);
                    success = true;
                } else if (command.equals("newdestnpi")) {
                    int val = Integer.parseInt(value);
                    mProcRule.setNewDestNpi(val);
                    success = true;
                } else if (command.equals("adddestdigprefix")) {
                    mProcRule.setAddDestDigPrefix(value);
                    success = true;
                } else if (command.equals("makecopy")) {
                    boolean val = Boolean.parseBoolean(value);
                    mProcRule.setMakeCopy(val);
                    success = true;
                }
            }
        }// while

        if (!success) {
            return SMSCOAMMessages.INVALID_COMMAND;
        }

        return String.format(SMSCOAMMessages.MPROC_MODIFY_SUCCESS, id);
    }

    /**
     * Command is mproc destroy <id>
     * 
     * @param args
     * @return
     * @throws Exception
     */
    private String removeMProc(String[] args) throws Exception {
        if (args.length < 4 || args.length > 4) {
            return SMSCOAMMessages.INVALID_COMMAND;
        }

        // destroy of existing MProcRule
        int id = Integer.parseInt(args[3]);

        MProcManagement mProcManagement = MProcManagement.getInstance();
        MProcRule mProcRule = mProcManagement.getMProcRuleById(id);
        if (mProcRule == null) {
            return String.format(MProcRuleOamMessages.DESTROY_MPROC_RULE_FAIL_NOT_EXIST, id);
        }

        mProcManagement.destroyMProcRule(id);

        return String.format(SMSCOAMMessages.MPROC_DESTROY_SUCCESS, id);
    }

    /**
     * Command is "show destroy <id>" or "show destroy"
     * 
     * @param args
     * @return
     * @throws Exception
     */
    private String showMProc(String[] args) throws Exception {
        if (args.length < 3 || args.length > 4) {
            return SMSCOAMMessages.INVALID_COMMAND;
        }

        // show of existing MProcRule / all MProcRule
        MProcManagement mProcManagement = MProcManagement.getInstance();
        if (args.length == 3) {
            ArrayList<MProcRule> lst = mProcManagement.getMProcRule();

            StringBuilder sb = new StringBuilder();
            if (lst.size() == 0) {
                sb.append(SMSCOAMMessages.MPROC_NO_RULES);
            } else {
                for (MProcRule rule : lst) {
                    sb.append(rule.toString());
                    sb.append("\n");
                }
            }
            return sb.toString();
        } else {
            int id = Integer.parseInt(args[3]);
            MProcRule rule = mProcManagement.getMProcRuleById(id);
            if (rule == null) {
                return String.format(SMSCOAMMessages.MPROC_NO_RULE, id);
            }

            return rule.toString();
        }
    }

	private String executeSmsc(String[] args) {
		try {
			if (args.length < 2 || args.length > 50) {
				// any command will have atleast 3 args
				return SMSCOAMMessages.INVALID_COMMAND;
			}

			if (args[1] == null) {
				return SMSCOAMMessages.INVALID_COMMAND;
			}

            if (args[1].equals("sip")) {
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

            } else if (args[1].equals("mproc")) {
                String rasCmd = args[2];
                if (rasCmd == null) {
                    return SMSCOAMMessages.INVALID_COMMAND;
                }

                if (rasCmd.equals("add")) {
                    return this.addMProc(args);
                } else if (rasCmd.equals("modify")) {
                    return this.modifyMProc(args);
                } else if (rasCmd.equals("remove")) {
                    return this.removeMProc(args);
                } else if (rasCmd.equals("show")) {
                    return this.showMProc(args);
                }

                return SMSCOAMMessages.INVALID_COMMAND;

			} else if (args[1].equals("set")) {
				return this.manageSet(args);
			} else if (args[1].equals("get")) {
				return this.manageGet(args);
			} else if (args[1].equals("remove")) {
				return this.manageRemove(args);
			} else if (args[1].toLowerCase().equals("databaserule")) {
                if (args.length < 3)
                    return SMSCOAMMessages.INVALID_COMMAND;

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
                if (args.length < 3)
                    return SMSCOAMMessages.INVALID_COMMAND;

				String rasCmd = args[2];
				if (rasCmd == null) {
					return SMSCOAMMessages.INVALID_COMMAND;
				}

				if (rasCmd.equals("generatecdr")) {
					return this.archiveGenerateCdr(args);
				}

				return SMSCOAMMessages.INVALID_COMMAND;
			} else if (args[1].toLowerCase().equals("mapcache")) {
                if (args.length < 3)
                    return SMSCOAMMessages.INVALID_COMMAND;

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
                if (args.length < 3)
                    return SMSCOAMMessages.INVALID_COMMAND;

				String rasCmd = args[2];

				if (rasCmd == null) {
					return SMSCOAMMessages.INVALID_COMMAND;
				}

				if (rasCmd.equals("get")) {
					return this.getStat(args);
				}

				return SMSCOAMMessages.INVALID_COMMAND;

            } else if (args[1].toLowerCase().equals("updateccmccmnstable")) {
                return this.updateCcMccmnstable(args);
            } else if (args[1].toLowerCase().equals("hrccmccmnc")) {
                return this.ccMccmnsValueUpdate(args);
			}

			return SMSCOAMMessages.INVALID_COMMAND;
		} catch (Throwable e) {
			logger.error(String.format("Error while executing comand %s", Arrays.toString(args)), e);
			return e.toString();
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

		FastMap<String, MapVersionNeg> cache = mapVersionCache.getMAPApplicationContextVersionCache();
		if (cache.size() == 0) {
			return SMSCOAMMessages.MAP_VERSION_CACHE_NOT_FOUND;
		}

		StringBuffer sb = new StringBuffer();
		for (FastMap.Entry<String, MapVersionNeg> e = cache.head(), end = cache.tail(); (e = e.getNext()) != end;) {
			sb.append(e.getKey()).append(MAP_CACHE_KEY_VALUE_SEPARATOR).append(e.getValue().getCurVersion()).append(LINE_SEPARATOR);
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

		mapVersionCache.forceMAPApplicationContextVersion(msisdn, mapApplicationContextVersion);

		return SMSCOAMMessages.MAP_VERSION_CACHE_SUCCESSFULLY_SET;
	}

	private String manageSet(String[] options) throws Exception {
		if (options.length < 4) {
			return SMSCOAMMessages.INVALID_COMMAND;
		}

		String parName = options[2].toLowerCase();
		try {
			if (parName.equals("scgt")) {
			    String gt = options[3];
                if (options.length >= 6 && options[4].equals("networkid")) {
                    int val = Integer.parseInt(options[5]);
                    smscPropertiesManagement.setServiceCenterGt(val, gt);
                } else {
                    smscPropertiesManagement.setServiceCenterGt(gt);
                }
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
            } else if (parName.equals("gti")) {
                String val = options[3];
                switch (val) {
                case "0001":
                    smscPropertiesManagement
                            .setGlobalTitleIndicator(GlobalTitleIndicator.GLOBAL_TITLE_INCLUDES_NATURE_OF_ADDRESS_INDICATOR_ONLY);
                    break;
                case "0010":
                    smscPropertiesManagement
                            .setGlobalTitleIndicator(GlobalTitleIndicator.GLOBAL_TITLE_INCLUDES_TRANSLATION_TYPE_ONLY);
                    break;
                case "0011":
                    smscPropertiesManagement
                            .setGlobalTitleIndicator(GlobalTitleIndicator.GLOBAL_TITLE_INCLUDES_TRANSLATION_TYPE_NUMBERING_PLAN_AND_ENCODING_SCHEME);
                    break;
                case "0100":
                    smscPropertiesManagement
                            .setGlobalTitleIndicator(GlobalTitleIndicator.GLOBAL_TITLE_INCLUDES_TRANSLATION_TYPE_NUMBERING_PLAN_ENCODING_SCHEME_AND_NATURE_OF_ADDRESS);
                    break;
                default:
                    return SMSCOAMMessages.GLOBAL_TYTLE_INDICATOR_BAD_VALUES;
                }
            } else if (parName.equals("tt")) {
                int val = Integer.parseInt(options[3]);
                smscPropertiesManagement.setTranslationType(val);

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
            } else if (parName.equals("smppencodingforgsm7")) {
                String s1 = options[3].toLowerCase();
                if (s1.equals("utf8")) {
                    smscPropertiesManagement.setSmppEncodingForGsm7(SmppEncoding.Utf8);
                } else if (s1.equals("unicode")) {
                    smscPropertiesManagement.setSmppEncodingForGsm7(SmppEncoding.Unicode);
                } else if (s1.equals("gsm7")) {
                    smscPropertiesManagement.setSmppEncodingForGsm7(SmppEncoding.Gsm7);
                } else {
                    return String.format(SMSCOAMMessages.ILLEGAL_ARGUMENT, "SmppEncodingForGsm7 value",
                            "UTF8 or UNICODE or GSM7 are possible");
                }
            } else if (parName.equals("smppencodingforucs2")) {
                String s1 = options[3].toLowerCase();
                if (s1.equals("utf8")) {
                    smscPropertiesManagement.setSmppEncodingForUCS2(SmppEncoding.Utf8);
                } else if (s1.equals("unicode")) {
                    smscPropertiesManagement.setSmppEncodingForUCS2(SmppEncoding.Unicode);
                } else if (s1.equals("gsm7")) {
                    smscPropertiesManagement.setSmppEncodingForUCS2(SmppEncoding.Gsm7);
                } else {
                    return String.format(SMSCOAMMessages.ILLEGAL_ARGUMENT, "SmppEncodingForUCS2 value",
                            "UTF8 or UNICODE or GSM7 are possible");
                }
//            } else if (parName.equals("hosts")) {
//                String val = options[3];
//                smscPropertiesManagement.setHosts(val);
            } else if (parName.equals("dbhosts")) {
                String val = options[3];
                smscPropertiesManagement.setDbHosts(val);
            } else if (parName.equals("dbport")) {
                int val = Integer.parseInt(options[3]);
                smscPropertiesManagement.setDbPort(val);
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
            } else if (parName.equals("correlationidlivetime")) {
                int val = Integer.parseInt(options[3]);
                smscPropertiesManagement.setCorrelationIdLiveTime(val);
			} else if (parName.equals("revisesecondsonsmscstart")) {
				int val = Integer.parseInt(options[3]);
				smscPropertiesManagement.setReviseSecondsOnSmscStart(val);
			} else if (parName.equals("processingsmssettimeout")) {
				int val = Integer.parseInt(options[3]);
				smscPropertiesManagement.setProcessingSmsSetTimeout(val);
            } else if (parName.equals("generatereceiptcdr")) {
                smscPropertiesManagement.setGenerateReceiptCdr(Boolean.parseBoolean(options[3]));
            } else if (parName.equals("receiptsdisabling")) {
                smscPropertiesManagement.setReceiptsDisabling(Boolean.parseBoolean(options[3]));
            } else if (parName.equals("orignetworkidforreceipts")) {
                smscPropertiesManagement.setOrigNetworkIdForReceipts(Boolean.parseBoolean(options[3]));
            } else if (parName.equals("generatecdr")) {
                int val = Integer.parseInt(options[3]);
                smscPropertiesManagement.setGenerateCdrInt(val);
            } else if (parName.equals("generatearchivetable")) {
                int val = Integer.parseInt(options[3]);
                smscPropertiesManagement.setGenerateArchiveTableInt(val);

            } else if (parName.equals("storeandforwordmode")) {
                smscPropertiesManagement.setStoreAndForwordMode(Enum.valueOf(StoreAndForwordMode.class, options[3]));
            } else if (parName.equals("mocharging")) {
                smscPropertiesManagement.setMoCharging(Enum.valueOf(MoChargingType.class, options[3]));
            } else if (parName.equals("hrcharging")) {
                smscPropertiesManagement.setHrCharging(Enum.valueOf(MoChargingType.class, options[3]));
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
            } else if (parName.equals("removinglivetablesdays")) {
                int val = Integer.parseInt(options[3]);
                if (val == 1 || val == 2 || val < 0)
                    return SMSCOAMMessages.REMOVING_LIVE_ARCHIVE_TABLES_DAYS_BAD_VALUES;
                smscPropertiesManagement.setRemovingLiveTablesDays(val);
            } else if (parName.equals("removingarchivetablesdays")) {
                int val = Integer.parseInt(options[3]);
                if (val == 1 || val == 2 || val < 0)
                    return SMSCOAMMessages.REMOVING_LIVE_ARCHIVE_TABLES_DAYS_BAD_VALUES;
                smscPropertiesManagement.setRemovingArchiveTablesDays(val);

            } else if (parName.equals("national-language-single-shift")) {
                int val = Integer.parseInt(options[3]);
                if (val < 0 || val > 13)
                    return SMSCOAMMessages.NATIONAL_LANGUAGE_SHIFT_BAD_VALUE;
                smscPropertiesManagement.setNationalLanguageSingleShift(val);
            } else if (parName.equals("national-language-locking-shift")) {
                int val = Integer.parseInt(options[3]);
                if (val < 0 || val > 13)
                    return SMSCOAMMessages.NATIONAL_LANGUAGE_SHIFT_BAD_VALUE;
                smscPropertiesManagement.setNationalLanguageLockingShift(val);

            } else if (parName.equals("deliverypause")) {
                boolean val = Boolean.parseBoolean(options[3]);
                smscPropertiesManagement.setDeliveryPause(val);
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

	private String manageGet(String[] options) throws Exception {
		if (options.length == 3) {
			String parName = options[2].toLowerCase();

			StringBuilder sb = new StringBuilder();
			sb.append(options[2]);
			sb.append(" = ");
			if (parName.equals("scgt")) {
                sb.append("networkId=0 - GT=");
                sb.append(smscPropertiesManagement.getServiceCenterGt());
                for (Integer key : smscPropertiesManagement.getNetworkIdVsServiceCenterGt().keySet()) {
                    sb.append("\nnetworkId=");
                    sb.append(key);
                    sb.append(" - GT=");
                    sb.append(smscPropertiesManagement.getNetworkIdVsServiceCenterGt().get(key));
                }
			} else if (parName.equals("scssn")) {
			    sb.append(smscPropertiesManagement.getServiceCenterSsn());
			} else if (parName.equals("hlrssn")) {
				sb.append(smscPropertiesManagement.getHlrSsn());
			} else if (parName.equals("mscssn")) {
				sb.append(smscPropertiesManagement.getMscSsn());
			} else if (parName.equals("maxmapv")) {
				sb.append(smscPropertiesManagement.getMaxMapVersion());
            } else if (parName.equals("gti")) {
                switch (smscPropertiesManagement.getGlobalTitleIndicator()) {
                case GLOBAL_TITLE_INCLUDES_NATURE_OF_ADDRESS_INDICATOR_ONLY:
                    sb.append("0001");
                    break;
                case GLOBAL_TITLE_INCLUDES_TRANSLATION_TYPE_ONLY:
                    sb.append("0010");
                    break;
                case GLOBAL_TITLE_INCLUDES_TRANSLATION_TYPE_NUMBERING_PLAN_AND_ENCODING_SCHEME:
                    sb.append("0011");
                    break;
                case GLOBAL_TITLE_INCLUDES_TRANSLATION_TYPE_NUMBERING_PLAN_ENCODING_SCHEME_AND_NATURE_OF_ADDRESS:
                    sb.append("0100");
                    break;
                }
            } else if (parName.equals("tt")) {
                sb.append(smscPropertiesManagement.getTranslationType());

			
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
            } else if (parName.equals("smppencodingforgsm7")) {
                sb.append(smscPropertiesManagement.getSmppEncodingForGsm7());
            } else if (parName.equals("smppencodingforucs2")) {
                sb.append(smscPropertiesManagement.getSmppEncodingForUCS2());
//            } else if (parName.equals("hosts")) {
//                sb.append(smscPropertiesManagement.getHosts());
            } else if (parName.equals("dbhosts")) {
                sb.append(smscPropertiesManagement.getDbHosts());
            } else if (parName.equals("dbport")) {
                sb.append(smscPropertiesManagement.getDbPort());
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
            } else if (parName.equals("correlationidlivetime")) {
                sb.append(smscPropertiesManagement.getCorrelationIdLiveTime());
			} else if (parName.equals("revisesecondsonsmscstart")) {
				sb.append(smscPropertiesManagement.getReviseSecondsOnSmscStart());
			} else if (parName.equals("processingsmssettimeout")) {
				sb.append(smscPropertiesManagement.getProcessingSmsSetTimeout());
            } else if (parName.equals("generatereceiptcdr")) {
                sb.append(smscPropertiesManagement.getGenerateReceiptCdr());
            } else if (parName.equals("receiptsdisabling")) {
                sb.append(smscPropertiesManagement.getReceiptsDisabling());
            } else if (parName.equals("receiptsdisabling")) {
                sb.append(smscPropertiesManagement.getReceiptsDisabling());
            } else if (parName.equals("orignetworkidforreceipts")) {
                sb.append(smscPropertiesManagement.getOrigNetworkIdForReceipts());
            } else if (parName.equals("generatearchivetable")) {
                sb.append(smscPropertiesManagement.getGenerateArchiveTable().getValue());

            } else if (parName.equals("storeandforwordmode")) {
                sb.append(smscPropertiesManagement.getStoreAndForwordMode());
            } else if (parName.equals("mocharging")) {
                sb.append(smscPropertiesManagement.getMoCharging());
            } else if (parName.equals("hrcharging")) {
                sb.append(smscPropertiesManagement.getHrCharging());
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
            } else if (parName.equals("removinglivetablesdays")) {
                sb.append(smscPropertiesManagement.getRemovingLiveTablesDays());
            } else if (parName.equals("removingarchivetablesdays")) {
                sb.append(smscPropertiesManagement.getRemovingArchiveTablesDays());

            } else if (parName.equals("national-language-single-shift")) {
                sb.append(smscPropertiesManagement.getNationalLanguageSingleShift());
            } else if (parName.equals("national-language-locking-shift")) {
                sb.append(smscPropertiesManagement.getNationalLanguageLockingShift());

            } else if (parName.equals("deliverypause")) {
                sb.append(smscPropertiesManagement.isDeliveryPause());
			} else {
				return SMSCOAMMessages.INVALID_COMMAND;
			}

			return sb.toString();
		} else {
			StringBuilder sb = new StringBuilder();
			sb.append("scgt : ");
            sb.append("networkId=0 - GT=");
            sb.append(smscPropertiesManagement.getServiceCenterGt());
            for (Integer key : smscPropertiesManagement.getNetworkIdVsServiceCenterGt().keySet()) {
                if (key != null) {
                    sb.append("\nnetworkId=");
                    sb.append(key);
                    sb.append(" - GT=");
                    sb.append(smscPropertiesManagement.getNetworkIdVsServiceCenterGt().get(key));
                }
            }
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

            sb.append("gti = ");
            switch (smscPropertiesManagement.getGlobalTitleIndicator()) {
            case GLOBAL_TITLE_INCLUDES_NATURE_OF_ADDRESS_INDICATOR_ONLY:
                sb.append("0001");
                break;
            case GLOBAL_TITLE_INCLUDES_TRANSLATION_TYPE_ONLY:
                sb.append("0010");
                break;
            case GLOBAL_TITLE_INCLUDES_TRANSLATION_TYPE_NUMBERING_PLAN_AND_ENCODING_SCHEME:
                sb.append("0011");
                break;
            case GLOBAL_TITLE_INCLUDES_TRANSLATION_TYPE_NUMBERING_PLAN_ENCODING_SCHEME_AND_NATURE_OF_ADDRESS:
                sb.append("0100");
                break;
            }
            sb.append("\n");

            sb.append("tt = ");
            sb.append(smscPropertiesManagement.getTranslationType());
            sb.append("\n");

			sb.append("defaultvalidityperiodhours = ");
			sb.append(smscPropertiesManagement.getDefaultValidityPeriodHours());
			sb.append("\n");

			sb.append("maxvalidityperiodhours = ");
			sb.append(smscPropertiesManagement.getMaxValidityPeriodHours());
			sb.append("\n");

			sb.append("defaultton = ");
			sb.append(smscPropertiesManagement.getDefaultTon());
			sb.append("\n");

			sb.append("defaultnpi = ");
			sb.append(smscPropertiesManagement.getDefaultNpi());
			sb.append("\n");

			sb.append("subscriberbusyduedelay = ");
			sb.append(smscPropertiesManagement.getSubscriberBusyDueDelay());
			sb.append("\n");

			sb.append("firstduedelay = ");
			sb.append(smscPropertiesManagement.getFirstDueDelay());
			sb.append("\n");

			sb.append("secondduedelay = ");
			sb.append(smscPropertiesManagement.getSecondDueDelay());
			sb.append("\n");

			sb.append("maxduedelay = ");
			sb.append(smscPropertiesManagement.getMaxDueDelay());
			sb.append("\n");

			sb.append("duedelaymultiplicator = ");
			sb.append(smscPropertiesManagement.getDueDelayMultiplicator());
			sb.append("\n");

			sb.append("maxmessagelengthreducer = ");
			sb.append(smscPropertiesManagement.getMaxMessageLengthReducer());
			sb.append("\n");

            sb.append("smppencodingforgsm7 = ");
            sb.append(smscPropertiesManagement.getSmppEncodingForGsm7());
            sb.append("\n");

            sb.append("smppencodingforucs2 = ");
            sb.append(smscPropertiesManagement.getSmppEncodingForUCS2());
            sb.append("\n");

//            sb.append("hosts = ");
//            sb.append(smscPropertiesManagement.getHosts());
//            sb.append("\n");

            sb.append("dbhosts = ");
            sb.append(smscPropertiesManagement.getDbHosts());
            sb.append("\n");

            sb.append("dbport = ");
            sb.append(smscPropertiesManagement.getDbPort());
            sb.append("\n");

			sb.append("keyspaceName = ");
			sb.append(smscPropertiesManagement.getKeyspaceName());
			sb.append("\n");

			sb.append("maxactivitycount = ");
			sb.append(smscPropertiesManagement.getMaxActivityCount());
			sb.append("\n");

			sb.append("fetchperiod = ");
			sb.append(smscPropertiesManagement.getFetchPeriod());
			sb.append("\n");

			sb.append("fetchmaxrows = ");
			sb.append(smscPropertiesManagement.getFetchMaxRows());
			sb.append("\n");

			sb.append("maxactivitycount = ");
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

            sb.append("correlationidlivetime = ");
            sb.append(smscPropertiesManagement.getCorrelationIdLiveTime());
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

            sb.append("receiptsdisabling = ");
            sb.append(smscPropertiesManagement.getReceiptsDisabling());
            sb.append("\n");

            sb.append("orignetworkidforreceipts = ");
            sb.append(smscPropertiesManagement.getOrigNetworkIdForReceipts());
            sb.append("\n");

            sb.append("generatecdr = ");
            sb.append(smscPropertiesManagement.getGenerateCdr().getValue());
            sb.append("\n");

            sb.append("generatearchivetable = ");
            sb.append(smscPropertiesManagement.getGenerateArchiveTable().getValue());
            sb.append("\n");

            sb.append("storeandforwordmode = ");
            sb.append(smscPropertiesManagement.getStoreAndForwordMode());
            sb.append("\n");

            sb.append("mocharging = ");
            sb.append(smscPropertiesManagement.getMoCharging());
            sb.append("\n");

            sb.append("hrcharging = ");
            sb.append(smscPropertiesManagement.getHrCharging());
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

            sb.append("removinglivetablesdays = ");
            sb.append(smscPropertiesManagement.getRemovingLiveTablesDays());
            sb.append("\n");

            sb.append("national-language-single-shift = ");
            sb.append(smscPropertiesManagement.getNationalLanguageSingleShift());
            sb.append("\n");

            sb.append("national-language-locking-shift = ");
            sb.append(smscPropertiesManagement.getNationalLanguageLockingShift());
            sb.append("\n");

            sb.append("deliverypause = ");
            sb.append(smscPropertiesManagement.isDeliveryPause());
            sb.append("\n");

			return sb.toString();
		}
	}

	/**
	 * smsc databaseRule update <address> <systemId> <SMPP|SIP> networkid <network-id>
	 * 
	 * @param args
	 * @return
	 */
	private String databaseRuleUpdate(String[] args) throws Exception {
		DatabaseSmsRoutingRule smsRoutingRule = (DatabaseSmsRoutingRule) this.smscManagement.getSmsRoutingRule();

		if (args.length < 5 || args.length > 8) {
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

        int count = 5;
        String command;
        SmsRoutingRuleType smsRoutingRuleType = SmsRoutingRuleType.SMPP;
        int networkId = 0;
        while (count < args.length && ((command = args[count++]) != null)) {
            if (command.equals("SMPP") || command.equals("SIP")) {
                smsRoutingRuleType = SmsRoutingRuleType.valueOf(command);
            } else if (command.equals("networkid")) {
                if (count < args.length) {
                    String value = args[count++];
                    networkId = Integer.parseInt(value);
                }
            }
        }// while

        smsRoutingRule.updateDbSmsRoutingRule(smsRoutingRuleType, address, networkId, systemId);
        return String.format(SMSCOAMMessages.UPDATE_DATABASE_RULE_SUCCESSFULL, smsRoutingRuleType.toString(), address, networkId);
	}

	/**
	 * smsc databaseRule delete <address> <SMPP|SIP> networkid <network-id>
	 * 
	 * @param args
	 * @return
	 */
	private String databaseRuleDelete(String[] args) throws Exception {
		DatabaseSmsRoutingRule smsRoutingRule = (DatabaseSmsRoutingRule) this.smscManagement.getSmsRoutingRule();

		if (args.length < 4 || args.length > 7) {
			return SMSCOAMMessages.INVALID_COMMAND;
		}

		String address = args[3];
		if (address == null) {
			return SMSCOAMMessages.INVALID_COMMAND;
		}

        int count = 4;
        String command;
        SmsRoutingRuleType smsRoutingRuleType = SmsRoutingRuleType.SMPP;
        int networkId = 0;
        while (count < args.length && ((command = args[count++]) != null)) {
            if (command.equals("SMPP") || command.equals("SIP")) {
                smsRoutingRuleType = SmsRoutingRuleType.valueOf(command);
            } else if (command.equals("networkid")) {
                if (count < args.length) {
                    String value = args[count++];
                    networkId = Integer.parseInt(value);
                }
            }
        }// while

        smsRoutingRule.deleteDbSmsRoutingRule(smsRoutingRuleType, address, networkId);
        return String.format(SMSCOAMMessages.DELETE_DATABASE_RULE_SUCCESSFULL, smsRoutingRuleType.toString(), address, networkId);
    }

	/**
	 * smsc databaseRule get <address> <SMPP|SIP> networkid <network-id>
	 * 
	 * @param args
	 * @return
	 */
	private String databaseRuleGet(String[] args) throws Exception {
		DatabaseSmsRoutingRule smsRoutingRule = (DatabaseSmsRoutingRule) this.smscManagement.getSmsRoutingRule();

		if (args.length < 4 || args.length > 7) {
			return SMSCOAMMessages.INVALID_COMMAND;
		}

		String address = args[3];
		if (address == null) {
			return SMSCOAMMessages.INVALID_COMMAND;
		}

        int count = 4;
        String command;
        SmsRoutingRuleType smsRoutingRuleType = SmsRoutingRuleType.SMPP;
        int networkId = 0;
        while (count < args.length && ((command = args[count++]) != null)) {
            if (command.equals("SMPP") || command.equals("SIP")) {
                smsRoutingRuleType = SmsRoutingRuleType.valueOf(command);
            } else if (command.equals("networkid")) {
                if (count < args.length) {
                    String value = args[count++];
                    networkId = Integer.parseInt(value);
                }
            }
        }// while

		DbSmsRoutingRule res = smsRoutingRule.getSmsRoutingRule(smsRoutingRuleType, address, networkId);
        if (res == null) {
            return String.format(SMSCOAMMessages.NO_ROUTING_RULE_DEFINED_YET, smsRoutingRuleType.name(), address, networkId);
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

    public String updateCcMccmnstable(String[] args) {
        HomeRoutingManagement homeRoutingManagement = HomeRoutingManagement.getInstance();
        if (homeRoutingManagement != null)
            homeRoutingManagement.updateCcMccmncTable();

        return SMSCOAMMessages.CORRELATION_TABLE_HAS_BE_LOADED;
    }

    /**
     * Home Routing management. Commands are
     * 
     * smsc hrccmccmnc add <cc> <mccmnc> smscgt <smscgt>
     * smsc hrccmccmnc modify <cc> <mccmnc> smscgt <smscgt>
     * smsc hrccmccmnc remove <cc>
     * smsc hrccmccmnc show
     * @param args
     * @return
     * @throws Exception
     */
    public String ccMccmnsValueUpdate(String[] args) throws Exception {
        if (args.length < 3 || args.length > 7) {
            return SMSCOAMMessages.INVALID_COMMAND;
        }

        HomeRoutingManagement homeRoutingManagement = HomeRoutingManagement.getInstance();
        if (homeRoutingManagement == null)
            return SMSCOAMMessages.HR_ABSENT;

        String cmd = args[2];
        String cc = null;
        if (args.length >= 4) {
            cc = args[3];
            if (cc == null)
                return SMSCOAMMessages.INVALID_COMMAND;
        }

        if (cmd.equals("add")) {
            if (args.length < 5)
                return SMSCOAMMessages.INVALID_COMMAND;
            String mccmnc = args[4];
            String smsc = null;
            if (args.length >= 7 && args[5] != null && args[5].equals("smscgt")) {
                smsc = args[6];
                if (smsc != null && smsc.equals("-1"))
                    smsc = null;
            }
            homeRoutingManagement.addCcMccmnc(cc, mccmnc, smsc);
            return SMSCOAMMessages.HR_CCMCCMNC_ADDED;
        
        } else if (cmd.equals("modify")) {
            if (args.length < 5)
                return SMSCOAMMessages.INVALID_COMMAND;
            String mccmnc = args[4];
            String smsc = null;
            if (args.length >= 7 && args[5] != null && args[5].equals("smscgt")) {
                smsc = args[6];
                if (smsc != null && smsc.equals("-1"))
                    smsc = null;
            }
            homeRoutingManagement.modifyCcMccmnc(cc, mccmnc, smsc);
            return SMSCOAMMessages.HR_CCMCCMNC_MODIFIED;
        } else if (cmd.equals("remove")) {
            homeRoutingManagement.removeCcMccmnc(cc);
            return SMSCOAMMessages.HR_CCMCCMNC_REMOVED;
        } else if (cmd.equals("show")) {
            if (cc == null) {
                Map<String, CcMccmncImpl> map = homeRoutingManagement.getCcMccmncMap();
                StringBuilder sb = new StringBuilder();
                sb.append(SMSCOAMMessages.HR_CCMCCMNC_COLL);
                for (CcMccmncImpl val : map.values()) {
                    sb.append(val.toString());
                    sb.append("\n");
                }
                sb.append("]");
                return sb.toString();
            } else {
                CcMccmncImpl ccMccmnc = (CcMccmncImpl) homeRoutingManagement.getCcMccmnc(cc);
                if (ccMccmnc != null) {
                    return ccMccmnc.toString();
                } else {
                    return String.format(SMSCOAMMessages.HR_CCMCCMNC_NOTFOUND, cc);
                }
            }
        } else {
            return SMSCOAMMessages.INVALID_COMMAND;
        }
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


//smsc homeroute add <countrycode> <mccmnc> smscgt <smsc-gt>
//smsc homeroute modify <countrycode> <mccmnc> smscgt <smsc-gt>
//smsc homeroute delete <countrycode>
//smsc homeroute show <mccmnc> 

