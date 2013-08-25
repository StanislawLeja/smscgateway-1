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

import javax.slee.facilities.Tracer;

import org.apache.log4j.Logger;
import org.mobicents.protocols.ss7.map.api.primitives.IMSI;
import org.mobicents.protocols.ss7.map.api.service.sms.LocationInfoWithLMSI;
import org.mobicents.smsc.cassandra.DBOperations;
import org.mobicents.smsc.cassandra.ErrorCode;
import org.mobicents.smsc.cassandra.PersistenceException;
import org.mobicents.smsc.cassandra.Schema;
import org.mobicents.smsc.cassandra.SmType;
import org.mobicents.smsc.cassandra.Sms;
import org.mobicents.smsc.cassandra.SmsSet;
import org.mobicents.smsc.cassandra.SmsSetCashe;
import org.mobicents.smsc.cassandra.TargetAddress;
import org.mobicents.smsc.slee.resources.persistence.PersistenceRAInterface;
import org.mobicents.smsc.smpp.PersistenceProxy;

import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Host;
import com.datastax.driver.core.Metadata;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;

/**
 * 
 * @author sergey vetyutnev
 * 
 */
public class PersistenceRAInterfaceProxy extends DBOperations implements PersistenceRAInterface {

    private static final Logger logger = Logger.getLogger(PersistenceRAInterfaceProxy.class);

	public boolean testCassandraAccess() {

        String ip = "127.0.0.1";
        String keyspace = "TelestaxSMSC";

        try {
            Cluster cluster = Cluster.builder().addContactPoint(ip).build();
            Metadata metadata = cluster.getMetadata();

            for (Host host : metadata.getAllHosts()) {
                logger.info(String.format("Datacenter: %s; Host: %s; Rack: %s\n", host.getDatacenter(), host.getAddress(), host.getRack()));
            }

            Session session = cluster.connect();

            session.execute("USE \"" + keyspace + "\"");

            PreparedStatement ps = session.prepare("select * from \"" + Schema.FAMILY_LIVE + "\" limit 1;");
            BoundStatement boundStatement = new BoundStatement(ps);
            boundStatement.bind();
            session.execute(boundStatement);

            return true;
        } catch (Exception e) {
            return false;
        }
	}

	public void deleteLiveSms(UUID id) throws PersistenceException {
        Sms sms = new Sms();
        sms.setDbId(id);
        super.deleteLiveSms(sms);
	}

	public void deleteArchiveSms(UUID id) throws PersistenceException {
        PreparedStatement ps = session.prepare("delete from \"" + Schema.FAMILY_ARCHIVE + "\" where \"" + Schema.COLUMN_ID + "\"=?;");
        BoundStatement boundStatement = new BoundStatement(ps);
        boundStatement.bind(id);
        session.execute(boundStatement);

//		Mutator<UUID> mutator = HFactory.createMutator(keyspace, UUIDSerializer.get());
//
//		mutator.addDeletion(id, Schema.FAMILY_ARCHIVE);
//		mutator.execute();
	}

	public SmsProxy obtainArchiveSms(UUID dbId) throws PersistenceException, IOException {
        // TODO: implement it

        PreparedStatement ps = session.prepare("select * from \"" + Schema.FAMILY_ARCHIVE + "\" where \"" + Schema.COLUMN_ID + "\"=?;");
        BoundStatement boundStatement = new BoundStatement(ps);
        boundStatement.bind(dbId);
        ResultSet result = session.execute(boundStatement);

        Row row = result.one();
        Sms sms = createSms(row, new SmsSet(), dbId);
        if (sms == null)
            return null;

        SmsProxy res = new SmsProxy();
        res.sms = sms;

            
        // TODO: ........................
        // !!!!!! заполнить это !!!!!!!!!!!!
//            Composite nm = col.getName();
//            String name = nm.get(0, DBOperations.SERIALIZER_STRING);
//
//            if (name.equals(Schema.COLUMN_ADDR_DST_DIGITS)) {
//                res.addrDstDigits = DBOperations.SERIALIZER_STRING.fromByteBuffer(col.getValue());
//            } else if (name.equals(Schema.COLUMN_ADDR_DST_TON)) {
//                res.addrDstTon = DBOperations.SERIALIZER_INTEGER.fromByteBuffer(col.getValue());
//            } else if (name.equals(Schema.COLUMN_ADDR_DST_NPI)) {
//                res.addrDstNpi = DBOperations.SERIALIZER_INTEGER.fromByteBuffer(col.getValue());
//
//            } else if (name.equals(Schema.COLUMN_DEST_CLUSTER_NAME)) {
//                res.destClusterName = DBOperations.SERIALIZER_STRING.fromByteBuffer(col.getValue());
//            } else if (name.equals(Schema.COLUMN_DEST_ESME_NAME)) {
//                res.destEsmeName = DBOperations.SERIALIZER_STRING.fromByteBuffer(col.getValue());
//            } else if (name.equals(Schema.COLUMN_DEST_SYSTEM_ID)) {
//                res.destSystemId = DBOperations.SERIALIZER_STRING.fromByteBuffer(col.getValue());
//
//            } else if (name.equals(Schema.COLUMN_IMSI)) {
//                res.imsi = DBOperations.SERIALIZER_STRING.fromByteBuffer(col.getValue());
//            } else if (name.equals(Schema.COLUMN_NNN_DIGITS)) {
//                res.nnnDigits = DBOperations.SERIALIZER_STRING.fromByteBuffer(col.getValue());
//            } else if (name.equals(Schema.COLUMN_SM_STATUS)) {
//                res.smStatus = DBOperations.SERIALIZER_INTEGER.fromByteBuffer(col.getValue());
//            } else if (name.equals(Schema.COLUMN_SM_TYPE)) {
//                res.smType = DBOperations.SERIALIZER_INTEGER.fromByteBuffer(col.getValue());
//            } else if (name.equals(Schema.COLUMN_DELIVERY_COUNT)) {
//                res.deliveryCount = DBOperations.SERIALIZER_INTEGER.fromByteBuffer(col.getValue());
//            } else if (name.equals(Schema.COLUMN_DELIVERY_DATE)) {
//                res.deliveryDate = DBOperations.SERIALIZER_DATE.fromByteBuffer(col.getValue());
//            }
        // !!!!!! заполнить это !!!!!!!!!!!!
        // TODO: ........................

        return res;

        
        
        
        
        
        
//		SliceQuery<UUID, Composite, ByteBuffer> query = HFactory.createSliceQuery(keyspace, UUIDSerializer.get(),
//				DBOperations.SERIALIZER_COMPOSITE, ByteBufferSerializer.get());
//		query.setColumnFamily(Schema.FAMILY_ARCHIVE);
//		query.setRange(null, null, false, 100);
//		Composite cc = new Composite();
//		cc.addComponent(Schema.COLUMN_ID, DBOperations.SERIALIZER_STRING);
//		query.setKey(dbId);
//
//		QueryResult<ColumnSlice<Composite, ByteBuffer>> result = query.execute();
//		ColumnSlice<Composite, ByteBuffer> cSlice = result.get();
//
//		Sms sms = DBOperationsProxy.doCreateSms(this.keyspace, cSlice, dbId, new SmsSet());
//		if (sms == null)
//			return null;
//
//		result = query.execute();
//		cSlice = result.get();
//		SmsProxy res = new SmsProxy();
//		res.sms = sms;
//		for (HColumn<Composite, ByteBuffer> col : cSlice.getColumns()) {
//			Composite nm = col.getName();
//			String name = nm.get(0, DBOperations.SERIALIZER_STRING);
//
//			if (name.equals(Schema.COLUMN_ADDR_DST_DIGITS)) {
//				res.addrDstDigits = DBOperations.SERIALIZER_STRING.fromByteBuffer(col.getValue());
//			} else if (name.equals(Schema.COLUMN_ADDR_DST_TON)) {
//				res.addrDstTon = DBOperations.SERIALIZER_INTEGER.fromByteBuffer(col.getValue());
//			} else if (name.equals(Schema.COLUMN_ADDR_DST_NPI)) {
//				res.addrDstNpi = DBOperations.SERIALIZER_INTEGER.fromByteBuffer(col.getValue());
//
//			} else if (name.equals(Schema.COLUMN_DEST_CLUSTER_NAME)) {
//				res.destClusterName = DBOperations.SERIALIZER_STRING.fromByteBuffer(col.getValue());
//			} else if (name.equals(Schema.COLUMN_DEST_ESME_NAME)) {
//				res.destEsmeName = DBOperations.SERIALIZER_STRING.fromByteBuffer(col.getValue());
//			} else if (name.equals(Schema.COLUMN_DEST_SYSTEM_ID)) {
//				res.destSystemId = DBOperations.SERIALIZER_STRING.fromByteBuffer(col.getValue());
//
//			} else if (name.equals(Schema.COLUMN_IMSI)) {
//				res.imsi = DBOperations.SERIALIZER_STRING.fromByteBuffer(col.getValue());
//			} else if (name.equals(Schema.COLUMN_NNN_DIGITS)) {
//				res.nnnDigits = DBOperations.SERIALIZER_STRING.fromByteBuffer(col.getValue());
//			} else if (name.equals(Schema.COLUMN_SM_STATUS)) {
//				res.smStatus = DBOperations.SERIALIZER_INTEGER.fromByteBuffer(col.getValue());
//			} else if (name.equals(Schema.COLUMN_SM_TYPE)) {
//				res.smType = DBOperations.SERIALIZER_INTEGER.fromByteBuffer(col.getValue());
//			} else if (name.equals(Schema.COLUMN_DELIVERY_COUNT)) {
//				res.deliveryCount = DBOperations.SERIALIZER_INTEGER.fromByteBuffer(col.getValue());
//			} else if (name.equals(Schema.COLUMN_DELIVERY_DATE)) {
//				res.deliveryDate = DBOperations.SERIALIZER_DATE.fromByteBuffer(col.getValue());
//			}
//		}
	}

    @Override
    public TargetAddress obtainSynchroObject(TargetAddress ta) {
        return SmsSetCashe.getInstance().addSmsSet(ta);
    }

    @Override
    public void releaseSynchroObject(TargetAddress ta) {
        SmsSetCashe.getInstance().removeSmsSet(ta);
    }

}
