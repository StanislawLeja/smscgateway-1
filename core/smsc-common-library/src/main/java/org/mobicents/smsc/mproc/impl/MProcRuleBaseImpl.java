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

import javolution.util.FastList;
import javolution.xml.XMLFormat;
import javolution.xml.stream.XMLStreamException;

import org.mobicents.smsc.mproc.MProcMessage;
import org.mobicents.smsc.mproc.MProcMessageDestination;
import org.mobicents.smsc.mproc.MProcRule;
import org.mobicents.smsc.mproc.PostArrivalProcessor;
import org.mobicents.smsc.mproc.PostDeliveryProcessor;
import org.mobicents.smsc.mproc.PostImsiProcessor;

/**
*
* @author sergey vetyutnev
*
*/
public abstract class MProcRuleBaseImpl implements MProcRule {

    private static final String ID = "id";

    private int id;

    @Override
    public int getId() {
        return this.id;
    }

    @Override
    public void setId(int val) {
        this.id = val;
    }

    @Override
    public boolean isForPostArrivalState() {
        return false;
    }

    @Override
    public boolean isForPostImsiRequestState() {
        return false;
    }

    @Override
    public boolean isForPostDeliveryState() {
        return false;
    }

    @Override
    public boolean matches(MProcMessage messageDest) {
        return false;
    }

    @Override
    public boolean matches(MProcMessageDestination messageDest) {
        return false;
    }

    @Override
    public void onPostArrival(PostArrivalProcessor factory, MProcMessage message) throws Exception {
    }

    @Override
    public void onPostImsiRequest(PostImsiProcessor factory, MProcMessageDestination messages) throws Exception {
    }

    @Override
    public void onPostDelivery(PostDeliveryProcessor factory, MProcMessage message) throws Exception {
    }

    /**
     * splitting of a message and removing of empty substrings. Space is a splitter between parameters instances.
     *
     * @param parametersString source parameters String
     * @return a list of parameters
     */
    protected String[] splitParametersString(String parametersString) {
        String[] args0 = parametersString.split(" ");
        FastList<String> al1 = new FastList<String>();
        for (int i1 = 0; i1 < args0.length; i1++) {
            String s = args0[i1];
            if (s != null && s.length() > 0)
                al1.add(s);
        }
        String[] args = new String[al1.size()];
        al1.toArray(args);
        return args;
    }

    protected void writeParameter(StringBuilder sb, int parNumber, String name, Object value) {
        if (parNumber > 0)
            sb.append(", ");
        sb.append(name);
        sb.append("=");
        sb.append(value);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("mProc=[class=");
        sb.append(getRuleClassName());
        sb.append(", id=");
        sb.append(id);
        sb.append(", ");
        sb.append(this.getRuleParameters());
        sb.append("]");

        return sb.toString();
    }

    /**
     * XML Serialization/Deserialization
     */
    protected static final XMLFormat<MProcRuleBaseImpl> M_PROC_RULE_BASE_XML = new XMLFormat<MProcRuleBaseImpl>(MProcRuleBaseImpl.class) {

        @Override
        public void read(javolution.xml.XMLFormat.InputElement xml, MProcRuleBaseImpl mProcRule) throws XMLStreamException {
            mProcRule.id = xml.getAttribute(ID, -1);
        }

        @Override
        public void write(MProcRuleBaseImpl mProcRule, javolution.xml.XMLFormat.OutputElement xml) throws XMLStreamException {
            xml.setAttribute(ID, mProcRule.id);
        }
    };

}
