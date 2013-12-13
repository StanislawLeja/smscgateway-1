package org.mobicents.smsc.tools.stresstool;

import com.datastax.driver.core.Session;

public class TT_DBOperationsProxy3 extends NN_DBOperations {

    public Session getSession() {
        return this.session;
    }

}
