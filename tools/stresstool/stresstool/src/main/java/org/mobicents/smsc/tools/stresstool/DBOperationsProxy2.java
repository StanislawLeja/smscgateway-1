package org.mobicents.smsc.tools.stresstool;

import com.datastax.driver.core.Session;

public class DBOperationsProxy2 extends DBOper {

    public Session getSession() {
        return this.session;
    }

}
