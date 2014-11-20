/*
 * TeleStax, Open Source Cloud Communications  Copyright 2012. 
 * and individual contributors
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

package org.mobicents.smsc.cassandra;

import javolution.xml.XMLFormat;
import javolution.xml.stream.XMLStreamException;

/**
 * 
 * @author sergey vetyutnev
 * 
 */
public class CcMccmns {

    private static final String COUNTRY_CODE = "countryCode";
    private static final String MCC_MNC = "mccMnc";
    private static final String SMSC = "smsc";

    private static final String DEFAULT_STRING = null;

    private String countryCode;
    private String mccMnc;
    private String smsc;

    public CcMccmns(String countryCode, String mccMnc, String smsc) {
        this.countryCode = countryCode;
        this.mccMnc = mccMnc;
        this.smsc = smsc;
    }

    public CcMccmns() {
    }

    public String getCountryCode() {
        return countryCode;
    }

    public String getMccMnc() {
        return mccMnc;
    }

    public String getSmsc() {
        return smsc;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("CcMccmns=[");
        sb.append("countryCode=");
        sb.append(countryCode);
        sb.append(", mccMnc=");
        sb.append(mccMnc);
        sb.append(", smsc=");
        sb.append(smsc);
        sb.append("]");

        return sb.toString();
    }

    /**
     * XML Serialization/Deserialization
     */
    protected static final XMLFormat<CcMccmns> CC_MCCMNS_XML = new XMLFormat<CcMccmns>(CcMccmns.class) {

        @Override
        public void read(javolution.xml.XMLFormat.InputElement xml, CcMccmns ccMccmns) throws XMLStreamException {
            ccMccmns.countryCode = xml.getAttribute(COUNTRY_CODE, DEFAULT_STRING);
            ccMccmns.mccMnc = xml.getAttribute(MCC_MNC, DEFAULT_STRING);
            ccMccmns.smsc = xml.getAttribute(SMSC, DEFAULT_STRING);
        }

        @Override
        public void write(CcMccmns ccMccmns, javolution.xml.XMLFormat.OutputElement xml) throws XMLStreamException {
            xml.setAttribute(COUNTRY_CODE, ccMccmns.countryCode);
            xml.setAttribute(MCC_MNC, ccMccmns.mccMnc);
            xml.setAttribute(SMSC, ccMccmns.smsc);
        }
    };

}
