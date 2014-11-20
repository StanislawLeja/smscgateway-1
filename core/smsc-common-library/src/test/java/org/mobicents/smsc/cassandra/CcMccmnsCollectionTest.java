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

package org.mobicents.smsc.cassandra;

import static org.testng.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;

import javolution.xml.XMLObjectReader;
import javolution.xml.XMLObjectWriter;

import org.testng.annotations.Test;

/**
*
* @author sergey vetyutnev
* 
*/
public class CcMccmnsCollectionTest {

    @Test(groups = { "CcMccmnsCollection" })
    public void testFunc() throws Exception {

        CcMccmnsCollection original = new CcMccmnsCollection();
        original.addCcMccmns(new CcMccmns("1111", "222", "00001"));
        original.addCcMccmns(new CcMccmns("3333", "444", null));
        original.addCcMccmns(new CcMccmns("", "555", ""));

        CcMccmns s1 = original.findMccmns("111");
        CcMccmns s2 = original.findMccmns("1111");
        CcMccmns s3 = original.findMccmns("11111");
        CcMccmns s4 = original.findMccmns("3");
        CcMccmns s5 = original.findMccmns("4444");

        assertEquals(s1.getMccMnc(), "222");
        assertEquals(s2.getMccMnc(), "222");
        assertEquals(s3.getMccMnc(), "555");
        assertEquals(s4.getMccMnc(), "444");
        assertEquals(s5.getMccMnc(), "555");
    }

    @Test(groups = { "CcMccmnsCollection" })
    public void testSerialition() throws Exception {

        CcMccmnsCollection original = new CcMccmnsCollection();
        original.addCcMccmns(new CcMccmns("1111", "222", "00001"));
        original.addCcMccmns(new CcMccmns("3333", "444", null));
        original.addCcMccmns(new CcMccmns("", "555", ""));

        // Writes the area to a file.
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        XMLObjectWriter writer = XMLObjectWriter.newInstance(baos);
        writer.setIndentation("\t");
        writer.write(original, "CcMccmnsCollection", CcMccmnsCollection.class);
        writer.close();

        byte[] rawData = baos.toByteArray();
        String serializedEvent = new String(rawData);

        System.out.println(serializedEvent);

        ByteArrayInputStream bais = new ByteArrayInputStream(rawData);
        XMLObjectReader reader = XMLObjectReader.newInstance(bais);
        CcMccmnsCollection copy = reader.read("CcMccmnsCollection", CcMccmnsCollection.class);

        ArrayList<CcMccmns> arr = copy.getCollection();
        assertEquals(arr.size(), 3);
        CcMccmns el = arr.get(0);
        assertEquals(el.getCountryCode(), "1111");
        assertEquals(el.getMccMnc(), "222");
        assertEquals(el.getSmsc(), "00001");
        el = arr.get(1);
        assertNull(el.getSmsc());
        el = arr.get(2);
        assertEquals(el.getSmsc(), "");
    }

}
