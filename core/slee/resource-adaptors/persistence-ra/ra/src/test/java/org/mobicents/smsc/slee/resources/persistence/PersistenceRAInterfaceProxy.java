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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.mobicents.protocols.ss7.map.api.primitives.IMSI;
import org.mobicents.protocols.ss7.map.api.service.sms.LocationInfoWithLMSI;
import org.mobicents.smsc.slee.resources.persistence.DBOperations;
import org.mobicents.smsc.slee.resources.persistence.ErrorCode;
import org.mobicents.smsc.slee.resources.persistence.PersistenceException;
import org.mobicents.smsc.slee.resources.persistence.PersistenceRAInterface;
import org.mobicents.smsc.slee.resources.persistence.Schema;
import org.mobicents.smsc.slee.resources.persistence.SmType;
import org.mobicents.smsc.slee.resources.persistence.Sms;
import org.mobicents.smsc.slee.resources.persistence.SmsSet;
import org.mobicents.smsc.slee.resources.persistence.SmsSetCashe;
import org.mobicents.smsc.slee.resources.persistence.TargetAddress;

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

/**
 * 
 * @author sergey vetyutnev
 * 
 */
public class PersistenceRAInterfaceProxy implements PersistenceRAInterface {

	private Keyspace keyspace;
	
	public void setKeyspace(Keyspace val) {
		this.keyspace = val;
	}

	@Override
	public boolean checkSmsSetExists(TargetAddress ta) throws PersistenceException {
		return DBOperations.checkSmsSetExists(this.keyspace, ta);
	}

	@Override
	public SmsSet obtainSmsSet(TargetAddress ta) throws PersistenceException {
		return DBOperations.obtainSmsSet(this.keyspace, ta);
	}

	@Override
	public void setNewMessageScheduled(SmsSet smsSet, Date newDueDate) throws PersistenceException {
		DBOperations.setNewMessageScheduled(this.keyspace, smsSet, newDueDate);
	}

	@Override
	public void setDeliveringProcessScheduled(SmsSet smsSet, Date newDueDate, int newDueDelay) throws PersistenceException {
		DBOperations.setDeliveringProcessScheduled(this.keyspace, smsSet, newDueDate, newDueDelay);
	}

	@Override
	public void setDestination(SmsSet smsSet, String destClusterName, String destSystemId, String destEsmeId, SmType type) {
		DBOperations.setDestination(smsSet, destClusterName, destSystemId, destEsmeId, type);
	}

	@Override
	public void setRoutingInfo(SmsSet smsSet, IMSI imsi, LocationInfoWithLMSI locationInfoWithLMSI) {
		DBOperations.setRoutingInfo(smsSet, imsi, locationInfoWithLMSI);
	}

	@Override
	public void setDeliveryStart(SmsSet smsSet, Date inSystemDate) throws PersistenceException {
		DBOperations.setDeliveryStart(this.keyspace, smsSet, inSystemDate);
	}

	@Override
	public void setDeliveryStart(Sms sms) throws PersistenceException {
		DBOperations.setDeliveryStart(this.keyspace, sms);
	}

	@Override
	public void setDeliverySuccess(SmsSet smsSet, Date lastDelivery) throws PersistenceException {
		DBOperations.setDeliverySuccess(this.keyspace, smsSet, lastDelivery);
	}

	@Override
	public void setDeliveryFailure(SmsSet smsSet, ErrorCode smStatus, Date lastDelivery) throws PersistenceException {
		DBOperations.setDeliveryFailure(this.keyspace, smsSet, smStatus, lastDelivery);
	}

	@Override
	public void setAlertingSupported(String targetId, boolean alertingSupported) throws PersistenceException {
		DBOperations.setAlertingSupported(this.keyspace, targetId, alertingSupported);
	}

	@Override
	public boolean deleteSmsSet(SmsSet smsSet) throws PersistenceException {
		return DBOperations.deleteSmsSet(this.keyspace, smsSet);
	}

	@Override
	public void createLiveSms(Sms sms) throws PersistenceException {
		DBOperations.createLiveSms(this.keyspace, sms);
	}

	@Override
	public Sms obtainLiveSms(UUID dbId) throws PersistenceException {
		return DBOperations.obtainLiveSms(this.keyspace, dbId);
	}

	@Override
	public Sms obtainLiveSms(long messageId) throws PersistenceException {
		return DBOperations.obtainLiveSms(this.keyspace, messageId);
	}

	@Override
	public void updateLiveSms(Sms sms) throws PersistenceException {
		DBOperations.updateLiveSms(this.keyspace, sms);
	}

	@Override
	public void archiveDeliveredSms(Sms sms, Date deliveryDate) throws PersistenceException {
		DBOperations.archiveDeliveredSms(this.keyspace, sms, deliveryDate);
	}

	@Override
	public void archiveFailuredSms(Sms sms) throws PersistenceException {
		DBOperations.archiveFailuredSms(this.keyspace, sms);
	}

	@Override
	public List<SmsSet> fetchSchedulableSmsSets(int maxRecordCount) throws PersistenceException {
		return DBOperations.fetchSchedulableSmsSets(this.keyspace, maxRecordCount);
	}

	@Override
	public TargetAddress obtainSynchroObject(TargetAddress ta) {
		return SmsSetCashe.getInstance().addSmsSet(ta);
	}

	@Override
	public void releaseSynchroObject(TargetAddress ta) {
    	SmsSetCashe.getInstance().removeSmsSet(ta);
	}

	@Override
	public void fetchSchedulableSms(SmsSet smsSet) throws PersistenceException {
		DBOperations.fetchSchedulableSms(this.keyspace, smsSet);
	}

	public boolean testCassandraAccess() {
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

			this.setKeyspace(keyspace);

			return true;
		} catch (Exception e) {
			return false;
		}
	}

	public void deleteLiveSms(UUID id) throws PersistenceException {
		Sms sms = new Sms();
		sms.setDbId(id);
		DBOperationsProxy.doDeleteLiveSms(this.keyspace, sms);
	}

	public void deleteArchiveSms(UUID id) throws PersistenceException {
		Mutator<UUID> mutator = HFactory.createMutator(keyspace, UUIDSerializer.get());

		mutator.addDeletion(id, Schema.FAMILY_ARCHIVE);
		mutator.execute();
	}

	public SmsProxy obtainArchiveSms(UUID dbId) throws PersistenceException, IOException {
		SliceQuery<UUID, Composite, ByteBuffer> query = HFactory.createSliceQuery(keyspace, UUIDSerializer.get(), DBOperations.SERIALIZER_COMPOSITE,
				ByteBufferSerializer.get());
		query.setColumnFamily(Schema.FAMILY_ARCHIVE);
		query.setRange(null, null, false, 100);
		Composite cc = new Composite();
		cc.addComponent(Schema.COLUMN_ID, DBOperations.SERIALIZER_STRING);
		query.setKey(dbId);

		QueryResult<ColumnSlice<Composite, ByteBuffer>> result = query.execute();
		ColumnSlice<Composite, ByteBuffer> cSlice = result.get();

		Sms sms = DBOperationsProxy.doCreateSms(this.keyspace, cSlice, dbId, new SmsSet());
		if (sms == null)
			return null;

		result = query.execute();
		cSlice = result.get();
		SmsProxy res = new SmsProxy();
		res.sms = sms;
		for (HColumn<Composite, ByteBuffer> col : cSlice.getColumns()) {
			Composite nm = col.getName();
			String name = nm.get(0, DBOperations.SERIALIZER_STRING);

			if (name.equals(Schema.COLUMN_ADDR_DST_DIGITS)) {
				res.addrDstDigits = DBOperations.SERIALIZER_STRING.fromByteBuffer(col.getValue());
			} else if (name.equals(Schema.COLUMN_ADDR_DST_TON)) {
				res.addrDstTon = DBOperations.SERIALIZER_INTEGER.fromByteBuffer(col.getValue());
			} else if (name.equals(Schema.COLUMN_ADDR_DST_NPI)) {
				res.addrDstNpi = DBOperations.SERIALIZER_INTEGER.fromByteBuffer(col.getValue());

			} else if (name.equals(Schema.COLUMN_DEST_CLUSTER_NAME)) {
				res.destClusterName = DBOperations.SERIALIZER_STRING.fromByteBuffer(col.getValue());
			} else if (name.equals(Schema.COLUMN_DEST_ESME_NAME)) {
				res.destEsmeName = DBOperations.SERIALIZER_STRING.fromByteBuffer(col.getValue());
			} else if (name.equals(Schema.COLUMN_DEST_SYSTEM_ID)) {
				res.destSystemId = DBOperations.SERIALIZER_STRING.fromByteBuffer(col.getValue());

			} else if (name.equals(Schema.COLUMN_IMSI)) {
				res.imsi = DBOperations.SERIALIZER_STRING.fromByteBuffer(col.getValue());
			} else if (name.equals(Schema.COLUMN_NNN_DIGITS)) {
				res.nnnDigits = DBOperations.SERIALIZER_STRING.fromByteBuffer(col.getValue());
			} else if (name.equals(Schema.COLUMN_SM_STATUS)) {
				res.smStatus = DBOperations.SERIALIZER_INTEGER.fromByteBuffer(col.getValue());
			} else if (name.equals(Schema.COLUMN_SM_TYPE)) {
				res.smType = DBOperations.SERIALIZER_INTEGER.fromByteBuffer(col.getValue());
			} else if (name.equals(Schema.COLUMN_DELIVERY_COUNT)) {
				res.deliveryCount = DBOperations.SERIALIZER_INTEGER.fromByteBuffer(col.getValue());
			} else if (name.equals(Schema.COLUMN_DELIVERY_DATE)) {
				res.deliveryDate = DBOperations.SERIALIZER_DATE.fromByteBuffer(col.getValue());
			}
		}

		return res;
	}

}
