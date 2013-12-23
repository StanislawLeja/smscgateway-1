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
import java.util.UUID;

import javolution.util.FastMap;
import javolution.xml.XMLObjectReader;
import javolution.xml.XMLObjectWriter;
import javolution.xml.stream.XMLStreamException;

import org.apache.log4j.Logger;

import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Host;
import com.datastax.driver.core.Metadata;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.Cluster.Builder;
import com.datastax.driver.core.exceptions.InvalidQueryException;

/**
 *
 * @author sergey vetyutnev
 *
 */
public class DBOperations_C2 {
    private static final Logger logger = Logger.getLogger(DBOperations_C2.class);

    public static final String TLV_SET = "tlvSet";
    public static final UUID emptyUuid = UUID.nameUUIDFromBytes(new byte[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 });
    public static final int IN_SYSTEM_UNSENT = 0;
    public static final int IN_SYSTEM_INPROCESS = 1;
    public static final int IN_SYSTEM_SENT = 2;

    private static final DBOperations_C2 instance = new DBOperations_C2();

    // cassandra access
    private Cluster cluster;
    protected Session session;

    // hardcored configuring data - for table structures

    // multiTableModel: splitting database tables depending on dates
    private boolean multiTableModel = true;
    // length of the due_slot in milliseconds
    private int slotMSecondsTimeArea = 1000;
    // how many days one table carries (if value is <1 or >30 this means one month)
    private int dataTableDaysTimeArea = 1;
    // the date from which due_slots are calculated (01.01.2000) 
    private Date slotOrigDate = new Date(100, 0, 1);

    // configurable configuring data - for table structures

    // TTL for DST_SLOT_TABLE and SLOT_MESSAGES_TABLE tables (0 - no TTL)
    private int ttlCurrent = 0;
    // TTL for MESSAGES table (0 - no TTL)
    private int ttlArchive = 0;
    // timeout of finishing of writing on new income messages (in dueSlotWritingArray) (5 sec)
    private int millisecDueSlotWritingTimeout = 5000;
    // due_slot count for forward storing after current processing due_slot
    private int dueSlotForwardStoring;
    // due_slot count for which previous loaded records were revised
    private int dueSlotReviseOnSmscStart;
    // Timeout of life cycle of SmsSet in SmsSetCashe.ProcessingSmsSet in seconds
    private int processingSmsSetTimeout;
    
    // data for processing

    private long currentDueSlot = 0;
    private UUID currentSessionUUID;

    private FastMap<String, PreparedStatementCollection_C3> dataTableRead = new FastMap<String, PreparedStatementCollection_C3>();
    private FastMap<Long, DueSlotWritingElement> dueSlotWritingArray = new FastMap<Long, DueSlotWritingElement>();

    // prepared general statements
    private PreparedStatement selectCurrentSlotTable;
    private PreparedStatement updateCurrentSlotTable;

    private Date pcsDate;
    private PreparedStatementCollection_C3[] savedPsc;

    private volatile boolean started = false;

    protected DBOperations_C2() {
        super();
    }

    public static DBOperations_C2 getInstance() {
        return instance;
    }

    public boolean isStarted() {
        return started;
    }

    protected Session getSession() {
        return this.session;
    }
    
    public void start(String ip, int port, String keyspace, int secondsForwardStoring, int reviseSecondsOnSmscStart, int processingSmsSetTimeout)
            throws Exception {
        if (this.started) {
            throw new Exception("DBOperations already started");
        }

        if (secondsForwardStoring < 10)
            secondsForwardStoring = 10;
        this.dueSlotForwardStoring = secondsForwardStoring * 1000 / slotMSecondsTimeArea;
        this.dueSlotReviseOnSmscStart = reviseSecondsOnSmscStart * 1000 / slotMSecondsTimeArea;
        this.processingSmsSetTimeout = processingSmsSetTimeout;

        this.pcsDate = null;
        currentSessionUUID = UUID.randomUUID();

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

        this.checkCurrentSlotTableExists();
        
        String sa = "SELECT \"" + Schema.COLUMN_NEXT_SLOT + "\" FROM \"" + Schema.FAMILY_CURRENT_SLOT_TABLE + "\" where \"" + Schema.COLUMN_ID + "\"=0;";
        selectCurrentSlotTable = session.prepare(sa);
        sa = "INSERT INTO \"" + Schema.FAMILY_CURRENT_SLOT_TABLE + "\" (\"" + Schema.COLUMN_ID + "\", \"" + Schema.COLUMN_NEXT_SLOT + "\") VALUES (0, ?);";
        updateCurrentSlotTable = session.prepare(sa);

        try {
            PreparedStatement ps = selectCurrentSlotTable;
            BoundStatement boundStatement = new BoundStatement(ps);
            ResultSet res = session.execute(boundStatement);

            for (Row row : res) {
                currentDueSlot = row.getLong(0);
                break;
            }
            if (currentDueSlot == 0) {
                // not yet set
                long l1 = this.c2_getDueSlotForTime(new Date());
                this.c2_setCurrentDueSlot(l1);
            } else {
                this.c2_setCurrentDueSlot(currentDueSlot - dueSlotReviseOnSmscStart);
            }
        } catch (Exception e1) {
            String msg = "Failed reading a currentDueSlot !";
            throw new PersistenceException(msg, e1);
        }

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

    public long getSlotMSecondsTimeArea() {
        return slotMSecondsTimeArea;
    }

    /**
     * Return due_slot for the given time
     */
    public long c2_getDueSlotForTime(Date time) {
        long a2 = time.getTime();
        long a1 = this.slotOrigDate.getTime();
        long diff = a2 - a1;
        long res = diff / this.slotMSecondsTimeArea;
        return res;
    }

    /**
     * Return time for the given due_slot
     */
    public Date c2_getTimeForDueSlot(long dueSlot) {
        long a1 = this.slotOrigDate.getTime() + dueSlot * this.slotMSecondsTimeArea;
        Date date = new Date(a1);
        return date;
    }

    /**
     * Return due_slop that SMSC is processing now
     */
    public long c2_getCurrentDueSlot() {
        return currentDueSlot;
    }

    /**
     * Set a new due_slop that SMSC is processing now and store it to the database
     */
    public void c2_setCurrentDueSlot(long newDueSlot) throws PersistenceException {
        currentDueSlot = newDueSlot;

        try {
            PreparedStatement ps = updateCurrentSlotTable;
            BoundStatement boundStatement = new BoundStatement(ps);
            boundStatement.bind(newDueSlot);
            ResultSet res = session.execute(boundStatement);
        } catch (Exception e1) {
            String msg = "Failed writing a currentDueSlot !";
            throw new PersistenceException(msg, e1);
        }
    }

    /**
     * Return due_slop for current time
     */
    public long c2_getIntimeDueSlot() {
        return this.c2_getDueSlotForTime(new Date());
    }

    /**
     * Return due_slop for storing next incoming to SMSC message
     */
    public long c2_getDueSlotForNewSms() {
        long res = c2_getIntimeDueSlot() + dueSlotForwardStoring;
        return res;

        // TODO: we can add here code incrementing of due_slot if current
        // due_slot is overloaded
    }

    /**
     * Registering that thread starts writing to this due_slot
     */
    public void c2_registerDueSlotWriting(long dueSlot) {
        synchronized (dueSlotWritingArray) {
            Long ll = dueSlot;
            DueSlotWritingElement el = dueSlotWritingArray.get(ll);
            if (el == null) {
                el = new DueSlotWritingElement(ll);
                dueSlotWritingArray.put(ll, el);
                el.writingCount = 1;
            } else {
                el.writingCount++;
            }
            el.lastStartDate = new Date();
        }
    }

    /**
     * Registering that thread finishes writing to this due_slot
     */
    public void c2_unregisterDueSlotWriting(long dueSlot) {
        synchronized (dueSlotWritingArray) {
            Long ll = dueSlot;
            DueSlotWritingElement el = dueSlotWritingArray.get(ll);
            if (el != null) {
                el.writingCount--;
                if (el.writingCount == 0) {
                    dueSlotWritingArray.remove(ll);
                }
            }
        }
    }

    /**
     * Checking if due_slot is not in writing state now
     * Returns true if due_slot is not in writing now
     */
    public boolean c2_checkDueSlotNotWriting(long dueSlot) {
        synchronized (dueSlotWritingArray) {
            Long ll = dueSlot;
            DueSlotWritingElement el = dueSlotWritingArray.get(ll);
            if (el != null) {
                Date d1 = el.lastStartDate;
                Date d2 = new Date();
                long diff = d2.getTime() - d1.getTime();
                if (diff > millisecDueSlotWritingTimeout) {
                    logger.warn("Timeout in millisecDueSlotWritingTimeout element");
                    dueSlotWritingArray.remove(ll);
                    return true;
                } else
                    return false;
            } else {
                return true;
            }
        }
    }

    /**
     * Generate a table name depending on long dueSlot
     */
    protected String getTableName(long dueSlot) {
        Date dt = this.c2_getTimeForDueSlot(dueSlot);
        return getTableName(dt);
    }

    /**
     * Generate a table name depending on date
     */
    protected String getTableName(Date dt) {
        if (multiTableModel) {
            StringBuilder sb = new StringBuilder();
            sb.append("_");
            sb.append(dt.getYear() + 1900);
            sb.append("_");
            int mn = dt.getMonth() + 1;
            if (mn >= 10)
                sb.append(mn);
            else {
                sb.append("0");
                sb.append(mn);
            }
            if (this.dataTableDaysTimeArea < 1 || this.dataTableDaysTimeArea >= 30) {
            } else {
                int dy = dt.getDate();
                int fNum;
                if (this.dataTableDaysTimeArea == 1)
                    fNum = dy / this.dataTableDaysTimeArea;
                else
                    fNum = dy / this.dataTableDaysTimeArea + 1;
                sb.append("_");
                if (fNum >= 10)
                    sb.append(fNum);
                else {
                    sb.append("0");
                    sb.append(fNum);
                }
            }
            return sb.toString();
        } else
            return "";
    }

    public PreparedStatementCollection_C3[] getPscList() throws PersistenceException {
        Date dt = new Date();
        if (!this.isStarted())
            return new PreparedStatementCollection_C3[0];
        if (pcsDate != null && dt.getDate() == pcsDate.getDate()) {
            return savedPsc;
        } else {
            createPscList();
            return savedPsc;
        }
    }

    private void createPscList() throws PersistenceException {
        Date dt = new Date();
        Date dtt = new Date(dt.getTime() + 1000 * 60 * 60 * 24);
        String s1 = this.getTableName(dt);
        String s2 = this.getTableName(dtt);
        PreparedStatementCollection_C3[] res;
        if (!s2.equals(s1)) {
            res = new PreparedStatementCollection_C3[2];
            res[0] = this.getStatementCollection(dtt);
            res[1] = this.getStatementCollection(dt);
        } else {
            res = new PreparedStatementCollection_C3[1];
            res[0] = this.getStatementCollection(dt);
        }
        savedPsc = res;

        pcsDate = dt;
    }

    public long c2_getDueSlotForTargetId(String targetId) throws PersistenceException {
        PreparedStatementCollection_C3[] lstPsc = this.getPscList();

        for (PreparedStatementCollection_C3 psc : lstPsc) {
            long dueSlot = this.c2_getDueSlotForTargetId(psc, targetId);
            if (dueSlot != 0)
                return dueSlot;
        }
        return 0;
    }

    public long c2_getDueSlotForTargetId(PreparedStatementCollection_C3 psc, String targetId) throws PersistenceException {
        try {
            PreparedStatement ps = psc.getDueSlotForTargetId;
            BoundStatement boundStatement = new BoundStatement(ps);
            boundStatement.bind(targetId);
            ResultSet res = session.execute(boundStatement);

            long l = 0;
            for (Row row : res) {
                l = row.getLong(0);
                break;
            }
            return l;
        } catch (Exception e1) {
            // TODO: remove it
            e1.printStackTrace();

            String msg = "Failed to execute getDueSlotForTargetId() !";
            throw new PersistenceException(msg, e1);
        }
    }

    public void c2_updateDueSlotForTargetId(String targetId, long newDueSlot) throws PersistenceException {
        PreparedStatementCollection_C3 psc = this.getStatementCollection(newDueSlot);

        try {
            PreparedStatement ps = psc.createDueSlotForTargetId;
            BoundStatement boundStatement = new BoundStatement(ps);
            boundStatement.bind(targetId, newDueSlot);
            ResultSet res = session.execute(boundStatement);
        } catch (Exception e1) {
            String msg = "Failed to execute createDueSlotForTargetId() !";
            throw new PersistenceException(msg, e1);
        }
    }

    private void c2_clearDueSlotForTargetId(String targetId, long newDueSlot) throws PersistenceException {
        PreparedStatementCollection_C3 psc = this.getStatementCollection(newDueSlot);

        try {
            PreparedStatement ps = psc.createDueSlotForTargetId;
            BoundStatement boundStatement = new BoundStatement(ps);
            boundStatement.bind(targetId, 0L);
            ResultSet res = session.execute(boundStatement);
        } catch (Exception e1) {
            String msg = "Failed to execute clearDueSlotForTargetId() !";
            throw new PersistenceException(msg, e1);
        }
    }

    public void c2_updateDueSlotForTargetId_WithTableCleaning(String targetId, long newDueSlot) throws PersistenceException {
        // removing dueSlot for other time tables is any
        PreparedStatementCollection_C3[] lstPsc = this.getPscList();
        if (lstPsc.length >= 2) {
            String s1 = this.getTableName(newDueSlot);
            for (int i1 = 0; i1 < lstPsc.length; i1++) {
                PreparedStatementCollection_C3 psc = lstPsc[i1];
                if (!psc.getTName().equals(s1)) {
                    long dueSlot = this.c2_getDueSlotForTargetId(psc, targetId);
                    if (dueSlot != 0) {
                        c2_clearDueSlotForTargetId(targetId, dueSlot);
                    }
                }
            }
        }

        c2_updateDueSlotForTargetId(targetId, newDueSlot);
    }

    public void c2_scheduleMessage(Sms sms) throws PersistenceException {
        long dueSlot = 0;
        PreparedStatementCollection_C3[] lstPsc = this.getPscList();
        boolean done = false;
        int cnt = 0;
        while (!done && cnt < 5) {
            cnt++;
            for (PreparedStatementCollection_C3 psc : lstPsc) {
                dueSlot = this.c2_getDueSlotForTargetId(psc, sms.getSmsSet().getTargetId());
                if (dueSlot != 0)
                    break;
            }

            if (dueSlot == 0 || dueSlot <= this.c2_getCurrentDueSlot()) {
                dueSlot = this.c2_getDueSlotForNewSms();
                this.c2_updateDueSlotForTargetId_WithTableCleaning(sms.getSmsSet().getTargetId(), dueSlot);
            }
            sms.setDueSlot(dueSlot);

            done = this.c2_scheduleMessage(sms, dueSlot);
        }

        if (!done) {
            logger.warn("5 retries of c2_scheduleMessage fails for targetId=" + sms.getSmsSet().getTargetId());
        }
    }

    /**
     * @param smsSet
     * @param dueSlot
     * @return
     * return false if dueSlot is out <= ProcessingDueSlot
     * @throws PersistenceException 
     */
    public boolean c2_scheduleMessage(Sms sms, long dueSlot) throws PersistenceException {

        if (!sms.getStored())
            return true;

        sms.setDueSlot(dueSlot);

        Date dt = this.c2_getTimeForDueSlot(dueSlot);

        // special case for ScheduleDeliveryTime
        Date schedTime = sms.getScheduleDeliveryTime();
        if (schedTime != null && schedTime.after(dt)) {
            dueSlot = this.c2_getDueSlotForTime(schedTime);
        }

        // checking validity date
        if (sms.getValidityPeriod() != null && sms.getValidityPeriod().before(dt))
            return true;

        this.c2_registerDueSlotWriting(dueSlot);
        try {
            if (dueSlot <= this.c2_getCurrentDueSlot()) {
                return false;
            } else {
                this.c2_createRecordCurrent(sms);
                return true;
            }
        } finally {
            this.c2_unregisterDueSlotWriting(dueSlot);
        }
    }

    public void c2_createRecordCurrent(Sms sms) throws PersistenceException {
        long dueSlot = sms.getDueSlot();
        PreparedStatementCollection_C3 psc = getStatementCollection(dueSlot);

        try {
            PreparedStatement ps = psc.createRecordCurrent;
            BoundStatement boundStatement = new BoundStatement(ps);

            setSmsFields(sms, dueSlot, boundStatement, false);

            ResultSet res = session.execute(boundStatement);
        } catch (Exception e1) {
            String msg = "Failed createRecordCurrent !" + e1.getMessage();

            throw new PersistenceException(msg, e1);
        }
    }

    public void c2_createRecordArchive(Sms sms) throws PersistenceException {
        Date deliveryDate = sms.getDeliverDate();
        if (deliveryDate == null)
            deliveryDate = new Date();
        long dueSlot = this.c2_getDueSlotForTime(deliveryDate);
        PreparedStatementCollection_C3 psc = getStatementCollection(deliveryDate);

        try {
            PreparedStatement ps = psc.createRecordArchive;
            BoundStatement boundStatement = new BoundStatement(ps);

            setSmsFields(sms, dueSlot, boundStatement, true);

            ResultSet res = session.execute(boundStatement);
        } catch (Exception e1) {
            String msg = "Failed createRecordArchive !" + e1.getMessage();

            throw new PersistenceException(msg, e1);
        }
    }

    private void setSmsFields(Sms sms, long dueSlot, BoundStatement boundStatement, boolean archive) throws PersistenceException {
        boundStatement.setUUID(Schema.COLUMN_ID, sms.getDbId());
        boundStatement.setString(Schema.COLUMN_TARGET_ID, sms.getSmsSet().getTargetId());
        boundStatement.setLong(Schema.COLUMN_DUE_SLOT, dueSlot);
        boundStatement.setInt(Schema.COLUMN_IN_SYSTEM, IN_SYSTEM_UNSENT);
        boundStatement.setUUID(Schema.COLUMN_SMSC_UUID, emptyUuid);

        boundStatement.setString(Schema.COLUMN_ADDR_DST_DIGITS, sms.getSmsSet().getDestAddr());
        boundStatement.setInt(Schema.COLUMN_ADDR_DST_TON, sms.getSmsSet().getDestAddrTon());
        boundStatement.setInt(Schema.COLUMN_ADDR_DST_NPI, sms.getSmsSet().getDestAddrNpi());
        if (sms.getSourceAddr() != null) {
            boundStatement.setString(Schema.COLUMN_ADDR_SRC_DIGITS, sms.getSourceAddr());
        }
        boundStatement.setInt(Schema.COLUMN_ADDR_SRC_TON, sms.getSourceAddrTon());
        boundStatement.setInt(Schema.COLUMN_ADDR_SRC_NPI, sms.getSourceAddrNpi());

        boundStatement.setInt(Schema.COLUMN_DUE_DELAY, sms.getSmsSet().getDueDelay());
        if (sms.getSmsSet().getStatus() != null)
            boundStatement.setInt(Schema.COLUMN_SM_STATUS, sms.getSmsSet().getStatus().getCode());
        boundStatement.setBool(Schema.COLUMN_ALERTING_SUPPORTED, sms.getSmsSet().isAlertingSupported());

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
        if (sms.getDeliverDate() != null) {
            boundStatement.setDate(Schema.COLUMN_DELIVERY_DATE, sms.getDeliverDate());
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

        if (archive) {
            if (sms.getSmsSet().getImsi() != null) {
                boundStatement.setString(Schema.COLUMN_IMSI, sms.getSmsSet().getImsi().getData());
            }
            if (sms.getSmsSet().getLocationInfoWithLMSI() != null && sms.getSmsSet().getLocationInfoWithLMSI().getNetworkNodeNumber() != null) {
                boundStatement.setString(Schema.COLUMN_NNN_DIGITS, sms.getSmsSet().getLocationInfoWithLMSI().getNetworkNodeNumber().getAddress());
                boundStatement.setInt(Schema.COLUMN_NNN_AN, sms.getSmsSet().getLocationInfoWithLMSI().getNetworkNodeNumber().getAddressNature().getIndicator());
                boundStatement.setInt(Schema.COLUMN_NNN_NP, sms.getSmsSet().getLocationInfoWithLMSI().getNetworkNodeNumber().getNumberingPlan().getIndicator());
            }
            if (sms.getSmsSet().getType() != null) {
                boundStatement.setInt(Schema.COLUMN_SM_TYPE, sms.getSmsSet().getType().getCode());
            }
        }
    }

    public ArrayList<SmsSet> c2_getRecordList(long dueSlot) throws PersistenceException {
        PreparedStatementCollection_C3 psc = getStatementCollection(dueSlot);

        ArrayList<SmsSet> result = new ArrayList<SmsSet>();
        try {
            PreparedStatement ps = psc.getRecordData;
            BoundStatement boundStatement = new BoundStatement(ps);
            boundStatement.bind(dueSlot);
            ResultSet res = session.execute(boundStatement);

            for (Row row : res) {
                SmsSet smsSet = this.createSms(row, null);
                if (smsSet != null)
                    result.add(smsSet);
            }
        } catch (Exception e1) {
            String msg = "Failed getRecordList()";

            throw new PersistenceException(msg, e1);
        }

        return result;
    }

    public SmsSet c2_getRecordListForTargeId(long dueSlot, String targetId) throws PersistenceException {
        PreparedStatementCollection_C3 psc = getStatementCollection(dueSlot);

        SmsSet result = null;
        try {
            PreparedStatement ps = psc.getRecordData2;
            BoundStatement boundStatement = new BoundStatement(ps);
            boundStatement.bind(dueSlot, targetId);
            ResultSet res = session.execute(boundStatement);

            for (Row row : res) {
                result = this.createSms(row, result);
            }
        } catch (Exception e1) {
            String msg = "Failed getRecordListForTargeId()";

            throw new PersistenceException(msg, e1);
        }

        return result;
    }

    protected SmsSet createSms(final Row row, SmsSet smsSet) throws PersistenceException {
        if (row == null)
            return null;

        int inSystem = row.getInt(Schema.COLUMN_IN_SYSTEM);
        UUID smscUuid = row.getUUID(Schema.COLUMN_SMSC_UUID);
        if (inSystem == IN_SYSTEM_SENT || inSystem == IN_SYSTEM_INPROCESS && smscUuid.equals(currentSessionUUID)) {
            // inSystem it is in processing or processed - skip this
            return null;
        }

        Sms sms = new Sms();
        sms.setStored(true);
        sms.setDbId(row.getUUID(Schema.COLUMN_ID));
        sms.setDueSlot(row.getLong(Schema.COLUMN_DUE_SLOT));

        sms.setSourceAddr(row.getString(Schema.COLUMN_ADDR_SRC_DIGITS));
        sms.setSourceAddrTon(row.getInt(Schema.COLUMN_ADDR_SRC_TON));
        sms.setSourceAddrNpi(row.getInt(Schema.COLUMN_ADDR_SRC_NPI));

        sms.setMessageId(row.getLong(Schema.COLUMN_MESSAGE_ID));
        sms.setMoMessageRef(row.getInt(Schema.COLUMN_MO_MESSAGE_REF));
        sms.setOrigEsmeName(row.getString(Schema.COLUMN_ORIG_ESME_NAME));
        sms.setOrigSystemId(row.getString(Schema.COLUMN_ORIG_SYSTEM_ID));
        sms.setSubmitDate(row.getDate(Schema.COLUMN_SUBMIT_DATE));

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

        if (smsSet == null) {
            smsSet = new SmsSet();

            String destAddr = null;
            int destAddrTon = -1;
            int destAddrNpi = -1;

            destAddr = row.getString(Schema.COLUMN_ADDR_DST_DIGITS);
            destAddrTon = row.getInt(Schema.COLUMN_ADDR_DST_TON);
            destAddrNpi = row.getInt(Schema.COLUMN_ADDR_DST_NPI);

            if (destAddr == null || destAddrTon == -1 || destAddrNpi == -1) {
                throw new PersistenceException("destAddr or destAddrTon or destAddrNpi is absent for ID='" + sms.getDbId() + "'");
            }
            smsSet.setDestAddr(destAddr);
            smsSet.setDestAddrTon(destAddrTon);
            smsSet.setDestAddrNpi(destAddrNpi);
        }
        int dueDelay = row.getInt(Schema.COLUMN_DUE_DELAY);
        if (dueDelay > smsSet.getDueDelay())
            smsSet.setDueDelay(dueDelay);

        smsSet.addSms(sms);

        return smsSet;
    }

    public ArrayList<SmsSet> c2_sortRecordList(ArrayList<SmsSet> sourceLst) {
        FastMap<String, SmsSet> res = new FastMap<String, SmsSet>();

        // aggregating messages for one targetId
        for (SmsSet smsSet : sourceLst) {
            SmsSet smsSet2 = null;
            try {
                smsSet2 = res.get(smsSet.getTargetId());
            } catch (Throwable e) {
                int dd = 0;
            }
            if (smsSet2 != null) {
                smsSet2.addSms(smsSet.getSms(0));
            } else {
                res.put(smsSet.getTargetId(), smsSet);
            }
        }
 
        // adding into SmsSetCashe
        ArrayList<SmsSet> res2 = new ArrayList<SmsSet>();
        // 60 min timeout
        Date timeOutDate = new Date(new Date().getTime() - 1000 * 60 * 30); 
        for (SmsSet smsSet : res.values()) {
            smsSet.resortSms();

            TargetAddress lock = SmsSetCashe.getInstance().addSmsSet(new TargetAddress(smsSet));
            try {
                SmsSet smsSet2;
                synchronized (lock) {
                    smsSet2 = SmsSetCashe.getInstance().getProcessingSmsSet(smsSet.getTargetId());
                    if (smsSet2 != null) {
                        if (smsSet2.getCreationTime().after(timeOutDate)) {
                            for (int i1 = 0; i1 < smsSet.getSmsCount(); i1++) {
                                smsSet2.addSms(smsSet.getSms(i1));
                            }
                        } else {
                            logger.warn("Timeout of SmsSet in ProcessingSmsSet: targetId=" + smsSet2.getTargetId() + ", messageCount=" + smsSet2.getSmsCount());
                            smsSet2 = smsSet;
                            SmsSetCashe.getInstance().addProcessingSmsSet(smsSet2.getTargetId(), smsSet2, processingSmsSetTimeout);
                        }
                    } else {
                        smsSet2 = smsSet;
                        SmsSetCashe.getInstance().addProcessingSmsSet(smsSet2.getTargetId(), smsSet2, processingSmsSetTimeout);
                    }
//                    if (smsSet2 != null && smsSet2.getCreationTime().after(timeOutDate)) {
//                        for (int i1 = 0; i1 < smsSet.getSmsCount(); i1++) {
//                            smsSet2.addSms(smsSet.getSms(i1));
//                        }
//                    } else {
//                        smsSet2 = smsSet;
//                        SmsSetCashe.getInstance().addProcessingSmsSet(smsSet2.getTargetId(), smsSet2);
//                    }
                }
                res2.add(smsSet2);
            } finally {
                SmsSetCashe.getInstance().removeSmsSet(lock);
            }
        }

        return res2;
    }

    public void c2_updateInSystem(Sms sms, int isSystemStatus) throws PersistenceException {
        PreparedStatementCollection_C3 psc = this.getStatementCollection(sms.getDueSlot());

        try {
            PreparedStatement ps = psc.updateInSystem;
            BoundStatement boundStatement = new BoundStatement(ps);
            boundStatement.bind(isSystemStatus, currentSessionUUID, sms.getDueSlot(), sms.getSmsSet().getTargetId(), sms.getDbId());
            ResultSet res = session.execute(boundStatement);
        } catch (Exception e1) {
            String msg = "Failed to execute updateInSystem() !";
            throw new PersistenceException(msg, e1);
        }
    }

    public void createArchiveMessage(Sms sms) throws PersistenceException {
        // TODO: ......................
        // .....................................
    }

    protected PreparedStatementCollection_C3 getStatementCollection(Date dt) throws PersistenceException {
        String tName = this.getTableName(dt);
        PreparedStatementCollection_C3 psc = dataTableRead.get(tName);
        if (psc != null)
            return psc;

        return doGetStatementCollection(tName);
    }

    protected PreparedStatementCollection_C3 getStatementCollection(long deuSlot) throws PersistenceException {
        String tName = this.getTableName(deuSlot);
        PreparedStatementCollection_C3 psc = dataTableRead.get(tName);
        if (psc != null)
            return psc;

        return doGetStatementCollection(tName);
    }

    private synchronized PreparedStatementCollection_C3 doGetStatementCollection(String tName) throws PersistenceException {
        PreparedStatementCollection_C3 psc = dataTableRead.get(tName);
        if (psc != null)
            return psc;

        try {
            try {
                // checking if a datatable exists
                String s1 = "SELECT * FROM \"" + Schema.FAMILY_DST_SLOT_TABLE + tName + "\";";
                PreparedStatement ps = session.prepare(s1);
            } catch (InvalidQueryException e) {
                // datatable does not exist

                // DST_SLOT_TABLE
                StringBuilder sb = new StringBuilder();
                sb.append("CREATE TABLE \"" + Schema.FAMILY_DST_SLOT_TABLE);
                sb.append(tName);
                sb.append("\" (");

                appendField(sb, Schema.COLUMN_TARGET_ID, "ascii");
                appendField(sb, Schema.COLUMN_DUE_SLOT, "bigint");

                sb.append("PRIMARY KEY (\"");
                sb.append(Schema.COLUMN_TARGET_ID);
                sb.append("\"");
                sb.append("));");

                String s2 = sb.toString();
                PreparedStatement ps = session.prepare(s2);
                BoundStatement boundStatement = new BoundStatement(ps);
                ResultSet res = session.execute(boundStatement);

                // SLOT_MESSAGES_TABLE
                sb = new StringBuilder();
                sb.append("CREATE TABLE \"" + Schema.FAMILY_SLOT_MESSAGES_TABLE);
                sb.append(tName);
                sb.append("\" (");

                addSmsFields(sb);

                sb.append("PRIMARY KEY ((\"");
                sb.append(Schema.COLUMN_DUE_SLOT);
                sb.append("\"), \"");
                sb.append(Schema.COLUMN_TARGET_ID);
                sb.append("\", \"");
                sb.append(Schema.COLUMN_ID);
                sb.append("\"");
                sb.append("));");

                s2 = sb.toString();
                ps = session.prepare(s2);
                boundStatement = new BoundStatement(ps);
                res = session.execute(boundStatement);

                // MESSAGES
                sb = new StringBuilder();
                sb.append("CREATE TABLE \"" + Schema.FAMILY_MESSAGES);
                sb.append(tName);
                sb.append("\" (");

                addSmsFields(sb);

                sb.append("PRIMARY KEY ((\"");
                sb.append(Schema.COLUMN_ADDR_DST_DIGITS);
                sb.append("\"), \"");
                sb.append(Schema.COLUMN_ID);
                sb.append("\"");
                sb.append("));");

                s2 = sb.toString();
                ps = session.prepare(s2);
                boundStatement = new BoundStatement(ps);
                res = session.execute(boundStatement);
            }
        } catch (Exception e1) {
            String msg = "Failed to access or create table " + tName + "!";
            throw new PersistenceException(msg, e1);
        }

        psc = new PreparedStatementCollection_C3(this, tName, ttlCurrent, ttlArchive);
        dataTableRead.putEntry(tName, psc);
        return psc;
    }

    private void addSmsFields(StringBuilder sb) {
        appendField(sb, Schema.COLUMN_ID, "uuid");
        appendField(sb, Schema.COLUMN_TARGET_ID, "ascii");
        appendField(sb, Schema.COLUMN_DUE_SLOT, "bigint");
        appendField(sb, Schema.COLUMN_IN_SYSTEM, "int");
        appendField(sb, Schema.COLUMN_SMSC_UUID, "uuid");

        appendField(sb, Schema.COLUMN_ADDR_DST_DIGITS, "ascii");
        appendField(sb, Schema.COLUMN_ADDR_DST_TON, "int");
        appendField(sb, Schema.COLUMN_ADDR_DST_NPI, "int");

        appendField(sb, Schema.COLUMN_ADDR_SRC_DIGITS, "ascii");
        appendField(sb, Schema.COLUMN_ADDR_SRC_TON, "int");
        appendField(sb, Schema.COLUMN_ADDR_SRC_NPI, "int");

        appendField(sb, Schema.COLUMN_DUE_DELAY, "int");
        appendField(sb, Schema.COLUMN_ALERTING_SUPPORTED, "boolean");

        appendField(sb, Schema.COLUMN_MESSAGE_ID, "bigint");
        appendField(sb, Schema.COLUMN_MO_MESSAGE_REF, "int");
        appendField(sb, Schema.COLUMN_ORIG_ESME_NAME, "text");
        appendField(sb, Schema.COLUMN_ORIG_SYSTEM_ID, "text");
        appendField(sb, Schema.COLUMN_DEST_CLUSTER_NAME, "text");
        appendField(sb, Schema.COLUMN_DEST_ESME_NAME, "text");
        appendField(sb, Schema.COLUMN_DEST_SYSTEM_ID, "text");
        appendField(sb, Schema.COLUMN_SUBMIT_DATE, "timestamp");
        appendField(sb, Schema.COLUMN_DELIVERY_DATE, "timestamp");

        appendField(sb, Schema.COLUMN_SERVICE_TYPE, "text");
        appendField(sb, Schema.COLUMN_ESM_CLASS, "int");
        appendField(sb, Schema.COLUMN_PROTOCOL_ID, "int");
        appendField(sb, Schema.COLUMN_PRIORITY, "int");
        appendField(sb, Schema.COLUMN_REGISTERED_DELIVERY, "int");
        appendField(sb, Schema.COLUMN_REPLACE, "int");
        appendField(sb, Schema.COLUMN_DATA_CODING, "int");
        appendField(sb, Schema.COLUMN_DEFAULT_MSG_ID, "int");

        appendField(sb, Schema.COLUMN_MESSAGE, "blob");
        appendField(sb, Schema.COLUMN_OPTIONAL_PARAMETERS, "text");
        appendField(sb, Schema.COLUMN_SCHEDULE_DELIVERY_TIME, "timestamp");
        appendField(sb, Schema.COLUMN_VALIDITY_PERIOD, "timestamp");

        appendField(sb, Schema.COLUMN_IMSI, "ascii");
        appendField(sb, Schema.COLUMN_NNN_DIGITS, "ascii");
        appendField(sb, Schema.COLUMN_NNN_AN, "int");
        appendField(sb, Schema.COLUMN_NNN_NP, "int");
        appendField(sb, Schema.COLUMN_SM_STATUS, "int");
        appendField(sb, Schema.COLUMN_SM_TYPE, "int");
        appendField(sb, Schema.COLUMN_DELIVERY_COUNT, "int");
    }

    private synchronized void checkCurrentSlotTableExists() throws PersistenceException {
        try {
            try {
                // checking of CURRENT_SLOT_TABLE existence
                String sa = "SELECT \"" + Schema.COLUMN_NEXT_SLOT + "\" FROM \"" + Schema.FAMILY_CURRENT_SLOT_TABLE + "\" where \"" + Schema.COLUMN_ID
                        + "\"=0;";
                PreparedStatement ps = session.prepare(sa);
            } catch (InvalidQueryException e) {
                StringBuilder sb = new StringBuilder();
                sb.append("CREATE TABLE \"");
                sb.append(Schema.FAMILY_CURRENT_SLOT_TABLE);
                sb.append("\" (");

                appendField(sb, Schema.COLUMN_ID, "int");
                appendField(sb, Schema.COLUMN_NEXT_SLOT, "bigint");

                sb.append("PRIMARY KEY (\"");
                sb.append(Schema.COLUMN_ID);
                sb.append("\"");
                sb.append("));");

                String s2 = sb.toString();
                PreparedStatement ps = session.prepare(s2);
                BoundStatement boundStatement = new BoundStatement(ps);
                ResultSet res = session.execute(boundStatement);
            }
        } catch (Exception e1) {
            String msg = "Failed to access or create table " + Schema.FAMILY_CURRENT_SLOT_TABLE + "!";
            throw new PersistenceException(msg, e1);
        }
    }

    private void appendField(StringBuilder sb, String name, String type) {
        sb.append("\"");
        sb.append(name);
        sb.append("\" ");
        sb.append(type);
        sb.append(", ");
    }

    private void appendIndex(String tName, String fieldName) {
        StringBuilder sb = new StringBuilder();
        sb.append("CREATE INDEX ON \"");
        sb.append(tName);
        sb.append("\" (\"");
        sb.append(fieldName);
        sb.append("\");");

        String s2 = sb.toString();
        PreparedStatement ps = session.prepare(s2);
        BoundStatement boundStatement = new BoundStatement(ps);
        ResultSet res = session.execute(boundStatement);
    }

    private class DueSlotWritingElement {
        public long dueSlot;
        public int writingCount;
        public Date lastStartDate;

        public DueSlotWritingElement(long dueSlot) {
            this.dueSlot = dueSlot;
        }
    }
}
