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
import org.mobicents.smsc.smpp.SmscPropertiesManagement;
import org.testng.annotations.Test;

import com.cloudhopper.smpp.SmppConstants;

/**
 * 
 * @author sergey vetyutnev
 * 
 */
public class RxSmppServerSbbTest {

    @Test(groups = { "TxSmppServer" })
    public void testSubmitSm_createSmsEvent() throws Exception {
        RxSmppServerSbbProxy proxy = new RxSmppServerSbbProxy();

        String s1 = "ПриветHel";

        Charset utf8Charset = Charset.forName("UTF-8");
        ByteBuffer bf = utf8Charset.encode(s1);
        byte[] msgUtf8 = new byte[bf.limit()];
        bf.get(msgUtf8);

        Charset ucs2Charset = Charset.forName("UTF-16BE");
        bf = ucs2Charset.encode(s1);
        byte[] msgUcs2 = new byte[bf.limit()];
        bf.get(msgUcs2);

        RxSmppServerSbb.smscPropertiesManagement.setSmppEncodingForUCS2(0);
        byte[] res = proxy.recodeShortMessage(0, 0, msgUcs2);
        assertEquals(res, msgUcs2);

        RxSmppServerSbb.smscPropertiesManagement.setSmppEncodingForUCS2(0);
        res = proxy.recodeShortMessage(0, 8, msgUcs2);
        assertEquals(res, msgUtf8);

        RxSmppServerSbb.smscPropertiesManagement.setSmppEncodingForUCS2(1);
        res = proxy.recodeShortMessage(0, 8, msgUcs2);
        assertEquals(res, msgUcs2);

        RxSmppServerSbb.smscPropertiesManagement.setSmppEncodingForUCS2(0);
        byte[] udh = new byte[] { 0x05, 0x00, 0x03, 0x29, 0x02, 0x02 };
        byte[] aMsgB = new byte[msgUcs2.length + udh.length];
        System.arraycopy(udh, 0, aMsgB, 0, udh.length);
        System.arraycopy(msgUcs2, 0, aMsgB, udh.length, msgUcs2.length);
        res = proxy.recodeShortMessage(SmppConstants.ESM_CLASS_UDHI_MASK, 8, aMsgB);
        byte[] bf1 = new byte[udh.length];
        byte[] bf2 = new byte[res.length - udh.length];
        System.arraycopy(res, 0, bf1, 0, udh.length);
        System.arraycopy(res, udh.length, bf2, 0, bf2.length);
        assertEquals(bf1, udh);
        assertEquals(bf2, msgUtf8);
    }

    public class RxSmppServerSbbProxy extends RxSmppServerSbb {

        public RxSmppServerSbbProxy() {
            RxSmppServerSbb.smscPropertiesManagement = SmscPropertiesManagement.getInstance("Test");
        }

        protected byte[] recodeShortMessage(int esmeClass, int dataCoding, byte[] msg) {
            return super.recodeShortMessage(esmeClass, dataCoding, msg);
        }

        @Override
        public void setSmsDeliveryData(SmsDeliveryData smsDeliveryData) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public SmsDeliveryData getSmsDeliveryData() {
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
