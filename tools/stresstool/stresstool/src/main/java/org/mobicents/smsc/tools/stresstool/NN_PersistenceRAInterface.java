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

import java.util.ArrayList;
import java.util.Date;
import org.mobicents.smsc.cassandra.PersistenceException;
import org.mobicents.smsc.cassandra.Sms;
import org.mobicents.smsc.cassandra.SmsSet;
import org.mobicents.smsc.cassandra.TargetAddress;

/**
*
* @author sergey vetyutnev
*
*/
public interface NN_PersistenceRAInterface {

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

    /**
     * Obtaining synchronizing object for a TargetAddress
     */
    TargetAddress obtainSynchroObject(TargetAddress ta);

    /**
     * Releasing synchronizing object for a TargetAddress
     */
    void releaseSynchroObject(TargetAddress ta);


    long getDueSlotForTargetId(PreparedStatementCollection psc, String targetId) throws PersistenceException;

    void updateDueSlotForTargetId(String targetId, long newDueSlot) throws PersistenceException;

    void createRecordCurrent(Sms sms) throws PersistenceException;

    void createRecordArchive(Sms sms) throws PersistenceException;

    ArrayList<SmsSet> getRecordList(long dueSlot) throws PersistenceException;

    SmsSet getRecordListForTargeId(long dueSlot, String targetId) throws PersistenceException;

    ArrayList<SmsSet> sortRecordList(ArrayList<SmsSet> sourceLst);

    void updateInSystem(Sms sms, int isSystemStatus) throws PersistenceException;

}
