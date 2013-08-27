package org.mobicents.smsc.slee.resources.scheduler;

import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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

import org.mobicents.smsc.cassandra.DBOperations;
import org.mobicents.smsc.cassandra.PersistenceException;
import org.mobicents.smsc.cassandra.SmType;
import org.mobicents.smsc.cassandra.SmsSet;
import org.mobicents.smsc.cassandra.SmsSetCashe;
import org.mobicents.smsc.cassandra.TargetAddress;
import org.mobicents.smsc.slee.common.ra.EventIDCache;
import org.mobicents.smsc.slee.services.smpp.server.events.SmsSetEvent;
import org.mobicents.smsc.smpp.SmsRouteManagement;
import org.mobicents.smsc.smpp.SmscPropertiesManagement;

public class SchedulerResourceAdaptor implements ResourceAdaptor {

	private static final int ACTIVITY_FLAGS = ActivityFlags
			.setRequestEndedCallback(ActivityFlags.REQUEST_ENDED_CALLBACK);

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

	private ScheduledExecutorService scheduler = null;

	private SchedulerRaSbbInterface schedulerRaSbbInterface = null;
	private SchedulerRaUsageParameters usageParameters;

	private DBOperations dbOperations = null;

	public SchedulerResourceAdaptor() {
		this.schedulerRaSbbInterface = new SchedulerRaSbbInterface() {

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
		return ((SchedulerActivityHandle) activityHandle).getActivity();
	}

	@Override
	public ActivityHandle getActivityHandle(Object activity) {
		if (activity instanceof SchedulerActivityImpl) {
			final SchedulerActivityImpl wrapper = ((SchedulerActivityImpl) activity);
			// if (wrapper.getRa() == this) {
			return wrapper.getActivityHandle();
			// }
		}

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
		clearActivityCount();

		SmscPropertiesManagement smscPropertiesManagement = SmscPropertiesManagement.getInstance();
		this.dbOperations = DBOperations.getInstance();

		if (!this.dbOperations.isStarted()) {
			throw new RuntimeException("DBOperations not started yet!");
		}

		scheduler = Executors.newScheduledThreadPool(1);

		scheduler.scheduleAtFixedRate(new TickTimerTask(), 500, smscPropertiesManagement.getFetchPeriod(),
				TimeUnit.MILLISECONDS);

		if (tracer.isInfoEnabled()) {
			tracer.info("SchedulerResourceAdaptor " + raContext.getEntityName() + " Activated");
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
			tracer.info("Inactivated SchedulerResourceAdaptor RA Entity " + this.raContext.getEntityName());
		}

	}

	@Override
	public void raStopping() {
		if (tracer.isInfoEnabled()) {
			tracer.info("Stopping Scheduler RA Entity " + this.raContext.getEntityName());
		}

		this.scheduler.shutdown();
		try {
			this.scheduler.awaitTermination(120, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			tracer.severe("InterruptedException while awaiting termination of tasks", e);
		}

		if (tracer.isInfoEnabled()) {
			tracer.info("Stopped Scheduler RA Entity " + this.raContext.getEntityName());
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
		this.usageParameters = (SchedulerRaUsageParameters) this.raContext.getDefaultUsageParameterSet();
	}

	@Override
	public void unsetResourceAdaptorContext() {
		this.tracer = null;
		this.eventIdCache = null;
		this.raContext = null;
		this.sleeTransactionManager = null;
		this.sleeEndpoint = null;
	}

	// /////////////////
	// Helper classes //
	// /////////////////
	protected class TickTimerTask implements Runnable {

		@Override
		public void run() {
			onTimerTick();
		}

	}

	// /////////////////
	// Helper methods //
	// /////////////////
	protected void onTimerTick() {

		List<SmsSet> schedulableSms;
		try {
			if (this.tracer.isFineEnabled())
				this.tracer.fine("Fetching: Starting fetching messages from database");

			SmscPropertiesManagement smscPropertiesManagement = SmscPropertiesManagement.getInstance();
			int fetchMaxRows = smscPropertiesManagement.getFetchMaxRows();
			int fetchAvailRows = smscPropertiesManagement.getMaxActivityCount() - (int) this.getActivityCount();
			int maxCnt = Math.min(fetchMaxRows, fetchAvailRows);
			schedulableSms = this.fetchSchedulable(maxCnt);

			int cnt = 0;
			if (schedulableSms != null)
				cnt = schedulableSms.size();

			if (this.tracer.isInfoEnabled()) {
				String s1 = "Fetching: Fetched " + schedulableSms.size() + " messages (max requested messages="
						+ maxCnt + ", fetched messages=" + cnt + ")";
				this.tracer.info(s1);
			}
		} catch (PersistenceException e1) {
			this.tracer
					.severe("PersistenceException when fetching SmsSet list from a database: " + e1.getMessage(), e1);
			return;
		}

		int count = 0;
		try {
			for (SmsSet sms : schedulableSms) {
				try {
					if (!injectSms(sms)) {
						return;
					}
					count++;
				} catch (Exception e) {
					this.tracer.severe("Exception when injectSms: " + e.getMessage(), e);
				}
			}
		} finally {

			if (this.tracer.isInfoEnabled()) {
				String s2 = "Fetching: Scheduled '" + count + "' out of '" + schedulableSms.size() + "'.";
				this.tracer.info(s2);
			}

		}
	}

	protected void endAcitivity(SchedulerActivityHandle activityHandle) throws Exception {
		this.sleeEndpoint.endActivity(activityHandle);
		this.decrementActivityCount();
	}

	protected boolean injectSms(SmsSet smsSet) throws Exception {
		SleeTransaction sleeTx = this.sleeTransactionManager.beginSleeTransaction();

		try {
			SmsRouteManagement smsRouteManagement = SmsRouteManagement.getInstance();
			String destClusterName = smsRouteManagement.getEsmeClusterName(smsSet.getDestAddrTon(),
					smsSet.getDestAddrNpi(), smsSet.getDestAddr());

			smsSet.setDestClusterName(destClusterName);
			smsSet.setType(destClusterName != null ? SmType.SMS_FOR_ESME : SmType.SMS_FOR_SS7);

			final String eventName = smsSet.getType() == SmType.SMS_FOR_ESME ? EVENT_DELIVER_SM : EVENT_SUBMIT_SM;
			final FireableEventType eventTypeId = this.eventIdCache.getEventId(eventName);
			SmsSetEvent event = new SmsSetEvent();
			event.setSmsSet(smsSet);

			SchedulerActivityImpl activity = new SchedulerActivityImpl(this);
			this.sleeEndpoint.startActivityTransacted(activity.getActivityHandle(), activity, ACTIVITY_FLAGS);

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
			markAsInSystem(smsSet);
		} catch (Exception e) {
			this.sleeTransactionManager.rollback();
			throw e;
		}

		this.sleeTransactionManager.commit();

		this.incrementActivityCount();
		return true;
	}

	protected List<SmsSet> fetchSchedulable(int maxRecordCount) throws PersistenceException {
		List<SmsSet> res = dbOperations.fetchSchedulableSmsSets(maxRecordCount, this.tracer);
		return res;
	}

	protected void markAsInSystem(SmsSet smsSet) throws PersistenceException {

		TargetAddress lock = SmsSetCashe.getInstance().addSmsSet(new TargetAddress(smsSet));

		synchronized (lock) {
			try {
				boolean b1 = dbOperations.checkSmsSetExists(new TargetAddress(smsSet));

				if (!b1)
					throw new PersistenceException("SmsSet record is not found when markAsInSystem()");

				dbOperations.fetchSchedulableSms(smsSet, smsSet.getType() == SmType.SMS_FOR_SS7);

				dbOperations.setDeliveryStart(smsSet, new Date());

			} finally {
				SmsSetCashe.getInstance().removeSmsSet(lock);
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
