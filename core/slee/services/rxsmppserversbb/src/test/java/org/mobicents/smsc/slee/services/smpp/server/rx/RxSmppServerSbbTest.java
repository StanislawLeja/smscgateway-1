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

package org.mobicents.smsc.slee.services.smpp.server.rx;

import static org.testng.Assert.*;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import org.mobicents.smsc.smpp.SmppEncoding;
import org.mobicents.smsc.smpp.SmscPropertiesManagement;
import org.testng.annotations.Test;

/**
 * 
 * @author sergey vetyutnev
 * 
 */
public class RxSmppServerSbbTest {

    @Test(groups = { "TxSmppServer" })
    public void testSubmitSm_createSmsEvent() throws Exception {
        RxSmppServerSbbProxy proxy = new RxSmppServerSbbProxy();

        String s1 = "������Hel";
        String s2 = "Hello bbs";

        Charset utf8Charset = Charset.forName("UTF-8");
        ByteBuffer bf = utf8Charset.encode(s1);
        byte[] msgUtf8 = new byte[bf.limit()];
        bf.get(msgUtf8);

        Charset ucs2Charset = Charset.forName("UTF-16BE");
        bf = ucs2Charset.encode(s1);
        byte[] msgUcs2 = new byte[bf.limit()];
        bf.get(msgUcs2);

        RxSmppServerSbb.smscPropertiesManagement.setSmppEncodingForGsm7(SmppEncoding.Utf8);
        byte[] res = proxy.recodeShortMessage(0, s1, null);
        assertEquals(res, msgUtf8);

        RxSmppServerSbb.smscPropertiesManagement.setSmppEncodingForUCS2(SmppEncoding.Utf8);
        res = proxy.recodeShortMessage(8, s1, null);
        assertEquals(res, msgUtf8);

        RxSmppServerSbb.smscPropertiesManagement.setSmppEncodingForUCS2(SmppEncoding.Unicode);
        res = proxy.recodeShortMessage(8, s1, null);
        assertEquals(res, msgUcs2);

        RxSmppServerSbb.smscPropertiesManagement.setSmppEncodingForGsm7(SmppEncoding.Unicode);
        byte[] udh = new byte[] { 0x05, 0x00, 0x03, 0x29, 0x02, 0x02 };
        byte[] aMsgB = new byte[msgUcs2.length + udh.length];
        System.arraycopy(udh, 0, aMsgB, 0, udh.length);
        System.arraycopy(msgUcs2, 0, aMsgB, udh.length, msgUcs2.length);
        res = proxy.recodeShortMessage(0, s1, udh);
        assertEquals(res, aMsgB);


        Charset isoCharset = Charset.forName("ISO-8859-1");
        byte[] msgAscii = s2.getBytes(isoCharset);
        byte[] aMsgC = new byte[msgAscii.length + udh.length];
        System.arraycopy(udh, 0, aMsgC, 0, udh.length);
        System.arraycopy(msgAscii, 0, aMsgC, udh.length, msgAscii.length);
        res = proxy.recodeShortMessage(4, s2, udh);
        assertEquals(res, aMsgC);
    }

    public class RxSmppServerSbbProxy extends RxSmppServerSbb {

        public RxSmppServerSbbProxy() {
            RxSmppServerSbb.smscPropertiesManagement = SmscPropertiesManagement.getInstance("Test");
        }

        protected byte[] recodeShortMessage(int dataCoding, String msg, byte[] udh) {
            return super.recodeShortMessage(dataCoding, msg, udh);
        }

        @Override
        public void setTargetId(String smsDeliveryData) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public String getTargetId() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public void setCurrentMsgNum(int currentMsgNum) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public int getCurrentMsgNum() {
            // TODO Auto-generated method stub
            return 0;
        }

    }
}
