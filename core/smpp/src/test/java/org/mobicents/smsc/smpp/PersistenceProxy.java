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

import me.prettyprint.cassandra.serializers.ByteBufferSerializer;
import me.prettyprint.cassandra.serializers.CompositeSerializer;
import me.prettyprint.cassandra.serializers.IntegerSerializer;
import me.prettyprint.cassandra.serializers.StringSerializer;
import me.prettyprint.cassandra.service.CassandraHostConfigurator;
import me.prettyprint.hector.api.Cluster;
import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.beans.Composite;
import me.prettyprint.hector.api.beans.HColumn;
import me.prettyprint.hector.api.factory.HFactory;
import me.prettyprint.hector.api.query.ColumnQuery;
import me.prettyprint.hector.api.query.QueryResult;

/**
 * 
 * @author sergey vetyutnev
 * 
 */
public class PersistenceProxy {

    private Keyspace keyspace;

    public void setKeyspace(Keyspace val) {
        this.keyspace = val;
    }

    public Keyspace getKeyspace() {
        return this.keyspace;
    }

    public boolean testCassandraAccess() {
        Cluster cluster = HFactory.getOrCreateCluster("TestCluster", new CassandraHostConfigurator("localhost:9160"));
        Keyspace keyspace = HFactory.createKeyspace("TelestaxSMSC", cluster);

        try {
            ColumnQuery<Integer, Composite, ByteBuffer> query = HFactory.createColumnQuery(keyspace, IntegerSerializer.get(), CompositeSerializer.get(), ByteBufferSerializer.get());
            query.setColumnFamily(Schema.FAMILY_SMS_ROUTING_RULE);
            Composite coKey3 = new Composite();
//            coKey3.addComponent(Schema.COLUMN_ADDR_DST_TON, StringSerializer.get());
            coKey3.addComponent(Schema.COLUMN_ADDRESS, StringSerializer.get());
            query.setName(coKey3);
            query.setKey(111);

            QueryResult<HColumn<Composite,ByteBuffer>> result = query.execute();

            this.setKeyspace(keyspace);

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public void updateDbSmsRoutingRule(DbSmsRoutingRule dbSmsRoutingRule) throws PersistenceException {
        DBOperations.updateDbSmsRoutingRule(keyspace, dbSmsRoutingRule);
    }

    public void deleteDbSmsRoutingRule(String address) throws PersistenceException {
        DBOperations.deleteDbSmsRoutingRule(keyspace, address);
    }

    public DbSmsRoutingRule getSmsRoutingRule(final String address) throws PersistenceException {
        return DBOperations.getSmsRoutingRule(keyspace, address);
    }

    public List<DbSmsRoutingRule> getSmsRoutingRulesRange() throws PersistenceException {
        return DBOperations.getSmsRoutingRulesRange(keyspace);
    }

    public List<DbSmsRoutingRule> getSmsRoutingRulesRange(String lastAdress) throws PersistenceException {
        return DBOperations.getSmsRoutingRulesRange(keyspace, lastAdress);
    }

}
