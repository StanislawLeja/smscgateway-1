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

package org.mobicents.smsc.tools.stresstool;

import java.io.IOException;
import java.util.Date;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.mobicents.smsc.cassandra.PersistenceException;
import org.mobicents.smsc.cassandra.Schema;
import org.mobicents.smsc.cassandra.Sms;
import org.mobicents.smsc.cassandra.SmsSet;
import org.mobicents.smsc.cassandra.SmsSetCashe;
import org.mobicents.smsc.cassandra.TargetAddress;
import org.mobicents.smsc.slee.resources.persistence.SmsProxy;

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
public class TT_PersistenceRAInterfaceProxy extends NN_DBOperations implements NN_PersistenceRAInterface {

    private static final Logger logger = Logger.getLogger(TT_PersistenceRAInterfaceProxy.class);

    String ip = "127.0.0.1";
    String keyspace = "saturn";

    public Session getSession() {
        return session;
    }

    public void start() throws Exception {
        super.start(ip, 9042, keyspace);
    }

    public boolean testCassandraAccess() {

        try {
            Cluster cluster = Cluster.builder().addContactPoint(ip).build();
            try {
                Metadata metadata = cluster.getMetadata();

                for (Host host : metadata.getAllHosts()) {
                    logger.info(String.format("Datacenter: %s; Host: %s; Rack: %s\n", host.getDatacenter(), host.getAddress(), host.getRack()));
                }

                Session session = cluster.connect();

                session.execute("USE \"" + keyspace + "\"");

                // testing if a keyspace is acceptable
                PreparedStatement ps = session.prepare("DROP TABLE \"TEST_TABLE\";");
                BoundStatement boundStatement = new BoundStatement(ps);
                boundStatement.bind();
                try {
                    session.execute(boundStatement);
                } catch (Exception e) {
                    int g1 = 0;
                    g1++;
                }                

                ps = session.prepare("CREATE TABLE \"TEST_TABLE\" ( id uuid primary key ) ;");
                boundStatement = new BoundStatement(ps);
                boundStatement.bind();
                session.execute(boundStatement);

                // deleting of current tables
                ps = session.prepare("DROP TABLE \"" + Schema.FAMILY_CURRENT_SLOT_TABLE + "\";");
                boundStatement = new BoundStatement(ps);
                boundStatement.bind();
                try {
                    session.execute(boundStatement);
                } catch (Exception e) {
                    int g1 = 0;
                    g1++;
                }                

                Date dt = new Date();
                String tName = this.getTableName(dt);

                ps = session.prepare("DROP TABLE \"" + Schema.FAMILY_DST_SLOT_TABLE +tName+ "\";");
                boundStatement = new BoundStatement(ps);
                boundStatement.bind();
                try {
                    session.execute(boundStatement);
                } catch (Exception e) {
                    int g1 = 0;
                    g1++;
                }

                ps = session.prepare("DROP TABLE \"" + Schema.FAMILY_SLOT_MESSAGES_TABLE +tName+ "\";");
                boundStatement = new BoundStatement(ps);
                boundStatement.bind();
                try {
                    session.execute(boundStatement);
                } catch (Exception e) {
                    int g1 = 0;
                    g1++;
                }

                ps = session.prepare("DROP TABLE \"" + Schema.FAMILY_MESSAGES +tName+ "\";");
                boundStatement = new BoundStatement(ps);
                boundStatement.bind();
                try {
                    session.execute(boundStatement);
                } catch (Exception e) {
                    int g1 = 0;
                    g1++;
                }

                return true;
            } finally {
                cluster.shutdown();
            }
        } catch (Exception e) {
            return false;
        }
    }

//    public SmsProxy obtainArchiveSms(UUID dbId) throws PersistenceException, IOException {
//
//        PreparedStatement ps = session.prepare("select * from \"" + Schema.FAMILY_ARCHIVE + "\" where \"" + Schema.COLUMN_ID + "\"=?;");
//        BoundStatement boundStatement = new BoundStatement(ps);
//        boundStatement.bind(dbId);
//        ResultSet result = session.execute(boundStatement);
//
//        Row row = result.one();
//        Sms sms = createSms(row, new SmsSet(), dbId);
//        if (sms == null)
//            return null;
//
//        SmsProxy res = new SmsProxy();
//        res.sms = sms;
//
//        res.addrDstDigits = row.getString(Schema.COLUMN_ADDR_DST_DIGITS);
//        res.addrDstTon = row.getInt(Schema.COLUMN_ADDR_DST_TON);
//        res.addrDstNpi = row.getInt(Schema.COLUMN_ADDR_DST_NPI);
//
//        res.destClusterName = row.getString(Schema.COLUMN_DEST_CLUSTER_NAME);
//        res.destEsmeName = row.getString(Schema.COLUMN_DEST_ESME_NAME);
//        res.destSystemId = row.getString(Schema.COLUMN_DEST_SYSTEM_ID);
//
//        res.imsi = row.getString(Schema.COLUMN_IMSI);
//        res.nnnDigits = row.getString(Schema.COLUMN_NNN_DIGITS);
//        res.smStatus = row.getInt(Schema.COLUMN_SM_STATUS);
//        res.smType = row.getInt(Schema.COLUMN_SM_TYPE);
//        res.deliveryCount = row.getInt(Schema.COLUMN_DELIVERY_COUNT);
//
//        res.deliveryDate = row.getDate(Schema.COLUMN_DELIVERY_DATE);
//
//        return res;
//    }

    @Override
    public TargetAddress obtainSynchroObject(TargetAddress ta) {
        return SmsSetCashe.getInstance().addSmsSet(ta);
    }

    @Override
    public void releaseSynchroObject(TargetAddress ta) {
        SmsSetCashe.getInstance().removeSmsSet(ta);
    }
}
