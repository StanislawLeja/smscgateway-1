package org.mobicents.smsc.domain;

import static org.testng.Assert.*;

import org.mobicents.smsc.domain.SmscPropertiesManagement;
import org.testng.annotations.Test;

public class SmscPropertiesManagementTest {

    @Test(groups = { "management" })
    public void testPropertiesLoad() throws Exception {
        SmscPropertiesManagement man = SmscPropertiesManagement.getInstance("SmscPropertiesManagementTest");
        man.start();

        man.setSMSHomeRouting(true);
        man.setServiceCenterGtNetworkId(1, "22229");
       
        man.stop();
        
        man.start();
        
        assertTrue(man.getSMSHomeRouting());
        assertEquals(man.getServiceCenterGt(1), "22229");        
        assertNull(man.getServiceCenterGt(2));        
        
    }
}
