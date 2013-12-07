package org.mobicents.smsc.tools.stresstool;

import java.util.ArrayList;
import java.util.Date;
import org.mobicents.smsc.cassandra.PersistenceException;
import org.mobicents.smsc.cassandra.Sms;
import org.mobicents.smsc.cassandra.SmsSet;

public interface DBOperInterface3 {

    /**
     * Return due_slot for the given time
     */
    long getDueSlotForTime(Date time);

    /**
     * Return time for the given due_slot
     */
    Date getTimeForDueSlot(long dueSlot);

    /**
     * Return due_slop that SMSC is processing now
     */
    long getProcessingDueSlot();

    /**
     * Set a new due_slop that SMSC is processing now and store it to the database
     */
    void setProcessingDueSlot(long newDueSlot) throws PersistenceException;

    /**
     * Return due_slop for current time
     */
    long getIntimeDueSlot();

    /**
     * Return due_slop for storing next incoming to SMSC message
     */
    long getStoringDueSlot();

    /**
     * Registering that thread starts writing to this due_slot
     */
    void registerDueSlotWriting(long dueSlot);

    /**
     * Registering that thread finishes writing to this due_slot
     */
    void unregisterDueSlotWriting(long dueSlot);

    /**
     * Checking if due_slot is not in writing state now
     * Returns true if due_slot is not in writing now
     */
    boolean checkDueSlotNotWriting(long dueSlot);


    long getDueSlotForTargetId(PreparedStatementCollection psc, String targetId) throws PersistenceException;

    void updateDueSlotForTargetId(String targetId, long newDueSlot) throws PersistenceException;

    void getRecordList(Sms sms) throws PersistenceException;

    SmsSet getRecordListForTargeId(long dueSlot, String targetId) throws PersistenceException;

    ArrayList<SmsSet> getRecordList(long dueSlot) throws PersistenceException;

    ArrayList<SmsSet> sortRecordList(ArrayList<SmsSet> sourceLst);

}
