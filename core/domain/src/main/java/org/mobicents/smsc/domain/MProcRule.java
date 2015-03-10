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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javolution.xml.XMLFormat;
import javolution.xml.stream.XMLStreamException;

import org.mobicents.smsc.library.Sms;

/**
*
* @author sergey vetyutnev
*
*/
public class MProcRule implements MProcRuleMBean {

    private static final String ID = "id";
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

    private int id;

    private int destTonMask = -1;
    private int destNpiMask = -1;
    private String destDigMask = "-1";
    private Sms.OriginationType originatingMask = null;
    private int networkIdMask = -1;

    private int newNetworkId = -1;
    private int newDestTon = -1;
    private int newDestNpi = -1;
    private String addDestDigPrefix = "-1";
    private boolean makeCopy = false;

    protected transient MProcManagement mProcManagement;

    private Pattern destDigMaskPattern;

    public MProcRule() {
    }

    public MProcRule(int id, int destTonMask, int destNpiMask, String destDigMask, Sms.OriginationType originatingMask, int networkIdMask, int newNetworkId,
            int newDestTon, int newDestNpi, String addDestDigPrefix, boolean makeCopy) {
        this.id = id;

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

    /**
     * @return the id of the mproc
     */
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    /**
     * @return mask for destination address type of number. -1 means any value
     */
    public int getDestTonMask() {
        return destTonMask;
    }

    public void setDestTonMask(int destTonMask) {
        this.destTonMask = destTonMask;
        this.store();
    }

    /**
     * @return mask for destination address numerical type indicator. -1 means any value
     */
    public int getDestNpiMask() {
        return destNpiMask;
    }

    public void setDestNpiMask(int destNpiMask) {
        this.destNpiMask = destNpiMask;
        this.store();
    }

    /**
     * @return mask (a regular expression) for destination address digits. "-1" means any value (same as "......")
     */
    public String getDestDigMask() {
        return destDigMask;
    }

    public void setDestDigMask(String destDigMask) {
        this.destDigMask = destDigMask;
        this.store();

        this.resetPattern();
    }

    /**
     * @return mask for message originatingMask (SMPP, SIP, MO, HR SS7). null (CLI "-1") means any value
     */
    public Sms.OriginationType getOriginatingMask() {
        return originatingMask;
    }

    public void setOriginatingMask(Sms.OriginationType originatingMask) {
        this.originatingMask = originatingMask;
        this.store();
    }

    /**
     * @return mask for message original NetworkId. "-1" means any value.
     */
    public int getNetworkIdMask() {
        return networkIdMask;
    }

    public void setNetworkIdMask(int networkIdMask) {
        this.networkIdMask = networkIdMask;
        this.store();
    }

    /**
     * @return if !=-1: the new networkId will be assigned to a message
     */
    public int getNewNetworkId() {
        return newNetworkId;
    }

    public void setNewNetworkId(int newNetworkId) {
        this.newNetworkId = newNetworkId;
        this.store();
    }

    /**
     * @return if !=-1: the new destination address type of number will be assigned to a message
     */
    public int getNewDestTon() {
        return newDestTon;
    }

    public void setNewDestTon(int newDestTon) {
        this.newDestTon = newDestTon;
        this.store();
    }

    /**
     * @return if !=-1: the new destination address numbering plan indicator will be assigned to a message
     */
    public int getNewDestNpi() {
        return newDestNpi;
    }

    public void setNewDestNpi(int newDestNpi) {
        this.newDestNpi = newDestNpi;
        this.store();
    }

    /**
     * @return if !="-1" / != null: the specified prefix will be added into a destination address digits of a message
     */
    public String getAddDestDigPrefix() {
        return addDestDigPrefix;
    }

    public void setAddDestDigPrefix(String addDestDigPrefix) {
        this.addDestDigPrefix = addDestDigPrefix;
        this.store();
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
        this.store();
    }

    private void resetPattern() {
        if (this.destDigMask != null && !this.destDigMask.equals("") && !this.destDigMask.equals("-1")) {
            this.destDigMaskPattern = Pattern.compile(this.destDigMask);
        } else {
            this.destDigMaskPattern = null;
        }
    }

    public boolean isMessageMatchToRule(int destTon, int destNpi, String destDig, Sms.OriginationType originatingType, int networkId) {
        if (destTonMask != -1 && destTonMask != destTon)
            return false;
        if (destNpiMask != -1 && destNpiMask != destNpi)
            return false;
        if (destDigMaskPattern != null) {
            Matcher m = this.destDigMaskPattern.matcher(destDig);
            if (!m.matches())
                return false;
        }
        if (originatingMask != null && originatingMask != originatingType)
            return false;
        if (networkIdMask != -1 && networkIdMask != networkId)
            return false;

        return true;
    }

    public void applyRule(Sms sms) {
        // .........................
    }

    private void store() {
        this.mProcManagement.store();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("mProc=[id=");
        sb.append(id);
        if (destTonMask != -1) {
            sb.append(", destTonMask=");
            sb.append(destTonMask);
        }
        if (destNpiMask != -1) {
            sb.append(", destNpiMask=");
            sb.append(destNpiMask);
        }
        if (this.destDigMask != null && !this.destDigMask.equals("") && !this.destDigMask.equals("-1")) {
            sb.append(", destDigMask=");
            sb.append(destDigMask);
        }
        if (originatingMask != null) {
            sb.append(", originatingMask=");
            sb.append(originatingMask);
        }
        if (networkIdMask != -1) {
            sb.append(", networkIdMask=");
            sb.append(networkIdMask);
        }
        if (newNetworkId != -1) {
            sb.append(", newNetworkId=");
            sb.append(newNetworkId);
        }
        if (newDestTon != -1) {
            sb.append(", newDestTon=");
            sb.append(newDestTon);
        }
        if (newDestNpi != -1) {
            sb.append(", newDestNpi=");
            sb.append(newDestNpi);
        }
        if (this.addDestDigPrefix != null && !this.addDestDigPrefix.equals("") && !this.addDestDigPrefix.equals("-1")) {
            sb.append(", addDestDigPrefix=");
            sb.append(addDestDigPrefix);
        }
        if (makeCopy) {
            sb.append(", makeCopy");
        }
        sb.append("]");

        return sb.toString();
    }

    /**
     * XML Serialization/Deserialization
     */
    protected static final XMLFormat<MProcRule> M_PROC_RULE_XML = new XMLFormat<MProcRule>(MProcRule.class) {

        @Override
        public void read(javolution.xml.XMLFormat.InputElement xml, MProcRule mProcRule) throws XMLStreamException {
            mProcRule.id = xml.getAttribute(ID, -1);

            mProcRule.destTonMask = xml.getAttribute(DEST_TON_MASK, -1);
            mProcRule.destNpiMask = xml.getAttribute(DEST_NPI_MASK, -1);
            mProcRule.destDigMask = xml.getAttribute(DEST_DIG_MASK, "-1");

            String val = xml.getAttribute(ORIGINATING_MASK, "");
            if (val != null) {
                try {
                    mProcRule.originatingMask = Enum.valueOf(Sms.OriginationType.class, val);
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
        public void write(MProcRule mProcRule, javolution.xml.XMLFormat.OutputElement xml) throws XMLStreamException {
            xml.setAttribute(ID, mProcRule.id);

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
