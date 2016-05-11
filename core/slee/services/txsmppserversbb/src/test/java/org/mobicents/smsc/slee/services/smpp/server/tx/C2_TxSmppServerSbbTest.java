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

package org.mobicents.smsc.slee.services.smpp.server.tx;

import static org.testng.Assert.*;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Date;

import javax.slee.ActivityContextInterface;
import javax.slee.SLEEException;
import javax.slee.SbbLocalObject;
import javax.slee.TransactionRequiredLocalException;
import javax.slee.TransactionRolledbackLocalException;

import org.mobicents.protocols.ss7.map.api.smstpdu.CharacterSet;
import org.mobicents.protocols.ss7.map.api.smstpdu.DataCodingGroup;
import org.mobicents.protocols.ss7.map.api.smstpdu.UserDataHeader;
import org.mobicents.protocols.ss7.map.api.smstpdu.UserDataHeaderElement;
import org.mobicents.protocols.ss7.map.smstpdu.ConcatenatedShortMessagesIdentifierImpl;
import org.mobicents.protocols.ss7.map.smstpdu.DataCodingSchemeImpl;
import org.mobicents.protocols.ss7.map.smstpdu.UserDataHeaderImpl;
import org.mobicents.slee.ChildRelationExt;
import org.mobicents.smsc.cassandra.DBOperations_C2;
import org.mobicents.smsc.cassandra.PersistenceException;
import org.mobicents.smsc.cassandra.PreparedStatementCollection_C3;
import org.mobicents.smsc.domain.MProcManagement;
import org.mobicents.smsc.domain.SmscManagement;
import org.mobicents.smsc.domain.SmscPropertiesManagement;
import org.mobicents.smsc.domain.StoreAndForwordMode;
import org.mobicents.smsc.library.MessageUtil;
import org.mobicents.smsc.library.Sms;
import org.mobicents.smsc.library.SmsSet;
import org.mobicents.smsc.library.SmscProcessingException;
import org.mobicents.smsc.library.TargetAddress;
import org.mobicents.smsc.mproc.impl.MProcRuleFactoryDefault;
import org.mobicents.smsc.slee.resources.persistence.PersistenceRAInterface;
import org.mobicents.smsc.slee.resources.persistence.SmppSessionsProxy;
import org.mobicents.smsc.slee.resources.persistence.TT_PersistenceRAInterfaceProxy;
import org.mobicents.smsc.slee.resources.persistence.TraceProxy;
import org.mobicents.smsc.slee.resources.scheduler.SchedulerRaSbbInterface;
import org.mobicents.smsc.slee.resources.smpp.server.SmppSessions;
import org.mobicents.smsc.slee.resources.smpp.server.SmppTransaction;
import org.mobicents.smsc.smpp.Esme;
import org.mobicents.smsc.smpp.SmppEncoding;
import org.mobicents.smsc.smpp.SmppInterfaceVersionType;
import org.mobicents.smsc.smpp.SmppManagement;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.cloudhopper.smpp.SmppBindType;
import com.cloudhopper.smpp.SmppConstants;
import com.cloudhopper.smpp.SmppSession;
import com.cloudhopper.smpp.pdu.BaseSm;
import com.cloudhopper.smpp.pdu.DataSm;
import com.cloudhopper.smpp.pdu.PduResponse;
import com.cloudhopper.smpp.pdu.SubmitMulti;
import com.cloudhopper.smpp.pdu.SubmitMultiResp;
import com.cloudhopper.smpp.pdu.SubmitSm;
import com.cloudhopper.smpp.tlv.Tlv;
import com.cloudhopper.smpp.tlv.TlvConvertException;
import com.cloudhopper.smpp.type.Address;
import com.datastax.driver.core.ProtocolVersion;

/**
 * 
 * @author sergey vetyutnev
 * 
 */
public class C2_TxSmppServerSbbTest {

	private TxSmppServerSbbProxy sbb;
	private TT_PersistenceRAInterfaceProxy pers;
	private SmppSessionsProxy smppSess;
	private boolean cassandraDbInited;

    private TargetAddress ta1 = new TargetAddress(1, 1, "5555", 0);
    private TargetAddress ta2 = new TargetAddress(1, 1, "5556", 0);

    private static String sMsg = "������Hel";
    private static String sMsg_2 = "Msg 2";
    private static String sMsg_3 = "Msg 3";
    private static String sMsg_4 = "Msg 4 []";
	private static byte[] msgUtf8, msgUtf8_2, msgUtf8_3;
    private static byte[] msgUcs2;
    private static byte[] msgGsm7;
    private static byte[] udhCode;
	private byte[] msg_ref_num = { 0, 10 };
	private Date scheduleDeliveryTime;

	static {
		String s1 = sMsg;

        Charset utf8Charset = Charset.forName("UTF-8");
        ByteBuffer bf = utf8Charset.encode(s1);
        msgUtf8 = new byte[bf.limit()];
        bf.get(msgUtf8);

        bf = utf8Charset.encode(sMsg_2);
        msgUtf8_2 = new byte[bf.limit()];
        bf.get(msgUtf8_2);

        bf = utf8Charset.encode(sMsg_3);
        msgUtf8_3 = new byte[bf.limit()];
        bf.get(msgUtf8_3);

        UserDataHeader udh = new UserDataHeaderImpl();
        UserDataHeaderElement informationElement = new ConcatenatedShortMessagesIdentifierImpl(false, 20, 5, 2);
        udh.addInformationElement(informationElement);
        udhCode = udh.getEncodedData();

        Charset ucs2Charset = Charset.forName("UTF-16BE");
		bf = ucs2Charset.encode(s1);
        msgUcs2 = new byte[udhCode.length + bf.limit()];
        bf.get(msgUcs2, udhCode.length, bf.limit());
        System.arraycopy(udhCode, 0, msgUcs2, 0, udhCode.length);

        // Msg 4 []
        msgGsm7 = new byte[] { 0x4D, 0x73, 0x67, 0x20, 0x34, 0x20, 0x1B, 0x3C, 0x1B, 0x3E };

        MProcManagement.getInstance("TestTxSmpp");
        try {
            MProcManagement.getInstance().destroyMProcRule(1);
            MProcManagement.getInstance().destroyMProcRule(2);
            MProcManagement.getInstance().destroyMProcRule(3);
        } catch (Exception e) {
        }
	}

	@BeforeMethod
	public void setUpClass() throws Exception {
		System.out.println("setUpClass");

		this.pers = new TT_PersistenceRAInterfaceProxy();
		this.cassandraDbInited = this.pers.testCassandraAccess();
		if (!this.cassandraDbInited)
			return;
		this.pers.start();

		this.sbb = new TxSmppServerSbbProxy(this.pers);

        SmscPropertiesManagement.getInstance("Test");
        SmscPropertiesManagement.getInstance().setSmscStopped(false);
        SmscPropertiesManagement.getInstance().setStoreAndForwordMode(StoreAndForwordMode.normal);
        MProcManagement.getInstance("Test");
	}

	@AfterMethod
	public void tearDownClass() throws Exception {
		System.out.println("tearDownClass");
	}

	@Test(groups = { "TxSmppServer" })
	public void testSubmitSm() throws Exception {

		if (!this.cassandraDbInited)
			return;

		this.smppSess = new SmppSessionsProxy();
		this.sbb.setSmppServerSessions(smppSess);

		int windowSize = SmppConstants.DEFAULT_WINDOW_SIZE;
		long connectTimeout = SmppConstants.DEFAULT_CONNECT_TIMEOUT;
		long requestExpiryTimeout = SmppConstants.DEFAULT_REQUEST_EXPIRY_TIMEOUT;
		long windowMonitorInterval = SmppConstants.DEFAULT_WINDOW_MONITOR_INTERVAL;
		long windowWaitTimeout = SmppConstants.DEFAULT_WINDOW_WAIT_TIMEOUT;

		Esme esme = new Esme("Esme_1", "Esme_systemId_1", "pwd", "host", 0, false, null,
				SmppInterfaceVersionType.SMPP34, -1, -1, null, SmppBindType.TRANSCEIVER, SmppSession.Type.CLIENT,
				windowSize, connectTimeout, requestExpiryTimeout, windowMonitorInterval, windowWaitTimeout, "Esme_1",
				true, 30000, -1, -1, "^[0-9a-zA-Z]*", -1, -1, "^[0-9a-zA-Z]*", 0, 0, 0, 0, 0, -1, -1, -1, -1, true);
		ActivityContextInterface aci = new SmppTransactionProxy(esme);

		SubmitSm event = new SubmitSm();
		Date curDate = new Date();
		this.fillSm(event, curDate, true);
		event.setShortMessage(msgUcs2);

		long dueSlot = this.pers.c2_getDueSlotForTime(scheduleDeliveryTime);
		PreparedStatementCollection_C3 psc = this.pers.getStatementCollection(scheduleDeliveryTime);
		int b1 = this.pers.checkSmsExists(dueSlot, ta1.getTargetId());
		long b2 = this.pers.c2_getDueSlotForTargetId(psc, ta1.getTargetId());
		assertEquals(b1, 0);
		assertEquals(b2, 0L);

		TxSmppServerSbb.smscPropertiesManagement.setSmppEncodingForUCS2(SmppEncoding.Unicode);
		this.sbb.onSubmitSm(event, aci);

		b1 = this.pers.checkSmsExists(dueSlot, ta1.getTargetId());
		assertEquals(b1, 1);

		SmsSet smsSet = this.pers.c2_getRecordListForTargeId(dueSlot, ta1.getTargetId());
		this.checkSmsSet(smsSet, curDate, true);
		Sms sms = smsSet.getSms(0);
        assertEquals(sms.getShortMessageText(), sMsg); // msgUcs2
        assertEquals(sms.getShortMessageBin(), udhCode);

		assertEquals(this.smppSess.getReqList().size(), 0);
		assertEquals(this.smppSess.getRespList().size(), 1);

		PduResponse resp = this.smppSess.getRespList().get(0);
		assertEquals(resp.getCommandStatus(), 0);
		assertEquals(resp.getOptionalParameterCount(), 0);
	}

    @Test(groups = { "TxSmppServer" })
    public void testSubmitSm_Gsm7Enc() throws Exception {

        if (!this.cassandraDbInited)
            return;

        this.smppSess = new SmppSessionsProxy();
        this.sbb.setSmppServerSessions(smppSess);

        int windowSize = SmppConstants.DEFAULT_WINDOW_SIZE;
        long connectTimeout = SmppConstants.DEFAULT_CONNECT_TIMEOUT;
        long requestExpiryTimeout = SmppConstants.DEFAULT_REQUEST_EXPIRY_TIMEOUT;
        long windowMonitorInterval = SmppConstants.DEFAULT_WINDOW_MONITOR_INTERVAL;
        long windowWaitTimeout = SmppConstants.DEFAULT_WINDOW_WAIT_TIMEOUT;

        Esme esme = new Esme("Esme_1", "Esme_systemId_1", "pwd", "host", 0, false, null,
                SmppInterfaceVersionType.SMPP34, -1, -1, null, SmppBindType.TRANSCEIVER, SmppSession.Type.CLIENT,
                windowSize, connectTimeout, requestExpiryTimeout, windowMonitorInterval, windowWaitTimeout, "Esme_1",
                true, 30000, -1, -1, "^[0-9a-zA-Z]*", -1, -1, "^[0-9a-zA-Z]*", 0, 0, 0, 0, 0, -1, -1, -1, -1, true);
        ActivityContextInterface aci = new SmppTransactionProxy(esme);

        SubmitSm event = new SubmitSm();
        Date curDate = new Date();
        this.fillSm(event, curDate, true);

        event.setShortMessage(msgGsm7);
        event.setDataCoding((byte) 0);
        event.setEsmClass((byte) 3);

        // byte[] bbb = new byte[25500];
        // bbb[0] = 6;
        // bbb[1] = 36;
        // bbb[2] = 1;
        // bbb[3] = 13;
        // bbb[4] = 37;
        // bbb[5] = 1;
        // bbb[6] = 13;
        // for (int i = 7; i < 25500; i++) {
        // bbb[i] = 0x41;
        // }
        // event.setShortMessage(null);
        // Tlv tlv = new Tlv(SmppConstants.TAG_MESSAGE_PAYLOAD, bbb);
        // event.addOptionalParameter(tlv);
        // TxSmppServerSbb.smscPropertiesManagement.setNationalLanguageLockingShift(13);
        // TxSmppServerSbb.smscPropertiesManagement.setNationalLanguageSingleShift(13);
        // event.setEsmClass((byte) 67);

        long dueSlot = this.pers.c2_getDueSlotForTime(scheduleDeliveryTime);
        PreparedStatementCollection_C3 psc = this.pers.getStatementCollection(scheduleDeliveryTime);
        int b1 = this.pers.checkSmsExists(dueSlot, ta1.getTargetId());
        long b2 = this.pers.c2_getDueSlotForTargetId(psc, ta1.getTargetId());
        assertEquals(b1, 0);
        assertEquals(b2, 0L);

        TxSmppServerSbb.smscPropertiesManagement.setSmppEncodingForGsm7(SmppEncoding.Gsm7);
        this.sbb.onSubmitSm(event, aci);

        b1 = this.pers.checkSmsExists(dueSlot, ta1.getTargetId());
        assertEquals(b1, 1);

        SmsSet smsSet = this.pers.c2_getRecordListForTargeId(dueSlot, ta1.getTargetId());
//        this.checkSmsSet(smsSet, curDate, true);
        Sms sms = smsSet.getSms(0);
        assertEquals(sms.getShortMessageText(), sMsg_4); // msgGsm7
        assertNull(sms.getShortMessageBin());

        assertEquals(this.smppSess.getReqList().size(), 0);
        assertEquals(this.smppSess.getRespList().size(), 1);

        PduResponse resp = this.smppSess.getRespList().get(0);
        assertEquals(resp.getCommandStatus(), 0);
        assertEquals(resp.getOptionalParameterCount(), 0);
    }

	@Test(groups = { "TxSmppServer" })
	public void testDataSm() throws Exception {

		if (!this.cassandraDbInited)
			return;

		this.smppSess = new SmppSessionsProxy();
		this.sbb.setSmppServerSessions(smppSess);

		int windowSize = SmppConstants.DEFAULT_WINDOW_SIZE;
		long connectTimeout = SmppConstants.DEFAULT_CONNECT_TIMEOUT;
		long requestExpiryTimeout = SmppConstants.DEFAULT_REQUEST_EXPIRY_TIMEOUT;
		long windowMonitorInterval = SmppConstants.DEFAULT_WINDOW_MONITOR_INTERVAL;
		long windowWaitTimeout = SmppConstants.DEFAULT_WINDOW_WAIT_TIMEOUT;

		Esme esme = new Esme("Esme_1", "Esme_systemId_1", "pwd", "host", 0, false, null,
				SmppInterfaceVersionType.SMPP34, -1, -1, null, SmppBindType.TRANSCEIVER, SmppSession.Type.CLIENT,
				windowSize, connectTimeout, requestExpiryTimeout, windowMonitorInterval, windowWaitTimeout, "Esme_1",
				true, 30000, -1, -1, "^[0-9a-zA-Z]*", -1, -1, "^[0-9a-zA-Z]*", 0, 0, 0, 0, 0, -1, -1, -1, -1, true);
		ActivityContextInterface aci = new SmppTransactionProxy(esme);

		DataSm event = new DataSm();
		Date curDate = new Date();
		this.fillSm(event, curDate, false);

		Tlv tlv = new Tlv(SmppConstants.TAG_MESSAGE_PAYLOAD, msgUtf8);
		event.addOptionalParameter(tlv);
		tlv = new Tlv(SmppConstants.TAG_SAR_MSG_REF_NUM, msg_ref_num);
		event.addOptionalParameter(tlv);
		tlv = new Tlv(SmppConstants.TAG_SAR_SEGMENT_SEQNUM, new byte[] { 1 });
		event.addOptionalParameter(tlv);
		tlv = new Tlv(SmppConstants.TAG_SAR_TOTAL_SEGMENTS, new byte[] { 2 });
		event.addOptionalParameter(tlv);

		long dueSlot = this.pers.c2_getDueSlotForTime(new Date());
		PreparedStatementCollection_C3 psc = this.pers.getStatementCollection(new Date());
		int b1 = this.pers.checkSmsExists(dueSlot, ta1.getTargetId());
		long b2 = this.pers.c2_getDueSlotForTargetId(psc, ta1.getTargetId());
		assertEquals(b1, 0);
		assertEquals(b2, 0L);

		TxSmppServerSbb.smscPropertiesManagement.setSmppEncodingForUCS2(SmppEncoding.Utf8);
		this.sbb.onDataSm(event, aci);

		b2 = this.pers.c2_getDueSlotForTargetId(psc, ta1.getTargetId());
		dueSlot = b2;
		b1 = this.pers.checkSmsExists(dueSlot, ta1.getTargetId());
		assertEquals(b1, 1);
		assertEquals(b2, dueSlot);

		SmsSet smsSet = this.pers.c2_getRecordListForTargeId(dueSlot, ta1.getTargetId());
		this.checkSmsSet(smsSet, curDate, false);
		Sms sms = smsSet.getSms(0);
		assertEquals(sms.getShortMessageText(), sMsg); // msgUcs2

		assertEquals(this.smppSess.getReqList().size(), 0);
		assertEquals(this.smppSess.getRespList().size(), 1);

		PduResponse resp = this.smppSess.getRespList().get(0);
		assertEquals(resp.getCommandStatus(), 0);
		assertEquals(resp.getOptionalParameterCount(), 0);
	}

    @Test(groups = { "TxSmppServer" })
    public void testSubmitMulti() throws Exception {

        if (!this.cassandraDbInited)
            return;

        this.smppSess = new SmppSessionsProxy();
        this.sbb.setSmppServerSessions(smppSess);

        int windowSize = SmppConstants.DEFAULT_WINDOW_SIZE;
        long connectTimeout = SmppConstants.DEFAULT_CONNECT_TIMEOUT;
        long requestExpiryTimeout = SmppConstants.DEFAULT_REQUEST_EXPIRY_TIMEOUT;
        long windowMonitorInterval = SmppConstants.DEFAULT_WINDOW_MONITOR_INTERVAL;
        long windowWaitTimeout = SmppConstants.DEFAULT_WINDOW_WAIT_TIMEOUT;

        Esme esme = new Esme("Esme_1", "Esme_systemId_1", "pwd", "host", 0, false, null,
                SmppInterfaceVersionType.SMPP34, -1, -1, null, SmppBindType.TRANSCEIVER, SmppSession.Type.CLIENT,
                windowSize, connectTimeout, requestExpiryTimeout, windowMonitorInterval, windowWaitTimeout, "Esme_1",
                true, 30000, -1, -1, "^[0-9a-zA-Z]*", -1, -1, "^[0-9a-zA-Z]*", 0, 0, 0, 0, 0, -1, -1, -1, -1, true);
        ActivityContextInterface aci = new SmppTransactionProxy(esme);

        SubmitMulti event = new SubmitMulti();
        Date curDate = new Date();
        this.fillSm(event, curDate, true);
        event.setShortMessage(msgUcs2);

        Address destAddr = new Address();
        destAddr.setAddress("5555");
        destAddr.setTon(SmppConstants.TON_INTERNATIONAL);
        destAddr.setNpi(SmppConstants.NPI_E164);
        event.addDestAddresses(destAddr);
        Address destAddr2 = new Address();
        destAddr2.setAddress("5556");
        destAddr2.setTon(SmppConstants.TON_INTERNATIONAL);
        destAddr2.setNpi(SmppConstants.NPI_E164);
        event.addDestAddresses(destAddr2);

        long dueSlot = this.pers.c2_getDueSlotForTime(scheduleDeliveryTime);
        PreparedStatementCollection_C3 psc = this.pers.getStatementCollection(scheduleDeliveryTime);
        int b1 = this.pers.checkSmsExists(dueSlot, ta1.getTargetId());
        long b2 = this.pers.c2_getDueSlotForTargetId(psc, ta1.getTargetId());
        assertEquals(b1, 0);
        assertEquals(b2, 0L);
        b1 = this.pers.checkSmsExists(dueSlot, ta2.getTargetId());
        b2 = this.pers.c2_getDueSlotForTargetId(psc, ta2.getTargetId());
        assertEquals(b1, 0);
        assertEquals(b2, 0L);

        TxSmppServerSbb.smscPropertiesManagement.setSmppEncodingForUCS2(SmppEncoding.Unicode);
        this.sbb.onSubmitMulti(event, aci);

        b1 = this.pers.checkSmsExists(dueSlot, ta1.getTargetId());
        assertEquals(b1, 1);
        b1 = this.pers.checkSmsExists(dueSlot, ta2.getTargetId());
        assertEquals(b1, 1);

        SmsSet smsSet = this.pers.c2_getRecordListForTargeId(dueSlot, ta1.getTargetId());
        this.checkSmsSet(smsSet, curDate, true);
        Sms sms = smsSet.getSms(0);
        assertEquals(sms.getShortMessageText(), sMsg); // msgUcs2
        assertEquals(sms.getShortMessageBin(), udhCode);

        assertEquals(this.smppSess.getReqList().size(), 0);
        assertEquals(this.smppSess.getRespList().size(), 1);

        SubmitMultiResp resp = (SubmitMultiResp)this.smppSess.getRespList().get(0);
        assertEquals(resp.getCommandStatus(), 0);
        assertEquals(resp.getOptionalParameterCount(), 0);
        assertEquals(resp.getUnsucessfulSmes().size(), 0);
    }

    @Test(groups = { "TxSmppServer" })
    public void testSubmitMulti_BadAddr() throws Exception {

        if (!this.cassandraDbInited)
            return;

        this.smppSess = new SmppSessionsProxy();
        this.sbb.setSmppServerSessions(smppSess);

        int windowSize = SmppConstants.DEFAULT_WINDOW_SIZE;
        long connectTimeout = SmppConstants.DEFAULT_CONNECT_TIMEOUT;
        long requestExpiryTimeout = SmppConstants.DEFAULT_REQUEST_EXPIRY_TIMEOUT;
        long windowMonitorInterval = SmppConstants.DEFAULT_WINDOW_MONITOR_INTERVAL;
        long windowWaitTimeout = SmppConstants.DEFAULT_WINDOW_WAIT_TIMEOUT;

        Esme esme = new Esme("Esme_1", "Esme_systemId_1", "pwd", "host", 0, false, null,
                SmppInterfaceVersionType.SMPP34, -1, -1, null, SmppBindType.TRANSCEIVER, SmppSession.Type.CLIENT,
                windowSize, connectTimeout, requestExpiryTimeout, windowMonitorInterval, windowWaitTimeout, "Esme_1",
                true, 30000, -1, -1, "^[0-9a-zA-Z]*", -1, -1, "^[0-9a-zA-Z]*", 0, 0, 0, 0, 0, -1, -1, -1, -1, true);
        ActivityContextInterface aci = new SmppTransactionProxy(esme);

        SubmitMulti event = new SubmitMulti();
        Date curDate = new Date();
        this.fillSm(event, curDate, true);
        event.setShortMessage(msgUcs2);

        Address destAddr = new Address();
        destAddr.setAddress("5555");
        destAddr.setTon(SmppConstants.TON_SUBSCRIBER);
        destAddr.setNpi(SmppConstants.NPI_E164);
        event.addDestAddresses(destAddr);
        Address destAddr2 = new Address();
        destAddr2.setAddress("5556");
        destAddr2.setTon(SmppConstants.TON_INTERNATIONAL);
        destAddr2.setNpi(SmppConstants.NPI_E164);
        event.addDestAddresses(destAddr2);

        long dueSlot = this.pers.c2_getDueSlotForTime(scheduleDeliveryTime);
        PreparedStatementCollection_C3 psc = this.pers.getStatementCollection(scheduleDeliveryTime);
        int b1 = this.pers.checkSmsExists(dueSlot, ta1.getTargetId());
        long b2 = this.pers.c2_getDueSlotForTargetId(psc, ta1.getTargetId());
        assertEquals(b1, 0);
        assertEquals(b2, 0L);
        b1 = this.pers.checkSmsExists(dueSlot, ta2.getTargetId());
        b2 = this.pers.c2_getDueSlotForTargetId(psc, ta2.getTargetId());
        assertEquals(b1, 0);
        assertEquals(b2, 0L);

        TxSmppServerSbb.smscPropertiesManagement.setSmppEncodingForUCS2(SmppEncoding.Unicode);
        this.sbb.onSubmitMulti(event, aci);

        b1 = this.pers.checkSmsExists(dueSlot, ta1.getTargetId());
        assertEquals(b1, 0);
        b1 = this.pers.checkSmsExists(dueSlot, ta2.getTargetId());
        assertEquals(b1, 1);

        SubmitMultiResp resp = (SubmitMultiResp)this.smppSess.getRespList().get(0);
        assertEquals(resp.getCommandStatus(), 0);
        assertEquals(resp.getOptionalParameterCount(), 0);
        assertEquals(resp.getUnsucessfulSmes().size(), 1);
        assertEquals(resp.getUnsucessfulSmes().get(0).getAddress().getAddress(), "5555");
    }

	@Test(groups = { "TxSmppServer" })
	public void testSubmitSm_BadCodingSchema() throws Exception {

		if (!this.cassandraDbInited)
			return;

		this.smppSess = new SmppSessionsProxy();
		this.sbb.setSmppServerSessions(smppSess);

		int windowSize = SmppConstants.DEFAULT_WINDOW_SIZE;
		long connectTimeout = SmppConstants.DEFAULT_CONNECT_TIMEOUT;
		long requestExpiryTimeout = SmppConstants.DEFAULT_REQUEST_EXPIRY_TIMEOUT;
		long windowMonitorInterval = SmppConstants.DEFAULT_WINDOW_MONITOR_INTERVAL;
		long windowWaitTimeout = SmppConstants.DEFAULT_WINDOW_WAIT_TIMEOUT;

		Esme esme = new Esme("Esme_1", "Esme_systemId_1", "pwd", "host", 0, false, null,
				SmppInterfaceVersionType.SMPP34, -1, -1, null, SmppBindType.TRANSCEIVER, SmppSession.Type.CLIENT,
				windowSize, connectTimeout, requestExpiryTimeout, windowMonitorInterval, windowWaitTimeout, "Esme_1",
				true, 30000, -1, -1, "^[0-9a-zA-Z]*", -1, -1, "^[0-9a-zA-Z]*", 0, 0, 0, 0, 0, -1, -1, -1, -1, true);
		ActivityContextInterface aci = new SmppTransactionProxy(esme);

		SubmitSm event = new SubmitSm();
		Date curDate = new Date();
		this.fillSm(event, curDate, true);
		event.setShortMessage(msgUtf8);

		DataCodingSchemeImpl dcss = new DataCodingSchemeImpl(DataCodingGroup.GeneralGroup, null, null, null,
				CharacterSet.GSM7, true);
		// DataCodingGroup dataCodingGroup, DataCodingSchemaMessageClass
		// messageClass,
		// DataCodingSchemaIndicationType dataCodingSchemaIndicationType,
		// Boolean setIndicationActive,
		// CharacterSet characterSet, boolean isCompressed

		event.setDataCoding((byte) 12);

		long dueSlot = this.pers.c2_getDueSlotForTime(scheduleDeliveryTime);
		PreparedStatementCollection_C3 psc = this.pers.getStatementCollection(scheduleDeliveryTime);
		int b1 = this.pers.checkSmsExists(dueSlot, ta1.getTargetId());
		long b2 = this.pers.c2_getDueSlotForTargetId(psc, ta1.getTargetId());
		assertEquals(b1, 0);
		assertEquals(b2, 0L);

		TxSmppServerSbb.smscPropertiesManagement.setSmppEncodingForUCS2(SmppEncoding.Utf8);
		this.sbb.onSubmitSm(event, aci);

		b2 = this.pers.c2_getDueSlotForTargetId(psc, ta1.getTargetId());
		assertEquals(b2, 0);

		assertEquals(this.smppSess.getReqList().size(), 0);
		assertEquals(this.smppSess.getRespList().size(), 1);

		PduResponse resp = this.smppSess.getRespList().get(0);
		assertEquals(resp.getCommandStatus(), 260);
		assertEquals(resp.getOptionalParameterCount(), 1);
		Tlv tlvr = resp.getOptionalParameter(SmppConstants.TAG_ADD_STATUS_INFO);
		String errMsg = tlvr.getValueAsString();
		assertEquals(errMsg, "TxSmpp DataCoding scheme does not supported: 12 - Only GSM7, GSM8 and USC2 are supported");
	}

	@Test(groups = { "TxSmppServer" })
	public void testSubmitSm_createSmsEvent() throws Exception {

		if (!this.cassandraDbInited)
			return;

		this.smppSess = new SmppSessionsProxy();
		this.sbb.setSmppServerSessions(smppSess);

		TxSmppServerSbb.smscPropertiesManagement.setSmppEncodingForUCS2(SmppEncoding.Utf8);

		SmscPropertiesManagement spm = SmscPropertiesManagement.getInstance("Test");
		String sMsgA = "������Hel";

		// only message part
		Charset utf8 = Charset.forName("UTF-8");
		ByteBuffer bb = utf8.encode(sMsgA);
		byte[] aMsgA = new byte[bb.limit()];
		bb.get(aMsgA);
		Charset ucs2 = Charset.forName("UTF-16BE");
		bb = ucs2.encode(sMsgA);
		byte[] aMsgAA = new byte[bb.limit()];
		bb.get(aMsgAA);

		com.cloudhopper.smpp.pdu.SubmitSm event = new com.cloudhopper.smpp.pdu.SubmitSm();
		Address addr = new Address();
		addr.setNpi((byte) 1);
		addr.setTon((byte) 1);
		addr.setAddress("2222");
		event.setSourceAddress(addr);

		Address addr2 = new Address();
		addr2.setNpi((byte) 1);
		addr2.setTon((byte) 1);
		addr2.setAddress("5555");
		event.setDestAddress(addr2);

		event.setDataCoding((byte) 8);
		event.setShortMessage(aMsgA);

		Esme origEsme = new Esme();
		TargetAddress ta = ta1;
		Sms sms = this.sbb.createSmsEvent(event, origEsme, ta, this.pers);
        assertEquals(sms.getShortMessageText(), sMsgA);

		// message part and UDH
		byte[] udh = new byte[] { 0x05, 0x00, 0x03, 0x29, 0x02, 0x02 };
		byte[] aMsgB = new byte[aMsgA.length + udh.length];
		System.arraycopy(udh, 0, aMsgB, 0, udh.length);
		System.arraycopy(aMsgA, 0, aMsgB, udh.length, aMsgA.length);

		event = new com.cloudhopper.smpp.pdu.SubmitSm();
		event.setSourceAddress(addr);
		event.setDestAddress(addr2);
		event.setDataCoding((byte) 8);
		event.setShortMessage(aMsgB);
		event.setEsmClass(SmppConstants.ESM_CLASS_UDHI_MASK);

		sms = this.sbb.createSmsEvent(event, origEsme, ta, this.pers);
        assertEquals(sms.getShortMessageText(), sMsgA);
        assertEquals(sms.getShortMessageBin(), udh);

        // binary GSM8
        String s1 = "Optic xxx";
        Charset iso = Charset.forName("ISO-8859-1");
        byte[] aMsgC = s1.getBytes(iso);
        byte[] aMsgCC = new byte[aMsgC.length + udh.length];
        System.arraycopy(udh, 0, aMsgCC, 0, udh.length);
        System.arraycopy(aMsgC, 0, aMsgCC, udh.length, aMsgC.length);

        event = new com.cloudhopper.smpp.pdu.SubmitSm();
        event.setSourceAddress(addr);
        event.setDestAddress(addr2);
        event.setDataCoding((byte) 4);
        event.setShortMessage(aMsgCC);
        event.setEsmClass(SmppConstants.ESM_CLASS_UDHI_MASK);

        sms = this.sbb.createSmsEvent(event, origEsme, ta, this.pers);
        assertEquals(sms.getShortMessageText(), s1);
        assertEquals(sms.getShortMessageBin(), udh);

        // GSM7
        event = new com.cloudhopper.smpp.pdu.SubmitSm();
        event.setSourceAddress(addr);
        event.setDestAddress(addr2);
        event.setDataCoding((byte) 0);
        event.setShortMessage(aMsgC);

        sms = this.sbb.createSmsEvent(event, origEsme, ta, this.pers);
        assertEquals(sms.getShortMessageText(), s1);
        assertNull(sms.getShortMessageBin());

	}

    @Test(groups = { "TxSmppServer" })
    public void testSubmitSm_MProc() throws Exception {

        if (!this.cassandraDbInited)
            return;

        MProcManagement mProcManagement = MProcManagement.getInstance();
        SmscManagement smscManagement = SmscManagement.getInstance("Test");
        SmppManagement smppManagement = SmppManagement.getInstance("Test");
        smscManagement.setSmppManagement(smppManagement);
        mProcManagement.setSmscManagement(smscManagement);
        smscManagement.registerRuleFactory(new MProcRuleFactoryDefault());
        smscManagement.start();

        try {
            mProcManagement.destroyMProcRule(1);
        } catch (Exception e) {
        }
        try {
            mProcManagement.destroyMProcRule(2);
        } catch (Exception e) {
        }

        mProcManagement.createMProcRule(1, MProcRuleFactoryDefault.RULE_CLASS_NAME,
                "desttonmask 1 destnpimask 1 originatingmask SMPP networkidmask 0 newnetworkid 5 adddestdigprefix 47 makecopy true");
        mProcManagement.createMProcRule(2, MProcRuleFactoryDefault.RULE_CLASS_NAME,
                "networkidmask 5 newdestnpi 2 makecopy true");

        // TODO: ***** make proper mproc rules testing
        // MProcManagement.getInstance().createMProcRule(1, 1, 1, "-1", "SMPP", 0, 5, -1, -1, "47", true);
        // MProcManagement.getInstance().createMProcRule(2, -1, -1, "-1", null, 5, -1, -1, 2, "-1", true);
        // destTonMask, destNpiMask, destDigMask, originatingMask, networkIdMask, newNetworkId, newDestTon, newDestNpi,
        // addDestDigPrefix, makeCopy
        // TODO: ***** make proper mproc rules testing

        this.smppSess = new SmppSessionsProxy();
        this.sbb.setSmppServerSessions(smppSess);

        int windowSize = SmppConstants.DEFAULT_WINDOW_SIZE;
        long connectTimeout = SmppConstants.DEFAULT_CONNECT_TIMEOUT;
        long requestExpiryTimeout = SmppConstants.DEFAULT_REQUEST_EXPIRY_TIMEOUT;
        long windowMonitorInterval = SmppConstants.DEFAULT_WINDOW_MONITOR_INTERVAL;
        long windowWaitTimeout = SmppConstants.DEFAULT_WINDOW_WAIT_TIMEOUT;

        Esme esme = new Esme("Esme_1", "Esme_systemId_1", "pwd", "host", 0, false, null,
                SmppInterfaceVersionType.SMPP34, -1, -1, null, SmppBindType.TRANSCEIVER, SmppSession.Type.CLIENT,
                windowSize, connectTimeout, requestExpiryTimeout, windowMonitorInterval, windowWaitTimeout, "Esme_1",
                true, 30000, -1, -1, "^[0-9a-zA-Z]*", -1, -1, "^[0-9a-zA-Z]*", 0, 0, 0, 0, 0, -1, -1, -1, -1, true);
        ActivityContextInterface aci = new SmppTransactionProxy(esme);

        SubmitSm event = new SubmitSm();
        Date curDate = new Date();
        this.fillSm(event, curDate, true);
        event.setShortMessage(msgUcs2);

        long dueSlot = this.pers.c2_getDueSlotForTime(scheduleDeliveryTime);
        PreparedStatementCollection_C3 psc = this.pers.getStatementCollection(scheduleDeliveryTime);
        int b1 = this.pers.checkSmsExists(dueSlot, ta1.getTargetId());
        long b2 = this.pers.c2_getDueSlotForTargetId(psc, ta1.getTargetId());
        assertEquals(b1, 0);
        assertEquals(b2, 0L);

        TxSmppServerSbb.smscPropertiesManagement.setSmppEncodingForUCS2(SmppEncoding.Unicode);
        this.sbb.onSubmitSm(event, aci);

        TargetAddress tax1 = new TargetAddress(1, 1, "475555", 5);
        TargetAddress tax2 = new TargetAddress(1, 2, "475555", 5);
//        int addrTon, int addrNpi, String addr, int networkId

        b1 = this.pers.checkSmsExists(dueSlot, ta1.getTargetId());
        assertEquals(b1, 1);
        b1 = this.pers.checkSmsExists(dueSlot, tax1.getTargetId());
        assertEquals(b1, 1);
        b1 = this.pers.checkSmsExists(dueSlot, tax2.getTargetId());
        assertEquals(b1, 1);

        SmsSet smsSet = this.pers.c2_getRecordListForTargeId(dueSlot, ta1.getTargetId());
        SmsSet smsSet2 = this.pers.c2_getRecordListForTargeId(dueSlot, tax1.getTargetId());
        SmsSet smsSet3 = this.pers.c2_getRecordListForTargeId(dueSlot, tax2.getTargetId());

        assertEquals(smsSet.getDestAddr(), "5555");
        assertEquals(smsSet.getDestAddrTon(), SmppConstants.TON_INTERNATIONAL);
        assertEquals(smsSet.getDestAddrNpi(), SmppConstants.NPI_E164);
        assertEquals(smsSet.getNetworkId(), 0);
        assertEquals(smsSet2.getDestAddr(), "475555");
        assertEquals(smsSet2.getDestAddrTon(), SmppConstants.TON_INTERNATIONAL);
        assertEquals(smsSet2.getDestAddrNpi(), SmppConstants.NPI_E164);
        assertEquals(smsSet2.getNetworkId(), 5);
        assertEquals(smsSet3.getDestAddr(), "475555");
        assertEquals(smsSet3.getDestAddrTon(), SmppConstants.TON_INTERNATIONAL);
        assertEquals(smsSet3.getDestAddrNpi(), SmppConstants.NPI_ISDN);
        assertEquals(smsSet3.getNetworkId(), 5);

        // .................

//        this.checkSmsSet(smsSet, curDate, true);
//        Sms sms = smsSet.getSms(0);
//        assertEquals(sms.getShortMessageText(), sMsg); // msgUcs2
//        assertEquals(sms.getShortMessageBin(), udhCode);
//
//        assertEquals(this.smppSess.getReqList().size(), 0);
//        assertEquals(this.smppSess.getRespList().size(), 1);

        PduResponse resp = this.smppSess.getRespList().get(0);
        assertEquals(resp.getCommandStatus(), 0);
        assertEquals(resp.getOptionalParameterCount(), 0);

        try {
            mProcManagement.destroyMProcRule(1);
            mProcManagement.destroyMProcRule(2);
        } catch (Exception e) {
        }
    }

	private long getStoredRecord() throws PersistenceException {
		long dueSlot;
		if (scheduleDeliveryTime != null) {
			dueSlot = this.pers.c2_getDueSlotForTime(scheduleDeliveryTime);
		} else {
			dueSlot = this.pers.c2_getDueSlotForTime(new Date());
		}
		PreparedStatementCollection_C3 psc = this.pers.getStatementCollection(new Date());
		dueSlot = this.pers.c2_getDueSlotForTargetId(psc, ta1.getTargetId());
		return dueSlot;
	}

	private void fillSm(BaseSm event, Date curDate, boolean isSubmitMsg) {
		Address destAddr = new Address();
		destAddr.setAddress("5555");
		destAddr.setTon(SmppConstants.TON_INTERNATIONAL);
		destAddr.setNpi(SmppConstants.NPI_E164);
		event.setDestAddress(destAddr);
		Address srcAddr = new Address();
		srcAddr.setAddress("4444");
		srcAddr.setTon(SmppConstants.TON_INTERNATIONAL);
		srcAddr.setNpi(SmppConstants.NPI_E164);
		event.setSourceAddress(srcAddr);

		event.setDataCoding((byte) 8);
		event.setServiceType("CMT");
		if (isSubmitMsg)
			event.setEsmClass((byte) 67);
		else
			event.setEsmClass((byte) 3);
		event.setRegisteredDelivery((byte) 1);

		if (isSubmitMsg) {
			event.setProtocolId((byte) 5);
			event.setPriority((byte) 2);
			event.setReplaceIfPresent((byte) 0);
			event.setDefaultMsgId((byte) 200);

			scheduleDeliveryTime = MessageUtil.addHours(curDate, 24);
			event.setScheduleDeliveryTime(MessageUtil.printSmppAbsoluteDate(scheduleDeliveryTime,
					-(new Date()).getTimezoneOffset()));
			event.setValidityPeriod(MessageUtil.printSmppRelativeDate(0, 0, 2, 0, 0, 0));
		}
	}

	private void checkSmsSet(SmsSet smsSet, Date curDate, boolean isSubmitMsg) throws PersistenceException,
			TlvConvertException {
		// this.pers.fetchSchedulableSms(smsSet, false);

		assertEquals(smsSet.getDestAddr(), "5555");
		assertEquals(smsSet.getDestAddrTon(), SmppConstants.TON_INTERNATIONAL);
		assertEquals(smsSet.getDestAddrNpi(), SmppConstants.NPI_E164);

		assertEquals(smsSet.getInSystem(), 0);
		assertEquals(smsSet.getDueDelay(), 0);
		assertNull(smsSet.getStatus());
		assertFalse(smsSet.isAlertingSupported());

		Sms sms = smsSet.getSms(0);
		assertNotNull(sms);
		assertEquals(sms.getSourceAddr(), "4444");
		assertEquals(sms.getSourceAddrTon(), SmppConstants.TON_INTERNATIONAL);
		assertEquals(sms.getSourceAddrNpi(), SmppConstants.NPI_E164);
		assertEquals(sms.getMessageId(), DBOperations_C2.MESSAGE_ID_LAG + 1);

		assertEquals(sms.getDataCoding(), 8);
		assertEquals(sms.getOrigEsmeName(), "Esme_1");
		assertEquals(sms.getOrigSystemId(), "Esme_systemId_1");

		assertEquals(sms.getServiceType(), "CMT");
		if (isSubmitMsg)
			assertEquals(sms.getEsmClass(), 67);
		else
			assertEquals(sms.getEsmClass(), 3);
		assertEquals(sms.getRegisteredDelivery(), 1);

		if (isSubmitMsg) {
			assertEquals(sms.getProtocolId(), 5);
			assertEquals(sms.getPriority(), 2);
			assertEquals(sms.getReplaceIfPresent(), 0);
			assertEquals(sms.getDefaultMsgId() & 0xFF, 200);

			assertEquals(sms.getTlvSet().getOptionalParameterCount(), 0);

			assertDateEq(sms.getScheduleDeliveryTime(), MessageUtil.addHours(curDate, 24));
			assertDateEq(sms.getValidityPeriod(), MessageUtil.addHours(curDate, 24 * 2));
		} else {
			assertEquals(sms.getProtocolId(), 0);
			assertEquals(sms.getPriority(), 0);
			assertEquals(sms.getReplaceIfPresent(), 0);
			assertEquals(sms.getDefaultMsgId(), 0);

			assertEquals(sms.getTlvSet().getOptionalParameterCount(), 3);
			int val = sms.getTlvSet().getOptionalParameter(SmppConstants.TAG_SAR_MSG_REF_NUM).getValueAsShort();
			assertEquals(val, 10);
			assertEquals(sms.getTlvSet().getOptionalParameter(SmppConstants.TAG_SAR_SEGMENT_SEQNUM).getValueAsByte(), 1);
			assertEquals(sms.getTlvSet().getOptionalParameter(SmppConstants.TAG_SAR_TOTAL_SEGMENTS).getValueAsByte(), 2);

			assertNull(sms.getScheduleDeliveryTime());
			assertDateEq(sms.getValidityPeriod(), MessageUtil.addHours(curDate, 24 * 3));
		}

		assertEquals(sms.getDeliveryCount(), 0);

		// if (isSubmitMsg)
		// assertDateEq(smsSet.getDueDate(), new Date(curDate.getTime() + 1 * 60
		// * 1000));
		// else
		// assertDateEq(smsSet.getDueDate(), sms.getScheduleDeliveryTime());
		assertDateEq(sms.getSubmitDate(), curDate);
	}

	private void assertDateEq(Date d1, Date d2) {
		// creating d3 = d1 + 2 min

		long tm = d2.getTime();
		tm -= 15 * 1000;
		Date d3 = new Date(tm);

		tm = d2.getTime();
		tm += 15 * 1000;
		Date d4 = new Date(tm);

		assertTrue(d1.after(d3));
		assertTrue(d1.before(d4));
	}

	private class TxSmppServerSbbProxy extends TxSmppServerSbb {

		private TT_PersistenceRAInterfaceProxy cassandraSbb;

		public TxSmppServerSbbProxy(TT_PersistenceRAInterfaceProxy cassandraSbb) {
			this.cassandraSbb = cassandraSbb;
			this.logger = new TraceProxy();
			this.scheduler = new SchedulerResourceAdaptorProxy();
			TxSmppServerSbb.smscPropertiesManagement = SmscPropertiesManagement.getInstance("Test");
		}

		@Override
		public TT_PersistenceRAInterfaceProxy getStore() {
			return cassandraSbb;
		}

		public void setSmppServerSessions(SmppSessions smppServerSessions) {
			this.smppServerSessions = smppServerSessions;
		}

		protected Sms createSmsEvent(BaseSm event, Esme origEsme, TargetAddress ta, PersistenceRAInterface store)
				throws SmscProcessingException {
			return super.createSmsEvent(event, origEsme, ta, store);
		}

		@Override
		public ChildRelationExt getChargingSbb() {
			// TODO Auto-generated method stub
			return null;
		}
	}

	private class SmppTransactionProxy implements SmppTransaction, ActivityContextInterface {

		private Esme esme;

		public SmppTransactionProxy(Esme esme) {
			this.esme = esme;
		}

		@Override
		public Esme getEsme() {
			return this.esme;
		}

		@Override
		public void attach(SbbLocalObject arg0) throws NullPointerException, TransactionRequiredLocalException,
				TransactionRolledbackLocalException, SLEEException {
			// TODO Auto-generated method stub

		}

		@Override
		public void detach(SbbLocalObject arg0) throws NullPointerException, TransactionRequiredLocalException,
				TransactionRolledbackLocalException, SLEEException {
			// TODO Auto-generated method stub

		}

		@Override
		public Object getActivity() throws TransactionRequiredLocalException, SLEEException {
			// TODO Auto-generated method stub
			return this;
		}

		@Override
		public boolean isAttached(SbbLocalObject arg0) throws NullPointerException, TransactionRequiredLocalException,
				TransactionRolledbackLocalException, SLEEException {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public boolean isEnding() throws TransactionRequiredLocalException, SLEEException {
			// TODO Auto-generated method stub
			return false;
		}

	}

    private class SchedulerResourceAdaptorProxy implements SchedulerRaSbbInterface {

        @Override
        public void injectSmsOnFly(SmsSet smsSet, boolean callFromSbb) throws Exception {
            // TODO Auto-generated method stub

        }

        @Override
        public void injectSmsDatabase(SmsSet smsSet) throws Exception {
            // TODO Auto-generated method stub

        }

        @Override
        public void setDestCluster(SmsSet smsSet) {
            // TODO Auto-generated method stub

        }

    }

}
