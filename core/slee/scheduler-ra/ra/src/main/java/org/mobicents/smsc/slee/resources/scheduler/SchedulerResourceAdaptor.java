package org.mobicents.smsc.slee.resources.scheduler;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

import javax.slee.Address;
import javax.slee.facilities.Tracer;
import javax.slee.resource.ActivityFlags;
import javax.slee.resource.ActivityHandle;
import javax.slee.resource.ConfigProperties;
import javax.slee.resource.ConfigProperties.Property;
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

import org.mobicents.smsc.slee.common.EventIDCache;
import org.mobicents.smsc.slee.services.persistence.SmType;
import org.mobicents.smsc.slee.services.persistence.Sms;
import org.mobicents.smsc.slee.services.persistence.SmsSet;
import org.mobicents.smsc.slee.services.smpp.server.events.SmsSetEvent;

public class SchedulerResourceAdaptor implements ResourceAdaptor {

	private static final int ACTIVITY_FLAGS = ActivityFlags
			.setRequestEndedCallback(ActivityFlags.REQUEST_ACTIVITY_UNREFERENCED_CALLBACK);
	private static final int EVENT_FLAGS = EventFlags.setRequestProcessingSuccessfulCallback(EventFlags
			.setRequestProcessingFailedCallback(EventFlags.REQUEST_EVENT_UNREFERENCED_CALLBACK));
	private static final String EVENT_VENDOR = "org.mobicents";
	private static final String EVENT_VERSION = "1.0";

	private static final String EVENT_DELIVER_SM = "org.mobicents.smsc.slee.services.smpp.server.events.DELIVER_SM";
	private static final String EVENT_SUBMIT_SM = "org.mobicents.smsc.slee.services.smpp.server.events.SUBMIT_SM";

	private static final String CONF_CLUSTER_NAME = "cluster.name";
	private static final String CONF_CLUSTER_HOSTS = "cluster.hosts";
	private static final String CONF_CLUSTER_KEYSPACE = "cluster.keyspace";
	private static final String CONF_FETCH_MAX_ROWS = "fetch.max.rows";
	private static final String CONF_FETCH_PERIOD = "fetch.period";
	private static final String CONF_HIGH_WATER_MARK = "high.water.mark";

	private Tracer tracer = null;
	private ResourceAdaptorContext raContext = null;
	private SleeTransactionManager sleeTransactionManager = null;
	private SleeEndpoint sleeEndpoint = null;
	private EventIDCache eventIdCache;
	private Cluster cluster = null;
	private Keyspace keyspace = null;

	private Timer raTimerService;
	private ConcurrentHashMap<ActivityHandle, SchedulerActivity> acitivties;
	// this is to avoid wicked SLEE spec - it mandates this.raSbbInterface to be
	// available before RA starts...
	// private SchedulerRAInterface raSbbInterface;

	private String hosts = null;
	private String keyspaceName = null;
	private String clusterName = null;

	// default is 5s ?
	private long fetchPeriod = 5000;
	// max intake, 10k rows
	private int fetchMaxRows = 5000;

	private int highWaterMark = 25000;

	private int activeCount = 0;

	public SchedulerResourceAdaptor() {

	}

	@Override
	public void activityEnded(ActivityHandle activityHandle) {
		if (this.tracer.isFineEnabled()) {
			this.tracer.fine("Activity with handle " + activityHandle + " ended.");
		}
		this.activeCount--;
		this.acitivties.remove(activityHandle);
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
		// return this.raSbbInterface;
		return null;
	}

	@Override
	public void queryLiveness(ActivityHandle activityHandle) {

	}

	@Override
	public void raActive() {
		this.activeCount = 0;
		this.acitivties = new ConcurrentHashMap<ActivityHandle, SchedulerActivity>();
		this.cluster = HFactory.getOrCreateCluster(this.clusterName, this.hosts);
		ConfigurableConsistencyLevel ccl = new ConfigurableConsistencyLevel();
		ccl.setDefaultReadConsistencyLevel(HConsistencyLevel.ONE);
		this.keyspace = HFactory.createKeyspace(this.keyspaceName, this.cluster, ccl);
		if (this.tracer.isInfoEnabled()) {
			this.tracer.info("Scheduler IS up, starting fetch tasks");
		}
		this.raTimerService.schedule(new TickTimerTask(), 500, this.fetchPeriod);
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

		Property configProperty = properties.getProperty(CONF_CLUSTER_NAME);
		this.clusterName = (String) configProperty.getValue();

		configProperty = properties.getProperty(CONF_CLUSTER_KEYSPACE);
		this.keyspaceName = (String) configProperty.getValue();

		configProperty = properties.getProperty(CONF_CLUSTER_HOSTS);
		this.hosts = (String) configProperty.getValue();

		configProperty = properties.getProperty(CONF_FETCH_MAX_ROWS);
		this.fetchMaxRows = (Integer) configProperty.getValue();

		configProperty = properties.getProperty(CONF_FETCH_PERIOD);
		this.fetchPeriod = (Long) configProperty.getValue();

		configProperty = properties.getProperty(CONF_HIGH_WATER_MARK);
		this.highWaterMark = (Integer) configProperty.getValue();

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
		this.hosts = null;
		this.keyspaceName = null;
		this.clusterName = null;

	}

	@Override
	public void raVerifyConfiguration(ConfigProperties properties) throws InvalidConfigurationException {
		if (tracer.isInfoEnabled()) {
			tracer.info("Verify configuration in RA Entity " + this.raContext.getEntityName());
		}
		Property configProperty = properties.getProperty(CONF_CLUSTER_NAME);
		if (configProperty == null || configProperty.getValue() == null
				|| !(configProperty.getValue() instanceof String) || ((String) configProperty.getValue()).isEmpty()) {
			throw new InvalidConfigurationException("Wrong value of '" + CONF_CLUSTER_NAME + "' property: "
					+ configProperty);
		}

		configProperty = properties.getProperty(CONF_CLUSTER_KEYSPACE);
		if (configProperty == null || configProperty.getValue() == null
				|| !(configProperty.getValue() instanceof String) || ((String) configProperty.getValue()).isEmpty()) {
			throw new InvalidConfigurationException("Wrong value of '" + CONF_CLUSTER_KEYSPACE + "' property: "
					+ configProperty);
		}

		configProperty = properties.getProperty(CONF_CLUSTER_HOSTS);
		if (configProperty == null || configProperty.getValue() == null
				|| !(configProperty.getValue() instanceof String) || ((String) configProperty.getValue()).isEmpty()) {
			throw new InvalidConfigurationException("Wrong value of '" + CONF_CLUSTER_HOSTS + "' property: "
					+ configProperty);
		}

		// TODO: better checks for long/minimal value
		configProperty = properties.getProperty(CONF_FETCH_MAX_ROWS);
		if (configProperty == null || configProperty.getValue() == null
				|| !(configProperty.getValue() instanceof Integer) || ((Integer) configProperty.getValue()) <= 0) {
			throw new InvalidConfigurationException("Wrong value of '" + CONF_FETCH_MAX_ROWS + "' property: "
					+ configProperty);
		}

		configProperty = properties.getProperty(CONF_FETCH_PERIOD);
		if (configProperty == null || configProperty.getValue() == null || !(configProperty.getValue() instanceof Long)
				|| ((Long) configProperty.getValue()) <= 0) {
			throw new InvalidConfigurationException("Wrong value of '" + CONF_FETCH_PERIOD + "' property: "
					+ configProperty);
		}

		configProperty = properties.getProperty(CONF_HIGH_WATER_MARK);
		if (configProperty == null || configProperty.getValue() == null
				|| !(configProperty.getValue() instanceof Integer) || ((Integer) configProperty.getValue()) <= 0) {
			throw new InvalidConfigurationException("Wrong value of '" + CONF_HIGH_WATER_MARK + "' property: "
					+ configProperty);
		}
		// CONF_FETCH_MAX_ROWS,CONF_FETCH_PERIOD,CONF_HIGH_WATER_MARK
		// TODO: add hosts validation: host:port,host2:port,...
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
		List<SmsSet> schedulableSms = this.fetchSchedulable();
		int count = 0;
		try {
			for (SmsSet sms : schedulableSms) {
				try {
					if (!injectSms(sms)) {
						return;
					}
					count++;
				} catch (Exception e) {
					// TODO: handle rejection!
				}
			}
		} finally {
			if (this.tracer.isFineEnabled()) {
				this.tracer.fine("Scheduled '" + count + "' out of '" + schedulableSms.size() + "'.");
			}
		}
	}

	protected boolean injectSms(SmsSet smsSet) throws Exception {
		// NOTE, we dont sync, +/-1 is not that important vs performance.
		if (this.activeCount >= this.highWaterMark) {
			return false;
		}
		SleeTransaction sleeTx = this.sleeTransactionManager.beginSleeTransaction();

		try {
			final String eventName = smsSet.getType() == SmType.DELIVER_SM ? EVENT_DELIVER_SM : EVENT_SUBMIT_SM;
			final FireableEventType eventTypeId = this.eventIdCache.getEventId(eventName);
			SmsSetEvent event = new SmsSetEvent();
			SchedulerActivityImpl activity = new SchedulerActivityImpl();
			this.sleeEndpoint.startActivityTransacted(activity.getActivityHandle(), activity);

			try {
				this.sleeEndpoint.fireEventTransacted(activity.getActivityHandle(), eventTypeId, event, null, null);
			} catch (Exception e) {
				if (this.tracer.isSevereEnabled()) {
					this.tracer.severe("Failed to fire SmsSet event: " + eventTypeId, e);
				}
				try {
					this.sleeEndpoint.endActivityTransacted(activity.getActivityHandle());
				} catch (Exception ee) {
				}
			}

			markAsInSystem(smsSet);
			this.acitivties.put(activity.getActivityHandle(), activity);
		} catch (Exception e) {
			this.sleeTransactionManager.rollback();
			throw e;
		}
		this.sleeTransactionManager.commit();
		this.activeCount++;
		return true;
	}

	protected List<SmsSet> fetchSchedulable() {
		return null;
	}

	protected void markAsInSystem(SmsSet smsSet) {
		// TODO:
		for (Sms sms : smsSet.getRawList()) {
			// TODO
		}
	}

}
