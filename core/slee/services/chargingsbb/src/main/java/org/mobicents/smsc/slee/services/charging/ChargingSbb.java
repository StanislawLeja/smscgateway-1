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
import javax.slee.facilities.TimerEvent;
import javax.slee.facilities.TimerFacility;
import javax.slee.facilities.TimerOptions;
import javax.slee.facilities.TimerPreserveMissed;
import javax.slee.facilities.Tracer;
import javax.slee.nullactivity.NullActivityContextInterfaceFactory;
import javax.slee.nullactivity.NullActivityFactory;
import javax.slee.resource.ResourceAdaptorTypeID;

import net.java.slee.resource.diameter.base.events.avp.DiameterAvp;
import net.java.slee.resource.diameter.base.events.avp.DiameterIdentity;
import net.java.slee.resource.diameter.cca.events.avp.CcRequestType;
import net.java.slee.resource.diameter.cca.events.avp.MultipleServicesCreditControlAvp;
import net.java.slee.resource.diameter.cca.events.avp.RequestedActionType;
import net.java.slee.resource.diameter.cca.events.avp.RequestedServiceUnitAvp;
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

import org.mobicents.protocols.ss7.map.api.errors.MAPErrorCode;
import org.mobicents.slee.SbbContextExt;
import org.mobicents.smsc.cassandra.DatabaseType;
import org.mobicents.smsc.cassandra.MessageDeliveryResultResponseInterface;
import org.mobicents.smsc.cassandra.PersistenceException;
import org.mobicents.smsc.domain.SmscPropertiesManagement;
import org.mobicents.smsc.domain.SmscStatAggregator;
import org.mobicents.smsc.domain.StoreAndForwordMode;
import org.mobicents.smsc.library.CdrGenerator;
import org.mobicents.smsc.library.ErrorCode;
import org.mobicents.smsc.library.MessageUtil;
import org.mobicents.smsc.library.Sms;
import org.mobicents.smsc.library.SmscProcessingException;
import org.mobicents.smsc.slee.resources.persistence.PersistenceRAInterface;
import org.mobicents.smsc.slee.resources.scheduler.SchedulerRaSbbInterface;

import com.cloudhopper.smpp.SmppConstants;

/**
 * 
 * @author sergey vetyutnev
 * 
 */
public abstract class ChargingSbb implements Sbb {

	public static final String SERVICE_CONTEXT_ID_SMSC = "32274@3gpp.org";
	public static final int APPLICATION_ID_OF_THE_DIAMETER_CREDIT_CONTROL_APPLICATION = 4;
	public static final int CCR_TIMEOUT = 15;

	protected static SmscPropertiesManagement smscPropertiesManagement = SmscPropertiesManagement.getInstance();

	private static final ResourceAdaptorTypeID DIAMETER_ID = new ResourceAdaptorTypeID("Diameter Ro", "java.net",
			"0.8.1");
	private static final String LINK_DIAM = "DiameterRo";
	private static final ResourceAdaptorTypeID PERSISTENCE_ID = new ResourceAdaptorTypeID(
			"PersistenceResourceAdaptorType", "org.mobicents", "1.0");
	private static final String LINK_PERS = "PersistenceResourceAdaptor";
    private static final ResourceAdaptorTypeID SCHEDULER_ID = new ResourceAdaptorTypeID(
            "SchedulerResourceAdaptorType", "org.mobicents", "1.0");
    private static final String SCHEDULER_LINK = "SchedulerResourceAdaptor";

	// private static String originIP = "127.0.0.1";
	// private static String originPort = "1812";
	// private static String originRealm = "mobicents.org";
	//
	// private static String destinationIP = "127.0.0.1";
	// private static String destinationPort = "3868";
	// private static String destinationRealm = "mobicents.org";

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
    private SmscStatAggregator smscStatAggregator = SmscStatAggregator.getInstance();
    protected SchedulerRaSbbInterface scheduler = null;

	private static final TimerOptions defaultTimerOptions = createDefaultTimerOptions();
	private NullActivityContextInterfaceFactory nullActivityContextInterfaceFactory;

	private static TimerOptions createDefaultTimerOptions() {
		TimerOptions timerOptions = new TimerOptions();
		timerOptions.setPreserveMissed(TimerPreserveMissed.ALL);
		return timerOptions;
	}

	// public abstract SbbActivityContextInterface
	// asSbbActivityContextInterface(ActivityContextInterface aci);

	public ChargingSbb() {
	}

	@Override
	public void sbbActivate() {
		if (logger.isFineEnabled()) {
			logger.fine("sbbActivate invoked.");
		}
	}

	@Override
	public void sbbCreate() throws CreateException {
		if (logger.isFineEnabled()) {
			logger.fine("sbbCreate invoked.");
		}
	}

	@Override
	public void sbbExceptionThrown(Exception arg0, Object arg1, ActivityContextInterface arg2) {
		if (logger.isFineEnabled()) {
			logger.fine("sbbExceptionThrown invoked.");
		}
	}

	@Override
	public void sbbLoad() {
		if (logger.isFineEnabled()) {
			logger.fine("sbbLoad invoked.");
		}
	}

	@Override
	public void sbbPassivate() {
		if (logger.isFineEnabled()) {
			logger.fine("sbbPassivate invoked.");
		}
	}

	@Override
	public void sbbPostCreate() throws CreateException {
		if (logger.isFineEnabled()) {
			logger.fine("sbbPostCreate invoked.");
		}
	}

	@Override
	public void sbbRemove() {
		if (logger.isFineEnabled()) {
			logger.fine("sbbRemove invoked.");
		}
	}

	@Override
	public void sbbRolledBack(RolledBackContext arg0) {
		if (logger.isFineEnabled()) {
			logger.fine("sbbRolledBack invoked.");
		}
	}

	@Override
	public void sbbStore() {
		if (logger.isFineEnabled()) {
			logger.fine("sbbStore invoked.");
		}
	}

	@Override
	public void setSbbContext(SbbContext sbbContext) {
		this.sbbContext = (SbbContextExt) sbbContext;

		try {
			Context ctx = (Context) new InitialContext().lookup("java:comp/env");

			this.logger = this.sbbContext.getTracer(getClass().getSimpleName());

			if (logger.isFineEnabled()) {
				logger.fine("setSbbContext invoked.");
			}

			this.roProvider = (RoProvider) this.sbbContext.getResourceAdaptorInterface(DIAMETER_ID, LINK_DIAM);

			roMessageFactory = roProvider.getRoMessageFactory();
			avpFactory = roProvider.getRoAvpFactory();

			acif = (RoActivityContextInterfaceFactory) ctx
					.lookup("slee/resources/JDiameterRoResourceAdaptor/java.net/0.8.1/acif");

			// SLEE Facilities
			timerFacility = (TimerFacility) ctx.lookup("slee/facilities/timer");
			nullActivityFactory = (NullActivityFactory) ctx.lookup("slee/nullactivity/factory");
			nullACIFactory = (NullActivityContextInterfaceFactory) ctx
					.lookup("slee/nullactivity/activitycontextinterfacefactory");

            this.persistence = (PersistenceRAInterface) this.sbbContext.getResourceAdaptorInterface(PERSISTENCE_ID, LINK_PERS);
            this.scheduler = (SchedulerRaSbbInterface) this.sbbContext.getResourceAdaptorInterface(SCHEDULER_ID, SCHEDULER_LINK);
		} catch (Exception ne) {
			logger.severe("Could not set SBB context:", ne);
		}
	}

	@Override
	public void unsetSbbContext() {
		if (logger.isFineEnabled()) {
			logger.fine("unsetSbbContext invoked.");
		}

		this.sbbContext = null;
	}

	public void onActivityEndEvent(ActivityEndEvent event, ActivityContextInterface aci) {
		logger.info(" Activity Ended[" + aci.getActivity() + "]");
	}

	// Setup charging request

	public void setupChargingRequestInterface(ChargingMedium chargingType, Sms sms) {
		if (logger.isFineEnabled()) {
			logger.fine("ChargingSbb: received message for process charging process: chargingType=" + chargingType
					+ ", message=[" + sms + "]");
		}

		ChargingData chargingData = new ChargingData();
		chargingData.setSms(sms);
		chargingData.setChargingType(chargingType);
		this.setChargingData(chargingData);

		String msisdn = sms.getSourceAddr();

		try {

			DiameterIdentity destHost = null;
			if (smscPropertiesManagement.getDiameterDestHost() != null
					&& !smscPropertiesManagement.getDiameterDestHost().equals("")) {
				// destHost = new DiameterIdentity("aaa://" +
				// smscPropertiesManagement.getDiameterDestHost() + ":" +
				// smscPropertiesManagement.getDiameterDestPort());
				destHost = new DiameterIdentity(smscPropertiesManagement.getDiameterDestHost());
			}
			DiameterIdentity destRealm = new DiameterIdentity(smscPropertiesManagement.getDiameterDestRealm());
			RoClientSessionActivity activity = this.roProvider.createRoClientSessionActivity(destHost, destRealm);
			ActivityContextInterface roACI = acif.getActivityContextInterface(activity);
			roACI.attach(getSbbContext().getSbbLocalObject());

			RoCreditControlRequest ccr = activity.createRoCreditControlRequest(CcRequestType.EVENT_REQUEST);

			// ccr.setDestinationRealm(destRealm);
			// ccr.setAuthApplicationId(APPLICATION_ID_OF_THE_DIAMETER_CREDIT_CONTROL_APPLICATION);
			ccr.setServiceContextId(SERVICE_CONTEXT_ID_SMSC);
			ccr.setCcRequestNumber(0);

			// destHost may be null, in this case it will be determined by
			// destRealm
			// ccr.setDestinationHost(destHost);

			// Contains the user name determined by the domain:
			// bearer, sub-system or service as described in middle tier TS.
			// contains Network Access Identifier (NAI).
			// for SIP: name =
			// ((SipUri)fromHeader.getAddress().getURI()).getUser();
			if (smscPropertiesManagement.getDiameterUserName() != null
					&& !smscPropertiesManagement.getDiameterUserName().equals("")) {
				ccr.setUserName(smscPropertiesManagement.getDiameterUserName());
			}

			// This field contains the state associated to the CTF
			// do not know how to use it
			// a monotonically increasing value that is advanced whenever a
			// Diameter
			// entity restarts with loss of previous state, for example upon
			// reboot
			// ccr.setOriginStateId(smscRebootStep);

			// do not know if we need it
			ccr.setEventTimestamp(Calendar.getInstance().getTime());

			SubscriptionIdAvp subId = avpFactory.createSubscriptionId(SubscriptionIdType.END_USER_E164, msisdn);
			ccr.setSubscriptionId(subId);

			ccr.setRequestedAction(RequestedActionType.DIRECT_DEBITING);

			// ccr.setMultipleServicesIndicator(MultipleServicesIndicatorType.MULTIPLE_SERVICES_NOT_SUPPORTED);

			// requested units
			int messageCount = 1;
			int serviceIdentifier = 1;

			MultipleServicesCreditControlAvp multipleServicesCreditControl = avpFactory
					.createMultipleServicesCreditControl();

			RequestedServiceUnitAvp requestedServiceUnit = avpFactory.createRequestedServiceUnit();
			requestedServiceUnit.setCreditControlServiceSpecificUnits(messageCount);
			multipleServicesCreditControl.setRequestedServiceUnit(requestedServiceUnit);

			multipleServicesCreditControl.setServiceIdentifier(serviceIdentifier);

			ccr.setMultipleServicesCreditControl(multipleServicesCreditControl);

			// RequestedServiceUnitAvp RSU =
			// avpFactory.createRequestedServiceUnit();
			// RSU.setCreditControlTime(_FIRST_CHARGE_TIME);
			// ccr.setRequestedServiceUnit(RSU);

			// ServiceInformation - SMS info
			ArrayList<DiameterAvp> smsInfoAvpLst = new ArrayList<DiameterAvp>();
			int vendorID = 10415;

			ArrayList<DiameterAvp> originatorReceivedAddressAvpLst = new ArrayList<DiameterAvp>();
			DiameterAvp avpAddressType = avpFactory.getBaseFactory().createAvp(vendorID, 899,
					AddressTypeEnum.Msisdn.getValue());
			originatorReceivedAddressAvpLst.add(avpAddressType);
			DiameterAvp avpAddressData = avpFactory.getBaseFactory().createAvp(vendorID, 897, msisdn);
			originatorReceivedAddressAvpLst.add(avpAddressData);
			DiameterAvp[] originatorReceivedAddressAvpArr = new DiameterAvp[originatorReceivedAddressAvpLst.size()];
			originatorReceivedAddressAvpLst.toArray(originatorReceivedAddressAvpArr);

			DiameterAvp avpOriginatorReceivedAddress = avpFactory.getBaseFactory().createAvp(vendorID, 2027,
					originatorReceivedAddressAvpArr);
			smsInfoAvpLst.add(avpOriginatorReceivedAddress);

			DiameterAvp[] smsInfoAvpArr = new DiameterAvp[smsInfoAvpLst.size()];
			smsInfoAvpLst.toArray(smsInfoAvpArr);

			DiameterAvp[] smsInfo = new DiameterAvp[1];
			smsInfo[0] = avpFactory.getBaseFactory().createAvp(vendorID, 2000, smsInfoAvpArr);
			ServiceInformation si = avpFactory.createServiceInformation();
			si.setExtensionAvps(smsInfo);
			ccr.setServiceInformation(si);

			activity.sendEventRoCreditControlRequest(ccr);
			if (logger.isFineEnabled()) {
				logger.fine("Sent INITIAL CCR: \n" + ccr);
			}

			// set new timer for the case we will not get CCA in time
			timerFacility.setTimer(roACI, null, System.currentTimeMillis() + (CCR_TIMEOUT * 1000), defaultTimerOptions);
		} catch (Exception e1) {
			logger.severe(
					"setupChargingRequestInterface(): error while sending RoCreditControlRequest: " + e1.getMessage(),
					e1);
		}
	}

	// CMP

	public abstract void setChargingData(ChargingData chargingData);

	public abstract ChargingData getChargingData();

	// Events

	public void onRoCreditControlAnswer(RoCreditControlAnswer cca, ActivityContextInterface aci) {
		if (logger.isFineEnabled()) {
			logger.fine("RoCreditControlAnswer received: " + cca);
		}

		ChargingData chargingData = getChargingData();
		if (chargingData == null) {
			logger.warning("RoCreditControlAnswer is recieved but chargingData is null");
			return;
		}

		try {
			long resultCode = cca.getResultCode();

			if (resultCode == 2001) { // access granted
				acceptSms(chargingData);
			} else { // access rejected
				rejectSms(chargingData, cca);
			}
		} catch (Throwable e) {
			logger.warning("Exception when processing RoCreditControlAnswer response: " + e.getMessage(), e);
		}
	}

	public void onTimerEvent(TimerEvent timer, ActivityContextInterface aci) {
		ChargingData chargingData = getChargingData();
		if (chargingData == null) {
			logger.warning("RoCreditControlAnswer is recieved but chargingData is null");
			return;
		}

		if (logger.isInfoEnabled()) {
			logger.info("Timeout waiting for CCA for: " + chargingData);
		}

		// detach from this activity, we don't want to handle any other event on
		// it
		aci.detach(this.sbbContext.getSbbLocalObject());

		try {
			rejectSms(chargingData, null);
		} catch (Throwable e) {
			logger.warning("Exception when processing onTimerEvent response: " + e.getMessage(), e);
		}
	}

	private void acceptSms(ChargingData chargingData) throws SmscProcessingException {
		Sms sms = chargingData.getSms();
		if (logger.isInfoEnabled()) {
			logger.info("ChargingSbb: accessGranted for: chargingType=" + chargingData.getChargingType()
					+ ", message=[" + sms + "]");
		}

		try {
            boolean storeAndForwMode = MessageUtil.isStoreAndForward(sms);
            if (!storeAndForwMode) {
                try {
                    this.scheduler.injectSmsOnFly(sms.getSmsSet());
                } catch (Exception e) {
                    throw new SmscProcessingException("Exception when runnung injectSmsOnFly(): " + e.getMessage(), SmppConstants.STATUS_SYSERR,
                            MAPErrorCode.systemFailure, e);
                }
            } else {
                if (smscPropertiesManagement.getStoreAndForwordMode() == StoreAndForwordMode.fast) {
                    try {
                        sms.setStoringAfterFailure(true);
                        this.scheduler.injectSmsOnFly(sms.getSmsSet());
                    } catch (Exception e) {
                        throw new SmscProcessingException("Exception when runnung injectSmsOnFly(): " + e.getMessage(), SmppConstants.STATUS_SYSERR,
                                MAPErrorCode.systemFailure, e);
                    }
                } else {
                    sms.setStored(true);
                    if (smscPropertiesManagement.getDatabaseType() == DatabaseType.Cassandra_1) {
                        persistence.createLiveSms(sms);
                        persistence.setNewMessageScheduled(sms.getSmsSet(),
                                MessageUtil.computeDueDate(MessageUtil.computeFirstDueDelay(smscPropertiesManagement.getFirstDueDelay())));
                    } else {
                        persistence
                                .c2_scheduleMessage_ReschedDueSlot(sms, smscPropertiesManagement.getStoreAndForwordMode() == StoreAndForwordMode.fast, false);
                    }
                }
            }

            smscStatAggregator.updateMsgInReceivedAll();
            switch (sms.getOriginationType()) {
            case SMPP:
                smscStatAggregator.updateMsgInReceivedSmpp();
                break;
            case SS7:
                smscStatAggregator.updateMsgInReceivedSs7();
                break;
            case SIP:
                smscStatAggregator.updateMsgInReceivedSip();
                break;
            }
		} catch (PersistenceException e) {
			throw new SmscProcessingException("PersistenceException when storing LIVE_SMS : " + e.getMessage(),
					SmppConstants.STATUS_SUBMITFAIL, MAPErrorCode.systemFailure, null, e);
		}
	}

	private void rejectSms(ChargingData chargingData, RoCreditControlAnswer evt) throws SmscProcessingException {
		Sms sms = chargingData.getSms();
		if (logger.isInfoEnabled()) {
			logger.info("ChargingSbb: accessRejected for: resultCode ="
					+ (evt != null ? evt.getResultCode() : "timeout") + ", chargingType="
					+ chargingData.getChargingType() + ", message=[" + sms + "]");
		}

		try {
	        // sending of a failure response for transactional mode
            MessageDeliveryResultResponseInterface.DeliveryFailureReason delReason = MessageDeliveryResultResponseInterface.DeliveryFailureReason.invalidDestinationAddress;
            if (sms.getMessageDeliveryResultResponse() != null) {
                sms.getMessageDeliveryResultResponse().responseDeliveryFailure(delReason);
                sms.setMessageDeliveryResultResponse(null);
            }

	        sms.getSmsSet().setStatus(ErrorCode.OCS_ACCESS_NOT_GRANTED);

            boolean storeAndForwMode = MessageUtil.isStoreAndForward(sms);
            if (storeAndForwMode) {
                sms.setStored(true);
            }

            if (smscPropertiesManagement.getDatabaseType() == DatabaseType.Cassandra_1) {
                persistence.archiveFailuredSms(sms);
            } else {
                if (MessageUtil.isNeedWriteArchiveMessage(sms, smscPropertiesManagement.getGenerateArchiveTable())) {
                    persistence.c2_createRecordArchive(sms);
                }
            }

            smscStatAggregator.updateMsgInRejectedAll();

			// TODO: if CCR gives some response verbal reject reason
			// we need replace CdrGenerator.CDR_SUCCESS_NO_REASON with this
			// reason
            CdrGenerator.generateCdr(sms, CdrGenerator.CDR_OCS_REJECTED, CdrGenerator.CDR_SUCCESS_NO_REASON, smscPropertiesManagement.getGenerateReceiptCdr(),
                    MessageUtil.isNeedWriteArchiveMessage(sms, smscPropertiesManagement.getGenerateCdr()));
		} catch (PersistenceException e) {
			throw new SmscProcessingException(
					"PersistenceException when storing into Archive rejected by OCS message : " + e.getMessage(),
					SmppConstants.STATUS_SUBMITFAIL, MAPErrorCode.systemFailure, null, e);
		}
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
