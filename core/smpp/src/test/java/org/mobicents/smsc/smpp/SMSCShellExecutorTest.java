package org.mobicents.smsc.smpp;

import java.util.Date;

import javax.slee.facilities.FacilityException;
import javax.slee.facilities.TraceLevel;

import org.mobicents.smsc.cassandra.DBOperations_C2;
import org.testng.annotations.Test;

public class SMSCShellExecutorTest {

    @Test(groups = { "ShellExecutor" })
    public void testShellExecutor() throws Exception {
        Date dt = new Date();
        String s1 = dt.toGMTString();

        long l2 = Date.parse(s1);
        Date d2 = new Date(l2);

        SMSCShellExecutor exec = new SMSCShellExecutor();
        String[] args = new String[2];
        args[0] = "stat";
        args[1] = "get";
        exec.execute(args);
    }

//    @Test(groups = { "ShellExecutor" })
//    public void testSchedulerRA() throws Exception {
//        SchedulerResourceAdaptorProxy ra = new SchedulerResourceAdaptorProxy();
//
//        SmscPropertiesManagement.getInstance().setFetchMaxRows(4);
//
//        while (true) {
//            ra.onTimerTick();
//        }
//    }

//    class SchedulerResourceAdaptorProxy extends SchedulerResourceAdaptor {
//
//        public SchedulerResourceAdaptorProxy() throws Exception {
//            this.tracer = new TracerProxy();
//            SmscPropertiesManagement prop = SmscPropertiesManagement.getInstance("Test");
//            String[] hostsArr = prop.getHosts().split(":");
//            String host = hostsArr[0];
//            int port = Integer.parseInt(hostsArr[1]);
//
//            this.dbOperations_C2 = DBOperations_C2.getInstance();
//            this.dbOperations_C2.start(host, port, prop.getKeyspaceName(), prop.getFirstDueDelay(), prop.getReviseSecondsOnSmscStart(),
//                    prop.getProcessingSmsSetTimeout());
//        }
//
//        public void onTimerTick() {
//            super.onTimerTick();
//        }
//    }
    
    class TracerProxy implements javax.slee.facilities.Tracer {

        @Override
        public void config(String arg0) throws NullPointerException, FacilityException {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void config(String arg0, Throwable arg1) throws NullPointerException, FacilityException {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void fine(String arg0) throws NullPointerException, FacilityException {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void fine(String arg0, Throwable arg1) throws NullPointerException, FacilityException {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void finer(String arg0) throws NullPointerException, FacilityException {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void finer(String arg0, Throwable arg1) throws NullPointerException, FacilityException {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void finest(String arg0) throws NullPointerException, FacilityException {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void finest(String arg0, Throwable arg1) throws NullPointerException, FacilityException {
            // TODO Auto-generated method stub
            
        }

        @Override
        public String getParentTracerName() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public TraceLevel getTraceLevel() throws FacilityException {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public String getTracerName() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public void info(String arg0) throws NullPointerException, FacilityException {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void info(String arg0, Throwable arg1) throws NullPointerException, FacilityException {
            // TODO Auto-generated method stub
            
        }

        @Override
        public boolean isConfigEnabled() throws FacilityException {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public boolean isFineEnabled() throws FacilityException {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public boolean isFinerEnabled() throws FacilityException {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public boolean isFinestEnabled() throws FacilityException {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public boolean isInfoEnabled() throws FacilityException {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public boolean isSevereEnabled() throws FacilityException {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public boolean isTraceable(TraceLevel arg0) throws NullPointerException, FacilityException {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public boolean isWarningEnabled() throws FacilityException {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public void severe(String arg0) throws NullPointerException, FacilityException {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void severe(String arg0, Throwable arg1) throws NullPointerException, FacilityException {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void trace(TraceLevel arg0, String arg1) throws NullPointerException, IllegalArgumentException, FacilityException {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void trace(TraceLevel arg0, String arg1, Throwable arg2) throws NullPointerException, IllegalArgumentException, FacilityException {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void warning(String arg0) throws NullPointerException, FacilityException {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void warning(String arg0, Throwable arg1) throws NullPointerException, FacilityException {
            // TODO Auto-generated method stub
            
        }
    }
}

