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

package org.mobicents.smsc.slee.resources.persistence;

import static org.testng.Assert.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.UUID;

import org.mobicents.protocols.ss7.map.api.primitives.AddressNature;
import org.mobicents.protocols.ss7.map.api.primitives.ISDNAddressString;
import org.mobicents.protocols.ss7.map.api.primitives.NumberingPlan;
import org.mobicents.protocols.ss7.map.primitives.IMSIImpl;
import org.mobicents.protocols.ss7.map.primitives.ISDNAddressStringImpl;
import org.mobicents.protocols.ss7.map.service.sms.LocationInfoWithLMSIImpl;
import org.mobicents.smsc.cassandra.DBOperations_C2;
import org.mobicents.smsc.cassandra.PersistenceException;
import org.mobicents.smsc.cassandra.PreparedStatementCollection_C3;
import org.mobicents.smsc.cassandra.SmType;
import org.mobicents.smsc.cassandra.Sms;
import org.mobicents.smsc.cassandra.SmsSet;
import org.mobicents.smsc.cassandra.TargetAddress;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.cloudhopper.smpp.tlv.Tlv;

/**
 *
 * @author sergey vetyutnev
 *
 */
public class TT_CassandraTest {

    private TT_PersistenceRAInterfaceProxy sbb;
    private boolean cassandraDbInited;

    private UUID id1 = UUID.fromString("59e815dc-49ad-4539-8cff-beb710a7de03");
    private UUID id2 = UUID.fromString("be26d2e9-1ba0-490c-bd5b-f04848127220");
    private UUID id3 = UUID.fromString("8bf7279f-3d4a-4494-8acd-cb9572c7ab33");
    private UUID id4 = UUID.fromString("c3bd98c2-355d-4572-8915-c6d0c767cae1");

    private TargetAddress ta1 = new TargetAddress(5, 1, "1111");
    private TargetAddress ta2 = new TargetAddress(5, 1, "1112");

    @BeforeMethod
    public void setUpClass() throws Exception {
        System.out.println("setUpClass");

        this.sbb = new TT_PersistenceRAInterfaceProxy();
        this.cassandraDbInited = this.sbb.testCassandraAccess();
        if (!this.cassandraDbInited)
            return;
        this.sbb.start();
    }

    @AfterMethod
    public void tearDownClass() throws Exception {
        System.out.println("tearDownClass");
        this.sbb.stop();
    }


    @Test(groups = { "cassandra" })
    public void testingDueSlotForTime() throws Exception {

        if (!this.cassandraDbInited)
            return;

        Date dt = new Date();
        long dueSlot = sbb.c2_getDueSlotForTime(dt);
        Date dt2 = sbb.c2_getTimeForDueSlot(dueSlot);
        long dueSlot2 = sbb.c2_getDueSlotForTime(dt2);
        Date dt3 = sbb.c2_getTimeForDueSlot(dueSlot);

        assertEquals(dueSlot, dueSlot2);
        assertTrue(dt2.equals(dt3));
    }

    @Test(groups = { "cassandra" })
    public void testingProcessingDueSlot() throws Exception {

        if (!this.cassandraDbInited)
            return;

        Date dt = new Date();
        long l0 = sbb.c2_getDueSlotForTime(dt);

        long l1 = sbb.c2_getCurrentDueSlot();
        long l2 = 222999;
        sbb.c2_setCurrentDueSlot(l2);
        long l3 = sbb.c2_getCurrentDueSlot();

        if (l1 > l0 || l1 < l0 - 100)
            fail("l1 value is bad");
        assertEquals(l2, l3);

        sbb.stop();
        sbb.start();

        long l4 = sbb.c2_getCurrentDueSlot();
        assertEquals(l2, l4);
    }

    @Test(groups = { "cassandra" })
    public void testingDueSlotWriting() throws Exception {

        if (!this.cassandraDbInited)
            return;

        long dueSlot = 101;
        long dueSlot2 = 102;
        boolean b1 = sbb.c2_checkDueSlotNotWriting(dueSlot);
        boolean b2 = sbb.c2_checkDueSlotNotWriting(dueSlot2);
        assertTrue(b1);
        assertTrue(b2);

        sbb.c2_registerDueSlotWriting(dueSlot);
        b1 = sbb.c2_checkDueSlotNotWriting(dueSlot);
        b2 = sbb.c2_checkDueSlotNotWriting(dueSlot2);
        assertFalse(b1);
        assertTrue(b2);

        sbb.c2_registerDueSlotWriting(dueSlot);
        b1 = sbb.c2_checkDueSlotNotWriting(dueSlot);
        b2 = sbb.c2_checkDueSlotNotWriting(dueSlot2);
        assertFalse(b1);
        assertTrue(b2);

        sbb.c2_registerDueSlotWriting(dueSlot2);
        b1 = sbb.c2_checkDueSlotNotWriting(dueSlot);
        b2 = sbb.c2_checkDueSlotNotWriting(dueSlot2);
        assertFalse(b1);
        assertFalse(b2);

        sbb.c2_unregisterDueSlotWriting(dueSlot);
        b1 = sbb.c2_checkDueSlotNotWriting(dueSlot);
        b2 = sbb.c2_checkDueSlotNotWriting(dueSlot2);
        assertFalse(b1);
        assertFalse(b2);

        sbb.c2_unregisterDueSlotWriting(dueSlot);
        b1 = sbb.c2_checkDueSlotNotWriting(dueSlot);
        b2 = sbb.c2_checkDueSlotNotWriting(dueSlot2);
        assertTrue(b1);
        assertFalse(b2);

        sbb.c2_unregisterDueSlotWriting(dueSlot);
        b1 = sbb.c2_checkDueSlotNotWriting(dueSlot);
        b2 = sbb.c2_checkDueSlotNotWriting(dueSlot2);
        assertTrue(b1);
        assertFalse(b2);

        sbb.c2_unregisterDueSlotWriting(dueSlot2);
        b1 = sbb.c2_checkDueSlotNotWriting(dueSlot);
        b2 = sbb.c2_checkDueSlotNotWriting(dueSlot2);
        assertTrue(b1);
        assertTrue(b2);
    }

    @Test(groups = { "cassandra" })
    public void testingDueSlotForTargetId() throws Exception {

        if (!this.cassandraDbInited)
            return;

        Date dt = new Date();
        String targetId = "111333";
        String targetId2 = "111444";
        PreparedStatementCollection_C3 psc = sbb.getStatementCollection(dt);

        long l1 = sbb.c2_getDueSlotForTargetId(psc, targetId);
        long l2 = sbb.c2_getDueSlotForTargetId(psc, targetId2);
        assertEquals(l1, 0);
        assertEquals(l2, 0);

        long newDueSlot = sbb.c2_getDueSlotForNewSms();
        sbb.c2_updateDueSlotForTargetId(targetId, newDueSlot);

        l1 = sbb.c2_getDueSlotForTargetId(psc, targetId);
        l2 = sbb.c2_getDueSlotForTargetId(psc, targetId2);
        assertEquals(l1, newDueSlot);
        assertEquals(l2, 0);
    }

    @Test(groups = { "cassandra" })
    public void testingLifeCycle() throws Exception {

        if (!this.cassandraDbInited)
            return;

        long dueSlot = this.addingNewMessages();

        this.readAlertMessage();

        SmsSet smsSet = this.readDueSlotMessage(dueSlot);

        archiveMessage(smsSet);

    }

    public long addingNewMessages() throws Exception {
        Date dt = new Date();
        PreparedStatementCollection_C3 psc = sbb.getStatementCollection(dt);

        // adding two messages for "1111"
        TargetAddress lock = this.sbb.obtainSynchroObject(ta1);
        long dueSlot;
        try {
            synchronized (lock) {
                Sms sms_a1 = this.createTestSms(1, ta1.getAddr(), id1);
                Sms sms_a2 = this.createTestSms(2, ta1.getAddr(), id2);
                Sms sms_a3 = this.createTestSms(3, ta1.getAddr(), id3);

                dueSlot = this.sbb.c2_getDueSlotForTargetId(psc, ta1.getTargetId());
                if (dueSlot == 0 || dueSlot <= sbb.c2_getCurrentDueSlot()) {
                    dueSlot = sbb.c2_getDueSlotForNewSms();
                    sbb.c2_updateDueSlotForTargetId(ta1.getTargetId(), dueSlot);
                }
                sms_a1.setDueSlot(dueSlot);
                sms_a2.setDueSlot(dueSlot);
                sms_a3.setDueSlot(dueSlot);

                sbb.c2_registerDueSlotWriting(dueSlot);
                try {
                    sbb.c2_createRecordCurrent(sms_a1);
                    sbb.c2_createRecordCurrent(sms_a2);
                    sbb.c2_createRecordCurrent(sms_a3);
                } finally {
                    sbb.c2_unregisterDueSlotWriting(dueSlot);
                }
            }
        } finally {
            this.sbb.obtainSynchroObject(lock);
        }

        // adding a messages for "1112"
        lock = this.sbb.obtainSynchroObject(ta2);
        try {
            synchronized (lock) {
                Sms sms_a1 = this.createTestSms(4, ta2.getAddr(), id4);

                sbb.c2_updateDueSlotForTargetId(ta2.getTargetId(), dueSlot);
                sms_a1.setDueSlot(dueSlot);

                sbb.c2_registerDueSlotWriting(dueSlot);
                try {
                    sbb.c2_createRecordCurrent(sms_a1);
                } finally {
                    sbb.c2_unregisterDueSlotWriting(dueSlot);
                }
            }
        } finally {
            this.sbb.obtainSynchroObject(lock);
        }

        return dueSlot;
    }

    public void readAlertMessage() throws Exception {
        Date dt = new Date();
        PreparedStatementCollection_C3 psc = sbb.getStatementCollection(dt);

        // reading "1112" for Alert
        TargetAddress lock = this.sbb.obtainSynchroObject(ta2);
        try {
            synchronized (lock) {
                long dueSlot = this.sbb.c2_getDueSlotForTargetId(psc, ta2.getTargetId());
                if (dueSlot == 0) {
                    fail("Bad dueSlot for reading of ta2");
                }

                sbb.c2_registerDueSlotWriting(dueSlot);
                SmsSet smsSet;
                try {
                    smsSet = sbb.c2_getRecordListForTargeId(dueSlot, ta2.getTargetId());
                } finally {
                    sbb.c2_unregisterDueSlotWriting(dueSlot);
                }
                assertEquals(smsSet.getSmsCount(), 1);
                Sms sms = smsSet.getSms(0);
                assertEquals(sms.getDueSlot(), dueSlot);
                this.checkTestSms(4, sms, id4, false);

                sbb.c2_updateInSystem(sms, DBOperations_C2.IN_SYSTEM_INPROCESS);
            }
        } finally {
            this.sbb.obtainSynchroObject(lock);
        }
    }

    public SmsSet readDueSlotMessage(long dueSlot) throws Exception {
        // reading dueSlot
        TargetAddress lock = this.sbb.obtainSynchroObject(ta2);
        try {
            synchronized (lock) {
                sbb.c2_registerDueSlotWriting(dueSlot);
                ArrayList<SmsSet> lst0, lst;
                try {
                    lst0 = sbb.c2_getRecordList(dueSlot);
                    lst = sbb.c2_sortRecordList(lst0);
                } finally {
                    sbb.c2_unregisterDueSlotWriting(dueSlot);
                }

                assertEquals(lst.size(), 1);
                SmsSet smsSet = lst.get(0);
                assertEquals(smsSet.getSmsCount(), 3);
                Sms sms1 = smsSet.getSms(0);
                Sms sms2 = smsSet.getSms(1);
                Sms sms3 = smsSet.getSms(2);
                assertEquals(sms1.getDueSlot(), dueSlot);
                assertEquals(sms2.getDueSlot(), dueSlot);
                assertEquals(sms3.getDueSlot(), dueSlot);
                this.checkTestSms(1, sms1, id1, false);
                this.checkTestSms(2, sms2, id2, false);
                this.checkTestSms(3, sms3, id3, false);

                return smsSet;
            }
        } finally {
            this.sbb.obtainSynchroObject(lock);
        }
    }

    public void archiveMessage(SmsSet smsSet) throws Exception {
        for (int i1 = 0; i1 < 3; i1++) {
            Sms sms = smsSet.getSms(i1);
            
            sms.getSmsSet().setType(SmType.SMS_FOR_SS7);
            IMSIImpl imsi = new IMSIImpl("12345678900000");
            sms.getSmsSet().setImsi(imsi);
            ISDNAddressStringImpl networkNodeNumber = new ISDNAddressStringImpl(AddressNature.international_number, NumberingPlan.ISDN, "2223334444");
            LocationInfoWithLMSIImpl locationInfoWithLMSI = new LocationInfoWithLMSIImpl(networkNodeNumber, null, null, null, null);
            sms.getSmsSet().setLocationInfoWithLMSI(locationInfoWithLMSI);
            
            sbb.c2_createRecordArchive(sms);
        }

        Sms sms = smsSet.getSms(0);
        SmsProxy smsx = sbb.obtainArchiveSms(sms.getDueSlot(), sms.getSmsSet().getDestAddr(), sms.getDbId());

        this.checkTestSms(1, smsx.sms, sms.getDbId(), true);
    }

    private Sms createTestSms(int num, String number, UUID id) throws Exception {
        PreparedStatementCollection_C3 psc = sbb.getStatementCollection(new Date());

        SmsSet smsSet = new SmsSet();
        smsSet.setDestAddr(number);
        smsSet.setDestAddrNpi(1);
        smsSet.setDestAddrTon(5);

        Sms sms = new Sms();
        sms.setSmsSet(smsSet);

//      sms.setDbId(UUID.randomUUID());
        sms.setDbId(id);
        sms.setSourceAddr("11112_" + num);
        sms.setSourceAddrTon(14 + num);
        sms.setSourceAddrNpi(11 + num);
        sms.setMessageId(8888888 + num);
        sms.setMoMessageRef(102 + num);

        sms.setOrigEsmeName("esme_" + num);
        sms.setOrigSystemId("sys_" + num);

        sms.setSubmitDate(new GregorianCalendar(2013, 1, 15, 12, 00 + num).getTime());
        sms.setDeliveryDate(new GregorianCalendar(2013, 1, 15, 12, 15 + num).getTime());

        sms.setServiceType("serv_type__" + num);
        sms.setEsmClass(11 + num);
        sms.setProtocolId(12 + num);
        sms.setPriority(13 + num);
        sms.setRegisteredDelivery(14 + num);
        sms.setReplaceIfPresent(15 + num);
        sms.setDataCoding(16 + num);
        sms.setDefaultMsgId(17 + num);

        sms.setShortMessage(new byte[] { (byte)(21 + num), 23, 25, 27, 29 });

        sms.setScheduleDeliveryTime(new GregorianCalendar(2013, 1, 20, 10, 00 + num).getTime());
        sms.setValidityPeriod(new GregorianCalendar(2013, 1, 23, 13, 33 + num).getTime());

        // short tag, byte[] value, String tagName
        Tlv tlv = new Tlv((short) 5, new byte[] { (byte) (1 + num), 2, 3, 4, 5 });
        sms.getTlvSet().addOptionalParameter(tlv);
        tlv = new Tlv((short) 6, new byte[] { (byte) (6 + num), 7, 8 });
        sms.getTlvSet().addOptionalParameter(tlv);

        return sms;
    }

    private void checkTestSms(int num, Sms sms, UUID id, boolean isArchive) {

        assertTrue(sms.getDbId().equals(id));

        assertEquals(sms.getSourceAddr(), "11112_" + num);
        assertEquals(sms.getSourceAddrTon(), 14 + num);
        assertEquals(sms.getSourceAddrNpi(), 11 + num);

        assertEquals(sms.getMessageId(), 8888888 + num);
        assertEquals(sms.getMoMessageRef(), 102 + num);
        assertEquals(sms.getOrigEsmeName(), "esme_" + num);
        assertEquals(sms.getOrigSystemId(), "sys_" + num);

        assertTrue(sms.getSubmitDate().equals(new GregorianCalendar(2013, 1, 15, 12, 00 + num).getTime()));

        assertEquals(sms.getServiceType(), "serv_type__" + num);
        assertEquals(sms.getEsmClass(), 11 + num);
        assertEquals(sms.getProtocolId(), 12 + num);
        assertEquals(sms.getPriority(), 13 + num);
        assertEquals(sms.getRegisteredDelivery(), 14 + num);
        assertEquals(sms.getReplaceIfPresent(), 15 + num);
        assertEquals(sms.getDataCoding(), 16 + num);
        assertEquals(sms.getDefaultMsgId(), 17 + num);

        assertEquals(sms.getShortMessage(), new byte[] { (byte) (21 + num), 23, 25, 27, 29 });

        assertEquals(sms.getScheduleDeliveryTime(), new GregorianCalendar(2013, 1, 20, 10, 00 + num).getTime());
        assertEquals(sms.getValidityPeriod(), new GregorianCalendar(2013, 1, 23, 13, 33 + num).getTime());

        // short tag, byte[] value, String tagName
        assertEquals(sms.getTlvSet().getOptionalParameterCount(), 2);
        assertEquals(sms.getTlvSet().getOptionalParameter((short) 5).getValue(), new byte[] { (byte) (1 + num), 2, 3, 4, 5 });
        assertEquals(sms.getTlvSet().getOptionalParameter((short) 6).getValue(), new byte[] { (byte) (6 + num), 7, 8 });
    }

}
