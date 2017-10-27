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

import org.mobicents.smsc.cassandra.DBOperations;
import org.mobicents.smsc.cassandra.PersistenceException;
import org.mobicents.smsc.cassandra.PreparedStatementCollection;
import org.mobicents.smsc.library.*;

import javax.slee.Address;
import javax.slee.facilities.Tracer;
import javax.slee.resource.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;

/**
 * 
 * @author Stanislaw Leja
 * 
 */
public class PersistenceResourceAdaptor implements ResourceAdaptor {

	private static final String CONF_CLUSTER_NAME = "cluster.name";
	private static final String CONF_CLUSTER_HOSTS = "cluster.hosts";
	private static final String CONF_CLUSTER_KEYSPACE = "cluster.keyspace";

	private Tracer tracer = null;
	private ResourceAdaptorContext raContext = null;

//    private DBOperations_C1 dbOperations_C1 = null;
    private DBOperations dbOperations_C2 = null;

	// this is to avoid wicked SLEE spec - it mandates this.raSbbInterface to be
	// available before RA starts...
	private PersistenceRAInterface raSbbInterface;

	// private String hosts = null;
	// private String keyspaceName = null;
	// private String clusterName = null;

	public PersistenceResourceAdaptor() {
		this.raSbbInterface = new PersistenceRAInterface() {

            @Override
            public TargetAddress obtainSynchroObject(TargetAddress ta) {
                return SmsSetCache.getInstance().addSmsSet(ta);
            }

            @Override
            public void releaseSynchroObject(TargetAddress ta) {
                SmsSetCache.getInstance().removeSmsSet(ta);
            }

            public boolean isDatabaseAvailable() {
                if (dbOperations_C2 == null)
                    return false;
                else
                    return dbOperations_C2.isDatabaseAvailable();
            }

		    // C2

		    @Override
            public long c2_getDueSlotForTime(Date time) {
                return dbOperations_C2.c2_getDueSlotForTime(time);
            }

            @Override
            public Date c2_getTimeForDueSlot(long dueSlot) {
                return dbOperations_C2.c2_getTimeForDueSlot(dueSlot);
            }

            @Override
            public long c2_getCurrentDueSlot() {
                return dbOperations_C2.c2_getCurrentDueSlot();
            }

            @Override
            public void c2_setCurrentDueSlot(long newDueSlot) throws PersistenceException {
                dbOperations_C2.c2_setCurrentDueSlot(newDueSlot);
            }

            @Override
            public long c2_getNextMessageId() {
                return dbOperations_C2.c2_getNextMessageId();
            }

            @Override
            public long c2_getIntimeDueSlot() {
                return dbOperations_C2.c2_getIntimeDueSlot();
            }

            @Override
            public long c2_getDueSlotForNewSms() {
                return dbOperations_C2.c2_getDueSlotForNewSms();
            }

            @Override
            public void c2_registerDueSlotWriting(long dueSlot) {
                dbOperations_C2.c2_registerDueSlotWriting(dueSlot);
            }

            @Override
            public void c2_unregisterDueSlotWriting(long dueSlot) {
                dbOperations_C2.c2_unregisterDueSlotWriting(dueSlot);
            }

            @Override
            public boolean c2_checkDueSlotNotWriting(long dueSlot) {
                return dbOperations_C2.c2_checkDueSlotNotWriting(dueSlot);
            }

            @Override
            public long c2_getDueSlotForTargetId(String targetId) throws PersistenceException {
                return dbOperations_C2.c2_getDueSlotForTargetId(targetId);
            }

            @Override
            public long c2_getDueSlotForTargetId(PreparedStatementCollection psc, String targetId) throws PersistenceException {
                return dbOperations_C2.c2_getDueSlotForTargetId(psc, targetId);
            }

            @Override
            public void c2_updateDueSlotForTargetId(String targetId, long newDueSlot) throws PersistenceException {
                dbOperations_C2.c2_updateDueSlotForTargetId(targetId, newDueSlot);
            }

            @Override
            public void c2_updateDueSlotForTargetId_WithTableCleaning(String targetId, long newDueSlot) throws PersistenceException {
                dbOperations_C2.c2_updateDueSlotForTargetId_WithTableCleaning(targetId, newDueSlot);
            }

            @Override
            public void c2_createRecordCurrent(Sms sms) throws PersistenceException {
                dbOperations_C2.c2_createRecordCurrent(sms);
            }

            @Override
            public void c2_createRecordArchive(Sms sms, String dlvMessageId, String dlvDestId, boolean deliveryReceipts,
                    boolean incomingDeliveryReceipts) throws PersistenceException {
                dbOperations_C2
                        .c2_createRecordArchive(sms, dlvMessageId, dlvDestId, deliveryReceipts, incomingDeliveryReceipts);
            }

            @Override
            public ArrayList<SmsSet> c2_getRecordList(long dueSlot) throws PersistenceException {
                return dbOperations_C2.c2_getRecordList(dueSlot);
            }

            @Override
            public SmsSet c2_getRecordListForTargeId(long dueSlot, String targetId) throws PersistenceException {
                return dbOperations_C2.c2_getRecordListForTargeId(dueSlot, targetId);
            }

            @Override
            public ArrayList<SmsSet> c2_sortRecordList(ArrayList<SmsSet> sourceLst) {
                return dbOperations_C2.c2_sortRecordList(sourceLst);
            }

            @Override
            public void c2_updateInSystem(Sms sms, int isSystemStatus, boolean fastStoreAndForwordMode) throws PersistenceException {
                dbOperations_C2.c2_updateInSystem(sms, isSystemStatus, fastStoreAndForwordMode);
            }

            @Override
            public void c2_updateAlertingSupport(long dueSlot, String targetId, UUID dbId) throws PersistenceException {
                dbOperations_C2.c2_updateAlertingSupport(dueSlot, targetId, dbId);
            }

            @Override
            public PreparedStatementCollection[] c2_getPscList() throws PersistenceException {
                return dbOperations_C2.c2_getPscList();
            }

            @Override
            public void c2_scheduleMessage_ReschedDueSlot(Sms sms, boolean fastStoreAndForwordMode, boolean removeExpiredValidityPeriod)
                    throws PersistenceException {
                dbOperations_C2.c2_scheduleMessage_ReschedDueSlot(sms, fastStoreAndForwordMode, removeExpiredValidityPeriod);
            }

            @Override
            public void c2_scheduleMessage_NewDueSlot(Sms sms, long dueSlot, ArrayList<Sms> lstFailured, boolean fastStoreAndForwordMode) throws PersistenceException {
                dbOperations_C2.c2_scheduleMessage_NewDueSlot(sms, dueSlot, lstFailured, fastStoreAndForwordMode);
            }

            @Override
            public long c2_checkDueSlotWritingPossibility(long dueSlot) {
                return dbOperations_C2.c2_checkDueSlotWritingPossibility(dueSlot);
            }

            @Override
            public Sms c2_getRecordArchiveForMessageId(long messageId) throws PersistenceException {
                return dbOperations_C2.c2_getRecordArchiveForMessageId(messageId);
            }

            @Override
            public QuerySmResponse c2_getQuerySmResponse(long messageId) throws PersistenceException {
                return dbOperations_C2.c2_getQuerySmResponse(messageId);
            }

            @Override
            public Long c2_getMessageIdByRemoteMessageId(String remoteMessageId, String destId) throws PersistenceException {
                return dbOperations_C2.c2_getMessageIdByRemoteMessageId(remoteMessageId, destId);
            }

		};
	}

	@Override
	public void activityEnded(ActivityHandle activityHandle) {
		if (this.tracer.isFineEnabled()) {
			this.tracer.fine("Activity with handle " + activityHandle + " ended.");
		}
	}

	@Override
	public void activityUnreferenced(ActivityHandle activityHandle) {
		if (this.tracer.isFineEnabled()) {
			this.tracer.fine("Activity unreferenced with handle " + activityHandle + ".");
		}
	}

	@Override
	public void administrativeRemove(ActivityHandle activityHandle) {
		if (this.tracer.isFineEnabled()) {
			this.tracer.fine("Activity administrative remove with handle " + activityHandle + ".");
		}
	}

	@Override
	public void eventProcessingFailed(ActivityHandle activityHandle, FireableEventType arg1, Object arg2, Address arg3,
			ReceivableService arg4, int arg5, FailureReason arg6) {
		if (this.tracer.isFineEnabled()) {
			this.tracer.fine("Event processing failed on activity with handle " + activityHandle + ".");
		}
	}

	@Override
	public void eventProcessingSuccessful(ActivityHandle activityHandle, FireableEventType arg1, Object arg2,
			Address arg3, ReceivableService arg4, int arg5) {
		if (this.tracer.isFineEnabled()) {
			this.tracer.fine("Event processing succeeded on activity with handle " + activityHandle + ".");
		}

	}

	@Override
	public void eventUnreferenced(ActivityHandle activityHandle, FireableEventType arg1, Object arg2, Address arg3,
			ReceivableService arg4, int arg5) {
		if (this.tracer.isFineEnabled()) {
			this.tracer.fine("Event unreferenced on activity with handle " + activityHandle + ".");
		}
	}

	@Override
	public Object getActivity(ActivityHandle activityHandle) {
		return null;
	}

	@Override
	public ActivityHandle getActivityHandle(Object activity) {
		return null;
	}

	@Override
	public Marshaler getMarshaler() {
		return null;
	}

	@Override
	public Object getResourceAdaptorInterface(String arg0) {
		return this.raSbbInterface;
	}

	@Override
	public void queryLiveness(ActivityHandle activityHandle) {

	}

	@Override
	public void raActive() {

//        dbOperations_C1 = DBOperations_C1.getInstance();
//		if (!this.dbOperations_C1.isStarted()) {
//			throw new RuntimeException("DBOperations_1 not started yet!");
//		}

		dbOperations_C2 = DBOperations.getInstance();
        if (!this.dbOperations_C2.isStarted()) {
            throw new RuntimeException("DBOperations_2 not started yet!");
        }

		if (tracer.isInfoEnabled()) {
			tracer.info("PersistenceResourceAdaptor " + this.raContext.getEntityName() + " Activated");
		}
	}

	@Override
	public void raConfigurationUpdate(ConfigProperties properties) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void raConfigure(ConfigProperties properties) {
		if (tracer.isFineEnabled()) {
			tracer.fine("Configuring RA Entity " + this.raContext.getEntityName());
		}
	}

	@Override
	public void raInactive() {

		if (tracer.isInfoEnabled()) {
			tracer.info("Inactivated RA Entity " + this.raContext.getEntityName());
		}
	}

	@Override
	public void raStopping() {
		if (tracer.isInfoEnabled()) {
			tracer.info("Stopping RA Entity " + this.raContext.getEntityName());
		}
	}

	@Override
	public void raUnconfigure() {
		if (tracer.isInfoEnabled()) {
			tracer.info("Unconfigure RA Entity " + this.raContext.getEntityName());
		}
	}

	@Override
	public void raVerifyConfiguration(ConfigProperties properties) throws InvalidConfigurationException {
		if (tracer.isInfoEnabled()) {
			tracer.info("Verify configuration in RA Entity " + this.raContext.getEntityName());
		}
	}

	@Override
	public void serviceActive(ReceivableService arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void serviceInactive(ReceivableService arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void serviceStopping(ReceivableService arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setResourceAdaptorContext(ResourceAdaptorContext raContext) {
		this.tracer = raContext.getTracer(getClass().getSimpleName());
		this.raContext = raContext;
	}

	@Override
	public void unsetResourceAdaptorContext() {
		this.raContext = null;
	}

}
