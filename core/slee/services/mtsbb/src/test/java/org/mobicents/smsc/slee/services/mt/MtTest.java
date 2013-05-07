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

package org.mobicents.smsc.slee.services.mt;

import java.io.IOException;
import java.util.Date;
import java.util.UUID;

import org.mobicents.smsc.slee.services.persistence.CassandraPersistenceSbbProxy;
import org.mobicents.smsc.slee.services.persistence.PersistenceException;
import org.mobicents.smsc.slee.services.persistence.Sms;
import org.mobicents.smsc.slee.services.persistence.SmsSet;
import org.mobicents.smsc.slee.services.persistence.TargetAddress;
import org.mobicents.smsc.slee.services.smpp.server.events.SmsSetEvent;
import org.mobicents.smsc.smpp.SmscPropertiesManagement;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * 
 * @author sergey vetyutnev
 * 
 */
public class MtTest {

	private MtSbbProxy mtSbb;
	private SriSbbProxy sriSbb;
	private CassandraPersistenceSbbProxy pers;
	private boolean cassandraDbInited;
	private Date curDate;

	private TargetAddress ta1 = new TargetAddress(1, 1, "5555");

	private String msg = "01230123";

	@BeforeClass
	public void setUpClass() throws Exception {
		System.out.println("setUpClass");

		SmscPropertiesManagement smscPropertiesManagement = SmscPropertiesManagement.getInstance("Test");
		smscPropertiesManagement.setServiceCenterGt("1111");
		smscPropertiesManagement.setServiceCenterSsn(8);
		smscPropertiesManagement.setHlrSsn(6);
		smscPropertiesManagement.setMscSsn(8);

		this.pers = new CassandraPersistenceSbbProxy();
		this.cassandraDbInited = this.pers.testCassandraAccess();
		if (!this.cassandraDbInited)
			return;

		this.mtSbb = new MtSbbProxy(this.pers);
		this.sriSbb = new SriSbbProxy(this.pers, this.mtSbb);

		SmscPropertiesManagement.getInstance("Test");
	}

	@AfterClass
	public void tearDownClass() throws Exception {
		System.out.println("tearDownClass");
	}

	@Test(groups = { "Mt" })
	public void SuccessDeliveryTest() throws Exception {

		if (!this.cassandraDbInited)
			return;

		this.clearDatabase();
		SmsSet smsSet = prepareDatabase();

		this.pers.setDeliveryStart(smsSet, curDate);

		SmsSetEvent event = new SmsSetEvent();
		event.setSmsSet(smsSet);
		this.sriSbb.onSms(event, null, null);
	}

	private void clearDatabase() throws PersistenceException, IOException {

		SmsSet smsSet_x1 = this.pers.obtainSmsSet(ta1);
		this.pers.fetchSchedulableSms(smsSet_x1);

		this.pers.deleteSmsSet(smsSet_x1);
		int cnt = smsSet_x1.getSmsCount();
		for (int i1 = 0; i1 < cnt; i1++) {
			Sms sms = smsSet_x1.getSms(i1);
			this.pers.deleteLiveSms(sms.getDbId());
		}
		this.pers.deleteSmsSet(smsSet_x1);
	}

	private SmsSet prepareDatabase() throws PersistenceException {
		SmsSet smsSet = this.pers.obtainSmsSet(ta1);

		Sms sms = this.prepareSms(smsSet, 1);
		this.pers.createLiveSms(sms);
		sms = this.prepareSms(smsSet, 2);
		this.pers.createLiveSms(sms);

		SmsSet res = this.pers.obtainSmsSet(ta1);
		this.pers.fetchSchedulableSms(res);
		curDate = new Date();
		this.pers.setDeliveryStart(smsSet, curDate);
		return res;
	}

	private Sms prepareSms(SmsSet smsSet, int num) {

		Sms sms = new Sms();
		sms.setSmsSet(smsSet);

		sms.setDbId(UUID.randomUUID());
		// sms.setDbId(id);
		sms.setSourceAddr("4444");
		sms.setSourceAddrTon(1);
		sms.setSourceAddrNpi(1);
		sms.setMessageId(8888888 + num);
		sms.setMoMessageRef(102 + num);
		
		sms.setMessageId(num);

		sms.setOrigEsmeName("esme_1");
		sms.setOrigSystemId("sys_1");

		sms.setSubmitDate(new Date());
		// sms.setDeliveryDate(new GregorianCalendar(2013, 1, 15, 12, 15 +
		// num).getTime());

		// sms.setServiceType("serv_type__" + num);
		sms.setEsmClass(3);
		sms.setProtocolId(0);
		sms.setPriority(0);
		sms.setRegisteredDelivery(0);
		sms.setReplaceIfPresent(0);
		sms.setDataCoding(0);
		sms.setDefaultMsgId(0);

		sms.setShortMessage(this.msg.getBytes());

		// sms.setScheduleDeliveryTime(new GregorianCalendar(2013, 1, 20, 10, 00
		// + num).getTime());
		// sms.setValidityPeriod(new GregorianCalendar(2013, 1, 23, 13, 33 +
		// num).getTime());

		// short tag, byte[] value, String tagName
		// Tlv tlv = new Tlv((short) 5, new byte[] { (byte) (1 + num), 2, 3, 4,
		// 5 });
		// sms.getTlvSet().addOptionalParameter(tlv);
		// tlv = new Tlv((short) 6, new byte[] { (byte) (6 + num), 7, 8 });
		// sms.getTlvSet().addOptionalParameter(tlv);

		return sms;
	}

}
