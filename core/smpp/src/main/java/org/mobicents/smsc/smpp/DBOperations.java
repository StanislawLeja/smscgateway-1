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

package org.mobicents.smsc.smpp;

import java.nio.ByteBuffer;
import java.util.List;

import me.prettyprint.cassandra.model.IndexedSlicesQuery;
import me.prettyprint.cassandra.serializers.ByteBufferSerializer;
import me.prettyprint.cassandra.serializers.CompositeSerializer;
import me.prettyprint.cassandra.serializers.IntegerSerializer;
import me.prettyprint.cassandra.serializers.StringSerializer;
import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.beans.Composite;
import me.prettyprint.hector.api.beans.HColumn;
import me.prettyprint.hector.api.beans.OrderedRows;
import me.prettyprint.hector.api.beans.Row;
import me.prettyprint.hector.api.factory.HFactory;
import me.prettyprint.hector.api.mutation.Mutator;
import me.prettyprint.hector.api.query.QueryResult;

/**
 * 
 * @author sergey vetyutnev
 * 
 */
public class DBOperations {
    
    public final static StringSerializer SERIALIZER_STRING = StringSerializer.get();
    public final static IntegerSerializer SERIALIZER_INTEGER = IntegerSerializer.get();
    public final static CompositeSerializer SERIALIZER_COMPOSITE = CompositeSerializer.get();

    /**
     * Adding DbSmsRoutingRule
     * 
     * @param dbSmsRoutingRule
     * @throws PersistenceException
     */
    public static void addDbSmsRoutingRule(final Keyspace keyspace, DbSmsRoutingRule dbSmsRoutingRule) throws PersistenceException {
        try {
            Mutator<Integer> mutator = HFactory.createMutator(keyspace, SERIALIZER_INTEGER.get());

            Composite cc;
            if (dbSmsRoutingRule.getAddress() != null) {
                cc = new Composite();
                cc.addComponent(Schema.COLUMN_ADDRESS, SERIALIZER_STRING);
                mutator.addInsertion(dbSmsRoutingRule.getId(), Schema.FAMILY_SMS_ROUTING_RULE,
                        HFactory.createColumn(cc, dbSmsRoutingRule.getAddress(), SERIALIZER_COMPOSITE, SERIALIZER_STRING));
            }
            if (dbSmsRoutingRule.getSystemId() != null) {
                cc = new Composite();
                cc.addComponent(Schema.COLUMN_SYSTEM_ID, SERIALIZER_STRING);
                mutator.addInsertion(dbSmsRoutingRule.getId(), Schema.FAMILY_SMS_ROUTING_RULE,
                        HFactory.createColumn(cc, dbSmsRoutingRule.getSystemId(), SERIALIZER_COMPOSITE, SERIALIZER_STRING));
            }

            mutator.execute();
        } catch (Exception e) {
            String msg = "Failed to addDbSmsRoutingRule for '" + dbSmsRoutingRule.getId() + "'!";

            throw new PersistenceException(msg, e);
        }
    }

    /**
     * Deleting DbSmsRoutingRule
     * 
     * @param id
     * @throws PersistenceException
     */
    public static void deleteDbSmsRoutingRule(final Keyspace keyspace, final int id) throws PersistenceException {
        try {
            Mutator<Integer> mutator = HFactory.createMutator(keyspace, SERIALIZER_INTEGER);

            mutator.addDeletion(id, Schema.FAMILY_SMS_ROUTING_RULE);
            mutator.execute();
        } catch (Exception e) {
            String msg = "Failed to deleteDbSmsRoutingRule for '" + id + "'!";
            throw new PersistenceException(msg, e);
        }
    }

    /**
     * Getting the first record from a SmsRoutingRule that ADDRESS==address.
     * Returns null if there is no such record
     * 
     * @param address
     * @return
     * @throws PersistenceException
     */
    public static DbSmsRoutingRule fetchSmsRoutingRule(final Keyspace keyspace, final String address) throws PersistenceException {

        try {
            IndexedSlicesQuery<Integer, Composite, ByteBuffer> query = HFactory.createIndexedSlicesQuery(keyspace, SERIALIZER_INTEGER, SERIALIZER_COMPOSITE,
                    ByteBufferSerializer.get());
            query.setColumnFamily(Schema.FAMILY_SMS_ROUTING_RULE);
            query.setRange(null, null, false, 100);
            Composite cc = new Composite();
            cc.addComponent(Schema.COLUMN_ADDRESS, SERIALIZER_STRING);
            query.addEqualsExpression(cc, StringSerializer.get().toByteBuffer(address));

            final QueryResult<OrderedRows<Integer, Composite, ByteBuffer>> result = query.execute();
            final OrderedRows<Integer, Composite, ByteBuffer> rows = result.get();
            final List<Row<Integer, Composite, ByteBuffer>> rowsList = rows.getList();
            DbSmsRoutingRule res = null;
            for (Row<Integer, Composite, ByteBuffer> row : rowsList) {
                try {
                    res = new DbSmsRoutingRule();
                    res.setId(row.getKey());

                    for (HColumn<Composite, ByteBuffer> col : row.getColumnSlice().getColumns()) {
                        Composite nm = col.getName();
                        String name = nm.get(0, SERIALIZER_STRING);

                        if (name.equals(Schema.COLUMN_ADDRESS)) {
                            res.setAddress(SERIALIZER_STRING.fromByteBuffer(col.getValue()));
                        } else if (name.equals(Schema.COLUMN_SYSTEM_ID)) {
                            res.setSystemId(SERIALIZER_STRING.fromByteBuffer(col.getValue()));
                        }
                    }
                } catch (Exception e) {
                    String msg = "Failed to deserialize SMS at key '" + row.getKey() + "'!";
                    throw new PersistenceException(msg, e);
                }
                break;
            }

            return res;
        } catch (Exception e) {
            String msg = "Failed to fetchSchedulableSms DbSmsRoutingRule for '" + address + "'!";

            throw new PersistenceException(msg, e);
        }
    }
}
