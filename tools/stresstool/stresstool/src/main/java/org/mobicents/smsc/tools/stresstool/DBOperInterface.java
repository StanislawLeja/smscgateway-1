package org.mobicents.smsc.tools.stresstool;

import java.util.Date;
import java.util.List;

import org.mobicents.smsc.cassandra.PersistenceException;
import org.mobicents.smsc.cassandra.Sms;
import org.mobicents.smsc.cassandra.SmsSet;

public interface DBOperInterface {

    long calculateSlot(Date dt);

    void createRecord(long dueSlot, Sms sms) throws PersistenceException;

    List<LoadedTargetId> getTargetIdListForDueSlot(Date[] dt, long dueSlot, long newDueSlot, int maxRecordCount) throws PersistenceException;

    SmsSet getSmsSetForTargetId(Date[] dtt, LoadedTargetId targetId) throws PersistenceException;

    void deleteIdFromDests(Sms sms, long dueSlot) throws PersistenceException;

}
