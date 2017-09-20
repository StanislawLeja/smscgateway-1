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

package org.mobicents.smsc.library.test;

import org.mobicents.smsc.library.SmsExposureLayerData;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Created by mniemiec on 25.08.17.
 */
public class SmsExposureLayerDataTest {

    @Test(groups = { "SmsSet" })
    public void testExposureLayerDataParse() {

        String messageId = "123456";
        String correlationId = "112233";

        long elApiStart = 1503655696096L;
        long elApiStop = 1503655721872L;
        long elQueStart = 1503655736078L;
        long elQueStop = 1503655741991L;

        final String SEPARATOR = ":";

        StringBuilder sb = new StringBuilder();
        sb.append(messageId)
                .append(SEPARATOR)
                .append(correlationId)
                .append(SEPARATOR)
                .append(elApiStart)
                .append(SEPARATOR)
                .append(elApiStop)
                .append(SEPARATOR)
                .append(elQueStart)
                .append(SEPARATOR)
                .append(elQueStop);
        String elDataString = sb.toString();
        System.out.println(elDataString);

        SmsExposureLayerData data = new SmsExposureLayerData(elDataString);

        Assert.assertEquals(data.getMessageId(), messageId);
        Assert.assertEquals(data.getCorrelationId(), correlationId);
        Assert.assertEquals(data.getElApiStart(), elApiStart);
        Assert.assertEquals(data.getElApiStop(), elApiStop);
        Assert.assertEquals(data.getElQueStart(), elQueStart);
        Assert.assertEquals(data.getElQueStop(), elQueStop);
    }
}
