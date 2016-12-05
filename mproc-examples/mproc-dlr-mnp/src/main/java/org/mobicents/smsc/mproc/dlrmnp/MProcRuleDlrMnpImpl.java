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

package org.mobicents.smsc.mproc.dlrmnp;

import javolution.xml.XMLFormat;
import javolution.xml.stream.XMLStreamException;

import org.apache.log4j.Logger;
import org.mobicents.smsc.mproc.DeliveryReceiptData;
import org.mobicents.smsc.mproc.MProcMessage;
import org.mobicents.smsc.mproc.MProcNewMessage;
import org.mobicents.smsc.mproc.MProcRuleBaseImpl;
import org.mobicents.smsc.mproc.PostArrivalProcessor;
import org.mobicents.smsc.mproc.PostDeliveryProcessor;

/**
*
* @author sergey vetyutnev
*
*/
public class MProcRuleDlrMnpImpl extends MProcRuleBaseImpl {

    private static final Logger logger = Logger.getLogger(MProcRuleDlrMnpImpl.class);

    private static final String NETWORK_ID_MASK = "networkIdMask";
    private static final String ORIG_NETWORK_ID_MASK = "origNetworkIdMask";
    private static final String ERROR_CODE = "errorCode";
    private static final String ERROR_CODE_SMPP = "errorCodeSmpp";
    private static final String DELIVERY_STATUS = "deliveryStatus";
    private static final String UNRECOGNIZED_RECEIPT = "unrecognizedReceipt";

    private static final String NEW_NETWORK_ID = "newNetworkId";
    private static final String DROP_DR = "dropDR";
    private static final String REROUTE_DR = "rerouteDR";

    private int networkIdMask = -1;
    private int origNetworkIdMask = -1;
    private int errorCode = -1;
    private int errorCodeSmpp = -1;
    private String deliveryStatus = "-1";
    private boolean unrecognizedReceipt = false;

    private int newNetworkId = -1;
    private boolean dropDR = false;
    private int rerouteDR = -1;

    @Override
    public String getRuleClassName() {
        return MProcRuleFactoryDlrMnpImpl.CLASS_NAME;
    }

    public int getNetworkIdMask() {
        return networkIdMask;
    }

    public void setNetworkIdMask(int networkIdMask) {
        this.networkIdMask = networkIdMask;
    }

    public int getOrigNetworkIdMask() {
        return origNetworkIdMask;
    }

    public void setOrigNetworkIdMask(int origNetworkIdMask) {
        this.origNetworkIdMask = origNetworkIdMask;
    }

    public int getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(int errorCode) {
        this.errorCode = errorCode;
    }

    public int getErrorCodeSmpp() {
        return errorCodeSmpp;
    }

    public void setErrorCodeSmpp(int errorCodeSmpp) {
        this.errorCodeSmpp = errorCodeSmpp;
    }

    public String getDeliveryStatus() {
        return deliveryStatus;
    }

    public void setDeliveryStatus(String deliveryStatus) {
        this.deliveryStatus = deliveryStatus;
    }

    public boolean isUnrecognizedReceipt() {
        return unrecognizedReceipt;
    }

    public void setUnrecognizedReceipt(boolean unrecognizedReceipt) {
        this.unrecognizedReceipt = unrecognizedReceipt;
    }

    public int getNewNetworkId() {
        return newNetworkId;
    }

    public void setNewNetworkId(int newNetworkId) {
        this.newNetworkId = newNetworkId;
    }

    public boolean isDropDR() {
        return dropDR;
    }

    public void setDropDR(boolean dropDR) {
        this.dropDR = dropDR;
    }

    public int getRerouteDR() {
        return rerouteDR;
    }

    public void setRerouteDR(int rerouteDR) {
        this.rerouteDR = rerouteDR;
    }

    protected void setRuleParameters(int networkIdMask, int origNetworkIdMask, int errorCode, int errorCodeSmpp,
            String deliveryStatus, boolean unrecognizedReceipt, int newNetworkId, boolean dropDR, int rerouteDR) {
        this.networkIdMask = networkIdMask;
        this.origNetworkIdMask = origNetworkIdMask;
        this.errorCode = errorCode;
        this.errorCodeSmpp = errorCodeSmpp;
        this.deliveryStatus = deliveryStatus;
        this.unrecognizedReceipt = unrecognizedReceipt;

        this.newNetworkId = newNetworkId;
        this.dropDR = dropDR;
        this.rerouteDR = rerouteDR;
    }

    @Override
    public boolean isForPostArrivalState() {
        return true;
    }

    @Override
    public boolean isForPostDeliveryState() {
        return true;
    }

    private boolean mArrival(MProcMessage message) {
        if (message.isDeliveryReceipt()) {
            DeliveryReceiptData drd = message.getDeliveryReceiptData();
            if (drd != null) {
                if (networkIdMask != -1 && networkIdMask != message.getNetworkId()) {
                    return false;
                }
                if (errorCode != -1 && errorCode != drd.getError()) {
                    return false;
                }

                if (deliveryStatus != null && !this.deliveryStatus.equals("-1") && !deliveryStatus.equals(drd.getStatus())) {
                    return false;
                }
            }

            return true;
        }

        return false;
    }

    private boolean mFailure(MProcMessage message) {
        if (message.getSmppCommandStatus() != 0 && errorCodeSmpp != -1) {
            if (networkIdMask != -1 && networkIdMask != message.getNetworkId()) {
                return false;
            }

            if (errorCodeSmpp != -1 && errorCodeSmpp != message.getSmppCommandStatus()) {
                return false;
            }

            return true;
        }

        return false;
    }

    @Override
    public boolean matchesPostArrival(MProcMessage message) {
        return mArrival(message);
    }

    @Override
    public boolean matchesPostDelivery(MProcMessage message) {
        return mFailure(message);
    }

    @Override
    public void onPostArrival(PostArrivalProcessor factory, MProcMessage message) throws Exception {
        DeliveryReceiptData deliveryReceiptData = null;
        MProcMessage sentMsg = null;

        if (message.isDeliveryReceipt()) {
            Long receiptLocalMessageId = message.getReceiptLocalMessageId();
            deliveryReceiptData = message.getDeliveryReceiptData();
            if (receiptLocalMessageId != null && deliveryReceiptData != null) {
                sentMsg = message.getOriginMessageForDeliveryReceipt(receiptLocalMessageId);
            }

            // unrecognizedReceipt
            if (unrecognizedReceipt) {
                if (deliveryReceiptData != null && receiptLocalMessageId != null && sentMsg != null) {
                    return;
                }
            }
            if (origNetworkIdMask != -1) {
                if (sentMsg == null || origNetworkIdMask != sentMsg.getOrigNetworkId()) {
                    return;
                }
            }

            if (newNetworkId != -1 && sentMsg != null) {
                logger.info("MProcRuleDlrMnp: received a delivery receipt: origNetworkId=" + message.getOrigNetworkId()
                        + ", deliveryReceiptData=" + deliveryReceiptData
                        + "\nThe receipt is dropped and an original message is rerouted to newNetworkId=" + newNetworkId);

                factory.dropMessage();

                MProcNewMessage newMsg = factory.createNewCopyMessage(sentMsg);
                newMsg.setNetworkId(newNetworkId);
                factory.postNewMessage(newMsg);
            } else if (rerouteDR != -1) {
                logger.info("MProcRuleDlrMnp: received a delivery receipt: origNetworkId=" + message.getOrigNetworkId()
                        + ", deliveryReceiptData=" + deliveryReceiptData + "\nThe receipt is rerouted to " + rerouteDR);

                factory.updateMessageNetworkId(message, rerouteDR);
            } else if (dropDR) {
                logger.info("MProcRuleDlrMnp: received a delivery receipt: origNetworkId=" + message.getOrigNetworkId()
                        + ", deliveryReceiptData=" + deliveryReceiptData + "\nThe receipt is dropped");

                factory.dropMessage();
            }
        }
    }

    @Override
    public void onPostDelivery(PostDeliveryProcessor factory, MProcMessage message) throws Exception {
        if (!factory.isDeliveryFailure())
            return;

        if (newNetworkId != -1) {
            logger.info("MProcRuleDlrMnp: received a rejected SMPP response: origNetworkId=" + message.getOrigNetworkId()
                    + ", errorCodeSmppData=" + message.getSmppCommandStatus()
                    + "\nAn original message is rerouted to newNetworkId=" + newNetworkId);

            factory.rerouteMessage(newNetworkId);
        }
    }

    @Override
    public void setInitialRuleParameters(String parametersString) throws Exception {
        String[] args = splitParametersString(parametersString);

        int count = 0;
        String command;

        boolean success = false;
        int networkIdMask = -1;
        int origNetworkIdMask = -1;
        int errorCode = -1;
        int errorCodeSmpp = -1;
        String deliveryStatus = "-1";
        boolean unrecognizedReceipt = false;

        int newNetworkId = -1;
        boolean dropDR = false;
        int rerouteDR = -1;

        while (count < args.length) {
            command = args[count++];
            if (count < args.length) {
                String value = args[count++];
                if (command.equals("networkidmask")) {
                    networkIdMask = Integer.parseInt(value);
                } else if (command.equals("orignetworkidmask")) {
                    origNetworkIdMask = Integer.parseInt(value);
                } else if (command.equals("errorcode")) {
                    errorCode = Integer.parseInt(value);
                } else if (command.equals("errorcodesmpp")) {
                    errorCodeSmpp = Integer.parseInt(value);
                } else if (command.equals("deliverystatus")) {
                    deliveryStatus = value;
                } else if (command.equals("unrecognizedreceipt")) {
                    unrecognizedReceipt = Boolean.parseBoolean(value);

                } else if (command.equals("newnetworkid")) {
                    newNetworkId = Integer.parseInt(value);
                    success = true;
                } else if (command.equals("dropdr")) {
                    dropDR = Boolean.parseBoolean(value);
                    success = true;
                } else if (command.equals("reroutedr")) {
                    rerouteDR = Integer.parseInt(value);
                    success = true;
                }
            }
        }// while

        if (!success) {
            throw new Exception(MProcRuleOamMessagesDlrMnp.SET_RULE_PARAMETERS_FAIL_NO_ACTIONS_PROVIDED);
        }

        this.setRuleParameters(networkIdMask, origNetworkIdMask, errorCode, errorCodeSmpp, deliveryStatus, unrecognizedReceipt,
                newNetworkId, dropDR, rerouteDR);
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
                if (command.equals("networkidmask")) {
                    int val = Integer.parseInt(value);
                    this.setNetworkIdMask(val);
                    success = true;
                } else if (command.equals("orignetworkidmask")) {
                    int val = Integer.parseInt(value);
                    this.setOrigNetworkIdMask(val);
                    success = true;
                } else if (command.equals("errorcode")) {
                    int val = Integer.parseInt(value);
                    this.setErrorCode(val);
                    success = true;
                } else if (command.equals("errorcodesmpp")) {
                    int val = Integer.parseInt(value);
                    this.setErrorCodeSmpp(val);
                    success = true;
                } else if (command.equals("deliverystatus")) {
                    this.setDeliveryStatus(value);
                    success = true;
                } else if (command.equals("unrecognizedreceipt")) {
                    boolean val = Boolean.parseBoolean(value);
                    this.setUnrecognizedReceipt(val);
                    success = true;

                } else if (command.equals("newnetworkid")) {
                    int val = Integer.parseInt(value);
                    this.setNewNetworkId(val);
                    success = true;
                } else if (command.equals("dropdr")) {
                    boolean val = Boolean.parseBoolean(value);
                    this.setDropDR(val);
                    success = true;
                } else if (command.equals("reroutedr")) {
                    int val = Integer.parseInt(value);
                    this.setRerouteDR(val);
                    success = true;
                }
            }
        }// while

        if (!success) {
            throw new Exception(MProcRuleOamMessagesDlrMnp.SET_RULE_PARAMETERS_FAIL_NO_PARAMETERS_PROVIDED);
        }
    }

    @Override
    public String getRuleParameters() {
        StringBuilder sb = new StringBuilder();
        int parNumber = 0;
        if (networkIdMask != -1) {
            writeParameter(sb, parNumber++, "networkidmask", networkIdMask, " ", " ");
        }
        if (origNetworkIdMask != -1) {
            writeParameter(sb, parNumber++, "orignetworkidmask", origNetworkIdMask, " ", " ");
        }
        if (errorCode != -1) {
            writeParameter(sb, parNumber++, "errorcode", errorCode, " ", " ");
        }
        if (errorCodeSmpp != -1) {
            writeParameter(sb, parNumber++, "errorcodesmpp", errorCodeSmpp, " ", " ");
        }
        if (this.deliveryStatus != null && !this.deliveryStatus.equals("") && !this.deliveryStatus.equals("-1")) {
            writeParameter(sb, parNumber++, "deliverystatus", deliveryStatus, " ", " ");
        }
        if (this.unrecognizedReceipt) {
            writeParameter(sb, parNumber++, "unrecognizedreceipt", "true", " ", " ");
        }

        if (newNetworkId != -1) {
            writeParameter(sb, parNumber++, "newnetworkid", newNetworkId, " ", " ");
        }
        if (dropDR) {
            writeParameter(sb, parNumber++, "dropdr", dropDR, " ", " ");
        }
        if (rerouteDR != -1) {
            writeParameter(sb, parNumber++, "reroutedr", rerouteDR, " ", " ");
        }

        return sb.toString();
    }

    /**
     * XML Serialization/Deserialization
     */
    protected static final XMLFormat<MProcRuleDlrMnpImpl> M_PROC_RULE_DLR_MNP_IMPL_XML = new XMLFormat<MProcRuleDlrMnpImpl>(
            MProcRuleDlrMnpImpl.class) {

        @Override
        public void read(javolution.xml.XMLFormat.InputElement xml, MProcRuleDlrMnpImpl mProcRule) throws XMLStreamException {
            M_PROC_RULE_BASE_XML.read(xml, mProcRule);

            mProcRule.networkIdMask = xml.getAttribute(NETWORK_ID_MASK, -1);
            mProcRule.origNetworkIdMask = xml.getAttribute(ORIG_NETWORK_ID_MASK, -1);
            mProcRule.errorCode = xml.getAttribute(ERROR_CODE, -1);
            mProcRule.errorCodeSmpp = xml.getAttribute(ERROR_CODE_SMPP, -1);
            mProcRule.deliveryStatus = xml.getAttribute(DELIVERY_STATUS, "-1");
            mProcRule.unrecognizedReceipt = xml.getAttribute(UNRECOGNIZED_RECEIPT, false);

            mProcRule.dropDR = xml.getAttribute(DROP_DR, false);
            mProcRule.rerouteDR = xml.getAttribute(REROUTE_DR, -1);
            mProcRule.newNetworkId = xml.getAttribute(NEW_NETWORK_ID, -1);
        }

        @Override
        public void write(MProcRuleDlrMnpImpl mProcRule, javolution.xml.XMLFormat.OutputElement xml) throws XMLStreamException {
            M_PROC_RULE_BASE_XML.write(mProcRule, xml);

            if (mProcRule.networkIdMask != -1)
                xml.setAttribute(NETWORK_ID_MASK, mProcRule.networkIdMask);
            if (mProcRule.origNetworkIdMask != -1)
                xml.setAttribute(ORIG_NETWORK_ID_MASK, mProcRule.origNetworkIdMask);
            if (mProcRule.errorCode != -1)
                xml.setAttribute(ERROR_CODE, mProcRule.errorCode);
            if (mProcRule.errorCodeSmpp != -1)
                xml.setAttribute(ERROR_CODE_SMPP, mProcRule.errorCodeSmpp);
            if (mProcRule.deliveryStatus != null && !mProcRule.deliveryStatus.equals("")
                    && !mProcRule.deliveryStatus.equals("-1"))
                xml.setAttribute(DELIVERY_STATUS, mProcRule.deliveryStatus);
            if (mProcRule.unrecognizedReceipt)
                xml.setAttribute(UNRECOGNIZED_RECEIPT, mProcRule.unrecognizedReceipt);

            if (mProcRule.dropDR)
                xml.setAttribute(DROP_DR, mProcRule.dropDR);
            if (mProcRule.rerouteDR != -1)
                xml.setAttribute(REROUTE_DR, mProcRule.rerouteDR);
            if (mProcRule.newNetworkId != -1)
                xml.setAttribute(NEW_NETWORK_ID, mProcRule.newNetworkId);
        }
    };

}
