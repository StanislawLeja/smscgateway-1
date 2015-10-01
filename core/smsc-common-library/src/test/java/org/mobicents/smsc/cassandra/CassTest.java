package org.mobicents.smsc.cassandra;

import org.testng.annotations.Test;

import com.datastax.driver.core.Session;

public class CassTest {

    @Test(groups = { "cassandra" })
    public void test1() throws Exception {
        DBOperations_C2_Proxy db = new DBOperations_C2_Proxy();
        String keySpacename = "TelestaxSMSC";
//        try {
//            db.session.execute("TRUNCATE \"" + Schema.FAMILY_CURRENT_SLOT_TABLE + "\";");
//        } catch (Exception e) {
//            int g1 = 0;
//            g1++;
//        }                
        db.start("127.0.0.1", 9042, keySpacename, 60, 60, 60 * 10);


        db.c2_setCurrentDueSlot(2009);
        db.stop();

//        Thread.sleep(1000);

        db.start("127.0.0.1", 9042, keySpacename, 60, 60, 60 * 10);

        long l1 = db.c2_getCurrentSlotTable(0);
        long l2 = db.c2_getCurrentSlotTable(0);
        long l3 = db.c2_getCurrentSlotTable(0);

        int i1 = 0;
        i1++;
    }

    public class DBOperations_C2_Proxy extends DBOperations_C2 {
        public Session getSession() {
            return super.session;
        }

        protected long c2_getCurrentSlotTable(int key) throws PersistenceException {
            return super.c2_getCurrentSlotTable(key);
        }
    }

}
