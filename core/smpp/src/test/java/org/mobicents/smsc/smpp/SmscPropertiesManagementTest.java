package org.mobicents.smsc.smpp;

import static org.testng.Assert.*;
import org.testng.annotations.Test;

public class SmscPropertiesManagementTest {

    @Test(groups = { "management" })
    public void testPropertiesLoad() throws Exception {
        SmscPropertiesManagement man = SmscPropertiesManagement.getInstance("SmscPropertiesManagementTest");
        man.start();
        
        man.setSMSHomeRouting(true);
        
        man.stop();
        
        man.start();
        
        assertTrue(man.getSMSHomeRouting());
        
        
    }
}
