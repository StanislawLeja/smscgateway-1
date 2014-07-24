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

import com.datastax.driver.core.PreparedStatement;

public class PreparedStatementCollection_C3 {

    private DBOperations_C2 dbOperation;
    private String tName;
    private boolean shortMessageNewStringFormat;

    protected PreparedStatement createDueSlotForTargetId;
    protected PreparedStatement getDueSlotForTargetId;
    protected PreparedStatement createRecordCurrent;
    protected PreparedStatement getRecordData;
    protected PreparedStatement getRecordData2;
    protected PreparedStatement updateInSystem;
    protected PreparedStatement updateAlertingSupport;
    protected PreparedStatement createRecordArchive;

    public PreparedStatementCollection_C3(DBOperations_C2 dbOperation, String tName, int ttlCurrent, int ttlArchive) {
        this.dbOperation = dbOperation;
        this.tName = tName;

        // check table version format
        try {
            shortMessageNewStringFormat = false;
            String s1 = "select \"" + Schema.COLUMN_MESSAGE_TEXT + "\" FROM \"" + Schema.FAMILY_SLOT_MESSAGES_TABLE + tName + "\" limit 1;";
            dbOperation.session.execute(s1);
            shortMessageNewStringFormat = true;
        } catch (Exception e) {
        }

        try {
            String s1 = getFillUpdateFields();
            String s2 = getFillUpdateFields2();
            String s3a, s3b;
            if (ttlCurrent > 0) {
                s3a = "USING TTL " + ttlCurrent;
            } else {
                s3a = "";
            }
            if (ttlArchive > 0) {
                s3b = "USING TTL " + ttlArchive;
            } else {
                s3b = "";
            }

            String sa = "INSERT INTO \"" + Schema.FAMILY_DST_SLOT_TABLE + tName + "\" (\"" + Schema.COLUMN_TARGET_ID + "\", \"" + Schema.COLUMN_DUE_SLOT
                    + "\") VALUES (?, ?) " + s3a + ";";
            createDueSlotForTargetId = dbOperation.session.prepare(sa);
            sa = "SELECT \"" + Schema.COLUMN_DUE_SLOT + "\" FROM \"" + Schema.FAMILY_DST_SLOT_TABLE + tName + "\" where \"" + Schema.COLUMN_TARGET_ID
                    + "\"=?;";
            getDueSlotForTargetId = dbOperation.session.prepare(sa);
            sa = "INSERT INTO \"" + Schema.FAMILY_SLOT_MESSAGES_TABLE + tName + "\" (" + s1 + ") VALUES (" + s2 + ") " + s3a + ";";
            createRecordCurrent = dbOperation.session.prepare(sa);
            sa = "SELECT * FROM \"" + Schema.FAMILY_SLOT_MESSAGES_TABLE + tName + "\" where \"" + Schema.COLUMN_DUE_SLOT
                    + "\"=?;";
            getRecordData = dbOperation.session.prepare(sa);
            sa = "SELECT * FROM \"" + Schema.FAMILY_SLOT_MESSAGES_TABLE + tName + "\" where \"" + Schema.COLUMN_DUE_SLOT + "\"=? and \""
                    + Schema.COLUMN_TARGET_ID + "\"=?;";
            getRecordData2 = dbOperation.session.prepare(sa);
            sa = "UPDATE \"" + Schema.FAMILY_SLOT_MESSAGES_TABLE + tName + "\" " + s3a + " SET \"" + Schema.COLUMN_IN_SYSTEM + "\"=?, \""
                    + Schema.COLUMN_SMSC_UUID + "\"=? where \"" + Schema.COLUMN_DUE_SLOT + "\"=? and \"" + Schema.COLUMN_TARGET_ID + "\"=? and \""
                    + Schema.COLUMN_ID + "\"=?;";
            updateInSystem = dbOperation.session.prepare(sa);
            sa = "UPDATE \"" + Schema.FAMILY_SLOT_MESSAGES_TABLE + tName + "\" " + s3a + " SET \"" + Schema.COLUMN_ALERTING_SUPPORTED + "\"=? where \""
                    + Schema.COLUMN_DUE_SLOT + "\"=? and \"" + Schema.COLUMN_TARGET_ID + "\"=? and \"" + Schema.COLUMN_ID + "\"=?;";
            updateAlertingSupport = dbOperation.session.prepare(sa);
            sa = "INSERT INTO \"" + Schema.FAMILY_MESSAGES + tName + "\" (" + s1 + ", \"" + Schema.COLUMN_IMSI + "\", \"" + Schema.COLUMN_NNN_DIGITS + "\", \""
                    + Schema.COLUMN_NNN_AN + "\", \"" + Schema.COLUMN_NNN_NP + "\", \"" + Schema.COLUMN_SM_TYPE + "\") VALUES (" + s2 + ", ?, ?, ?, ?, ?) "
                    + s3b + ";";
            createRecordArchive = dbOperation.session.prepare(sa);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public String getTName() {
        return tName;
    }

    public boolean getShortMessageNewStringFormat() {
        return shortMessageNewStringFormat;
    }

    private String getFillUpdateFields() {
        StringBuilder sb = new StringBuilder();

        sb.append("\"");
        sb.append(Schema.COLUMN_ID);
        sb.append("\", \"");
        sb.append(Schema.COLUMN_TARGET_ID);
        sb.append("\", \"");
        sb.append(Schema.COLUMN_DUE_SLOT);
        sb.append("\", \"");
        sb.append(Schema.COLUMN_IN_SYSTEM);
        sb.append("\", \"");
        sb.append(Schema.COLUMN_SMSC_UUID);
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

        sb.append(Schema.COLUMN_DUE_DELAY);
        sb.append("\", \"");
        sb.append(Schema.COLUMN_SM_STATUS);
        sb.append("\", \"");
        sb.append(Schema.COLUMN_ALERTING_SUPPORTED);
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
        sb.append(Schema.COLUMN_DELIVERY_DATE);
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
        if (shortMessageNewStringFormat) {
            sb.append(Schema.COLUMN_MESSAGE_TEXT);
            sb.append("\", \"");
            sb.append(Schema.COLUMN_MESSAGE_BIN);
            sb.append("\", \"");
        }
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
        int cnt;
        if (shortMessageNewStringFormat) {
            cnt = 35;
        } else {
            cnt = 33;
        }

        StringBuilder sb = new StringBuilder();
        int i2 = 0;
        for (int i1 = 0; i1 < cnt; i1++) {
            if (i2 == 0)
                i2 = 1;
            else
                sb.append(", ");
            sb.append("?");
        }
        return sb.toString();
    }

}
