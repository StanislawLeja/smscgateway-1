package org.mobicents.smsc.slee.resources.persistence;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.slee.Address;
import javax.slee.facilities.Tracer;
import javax.slee.resource.ActivityHandle;
import javax.slee.resource.ConfigProperties;
import javax.slee.resource.ConfigProperties.Property;
import javax.slee.resource.FailureReason;
import javax.slee.resource.FireableEventType;
import javax.slee.resource.InvalidConfigurationException;
import javax.slee.resource.Marshaler;
import javax.slee.resource.ReceivableService;
import javax.slee.resource.ResourceAdaptor;
import javax.slee.resource.ResourceAdaptorContext;

import org.mobicents.protocols.ss7.map.api.primitives.IMSI;
import org.mobicents.protocols.ss7.map.api.service.sms.LocationInfoWithLMSI;
import org.mobicents.smsc.slee.resources.persistence.DBOperations;
import org.mobicents.smsc.slee.resources.persistence.ErrorCode;
import org.mobicents.smsc.slee.resources.persistence.PersistenceException;
import org.mobicents.smsc.slee.resources.persistence.PersistenceRAInterface;
import org.mobicents.smsc.slee.resources.persistence.SmType;
import org.mobicents.smsc.slee.resources.persistence.Sms;
import org.mobicents.smsc.slee.resources.persistence.SmsSet;
import org.mobicents.smsc.slee.resources.persistence.SmsSetCashe;
import org.mobicents.smsc.slee.resources.persistence.TargetAddress;

import me.prettyprint.cassandra.model.ConfigurableConsistencyLevel;
import me.prettyprint.hector.api.Cluster;
import me.prettyprint.hector.api.HConsistencyLevel;
import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.factory.HFactory;

public class PersistenceResourceAdaptor implements ResourceAdaptor {

    private static final String CONF_CLUSTER_NAME = "cluster.name";
    private static final String CONF_CLUSTER_HOSTS = "cluster.hosts";
    private static final String CONF_CLUSTER_KEYSPACE = "cluster.keyspace";

    private Tracer tracer = null;
    private ResourceAdaptorContext raContext = null;
    

    private Cluster cluster = null;
    private Keyspace keyspace = null; 
    //this is to avoid wicked SLEE spec - it mandates this.raSbbInterface to be available before RA starts...
    private PersistenceRAInterface raSbbInterface;
    
    private String hosts = null;
    private String keyspaceName = null;
    private String clusterName = null;

    public PersistenceResourceAdaptor() {
        this.raSbbInterface = new PersistenceRAInterface() {

        	@Override
        	public boolean checkSmsSetExists(TargetAddress ta) throws PersistenceException {
        		return DBOperations.checkSmsSetExists(keyspace, ta);
        	}

        	@Override
        	public SmsSet obtainSmsSet(TargetAddress ta) throws PersistenceException {
        		return DBOperations.obtainSmsSet(keyspace, ta);
        	}

        	@Override
        	public void setNewMessageScheduled(SmsSet smsSet, Date newDueDate) throws PersistenceException {
        		DBOperations.setNewMessageScheduled(keyspace, smsSet, newDueDate);
        	}

        	@Override
			public void setDeliveringProcessScheduled(SmsSet smsSet, Date newDueDate, int newDueDelay) throws PersistenceException {
				DBOperations.setDeliveringProcessScheduled(keyspace, smsSet, newDueDate, newDueDelay);
			}

        	@Override
        	public void setDestination(SmsSet smsSet, String destClusterName, String destSystemId, String destEsmeId, SmType type) {
        		DBOperations.setDestination(smsSet, destClusterName, destSystemId, destEsmeId, type);
        	}

        	@Override
        	public void setRoutingInfo(SmsSet smsSet, IMSI imsi, LocationInfoWithLMSI locationInfoWithLMSI) {
        		DBOperations.setRoutingInfo(smsSet, imsi, locationInfoWithLMSI);
        	}

        	@Override
        	public void setDeliveryStart(SmsSet smsSet, Date inSystemDate) throws PersistenceException {
        		DBOperations.setDeliveryStart(keyspace, smsSet, inSystemDate);
        	}

        	@Override
        	public void setDeliveryStart(Sms sms) throws PersistenceException {
        		DBOperations.setDeliveryStart(keyspace, sms);
        	}

        	@Override
        	public void setDeliverySuccess(SmsSet smsSet, Date lastDelivery) throws PersistenceException {
        		DBOperations.setDeliverySuccess(keyspace, smsSet, lastDelivery);
        	}

        	@Override
        	public void setDeliveryFailure(SmsSet smsSet, ErrorCode smStatus, Date lastDelivery) throws PersistenceException {
        		DBOperations.setDeliveryFailure(keyspace, smsSet, smStatus, lastDelivery);
        	}

        	@Override
        	public void setAlertingSupported(String targetId, boolean alertingSupported) throws PersistenceException {
        		DBOperations.setAlertingSupported(keyspace, targetId, alertingSupported);
        	}

        	@Override
        	public boolean deleteSmsSet(SmsSet smsSet) throws PersistenceException {
        		return DBOperations.deleteSmsSet(keyspace, smsSet);
        	}

        	@Override
        	public void createLiveSms(Sms sms) throws PersistenceException {
        		DBOperations.createLiveSms(keyspace, sms);
        	}

        	@Override
        	public Sms obtainLiveSms(UUID dbId) throws PersistenceException {
        		return DBOperations.obtainLiveSms(keyspace, dbId);
        	}

        	@Override
        	public Sms obtainLiveSms(long messageId) throws PersistenceException {
        		return DBOperations.obtainLiveSms(keyspace, messageId);
        	}

        	@Override
        	public void updateLiveSms(Sms sms) throws PersistenceException {
        		DBOperations.updateLiveSms(keyspace, sms);
        	}

        	@Override
        	public void archiveDeliveredSms(Sms sms, Date deliveryDate) throws PersistenceException {
        		DBOperations.archiveDeliveredSms(keyspace, sms, deliveryDate);
        	}

        	@Override
        	public void archiveFailuredSms(Sms sms) throws PersistenceException {
        		DBOperations.archiveFailuredSms(keyspace, sms);
        	}

        	@Override
        	public List<SmsSet> fetchSchedulableSmsSets(int maxRecordCount, Tracer tracer) throws PersistenceException {
        		return DBOperations.fetchSchedulableSmsSets(keyspace, maxRecordCount, tracer);
        	}

        	@Override
        	public TargetAddress obtainSynchroObject(TargetAddress ta) {
        		return SmsSetCashe.getInstance().addSmsSet(ta);
        	}

        	@Override
        	public void releaseSynchroObject(TargetAddress ta) {
            	SmsSetCashe.getInstance().removeSmsSet(ta);
        	}

        	@Override
        	public void fetchSchedulableSms(SmsSet smsSet, boolean excludeNonScheduleDeliveryTime) throws PersistenceException {
        		DBOperations.fetchSchedulableSms(keyspace, smsSet, excludeNonScheduleDeliveryTime);
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
    public void eventProcessingSuccessful(ActivityHandle activityHandle, FireableEventType arg1, Object arg2, Address arg3,
            ReceivableService arg4, int arg5) {
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
        this.cluster = HFactory.getOrCreateCluster(this.clusterName, this.hosts);
        ConfigurableConsistencyLevel ccl = new ConfigurableConsistencyLevel();
        ccl.setDefaultReadConsistencyLevel(HConsistencyLevel.ONE);
        this.keyspace = HFactory.createKeyspace(this.keyspaceName, this.cluster, ccl);
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

    }

    @Override
    public void raInactive() {

        if (tracer.isInfoEnabled()) {
            tracer.info("Inactivated RA Entity " + this.raContext.getEntityName());
        }
        this.cluster.getConnectionManager().shutdown();
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
        if (configProperty == null || configProperty.getValue() == null || !(configProperty.getValue() instanceof String)
                || ((String) configProperty.getValue()).isEmpty()) {
            throw new InvalidConfigurationException("Wrong value of '" + CONF_CLUSTER_NAME + "' property: " + configProperty);
        }

        configProperty = properties.getProperty(CONF_CLUSTER_KEYSPACE);
        if (configProperty == null || configProperty.getValue() == null || !(configProperty.getValue() instanceof String)
                || ((String) configProperty.getValue()).isEmpty()) {
            throw new InvalidConfigurationException("Wrong value of '" + CONF_CLUSTER_KEYSPACE + "' property: "
                    + configProperty);
        }

        configProperty = properties.getProperty(CONF_CLUSTER_HOSTS);
        if (configProperty == null || configProperty.getValue() == null || !(configProperty.getValue() instanceof String)
                || ((String) configProperty.getValue()).isEmpty()) {
            throw new InvalidConfigurationException("Wrong value of '" + CONF_CLUSTER_HOSTS + "' property: " + configProperty);
        }

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
    }

    @Override
    public void unsetResourceAdaptorContext() {
        this.raContext = null;
    }

    
}
