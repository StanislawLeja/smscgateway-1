/*
 * TeleStax, Open Source Cloud Communications  Copyright 2012. 
 * and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.mobicents.smsc.smpp;

import static org.testng.Assert.*;

import org.mobicents.smsc.smpp.DbSmsRoutingRule;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * 
 * @author sergey vetyutnev
 * 
 */
public class DbSmsRoutingRuleTest {

    private PersistenceProxy sbb = new PersistenceProxy();
    private boolean cassandraDbInited;

    @BeforeClass
    public void setUpClass() throws Exception {
        System.out.println("setUpClass");

        this.cassandraDbInited = this.sbb.testCassandraAccess();
    }

    @AfterClass
    public void tearDownClass() throws Exception {
        System.out.println("tearDownClass");
    }


    @Test(groups = { "cassandra" })
    public void testingDbSmsRoutingRule() throws Exception {

        if (!this.cassandraDbInited)
            return;

        this.clearDatabase();

        DbSmsRoutingRule rl1 = this.sbb.fetchSmsRoutingRule("1111");
        DbSmsRoutingRule rl11 = this.sbb.fetchSmsRoutingRule("11111");
        DbSmsRoutingRule rl2 = this.sbb.fetchSmsRoutingRule("2222");
        assertNull(rl1);
        assertNull(rl11);
        assertNull(rl2);

        DbSmsRoutingRule rla = new DbSmsRoutingRule();
        rla.setId(1);
        rla.setAddress("1111");
        rla.setSystemId("AAA");

        this.sbb.addDbSmsRoutingRule(rla);
        rl1 = this.sbb.fetchSmsRoutingRule("1111");
        rl11 = this.sbb.fetchSmsRoutingRule("11111");
        rl2 = this.sbb.fetchSmsRoutingRule("2222");
        assertNotNull(rl1);
        assertNull(rl11);
        assertNull(rl2);
        assertEquals(rl1.getId(), 1);
        assertEquals(rl1.getAddress(), "1111");
        assertEquals(rl1.getSystemId(), "AAA");


        rla = new DbSmsRoutingRule();
        rla.setId(1);
        rla.setAddress("11111");
        rla.setSystemId("AAAA");

        this.sbb.addDbSmsRoutingRule(rla);
        rl1 = this.sbb.fetchSmsRoutingRule("1111");
        rl11 = this.sbb.fetchSmsRoutingRule("11111");
        rl2 = this.sbb.fetchSmsRoutingRule("2222");
        assertNull(rl1);
        assertNotNull(rl11);
        assertNull(rl2);
        assertEquals(rl11.getId(), 1);
        assertEquals(rl11.getAddress(), "11111");
        assertEquals(rl11.getSystemId(), "AAAA");


        rla = new DbSmsRoutingRule();
        rla.setId(2);
        rla.setAddress("2222");
        rla.setSystemId("BBB");

        this.sbb.addDbSmsRoutingRule(rla);
        rl1 = this.sbb.fetchSmsRoutingRule("1111");
        rl11 = this.sbb.fetchSmsRoutingRule("11111");
        rl2 = this.sbb.fetchSmsRoutingRule("2222");
        assertNull(rl1);
        assertNotNull(rl11);
        assertNotNull(rl2);
        assertEquals(rl11.getId(), 1);
        assertEquals(rl11.getAddress(), "11111");
        assertEquals(rl11.getSystemId(), "AAAA");
        assertEquals(rl2.getId(), 2);
        assertEquals(rl2.getAddress(), "2222");
        assertEquals(rl2.getSystemId(), "BBB");


        this.sbb.deleteDbSmsRoutingRule(1);
        rl1 = this.sbb.fetchSmsRoutingRule("1111");
        rl11 = this.sbb.fetchSmsRoutingRule("11111");
        rl2 = this.sbb.fetchSmsRoutingRule("2222");
        assertNull(rl1);
        assertNull(rl11);
        assertNotNull(rl2);
        assertEquals(rl2.getId(), 2);
        assertEquals(rl2.getAddress(), "2222");
        assertEquals(rl2.getSystemId(), "BBB");

    }

    private void clearDatabase() throws Exception {
        this.sbb.deleteDbSmsRoutingRule(1);
        this.sbb.deleteDbSmsRoutingRule(2);
    }
}
