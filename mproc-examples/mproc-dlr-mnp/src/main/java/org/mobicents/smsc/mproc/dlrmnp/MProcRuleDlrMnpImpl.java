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

/**
*
* @author sergey vetyutnev
*
*/
public class MProcRuleDlrMnpImpl extends MProcRuleBaseImpl {

    private static final Logger logger = Logger.getLogger(MProcRuleDlrMnpImpl.class);

    private static final String NETWORK_ID_MASK = "networkIdMask";
    private static final String ERROR_CODE = "errorCode";
    private static final String DELIVERY_STATUS = "deliveryStatus";

    private static final String NEW_NETWORK_ID = "newNetworkId";

    private int networkIdMask = -1;
    private int errorCode = -1;
    private String deliveryStatus = "-1";

    private int newNetworkId = -1;

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

    public int getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(int errorCode) {
        this.errorCode = errorCode;
    }

    public String getDeliveryStatus() {
        return deliveryStatus;
    }

    public void setDeliveryStatus(String deliveryStatus) {
        this.deliveryStatus = deliveryStatus;
    }

    public int getNewNetworkId() {
        return newNetworkId;
    }

    public void setNewNetworkId(int newNetworkId) {
        this.newNetworkId = newNetworkId;
    }

    protected void setRuleParameters(int networkIdMask, int errorCode, String deliveryStatus, int newNetworkId) {
        this.networkIdMask = networkIdMask;
        this.errorCode = errorCode;
        this.deliveryStatus = deliveryStatus;

        this.newNetworkId = newNetworkId;
    }

    @Override
    public boolean isForPostArrivalState() {
        return true;
    }

    private boolean matches(MProcMessage message) {
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

    @Override
    public boolean matchesPostArrival(MProcMessage message) {
        return matches(message);
    }

    @Override
    public void onPostArrival(PostArrivalProcessor factory, MProcMessage message) throws Exception {
        if (message.isDeliveryReceipt()) {
            Long receiptLocalMessageId = message.getReceiptLocalMessageId();
            DeliveryReceiptData deliveryReceiptData = message.getDeliveryReceiptData();
            if (receiptLocalMessageId != null && deliveryReceiptData != null) {
                MProcMessage sentMsg = message.getOriginMessageForDeliveryReceipt(receiptLocalMessageId);
                if (sentMsg != null) {
                    logger.info("MProcRuleDlrMnp: received a delivery receipt: origNetworkId=" + message.getOrigNetworkId()
                            + ", deliveryReceiptData=" + deliveryReceiptData
                            + "\nThe receipt is dropped and an original message is rerouted to newNetworkId=" + newNetworkId);

                    factory.dropMessage();

                    MProcNewMessage newMsg = factory.createNewCopyMessage(sentMsg);
                    newMsg.setNetworkId(newNetworkId);
                    factory.postNewMessage(newMsg);
                }
            }
        }
    }

    @Override
    public void setInitialRuleParameters(String parametersString) throws Exception {
        String[] args = splitParametersString(parametersString);

        int count = 0;
        String command;

        boolean success = false;
        int networkIdMask = -1;
        int errorCode = -1;
        String deliveryStatus = "-1";

        int newNetworkId = -1;

        while (count < args.length) {
            command = args[count++];
            if (count < args.length) {
                String value = args[count++];
                if (command.equals("networkidmask")) {
                    networkIdMask = Integer.parseInt(value);
                } else if (command.equals("errorcode")) {
                    errorCode = Integer.parseInt(value);
                } else if (command.equals("deliverystatus")) {
                    deliveryStatus = value;

                } else if (command.equals("newnetworkid")) {
                    newNetworkId = Integer.parseInt(value);
                    success = true;
                }
            }
        }// while

        if (!success) {
            throw new Exception(MProcRuleOamMessagesDlrMnp.SET_RULE_PARAMETERS_FAIL_NO_ACTIONS_PROVIDED);
        }

        this.setRuleParameters(networkIdMask, errorCode, deliveryStatus, newNetworkId);
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
                } else if (command.equals("errorcode")) {
                    int val = Integer.parseInt(value);
                    this.setErrorCode(val);
                    success = true;
                } else if (command.equals("deliverystatus")) {
                    this.setDeliveryStatus(value);
                    success = true;

                } else if (command.equals("newnetworkid")) {
                    int val = Integer.parseInt(value);
                    this.setNewNetworkId(val);
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
        if (errorCode != -1) {
            writeParameter(sb, parNumber++, "errorcode", errorCode, " ", " ");
        }
        if (this.deliveryStatus != null && !this.deliveryStatus.equals("") && !this.deliveryStatus.equals("-1")) {
            writeParameter(sb, parNumber++, "deliverystatus", deliveryStatus, " ", " ");
        }

        if (newNetworkId != -1) {
            writeParameter(sb, parNumber++, "newnetworkid", newNetworkId, " ", " ");
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
            mProcRule.errorCode = xml.getAttribute(ERROR_CODE, -1);
            mProcRule.deliveryStatus = xml.getAttribute(DELIVERY_STATUS, "-1");

            mProcRule.newNetworkId = xml.getAttribute(NEW_NETWORK_ID, -1);
        }

        @Override
        public void write(MProcRuleDlrMnpImpl mProcRule, javolution.xml.XMLFormat.OutputElement xml) throws XMLStreamException {
            M_PROC_RULE_BASE_XML.write(mProcRule, xml);

            if (mProcRule.networkIdMask != -1)
                xml.setAttribute(NETWORK_ID_MASK, mProcRule.networkIdMask);
            if (mProcRule.errorCode != -1)
                xml.setAttribute(ERROR_CODE, mProcRule.errorCode);
            if (mProcRule.deliveryStatus != null && !mProcRule.deliveryStatus.equals("")
                    && !mProcRule.deliveryStatus.equals("-1"))
                xml.setAttribute(DELIVERY_STATUS, mProcRule.deliveryStatus);

            if (mProcRule.newNetworkId != -1)
                xml.setAttribute(NEW_NETWORK_ID, mProcRule.newNetworkId);
        }
    };

}
