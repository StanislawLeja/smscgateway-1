package org.mobicents.smsc.tools.stresstool;

import org.mobicents.smsc.cassandra.DBOperations;

import com.datastax.driver.core.Session;

public class TT_DBOperationsProxy extends DBOperations {

    public Session getSession() {
        return this.session;
    }

}
