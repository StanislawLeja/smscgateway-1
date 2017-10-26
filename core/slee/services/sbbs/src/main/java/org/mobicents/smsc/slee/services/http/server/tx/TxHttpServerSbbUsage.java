package org.mobicents.smsc.slee.services.http.server.tx;

public interface TxHttpServerSbbUsage {

    void incrementCounterGet(long aValue);
    void incrementCounterPost(long aValue);
    
    void sampleGetProcessing(long aValue);
    void samplePostProcessing(long aValue);

}
