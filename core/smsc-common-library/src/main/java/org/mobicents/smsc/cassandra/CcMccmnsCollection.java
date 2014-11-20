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

import java.util.ArrayList;

import javolution.xml.XMLFormat;
import javolution.xml.stream.XMLStreamException;

import org.mobicents.protocols.ss7.map.primitives.ArrayListSerializingBase;

/**
 * 
 * @author sergey vetyutnev
 * 
 */
public class CcMccmnsCollection {

    private static final String CC_MCCMNS = "ccMccmns";
    private static final String CC_MCCMNS_LIST = "ccMccmnsList";

    private ArrayList<CcMccmns> arr = new ArrayList<CcMccmns>();

    public void addCcMccmns(CcMccmns el) {
        this.arr.add(el);
    }

    public CcMccmns findMccmns(String countryCode) {
        for (CcMccmns ccMccmns : this.arr) {
            if (ccMccmns.getCountryCode().equals(""))
                return ccMccmns;
            if (ccMccmns.getCountryCode().startsWith(countryCode)) {
                return ccMccmns;
            }
        }
        return null;
    }

    public ArrayList<CcMccmns> getCollection() {
        return this.arr;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("CcMccmnsCollection=[");
        int i1 = 0;
        for (CcMccmns ccMccmns : this.arr) {
            if (i1 == 0)
                i1 = 1;
            else
                sb.append(", ");
            sb.append(ccMccmns.toString());
        }
        sb.append("]");

        return sb.toString();
    }

    /**
     * XML Serialization/Deserialization
     */
    protected static final XMLFormat<CcMccmnsCollection> CC_MCCMNS_COLLECTION_XML = new XMLFormat<CcMccmnsCollection>(CcMccmnsCollection.class) {

        @Override
        public void read(javolution.xml.XMLFormat.InputElement xml, CcMccmnsCollection ccMccmnsCollection) throws XMLStreamException {
            CcMccmnsCollection_CcMccmns al = xml.get(CC_MCCMNS_LIST, CcMccmnsCollection_CcMccmns.class);
            if (al != null) {
                ccMccmnsCollection.arr = al.getData();
            }
        }

        @Override
        public void write(CcMccmnsCollection ccMccmnsCollection, javolution.xml.XMLFormat.OutputElement xml) throws XMLStreamException {
            CcMccmnsCollection_CcMccmns al = new CcMccmnsCollection_CcMccmns(ccMccmnsCollection.arr);
            xml.add(al, CC_MCCMNS_LIST, CcMccmnsCollection_CcMccmns.class);
        }
    };

    public static class CcMccmnsCollection_CcMccmns extends ArrayListSerializingBase<CcMccmns> {

        public CcMccmnsCollection_CcMccmns() {
            super(CC_MCCMNS, CcMccmns.class);
        }

        public CcMccmnsCollection_CcMccmns(ArrayList<CcMccmns> data) {
            super(CC_MCCMNS, CcMccmns.class, data);
        }

    }

}
