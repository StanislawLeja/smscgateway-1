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

package org.mobicents.smsc.slee.resources.exposurelayer.persistence;


import org.mobicents.smsc.cassandra.PersistenceException;
import org.mobicents.smsc.cassandra.PreparedStatementCollection;
import org.mobicents.smsc.library.QuerySmResponse;
import org.mobicents.smsc.library.Sms;
import org.mobicents.smsc.library.SmsSet;
import org.mobicents.smsc.library.TargetAddress;
import org.mobicents.smsc.mproc.impl.PersistenseCommonInterface;

import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;

/**
 * @author Stanislaw Leja
 *
 */
public interface PersistenceRAInterface extends PersistenseCommonInterface {

    boolean isDatabaseAvailable();
    // C2
    /**
     * Return due_slot for the given time
     */
    long c2_getDueSlotForTime(Date time);

    /**
     * Return time for the given due_slot
     */
    Date c2_getTimeForDueSlot(long dueSlot);

    /**
     * Return due_slop that SMSC is processing now
     */
    long c2_getCurrentDueSlot();

    /**
     * Set a new due_slop that SMSC is processing now and store it to the database
     */
    void c2_setCurrentDueSlot(long newDueSlot) throws PersistenceException;

    /**
     * Return next messageId for a new incoming message
     */
    long c2_getNextMessageId();

    /**
     * Return due_slop for current time
     */
    long c2_getIntimeDueSlot();

    /**
     * Return due_slop for storing next incoming to SMSC message
     */
    long c2_getDueSlotForNewSms();

    long c2_checkDueSlotWritingPossibility(long dueSlot);

    /**
     * Registering that thread starts writing to this due_slot
     */
    void c2_registerDueSlotWriting(long dueSlot);

    /**
     * Registering that thread finishes writing to this due_slot
     */
    void c2_unregisterDueSlotWriting(long dueSlot);

    /**
     * Checking if due_slot is not in writing state now
     * Returns true if due_slot is not in writing now
     */
    boolean c2_checkDueSlotNotWriting(long dueSlot);

    /**
     * Obtaining synchronizing object for a TargetAddress
     */
    TargetAddress obtainSynchroObject(TargetAddress ta);

    /**
     * Releasing synchronizing object for a TargetAddress
     */
    void releaseSynchroObject(TargetAddress ta);

    PreparedStatementCollection[] c2_getPscList() throws PersistenceException;


    long c2_getDueSlotForTargetId(String targetId) throws PersistenceException;

    long c2_getDueSlotForTargetId(PreparedStatementCollection psc, String targetId) throws PersistenceException;

    void c2_updateDueSlotForTargetId(String targetId, long newDueSlot) throws PersistenceException;

    void c2_updateDueSlotForTargetId_WithTableCleaning(String targetId, long newDueSlot) throws PersistenceException;

    void c2_createRecordCurrent(Sms sms) throws PersistenceException;

    void c2_createRecordArchive(Sms sms, String dlvMessageId, String dlvDestId, boolean deliveryReceipts,
            boolean incomingDeliveryReceipts) throws PersistenceException;

    void c2_scheduleMessage_ReschedDueSlot(Sms sms, boolean fastStoreAndForwordMode, boolean removeExpiredValidityPeriod) throws PersistenceException;

    void c2_scheduleMessage_NewDueSlot(Sms sms, long dueSlot, ArrayList<Sms> lstFailured, boolean fastStoreAndForwordMode) throws PersistenceException;

    ArrayList<SmsSet> c2_getRecordList(long dueSlot) throws PersistenceException;

    SmsSet c2_getRecordListForTargeId(long dueSlot, String targetId) throws PersistenceException;

    ArrayList<SmsSet> c2_sortRecordList(ArrayList<SmsSet> sourceLst);

    void c2_updateInSystem(Sms sms, int isSystemStatus, boolean fastStoreAndForwordMode) throws PersistenceException;

    void c2_updateAlertingSupport(long dueSlot, String targetId, UUID dbId) throws PersistenceException;

    Sms c2_getRecordArchiveForMessageId(long messageId) throws PersistenceException;

    QuerySmResponse c2_getQuerySmResponse(long messageId) throws PersistenceException;

    Long c2_getMessageIdByRemoteMessageId(String remoteMessageId, String destId) throws PersistenceException;

}
