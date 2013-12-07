package org.mobicents.smsc.tools.stresstool;

import org.mobicents.smsc.cassandra.Schema;

import com.datastax.driver.core.PreparedStatement;

public class PreparedStatementCollection {

    private DBOper2 dbOperation;
    private String tName;

    protected PreparedStatement createDueSlotForTargetId;
    protected PreparedStatement getDueSlotForTargetId;
    protected PreparedStatement createRecordCurrent;
    protected PreparedStatement getRecordData;
    protected PreparedStatement getRecordData2;

    public PreparedStatementCollection(DBOper2 dbOperation, String tName) {
        this.dbOperation = dbOperation;
        this.tName = tName;

        try {
            String s1 = getFillUpdateFields();
            String s2 = getFillUpdateFields2();
            String sa = "INSERT INTO \"" + Schema.FAMILY_DST_SLOT_TABLE + tName + "\" (\"" + Schema.COLUMN_TARGET_ID + "\", \"" + Schema.COLUMN_DUE_SLOT
                    + "\") VALUES (?, ?);";
            createDueSlotForTargetId = dbOperation.session.prepare(sa);
            sa = "SELECT \"" + Schema.COLUMN_DUE_SLOT + "\" FROM \"" + Schema.FAMILY_DST_SLOT_TABLE + tName + "\" where \"" + Schema.COLUMN_TARGET_ID
                    + "\"=?;";
            getDueSlotForTargetId = dbOperation.session.prepare(sa);
            sa = "INSERT INTO \"" + Schema.FAMILY_SLOT_MESSAGES_TABLE + tName + "\" (" + s1 + ") VALUES (" + s2 + ");";
            createRecordCurrent = dbOperation.session.prepare(sa);
            sa = "SELECT * FROM \"" + Schema.FAMILY_SLOT_MESSAGES_TABLE + tName + "\" where \"" + Schema.COLUMN_DUE_SLOT
                    + "\"=?;";
            getRecordData = dbOperation.session.prepare(sa);
            sa = "SELECT * FROM \"" + Schema.FAMILY_SLOT_MESSAGES_TABLE + tName + "\" where \"" + Schema.COLUMN_DUE_SLOT + "\"=? and \""
                    + Schema.COLUMN_TARGET_ID + "\"=?;";
            getRecordData2 = dbOperation.session.prepare(sa);


//            sa = "DELETE FROM \"" + Schema.FAMILY_SLOTS + tName + "\" where \"" + Schema.COLUMN_DUE_SLOT + "\"=? and \"" + Schema.COLUMN_TARGET_ID
//                    + "\"=?;";
//            sa = "UPDATE \"" + Schema.FAMILY_DESTS + tName + "\" SET \"" + Schema.COLUMN_SENT + "\"=true where \"" + Schema.COLUMN_TARGET_ID
//                    + "\"=? and \"" + Schema.COLUMN_ID + "\"=?;";
        } catch (Throwable e) {
            e.printStackTrace();
        }
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
        int cnt = 31;
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
