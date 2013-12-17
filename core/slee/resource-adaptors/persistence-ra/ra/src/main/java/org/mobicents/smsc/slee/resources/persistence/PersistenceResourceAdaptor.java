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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.mobicents.protocols.ss7.map.api.primitives.IMSI;
import org.mobicents.protocols.ss7.map.api.service.sms.LocationInfoWithLMSI;
import org.mobicents.smsc.cassandra.SmType;
import org.mobicents.smsc.cassandra.ErrorCode;

import javax.slee.Address;
import javax.slee.facilities.Tracer;
import javax.slee.resource.ActivityHandle;
import javax.slee.resource.ConfigProperties;
import javax.slee.resource.FailureReason;
import javax.slee.resource.FireableEventType;
import javax.slee.resource.InvalidConfigurationException;
import javax.slee.resource.Marshaler;
import javax.slee.resource.ReceivableService;
import javax.slee.resource.ResourceAdaptor;
import javax.slee.resource.ResourceAdaptorContext;

import org.mobicents.smsc.cassandra.DBOperations_C1;
import org.mobicents.smsc.cassandra.DBOperations_C2;
import org.mobicents.smsc.cassandra.PersistenceException;
import org.mobicents.smsc.cassandra.PreparedStatementCollection_C3;
import org.mobicents.smsc.cassandra.Sms;
import org.mobicents.smsc.cassandra.SmsSet;
import org.mobicents.smsc.cassandra.SmsSetCashe;
import org.mobicents.smsc.cassandra.TargetAddress;

public class PersistenceResourceAdaptor implements ResourceAdaptor {

	private static final String CONF_CLUSTER_NAME = "cluster.name";
	private static final String CONF_CLUSTER_HOSTS = "cluster.hosts";
	private static final String CONF_CLUSTER_KEYSPACE = "cluster.keyspace";

	private Tracer tracer = null;
	private ResourceAdaptorContext raContext = null;

    private DBOperations_C1 dbOperations_C1 = null;
    private DBOperations_C2 dbOperations_C2 = null;

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
                return SmsSetCashe.getInstance().addSmsSet(ta);
            }

            @Override
            public void releaseSynchroObject(TargetAddress ta) {
                SmsSetCashe.getInstance().removeSmsSet(ta);
            }

		    // C1

			@Override
			public boolean checkSmsSetExists(TargetAddress ta) throws PersistenceException {
				return dbOperations_C1.checkSmsSetExists(ta);
			}

			@Override
			public SmsSet obtainSmsSet(TargetAddress ta) throws PersistenceException {
				return dbOperations_C1.obtainSmsSet(ta);
			}

			@Override
			public void setNewMessageScheduled(SmsSet smsSet, Date newDueDate) throws PersistenceException {
				dbOperations_C1.setNewMessageScheduled(smsSet, newDueDate);
			}

			@Override
			public void setDeliveringProcessScheduled(SmsSet smsSet, Date newDueDate, int newDueDelay)
					throws PersistenceException {
				dbOperations_C1.setDeliveringProcessScheduled(smsSet, newDueDate, newDueDelay);
			}

			@Override
			public void setDestination(SmsSet smsSet, String destClusterName, String destSystemId, String destEsmeId,
					SmType type) {
				dbOperations_C1.setDestination(smsSet, destClusterName, destSystemId, destEsmeId, type);
			}

			@Override
			public void setRoutingInfo(SmsSet smsSet, IMSI imsi, LocationInfoWithLMSI locationInfoWithLMSI) {
				dbOperations_C1.setRoutingInfo(smsSet, imsi, locationInfoWithLMSI);
			}

			@Override
			public void setDeliveryStart(SmsSet smsSet, Date inSystemDate) throws PersistenceException {
				dbOperations_C1.setDeliveryStart(smsSet, inSystemDate);
			}

			@Override
			public void setDeliveryStart(Sms sms) throws PersistenceException {
				dbOperations_C1.setDeliveryStart(sms);
			}

			@Override
			public void setDeliverySuccess(SmsSet smsSet, Date lastDelivery) throws PersistenceException {
				dbOperations_C1.setDeliverySuccess(smsSet, lastDelivery);
			}

			@Override
			public void setDeliveryFailure(SmsSet smsSet, ErrorCode smStatus, Date lastDelivery)
					throws PersistenceException {
				dbOperations_C1.setDeliveryFailure(smsSet, smStatus, lastDelivery);
			}

			@Override
			public void setAlertingSupported(String targetId, boolean alertingSupported) throws PersistenceException {
				dbOperations_C1.setAlertingSupported(targetId, alertingSupported);
			}

			@Override
			public boolean deleteSmsSet(SmsSet smsSet) throws PersistenceException {
				return dbOperations_C1.deleteSmsSet(smsSet);
			}

			@Override
			public void createLiveSms(Sms sms) throws PersistenceException {
				dbOperations_C1.createLiveSms(sms);
			}

			@Override
			public Sms obtainLiveSms(UUID dbId) throws PersistenceException {
				return dbOperations_C1.obtainLiveSms(dbId);
			}

			@Override
			public Sms obtainLiveSms(long messageId) throws PersistenceException {
				return dbOperations_C1.obtainLiveSms(messageId);
			}

			@Override
			public void updateLiveSms(Sms sms) throws PersistenceException {
				dbOperations_C1.updateLiveSms(sms);
			}

			@Override
			public void archiveDeliveredSms(Sms sms, Date deliveryDate) throws PersistenceException {
				dbOperations_C1.archiveDeliveredSms(sms, deliveryDate);
			}

			@Override
			public void archiveFailuredSms(Sms sms) throws PersistenceException {
				dbOperations_C1.archiveFailuredSms(sms);
			}

			@Override
			public List<SmsSet> fetchSchedulableSmsSets(int maxRecordCount, Tracer tracer) throws PersistenceException {
				return dbOperations_C1.fetchSchedulableSmsSets(maxRecordCount, tracer);
			}

			@Override
            public void fetchSchedulableSms(SmsSet smsSet, boolean excludeNonScheduleDeliveryTime) throws PersistenceException {
                dbOperations_C1.fetchSchedulableSms(smsSet, excludeNonScheduleDeliveryTime);
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
            public long c2_getDueSlotForTargetId(PreparedStatementCollection_C3 psc, String targetId) throws PersistenceException {
                return dbOperations_C2.c2_getDueSlotForTargetId(psc, targetId);
            }

            @Override
            public void c2_updateDueSlotForTargetId(String targetId, long newDueSlot) throws PersistenceException {
                dbOperations_C2.c2_updateDueSlotForTargetId(targetId, newDueSlot);
            }

            @Override
            public void c2_createRecordCurrent(Sms sms) throws PersistenceException {
                dbOperations_C2.c2_createRecordCurrent(sms);
            }

            @Override
            public void c2_createRecordArchive(Sms sms) throws PersistenceException {
                dbOperations_C2.c2_createRecordArchive(sms);
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
            public void c2_updateInSystem(Sms sms, int isSystemStatus) throws PersistenceException {
                dbOperations_C2.c2_updateInSystem(sms, isSystemStatus);
            }

            @Override
            public PreparedStatementCollection_C3[] getPscList() throws PersistenceException {
                return dbOperations_C2.getPscList();
            }

            @Override
            public void c2_scheduleMessage(Sms sms) throws PersistenceException {
                dbOperations_C2.c2_scheduleMessage(sms);
            }

            @Override
            public boolean c2_scheduleMessage(Sms sms, long dueSlot) throws PersistenceException {
                return dbOperations_C2.c2_scheduleMessage(sms, dueSlot);
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

		dbOperations_C2 = DBOperations_C2.getInstance();
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
