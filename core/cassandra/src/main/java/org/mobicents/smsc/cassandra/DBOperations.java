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

package org.mobicents.smsc.cassandra;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.slee.facilities.Tracer;

import javolution.util.FastList;
import javolution.xml.XMLObjectReader;
import javolution.xml.XMLObjectWriter;
import javolution.xml.stream.XMLStreamException;

import org.apache.log4j.Logger;
import org.mobicents.protocols.ss7.map.api.primitives.IMSI;
import org.mobicents.protocols.ss7.map.api.service.sms.LocationInfoWithLMSI;

import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Cluster.Builder;
import com.datastax.driver.core.Host;
import com.datastax.driver.core.Metadata;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;

/**
 * 
 * @author baranowb
 * @author sergey vetyutnev
 * 
 */
public class DBOperations {
	private static final Logger logger = Logger.getLogger(DBOperations.class);

	public static final String TLV_SET = "tlvSet";

	protected Cluster cluster;
	protected Session session;

	private PreparedStatement smsSetExist;
	private PreparedStatement obtainSmsSet;
	private PreparedStatement obtainSmsSet2;
	private PreparedStatement setNewMessageScheduled;
	private PreparedStatement setDeliveringProcessScheduled;
	private PreparedStatement setDeliveryStart;
	private PreparedStatement setDeliveryStart2;
	private PreparedStatement setDeliverySuccess;
	private PreparedStatement setDeliveryFailure;
	private PreparedStatement setAlertingSupported;
	private PreparedStatement deleteSmsSet;
	private PreparedStatement createLiveSms;
	private PreparedStatement obtainLiveSms;
	private PreparedStatement obtainLiveSms2;
    private PreparedStatement doFetchSchedulableSmsSets;
    private PreparedStatement doFetchSchedulableSmsSets2;
    private PreparedStatement fetchSchedulableSms;
    private PreparedStatement getSmsRoutingRule;
    private PreparedStatement updateDbSmsRoutingRule;
    private PreparedStatement deleteDbSmsRoutingRule;
    private PreparedStatement getSmsRoutingRulesRange;
    private PreparedStatement getSmsRoutingRulesRange2;
    private PreparedStatement deleteLiveSms;
    private PreparedStatement doArchiveDeliveredSms;

	private static final DBOperations instance = new DBOperations();

	private volatile boolean started = false;

	protected DBOperations() {
		super();
	}

	public static DBOperations getInstance() {
		return instance;
	}
	
	public boolean isStarted() {
		return started;
	}

    protected Session getSession() {
        return this.session;
    }

    public void start(String ip, int port, String keyspace) throws Exception {
		if (this.started) {
			throw new Exception("DBOperations already started");
		}

		Builder builder = Cluster.builder();

		builder.withPort(port);
		builder.addContactPoint(ip);

		this.cluster = builder.build();
		Metadata metadata = cluster.getMetadata();

		logger.info(String.format("Connected to cluster: %s\n", metadata.getClusterName()));
		for (Host host : metadata.getAllHosts()) {
			logger.info(String.format("Datacenter: %s; Host: %s; Rack: %s\n", host.getDatacenter(), host.getAddress(),
					host.getRack()));
		}

        session = cluster.connect();

        session.execute("USE \"" + keyspace + "\"");

        smsSetExist = session.prepare("SELECT count(*) FROM \"LIVE\" WHERE \"TARGET_ID\"=?;");
        obtainSmsSet = session.prepare("select * from \"" + Schema.FAMILY_LIVE + "\" where \"" + Schema.COLUMN_TARGET_ID + "\"=?;");
        obtainSmsSet2 = session.prepare("INSERT INTO \"" + Schema.FAMILY_LIVE + "\" (\"" + Schema.COLUMN_TARGET_ID + "\", \"" + Schema.COLUMN_ADDR_DST_DIGITS
                + "\", \"" + Schema.COLUMN_ADDR_DST_TON + "\", \"" + Schema.COLUMN_ADDR_DST_NPI + "\", \"" + Schema.COLUMN_IN_SYSTEM
                + "\") VALUES (?, ?, ?, ?, ?);");
        setNewMessageScheduled = session.prepare("INSERT INTO \"" + Schema.FAMILY_LIVE + "\" (\"" + Schema.COLUMN_TARGET_ID + "\", \"" + Schema.COLUMN_DUE_DATE
                + "\", \"" + Schema.COLUMN_IN_SYSTEM + "\", \"" + Schema.COLUMN_DUE_DELAY + "\") VALUES (?, ?, ?, ?);");
        setDeliveringProcessScheduled = session.prepare("INSERT INTO \"" + Schema.FAMILY_LIVE + "\" (\"" + Schema.COLUMN_TARGET_ID + "\", \""
                + Schema.COLUMN_DUE_DATE + "\", \"" + Schema.COLUMN_IN_SYSTEM + "\", \"" + Schema.COLUMN_DUE_DELAY + "\") VALUES (?, ?, ?, ?);");
        setDeliveryStart = session.prepare("INSERT INTO \"" + Schema.FAMILY_LIVE + "\" (\"" + Schema.COLUMN_TARGET_ID + "\", \"" + Schema.COLUMN_IN_SYSTEM
                + "\", \"" + Schema.COLUMN_IN_SYSTEM_DATE + "\") VALUES (?, ?, ?);");
        setDeliveryStart2 = session.prepare("INSERT INTO \"" + Schema.FAMILY_LIVE_SMS + "\" (\"" + Schema.COLUMN_ID + "\", \"" + Schema.COLUMN_DELIVERY_COUNT
                + "\") VALUES (?, ?);");
        setDeliverySuccess = session.prepare("INSERT INTO \"" + Schema.FAMILY_LIVE + "\" (\"" + Schema.COLUMN_TARGET_ID + "\", \"" + Schema.COLUMN_IN_SYSTEM
                + "\", \"" + Schema.COLUMN_SM_STATUS + "\") VALUES (?, ?, ?);");
        setDeliveryFailure = session.prepare("INSERT INTO \"" + Schema.FAMILY_LIVE + "\" (\"" + Schema.COLUMN_TARGET_ID + "\", \"" + Schema.COLUMN_IN_SYSTEM
                + "\", \"" + Schema.COLUMN_SM_STATUS + "\", \"" + Schema.COLUMN_ALERTING_SUPPORTED + "\") VALUES (?, ?, ?, ?);");
        setAlertingSupported = session.prepare("INSERT INTO \"" + Schema.FAMILY_LIVE + "\" (\"" + Schema.COLUMN_TARGET_ID + "\", \""
                + Schema.COLUMN_ALERTING_SUPPORTED + "\") VALUES (?, ?);");
        deleteSmsSet = session.prepare("delete from \"" + Schema.FAMILY_LIVE + "\" where \"" + Schema.COLUMN_TARGET_ID + "\"=?;");
        String s1 = getFillUpdateFields();
        String s2 = getFillUpdateFields2();
        createLiveSms = session.prepare("INSERT INTO \"" + Schema.FAMILY_LIVE_SMS + "\" (\"" + Schema.COLUMN_TARGET_ID + "\", " + s1 + ") VALUES (? " + s2
                + ");");
        obtainLiveSms = session.prepare("select * from \"" + Schema.FAMILY_LIVE_SMS + "\" where \"" + Schema.COLUMN_ID + "\"=?;");
        obtainLiveSms2 = session.prepare("select * from \"" + Schema.FAMILY_LIVE_SMS + "\" where \"" + Schema.COLUMN_MESSAGE_ID + "\"=?;");
        doFetchSchedulableSmsSets = session.prepare("select * from \"" + Schema.FAMILY_LIVE + "\" where \"" + Schema.COLUMN_IN_SYSTEM + "\"=? and \""
                + Schema.COLUMN_IN_SYSTEM_DATE + "\"<=?  ALLOW FILTERING;");
        doFetchSchedulableSmsSets2 = session.prepare("select * from \"" + Schema.FAMILY_LIVE + "\" where \"" + Schema.COLUMN_IN_SYSTEM + "\"=? and \""
                + Schema.COLUMN_DUE_DATE + "\"<=?  ALLOW FILTERING;");
        fetchSchedulableSms = session.prepare("select * from \"" + Schema.FAMILY_LIVE_SMS + "\" where \"" + Schema.COLUMN_TARGET_ID + "\"=?;");
        getSmsRoutingRule = session.prepare("select * from \"" + Schema.FAMILY_SMS_ROUTING_RULE + "\" where \"" + Schema.COLUMN_ADDRESS + "\"=?;");
        updateDbSmsRoutingRule = session.prepare("INSERT INTO \"" + Schema.FAMILY_SMS_ROUTING_RULE + "\" (\"" + Schema.COLUMN_ADDRESS + "\", \""
                + Schema.COLUMN_CLUSTER_NAME + "\") VALUES (?, ?);");
        deleteDbSmsRoutingRule = session.prepare("delete from \"" + Schema.FAMILY_SMS_ROUTING_RULE + "\" where \"" + Schema.COLUMN_ADDRESS + "\"=?;");
        int row_count = 100;
        getSmsRoutingRulesRange = session.prepare("select * from \"" + Schema.FAMILY_SMS_ROUTING_RULE + "\" where token(\"" + Schema.COLUMN_ADDRESS
                + "\") >= token(?) LIMIT " + row_count + ";");
        getSmsRoutingRulesRange2 = session.prepare("select * from \"" + Schema.FAMILY_SMS_ROUTING_RULE + "\"  LIMIT " + row_count + ";");
        deleteLiveSms = session.prepare("delete from \"" + Schema.FAMILY_LIVE_SMS + "\" where \"" + Schema.COLUMN_ID + "\"=?;");

        s1 = getFillUpdateFields();
        StringBuilder sb = new StringBuilder();
        sb.append("\"");
        sb.append(Schema.COLUMN_IN_SYSTEM);
        sb.append("\", \"");
        sb.append(Schema.COLUMN_DEST_CLUSTER_NAME);
        sb.append("\", \"");
        sb.append(Schema.COLUMN_DEST_ESME_NAME);
        sb.append("\", \"");
        sb.append(Schema.COLUMN_DEST_SYSTEM_ID);
        sb.append("\", \"");
        sb.append(Schema.COLUMN_DELIVERY_DATE);
        sb.append("\", \"");
        sb.append(Schema.COLUMN_IMSI);
        sb.append("\", \"");
        sb.append(Schema.COLUMN_NNN_DIGITS);
        sb.append("\", \"");
        sb.append(Schema.COLUMN_NNN_AN);
        sb.append("\", \"");
        sb.append(Schema.COLUMN_NNN_NP);
        sb.append("\", \"");
        sb.append(Schema.COLUMN_SM_STATUS);
        sb.append("\", \"");
        sb.append(Schema.COLUMN_SM_TYPE);
        sb.append("\"");
        String s11 = sb.toString();

        s2 = getFillUpdateFields2();
        String s22 = ", ?, ?, ?, ?, ?, ?, ?, ?, ?, ?";
        doArchiveDeliveredSms = session.prepare("INSERT INTO \"" + Schema.FAMILY_ARCHIVE + "\" (" + s1 + ", " + s11 + ") VALUES (? " + s2 + s22 + ");");

		this.started = true;
	}

	public void stop() throws Exception {
        if (!this.started)
            return;

        cluster.shutdown();
        Metadata metadata = cluster.getMetadata();
        logger.info(String.format("Disconnected from cluster: %s\n", metadata.getClusterName()));

        this.started = false;
	}

	public boolean checkSmsSetExists(final TargetAddress ta) throws PersistenceException {

		try {
			BoundStatement boundStatement = new BoundStatement(smsSetExist);
			boundStatement.bind(ta.getTargetId());
			ResultSet results = session.execute(boundStatement);

			Row row = results.one();

			long count = row.getLong(0);

			return (count > 0);
		} catch (Exception e) {
			String msg = "Failed to checkSmsSetExists SMS for '" + ta.getAddr() + ",Ton=" + ta.getAddrTon() + ",Npi="
					+ ta.getAddrNpi() + "'!";
			throw new PersistenceException(msg, e);
		}
	}

	public SmsSet obtainSmsSet(final TargetAddress ta) throws PersistenceException {

        TargetAddress lock = SmsSetCashe.getInstance().addSmsSet(ta);
        try {
            synchronized (lock) {
                try {
                    BoundStatement boundStatement = new BoundStatement(obtainSmsSet);
                    boundStatement.bind(ta.getTargetId());
                    ResultSet res = session.execute(boundStatement);

                    Row row = res.one();
                    SmsSet smsSet = createSmsSet(row);

                    if (smsSet.getDestAddr() == null) {
                        smsSet = new SmsSet();

                        smsSet.setDestAddr(ta.getAddr());
                        smsSet.setDestAddrTon(ta.getAddrTon());
                        smsSet.setDestAddrNpi(ta.getAddrNpi());
                        smsSet.setInSystem(0);

                        boundStatement = new BoundStatement(obtainSmsSet2);
                        boundStatement.bind(ta.getTargetId(), ta.getAddr(), ta.getAddrTon(), ta.getAddrNpi(), 0);
                        session.execute(boundStatement);
                    }

                    return smsSet;                    

//                    SliceQuery<String, Composite, ByteBuffer> query = HFactory.createSliceQuery(keyspace, SERIALIZER_STRING, SERIALIZER_COMPOSITE,
//                            ByteBufferSerializer.get());
//                    query.setColumnFamily(Schema.FAMILY_LIVE);
//                    query.setKey(ta.getTargetId());
//
//                    query.setRange(null, null, false, 100);
//
//                    QueryResult<ColumnSlice<Composite, ByteBuffer>> result = query.execute();
//                    ColumnSlice<Composite, ByteBuffer> cSlice = result.get();
//
//                    SmsSet smsSet = createSmsSet(cSlice);
//
//                    if (smsSet.getDestAddr() == null) {
//                        smsSet = new SmsSet();
//
//                        Mutator<String> mutator = HFactory.createMutator(keyspace, SERIALIZER_STRING);
//
//                        smsSet.setDestAddr(ta.getAddr());
//                        smsSet.setDestAddrTon(ta.getAddrTon());
//                        smsSet.setDestAddrNpi(ta.getAddrNpi());
//                        smsSet.setInSystem(0);
//
//                        Composite cc = createLiveColumnComposite(ta, Schema.COLUMN_ADDR_DST_DIGITS);
//                        mutator.addInsertion(ta.getTargetId(), Schema.FAMILY_LIVE,
//                                HFactory.createColumn(cc, ta.getAddr(), SERIALIZER_COMPOSITE, SERIALIZER_STRING));
//                        cc = createLiveColumnComposite(ta, Schema.COLUMN_ADDR_DST_TON);
//                        mutator.addInsertion(ta.getTargetId(), Schema.FAMILY_LIVE,
//                                HFactory.createColumn(cc, ta.getAddrTon(), SERIALIZER_COMPOSITE, SERIALIZER_INTEGER));
//                        cc = createLiveColumnComposite(ta, Schema.COLUMN_ADDR_DST_NPI);
//                        mutator.addInsertion(ta.getTargetId(), Schema.FAMILY_LIVE,
//                                HFactory.createColumn(cc, ta.getAddrNpi(), SERIALIZER_COMPOSITE, SERIALIZER_INTEGER));
//
//                        cc = createLiveColumnComposite(ta, Schema.COLUMN_IN_SYSTEM);
//                        mutator.addInsertion(ta.getTargetId(), Schema.FAMILY_LIVE, HFactory.createColumn(cc, 0, SERIALIZER_COMPOSITE, SERIALIZER_INTEGER));
//
//                        mutator.execute();
//                    }
//
//                    return smsSet;
                } catch (Exception e) {
                    String msg = "Failed to obtainSmsSet SMS for '" + ta.getAddr() + ",Ton=" + ta.getAddrTon() + ",Npi=" + ta.getAddrNpi() + "'!";
                    throw new PersistenceException(msg, e);
                }
            }
        } finally {
            SmsSetCashe.getInstance().removeSmsSet(lock);
        }
	}

	public void setNewMessageScheduled(final SmsSet smsSet, final Date newDueDate) throws PersistenceException {

        if (smsSet.getInSystem() == 2)
            // we do not update Scheduled if it is a new message insertion and
            // target is under delivering process
            return;

        if (smsSet.getInSystem() == 1 && smsSet.getDueDate() != null && newDueDate.after(smsSet.getDueDate()))
            // we do not update Scheduled if it is already schedulered for a
            // earlier time
            return;

        try {

            BoundStatement boundStatement = new BoundStatement(setNewMessageScheduled);
            boundStatement.bind(smsSet.getTargetId(), newDueDate, 1, 0);
            session.execute(boundStatement);
            
//            Mutator<String> mutator = HFactory.createMutator(keyspace, SERIALIZER_STRING);
//
//            Composite cc = createLiveColumnComposite(smsSet, Schema.COLUMN_DUE_DATE);
//            mutator.addInsertion(smsSet.getTargetId(), Schema.FAMILY_LIVE,
//                    HFactory.createColumn(cc, newDueDate, SERIALIZER_COMPOSITE, SERIALIZER_DATE));
//            cc = createLiveColumnComposite(smsSet, Schema.COLUMN_IN_SYSTEM);
//            mutator.addInsertion(smsSet.getTargetId(), Schema.FAMILY_LIVE,
//                    HFactory.createColumn(cc, 1, SERIALIZER_COMPOSITE, SERIALIZER_INTEGER));
//            cc = createLiveColumnComposite(smsSet, Schema.COLUMN_DUE_DELAY);
//            mutator.addInsertion(smsSet.getTargetId(), Schema.FAMILY_LIVE,
//                    HFactory.createColumn(cc, 0, SERIALIZER_COMPOSITE, SERIALIZER_INTEGER));
//
//            mutator.execute();

            smsSet.setInSystem(1);
            smsSet.setDueDate(newDueDate);
            smsSet.setDueDelay(0);
        } catch (Exception e) {
            String msg = "Failed to setScheduled for '" + smsSet.getDestAddr() + ",Ton=" + smsSet.getDestAddrTon() + ",Npi="
                    + smsSet.getDestAddrNpi() + "'!";
            throw new PersistenceException(msg, e);
        }
	}

	public void setDeliveringProcessScheduled(final SmsSet smsSet, Date newDueDate, final int newDueDelay)
			throws PersistenceException {

        try {

            BoundStatement boundStatement = new BoundStatement(setDeliveringProcessScheduled);
            boundStatement.bind(smsSet.getTargetId(), newDueDate, 1, newDueDelay);
            session.execute(boundStatement);




//            Mutator<String> mutator = HFactory.createMutator(keyspace, SERIALIZER_STRING);
//
//            Composite cc = createLiveColumnComposite(smsSet, Schema.COLUMN_DUE_DATE);
//            mutator.addInsertion(smsSet.getTargetId(), Schema.FAMILY_LIVE,
//                    HFactory.createColumn(cc, newDueDate, SERIALIZER_COMPOSITE, SERIALIZER_DATE));
//            cc = createLiveColumnComposite(smsSet, Schema.COLUMN_IN_SYSTEM);
//            mutator.addInsertion(smsSet.getTargetId(), Schema.FAMILY_LIVE,
//                    HFactory.createColumn(cc, 1, SERIALIZER_COMPOSITE, SERIALIZER_INTEGER));
//            cc = createLiveColumnComposite(smsSet, Schema.COLUMN_DUE_DELAY);
//            mutator.addInsertion(smsSet.getTargetId(), Schema.FAMILY_LIVE,
//                    HFactory.createColumn(cc, newDueDelay, SERIALIZER_COMPOSITE, SERIALIZER_INTEGER));
//
//            mutator.execute();

            smsSet.setInSystem(1);
            smsSet.setDueDate(newDueDate);
            smsSet.setDueDelay(newDueDelay);
        } catch (Exception e) {
            String msg = "Failed to setScheduled for '" + smsSet.getDestAddr() + ",Ton=" + smsSet.getDestAddrTon() + ",Npi="
                    + smsSet.getDestAddrNpi() + "'!";
            throw new PersistenceException(msg, e);
        }
	}

	public void setDestination(SmsSet smsSet, String destClusterName, String destSystemId, String destEsmeId,
			SmType type) {

        smsSet.setDestClusterName(destClusterName);
        smsSet.setDestSystemId(destSystemId);
        smsSet.setDestEsmeName(destEsmeId);
        smsSet.setType(type);
	}

	public void setRoutingInfo(SmsSet smsSet, IMSI imsi, LocationInfoWithLMSI locationInfoWithLMSI) {

        smsSet.setImsi(imsi);
        smsSet.setLocationInfoWithLMSI(locationInfoWithLMSI);
	}

	public void setDeliveryStart(final SmsSet smsSet, final Date newInSystemDate) throws PersistenceException {

        try {
            BoundStatement boundStatement = new BoundStatement(setDeliveryStart);
            boundStatement.bind(smsSet.getTargetId(), 2, newInSystemDate);
            session.execute(boundStatement);            
            
            
//            Mutator<String> mutator = HFactory.createMutator(keyspace, SERIALIZER_STRING);
//
//            Composite cc = createLiveColumnComposite(smsSet, Schema.COLUMN_IN_SYSTEM);
//            mutator.addInsertion(smsSet.getTargetId(), Schema.FAMILY_LIVE,
//                    HFactory.createColumn(cc, 2, SERIALIZER_COMPOSITE, SERIALIZER_INTEGER));
//            cc = createLiveColumnComposite(smsSet, Schema.COLUMN_IN_SYSTEM_DATE);
//            mutator.addInsertion(smsSet.getTargetId(), Schema.FAMILY_LIVE,
//                    HFactory.createColumn(cc, newInSystemDate, SERIALIZER_COMPOSITE, SERIALIZER_DATE));
//
//            mutator.execute();


            smsSet.setInSystem(2);
            smsSet.setInSystemDate(newInSystemDate);
        } catch (Exception e) {
            String msg = "Failed to setDeliveryStart smsSet for '" + smsSet.getDestAddr() + ",Ton=" + smsSet.getDestAddrTon()
                    + ",Npi=" + smsSet.getDestAddrNpi() + "'!";
            throw new PersistenceException(msg, e);
        }
	}

	public void setDeliveryStart(final Sms sms) throws PersistenceException {

        try {
            BoundStatement boundStatement = new BoundStatement(setDeliveryStart2);
            boundStatement.bind(sms.getDbId(), sms.getDeliveryCount() + 1);
            session.execute(boundStatement);

//            Mutator<UUID> mutator = HFactory.createMutator(keyspace, UUIDSerializer.get());
//
//            Composite cc = new Composite();
//            cc.addComponent(Schema.COLUMN_DELIVERY_COUNT, SERIALIZER_STRING);
//            mutator.addInsertion(sms.getDbId(), Schema.FAMILY_LIVE_SMS,
//                    HFactory.createColumn(cc, sms.getDeliveryCount() + 1, SERIALIZER_COMPOSITE, SERIALIZER_INTEGER));
//
//            mutator.execute();

            sms.setDeliveryCount(sms.getDeliveryCount() + 1);
        } catch (Exception e) {
            String msg = "Failed to setDeliveryStart sms for '" + sms.getDbId() + "'!";
            throw new PersistenceException(msg, e);
        }
	}

	public void setDeliverySuccess(final SmsSet smsSet, final Date lastDelivery) throws PersistenceException {
        try {
            BoundStatement boundStatement = new BoundStatement(setDeliverySuccess);
            boundStatement.bind(smsSet.getTargetId(), 0, 0);
            session.execute(boundStatement);


//            Mutator<String> mutator = HFactory.createMutator(keyspace, SERIALIZER_STRING);
//
//            Composite cc = createLiveColumnComposite(smsSet, Schema.COLUMN_IN_SYSTEM);
//            mutator.addInsertion(smsSet.getTargetId(), Schema.FAMILY_LIVE,
//                    HFactory.createColumn(cc, 0, SERIALIZER_COMPOSITE, SERIALIZER_INTEGER));
//            cc = createLiveColumnComposite(smsSet, Schema.COLUMN_SM_STATUS);
//            mutator.addInsertion(smsSet.getTargetId(), Schema.FAMILY_LIVE,
//                    HFactory.createColumn(cc, 0, SERIALIZER_COMPOSITE, SERIALIZER_INTEGER));
//            cc = createLiveColumnComposite(smsSet, Schema.COLUMN_LAST_DELIVERY);
//            mutator.addInsertion(smsSet.getTargetId(), Schema.FAMILY_LIVE,
//                    HFactory.createColumn(cc, lastDelivery, SERIALIZER_COMPOSITE, SERIALIZER_DATE));
//
//            mutator.execute();

            smsSet.setInSystem(0);
            smsSet.setStatus(ErrorCode.SUCCESS);
            smsSet.setLastDelivery(lastDelivery);
        } catch (Exception e) {
            String msg = "Failed to setDeliverySuccess for '" + smsSet.getDestAddr() + ",Ton=" + smsSet.getDestAddrTon()
                    + ",Npi=" + smsSet.getDestAddrNpi() + "'!";
            throw new PersistenceException(msg, e);
        }
	}

	public void setDeliveryFailure(final SmsSet smsSet, final ErrorCode smStatus, final Date lastDelivery)
			throws PersistenceException {

        try {
            BoundStatement boundStatement = new BoundStatement(setDeliveryFailure);
            boundStatement.bind(smsSet.getTargetId(), 0, smStatus.getCode(), false);
            session.execute(boundStatement);

//            Mutator<String> mutator = HFactory.createMutator(keyspace, SERIALIZER_STRING);
//
//            Composite cc = createLiveColumnComposite(smsSet, Schema.COLUMN_IN_SYSTEM);
//            mutator.addInsertion(smsSet.getTargetId(), Schema.FAMILY_LIVE,
//                    HFactory.createColumn(cc, 0, SERIALIZER_COMPOSITE, SERIALIZER_INTEGER));
//            cc = createLiveColumnComposite(smsSet, Schema.COLUMN_SM_STATUS);
//            mutator.addInsertion(smsSet.getTargetId(), Schema.FAMILY_LIVE,
//                    HFactory.createColumn(cc, smStatus.getCode(), SERIALIZER_COMPOSITE, SERIALIZER_INTEGER));
//            cc = createLiveColumnComposite(smsSet, Schema.COLUMN_LAST_DELIVERY);
//            mutator.addInsertion(smsSet.getTargetId(), Schema.FAMILY_LIVE,
//                    HFactory.createColumn(cc, lastDelivery, SERIALIZER_COMPOSITE, SERIALIZER_DATE));
//            cc = createLiveColumnComposite(smsSet, Schema.COLUMN_ALERTING_SUPPORTED);
//            mutator.addInsertion(smsSet.getTargetId(), Schema.FAMILY_LIVE,
//                    HFactory.createColumn(cc, false, SERIALIZER_COMPOSITE, SERIALIZER_BOOLEAN));
//
//            mutator.execute();

            smsSet.setInSystem(0);
            smsSet.setStatus(smStatus);
            smsSet.setLastDelivery(lastDelivery);
        } catch (Exception e) {
            String msg = "Failed to setDeliverySuccess for '" + smsSet.getDestAddr() + ",Ton=" + smsSet.getDestAddrTon()
                    + ",Npi=" + smsSet.getDestAddrNpi() + "'!";
            throw new PersistenceException(msg, e);
        }
	}

	public void setAlertingSupported(final String targetId, final boolean alertingSupported)
			throws PersistenceException {

        try {
            BoundStatement boundStatement = new BoundStatement(setAlertingSupported);
            boundStatement.bind(targetId, alertingSupported);
            session.execute(boundStatement);


//            Mutator<String> mutator = HFactory.createMutator(keyspace, SERIALIZER_STRING);
//
//            Composite cc = new Composite();
//            // cc.addComponent(ta.getAddrTon(), SERIALIZER_INTEGER);
//            // cc.addComponent(ta.getAddrNpi(), SERIALIZER_INTEGER);
//            cc.addComponent(Schema.COLUMN_ALERTING_SUPPORTED, SERIALIZER_STRING);
//            mutator.addInsertion(targetId, Schema.FAMILY_LIVE,
//                    HFactory.createColumn(cc, alertingSupported, SERIALIZER_COMPOSITE, SERIALIZER_BOOLEAN));
//
//            mutator.execute();
        } catch (Exception e) {
            String msg = "Failed to setAlertingSupported for '" + targetId + "'!";
            throw new PersistenceException(msg, e);
        }
	}

	public boolean deleteSmsSet(final SmsSet smsSet) throws PersistenceException {
        TargetAddress lock = SmsSetCashe.getInstance().addSmsSet(new TargetAddress(smsSet));
        try {
            synchronized (lock) {

                // firstly we are looking for corresponded records in LIVE_SMS
                // table
                smsSet.clearSmsList();
                fetchSchedulableSms(smsSet, false);
                if (smsSet.getSmsCount() > 0) {
                    // there are corresponded records in LIVE_SMS table - we
                    // will not delete LIVE record
                    return false;
                }

                try {
                    BoundStatement boundStatement = new BoundStatement(deleteSmsSet);
                    boundStatement.bind(smsSet.getTargetId());
                    session.execute(boundStatement);


//                    Mutator<String> mutator = HFactory.createMutator(keyspace, SERIALIZER_STRING);
//
//                    mutator.addDeletion(smsSet.getTargetId(), Schema.FAMILY_LIVE);
//                    mutator.execute();
                } catch (Exception e) {
                    String msg = "Failed to deleteSmsSet for '" + smsSet.getDestAddr() + ",Ton=" + smsSet.getDestAddrTon()
                            + ",Npi=" + smsSet.getDestAddrNpi() + "'!";
                    throw new PersistenceException(msg, e);
                }

                return true;
            }
        } finally {
            SmsSetCashe.getInstance().removeSmsSet(lock);
        }
	}

	public void createLiveSms(final Sms sms) throws PersistenceException {
        try {
            BoundStatement boundStatement = new BoundStatement(createLiveSms);

            boundStatement.setString(Schema.COLUMN_TARGET_ID, sms.getSmsSet().getTargetId());
            this.FillUpdateFields(sms, boundStatement, Schema.FAMILY_LIVE_SMS);

            session.execute(boundStatement);            

//            Mutator<UUID> mutator = HFactory.createMutator(keyspace, UUIDSerializer.get());
//
//            Composite cc = new Composite();
//            cc.addComponent(Schema.COLUMN_TARGET_ID, SERIALIZER_STRING);
//            mutator.addInsertion(sms.getDbId(), Schema.FAMILY_LIVE_SMS,
//                    HFactory.createColumn(cc, sms.getSmsSet().getTargetId(), SERIALIZER_COMPOSITE, StringSerializer.get()));
//
//            FillUpdateFields(sms, mutator, Schema.FAMILY_LIVE_SMS);
//
//            mutator.execute();
        } catch (Exception e) {
            String msg = "Failed to createLiveSms SMS for '" + sms.getDbId() + "'!";

            throw new PersistenceException(msg, e);
        }
	}

	public Sms obtainLiveSms(final UUID dbId) throws PersistenceException {
        try {
            BoundStatement boundStatement = new BoundStatement(obtainLiveSms);
            boundStatement.bind(dbId);
            ResultSet res = session.execute(boundStatement);

            Row row = res.one();
            try {
                return createSms(row, null, dbId);
            } catch (Exception e) {
                throw new PersistenceException("Failed to deserialize SMS at key '" + dbId + "'!", e);
            }            

//            SliceQuery<UUID, Composite, ByteBuffer> query = HFactory.createSliceQuery(keyspace, UUIDSerializer.get(),
//                    SERIALIZER_COMPOSITE, ByteBufferSerializer.get());
//            query.setColumnFamily(Schema.FAMILY_LIVE_SMS);
//            query.setRange(null, null, false, 100);
//            Composite cc = new Composite();
//            cc.addComponent(Schema.COLUMN_ID, SERIALIZER_STRING);
//            query.setKey(dbId);
//
//            QueryResult<ColumnSlice<Composite, ByteBuffer>> result = query.execute();
//            ColumnSlice<Composite, ByteBuffer> cSlice = result.get();
//            try {
//                return createSms(keyspace,cSlice, dbId, null);
//            } catch (Exception e) {
//                throw new PersistenceException("Failed to deserialize SMS at key '" + dbId + "'!", e);
//            }
        } catch (Exception e) {
            String msg = "Failed to obtainLiveSms SMS for '" + dbId + "'!";

            throw new PersistenceException(msg, e);
        }
	}

	public Sms obtainLiveSms(final long messageId) throws PersistenceException {

        try {
            BoundStatement boundStatement = new BoundStatement(obtainLiveSms2);
            boundStatement.bind(messageId);
            ResultSet res = session.execute(boundStatement);

            Row row = res.one();
            try {
                UUID key = row.getUUID(Schema.COLUMN_ID);
                return createSms(row, null, key);
            } catch (Exception e) {
                throw new PersistenceException("Failed to deserialize SMS at key '" + messageId + "'!", e);
            }            

            
            
//            IndexedSlicesQuery<UUID, Composite, ByteBuffer> query = HFactory.createIndexedSlicesQuery(keyspace,
//                    UUIDSerializer.get(), SERIALIZER_COMPOSITE, ByteBufferSerializer.get());
//            query.setColumnFamily(Schema.FAMILY_LIVE_SMS);
//            query.setRange(null, null, false, 100);
//            Composite cc = new Composite();
//            cc.addComponent(Schema.COLUMN_MESSAGE_ID, SERIALIZER_STRING);
//            query.addEqualsExpression(cc, LongSerializer.get().toByteBuffer(messageId));
//
//            final QueryResult<OrderedRows<UUID, Composite, ByteBuffer>> result = query.execute();
//            final OrderedRows<UUID, Composite, ByteBuffer> rows = result.get();
//            final List<Row<UUID, Composite, ByteBuffer>> rowsList = rows.getList();
//            for (Row<UUID, Composite, ByteBuffer> row : rowsList) {
//                try {
//                    return createSms(keyspace,row.getColumnSlice(), row.getKey(), null);
//                } catch (Exception e) {
//                    throw new PersistenceException("Failed to deserialize SMS at key '" + row.getKey() + "'!", e);
//                }
//            }
        } catch (Exception e) {
            String msg = "Failed to obtainLiveSms SMS for '" + messageId + "'!";

            throw new PersistenceException(msg, e);
        }
	}

	public void updateLiveSms(Sms sms) throws PersistenceException {
        // TODO: implement it
        // .....................................
	}

	public void archiveDeliveredSms(final Sms sms, Date deliveryDate) throws PersistenceException {
        deleteLiveSms(sms);
        sms.setDeliveryDate(deliveryDate);
        sms.getSmsSet().setStatus(ErrorCode.SUCCESS);
        doArchiveDeliveredSms(sms);
	}

	public void archiveFailuredSms(final Sms sms) throws PersistenceException {
        deleteLiveSms(sms);
        sms.setDeliveryDate(sms.getSmsSet().getLastDelivery());
        doArchiveDeliveredSms(sms);
	}

	public List<SmsSet> fetchSchedulableSmsSets(final int maxRecordCount, Tracer tracer) throws PersistenceException {
        try {
            List<SmsSet> lst = new ArrayList<SmsSet>();

            doFetchSchedulableSmsSets(maxRecordCount, lst, 1);
            if (tracer != null) {
                for (SmsSet smsSet : lst) {
                    tracer.severe("SmsSet was scheduled with inSystem==2 and InSystemDate+30min > Now: " + smsSet);
                }
            }

            doFetchSchedulableSmsSets(maxRecordCount - lst.size(), lst, 2);

            return lst;
        } catch (Exception e) {
            String msg = "Failed to fetchSchedulableSmsSets!";

            throw new PersistenceException(msg, e);
        }
	}

	private void doFetchSchedulableSmsSets(final int maxRecordCount, List<SmsSet> lst, int opt)
			throws PersistenceException {

        if (maxRecordCount <= 0)
            return;

        PreparedStatement ps;
        Date date;
        int inSyst;
        if (opt == 1) {
            ps = doFetchSchedulableSmsSets;
            inSyst = 2;
            date = new Date((new Date()).getTime() - 30 * 60 * 1000);
        } else {
            ps = doFetchSchedulableSmsSets2;
            inSyst = 1;
            date = new Date();
        }
        BoundStatement boundStatement = new BoundStatement(ps);
        boundStatement.bind(inSyst, date);
        ResultSet res = session.execute(boundStatement);

        for (Row row : res) {
            String s = "???";
            try {
                s = row.getString(Schema.COLUMN_TARGET_ID);
                SmsSet smsSet = createSmsSet(row);
                lst.add(smsSet);
            } catch (Exception e) {
                throw new PersistenceException("Failed to deserialize SMS at key '" + s + "!", e);
            }
        }

//        IndexedSlicesQuery<String, Composite, ByteBuffer> query = HFactory.createIndexedSlicesQuery(keyspace, StringSerializer.get(), SERIALIZER_COMPOSITE,
//                ByteBufferSerializer.get());
//        query.setColumnFamily(Schema.FAMILY_LIVE);
//        query.setRange(null, null, false, 100);
//        Composite cc = new Composite();
//        if (opt == 1) {
//            cc.addComponent(Schema.COLUMN_IN_SYSTEM, SERIALIZER_STRING);
//            query.addEqualsExpression(cc, IntegerSerializer.get().toByteBuffer(2));
//            cc = new Composite();
//            // we are using 30 min interval for badly processed messages with
//            // in_system==2 after failure delivering
//            Date date = new Date((new Date()).getTime() - 30 * 60 * 1000);
//            cc.addComponent(Schema.COLUMN_IN_SYSTEM_DATE, SERIALIZER_STRING);
//            query.addLteExpression(cc, DateSerializer.get().toByteBuffer(date));
//        } else {
//            cc.addComponent(Schema.COLUMN_IN_SYSTEM, SERIALIZER_STRING);
//            query.addEqualsExpression(cc, IntegerSerializer.get().toByteBuffer(1));
//            cc = new Composite();
//            Date date = new Date();
//            cc.addComponent(Schema.COLUMN_DUE_DATE, SERIALIZER_STRING);
//            query.addLteExpression(cc, DateSerializer.get().toByteBuffer(date));
//        }
////        query.setRowCount(maxRecordCount);
//
//        final QueryResult<OrderedRows<String, Composite, ByteBuffer>> result = query.execute();
//        final OrderedRows<String, Composite, ByteBuffer> rows = result.get();
//        final List<Row<String, Composite, ByteBuffer>> rowsList = rows.getList();
//        for (Row<String, Composite, ByteBuffer> row : rowsList) {
//            try {
//                SmsSet smsSet = createSmsSet(row.getColumnSlice());
//                lst.add(smsSet);
//            } catch (Exception e) {
//                throw new PersistenceException("Failed to deserialize SMS at key '" + row.getKey() + "!", e);
//            }
//        }
	}

	public void fetchSchedulableSms(final SmsSet smsSet, boolean excludeNonScheduleDeliveryTime)
			throws PersistenceException {

	    try {
            BoundStatement boundStatement = new BoundStatement(fetchSchedulableSms);
            boundStatement.bind(smsSet.getTargetId());
            ResultSet res = session.execute(boundStatement);
            smsSet.clearSmsList();
            Date curDate = new Date();
            for (Row row : res) {
                try {
                    UUID key = row.getUUID(Schema.COLUMN_ID);
                    Sms sms = createSms(row, smsSet, key);
                    if (excludeNonScheduleDeliveryTime == false || sms.getScheduleDeliveryTime() == null || sms.getScheduleDeliveryTime().before(curDate)) {
                        smsSet.addSms(sms);
                    }
                } catch (Exception e) {
                    String msg = "Failed to deserialize SMS at key '" + row.getUUID(0) + "'!";

                throw new PersistenceException(msg, e);
                }
            }
            smsSet.resortSms();




//	        IndexedSlicesQuery<UUID, Composite, ByteBuffer> query = HFactory.createIndexedSlicesQuery(keyspace,
//                    UUIDSerializer.get(), SERIALIZER_COMPOSITE, ByteBufferSerializer.get());
//            query.setColumnFamily(Schema.FAMILY_LIVE_SMS);
//            query.setRange(null, null, false, 100);
//            Composite cc = new Composite();
//            cc.addComponent(Schema.COLUMN_TARGET_ID, SERIALIZER_STRING);
//            query.addEqualsExpression(cc, StringSerializer.get().toByteBuffer(smsSet.getTargetId()));
//
//            final QueryResult<OrderedRows<UUID, Composite, ByteBuffer>> result = query.execute();
//            final OrderedRows<UUID, Composite, ByteBuffer> rows = result.get();
//            final List<Row<UUID, Composite, ByteBuffer>> rowsList = rows.getList();
//            smsSet.clearSmsList();
//            Date curDate = new Date();
//            for (Row<UUID, Composite, ByteBuffer> row : rowsList) {
//                try {
//                    Sms sms = createSms(keyspace, row.getColumnSlice(), row.getKey(), smsSet);
//                    if (excludeNonScheduleDeliveryTime == false || sms.getScheduleDeliveryTime() == null || sms.getScheduleDeliveryTime().before(curDate)) {
//                        smsSet.addSms(sms);
//                    }
//                } catch (Exception e) {
//                    String msg = "Failed to deserialize SMS at key '" + row.getKey() + "'!";
//
//                throw new PersistenceException(msg, e);
//                }
//            }
//            smsSet.resortSms();
        } catch (Exception e) {
            String msg = "Failed to fetchSchedulableSms SMS for '" + smsSet.getTargetId() + "'!";

            throw new PersistenceException(msg, e);
        }
	}

//    private static Composite createLiveColumnComposite(TargetAddress ta, String colName) {
//        Composite cc;
//        cc = new Composite();
//        cc.addComponent(colName, SERIALIZER_STRING);
//        return cc;
//    }
//
//    private static Composite createLiveColumnComposite(SmsSet smsSet, String colName) {
//        Composite cc;
//        cc = new Composite();
//        cc.addComponent(colName, SERIALIZER_STRING);
//        return cc;
//    }

    private String getFillUpdateFields() {
        StringBuilder sb = new StringBuilder();

        sb.append("\"");
        sb.append(Schema.COLUMN_ID);
        sb.append("\", \"");
        sb.append(Schema.COLUMN_ADDR_DST_DIGITS);
        sb.append("\", \"");
        sb.append(Schema.COLUMN_ADDR_DST_TON);
        sb.append("\", \"");
        sb.append(Schema.COLUMN_ADDR_DST_NPI);
        sb.append("\", \"");
        sb.append(Schema.COLUMN_ADDR_SRC_DIGITS);
        sb.append("\", \"");
        sb.append(Schema.COLUMN_ADDR_SRC_TON);
        sb.append("\", \"");
        sb.append(Schema.COLUMN_ADDR_SRC_NPI);
        sb.append("\", \"");
        sb.append(Schema.COLUMN_MESSAGE_ID);
        sb.append("\", \"");
        sb.append(Schema.COLUMN_MO_MESSAGE_REF);
        sb.append("\", \"");
        sb.append(Schema.COLUMN_ORIG_ESME_NAME);
        sb.append("\", \"");
        sb.append(Schema.COLUMN_ORIG_SYSTEM_ID);
        sb.append("\", \"");
        sb.append(Schema.COLUMN_SUBMIT_DATE);
        sb.append("\", \"");
        sb.append(Schema.COLUMN_SERVICE_TYPE);
        sb.append("\", \"");
        sb.append(Schema.COLUMN_ESM_CLASS);
        sb.append("\", \"");
        sb.append(Schema.COLUMN_PROTOCOL_ID);
        sb.append("\", \"");
        sb.append(Schema.COLUMN_PRIORITY);
        sb.append("\", \"");
        sb.append(Schema.COLUMN_REGISTERED_DELIVERY);
        sb.append("\", \"");
        sb.append(Schema.COLUMN_REPLACE);
        sb.append("\", \"");
        sb.append(Schema.COLUMN_DATA_CODING);
        sb.append("\", \"");
        sb.append(Schema.COLUMN_DEFAULT_MSG_ID);
        sb.append("\", \"");
        sb.append(Schema.COLUMN_MESSAGE);
        sb.append("\", \"");
        sb.append(Schema.COLUMN_SCHEDULE_DELIVERY_TIME);
        sb.append("\", \"");
        sb.append(Schema.COLUMN_VALIDITY_PERIOD);
        sb.append("\", \"");
        sb.append(Schema.COLUMN_DELIVERY_COUNT);
        sb.append("\", \"");
        sb.append(Schema.COLUMN_OPTIONAL_PARAMETERS);
        sb.append("\"");

        return sb.toString();
   }


    private String getFillUpdateFields2() {
        int cnt = 25;
        StringBuilder sb = new StringBuilder();
        for (int i1 = 0; i1 < cnt; i1++) {
            sb.append(", ?");
        }
        return sb.toString();
    }

    private void FillUpdateFields(Sms sms, BoundStatement boundStatement, String columnFamilyName) throws PersistenceException {

//        Composite cc = new Composite();
//        cc.addComponent(Schema.COLUMN_ID, SERIALIZER_STRING);
//        mutator.addInsertion(sms.getDbId(), columnFamilyName, HFactory.createColumn(cc, sms.getDbId(), SERIALIZER_COMPOSITE, UUIDSerializer.get()));        
//
//        cc = new Composite();
//        cc.addComponent(Schema.COLUMN_ADDR_DST_DIGITS, SERIALIZER_STRING);
//        mutator.addInsertion(sms.getDbId(), columnFamilyName,
//                HFactory.createColumn(cc, sms.getSmsSet().getDestAddr(), SERIALIZER_COMPOSITE, SERIALIZER_STRING));
//        cc = new Composite();
//        cc.addComponent(Schema.COLUMN_ADDR_DST_TON, SERIALIZER_STRING);
//        mutator.addInsertion(sms.getDbId(), columnFamilyName,
//                HFactory.createColumn(cc, sms.getSmsSet().getDestAddrTon(), SERIALIZER_COMPOSITE, SERIALIZER_INTEGER));
//        cc = new Composite();
//        cc.addComponent(Schema.COLUMN_ADDR_DST_NPI, SERIALIZER_STRING);
//        mutator.addInsertion(sms.getDbId(), columnFamilyName,
//                HFactory.createColumn(cc, sms.getSmsSet().getDestAddrNpi(), SERIALIZER_COMPOSITE, SERIALIZER_INTEGER));

//      if (sms.getSourceAddr() != null) {
//          cc = new Composite();
//          cc.addComponent(Schema.COLUMN_ADDR_SRC_DIGITS, SERIALIZER_STRING);
//          mutator.addInsertion(sms.getDbId(), columnFamilyName,
//                  HFactory.createColumn(cc, sms.getSourceAddr(), SERIALIZER_COMPOSITE, SERIALIZER_STRING));
//      }
//
//        cc = new Composite();
//        cc.addComponent(Schema.COLUMN_ADDR_SRC_TON, SERIALIZER_STRING);
//        mutator.addInsertion(sms.getDbId(), columnFamilyName,
//                HFactory.createColumn(cc, sms.getSourceAddrTon(), SERIALIZER_COMPOSITE, SERIALIZER_INTEGER));
//        cc = new Composite();
//        cc.addComponent(Schema.COLUMN_ADDR_SRC_NPI, SERIALIZER_STRING);
//        mutator.addInsertion(sms.getDbId(), columnFamilyName,
//                HFactory.createColumn(cc, sms.getSourceAddrNpi(), SERIALIZER_COMPOSITE, SERIALIZER_INTEGER));
//
//        cc = new Composite();
//        cc.addComponent(Schema.COLUMN_MESSAGE_ID, SERIALIZER_STRING);
//        mutator.addInsertion(sms.getDbId(), columnFamilyName,
//                HFactory.createColumn(cc, sms.getMessageId(), SERIALIZER_COMPOSITE, LongSerializer.get()));
//        cc = new Composite();
//        cc.addComponent(Schema.COLUMN_MO_MESSAGE_REF, SERIALIZER_STRING);
//        mutator.addInsertion(sms.getDbId(), columnFamilyName,
//                HFactory.createColumn(cc, sms.getMoMessageRef(), SERIALIZER_COMPOSITE, SERIALIZER_INTEGER));
//        
//        if (sms.getOrigEsmeName() != null) {
//            cc = new Composite();
//            cc.addComponent(Schema.COLUMN_ORIG_ESME_NAME, SERIALIZER_STRING);
//            mutator.addInsertion(sms.getDbId(), columnFamilyName,
//                    HFactory.createColumn(cc, sms.getOrigEsmeName(), SERIALIZER_COMPOSITE, SERIALIZER_STRING));
//        }
//        if (sms.getOrigSystemId() != null) {
//            cc = new Composite();
//            cc.addComponent(Schema.COLUMN_ORIG_SYSTEM_ID, SERIALIZER_STRING);
//            mutator.addInsertion(sms.getDbId(), columnFamilyName,
//                    HFactory.createColumn(cc, sms.getOrigSystemId(), SERIALIZER_COMPOSITE, SERIALIZER_STRING));
//        }
//        if (sms.getSubmitDate() != null) {
//            cc = new Composite();
//            cc.addComponent(Schema.COLUMN_SUBMIT_DATE, SERIALIZER_STRING);
//            mutator.addInsertion(sms.getDbId(), columnFamilyName,
//                    HFactory.createColumn(cc, sms.getSubmitDate(), SERIALIZER_COMPOSITE, SERIALIZER_DATE));
//        }
//
//        if (sms.getServiceType() != null) {
//            cc = new Composite();
//            cc.addComponent(Schema.COLUMN_SERVICE_TYPE, SERIALIZER_STRING);
//            mutator.addInsertion(sms.getDbId(), columnFamilyName,
//                    HFactory.createColumn(cc, sms.getServiceType(), SERIALIZER_COMPOSITE, SERIALIZER_STRING));
//        }
//        cc = new Composite();
//        cc.addComponent(Schema.COLUMN_ESM_CLASS, SERIALIZER_STRING);
//        mutator.addInsertion(sms.getDbId(), columnFamilyName,
//                HFactory.createColumn(cc, sms.getEsmClass(), SERIALIZER_COMPOSITE, SERIALIZER_INTEGER));
//        cc = new Composite();
//        cc.addComponent(Schema.COLUMN_PROTOCOL_ID, SERIALIZER_STRING);
//        mutator.addInsertion(sms.getDbId(), columnFamilyName,
//                HFactory.createColumn(cc, sms.getProtocolId(), SERIALIZER_COMPOSITE, SERIALIZER_INTEGER));
//        cc = new Composite();
//        cc.addComponent(Schema.COLUMN_PRIORITY, SERIALIZER_STRING);
//        mutator.addInsertion(sms.getDbId(), columnFamilyName,
//                HFactory.createColumn(cc, sms.getPriority(), SERIALIZER_COMPOSITE, SERIALIZER_INTEGER));
//
//        cc = new Composite();
//        cc.addComponent(Schema.COLUMN_REGISTERED_DELIVERY, SERIALIZER_STRING);
//        mutator.addInsertion(sms.getDbId(), columnFamilyName,
//                HFactory.createColumn(cc, sms.getRegisteredDelivery(), SERIALIZER_COMPOSITE, SERIALIZER_INTEGER));
//        cc = new Composite();
//        cc.addComponent(Schema.COLUMN_REPLACE, SERIALIZER_STRING);
//        mutator.addInsertion(sms.getDbId(), columnFamilyName,
//                HFactory.createColumn(cc, sms.getReplaceIfPresent(), SERIALIZER_COMPOSITE, SERIALIZER_INTEGER));
//        cc = new Composite();
//        cc.addComponent(Schema.COLUMN_DATA_CODING, SERIALIZER_STRING);
//        mutator.addInsertion(sms.getDbId(), columnFamilyName,
//                HFactory.createColumn(cc, sms.getDataCoding(), SERIALIZER_COMPOSITE, SERIALIZER_INTEGER));
//        cc = new Composite();
//        cc.addComponent(Schema.COLUMN_DEFAULT_MSG_ID, SERIALIZER_STRING);
//        mutator.addInsertion(sms.getDbId(), columnFamilyName,
//                HFactory.createColumn(cc, sms.getDefaultMsgId(), SERIALIZER_COMPOSITE, SERIALIZER_INTEGER));
//      if (sms.getShortMessage() != null) {
//      cc = new Composite();
//      cc.addComponent(Schema.COLUMN_MESSAGE, SERIALIZER_STRING);
//      mutator.addInsertion(sms.getDbId(), columnFamilyName,
//              HFactory.createColumn(cc, sms.getShortMessage(), SERIALIZER_COMPOSITE, SERIALIZER_BYTE_ARRAY));
//  }
//  if (sms.getScheduleDeliveryTime() != null) {
//      cc = new Composite();
//      cc.addComponent(Schema.COLUMN_SCHEDULE_DELIVERY_TIME, SERIALIZER_STRING);
//      mutator.addInsertion(sms.getDbId(), columnFamilyName,
//              HFactory.createColumn(cc, sms.getScheduleDeliveryTime(), SERIALIZER_COMPOSITE, SERIALIZER_DATE));
//  }
//  if (sms.getValidityPeriod() != null) {
//      cc = new Composite();
//      cc.addComponent(Schema.COLUMN_VALIDITY_PERIOD, SERIALIZER_STRING);
//      mutator.addInsertion(sms.getDbId(), columnFamilyName,
//              HFactory.createColumn(cc, sms.getValidityPeriod(), SERIALIZER_COMPOSITE, SERIALIZER_DATE));
//  }
//      cc = new Composite();
//      cc.addComponent(Schema.COLUMN_DELIVERY_COUNT, SERIALIZER_STRING);
//      mutator.addInsertion(sms.getDbId(), columnFamilyName,
//              HFactory.createColumn(cc, sms.getDeliveryCount(), SERIALIZER_COMPOSITE, SERIALIZER_INTEGER));
//
//      if (sms.getTlvSet().getOptionalParameterCount() > 0) {
//          cc = new Composite();
//          cc.addComponent(Schema.COLUMN_OPTIONAL_PARAMETERS, SERIALIZER_STRING);
//
//          try {
//              ByteArrayOutputStream baos = new ByteArrayOutputStream();
//              XMLObjectWriter writer = XMLObjectWriter.newInstance(baos);
//              writer.setIndentation("\t");
//              writer.write(sms.getTlvSet(), TLV_SET, TlvSet.class);
//              writer.close();
//              byte[] rawData = baos.toByteArray();
//              String serializedEvent = new String(rawData);
//
//              mutator.addInsertion(sms.getDbId(), columnFamilyName,
//                      HFactory.createColumn(cc, serializedEvent, SERIALIZER_COMPOSITE, SERIALIZER_STRING));
//          } catch (XMLStreamException e) {
//              String msg = "XMLStreamException when serializing optional parameters for '" + sms.getDbId() + "'!";
//
//              throw new PersistenceException(msg, e);
//          }
//      }


        boundStatement.setUUID(Schema.COLUMN_ID, sms.getDbId());

        boundStatement.setString(Schema.COLUMN_ADDR_DST_DIGITS, sms.getSmsSet().getDestAddr());
        boundStatement.setInt(Schema.COLUMN_ADDR_DST_TON, sms.getSmsSet().getDestAddrTon());
        boundStatement.setInt(Schema.COLUMN_ADDR_DST_NPI, sms.getSmsSet().getDestAddrNpi());

        if (sms.getSourceAddr() != null) {
            boundStatement.setString(Schema.COLUMN_ADDR_SRC_DIGITS, sms.getSourceAddr());
        }
        boundStatement.setInt(Schema.COLUMN_ADDR_SRC_TON, sms.getSourceAddrTon());
        boundStatement.setInt(Schema.COLUMN_ADDR_SRC_NPI, sms.getSourceAddrNpi());

        boundStatement.setLong(Schema.COLUMN_MESSAGE_ID, sms.getMessageId());
        boundStatement.setInt(Schema.COLUMN_MO_MESSAGE_REF, sms.getMoMessageRef());

        if (sms.getOrigEsmeName() != null) {
            boundStatement.setString(Schema.COLUMN_ORIG_ESME_NAME, sms.getOrigEsmeName());
        }
        if (sms.getOrigSystemId() != null) {
            boundStatement.setString(Schema.COLUMN_ORIG_SYSTEM_ID, sms.getOrigSystemId());
        }
        if (sms.getSubmitDate() != null) {
            boundStatement.setDate(Schema.COLUMN_SUBMIT_DATE, sms.getSubmitDate());
        }
        if (sms.getServiceType() != null) {
            boundStatement.setString(Schema.COLUMN_SERVICE_TYPE, sms.getServiceType());
        }
        boundStatement.setInt(Schema.COLUMN_ESM_CLASS, sms.getEsmClass());
        boundStatement.setInt(Schema.COLUMN_PROTOCOL_ID, sms.getProtocolId());
        boundStatement.setInt(Schema.COLUMN_PRIORITY, sms.getPriority());

        boundStatement.setInt(Schema.COLUMN_REGISTERED_DELIVERY, sms.getRegisteredDelivery());
        boundStatement.setInt(Schema.COLUMN_REPLACE, sms.getReplaceIfPresent());
        boundStatement.setInt(Schema.COLUMN_DATA_CODING, sms.getDataCoding());
        boundStatement.setInt(Schema.COLUMN_DEFAULT_MSG_ID, sms.getDefaultMsgId());
        
        if (sms.getShortMessage() != null) {
            boundStatement.setBytes(Schema.COLUMN_MESSAGE, ByteBuffer.wrap(sms.getShortMessage()));
        }
        if (sms.getScheduleDeliveryTime() != null) {
            boundStatement.setDate(Schema.COLUMN_SCHEDULE_DELIVERY_TIME, sms.getScheduleDeliveryTime());
        }
        if (sms.getValidityPeriod() != null) {
            boundStatement.setDate(Schema.COLUMN_VALIDITY_PERIOD, sms.getValidityPeriod());
        }

        boundStatement.setInt(Schema.COLUMN_DELIVERY_COUNT, sms.getDeliveryCount());

        if (sms.getTlvSet().getOptionalParameterCount() > 0) {
            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                XMLObjectWriter writer = XMLObjectWriter.newInstance(baos);
                writer.setIndentation("\t");
                writer.write(sms.getTlvSet(), TLV_SET, TlvSet.class);
                writer.close();
                byte[] rawData = baos.toByteArray();
                String serializedEvent = new String(rawData);

                boundStatement.setString(Schema.COLUMN_OPTIONAL_PARAMETERS, serializedEvent);
            } catch (XMLStreamException e) {
                String msg = "XMLStreamException when serializing optional parameters for '" + sms.getDbId() + "'!";

                throw new PersistenceException(msg, e);
            }
        }

    }

    protected Sms createSms(final Row row, SmsSet smsSet, UUID dbId) throws PersistenceException {

        if (row == null)
            return null;
//      if (cSlice.getColumns().size() == 0)
//      return null;

        Sms sms = new Sms();
        sms.setDbId(row.getUUID(Schema.COLUMN_ID));
        String destAddr = null;
        int destAddrTon = -1;
        int destAddrNpi = -1;

//        if (name.equals(Schema.COLUMN_ADDR_DST_DIGITS)) {
//            destAddr = SERIALIZER_STRING.fromByteBuffer(col.getValue());
//        } else if (name.equals(Schema.COLUMN_ADDR_DST_TON)) {
//            destAddrTon = SERIALIZER_INTEGER.fromByteBuffer(col.getValue());
//        } else if (name.equals(Schema.COLUMN_ADDR_DST_NPI)) {
//            destAddrNpi = SERIALIZER_INTEGER.fromByteBuffer(col.getValue());
//    } else if (name.equals(Schema.COLUMN_MESSAGE_ID)) {
//        long val = LongSerializer.get().fromByteBuffer(col.getValue());
//        sms.setMessageId(val);
//    } else if (name.equals(Schema.COLUMN_MO_MESSAGE_REF)) {
//        int val = SERIALIZER_INTEGER.fromByteBuffer(col.getValue());
//        sms.setMoMessageRef(val);
//    } else if (name.equals(Schema.COLUMN_ORIG_ESME_NAME)) {
//        String val = SERIALIZER_STRING.fromByteBuffer(col.getValue());
//        sms.setOrigEsmeName(val);
//    } else if (name.equals(Schema.COLUMN_ORIG_SYSTEM_ID)) {
//        String val = SERIALIZER_STRING.fromByteBuffer(col.getValue());
//        sms.setOrigSystemId(val);
//    } else if (name.equals(Schema.COLUMN_SUBMIT_DATE)) {
//        Date val = SERIALIZER_DATE.fromByteBuffer(col.getValue());
//        sms.setSubmitDate(val);
//    } else if (name.equals(Schema.COLUMN_ADDR_SRC_DIGITS)) {
//        sms.setSourceAddr(SERIALIZER_STRING.fromByteBuffer(col.getValue()));
//    } else if (name.equals(Schema.COLUMN_ADDR_SRC_TON)) {
//        sms.setSourceAddrTon(SERIALIZER_INTEGER.fromByteBuffer(col.getValue()));
//    } else if (name.equals(Schema.COLUMN_ADDR_SRC_NPI)) {
//        sms.setSourceAddrNpi(SERIALIZER_INTEGER.fromByteBuffer(col.getValue()));
//    } else if (name.equals(Schema.COLUMN_SERVICE_TYPE)) {
//        String val = SERIALIZER_STRING.fromByteBuffer(col.getValue());
//        sms.setServiceType(val);
//    } else if (name.equals(Schema.COLUMN_ESM_CLASS)) {
//        sms.setEsmClass(SERIALIZER_INTEGER.fromByteBuffer(col.getValue()));
//    } else if (name.equals(Schema.COLUMN_PROTOCOL_ID)) {
//        sms.setProtocolId(SERIALIZER_INTEGER.fromByteBuffer(col.getValue()));
//    } else if (name.equals(Schema.COLUMN_PRIORITY)) {
//        sms.setPriority(SERIALIZER_INTEGER.fromByteBuffer(col.getValue()));
//    } else if (name.equals(Schema.COLUMN_REGISTERED_DELIVERY)) {
//        sms.setRegisteredDelivery(SERIALIZER_INTEGER.fromByteBuffer(col.getValue()));
//    } else if (name.equals(Schema.COLUMN_REPLACE)) {
//        sms.setReplaceIfPresent(SERIALIZER_INTEGER.fromByteBuffer(col.getValue()));
//    } else if (name.equals(Schema.COLUMN_DATA_CODING)) {
//        sms.setDataCoding(SERIALIZER_INTEGER.fromByteBuffer(col.getValue()));
//    } else if (name.equals(Schema.COLUMN_DEFAULT_MSG_ID)) {
//        sms.setDefaultMsgId(SERIALIZER_INTEGER.fromByteBuffer(col.getValue()));
//    } else if (name.equals(Schema.COLUMN_MESSAGE)) {
//        sms.setShortMessage(SERIALIZER_BYTE_ARRAY.fromByteBuffer(col.getValue()));
//    } else if (name.equals(Schema.COLUMN_SCHEDULE_DELIVERY_TIME)) {
//        Date val = SERIALIZER_DATE.fromByteBuffer(col.getValue());
//        sms.setScheduleDeliveryTime(val);
//    } else if (name.equals(Schema.COLUMN_VALIDITY_PERIOD)) {
//        Date val = SERIALIZER_DATE.fromByteBuffer(col.getValue());
//        sms.setValidityPeriod(val);
//    } else if (name.equals(Schema.COLUMN_DELIVERY_COUNT)) {
//        sms.setDeliveryCount(SERIALIZER_INTEGER.fromByteBuffer(col.getValue()));

    //  } else if (name.equals(Schema.COLUMN_OPTIONAL_PARAMETERS)) {
//          String s = SERIALIZER_STRING.fromByteBuffer(col.getValue());
//          try {
//              ByteArrayInputStream bais = new ByteArrayInputStream(s.getBytes());
//              XMLObjectReader reader = XMLObjectReader.newInstance(bais);
//              TlvSet copy = reader.read(TLV_SET, TlvSet.class);
//              sms.getTlvSet().clearAllOptionalParameter();
//              sms.getTlvSet().addAllOptionalParameter(copy.getOptionalParameters());
//          } catch (XMLStreamException e) {
//              String msg = "XMLStreamException when deserializing optional parameters for '" + sms.getDbId() + "'!";
    //
//              throw new PersistenceException(msg, e);
//          }
    //  }

 
        destAddr = row.getString(Schema.COLUMN_ADDR_DST_DIGITS);
        destAddrTon = row.getInt(Schema.COLUMN_ADDR_DST_TON);
        destAddrNpi = row.getInt(Schema.COLUMN_ADDR_DST_NPI);

        sms.setMessageId(row.getLong(Schema.COLUMN_MESSAGE_ID));
        sms.setMoMessageRef(row.getInt(Schema.COLUMN_MO_MESSAGE_REF));
        sms.setOrigEsmeName(row.getString(Schema.COLUMN_ORIG_ESME_NAME));
        sms.setOrigSystemId(row.getString(Schema.COLUMN_ORIG_SYSTEM_ID));
        sms.setSubmitDate(row.getDate(Schema.COLUMN_SUBMIT_DATE));

        sms.setSourceAddr(row.getString(Schema.COLUMN_ADDR_SRC_DIGITS));
        sms.setSourceAddrTon(row.getInt(Schema.COLUMN_ADDR_SRC_TON));
        sms.setSourceAddrNpi(row.getInt(Schema.COLUMN_ADDR_SRC_NPI));

        sms.setServiceType(row.getString(Schema.COLUMN_SERVICE_TYPE));
        sms.setEsmClass(row.getInt(Schema.COLUMN_ESM_CLASS));
        sms.setProtocolId(row.getInt(Schema.COLUMN_PROTOCOL_ID));
        sms.setPriority(row.getInt(Schema.COLUMN_PRIORITY));
        sms.setRegisteredDelivery(row.getInt(Schema.COLUMN_REGISTERED_DELIVERY));
        sms.setReplaceIfPresent(row.getInt(Schema.COLUMN_REPLACE));
        sms.setDataCoding(row.getInt(Schema.COLUMN_DATA_CODING));
        sms.setDefaultMsgId(row.getInt(Schema.COLUMN_DEFAULT_MSG_ID));

        ByteBuffer bb = row.getBytes(Schema.COLUMN_MESSAGE);
        byte[] buf = new byte[bb.limit() - bb.position()];
        bb.get(buf);
        sms.setShortMessage(buf);
        sms.setScheduleDeliveryTime(row.getDate(Schema.COLUMN_SCHEDULE_DELIVERY_TIME));
        sms.setValidityPeriod(row.getDate(Schema.COLUMN_VALIDITY_PERIOD));
        sms.setDeliveryCount(row.getInt(Schema.COLUMN_DELIVERY_COUNT));

        String s = row.getString(Schema.COLUMN_OPTIONAL_PARAMETERS);
        if (s != null) {
            try {
                ByteArrayInputStream bais = new ByteArrayInputStream(s.getBytes());
                XMLObjectReader reader = XMLObjectReader.newInstance(bais);
                TlvSet copy = reader.read(TLV_SET, TlvSet.class);
                sms.getTlvSet().clearAllOptionalParameter();
                sms.getTlvSet().addAllOptionalParameter(copy.getOptionalParameters());
            } catch (XMLStreamException e) {
                String msg = "XMLStreamException when deserializing optional parameters for '" + sms.getDbId() + "'!";

                throw new PersistenceException(msg, e);
            }
        }

        if (destAddr == null || destAddrTon == -1 || destAddrNpi == -1) {
            throw new PersistenceException("destAddr or destAddrTon or destAddrNpi is absent in LIVE_SMS for ID='" + dbId + "'");
        }

        if (smsSet == null) {
            TargetAddress ta = new TargetAddress(destAddrTon, destAddrNpi, destAddr);
            smsSet = obtainSmsSet(ta);
        } else {
            if (smsSet.getDestAddr() == null) {
                smsSet.setDestAddr(destAddr);
                smsSet.setDestAddrTon(destAddrTon);
                smsSet.setDestAddrNpi(destAddrNpi);

                // TODO: here we can add fields that are present only in ARCHIVE
                // table (into extra fields of Sms class)
                // "DEST_CLUSTER_NAME" text,
                // "DEST_ESME_ID" text,
                // "DEST_SYSTEM_ID" text,
                // "DELIVERY_DATE" timestamp,
                // "IMSI" ascii,
                // "NNN_DIGITS" ascii,
                // "NNN_AN" int,
                // "NNN_NP" int,
                // "SM_STATUS" int,
                // "SM_TYPE" int,
                // "DELIVERY_COUNT" int,
            }
        }
        sms.setSmsSet(smsSet);

        return sms;
    }

    private static SmsSet createSmsSet(Row row) {
        SmsSet smsSet = new SmsSet();

        if (row != null) {
            smsSet.setDestAddr(row.getString(Schema.COLUMN_ADDR_DST_DIGITS));
            smsSet.setDestAddrTon(row.getInt(Schema.COLUMN_ADDR_DST_TON));
            smsSet.setDestAddrNpi(row.getInt(Schema.COLUMN_ADDR_DST_NPI));

            smsSet.setInSystem(row.getInt(Schema.COLUMN_IN_SYSTEM));
            smsSet.setInSystemDate(row.getDate(Schema.COLUMN_IN_SYSTEM_DATE));
            smsSet.setDueDate(row.getDate(Schema.COLUMN_DUE_DATE));

            if (!row.isNull(Schema.COLUMN_SM_STATUS))
                smsSet.setStatus(ErrorCode.fromInt(row.getInt(Schema.COLUMN_SM_STATUS)));
            smsSet.setDueDelay(row.getInt(Schema.COLUMN_DUE_DELAY));
//            smsSet.setLastDelivery(row.getDate(Schema.COLUMN_LAST_DELIVERY));
            smsSet.setAlertingSupported(row.getBool(Schema.COLUMN_ALERTING_SUPPORTED));
        }

//        for (HColumn<Composite, ByteBuffer> col : cSlice.getColumns()) {
//            Composite nm = col.getName();
//            String name = nm.get(0, SERIALIZER_STRING);
//
//            if (name.equals(Schema.COLUMN_ADDR_DST_DIGITS)) {
//                smsSet.setDestAddr(SERIALIZER_STRING.fromByteBuffer(col.getValue()));
//            } else if (name.equals(Schema.COLUMN_ADDR_DST_TON)) {
//                smsSet.setDestAddrTon(SERIALIZER_INTEGER.fromByteBuffer(col.getValue()));
//            } else if (name.equals(Schema.COLUMN_ADDR_DST_NPI)) {
//                smsSet.setDestAddrNpi(SERIALIZER_INTEGER.fromByteBuffer(col.getValue()));
//            } else if (name.equals(Schema.COLUMN_IN_SYSTEM)) {
//                smsSet.setInSystem(SERIALIZER_INTEGER.fromByteBuffer(col.getValue()));
//            } else if (name.equals(Schema.COLUMN_IN_SYSTEM_DATE)) {
//                smsSet.setInSystemDate(SERIALIZER_DATE.fromByteBuffer(col.getValue()));
//            } else if (name.equals(Schema.COLUMN_DUE_DATE)) {
//                smsSet.setDueDate(SERIALIZER_DATE.fromByteBuffer(col.getValue()));
//            } else if (name.equals(Schema.COLUMN_SM_STATUS)) {
//                smsSet.setStatus(ErrorCode.fromInt((SERIALIZER_INTEGER.fromByteBuffer(col.getValue()))));
//            } else if (name.equals(Schema.COLUMN_DUE_DELAY)) {
//                smsSet.setDueDelay(SERIALIZER_INTEGER.fromByteBuffer(col.getValue()));
//            } else if (name.equals(Schema.COLUMN_LAST_DELIVERY)) {
//                smsSet.setLastDelivery(SERIALIZER_DATE.fromByteBuffer(col.getValue()));
//            } else if (name.equals(Schema.COLUMN_ALERTING_SUPPORTED)) {
//                smsSet.setAlertingSupported(SERIALIZER_BOOLEAN.fromByteBuffer(col.getValue()));
//            }
//        }
        return smsSet;
    }

	public DbSmsRoutingRule getSmsRoutingRule(final String address) throws PersistenceException {

        try {
            BoundStatement boundStatement = new BoundStatement(getSmsRoutingRule);
            boundStatement.bind(address);
            ResultSet result = session.execute(boundStatement);

            Row row = result.one();
            if (row == null) {
                return null;
            } else {
                DbSmsRoutingRule res = new DbSmsRoutingRule();
                res.setAddress(address);
                String name = row.getString(Schema.COLUMN_CLUSTER_NAME);
                res.setClusterName(name);
                return res;
            }



//            SliceQuery<String, Composite, ByteBuffer> query = HFactory.createSliceQuery(keyspace, SERIALIZER_STRING, SERIALIZER_COMPOSITE,
//                    ByteBufferSerializer.get());
//            query.setColumnFamily(Schema.FAMILY_SMS_ROUTING_RULE);
//            query.setKey(address);
//
//            query.setRange(null, null, false, 100);
//
//            QueryResult<ColumnSlice<Composite, ByteBuffer>> result = query.execute();
//            ColumnSlice<Composite, ByteBuffer> cSlice = result.get();
//            if (cSlice == null || cSlice.getColumns().size() == 0)
//                return null;
//
//            DbSmsRoutingRule res = new DbSmsRoutingRule();
//            for (HColumn<Composite, ByteBuffer> col : cSlice.getColumns()) {
//                Composite nm = col.getName();
//                String name = nm.get(0, SERIALIZER_STRING);
//                res.setAddress(address);
//
//                if (name.equals(Schema.COLUMN_CLUSTER_NAME)) {
//                    res.setClusterName(SERIALIZER_STRING.fromByteBuffer(col.getValue()));
//                }
//            }
        } catch (Exception e) {
            String msg = "Failed to getSmsRoutingRule DbSmsRoutingRule for id='" + address + "'!";

            throw new PersistenceException(msg, e);
        }
	}

	public void updateDbSmsRoutingRule(DbSmsRoutingRule dbSmsRoutingRule) throws PersistenceException {
        try {
            BoundStatement boundStatement = new BoundStatement(updateDbSmsRoutingRule);
            boundStatement.bind(dbSmsRoutingRule.getAddress(), dbSmsRoutingRule.getClusterName());
            session.execute(boundStatement);



//            Mutator<String> mutator = HFactory.createMutator(keyspace, SERIALIZER_STRING);
//
//            Composite cc;
//            if (dbSmsRoutingRule.getClusterName() != null) {
//                cc = new Composite();
//                cc.addComponent(Schema.COLUMN_CLUSTER_NAME, SERIALIZER_STRING);
//                mutator.addInsertion(dbSmsRoutingRule.getAddress(), Schema.FAMILY_SMS_ROUTING_RULE,
//                        HFactory.createColumn(cc, dbSmsRoutingRule.getClusterName(), SERIALIZER_COMPOSITE, SERIALIZER_STRING));
//            }
//
//            mutator.execute();
        } catch (Exception e) {
            String msg = "Failed to addDbSmsRoutingRule for '" + dbSmsRoutingRule.getAddress() + "'!";

            throw new PersistenceException(msg, e);
        }
	}

	public void deleteDbSmsRoutingRule(final String address) throws PersistenceException {
        try {
            BoundStatement boundStatement = new BoundStatement(deleteDbSmsRoutingRule);
            boundStatement.bind(address);
            session.execute(boundStatement);



//            Mutator<String> mutator = HFactory.createMutator(keyspace, SERIALIZER_STRING);
//
//            mutator.addDeletion(address, Schema.FAMILY_SMS_ROUTING_RULE);
//            mutator.execute();
        } catch (Exception e) {
            String msg = "Failed to deleteDbSmsRoutingRule for '" + address + "'!";
            throw new PersistenceException(msg, e);
        }
	}

	public List<DbSmsRoutingRule> getSmsRoutingRulesRange() throws PersistenceException {
		return getSmsRoutingRulesRange(null);
	}
	
	public List<DbSmsRoutingRule> getSmsRoutingRulesRange(String lastAdress) throws PersistenceException {

        List<DbSmsRoutingRule> ress = new FastList<DbSmsRoutingRule>();
        try {
            PreparedStatement ps = lastAdress != null ? getSmsRoutingRulesRange : getSmsRoutingRulesRange2;
            BoundStatement boundStatement = new BoundStatement(ps);
            if (lastAdress != null) {
                boundStatement.bind(lastAdress);
            }
            ResultSet result = session.execute(boundStatement);

            int i1 = 0;
            for (Row row : result) {
                DbSmsRoutingRule res = new DbSmsRoutingRule();
                String address = row.getString(Schema.COLUMN_ADDRESS);
                res.setAddress(address);
                String name = row.getString(Schema.COLUMN_CLUSTER_NAME);
                res.setClusterName(name);

                if (i1 == 0) {
                    i1 = 1;
                    if (lastAdress == null)
                        ress.add(res);
                } else {
                    ress.add(res);
                }
            }

            return ress;


//            RangeSlicesQuery<String, Composite, ByteBuffer> rangeSlicesQuery = HFactory.createRangeSlicesQuery(keyspace, SERIALIZER_STRING,
//                    SERIALIZER_COMPOSITE, ByteBufferSerializer.get());
//            rangeSlicesQuery.setColumnFamily(Schema.FAMILY_SMS_ROUTING_RULE);
//            rangeSlicesQuery.setRange(null, null, false, 10);
//            rangeSlicesQuery.setRowCount(row_count);

//            while (true) {
//                rangeSlicesQuery.setKeys(lastAdress, null);
//
//                QueryResult<OrderedRows<String, Composite, ByteBuffer>> result = rangeSlicesQuery.execute();
//                OrderedRows<String, Composite, ByteBuffer> rows = result.get();
//                Iterator<Row<String, Composite, ByteBuffer>> rowsIterator = rows.iterator();
//
//                // we'll skip this first one, since it is the same as the last
//                // one from previous time we executed
//                if (lastAdress != null && rowsIterator != null)
//                    rowsIterator.next();
//
//                while (rowsIterator.hasNext()) {
//                    Row<String, Composite, ByteBuffer> row = rowsIterator.next();
//                    lastAdress = row.getKey();
//
//                    DbSmsRoutingRule res = new DbSmsRoutingRule();
//                    res.setAddress(row.getKey());
//                    for (HColumn<Composite, ByteBuffer> col : row.getColumnSlice().getColumns()) {
//                        Composite nm = col.getName();
//                        String name = nm.get(0, SERIALIZER_STRING);
//
//                        if (name.equals(Schema.COLUMN_CLUSTER_NAME)) {
//                            res.setClusterName(SERIALIZER_STRING.fromByteBuffer(col.getValue()));
//                        }
//                    }
//                    if (res.getClusterName() != null)
//                        ress.add(res);
//                }
//
//                // now we support only one step - 100 records
//                break;
//            }
        } catch (Exception e) {
            String msg = "Failed to getSmsRoutingRule DbSmsRoutingRule for all records: " + e;

            throw new PersistenceException(msg, e);
        }
	}

    protected void deleteLiveSms(final Sms sms) throws PersistenceException {

        try {
            BoundStatement boundStatement = new BoundStatement(deleteLiveSms);
            boundStatement.bind(sms.getDbId());
            session.execute(boundStatement);
        } catch (Exception e) {
            String msg = "Failed to deleteLiveSms for '" + sms.getDbId() + "'!";

            throw new PersistenceException(msg, e);
        }
    }

    private void doArchiveDeliveredSms(final Sms sms) throws PersistenceException {
        try {
            BoundStatement boundStatement = new BoundStatement(doArchiveDeliveredSms);

            this.FillUpdateFields(sms, boundStatement, Schema.FAMILY_LIVE_SMS);

            boundStatement.setInt(Schema.COLUMN_IN_SYSTEM, 0);
            if (sms.getSmsSet().getDestClusterName() != null) {
                boundStatement.setString(Schema.COLUMN_DEST_CLUSTER_NAME, sms.getSmsSet().getDestClusterName());
            }
            if (sms.getSmsSet().getDestEsmeName() != null) {
                boundStatement.setString(Schema.COLUMN_DEST_ESME_NAME, sms.getSmsSet().getDestEsmeName());
            }
            if (sms.getSmsSet().getDestSystemId() != null) {
                boundStatement.setString(Schema.COLUMN_DEST_SYSTEM_ID, sms.getSmsSet().getDestSystemId());
            }
            if (sms.getDeliverDate() != null) {
                boundStatement.setDate(Schema.COLUMN_DELIVERY_DATE, sms.getDeliverDate());
            }
            if (sms.getSmsSet().getImsi() != null) {
                boundStatement.setString(Schema.COLUMN_IMSI, sms.getSmsSet().getImsi().getData());
            }
            if (sms.getSmsSet().getLocationInfoWithLMSI() != null) {
                boundStatement.setString(Schema.COLUMN_NNN_DIGITS, sms.getSmsSet().getLocationInfoWithLMSI().getNetworkNodeNumber().getAddress());
                boundStatement.setInt(Schema.COLUMN_NNN_AN, sms.getSmsSet().getLocationInfoWithLMSI().getNetworkNodeNumber().getAddressNature().getIndicator());
                boundStatement.setInt(Schema.COLUMN_NNN_NP, sms.getSmsSet().getLocationInfoWithLMSI().getNetworkNodeNumber().getNumberingPlan().getIndicator());
            }
            if (sms.getSmsSet().getStatus() != null) {
                boundStatement.setInt(Schema.COLUMN_SM_STATUS, sms.getSmsSet().getStatus().getCode());
            }
            if (sms.getSmsSet().getType() != null) {
                boundStatement.setInt(Schema.COLUMN_SM_TYPE, sms.getSmsSet().getType().getCode());
            }

            session.execute(boundStatement);            





//            Mutator<UUID> mutator = HFactory.createMutator(keyspace, UUIDSerializer.get());
//
//            FillUpdateFields(sms, mutator, Schema.FAMILY_ARCHIVE);

//            Composite cc = new Composite();
//            cc.addComponent(Schema.COLUMN_IN_SYSTEM, SERIALIZER_STRING);
//            mutator.addInsertion(sms.getDbId(), Schema.FAMILY_ARCHIVE, HFactory.createColumn(cc, 0, SERIALIZER_COMPOSITE, SERIALIZER_INTEGER));
//
//            if (sms.getSmsSet().getDestClusterName() != null) {
//                cc = new Composite();
//                cc.addComponent(Schema.COLUMN_DEST_CLUSTER_NAME, SERIALIZER_STRING);
//                mutator.addInsertion(sms.getDbId(), Schema.FAMILY_ARCHIVE, HFactory.createColumn(cc, sms.getSmsSet()
//                        .getDestClusterName(), SERIALIZER_COMPOSITE, SERIALIZER_STRING));
//            }
//            if (sms.getSmsSet().getDestEsmeName() != null) {
//                cc = new Composite();
//                cc.addComponent(Schema.COLUMN_DEST_ESME_NAME, SERIALIZER_STRING);
//                mutator.addInsertion(sms.getDbId(), Schema.FAMILY_ARCHIVE,
//                        HFactory.createColumn(cc, sms.getSmsSet().getDestEsmeName(), SERIALIZER_COMPOSITE, SERIALIZER_STRING));
//            }
//            if (sms.getSmsSet().getDestSystemId() != null) {
//                cc = new Composite();
//                cc.addComponent(Schema.COLUMN_DEST_SYSTEM_ID, SERIALIZER_STRING);
//                mutator.addInsertion(sms.getDbId(), Schema.FAMILY_ARCHIVE,
//                        HFactory.createColumn(cc, sms.getSmsSet().getDestSystemId(), SERIALIZER_COMPOSITE, SERIALIZER_STRING));
//            }
//            if (sms.getDeliverDate() != null) {
//                cc = new Composite();
//                cc.addComponent(Schema.COLUMN_DELIVERY_DATE, SERIALIZER_STRING);
//                mutator.addInsertion(sms.getDbId(), Schema.FAMILY_ARCHIVE,
//                        HFactory.createColumn(cc, sms.getDeliverDate(), SERIALIZER_COMPOSITE, SERIALIZER_DATE));
//            }
//
//            if (sms.getSmsSet().getImsi() != null) {
//                cc = new Composite();
//                cc.addComponent(Schema.COLUMN_IMSI, SERIALIZER_STRING);
//                mutator.addInsertion(sms.getDbId(), Schema.FAMILY_ARCHIVE,
//                        HFactory.createColumn(cc, sms.getSmsSet().getImsi().getData(), SERIALIZER_COMPOSITE, SERIALIZER_STRING));
//            }
//            if (sms.getSmsSet().getLocationInfoWithLMSI() != null) {
//                cc = new Composite();
//                cc.addComponent(Schema.COLUMN_NNN_DIGITS, SERIALIZER_STRING);
//                mutator.addInsertion(sms.getDbId(), Schema.FAMILY_ARCHIVE, HFactory
//                        .createColumn(cc, sms.getSmsSet().getLocationInfoWithLMSI().getNetworkNodeNumber().getAddress(),
//                                SERIALIZER_COMPOSITE, SERIALIZER_STRING));
//                cc = new Composite();
//                cc.addComponent(Schema.COLUMN_NNN_AN, SERIALIZER_STRING);
//                mutator.addInsertion(
//                        sms.getDbId(),
//                        Schema.FAMILY_ARCHIVE,
//                        HFactory.createColumn(cc, sms.getSmsSet().getLocationInfoWithLMSI().getNetworkNodeNumber()
//                                .getAddressNature().getIndicator(), SERIALIZER_COMPOSITE, SERIALIZER_INTEGER));
//                cc = new Composite();
//                cc.addComponent(Schema.COLUMN_NNN_NP, SERIALIZER_STRING);
//                mutator.addInsertion(
//                        sms.getDbId(),
//                        Schema.FAMILY_ARCHIVE,
//                        HFactory.createColumn(cc, sms.getSmsSet().getLocationInfoWithLMSI().getNetworkNodeNumber()
//                                .getNumberingPlan().getIndicator(), SERIALIZER_COMPOSITE, SERIALIZER_INTEGER));
//            }
//            if (sms.getSmsSet().getStatus() != null) {
//                cc = new Composite();
//                cc.addComponent(Schema.COLUMN_SM_STATUS, SERIALIZER_STRING);
//                mutator.addInsertion(sms.getDbId(), Schema.FAMILY_ARCHIVE, HFactory.createColumn(cc, sms.getSmsSet()
//                        .getStatus().getCode(), SERIALIZER_COMPOSITE, SERIALIZER_INTEGER));
//            }
//            if (sms.getSmsSet().getType() != null) {
//                cc = new Composite();
//                cc.addComponent(Schema.COLUMN_SM_TYPE, SERIALIZER_STRING);
//                mutator.addInsertion(sms.getDbId(), Schema.FAMILY_ARCHIVE, HFactory.createColumn(cc, sms.getSmsSet().getType()
//                        .getCode(), SERIALIZER_COMPOSITE, SERIALIZER_INTEGER));
//            }
//
//            mutator.execute();
        } catch (Exception e) {
            String msg = "Failed to archiveDeliveredSms SMS for '" + sms.getDbId() + "'!";

            throw new PersistenceException(msg, e);
        }
    }

}
