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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.UUID;

import org.mobicents.smsc.slee.services.persistence.cassandra.CassandraPersistenceSbb;
import org.mobicents.smsc.slee.services.persistence.cassandra.Schema;

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
public class CassandraPersistenceSbbProxy extends CassandraPersistenceSbb {

	public void setKeyspace(Keyspace val) {
		this.keyspace = val;
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
				res.destClusterName = SERIALIZER_STRING.fromByteBuffer(col.getValue());
			} else if (name.equals(Schema.COLUMN_DEST_ESME_NAME)) {
				res.destEsmeName = SERIALIZER_STRING.fromByteBuffer(col.getValue());
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
