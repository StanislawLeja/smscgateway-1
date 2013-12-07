package org.mobicents.smsc.tools.stresstool;

import com.datastax.driver.core.Session;

public class DBOperationsProxy3 extends DBOper2 {

    public Session getSession() {
        return this.session;
    }

}
