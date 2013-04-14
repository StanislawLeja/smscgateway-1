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

import java.nio.ByteBuffer;
import java.util.GregorianCalendar;
import java.util.UUID;

import me.prettyprint.cassandra.serializers.ByteBufferSerializer;
import me.prettyprint.cassandra.serializers.CompositeSerializer;
import me.prettyprint.cassandra.serializers.StringSerializer;
import me.prettyprint.cassandra.service.CassandraHostConfigurator;
import me.prettyprint.hector.api.Cluster;
import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.beans.Composite;
import me.prettyprint.hector.api.beans.HColumn;
import me.prettyprint.hector.api.factory.HFactory;
import me.prettyprint.hector.api.query.ColumnQuery;
import me.prettyprint.hector.api.query.QueryResult;

import org.mobicents.protocols.ss7.map.api.primitives.AddressNature;
import org.mobicents.protocols.ss7.map.api.primitives.ISDNAddressString;
import org.mobicents.protocols.ss7.map.api.primitives.NumberingPlan;
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
	public void storeRecallSms() throws Exception {
		if (!this.cassandraDbInited)
			return;


		TargetAddress ta1 = new TargetAddress(1, 4, "1111");		
		TargetAddress ta2 = new TargetAddress(1, 4, "1111");
		TargetAddress ta3 = new TargetAddress(1, 5, "1111");
		TargetAddress taa1 = this.sbb.obtainSynchroObject(ta1);
		TargetAddress taa2 = this.sbb.obtainSynchroObject(ta2);
		TargetAddress taa3 = this.sbb.obtainSynchroObject(ta3);
		
		this.sbb.releaseSynchroObject(taa1);
		this.sbb.releaseSynchroObject(taa2);
		this.sbb.releaseSynchroObject(taa3);
		
		
		
		
		Sms sms = new Sms();
		SmsSet smsSet = new SmsSet();
		sms.setSmsSet(smsSet);
		smsSet.setDestAddr("1111");
		smsSet.setDestAddrTon(5);
		smsSet.setDestAddrNpi(1);

		smsSet.setDestClusterName("tag_cluster");
		smsSet.setDestEsmeId("esme_2");
		smsSet.setDestSystemId("sys_2");

//		sms.setDbId(UUID.randomUUID());
		sms.setDbId(UUID.fromString("59e815dc-49ad-4539-8cff-beb710a7de03"));
		sms.setSourceAddr("11112");
		sms.setSourceAddrTon(14);
		sms.setSourceAddrNpi(11);
		sms.setMessageId(8888888);

		sms.setOrigEsmeId("esme_1");
		sms.setOrigSystemId("sys_1");

		sms.setSubmitDate(new GregorianCalendar(2013, 1, 15, 12, 00).getTime());
		sms.setDeliveryDate(new GregorianCalendar(2013, 1, 15, 12, 15).getTime());

		sms.setServiceType("serv_type_1");
		sms.setEsmClass(11);
		sms.setProtocolId(12);
		sms.setPriority(13);
		sms.setRegisteredDelivery(14);
		sms.setReplaceIfPresent(15);
		sms.setDataCoding(16);
		sms.setDefaultMsgId(17);

		sms.setShortMessage(new byte[] { 21, 23, 25, 27, 29 });		

		sms.setScheduleDeliveryTime(new GregorianCalendar(2013, 1, 20, 10, 00).getTime());
		sms.setValidityPeriod(new GregorianCalendar(2013, 1, 23, 13, 33).getTime());

		// short tag, byte[] value, String tagName
		Tlv tlv = new Tlv((short)5, new byte[] { 1, 2, 3, 4, 5 });
		sms.getTlvSet().addOptionalParameter(tlv);
		tlv = new Tlv((short)6, new byte[] { 6, 7, 8 });
		sms.getTlvSet().addOptionalParameter(tlv);

		smsSet.setImsi(new IMSIImpl("12345678901234"));
		ISDNAddressString nnn = new ISDNAddressStringImpl(AddressNature.network_specific_number, NumberingPlan.land_mobile, "335577");
		smsSet.setLocationInfoWithLMSI(new LocationInfoWithLMSIImpl(nnn, null, null, null, null));
		smsSet.setStatus(ErrorCode.APP_SPECIFIC_227);
		smsSet.setType(SmType.SUBMIT_SM);
		smsSet.setDeliveryCount(78);

		TargetAddress ta = new TargetAddress(smsSet);
		this.sbb.obtainSmsSet(ta);
		this.sbb.createLiveSms(sms);
		Sms sms2 = this.sbb.obtainLiveSms(sms.getDbId());

		this.sbb.archiveDeliveredSms(sms, new GregorianCalendar(2013, 1, 23, 14, 33).getTime());

		int j1 = 0;
		j1++;
	}

	@Test(groups = { "cassandra" })
	public void obtainSmsSet() throws Exception {
		if (!this.cassandraDbInited)
			return;

//		TargetAddress ta = new TargetAddress(5, 1, "1111");
//		SmsSet smsSet = this.sbb.obtainSmsSet(ta);
//		smsSet = this.sbb.obtainSmsSet(ta);
//
//		Date newDueDate = new GregorianCalendar(2013, 1, 16, 11, 00).getTime();
//		this.sbb.setScheduled(smsSet, newDueDate, false);
//
//		this.sbb.setDeliveryStart(smsSet);
//
//		this.sbb.setDeliverySuccess(smsSet);
//
//		Date lastDelivery = new GregorianCalendar(2013, 1, 18, 5, 55).getTime();
//		this.sbb.setDeliveryFailure(smsSet, ErrorCode.APP_SPECIFIC_250, lastDelivery, true);
//
//		this.sbb.deleteSmsSet(smsSet);
//		smsSet = this.sbb.obtainSmsSet(ta);
//		this.sbb.deleteSmsSet(smsSet);
//		this.sbb.deleteSmsSet(smsSet);
	}

	@Test(groups = { "cassandra" })
	public void createLiveSms() throws Exception {
		if (!this.cassandraDbInited)
			return;

		
		
//		Sms sms2 = this.sbb.obtainLiveSms(UUID.fromString("59e815dc-49ad-4539-8cff-beb710a7de03"));

		
		
//		Sms sms = new Sms();
//		SmsSet smsSet = new SmsSet();
//		sms.setSmsSet(smsSet);
//		smsSet.setDestAddr("1111");
//		smsSet.setDestAddrTon(5);
//		smsSet.setDestAddrNpi(1);
//
//		smsSet.setDestClusterName("tag_cluster");
//		smsSet.setDestEsmeId("esme_2");
//		smsSet.setDestSystemId("sys_2");
//		
////		sms.setDbId(UUID.randomUUID());
//		sms.setDbId(UUID.fromString("59e815dc-49ad-4539-8cff-beb710a7de03"));
//		sms.setSourceAddr("11112");
//		sms.setSourceAddrTon(14);
//		sms.setSourceAddrNpi(11);
//		sms.setMessageId(8888888);
//
//		sms.setOrigEsmeId("esme_1");
//		sms.setOrigSystemId("sys_1");
//
//		sms.setSubmitDate(new GregorianCalendar(2013, 1, 15, 12, 00).getTime());
//		sms.setDeliverDate(new GregorianCalendar(2013, 1, 15, 12, 15).getTime());
//
//		// short tag, byte[] value, String tagName
//		Tlv tlv = new Tlv((short)5, new byte[] { 1, 2, 3, 4, 5 });
//		sms.addOptionalParameter(tlv);
//		tlv = new Tlv((short)6, new byte[] { 6, 7, 8 });
//		sms.addOptionalParameter(tlv);
//
//		this.sbb.createLiveSms(sms);
//
//		this.sbb.fetchSchedulableSmsSets(100);
//
//		this.sbb.archiveDeliveredSms(sms);
//
////		Sms sms2 = this.sbb.obtainLiveSms(UUID.randomUUID());
//		Sms sms2 = this.sbb.obtainLiveSms(sms.getDbId());
//		Sms sms3 = this.sbb.obtainLiveSms(sms.getMessageId());
//
//		int j1 = 0;
//		j1++;
	}


	private class CassandraPersistenceSbbProxy extends CassandraPersistenceSbb {

		public void setKeyspace(Keyspace val) {
			this.keyspace = val;
		}

	}
}
