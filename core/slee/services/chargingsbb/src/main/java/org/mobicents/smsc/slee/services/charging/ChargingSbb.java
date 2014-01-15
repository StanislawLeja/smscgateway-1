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

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.slee.ActivityContextInterface;
import javax.slee.CreateException;
import javax.slee.RolledBackContext;
import javax.slee.Sbb;
import javax.slee.SbbContext;
import javax.slee.facilities.Tracer;
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

import net.java.slee.resource.diameter.cca.events.CreditControlAnswer;
import net.java.slee.resource.diameter.ro.RoActivityContextInterfaceFactory;

/**
 * 
 * @author sergey vetyutnev
 * 
 */
public abstract class ChargingSbb implements Sbb {

    protected static SmscPropertiesManagement smscPropertiesManagement = SmscPropertiesManagement.getInstance();

    private static final ResourceAdaptorTypeID DIAMETER_ID = new ResourceAdaptorTypeID("Diameter Ro", "java.net", "0.8.1");
    private static final String LINK_DIAM = "DiameterRo";
    private static final ResourceAdaptorTypeID PERSISTENCE_ID = new ResourceAdaptorTypeID("PersistenceResourceAdaptorType", "org.mobicents", "1.0");
    private static final String LINK_PERS = "PersistenceResourceAdaptor";

    protected Tracer logger;
    private SbbContextExt sbbContext;

    private RoActivityContextInterfaceFactory roActivityContextInterfaceFactory;
    private PersistenceRAInterface persistence;


    public ChargingSbb() {
    }

    @Override
    public void sbbActivate() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void sbbCreate() throws CreateException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void sbbExceptionThrown(Exception arg0, Object arg1, ActivityContextInterface arg2) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void sbbLoad() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void sbbPassivate() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void sbbPostCreate() throws CreateException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void sbbRemove() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void sbbRolledBack(RolledBackContext arg0) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void sbbStore() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setSbbContext(SbbContext arg0) {
        this.sbbContext = (SbbContextExt) sbbContext;

        try {
            Context ctx = (Context) new InitialContext().lookup("java:comp/env");

            this.logger = this.sbbContext.getTracer(getClass().getSimpleName());
            
            this.roActivityContextInterfaceFactory = (RoActivityContextInterfaceFactory) this.sbbContext.getResourceAdaptorInterface(DIAMETER_ID, LINK_DIAM);
            this.persistence = (PersistenceRAInterface) this.sbbContext.getResourceAdaptorInterface(PERSISTENCE_ID, LINK_PERS);
        } catch (Exception ne) {
            logger.severe("Could not set SBB context:", ne);
        }
    }


    @Override
    public void unsetSbbContext() {
        // TODO Auto-generated method stub
        
    }


    // Setup charging request

    public void setupChargingRequestInterface(ChargingType chargingType, Sms sms) {

        logger.info("ChargingSbb: received message for process charging process: chargingType=" + chargingType + ", message=[" + sms + "]");

        // TODO: implement it

        // TODO: here for chargingType==TxSmppOrig and not storeAndForwMode mode is declared at SMSC
        // we need to launch direct (not store at the database)

        // now we just storing a message
        try {
            this.storeSms(sms);
        } catch (SmscProcessingException e) {
            logger.severe("ChargingSbb: error while storeing message into database: " + e.getMessage(), e);
        }
    }


    // CMP

    public abstract void setChargingData(ChargingData chargingData);

    public abstract ChargingData getChargingData();


    // Events

    public void onCreditControlAnswer(CreditControlAnswer evt, ActivityContextInterface aci) {
        // TODO: implement it
    }

    private void storeSms(Sms sms) throws SmscProcessingException {
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
}
