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
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.slee.facilities.Tracer;

import org.apache.log4j.Logger;
import org.mobicents.protocols.ss7.map.api.primitives.IMSI;
import org.mobicents.protocols.ss7.map.api.service.sms.LocationInfoWithLMSI;
import org.mobicents.smsc.cassandra.DBOperations_C2;
import org.mobicents.smsc.cassandra.ErrorCode;
import org.mobicents.smsc.cassandra.PersistenceException;
import org.mobicents.smsc.cassandra.PreparedStatementCollection_C3;
import org.mobicents.smsc.cassandra.Schema;
import org.mobicents.smsc.cassandra.SmType;
import org.mobicents.smsc.cassandra.Sms;
import org.mobicents.smsc.cassandra.SmsSet;
import org.mobicents.smsc.cassandra.SmsSetCashe;
import org.mobicents.smsc.cassandra.TargetAddress;

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
public class TT_PersistenceRAInterfaceProxy extends DBOperations_C2 implements PersistenceRAInterface {

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

    public SmsProxy obtainArchiveSms(long dueSlot, String dstDigits, UUID dbId) throws PersistenceException, IOException {

        PreparedStatement ps = session.prepare("select * from \"" + Schema.FAMILY_MESSAGES + this.getTableName(dueSlot) + "\" where \""
                + Schema.COLUMN_ADDR_DST_DIGITS + "\"=? and \"" + Schema.COLUMN_ID + "\"=?;");
        BoundStatement boundStatement = new BoundStatement(ps);
        boundStatement.bind(dstDigits, dbId);
        ResultSet result = session.execute(boundStatement);

        Row row = result.one();
        SmsSet smsSet = createSms(row, null);
        if (smsSet == null)
            return null;

        SmsProxy res = new SmsProxy();
        res.sms = smsSet.getSms(0);

        res.addrDstDigits = row.getString(Schema.COLUMN_ADDR_DST_DIGITS);
        res.addrDstTon = row.getInt(Schema.COLUMN_ADDR_DST_TON);
        res.addrDstNpi = row.getInt(Schema.COLUMN_ADDR_DST_NPI);

        res.destClusterName = row.getString(Schema.COLUMN_DEST_CLUSTER_NAME);
        res.destEsmeName = row.getString(Schema.COLUMN_DEST_ESME_NAME);
        res.destSystemId = row.getString(Schema.COLUMN_DEST_SYSTEM_ID);

        res.imsi = row.getString(Schema.COLUMN_IMSI);
        res.nnnDigits = row.getString(Schema.COLUMN_NNN_DIGITS);
        res.smStatus = row.getInt(Schema.COLUMN_SM_STATUS);
        res.smType = row.getInt(Schema.COLUMN_SM_TYPE);
        res.deliveryCount = row.getInt(Schema.COLUMN_DELIVERY_COUNT);

        res.deliveryDate = row.getDate(Schema.COLUMN_DELIVERY_DATE);

        return res;
    }

    public PreparedStatementCollection_C3 getStatementCollection(Date dt) throws PersistenceException {
        return super.getStatementCollection(dt);
    }

    public TargetAddress obtainSynchroObject(TargetAddress ta) {
        return SmsSetCashe.getInstance().addSmsSet(ta);
    }

    public void releaseSynchroObject(TargetAddress ta) {
        SmsSetCashe.getInstance().removeSmsSet(ta);
    }

    public int checkSmsExists(long dueSlot, String targetId) throws PersistenceException {
        try {
            String s1 = "select \"ID\" from \"SLOT_MESSAGES_TABLE" + this.getTableName(dueSlot) + "\" where \"DUE_SLOT\"=? and \"TARGET_ID\"=?;";
            PreparedStatement ps = session.prepare(s1);
            BoundStatement boundStatement = new BoundStatement(ps);
            boundStatement.bind(dueSlot, targetId);
            ResultSet rs = session.execute(boundStatement);

            return rs.all().size();
        } catch (Exception e) {
            int ggg = 0;
            ggg = 0;
            return -1;
        }
    }

    public Sms obtainLiveSms(long dueSlot, String targetId, UUID id) throws PersistenceException {
        try {
            String s1 = "select * from \"SLOT_MESSAGES_TABLE" + this.getTableName(dueSlot) + "\" where \"DUE_SLOT\"=? and \"TARGET_ID\"=? and \"ID\"=?;";
            PreparedStatement ps = session.prepare(s1);
            BoundStatement boundStatement = new BoundStatement(ps);
            boundStatement.bind(dueSlot, targetId, id);
            ResultSet rs = session.execute(boundStatement);

            SmsSet smsSet = null;
            for (Row row : rs) {
                smsSet = this.createSms(row, null);
            }
            if (smsSet == null || smsSet.getSmsCount() == 0)
                return null;
            else
                return smsSet.getSms(0);

        } catch (Exception e) {
            int ggg = 0;
            ggg = 0;
            return null;
        }
    }

//    @Override
//    public boolean checkSmsSetExists(TargetAddress ta) throws PersistenceException {
//
//        // TODO Auto-generated method stub
//        return false;
//    }

    @Override
    public SmsSet obtainSmsSet(TargetAddress ta) throws PersistenceException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setNewMessageScheduled(SmsSet smsSet, Date newDueDate) throws PersistenceException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setDeliveringProcessScheduled(SmsSet smsSet, Date newDueDate, int newDueDelay) throws PersistenceException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setDestination(SmsSet smsSet, String destClusterName, String destSystemId, String destEsmeId, SmType type) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setRoutingInfo(SmsSet smsSet, IMSI imsi, LocationInfoWithLMSI locationInfoWithLMSI) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setDeliveryStart(SmsSet smsSet, Date inSystemDate) throws PersistenceException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setDeliveryStart(Sms sms) throws PersistenceException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setDeliverySuccess(SmsSet smsSet, Date lastDelivery) throws PersistenceException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setDeliveryFailure(SmsSet smsSet, ErrorCode smStatus, Date lastDelivery) throws PersistenceException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setAlertingSupported(String targetId, boolean alertingSupported) throws PersistenceException {
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
    public void archiveDeliveredSms(Sms sms, Date deliveryDate) throws PersistenceException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void archiveFailuredSms(Sms sms) throws PersistenceException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public List<SmsSet> fetchSchedulableSmsSets(int maxRecordCount, Tracer tracer) throws PersistenceException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void fetchSchedulableSms(SmsSet smsSet, boolean excludeNonScheduleDeliveryTime) throws PersistenceException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public boolean checkSmsSetExists(TargetAddress ta) throws PersistenceException {
        // TODO Auto-generated method stub
        return false;
    }
}
