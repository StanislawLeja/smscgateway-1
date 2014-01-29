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

package org.mobicents.smsc.slee.services.charging;

import java.util.ArrayList;
import java.util.Calendar;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.slee.ActivityContextInterface;
import javax.slee.ActivityEndEvent;
import javax.slee.CreateException;
import javax.slee.RolledBackContext;
import javax.slee.Sbb;
import javax.slee.SbbContext;
import javax.slee.facilities.TimerFacility;
import javax.slee.facilities.Tracer;
import javax.slee.nullactivity.NullActivityContextInterfaceFactory;
import javax.slee.nullactivity.NullActivityFactory;
import javax.slee.resource.ResourceAdaptorTypeID;

import org.mobicents.protocols.ss7.map.api.errors.MAPErrorCode;
import org.mobicents.slee.SbbContextExt;
import org.mobicents.smsc.cassandra.DatabaseType;
import org.mobicents.smsc.cassandra.PersistenceException;
import org.mobicents.smsc.cassandra.Sms;
import org.mobicents.smsc.slee.resources.persistence.MessageUtil;
import org.mobicents.smsc.slee.resources.persistence.PersistenceRAInterface;
import org.mobicents.smsc.slee.resources.persistence.SmscProcessingException;
import org.mobicents.smsc.smpp.SmscPropertiesManagement;

import com.cloudhopper.smpp.SmppConstants;

import net.java.slee.resource.diameter.base.events.avp.DiameterAvp;
import net.java.slee.resource.diameter.base.events.avp.DiameterIdentity;
import net.java.slee.resource.diameter.cca.events.avp.CcRequestType;
import net.java.slee.resource.diameter.cca.events.avp.RequestedActionType;
import net.java.slee.resource.diameter.cca.events.avp.SubscriptionIdAvp;
import net.java.slee.resource.diameter.cca.events.avp.SubscriptionIdType;
import net.java.slee.resource.diameter.ro.RoActivityContextInterfaceFactory;
import net.java.slee.resource.diameter.ro.RoAvpFactory;
import net.java.slee.resource.diameter.ro.RoClientSessionActivity;
import net.java.slee.resource.diameter.ro.RoMessageFactory;
import net.java.slee.resource.diameter.ro.RoProvider;
import net.java.slee.resource.diameter.ro.events.RoCreditControlAnswer;
import net.java.slee.resource.diameter.ro.events.RoCreditControlRequest;
import net.java.slee.resource.diameter.ro.events.avp.ServiceInformation;

/**
 * 
 * @author sergey vetyutnev
 * 
 */
public abstract class ChargingSbb implements Sbb {

    public static final String SERVICE_CONTEXT_ID_SMSC = "32274@3gpp.org";
    public static final int APPLICATION_ID_OF_THE_DIAMETER_CREDIT_CONTROL_APPLICATION = 4;

    protected static SmscPropertiesManagement smscPropertiesManagement = SmscPropertiesManagement.getInstance();

    private static final ResourceAdaptorTypeID DIAMETER_ID = new ResourceAdaptorTypeID("Diameter Ro", "java.net", "0.8.1");
    private static final String LINK_DIAM = "DiameterRo";
    private static final ResourceAdaptorTypeID PERSISTENCE_ID = new ResourceAdaptorTypeID("PersistenceResourceAdaptorType", "org.mobicents", "1.0");
    private static final String LINK_PERS = "PersistenceResourceAdaptor";

//    private static String originIP = "127.0.0.1";
//    private static String originPort = "1812";
//    private static String originRealm = "mobicents.org";
//
//    private static String destinationIP = "127.0.0.1";
//    private static String destinationPort = "3868";
//    private static String destinationRealm = "mobicents.org";
    
    protected Tracer logger;
    private SbbContextExt sbbContext;

    private RoProvider roProvider;
    private RoMessageFactory roMessageFactory;
    private RoAvpFactory avpFactory;
    private RoActivityContextInterfaceFactory acif;

    private TimerFacility timerFacility = null;
    private NullActivityFactory nullActivityFactory;
    private NullActivityContextInterfaceFactory nullACIFactory;

    private PersistenceRAInterface persistence;


    public ChargingSbb() {
    }

    @Override
    public void sbbActivate() {
        logger.info("sbbActivate invoked.");
    }

    @Override
    public void sbbCreate() throws CreateException {
        logger.info("sbbCreate invoked.");
    }

    @Override
    public void sbbExceptionThrown(Exception arg0, Object arg1, ActivityContextInterface arg2) {
        logger.info("sbbExceptionThrown invoked.");
    }

    @Override
    public void sbbLoad() {
        logger.info("sbbLoad invoked.");
    }

    @Override
    public void sbbPassivate() {
        logger.info("sbbPassivate invoked.");
    }

    @Override
    public void sbbPostCreate() throws CreateException {
        logger.info("sbbPostCreate invoked.");
    }

    @Override
    public void sbbRemove() {
        logger.info("sbbRemove invoked.");
    }

    @Override
    public void sbbRolledBack(RolledBackContext arg0) {
        logger.info("sbbRolledBack invoked.");
    }

    @Override
    public void sbbStore() {
        logger.info("sbbStore invoked.");
    }

    @Override
    public void setSbbContext(SbbContext sbbContext) {
        this.sbbContext = (SbbContextExt) sbbContext;

        try {
            Context ctx = (Context) new InitialContext().lookup("java:comp/env");

            this.logger = this.sbbContext.getTracer(getClass().getSimpleName());

            logger.info("setSbbContext invoked.");

            this.roProvider = (RoProvider) this.sbbContext.getResourceAdaptorInterface(DIAMETER_ID, LINK_DIAM);

            roMessageFactory = roProvider.getRoMessageFactory();
            avpFactory = roProvider.getRoAvpFactory();

            acif = (RoActivityContextInterfaceFactory) ctx.lookup("slee/resources/JDiameterRoResourceAdaptor/java.net/0.8.1/acif");

            // SLEE Facilities
            timerFacility = (TimerFacility) ctx.lookup("slee/facilities/timer");
            nullActivityFactory = (NullActivityFactory) ctx.lookup("slee/nullactivity/factory");
            nullACIFactory = (NullActivityContextInterfaceFactory) ctx.lookup("slee/nullactivity/activitycontextinterfacefactory");

            this.persistence = (PersistenceRAInterface) this.sbbContext.getResourceAdaptorInterface(PERSISTENCE_ID, LINK_PERS);
        } catch (Exception ne) {
            logger.severe("Could not set SBB context:", ne);
        }
    }


    @Override
    public void unsetSbbContext() {
        logger.info("unsetSbbContext invoked.");

        this.sbbContext = null;
    }

    public void onActivityEndEvent(ActivityEndEvent event, ActivityContextInterface aci) {
        logger.info(" Activity Ended[" + aci.getActivity() + "]");
    }


    // Setup charging request

    public void setupChargingRequestInterface(ChargingType chargingType, Sms sms) {
        logger.info("ChargingSbb: received message for process charging process: chargingType=" + chargingType + ", message=[" + sms + "]");

        ChargingData chargingData = new ChargingData();
        chargingData.setSms(sms);
        chargingData.setChargingType(chargingType);
        this.setChargingData(chargingData);

        String msisdn = sms.getSourceAddr();

        try {




//            ServerTransaction serverTransaction = requestEvent.getServerTransaction();
//            DialogActivity dialog = (DialogActivity) sipProvider.getNewDialog(serverTransaction);
//            dialog.terminateOnBye(true);
//            sipAcif.getActivityContextInterface(dialog).attach(this.sbbContext.getSbbLocalObject());





            DiameterIdentity destRealm = new DiameterIdentity(smscPropertiesManagement.getDiameterDestRealm());
//            RoClientSession roSession = roProvider.createRoClientSessionActivity(destHost, destRealm);
            RoClientSessionActivity activity = this.roProvider.createRoClientSessionActivity(null, destRealm);
            ActivityContextInterface roACI = acif.getActivityContextInterface(activity);
            roACI.attach(getSbbContext().getSbbLocalObject());

            RoCreditControlRequest ccr = activity.createRoCreditControlRequest(CcRequestType.EVENT_REQUEST);

            ccr.setDestinationRealm(destRealm);
            ccr.setAuthApplicationId(APPLICATION_ID_OF_THE_DIAMETER_CREDIT_CONTROL_APPLICATION);
            ccr.setServiceContextId(SERVICE_CONTEXT_ID_SMSC);
            ccr.setCcRequestNumber(0);
            // destHost may be null, in this case it will be determined by destRealm

            if (smscPropertiesManagement.getDiameterDestHost() != null) {
                DiameterIdentity destHost = new DiameterIdentity("aaa://" + smscPropertiesManagement.getDiameterDestHost() + ":" + smscPropertiesManagement.getDiameterDestPort());
                ccr.setDestinationHost(destHost);
            }

            // Contains the user name determined by the domain:
            // bearer, sub-system or service as described in middle tier TS.
            // contains Network Access Identifier (NAI).
            // for SIP: name = ((SipUri)fromHeader.getAddress().getURI()).getUser();
            if (smscPropertiesManagement.getDiameterUserName() != null) {
                ccr.setUserName(smscPropertiesManagement.getDiameterUserName());
            }

            // This field contains the state associated to the CTF
            // do not know how to use it
            // a monotonically increasing value that is advanced whenever a Diameter
            // entity restarts with loss of previous state, for example upon reboot
            // ccr.setOriginStateId(smscRebootStep);

            // do not know if we need it
            ccr.setEventTimestamp(Calendar.getInstance().getTime());

            SubscriptionIdAvp subId = avpFactory.createSubscriptionId(SubscriptionIdType.END_USER_E164, msisdn);
            ccr.setSubscriptionId(subId);

            ccr.setRequestedAction(RequestedActionType.DIRECT_DEBITING);

//            ccr.setMultipleServicesIndicator(MultipleServicesIndicatorType.MULTIPLE_SERVICES_NOT_SUPPORTED);

//            RequestedServiceUnitAvp RSU = avpFactory.createRequestedServiceUnit();
//            RSU.setCreditControlTime(_FIRST_CHARGE_TIME);
//            ccr.setRequestedServiceUnit(RSU);

            // ServiceInformation - SMS info
            ArrayList<DiameterAvp> smsInfoAvpLst = new ArrayList<DiameterAvp>();
            int vendorID = 10415;

            ArrayList<DiameterAvp> originatorReceivedAddressAvpLst = new ArrayList<DiameterAvp>();
            DiameterAvp avpAddressType = avpFactory.getBaseFactory().createAvp(vendorID, 899, AddressTypeEnum.Msisdn);
            originatorReceivedAddressAvpLst.add(avpAddressType);
            DiameterAvp avpAddressData = avpFactory.getBaseFactory().createAvp(vendorID, 897, msisdn);
            originatorReceivedAddressAvpLst.add(avpAddressData);
            DiameterAvp[] originatorReceivedAddressAvpArr = new DiameterAvp[originatorReceivedAddressAvpLst.size()];
            originatorReceivedAddressAvpLst.toArray(originatorReceivedAddressAvpArr);

            DiameterAvp avpOriginatorReceivedAddress = avpFactory.getBaseFactory().createAvp(vendorID, 2027, originatorReceivedAddressAvpArr);
            smsInfoAvpLst.add(avpOriginatorReceivedAddress);

            DiameterAvp[] smsInfoAvpArr = new DiameterAvp[smsInfoAvpLst.size()];
            smsInfoAvpLst.toArray(smsInfoAvpArr);

            DiameterAvp[] smsInfo = new DiameterAvp[1];
            smsInfo[0] = avpFactory.getBaseFactory().createAvp(vendorID, 2000, smsInfoAvpArr);
            ServiceInformation si = avpFactory.createServiceInformation();
            si.setExtensionAvps(smsInfo);
            ccr.setServiceInformation(si);

            activity.sendEventRoCreditControlRequest(ccr);
            logger.info("Sent INITIAL CCR: \n"+ccr);
        } catch (Exception e1) {
            logger.severe("setupChargingRequestInterface(): error while sending RoCreditControlRequest: " + e1.getMessage(), e1);
        }
        
        
        
        // ....................................
        // ....................................
        
        
        // TODO: implement it

        // TODO: here for chargingType==TxSmppOrig and not storeAndForwMode mode is declared at SMSC
        // we need to launch direct (not store at the database)

        // now we just storing a message
//        try {
//            this.storeSms(sms);
//        } catch (SmscProcessingException e) {
//            logger.severe("ChargingSbb: error while storeing message into database: " + e.getMessage(), e);
//        }
    }


    // CMP

    public abstract void setChargingData(ChargingData chargingData);

    public abstract ChargingData getChargingData();


    // Events

    public void onRoCreditControlAnswer(RoCreditControlAnswer evt, ActivityContextInterface aci) {
        logger.info("RoCreditControlAnswer received: " + evt);

        // TODO: implement it
    }

    private void acceptSms(Sms sms) throws SmscProcessingException {
        try {
            sms.setStored(true);
            if (smscPropertiesManagement.getDatabaseType() == DatabaseType.Cassandra_1) {
                persistence.createLiveSms(sms);
                persistence.setNewMessageScheduled(sms.getSmsSet(), MessageUtil.computeDueDate(MessageUtil.computeFirstDueDelay()));
            } else {
                sms.setStored(true);
                persistence.c2_scheduleMessage(sms);
            }
        } catch (PersistenceException e) {
            throw new SmscProcessingException("MO PersistenceException when storing LIVE_SMS : " + e.getMessage(), SmppConstants.STATUS_SUBMITFAIL,
                    MAPErrorCode.systemFailure, null, e);
        }
    }

    private void rejectSms(Sms sms) {
        // TODO: implement it
    }

    protected SbbContext getSbbContext() {
        return sbbContext;
    }
    
    public enum AddressTypeEnum implements net.java.slee.resource.diameter.base.events.avp.Enumerated {
        Msisdn(1);

        private int code;

        private AddressTypeEnum(int code) {
            this.code = code;
        }

        @Override
        public int getValue() {
            return code;
        }
    }
}
