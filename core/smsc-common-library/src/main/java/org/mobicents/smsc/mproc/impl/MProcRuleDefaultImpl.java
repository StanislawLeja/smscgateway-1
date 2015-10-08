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

package org.mobicents.smsc.mproc.impl;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javolution.xml.XMLFormat;
import javolution.xml.stream.XMLStreamException;

import org.mobicents.smsc.library.OriginationType;
import org.mobicents.smsc.mproc.MProcMessage;
import org.mobicents.smsc.mproc.MProcMessageDestination;
import org.mobicents.smsc.mproc.MProcNewMessage;
import org.mobicents.smsc.mproc.MProcRuleDefault;
import org.mobicents.smsc.mproc.PostArrivalProcessor;
import org.mobicents.smsc.mproc.PostDeliveryProcessor;
import org.mobicents.smsc.mproc.PostImsiProcessor;

/**
*
* @author sergey vetyutnev
*
*/
public class MProcRuleDefaultImpl extends MProcRuleBaseImpl implements MProcRuleDefault {

    private static final String DEST_TON_MASK = "destTonMask";
    private static final String DEST_NPI_MASK = "destNpiMask";
    private static final String DEST_DIG_MASK = "destDigMask";
    private static final String ORIGINATING_MASK = "originatingMask";
    private static final String NETWORK_ID_MASK = "networkIdMask";
    private static final String NEW_NETWORK_ID = "newNetworkId";
    private static final String NEW_DEST_TON = "newDestTon";
    private static final String NEW_DEST_NPI = "newDestNpi";
    private static final String ADD_DEST_DIG_PREFIX = "addDestDigPrefix";
    private static final String MAKE_COPY = "makeCopy";

    // TODO: we need proper implementing
//    // test magic mproc rules - we need to remove then later after proper implementing
//    public static final int MAGIC_RULES_ID_START = -21100;
//    public static final int MAGIC_RULES_ID_END = -21000;
//    // Testing PostImsi case: if NNN digits are started with "1", MT messages will be dropped
//    public static final int MAGIC_RULES_ID_NNN_CHECK = -21000;
//    // Testing PostDelivery case: generating a report to originator for all delivered / failed message as a plain text message
//    public static final int MAGIC_RULES_ID_DELIVERY_ANNOUNCEMENT = -21001;
//    // Testing PostArrivale case: drop a message
//    public static final int MAGIC_RULES_ID_ARRIVAL_DROP = -21002;
//    // Testing PostArrivale case: reject a message
//    public static final int MAGIC_RULES_ID_ARRIVAL_REJECT = -21003;
    // TODO: we need proper implementing

    private int destTonMask = -1;
    private int destNpiMask = -1;
    private String destDigMask = "-1";
    private OriginationType originatingMask = null;
    private int networkIdMask = -1;

    private int newNetworkId = -1;
    private int newDestTon = -1;
    private int newDestNpi = -1;
    private String addDestDigPrefix = "-1";
    private boolean makeCopy = false;

    private Pattern destDigMaskPattern;

    @Override
    public String getRuleClassName() {
        return MProcRuleFactoryDefault.RULE_CLASS_NAME;
    }

    /**
     * @return mask for destination address type of number. -1 means any value
     */
    public int getDestTonMask() {
        return destTonMask;
    }

    public void setDestTonMask(int destTonMask) {
        this.destTonMask = destTonMask;
    }

    /**
     * @return mask for destination address numerical type indicator. -1 means any value
     */
    public int getDestNpiMask() {
        return destNpiMask;
    }

    public void setDestNpiMask(int destNpiMask) {
        this.destNpiMask = destNpiMask;
    }

    /**
     * @return mask (a regular expression) for destination address digits. "-1" means any value (same as "......")
     */
    public String getDestDigMask() {
        return destDigMask;
    }

    public void setDestDigMask(String destDigMask) {
        this.destDigMask = destDigMask;

        this.resetPattern();
    }

    /**
     * @return mask for message originatingMask (SMPP, SIP, MO, HR SS7). null (CLI "-1") means any value
     */
    public OriginationType getOriginatingMask() {
        return originatingMask;
    }

    public void setOriginatingMask(OriginationType originatingMask) {
        this.originatingMask = originatingMask;
    }

    /**
     * @return mask for message original NetworkId. "-1" means any value.
     */
    public int getNetworkIdMask() {
        return networkIdMask;
    }

    public void setNetworkIdMask(int networkIdMask) {
        this.networkIdMask = networkIdMask;
    }

    /**
     * @return if !=-1: the new networkId will be assigned to a message
     */
    public int getNewNetworkId() {
        return newNetworkId;
    }

    public void setNewNetworkId(int newNetworkId) {
        this.newNetworkId = newNetworkId;
    }

    /**
     * @return if !=-1: the new destination address type of number will be assigned to a message
     */
    public int getNewDestTon() {
        return newDestTon;
    }

    public void setNewDestTon(int newDestTon) {
        this.newDestTon = newDestTon;
    }

    /**
     * @return if !=-1: the new destination address numbering plan indicator will be assigned to a message
     */
    public int getNewDestNpi() {
        return newDestNpi;
    }

    public void setNewDestNpi(int newDestNpi) {
        this.newDestNpi = newDestNpi;
    }

    /**
     * @return if !="-1" / != null: the specified prefix will be added into a destination address digits of a message
     */
    public String getAddDestDigPrefix() {
        return addDestDigPrefix;
    }

    public void setAddDestDigPrefix(String addDestDigPrefix) {
        this.addDestDigPrefix = addDestDigPrefix;
    }

    /**
     * @return if true - a copy of a message will be created. All other next
     *         rules will be applied only for a copy of a message
     */
    public boolean isMakeCopy() {
        return makeCopy;
    }

    public void setMakeCopy(boolean makeCopy) {
        this.makeCopy = makeCopy;
    }

    private void resetPattern() {
        if (this.destDigMask != null && !this.destDigMask.equals("") && !this.destDigMask.equals("-1")) {
            this.destDigMaskPattern = Pattern.compile(this.destDigMask);
        } else {
            this.destDigMaskPattern = null;
        }
    }

    protected void setRuleParameters(int destTonMask, int destNpiMask, String destDigMask, OriginationType originatingMask,
            int networkIdMask, int newNetworkId, int newDestTon, int newDestNpi, String addDestDigPrefix, boolean makeCopy) {
        this.destTonMask = destTonMask;
        this.destNpiMask = destNpiMask;
        this.destDigMask = destDigMask;
        this.originatingMask = originatingMask;
        this.networkIdMask = networkIdMask;

        this.newNetworkId = newNetworkId;
        this.newDestTon = newDestTon;
        this.newDestNpi = newDestNpi;
        this.addDestDigPrefix = addDestDigPrefix;
        this.makeCopy = makeCopy;

        this.resetPattern();
    }


    @Override
    public boolean isForPostArrivalState() {
        return true;
    }

    @Override
    public boolean isForPostImsiRequestState() {
        // TODO: we need proper implementing
//        if (this.getId() == MAGIC_RULES_ID_NNN_CHECK)
//            return true;
        // TODO: we need proper implementing

        return false;
    }

    @Override
    public boolean isForPostDeliveryState() {
        // TODO: we need proper implementing
//        if (this.getId() == MAGIC_RULES_ID_DELIVERY_ANNOUNCEMENT)
//            return true;
        // TODO: we need proper implementing

        return false;
    }

    @Override
    public boolean matches(MProcMessage message) {
        // TODO: we need proper implementing
//        if (this.getId() >= MAGIC_RULES_ID_START && this.getId() <= MAGIC_RULES_ID_END) {
//            return true;
//        }
        // TODO: we need proper implementing

        if (destTonMask != -1 && destTonMask != message.getDestAddrTon())
            return false;
        if (destNpiMask != -1 && destNpiMask != message.getDestAddrNpi())
            return false;
        if (destDigMaskPattern != null) {
            Matcher m = this.destDigMaskPattern.matcher(message.getDestAddr());
            if (!m.matches())
                return false;
        }
        if (originatingMask != null && originatingMask != message.getOriginationType())
            return false;
        if (networkIdMask != -1 && networkIdMask != message.getNetworkId())
            return false;

        return true;
    }

    @Override
    public boolean matches(MProcMessageDestination messageDest) {
        // TODO: we need proper implementing
//        if (this.getId() >= MAGIC_RULES_ID_START && this.getId() <= MAGIC_RULES_ID_END) {
//            return true;
//        }
        // TODO: we need proper implementing

        return false;
    }

    @Override
    public void onPostArrival(PostArrivalProcessor factory, MProcMessage message) throws Exception {
        // TODO: we need proper implementing
//        if (this.getId() == MAGIC_RULES_ID_ARRIVAL_DROP) {
//            factory.dropMessage();
//            return;
//        }
//        if (this.getId() == MAGIC_RULES_ID_ARRIVAL_REJECT) {
//            factory.rejectMessage();
//            return;
//        }
        // TODO: we need proper implementing

        if (this.makeCopy) {
            MProcNewMessage copy = factory.createNewCopyMessage(message);
            factory.postNewMessage(copy);
        }

        if (this.newNetworkId != -1) {
            factory.updateMessageNetworkId(message, this.newNetworkId);
        }

        if (this.addDestDigPrefix != null && !this.addDestDigPrefix.equals("") && !this.addDestDigPrefix.equals("-1")) {
            String destAddr = this.getAddDestDigPrefix() + message.getDestAddr();
            factory.updateMessageDestAddr(message, destAddr);
        }

        if (this.newDestNpi != -1) {
            factory.updateMessageDestAddrNpi(message, this.newDestNpi);
        }

        if (this.newDestTon != -1) {
            factory.updateMessageDestAddrTon(message, this.newDestTon);
        }
    }

    @Override
    public void onPostImsiRequest(PostImsiProcessor factory, MProcMessageDestination messages) throws Exception {
        // TODO: we need proper implementing
//        if (this.getId() == MAGIC_RULES_ID_NNN_CHECK) {
//            if (factory.getNnnDigits().startsWith("1")) {
//                factory.dropMessages();
//            }
//        }
        // TODO: we need proper implementing
    }

    @Override
    public void onPostDelivery(PostDeliveryProcessor factory, MProcMessage message)
            throws Exception {
        // TODO: we need proper implementing
//        if (this.getId() == MAGIC_RULES_ID_DELIVERY_ANNOUNCEMENT) {
//            // this is a protection against cyclic report for report
//            if (message.getShortMessageText().startsWith("Delivery ") || (message.getEsmClass() & 0x3C) != 0)
//                return;
//
//            String respTxt;
//            if (factory.isDeliveryFailure())
//                respTxt = "Delivery failed for a dest:" + message.getDestAddr() + ", msg:" + message.getShortMessageText();
//            else
//                respTxt = "Delivery succeded for a dest:" + message.getDestAddr() + ", msg:" + message.getShortMessageText();
//            MProcNewMessage resp = factory.createNewResponseMessage(message);
//            resp.setShortMessageText(respTxt);
//            factory.postNewMessage(resp);
//        }
        // TODO: we need proper implementing
    }

    @Override
    public void setInitialRuleParameters(String parametersString) throws Exception {
        // TODO: we need proper implementing
//        if (this.getId() >= MAGIC_RULES_ID_START && this.getId() <= MAGIC_RULES_ID_END) {
//            return;
//        }
        // TODO: we need proper implementing

        String[] args = splitParametersString(parametersString);

        int count = 0;
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
            throw new Exception(MProcRuleOamMessages.SET_RULE_PARAMETERS_FAIL_NO_PARAMETERS_POVIDED);
        }

        OriginationType originatingMaskVal = null;
        try {
            originatingMaskVal = OriginationType.valueOf(originatingMask);
        } catch (Exception e) {
        }

        this.setRuleParameters(destTonMask, destNpiMask, destDigMask, originatingMaskVal, networkIdMask, newNetworkId, newDestTon,
                newDestNpi, addDestDigPrefix, makeCopy);
    }

    @Override
    public void updateRuleParameters(String parametersString) throws Exception {
        String[] args = splitParametersString(parametersString);

        int count = 0;
        String command;

        boolean success = false;
        while (count < args.length) {
            command = args[count++];
            if (count < args.length) {
                String value = args[count++];
                if (command.equals("desttonmask")) {
                    int val = Integer.parseInt(value);
                    this.setDestTonMask(val);
                    success = true;
                } else if (command.equals("destnpimask")) {
                    int val = Integer.parseInt(value);
                    this.setDestNpiMask(val);
                    success = true;
                } else if (command.equals("destdigmask")) {
                    this.setDestDigMask(value);
                    success = true;
                } else if (command.equals("originatingmask")) {
                    if (value != null && value.equals("-1")) {
                        this.setOriginatingMask(null);
                    } else {
                        OriginationType originatingMask = Enum.valueOf(OriginationType.class, value);
                        this.setOriginatingMask(originatingMask);
                    }
                    success = true;
                } else if (command.equals("networkidmask")) {
                    int val = Integer.parseInt(value);
                    this.setNetworkIdMask(val);
                    success = true;
                } else if (command.equals("newnetworkid")) {
                    int val = Integer.parseInt(value);
                    this.setNewNetworkId(val);
                    success = true;
                } else if (command.equals("newdestton")) {
                    int val = Integer.parseInt(value);
                    this.setNewDestTon(val);
                    success = true;
                } else if (command.equals("newdestnpi")) {
                    int val = Integer.parseInt(value);
                    this.setNewDestNpi(val);
                    success = true;
                } else if (command.equals("adddestdigprefix")) {
                    this.setAddDestDigPrefix(value);
                    success = true;
                } else if (command.equals("makecopy")) {
                    boolean val = Boolean.parseBoolean(value);
                    this.setMakeCopy(val);
                    success = true;
                }
            }
        }// while

        if (!success) {
            throw new Exception(MProcRuleOamMessages.SET_RULE_PARAMETERS_FAIL_NO_PARAMETERS_POVIDED);
        }
    }

    @Override
    public String getRuleParameters() {

        // TODO: we need proper implementing
//        if (this.getId() == MAGIC_RULES_ID_NNN_CHECK)
//            return "MAGIC_RULES_ID_NNN_CHECK";
//        if (this.getId() == MAGIC_RULES_ID_DELIVERY_ANNOUNCEMENT)
//            return "MAGIC_RULES_ID_DELIVERY_ANNOUNCEMENT";
//        if (this.getId() == MAGIC_RULES_ID_ARRIVAL_DROP)
//            return "MAGIC_RULES_ID_ARRIVAL_DROP";
//        if (this.getId() == MAGIC_RULES_ID_ARRIVAL_REJECT)
//            return "MAGIC_RULES_ID_ARRIVAL_REJECT";
        // TODO: we need proper implementing

        StringBuilder sb = new StringBuilder();
        int parNumber = 0;

        if (destTonMask != -1) {
            writeParameter(sb, parNumber++, "destTonMask", destTonMask);
        }
        if (destNpiMask != -1) {
            writeParameter(sb, parNumber++, "destNpiMask", destNpiMask);
        }
        if (this.destDigMask != null && !this.destDigMask.equals("") && !this.destDigMask.equals("-1")) {
            writeParameter(sb, parNumber++, "destDigMask", destDigMask);
        }
        if (originatingMask != null) {
            writeParameter(sb, parNumber++, "originatingMask", originatingMask);
        }
        if (networkIdMask != -1) {
            writeParameter(sb, parNumber++, "networkIdMask", networkIdMask);
        }
        if (newNetworkId != -1) {
            writeParameter(sb, parNumber++, "newNetworkId", newNetworkId);
        }
        if (newDestTon != -1) {
            writeParameter(sb, parNumber++, "newDestTon", newDestTon);
        }
        if (newDestNpi != -1) {
            writeParameter(sb, parNumber++, "newDestNpi", newDestNpi);
        }
        if (this.addDestDigPrefix != null && !this.addDestDigPrefix.equals("") && !this.addDestDigPrefix.equals("-1")) {
            writeParameter(sb, parNumber++, "addDestDigPrefix", addDestDigPrefix);
        }
        if (makeCopy) {
            writeParameter(sb, parNumber++, "makeCopy", makeCopy);
        }

        return sb.toString();
    }

    /**
     * XML Serialization/Deserialization
     */
    protected static final XMLFormat<MProcRuleDefaultImpl> M_PROC_RULE_DEFAULT_XML = new XMLFormat<MProcRuleDefaultImpl>(MProcRuleDefaultImpl.class) {

        @Override
        public void read(javolution.xml.XMLFormat.InputElement xml, MProcRuleDefaultImpl mProcRule) throws XMLStreamException {
            M_PROC_RULE_BASE_XML.read(xml, mProcRule);

            mProcRule.destTonMask = xml.getAttribute(DEST_TON_MASK, -1);
            mProcRule.destNpiMask = xml.getAttribute(DEST_NPI_MASK, -1);
            mProcRule.destDigMask = xml.getAttribute(DEST_DIG_MASK, "-1");

            String val = xml.getAttribute(ORIGINATING_MASK, "");
            if (val != null) {
                try {
                    mProcRule.originatingMask = Enum.valueOf(OriginationType.class, val);
                } catch (Exception e) {
                }
            }

            mProcRule.networkIdMask = xml.getAttribute(NETWORK_ID_MASK, -1);

            mProcRule.newNetworkId = xml.getAttribute(NEW_NETWORK_ID, -1);
            mProcRule.newDestTon = xml.getAttribute(NEW_DEST_TON, -1);
            mProcRule.newDestNpi = xml.getAttribute(NEW_DEST_NPI, -1);
            mProcRule.addDestDigPrefix = xml.getAttribute(ADD_DEST_DIG_PREFIX, "-1");
            mProcRule.makeCopy = xml.getAttribute(MAKE_COPY, false);

            mProcRule.resetPattern();
        }

        @Override
        public void write(MProcRuleDefaultImpl mProcRule, javolution.xml.XMLFormat.OutputElement xml) throws XMLStreamException {
            M_PROC_RULE_BASE_XML.write(mProcRule, xml);

            xml.setAttribute(DEST_TON_MASK, mProcRule.destTonMask);
            xml.setAttribute(DEST_NPI_MASK, mProcRule.destNpiMask);

            if (mProcRule.destDigMask != null)
                xml.setAttribute(DEST_DIG_MASK, mProcRule.destDigMask);
            if (mProcRule.originatingMask != null)
                xml.setAttribute(ORIGINATING_MASK, mProcRule.originatingMask.toString());

            xml.setAttribute(NETWORK_ID_MASK, mProcRule.networkIdMask);
            xml.setAttribute(NEW_NETWORK_ID, mProcRule.newNetworkId);
            xml.setAttribute(NEW_DEST_TON, mProcRule.newDestTon);
            xml.setAttribute(NEW_DEST_NPI, mProcRule.newDestNpi);

            if (mProcRule.addDestDigPrefix != null)
                xml.setAttribute(ADD_DEST_DIG_PREFIX, mProcRule.addDestDigPrefix);

            if (mProcRule.makeCopy)
                xml.setAttribute(MAKE_COPY, mProcRule.makeCopy);
        }
    };

}
