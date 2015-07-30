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

package org.mobicents.smsc.library;

import static org.testng.Assert.*;

import org.mobicents.protocols.ss7.map.api.primitives.AddressNature;
import org.mobicents.protocols.ss7.map.api.primitives.AddressString;
import org.mobicents.protocols.ss7.map.api.primitives.ISDNAddressString;
import org.mobicents.protocols.ss7.map.api.primitives.NumberingPlan;
import org.mobicents.protocols.ss7.map.primitives.AddressStringImpl;
import org.mobicents.protocols.ss7.map.primitives.ISDNAddressStringImpl;
import org.testng.annotations.Test;

/**
*
* @author sergey vetyutnev
* 
*/
public class SmsSetCasheTest {

    @Test(groups = { "ShellExecutor" })
    public void testSmppShellExecutor() throws Exception {

        int correlationIdLiveTime = 2;
        SmsSetCache.start(correlationIdLiveTime);
        SmsSetCache ssc = SmsSetCache.getInstance();

        String correlationID = "000000000011111";
        ISDNAddressString msisdn = new ISDNAddressStringImpl(AddressNature.international_number, NumberingPlan.ISDN, "11111111");
        AddressString serviceCentreAddress = new AddressStringImpl(AddressNature.international_number, NumberingPlan.ISDN, "22222222");
        CorrelationIdValue elem = new CorrelationIdValue(correlationID, msisdn, serviceCentreAddress, 0);
        ssc.putCorrelationIdCacheElement(elem, correlationIdLiveTime);

        Thread.sleep(3000);
        CorrelationIdValue v1 = ssc.getCorrelationIdCacheElement(correlationID);
        assertNotNull(v1);

        Thread.sleep(2000);
        CorrelationIdValue v2 = ssc.getCorrelationIdCacheElement(correlationID);
        assertNull(v2);

        SmsSetCache.stop();
    }

    @Test(groups = { "ShellExecutor" })
    public void testSmsDataCoding() throws Exception {
        int encoded = 10 + 5 * 256 + 1 * 256 * 256;
        
        Sms sms = new Sms();

        sms.setDataCoding(10);
        sms.setNationalLanguageLockingShift(5);
        sms.setNationalLanguageSingleShift(1);

        int d = sms.getDataCodingForDatabase();
        assertEquals(d, encoded);

        sms = new Sms();
        sms.setDataCodingForDatabase(encoded);

        assertEquals(sms.getDataCoding(), 10);
        assertEquals(sms.getNationalLanguageLockingShift(), 5);
        assertEquals(sms.getNationalLanguageSingleShift(), 1);
    }

}
