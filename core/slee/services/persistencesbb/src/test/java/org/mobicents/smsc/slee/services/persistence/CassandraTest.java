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

package org.mobicents.smsc.slee.services.persistence;

import static org.testng.Assert.*;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.UUID;

import me.prettyprint.cassandra.serializers.ByteBufferSerializer;
import me.prettyprint.cassandra.serializers.CompositeSerializer;
import me.prettyprint.cassandra.serializers.StringSerializer;
import me.prettyprint.cassandra.serializers.UUIDSerializer;
import me.prettyprint.cassandra.service.CassandraHostConfigurator;
import me.prettyprint.hector.api.Cluster;
import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.beans.ColumnSlice;
import me.prettyprint.hector.api.beans.Composite;
import me.prettyprint.hector.api.beans.HColumn;
import me.prettyprint.hector.api.factory.HFactory;
import me.prettyprint.hector.api.mutation.Mutator;
import me.prettyprint.hector.api.query.ColumnQuery;
import me.prettyprint.hector.api.query.QueryResult;
import me.prettyprint.hector.api.query.SliceQuery;

import org.mobicents.protocols.ss7.map.api.primitives.AddressNature;
import org.mobicents.protocols.ss7.map.api.primitives.IMSI;
import org.mobicents.protocols.ss7.map.api.primitives.ISDNAddressString;
import org.mobicents.protocols.ss7.map.api.primitives.NumberingPlan;
import org.mobicents.protocols.ss7.map.api.service.sms.LocationInfoWithLMSI;
import org.mobicents.protocols.ss7.map.primitives.IMSIImpl;
import org.mobicents.protocols.ss7.map.primitives.ISDNAddressStringImpl;
import org.mobicents.protocols.ss7.map.service.sms.LocationInfoWithLMSIImpl;
import org.mobicents.smsc.slee.services.persistence.Sms;
import org.mobicents.smsc.slee.services.persistence.cassandra.CassandraPersistenceSbb;
import org.mobicents.smsc.slee.services.persistence.cassandra.Schema;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.cloudhopper.smpp.tlv.Tlv;

/**
 * 
 * @author sergey vetyutnev
 * 
 */
public class CassandraTest {

	private CassandraPersistenceSbbProxy sbb = new CassandraPersistenceSbbProxy();
	private boolean cassandraDbInited;

	private UUID id1 = UUID.fromString("59e815dc-49ad-4539-8cff-beb710a7de03");
	private UUID id2 = UUID.fromString("be26d2e9-1ba0-490c-bd5b-f04848127220");
	private UUID id3 = UUID.fromString("8bf7279f-3d4a-4494-8acd-cb9572c7ab33");
	private UUID id4 = UUID.fromString("c3bd98c2-355d-4572-8915-c6d0c767cae1");

	private TargetAddress ta1 = new TargetAddress(5, 1, "1111");
	private TargetAddress ta2 = new TargetAddress(5, 1, "1112");

	private SmsSet smsSetSched1;
	private SmsSet smsSetSched2;

	@BeforeClass
	public void setUpClass() throws Exception {
		System.out.println("setUpClass");

		Cluster cluster = HFactory.getOrCreateCluster("TestCluster", new CassandraHostConfigurator("localhost:9160"));
		Keyspace keyspace = HFactory.createKeyspace("TelestaxSMSC", cluster);

		try {
			ColumnQuery<String, Composite, ByteBuffer> query = HFactory.createColumnQuery(keyspace, StringSerializer.get(), CompositeSerializer.get(), ByteBufferSerializer.get());
			query.setColumnFamily(Schema.FAMILY_LIVE);
			Composite coKey3 = new Composite();
//			coKey3.addComponent(1, IntegerSerializer.get());
//			coKey3.addComponent(4, IntegerSerializer.get());
			coKey3.addComponent(Schema.COLUMN_ADDR_DST_TON, StringSerializer.get());
			query.setName(coKey3);
			query.setKey("111");

			QueryResult<HColumn<Composite,ByteBuffer>> result = query.execute();
		} catch (Exception e) {
			return;
		}

		this.cassandraDbInited = true;
		this.sbb.setKeyspace(keyspace);
	}

	@AfterClass
	public void tearDownClass() throws Exception {
		System.out.println("tearDownClass");
	}



	@Test(groups = { "cassandra" })
	public void testingLifeCycle() throws Exception {
		if (!this.cassandraDbInited)
			return;

		this.clearDatabase();

		this.addingNewMessages();

		this.scheduling();

		this.processSuccessDelivery();
		
		this.processFailuredDelivery();
	}

	public void addingNewMessages() throws Exception {
		boolean b1;
		boolean b2;
		Sms sms_x1;
		Sms sms_x2;
		Sms sms_x3;
		Sms sms_x4;
		SmsProxy sms_y1;
		SmsProxy sms_y2;
		SmsProxy sms_y3;
		SmsProxy sms_y4;

		// adding two messages for "1111"
		TargetAddress lock = this.sbb.obtainSynchroObject(ta1);
		try {
			synchronized (lock) {
				SmsSet smsSet_a = this.sbb.obtainSmsSet(ta1);
				Sms sms_a1 = this.createTestSms(1, smsSet_a, id1);
				Sms sms_a2 = this.createTestSms(2, smsSet_a, id2);
				this.sbb.createLiveSms(sms_a1);
				this.sbb.createLiveSms(sms_a2);

				// start of result testing
				b1 = this.sbb.checkSmsSetExists(ta1);
				b2 = this.sbb.checkSmsSetExists(ta2);
				sms_x1 = this.sbb.obtainLiveSms(id1);
				sms_x2 = this.sbb.obtainLiveSms(id2);
				sms_x3 = this.sbb.obtainLiveSms(id3);
				sms_x4 = this.sbb.obtainLiveSms(id4);
				sms_y1 = this.sbb.obtainArchiveSms(id1);
				sms_y2 = this.sbb.obtainArchiveSms(id2);
				sms_y3 = this.sbb.obtainArchiveSms(id3);
				sms_y4 = this.sbb.obtainArchiveSms(id4);

				assertTrue(b1);
				assertFalse(b2);
				this.checkTestSms(1, sms_x1, id1, false);
				this.checkTestSms(2, sms_x2, id2, false);
				assertNull(sms_x3);
				assertNull(sms_x4);
				assertNull(sms_y1);
				assertNull(sms_y2);
				assertNull(sms_y3);
				assertNull(sms_y4);
				// end of result testing
				
				// schedulering
				SmsSet smsSet = this.sbb.obtainSmsSet(ta1);
				assertEquals(smsSet.getInSystem(), 0);
				assertNull(smsSet.getDueDate());
				int year = new GregorianCalendar().get(GregorianCalendar.YEAR) - 1;
				Date d1 = new GregorianCalendar(year, 1, 20, 10, 00).getTime();
				Date d2 = new GregorianCalendar(year, 1, 20, 11, 00).getTime();
				Date d3 = new GregorianCalendar(year, 1, 20, 9, 00).getTime();

				// first date setting - any date is accepted
				this.sbb.setScheduled(smsSet_a, d1, true);
				smsSet = this.sbb.obtainSmsSet(ta1);
				assertEquals(smsSet.getInSystem(), 1);
				assertTrue(smsSet.getDueDate().equals(d1));

				// later date setting - not accepted
				this.sbb.setScheduled(smsSet_a, d2, true);
				smsSet = this.sbb.obtainSmsSet(ta1);
				assertEquals(smsSet.getInSystem(), 1);
				assertTrue(smsSet.getDueDate().equals(d1));

				// previous date setting - not accepted
				this.sbb.setScheduled(smsSet_a, d3, true);
				smsSet = this.sbb.obtainSmsSet(ta1);
				assertEquals(smsSet.getInSystem(), 1);
				assertTrue(smsSet.getDueDate().equals(d3));
			}
		} finally {
			this.sbb.obtainSynchroObject(lock);
		}

		// adding an extra message for "1111"
		lock = this.sbb.obtainSynchroObject(ta1);
		try {
			synchronized (lock) {
				SmsSet smsSet_a = this.sbb.obtainSmsSet(ta1);
				Sms sms_a3 = this.createTestSms(3, smsSet_a, id3);
				this.sbb.createLiveSms(sms_a3);

				// start of result testing
				b1 = this.sbb.checkSmsSetExists(ta1);
				b2 = this.sbb.checkSmsSetExists(ta2);
				sms_x1 = this.sbb.obtainLiveSms(id1);
				sms_x2 = this.sbb.obtainLiveSms(id2);
				sms_x3 = this.sbb.obtainLiveSms(id3);
				sms_x4 = this.sbb.obtainLiveSms(id4);
				sms_y1 = this.sbb.obtainArchiveSms(id1);
				sms_y2 = this.sbb.obtainArchiveSms(id2);
				sms_y3 = this.sbb.obtainArchiveSms(id3);
				sms_y4 = this.sbb.obtainArchiveSms(id4);

				assertTrue(b1);
				assertFalse(b2);
				this.checkTestSms(1, sms_x1, id1, false);
				this.checkTestSms(2, sms_x2, id2, false);
				this.checkTestSms(3, sms_x3, id3, false);
				assertNull(sms_x4);
				assertNull(sms_y1);
				assertNull(sms_y2);
				assertNull(sms_y3);
				assertNull(sms_y4);
				// end of result testing
			}
		} finally {
			this.sbb.obtainSynchroObject(lock);
		}

		// adding a messages for "1112"
		lock = this.sbb.obtainSynchroObject(ta2);
		try {
			synchronized (lock) {
				SmsSet smsSet_b = this.sbb.obtainSmsSet(ta2);
				Sms sms_a4 = this.createTestSms(4, smsSet_b, id4);
				this.sbb.createLiveSms(sms_a4);

				// start of result testing
				b1 = this.sbb.checkSmsSetExists(ta1);
				b2 = this.sbb.checkSmsSetExists(ta2);
				sms_x1 = this.sbb.obtainLiveSms(id1);
				sms_x2 = this.sbb.obtainLiveSms(id2);
				sms_x3 = this.sbb.obtainLiveSms(id3);
				sms_x4 = this.sbb.obtainLiveSms(id4);
				sms_y1 = this.sbb.obtainArchiveSms(id1);
				sms_y2 = this.sbb.obtainArchiveSms(id2);
				sms_y3 = this.sbb.obtainArchiveSms(id3);
				sms_y4 = this.sbb.obtainArchiveSms(id4);

				assertTrue(b1);
				assertTrue(b2);
				this.checkTestSms(1, sms_x1, id1, false);
				this.checkTestSms(2, sms_x2, id2, false);
				this.checkTestSms(3, sms_x3, id3, false);
				this.checkTestSms(4, sms_x4, id4, false);
				assertNull(sms_y1);
				assertNull(sms_y2);
				assertNull(sms_y3);
				assertNull(sms_y4);
				// end of result testing

				// schedulering
				int year = new GregorianCalendar().get(GregorianCalendar.YEAR) + 1;
				Date d1 = new GregorianCalendar(year, 11, 20, 10, 00).getTime();
				this.sbb.setScheduled(smsSet_b, d1, true);
			}
		} finally {
			this.sbb.obtainSynchroObject(lock);
		}

		// getting live_sms as messageId
		Sms sms_x12 = this.sbb.obtainLiveSms(8888888 + 2);
		this.checkTestSms(2, sms_x12, id2, false);
	}
	
	public void scheduling() throws Exception {

		// 1 - scheduling by dueTime
		int maxRecordCount = 100;
		List<SmsSet> lst = this.sbb.fetchSchedulableSmsSets(maxRecordCount);

		assertEquals(lst.size(), 1);
		SmsSet smsSet_a = lst.get(0);
		this.checkTestSmsSet(smsSet_a, 1, this.ta1);
		assertEquals(smsSet_a.getInSystem(), 1);
		assertEquals(smsSet_a.getDeliveryCount(), 0);

		for (SmsSet smsSet : lst) {
			TargetAddress lock = this.sbb.obtainSynchroObject(new TargetAddress(smsSet));
			try {
				synchronized (lock) {
					this.sbb.setDestination(smsSet, "ClusterName_1", "destSystemId_1", "destEsmeId_1", SmType.DELIVER_SM);
					this.sbb.fetchSchedulableSms(smsSet);

					// checking for sms count
					int smsCnt = 0;
					Sms sms = smsSet.getFirstSms();
					while (sms != null) {
						smsCnt++;
						assertEquals(sms.getMessageId(), 8888888 + smsCnt);
						sms = smsSet.getNextSms();
					}
					assertEquals(smsCnt, 3);

					if (smsSet.getFirstSms() != null) {
						this.smsSetSched1 = smsSet;
						this.sbb.setDeliveryStart(smsSet);

						SmsSet smsSet1 = this.sbb.obtainSmsSet(new TargetAddress(smsSet));
						assertEquals(smsSet_a.getInSystem(), 2);
						assertEquals(smsSet_a.getDeliveryCount(), 1);
						assertEquals(smsSet1.getInSystem(), 2);
						assertEquals(smsSet1.getDeliveryCount(), 1);
					}
				}
			} finally {
				this.sbb.obtainSynchroObject(lock);
			}
		}

		// 2 - scheduling by TargetAddress (after an Alert)
		this.smsSetSched2 = this.sbb.obtainSmsSet(ta2);
		assertEquals(this.smsSetSched2.getInSystem(), 1);
		assertEquals(this.smsSetSched2.getDeliveryCount(), 0);

		if (this.smsSetSched2.getInSystem() == 1) {
			TargetAddress lock = this.sbb.obtainSynchroObject(new TargetAddress(this.smsSetSched2));
			try {
				synchronized (lock) {
					this.sbb.setDestination(this.smsSetSched2, "ClusterName_2", "destSystemId_2", "destEsmeId_2", SmType.SUBMIT_SM);
					this.sbb.fetchSchedulableSms(this.smsSetSched2);

					// checking for sms count
					int smsCnt = 0;
					Sms sms = this.smsSetSched2.getFirstSms();
					while (sms != null) {
						smsCnt++;
						sms = this.smsSetSched2.getNextSms();
					}
					assertEquals(smsCnt, 1);

					if (this.smsSetSched2.getFirstSms() != null) {
						this.sbb.setDeliveryStart(this.smsSetSched2);
						SmsSet smsSet1 = this.sbb.obtainSmsSet(new TargetAddress(this.smsSetSched2));
						assertEquals(smsSet_a.getInSystem(), 2);
						assertEquals(smsSet_a.getDeliveryCount(), 1);
						assertEquals(smsSet1.getInSystem(), 2);
						assertEquals(smsSet1.getDeliveryCount(), 1);
					}
				}
			} finally {
				this.sbb.obtainSynchroObject(lock);
			}
		}
	}

	public void processSuccessDelivery() throws Exception {
		boolean b1;
		boolean b2;
		Sms sms_x1;
		Sms sms_x2;
		Sms sms_x3;
		Sms sms_x4;
		SmsProxy sms_y1;
		SmsProxy sms_y2;
		SmsProxy sms_y3;
		SmsProxy sms_y4;

		// start of result testing
		b1 = this.sbb.checkSmsSetExists(ta1);
		b2 = this.sbb.checkSmsSetExists(ta2);
		sms_x1 = this.sbb.obtainLiveSms(id1);
		sms_x2 = this.sbb.obtainLiveSms(id2);
		sms_x3 = this.sbb.obtainLiveSms(id3);
		sms_x4 = this.sbb.obtainLiveSms(id4);
		sms_y1 = this.sbb.obtainArchiveSms(id1);
		sms_y2 = this.sbb.obtainArchiveSms(id2);
		sms_y3 = this.sbb.obtainArchiveSms(id3);
		sms_y4 = this.sbb.obtainArchiveSms(id4);

		assertTrue(b1);
		assertTrue(b2);
		this.checkTestSms(1, sms_x1, id1, false);
		this.checkTestSms(2, sms_x2, id2, false);
		this.checkTestSms(3, sms_x3, id3, false);
		this.checkTestSms(4, sms_x4, id4, false);
		assertNull(sms_y1);
		assertNull(sms_y2);
		assertNull(sms_y3);
		assertNull(sms_y4);

		TargetAddress lock = this.sbb.obtainSynchroObject(new TargetAddress(this.smsSetSched1));
		try {
			synchronized (lock) {
				// getting routing info (SRI) ...
				IMSI imsi = new IMSIImpl("12345678901234");
				ISDNAddressString nnn = new ISDNAddressStringImpl(AddressNature.international_number, NumberingPlan.ISDN, "3355778");
				LocationInfoWithLMSI locationInfoWithLMSI = new LocationInfoWithLMSIImpl(nnn, null, null, null, null);
				this.sbb.setRoutingInfo(smsSetSched1, imsi, locationInfoWithLMSI);
				
				Sms sms = smsSetSched1.getFirstSms();
				int step = 0;
				while (sms != null) {
					// process message delivering ...

					step++;

					this.sbb.archiveDeliveredSms(sms, new GregorianCalendar(2013, 1, 15, 12, 15 + step).getTime());

					b1 = this.sbb.checkSmsSetExists(ta1);
					b2 = this.sbb.checkSmsSetExists(ta2);
					sms_x1 = this.sbb.obtainLiveSms(id1);
					sms_x2 = this.sbb.obtainLiveSms(id2);
					sms_x3 = this.sbb.obtainLiveSms(id3);
					sms_x4 = this.sbb.obtainLiveSms(id4);
					sms_y1 = this.sbb.obtainArchiveSms(id1);
					sms_y2 = this.sbb.obtainArchiveSms(id2);
					sms_y3 = this.sbb.obtainArchiveSms(id3);
					sms_y4 = this.sbb.obtainArchiveSms(id4);

					switch (step) {
					case 1:
						assertTrue(b1);
						assertTrue(b2);
						assertNull(sms_x1);
						this.checkTestSms(2, sms_x2, id2, false);
						this.checkTestSms(3, sms_x3, id3, false);
						this.checkTestSms(4, sms_x4, id4, false);
						this.checkTestSms(1, sms_y1, id1, true);
						assertNull(sms_y2);
						assertNull(sms_y3);
						assertNull(sms_y4);
						break;
					case 2:
						assertTrue(b1);
						assertTrue(b2);
						assertNull(sms_x1);
						assertNull(sms_x2);
						this.checkTestSms(3, sms_x3, id3, false);
						this.checkTestSms(4, sms_x4, id4, false);
						this.checkTestSms(1, sms_y1, id1, true);
						this.checkTestSms(2, sms_y2, id2, true);
						assertNull(sms_y3);
						assertNull(sms_y4);
						break;
					case 3:
						assertTrue(b1);
						assertTrue(b2);
						assertNull(sms_x1);
						assertNull(sms_x2);
						assertNull(sms_x3);
						this.checkTestSms(4, sms_x4, id4, false);
						this.checkTestSms(1, sms_y1, id1, true);
						this.checkTestSms(2, sms_y2, id2, true);
						this.checkTestSms(3, sms_y3, id3, true);
						assertNull(sms_y4);
						break;
					}

					sms = smsSetSched1.getNextSms();
				}

				this.sbb.fetchSchedulableSms(smsSetSched1);
				if (smsSetSched1.getFirstSms() != null) {
					// new message found - continue delivering ......
				} else {
					b1 = this.sbb.checkSmsSetExists(ta1);
					assertTrue(b1);
					SmsSet smsSet_b1 = this.sbb.obtainSmsSet(ta1);
					assertNull(smsSet_b1.getLastDelivery());
					assertEquals(smsSet_b1.getInSystem(), 2);
					assertNull(smsSet_b1.getStatus());
					assertEquals(smsSet_b1.getDeliveryCount(), 1);
					
					this.sbb.setDeliverySuccess(smsSetSched1, new GregorianCalendar(2013, 1, 15, 12, 15 + 3).getTime());

					b1 = this.sbb.checkSmsSetExists(ta1);
					assertTrue(b1);
					SmsSet smsSet_b2 = this.sbb.obtainSmsSet(ta1);
					assertTrue(smsSet_b2.getLastDelivery().equals(new GregorianCalendar(2013, 1, 15, 12, 15 + 3).getTime()));
					assertEquals(smsSet_b2.getInSystem(), 0);
					assertEquals(smsSet_b2.getStatus().getCode(), 0);
					assertEquals(smsSet_b2.getDeliveryCount(), 1);

					assertTrue(this.sbb.deleteSmsSet(smsSetSched1));

					b1 = this.sbb.checkSmsSetExists(ta1);
					assertFalse(b1);
				}
			}
		} finally {
			this.sbb.obtainSynchroObject(lock);
		}

		// testing - deleting absent smsSet
		b1 = this.sbb.checkSmsSetExists(ta1);
		assertFalse(b1);
		assertTrue(this.sbb.deleteSmsSet(smsSetSched1));
		b1 = this.sbb.checkSmsSetExists(ta1);
		assertFalse(b1);

		// testing - we can not delete smsSet with children
		b2 = this.sbb.checkSmsSetExists(ta2);
		assertTrue(b2);
		assertFalse(this.sbb.deleteSmsSet(smsSetSched2));
		b2 = this.sbb.checkSmsSetExists(ta2);
		assertTrue(b2);
	
	}

	public void processFailuredDelivery() throws Exception {
		boolean b1;
		boolean b2;
		Sms sms_x1;
		Sms sms_x2;
		Sms sms_x3;
		Sms sms_x4;
		SmsProxy sms_y1;
		SmsProxy sms_y2;
		SmsProxy sms_y3;
		SmsProxy sms_y4;

		// start of result testing
		b1 = this.sbb.checkSmsSetExists(ta1);
		b2 = this.sbb.checkSmsSetExists(ta2);
		sms_x1 = this.sbb.obtainLiveSms(id1);
		sms_x2 = this.sbb.obtainLiveSms(id2);
		sms_x3 = this.sbb.obtainLiveSms(id3);
		sms_x4 = this.sbb.obtainLiveSms(id4);
		sms_y1 = this.sbb.obtainArchiveSms(id1);
		sms_y2 = this.sbb.obtainArchiveSms(id2);
		sms_y3 = this.sbb.obtainArchiveSms(id3);
		sms_y4 = this.sbb.obtainArchiveSms(id4);

		assertFalse(b1);
		assertTrue(b2);
		assertNull(sms_x1);
		assertNull(sms_x2);
		assertNull(sms_x3);
		this.checkTestSms(4, sms_x4, id4, false);
		assertNotNull(sms_y1);
		assertNotNull(sms_y2);
		assertNotNull(sms_y3);
		assertNull(sms_y4);

		TargetAddress lock = this.sbb.obtainSynchroObject(new TargetAddress(this.smsSetSched2));
		try {
			synchronized (lock) {
				// getting routing info (SRI) ...
				IMSI imsi = new IMSIImpl("12345678901234");
				ISDNAddressString nnn = new ISDNAddressStringImpl(AddressNature.international_number, NumberingPlan.ISDN, "3355778");
				LocationInfoWithLMSI locationInfoWithLMSI = new LocationInfoWithLMSIImpl(nnn, null, null, null, null);
				this.sbb.setRoutingInfo(smsSetSched2, imsi, locationInfoWithLMSI);

				b1 = this.sbb.checkSmsSetExists(ta2);
				assertTrue(b1);
				SmsSet smsSet_b1 = this.sbb.obtainSmsSet(ta2);
				assertNull(smsSet_b1.getLastDelivery());
				assertEquals(smsSet_b1.getInSystem(), 2);
				assertNull(smsSet_b1.getStatus());
				assertEquals(smsSet_b1.getDeliveryCount(), 1);
				
				// we must firstly mark smsSet as failured
				this.sbb.setDeliveryFailure(smsSetSched2, ErrorCode.MEMORY_FULL, new GregorianCalendar(2013, 1, 15, 12, 15 + 4).getTime(), true);

				b1 = this.sbb.checkSmsSetExists(ta2);
				assertTrue(b1);
				SmsSet smsSet_b2 = this.sbb.obtainSmsSet(ta2);
				assertTrue(smsSet_b2.getLastDelivery().equals(new GregorianCalendar(2013, 1, 15, 12, 15 + 4).getTime()));
				assertEquals(smsSet_b2.getInSystem(), 0);
				assertEquals(smsSet_b2.getStatus().getCode(), 17);
				assertEquals(smsSet_b2.getDeliveryCount(), 1);
				
				Sms sms = smsSetSched2.getFirstSms();
				while (sms != null) {
					this.sbb.archiveFailuredSms(sms);
					sms = smsSetSched1.getNextSms();
				}

				b1 = this.sbb.checkSmsSetExists(ta1);
				b2 = this.sbb.checkSmsSetExists(ta2);
				sms_x1 = this.sbb.obtainLiveSms(id1);
				sms_x2 = this.sbb.obtainLiveSms(id2);
				sms_x3 = this.sbb.obtainLiveSms(id3);
				sms_x4 = this.sbb.obtainLiveSms(id4);
				sms_y1 = this.sbb.obtainArchiveSms(id1);
				sms_y2 = this.sbb.obtainArchiveSms(id2);
				sms_y3 = this.sbb.obtainArchiveSms(id3);
				sms_y4 = this.sbb.obtainArchiveSms(id4);

				assertFalse(b1);
				assertTrue(b2);
				assertNull(sms_x1);
				assertNull(sms_x2);
				assertNull(sms_x3);
				assertNull(sms_x4);
				this.checkTestSms(1, sms_y1, id1, true);
				this.checkTestSms(2, sms_y2, id2, true);
				this.checkTestSms(3, sms_y3, id3, true);
				this.checkTestSms(4, sms_y4, id4, true);

			}
		} finally {
			this.sbb.obtainSynchroObject(lock);
		}
	}

	private void clearDatabase() throws PersistenceException, IOException {
		this.sbb.deleteArchiveSms(id1);
		this.sbb.deleteArchiveSms(id2);
		this.sbb.deleteArchiveSms(id3);
		this.sbb.deleteArchiveSms(id4);
		this.sbb.deleteLiveSms(id1);
		this.sbb.deleteLiveSms(id2);
		this.sbb.deleteLiveSms(id3);
		this.sbb.deleteLiveSms(id4);

		SmsSet smsSet_x1 = new SmsSet();
		smsSet_x1.setDestAddr(ta1.getAddr());
		smsSet_x1.setDestAddrTon(ta1.getAddrTon());
		smsSet_x1.setDestAddrNpi(ta1.getAddrNpi());
		SmsSet smsSet_x2 = new SmsSet();
		smsSet_x2.setDestAddr(ta2.getAddr());
		smsSet_x2.setDestAddrTon(ta2.getAddrTon());
		smsSet_x2.setDestAddrNpi(ta2.getAddrNpi());
		this.sbb.deleteSmsSet(smsSet_x1);
		this.sbb.deleteSmsSet(smsSet_x2);

		boolean b1 = this.sbb.checkSmsSetExists(ta1);
		boolean b2 = this.sbb.checkSmsSetExists(ta2);
		Sms sms_x1 = this.sbb.obtainLiveSms(id1);
		Sms sms_x2 = this.sbb.obtainLiveSms(id2);
		Sms sms_x3 = this.sbb.obtainLiveSms(id3);
		Sms sms_x4 = this.sbb.obtainLiveSms(id4);
		SmsProxy sms_y1 = this.sbb.obtainArchiveSms(id1);
		SmsProxy sms_y2 = this.sbb.obtainArchiveSms(id2);
		SmsProxy sms_y3 = this.sbb.obtainArchiveSms(id3);
		SmsProxy sms_y4 = this.sbb.obtainArchiveSms(id4);

		assertFalse(b1);
		assertFalse(b2);
		assertNull(sms_x1);
		assertNull(sms_x2);
		assertNull(sms_x3);
		assertNull(sms_x4);
		assertNull(sms_y1);
		assertNull(sms_y2);
		assertNull(sms_y3);
		assertNull(sms_y4);
	}
	


	private SmsSet createTestSmsSet(int num, TargetAddress ta) {

		SmsSet smsSet = new SmsSet();
		smsSet.setDestAddr(ta.getAddr());
		smsSet.setDestAddrTon(ta.getAddrTon());
		smsSet.setDestAddrNpi(ta.getAddrNpi());

//		smsSet.setDestClusterName("tag_cluster_" + num);
//		smsSet.setDestEsmeId("esme_" + num);
//		smsSet.setDestSystemId("sys_" + num);
//
//		smsSet.setImsi(new IMSIImpl("1234567890123_" + num));
//		ISDNAddressString nnn = new ISDNAddressStringImpl(AddressNature.network_specific_number, NumberingPlan.land_mobile, "335577_" + num);
//		smsSet.setLocationInfoWithLMSI(new LocationInfoWithLMSIImpl(nnn, null, null, null, null));
//
//		smsSet.setStatus(ErrorCode.fromInt(10 + num));
//		smsSet.setDeliveryCount(78 + num);

		return smsSet;
	}

	private void checkTestSmsSet(SmsSet smsSet, int num, TargetAddress ta) {

		assertEquals(smsSet.getDestAddr(), ta.getAddr());
		assertEquals(smsSet.getDestAddrTon(), ta.getAddrTon());
		assertEquals(smsSet.getDestAddrNpi(), ta.getAddrNpi());
	}


	private Sms createTestSms(int num, SmsSet smsSet, UUID id) {

		Sms sms = new Sms();
		sms.setSmsSet(smsSet);

//		sms.setDbId(UUID.randomUUID());
		sms.setDbId(id);
		sms.setSourceAddr("11112_" + num);
		sms.setSourceAddrTon(14 + num);
		sms.setSourceAddrNpi(11 + num);
		sms.setMessageId(8888888 + num);

		sms.setOrigEsmeId("esme_" + num);
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

	private void checkTestSms(int numm, SmsProxy sms, UUID id, boolean isArchive) {
		this.checkTestSms(numm, sms.sms, id, isArchive);

		int num;
		if (numm == 4)
			num = 2;
		else
			num = 1;

		if (num == 1)
			assertEquals(sms.addrDstDigits, "1111");
		else
			assertEquals(sms.addrDstDigits, "1112");

		assertEquals(sms.addrDstTon, 5);
		assertEquals(sms.addrDstNpi, 1);

		if (isArchive) {
			assertTrue(sms.deliveryDate.equals(new GregorianCalendar(2013, 1, 15, 12, 15 + numm).getTime()));

			assertEquals(sms.dest�lusterName, "ClusterName_" + num);
			assertEquals(sms.destEsmeId, "destEsmeId_" + num);
			assertEquals(sms.destSystemId, "destSystemId_" + num);

			assertEquals(sms.imsi, "12345678901234");
			assertEquals(sms.nnnDigits, "3355778");
			assertEquals(sms.smStatus, (num == 1 ? 0 : ErrorCode.MEMORY_FULL.getCode()));
			assertEquals(sms.smType, (num == 1 ? SmType.DELIVER_SM.getCode() : SmType.SUBMIT_SM.getCode()));
			assertEquals(sms.deliveryCount, 1);
		}
	}

	private void checkTestSms(int num, Sms sms, UUID id, boolean isArchive) {

		assertTrue(sms.getDbId().equals(id));

		assertEquals(sms.getSourceAddr(), "11112_" + num);
		assertEquals(sms.getSourceAddrTon(), 14 + num);
		assertEquals(sms.getSourceAddrNpi(), 11 + num);

		assertEquals(sms.getMessageId(), 8888888 + num);
		assertEquals(sms.getOrigEsmeId(), "esme_" + num);
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



	private class SmsProxy {

		public Sms sms;

		public String addrDstDigits;
		public int addrDstTon;
		public int addrDstNpi;

		public String dest�lusterName; 
		public String destEsmeId;
		public String destSystemId;

		public String imsi;
		public String nnnDigits;
		public int smStatus;
		public int smType;
		public int deliveryCount;
		public Date deliveryDate;
	}

	private class CassandraPersistenceSbbProxy extends CassandraPersistenceSbb {

		public void setKeyspace(Keyspace val) {
			this.keyspace = val;
		}

		public boolean checkSmsSetExists(TargetAddress ta) {
			SliceQuery<String, Composite, ByteBuffer> query = HFactory.createSliceQuery(keyspace, SERIALIZER_STRING, SERIALIZER_COMPOSITE,
					ByteBufferSerializer.get());
			query.setColumnFamily(Schema.FAMILY_LIVE);
			query.setKey(ta.getTargetId());

			query.setRange(null, null, false, 100);

			QueryResult<ColumnSlice<Composite, ByteBuffer>> result = query.execute();
			ColumnSlice<Composite, ByteBuffer> cSlice = result.get();

			if (cSlice.getColumns().size() > 0)
				return true;
			else
				return false;
		}

		public void deleteLiveSms(UUID id) throws PersistenceException {
			Sms sms = new Sms();
			sms.setDbId(id);
			super.deleteLiveSms(sms);
		}

		public void deleteArchiveSms(UUID id) throws PersistenceException {
			Mutator<UUID> mutator = HFactory.createMutator(keyspace, UUIDSerializer.get());

			mutator.addDeletion(id, Schema.FAMILY_ARCHIVE);
			mutator.execute();
		}

		public SmsProxy obtainArchiveSms(UUID dbId) throws PersistenceException, IOException {
			SliceQuery<UUID, Composite, ByteBuffer> query = HFactory.createSliceQuery(keyspace, UUIDSerializer.get(), SERIALIZER_COMPOSITE,
					ByteBufferSerializer.get());
			query.setColumnFamily(Schema.FAMILY_ARCHIVE);
			query.setRange(null, null, false, 100);
			Composite cc = new Composite();
			cc.addComponent(Schema.COLUMN_ID, SERIALIZER_STRING);
			query.setKey(dbId);

			QueryResult<ColumnSlice<Composite, ByteBuffer>> result = query.execute();
			ColumnSlice<Composite, ByteBuffer> cSlice = result.get();

			Sms sms = this.createSms(cSlice, dbId, new SmsSet());
			if (sms == null)
				return null;

			result = query.execute();
			cSlice = result.get();
			SmsProxy res = new SmsProxy();
			res.sms = sms;
			for (HColumn<Composite, ByteBuffer> col : cSlice.getColumns()) {
				Composite nm = col.getName();
				String name = nm.get(0, SERIALIZER_STRING);

				if (name.equals(Schema.COLUMN_ADDR_DST_DIGITS)) {
					res.addrDstDigits = SERIALIZER_STRING.fromByteBuffer(col.getValue());
				} else if (name.equals(Schema.COLUMN_ADDR_DST_TON)) {
					res.addrDstTon = SERIALIZER_INTEGER.fromByteBuffer(col.getValue());
				} else if (name.equals(Schema.COLUMN_ADDR_DST_NPI)) {
					res.addrDstNpi = SERIALIZER_INTEGER.fromByteBuffer(col.getValue());

				} else if (name.equals(Schema.COLUMN_DEST_CLUSTER_NAME)) {
					res.dest�lusterName = SERIALIZER_STRING.fromByteBuffer(col.getValue());
				} else if (name.equals(Schema.COLUMN_DEST_ESME_ID)) {
					res.destEsmeId = SERIALIZER_STRING.fromByteBuffer(col.getValue());
				} else if (name.equals(Schema.COLUMN_DEST_SYSTEM_ID)) {
					res.destSystemId = SERIALIZER_STRING.fromByteBuffer(col.getValue());

				} else if (name.equals(Schema.COLUMN_IMSI)) {
					res.imsi = SERIALIZER_STRING.fromByteBuffer(col.getValue());
				} else if (name.equals(Schema.COLUMN_NNN_DIGITS)) {
					res.nnnDigits = SERIALIZER_STRING.fromByteBuffer(col.getValue());
				} else if (name.equals(Schema.COLUMN_SM_STATUS)) {
					res.smStatus = SERIALIZER_INTEGER.fromByteBuffer(col.getValue());
				} else if (name.equals(Schema.COLUMN_SM_TYPE)) {
					res.smType = SERIALIZER_INTEGER.fromByteBuffer(col.getValue());
				} else if (name.equals(Schema.COLUMN_DELIVERY_COUNT)) {
					res.deliveryCount = SERIALIZER_INTEGER.fromByteBuffer(col.getValue());
				} else if (name.equals(Schema.COLUMN_DELIVERY_DATE)) {
					res.deliveryDate = SERIALIZER_DATE.fromByteBuffer(col.getValue());
				}
			}

			return res;
		}
	}
}
