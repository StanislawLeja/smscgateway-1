package org.mobicents.smsc.slee.resources.scheduler;

import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import javax.slee.Address;
import javax.slee.facilities.Tracer;
import javax.slee.resource.ActivityFlags;
import javax.slee.resource.ActivityHandle;
import javax.slee.resource.ConfigProperties;
import javax.slee.resource.EventFlags;
import javax.slee.resource.FailureReason;
import javax.slee.resource.FireableEventType;
import javax.slee.resource.InvalidConfigurationException;
import javax.slee.resource.Marshaler;
import javax.slee.resource.ReceivableService;
import javax.slee.resource.ResourceAdaptor;
import javax.slee.resource.ResourceAdaptorContext;
import javax.slee.resource.SleeEndpoint;
import javax.slee.transaction.SleeTransaction;
import javax.slee.transaction.SleeTransactionManager;

import me.prettyprint.cassandra.model.ConfigurableConsistencyLevel;
import me.prettyprint.hector.api.Cluster;
import me.prettyprint.hector.api.HConsistencyLevel;
import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.factory.HFactory;

import org.mobicents.smsc.slee.common.ra.EventIDCache;
import org.mobicents.smsc.slee.resources.persistence.DBOperations;
import org.mobicents.smsc.slee.resources.persistence.PersistenceException;
import org.mobicents.smsc.slee.resources.persistence.SmType;
import org.mobicents.smsc.slee.resources.persistence.SmsSet;
import org.mobicents.smsc.slee.resources.persistence.SmsSetCashe;
import org.mobicents.smsc.slee.resources.persistence.TargetAddress;
import org.mobicents.smsc.slee.services.smpp.server.events.SmsSetEvent;
import org.mobicents.smsc.smpp.SmsRouteManagement;
import org.mobicents.smsc.smpp.SmscPropertiesManagement;

public class SchedulerResourceAdaptor implements ResourceAdaptor {

	private static final int ACTIVITY_FLAGS = ActivityFlags
			.setRequestEndedCallback(ActivityFlags.REQUEST_ACTIVITY_UNREFERENCED_CALLBACK);
	private static final int EVENT_FLAGS = EventFlags.setRequestProcessingSuccessfulCallback(EventFlags
			.setRequestProcessingFailedCallback(EventFlags.REQUEST_EVENT_UNREFERENCED_CALLBACK));
	private static final String EVENT_VENDOR = "org.mobicents";
	private static final String EVENT_VERSION = "1.0";

	private static final String EVENT_DELIVER_SM = "org.mobicents.smsc.slee.services.smpp.server.events.DELIVER_SM";
	private static final String EVENT_SUBMIT_SM = "org.mobicents.smsc.slee.services.smpp.server.events.SUBMIT_SM";

	private Tracer tracer = null;
	private ResourceAdaptorContext raContext = null;
	private SleeTransactionManager sleeTransactionManager = null;
	private SleeEndpoint sleeEndpoint = null;
	private EventIDCache eventIdCache;
	private Cluster cluster = null;
	private Keyspace keyspace = null;

	private Timer raTimerService;

	private SchedulerRaSbbInterface schedulerRaSbbInterface = null;
	private SchedulerRaUsageParameters usageParameters;

    public SchedulerResourceAdaptor() {
        this.schedulerRaSbbInterface = new SchedulerRaSbbInterface() {
            @Override
            public void decrementDeliveryActivityCount() {
                decrementActivityCount();
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
		// something bad happened, push back to DB:
		// TODO: markAsNotInSystem();
		this.sleeEndpoint.endActivity(activityHandle);
	}

	@Override
	public void eventProcessingSuccessful(ActivityHandle activityHandle, FireableEventType arg1, Object arg2,
			Address arg3, ReceivableService arg4, int flags) {
		if (this.tracer.isFineEnabled()) {
			this.tracer.fine("Event processing succeeded on activity with handle " + activityHandle + ".");
		}
		if (EventFlags.hasSbbProcessedEvent(flags)) {
			// SBB did process
		} else {
			// nothing happened
			// TODO: markAsNotInSystem();
		}
		this.sleeEndpoint.endActivity(activityHandle);
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
		return this.schedulerRaSbbInterface;
	}

	@Override
	public void queryLiveness(ActivityHandle activityHandle) {

	}

	@Override
	public void raActive() {
		(new Thread(new ActivateRa())).start();
	}
	
	private class ActivateRa implements Runnable {
	    this.clearActivityCount();

		@Override
		public void run() {
		    clearActivityCount();

	        SmscPropertiesManagement smscPropertiesManagement = SmscPropertiesManagement.getInstance();
	        while(smscPropertiesManagement == null){
	        	 System.out.println(smscPropertiesManagement);
	        	smscPropertiesManagement = SmscPropertiesManagement.getInstance();
	        	try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
	        }
	        
	       
	        System.out.println(smscPropertiesManagement.getClusterName());
	        System.out.println(smscPropertiesManagement.getHosts());
	        
	        cluster = HFactory.getOrCreateCluster(smscPropertiesManagement.getClusterName(), smscPropertiesManagement.getHosts());
			ConfigurableConsistencyLevel ccl = new ConfigurableConsistencyLevel();
			ccl.setDefaultReadConsistencyLevel(HConsistencyLevel.ONE);
			keyspace = HFactory.createKeyspace(smscPropertiesManagement.getKeyspaceName(), cluster, ccl);
			if (tracer.isInfoEnabled()) {
				tracer.info("Scheduler IS up, starting fetch tasks");
			}

			raTimerService.schedule(new TickTimerTask(), 500, smscPropertiesManagement.getFetchPeriod());
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
		if (this.cluster != null) {
			this.cluster.getConnectionManager().shutdown();
		}
		this.cluster = null;
		this.keyspace = null;
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
		this.eventIdCache = new EventIDCache(this.raContext, EVENT_VENDOR, EVENT_VERSION);
		this.sleeTransactionManager = this.raContext.getSleeTransactionManager();
		this.sleeEndpoint = this.raContext.getSleeEndpoint();
		this.raTimerService = this.raContext.getTimer();
		this.usageParameters = (SchedulerRaUsageParameters)this.raContext.getDefaultUsageParameterSet();
	}

	@Override
	public void unsetResourceAdaptorContext() {
		this.tracer = null;
		this.eventIdCache = null;
		this.raContext = null;
		this.sleeTransactionManager = null;
		this.sleeEndpoint = null;
		this.raTimerService = null;
	}

	// /////////////////
	// Helper classes //
	// /////////////////
	protected class TickTimerTask extends TimerTask {

		@Override
		public void run() {
			onTimerTick();
		}

	}

	protected class SchedulerActivityImpl implements SchedulerActivity {
		private final SchedulerActivityHandle handle = new SchedulerActivityHandle();

		SchedulerActivityHandle getActivityHandle() {
			return this.handle;
		}

	}

	// /////////////////
	// Helper methods //
	// /////////////////
	protected void onTimerTick() {
		Keyspace kSpace = this.keyspace;
		if (kSpace == null)
			return;

		List<SmsSet> schedulableSms;
		try {
            if (this.tracer.isFineEnabled())
                this.tracer.fine("Fetching: Starting fetching messages from database");

            SmscPropertiesManagement smscPropertiesManagement = SmscPropertiesManagement.getInstance();
            int fetchMaxRows = smscPropertiesManagement.getFetchMaxRows();
            int fetchAvailRows = smscPropertiesManagement.getMaxActivityCount() - (int) this.getActivityCount();
            int maxCnt = Math.min(fetchMaxRows, fetchAvailRows);
            schedulableSms = this.fetchSchedulable(maxCnt, kSpace);

            int cnt = 0;
            if(schedulableSms!=null)
                cnt = schedulableSms.size();
            String s1 = "Fetching: Fetched " + schedulableSms.size() + " messages (max requested messages=" + maxCnt + ", fetched messages=" + cnt + ")";
            if (cnt > 0) {
                if (this.tracer.isInfoEnabled())
                    this.tracer.info(s1);
            } else {
                if (this.tracer.isFineEnabled())
                    this.tracer.fine(s1);
            }
		} catch (PersistenceException e1) {
			this.tracer.severe("PersistenceException when fetching SmsSet list from a database: " + e1.getMessage(), e1);
			return;
		}

		int count = 0;
		try {
			for (SmsSet sms : schedulableSms) {
				try {
					if (!injectSms(kSpace, sms)) {
						return;
					}
					count++;
				} catch (Exception e) {
					this.tracer.severe("Exception when injectSms: " + e.getMessage(), e);
				}
			}
		} finally {
            String s2 = "Fetching: Scheduled '" + count + "' out of '" + schedulableSms.size() + "'.";
            if (schedulableSms.size() > 0) {
                if (this.tracer.isInfoEnabled()) {
                    this.tracer.info(s2);
                }
            } else {
                if (this.tracer.isFineEnabled())
                    this.tracer.fine(s2);
            }
		}
	}

	protected boolean injectSms(Keyspace kSpace, SmsSet smsSet) throws Exception {
		SleeTransaction sleeTx = this.sleeTransactionManager.beginSleeTransaction();

		try {
			SmsRouteManagement smsRouteManagement = SmsRouteManagement.getInstance();
			String destClusterName = smsRouteManagement.getEsmeClusterName(smsSet.getDestAddrTon(), smsSet.getDestAddrNpi(), smsSet.getDestAddr());

			smsSet.setDestClusterName(destClusterName);
			smsSet.setType(destClusterName != null ? SmType.SMS_FOR_ESME : SmType.SMS_FOR_SS7);

			final String eventName = smsSet.getType() == SmType.SMS_FOR_ESME ? EVENT_DELIVER_SM : EVENT_SUBMIT_SM;
			final FireableEventType eventTypeId = this.eventIdCache.getEventId(eventName);
			SmsSetEvent event = new SmsSetEvent();
			event.setSmsSet(smsSet);
			SchedulerActivityImpl activity = new SchedulerActivityImpl();
			this.sleeEndpoint.startActivityTransacted(activity.getActivityHandle(), activity);

			try {
				this.sleeEndpoint.fireEventTransacted(activity.getActivityHandle(), eventTypeId, event, null, null);
			} catch (Exception e) {
				if (this.tracer.isSevereEnabled()) {
					this.tracer.severe("Failed to fire SmsSet event Class=: " + eventTypeId.getEventClassName(), e);
				}
				try {
					this.sleeEndpoint.endActivityTransacted(activity.getActivityHandle());
				} catch (Exception ee) {
				}
			}
 
			markAsInSystem(kSpace, smsSet);
		} catch (Exception e) {
			this.sleeTransactionManager.rollback();
			throw e;
		}
		this.sleeTransactionManager.commit();
		this.incrementActivityCount();
		return true;
	}

	protected List<SmsSet> fetchSchedulable(int maxRecordCount, Keyspace kSpace) throws PersistenceException {
		List<SmsSet> res = DBOperations.fetchSchedulableSmsSets(kSpace, maxRecordCount, this.tracer);
		return res;
	}

	protected void markAsInSystem(Keyspace kSpace, SmsSet smsSet) throws PersistenceException {
		TargetAddress lock = SmsSetCashe.getInstance().addSmsSet(new TargetAddress(smsSet));
		synchronized (lock) {
			try {
				boolean b1 = DBOperations.checkSmsSetExists(kSpace, new TargetAddress(smsSet));
				if (!b1)
					throw new PersistenceException("SmsSet record is not found when markAsInSystem()");

				DBOperations.fetchSchedulableSms(kSpace, smsSet, smsSet.getType() == SmType.SMS_FOR_SS7);
				DBOperations.setDeliveryStart(kSpace, smsSet, new Date());
			} finally {
				SmsSetCashe.getInstance().addSmsSet(lock);
			}
		}
	}

    private void clearActivityCount() {
        long cnt = this.getActivityCount();
        this.usageParameters.incrementActivityCount(-cnt);

    }

    private void incrementActivityCount() {
        this.usageParameters.incrementActivityCount(1);
    }

    private void decrementActivityCount() {
        this.usageParameters.incrementActivityCount(-1);
    }

    private long getActivityCount() {
        return this.usageParameters.getActivityCount();
    }
}
