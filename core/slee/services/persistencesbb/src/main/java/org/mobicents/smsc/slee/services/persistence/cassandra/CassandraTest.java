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

package org.mobicents.smsc.slee.services.persistence.cassandra;

import java.nio.ByteBuffer;
import java.util.Date;
import java.util.List;

import javax.slee.ActivityContextInterface;
import javax.slee.CreateException;
import javax.slee.RolledBackContext;

import me.prettyprint.cassandra.model.IndexedSlicesQuery;
import me.prettyprint.cassandra.serializers.ByteBufferSerializer;
import me.prettyprint.cassandra.serializers.CompositeSerializer;
import me.prettyprint.cassandra.serializers.IntegerSerializer;
import me.prettyprint.cassandra.serializers.StringSerializer;
import me.prettyprint.cassandra.serializers.TimeUUIDSerializer;
import me.prettyprint.cassandra.service.CassandraHostConfigurator;
import me.prettyprint.hector.api.Cluster;
import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.beans.Composite;
import me.prettyprint.hector.api.beans.HColumn;
import me.prettyprint.hector.api.beans.OrderedRows;
import me.prettyprint.hector.api.beans.Row;
import me.prettyprint.hector.api.factory.HFactory;
import me.prettyprint.hector.api.mutation.Mutator;
import me.prettyprint.hector.api.query.ColumnQuery;
import me.prettyprint.hector.api.query.QueryResult;
import me.prettyprint.hector.api.query.SliceQuery;

import org.mobicents.protocols.ss7.map.api.primitives.IMSI;
import org.mobicents.protocols.ss7.map.api.service.sms.LocationInfoWithLMSI;
import org.mobicents.smsc.slee.services.persistence.ErrorCode;
import org.mobicents.smsc.slee.services.persistence.PersistenceException;
import org.mobicents.smsc.slee.services.persistence.Sms;
import org.mobicents.smsc.slee.services.persistence.SmsSet;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.eaio.uuid.UUID;

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
			coKey3.addComponent(1, IntegerSerializer.get());
			coKey3.addComponent(4, IntegerSerializer.get());
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

	@Test(groups = { "cassandra"})
	public void testA() {
		if (!this.cassandraDbInited)
			return;		
		
		
		try {
			Cluster cluster = HFactory.getOrCreateCluster("TestCluster", new CassandraHostConfigurator("localhost:9160"));
			Keyspace keyspace = HFactory.createKeyspace("TelestaxSMSC", cluster);

			// saving 
			String addrDstDigits = "1111";
			Mutator<String> mutator = HFactory.createMutator(keyspace, StringSerializer.get());
			Composite cc = new Composite();
			cc.addComponent("ADDR_DST_DIGITS", StringSerializer.get());
	        mutator.addInsertion(addrDstDigits, "TST", HFactory.createColumn(cc, addrDstDigits, CompositeSerializer.get(), StringSerializer.get()));
			cc = new Composite();
			cc.addComponent("IN_SYSTEM", StringSerializer.get());
	        mutator.addInsertion(addrDstDigits, "TST", HFactory.createColumn(cc, 1, CompositeSerializer.get(), IntegerSerializer.get()));
//	        mutator.addInsertion(addrDstDigits, "TST", HFactory.createColumn("IN_SYSTEM", 1, StringSerializer.get(), IntegerSerializer.get()));
//	        mutator.addInsertion(addrDstDigits, "TST", HFactory.createColumn("SM_STATUS", 0, StringSerializer.get(), IntegerSerializer.get()));

	        mutator.execute();
			
//			String addrDstDigits = "1111";
//			int ton = 1;
//			int npi = 4;
//			Mutator<String> mutator = HFactory.createMutator(keyspace, StringSerializer.get());
//			Composite coKey3 = new Composite();
//			coKey3.addComponent(ton, IntegerSerializer.get());
//			coKey3.addComponent(npi, IntegerSerializer.get());
//			coKey3.addComponent(Schema.COLUMN_ADDR_DST_TON, StringSerializer.get());
//	        mutator.addInsertion(addrDstDigits, Schema.FAMILY_LIVE, HFactory.createColumn(coKey3, ton, CompositeSerializer.get(), IntegerSerializer.get()));
//			coKey3 = new Composite();
//			coKey3.addComponent(ton, IntegerSerializer.get());
//			coKey3.addComponent(npi, IntegerSerializer.get());
//			coKey3.addComponent(Schema.COLUMN_ADDR_DST_NPI, StringSerializer.get());
//	        mutator.addInsertion(addrDstDigits, Schema.FAMILY_LIVE, HFactory.createColumn(coKey3, npi, CompositeSerializer.get(), IntegerSerializer.get()));
//
//	        mutator.execute();


//			Mutator<Composite> mutator = HFactory.createMutator(keyspace, CompositeSerializer.get());
//	        Composite colKey = new Composite();
//	        colKey.addComponent("1111", new StringSerializer());
//	        colKey.addComponent(1, new IntegerSerializer());
//	        colKey.addComponent(4, new IntegerSerializer());
//
//	        mutator.addInsertion(colKey, Schema.FAMILY_LIVE, HFactory.createStringColumn(Schema.COLUMN_ADDR_DST_DIGITS, "1111"));
//	        mutator.addInsertion(colKey, Schema.FAMILY_LIVE, HFactory.createColumn(Schema.COLUMN_ADDR_DST_TON, 1, StringSerializer.get(), IntegerSerializer.get()));
//	        mutator.addInsertion(colKey, Schema.FAMILY_LIVE, HFactory.createColumn(Schema.COLUMN_ADDR_DST_NPI, 4, StringSerializer.get(), IntegerSerializer.get()));

			
//	        // COLUMN_SYSTEM_ID
//	        if (sms.getOrigSystemId() != null)
//	            mutator.addInsertion(archiveKey, FAMILY,
//	                    HFactory.createStringColumn(Schema.COLUMN_SYSTEM_ID, sms.getOrigSystemId()));
//	        // COLUMN_ADDR_SRC_TON
//	        mutator.addInsertion(archiveKey, FAMILY, HFactory.createColumn(Schema.COLUMN_ADDR_SRC_TON,
//	                (int) sms.getSourceAddrTon(), SERIALIZER_STRING, SERIALIZER_INTEGER));
//	        // COLUMN_ADDR_SRC_NPI
//	        mutator.addInsertion(archiveKey, FAMILY, HFactory.createColumn(Schema.COLUMN_ADDR_SRC_NPI,
//	                (int) sms.getSourceAddrNpi(), SERIALIZER_STRING, SERIALIZER_INTEGER));
//	        // COLUMN_ADDR_SRC_DIGITS
//	        mutator.addInsertion(archiveKey, FAMILY,
//	                HFactory.createStringColumn(Schema.COLUMN_ADDR_SRC_DIGITS, sms.getSourceAddr()));
	//
//	        // COLUMN_ADDR_DST_TON
//	        mutator.addInsertion(archiveKey, FAMILY, HFactory.createColumn(Schema.COLUMN_ADDR_DST_TON,
//	                (int) sms.getDestAddrTon(), SERIALIZER_STRING, SERIALIZER_INTEGER));
//	        // COLUMN_ADDR_DST_NPI
//	        mutator.addInsertion(archiveKey, FAMILY, HFactory.createColumn(Schema.COLUMN_ADDR_DST_NPI,
//	                (int) sms.getDestAddrNpi(), SERIALIZER_STRING, SERIALIZER_INTEGER));
//	        // COLUMN_ADDR_DST_DIGITS
//	        mutator.addInsertion(archiveKey, FAMILY,
//	                HFactory.createStringColumn(Schema.COLUMN_ADDR_DST_DIGITS, sms.getDestAddr()));
	//
//	        // COLUMN_ESM_CLASS
//	        mutator.addInsertion(archiveKey, FAMILY, HFactory.createColumn(Schema.COLUMN_ESM_CLASS,
//	                (int) sms.getEsmClass(), SERIALIZER_STRING, SERIALIZER_INTEGER));
//	        // COLUMN_PROTOCOL_ID
//	        mutator.addInsertion(archiveKey, FAMILY, HFactory.createColumn(Schema.COLUMN_PROTOCOL_ID,
//	                (int) sms.getProtocolId(), SERIALIZER_STRING, SERIALIZER_INTEGER));
//	        // COLUMN_PRIORITY
//	        mutator.addInsertion(archiveKey, FAMILY, HFactory.createColumn(Schema.COLUMN_PRIORITY,
//	                (int) sms.getPriority(), SERIALIZER_STRING, SERIALIZER_INTEGER));
//	        // COLUMN_REGISTERED_DELIVERY
//	        // TODO: XXX: improve SMS.getRegisteredDelivery() its [1,0], but its not set as boolean.
//	        mutator.addInsertion(archiveKey, FAMILY, HFactory.createColumn(Schema.COLUMN_PRIORITY,
//	                new Integer(sms.getRegisteredDelivery()) , SERIALIZER_STRING, IntegerSerializer.get()));
//	        // COLUMN_REPLACE
//	        // TODO: XXX: improve SMS.getReplaceIfPresent() its [1,0], but its not set as boolean.
//	        mutator.addInsertion(archiveKey, FAMILY, HFactory.createColumn(Schema.COLUMN_REPLACE,
//	                new Integer(sms.getReplaceIfPresent()) , SERIALIZER_STRING, IntegerSerializer.get()));
//	        // COLUMN_DATA_CODING
//	        mutator.addInsertion(archiveKey, FAMILY, HFactory.createColumn(Schema.COLUMN_DATA_CODING,
//	                (int) sms.getDataCoding(), SERIALIZER_STRING, SERIALIZER_INTEGER));
//	        // COLUMN_DEFAULT_MSG_ID
//	        mutator.addInsertion(archiveKey, FAMILY, HFactory.createColumn(Schema.COLUMN_DEFAULT_MSG_ID,
//	                (int) sms.getDefaultMsgId(), SERIALIZER_STRING, SERIALIZER_INTEGER));
//	        // COLUMN_MESSAGE
//	        mutator.addInsertion(archiveKey, FAMILY, HFactory.createColumn(Schema.COLUMN_MESSAGE, sms.getShortMessage(),
//	                SERIALIZER_STRING, SERIALIZER_BYTE_ARRAY));
//	        // COLUMN_OPTIONAL_PARAMETERS
//	        if (sms.getOptionalParameterCount() > 0) {
//	            // TODO: XXX: tricky stuff
//	        }
	//
//	        // COLUMN_SUBMIT_DATE
//	        mutator.addInsertion(archiveKey, FAMILY, HFactory.createColumn(Schema.COLUMN_SUBMIT_DATE, sms.getSubmitDate(),
//	                SERIALIZER_STRING, SERIALIZER_DATE));
//	        // COLUMN_SCHEDULE_DELIVERY
//	        // TODO: XXX: ??
//	        final Date scheduledDelivery = sms.getScheduleDeliveryTime();
//	        mutator.addInsertion(archiveKey, FAMILY, HFactory.createColumn(Schema.COLUMN_SCHEDULE_DELIVERY,
//	                scheduledDelivery, SERIALIZER_STRING, SERIALIZER_DATE));
//	        // COLUMN_VALIDITY_PERIOD
//	        // TODO: XXX: ??
//	        final Date validityPeriod = sms.getValidityPeriod();
//	        mutator.addInsertion(archiveKey, FAMILY, HFactory.createColumn(Schema.COLUMN_VALIDITY_PERIOD, validityPeriod,
//	                SERIALIZER_STRING, SERIALIZER_DATE));
//	        // COLUMN_EXPIRY_DATE
//	        // TODO: XXX: XXX: what is that ?
//	        // COLUMN_SM_TYPE
//	        mutator.addInsertion(archiveKey, FAMILY, HFactory.createColumn(Schema.COLUMN_SM_TYPE, persistableSms.getType()
//	                .getCode(), SERIALIZER_STRING, SERIALIZER_INTEGER));
//	        // COLUMN_DELIVERY_COUNT
//	        mutator.addInsertion(archiveKey, FAMILY, HFactory.createColumn(Schema.COLUMN_DELIVERY_COUNT,
//	                persistableSms.getDeliveryCount(), SERIALIZER_STRING, SERIALIZER_INTEGER));
////	        // COLUMN_LAST_DELIVERY
////	        mutator.addInsertion(archiveKey, FAMILY,
////	                HFactory.createColumn(Schema.COLUMN_LAST_DELIVERY, new Date(), SERIALIZER_STRING, SERIALIZER_DATE));
//	        if(includeRuntime){
//	            // COLUMN_IN_SYSTEM
//	            mutator.addInsertion(archiveKey, FAMILY,
//	                    HFactory.createColumn(Schema.COLUMN_IN_SYSTEM, true, SERIALIZER_STRING, SERIALIZER_BOOLEAN));
//	            // COLUMN_ALERTING_SUPPORTED
//	            mutator.addInsertion(archiveKey, FAMILY, HFactory.createColumn(Schema.COLUMN_ALERTING_SUPPORTED,
//	                    persistableSms.isAlertingSupported(), SERIALIZER_STRING, SERIALIZER_BOOLEAN));
//	            //DUE_DATE is set on passivation
//	        }
//	        // Address Creation.
	//
//	        ((PersistableSmsImpl)persistableSms).setDbId(archiveKey);
//	        createSccpAddress(mutator, FAMILY,archiveKey,Schema.SCCP_PREFIX_SC, persistableSms.getScAddress());
	//
//	        if (persistableSms.getMxAddress() != null)
//	            createSccpAddress(mutator, FAMILY,archiveKey,Schema.SCCP_PREFIX_MX, persistableSms.getMxAddress());
	        
	        mutator.execute();
			
			// getting


			ColumnQuery<Composite, String, String> q = HFactory.createColumnQuery(keyspace, CompositeSerializer.get(), StringSerializer.get(),
					StringSerializer.get());			
	        Composite colKey2 = new Composite();
	        colKey2.addComponent("1111", StringSerializer.get());
	        colKey2.addComponent(1, IntegerSerializer.get());
	        colKey2.addComponent(4, IntegerSerializer.get());
	        q.setColumnFamily(Schema.FAMILY_LIVE);
	        q.setKey(colKey2);
	        q.setName(Schema.COLUMN_ADDR_DST_DIGITS);
	        QueryResult<HColumn<String, String>> qr = q.execute();

			
			
			
			IndexedSlicesQuery<String, String, ByteBuffer> query = HFactory.createIndexedSlicesQuery(keyspace, StringSerializer.get(), StringSerializer.get(),
					ByteBufferSerializer.get());
			query.setColumnFamily(Schema.FAMILY_LIVE);
			query.setColumnNames(Schema.COLUMNS_LIVE);
			query.addEqualsExpression(Schema.COLUMN_ADDR_DST_DIGITS, StringSerializer.get().toByteBuffer("1111"));
//			query.addEqualsExpression(Schema.COLUMN_ADDR_DST_TON, IntegerSerializer.get().toByteBuffer(1));
//			query.addEqualsExpression(Schema.COLUMN_ADDR_DST_NPI+"XXX", IntegerSerializer.get().toByteBuffer(4));
			// query.addGteExpression(Schema.COLUMN_SCHEDULE_DELIVERY,
			// SERIALIZER_DATE.toByteBuffer(new Date()));

			final QueryResult<OrderedRows<String, String, ByteBuffer>> result = query.execute();
			final OrderedRows<String, String, ByteBuffer> rows = result.get();
			final List<Row<String, String, ByteBuffer>> rowsList = rows.getList();

			// SliceQuery<String, String> q = HFactory.createSliceQuery(ko, se,
			// se, se);
			// q.setColumnFamily(cf).setKey("jsmith").setColumnNames("first",
			// "last", "middle");
			// Result<ColumnSlice<String, String>> r = q.execute();

			// ColumnQuery<String, String, String> columnQuery =
			// HFactory.createStringColumnQuery(keyspace);
			// columnQuery.setColumnFamily(Schema.FAMILY_LIVE).setKey("jsmith").setName("first");
			// QueryResult<HColumn<String, String>> result =
			// columnQuery.execute();

			int i1 = 10;
			i1++;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private class CassandraPersistenceSbbProxy extends CassandraPersistenceSbb {

		public void setKeyspace(Keyspace val) {
			this.keyspace = val;
		}

		@Override
		public void sbbActivate() {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void sbbCreate() throws CreateException {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void sbbExceptionThrown(Exception arg0, Object arg1, ActivityContextInterface arg2) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void sbbLoad() {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void sbbPassivate() {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void sbbPostCreate() throws CreateException {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void sbbRemove() {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void sbbRolledBack(RolledBackContext arg0) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void sbbStore() {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void setScheduled(SmsSet smsSet, Date newDueDate) throws PersistenceException {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void setDestination(SmsSet smsSet, String destClusterName, String destSystemId, String destEsmeId) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void setRoutingInfo(SmsSet smsSet, IMSI imsi, LocationInfoWithLMSI locationInfoWithLMSI) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void setDeliveryStart(SmsSet smsSet) throws PersistenceException {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void setDeliverySuccess(SmsSet smsSet) throws PersistenceException {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void setDeliveryFailure(SmsSet smsSet, ErrorCode smStatus, Date lastDelivery, boolean alertingSupported) throws PersistenceException {
			// TODO Auto-generated method stub
			
		}

		@Override
		public boolean deleteSmsSet(SmsSet smsSet) throws PersistenceException {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public void createLiveSms(Sms sms) throws PersistenceException {
			// TODO Auto-generated method stub
			
		}

		@Override
		public Sms obtainLiveSms(UUID dbId) throws PersistenceException {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Sms obtainLiveSms(long messageId) throws PersistenceException {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void updateLiveSms(Sms sms) throws PersistenceException {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void archiveDeliveredSms(Sms sms) throws PersistenceException {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void archiveFailuredSms(Sms sms) throws PersistenceException {
			// TODO Auto-generated method stub
			
		}

		@Override
		public List<SmsSet> fetchSchedulableSmsSets(int maxRecordCount) throws PersistenceException {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void fetchSchedulableSms(SmsSet smsSet) throws PersistenceException {
			// TODO Auto-generated method stub
			
		}
		
	}
}
