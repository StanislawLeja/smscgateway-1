package org.mobicents.smsc.slee.resources.hector.client;

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

import me.prettyprint.cassandra.model.ConfigurableConsistencyLevel;
import me.prettyprint.hector.api.Cluster;
import me.prettyprint.hector.api.HConsistencyLevel;
import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.factory.HFactory;

public class HectorClientResourceAdaptor implements ResourceAdaptor {

    private static final String CONF_CLUSTER_NAME = "cluster.name";
    private static final String CONF_CLUSTER_HOSTS = "cluster.hosts";
    private static final String CONF_CLUSTER_KEYSPACE = "cluster.keyspace";

    private Tracer tracer = null;
    private ResourceAdaptorContext raContext = null;
    

    private Cluster cluster = null;
    private Keyspace keyspace = null; 
    //this is to avoid wicked SLEE spec - it mandates this.raSbbInterface to be available before RA starts...
    private HectorClientRAInterface raSbbInterface;
    
    private String hosts = null;
    private String keyspaceName = null;
    private String clusterName = null;

    public HectorClientResourceAdaptor() {
        this.raSbbInterface = new HectorClientRAInterface() {
            
            @Override
            public Keyspace getKeyspace() {
                return keyspace;
            }
            
            @Override
            public Cluster getCluster() {
                return cluster;
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
