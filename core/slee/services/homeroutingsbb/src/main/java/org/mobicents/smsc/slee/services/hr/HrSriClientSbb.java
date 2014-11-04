/*
 * TeleStax, Open Source Cloud Communications  
 * Copyright 2012, Telestax Inc and individual contributors
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

package org.mobicents.smsc.slee.services.hr;

import javax.slee.ActivityContextInterface;
import javax.slee.SbbContext;

import org.mobicents.protocols.ss7.map.api.MAPApplicationContext;
import org.mobicents.protocols.ss7.map.api.MAPApplicationContextName;
import org.mobicents.protocols.ss7.map.api.MAPApplicationContextVersion;
import org.mobicents.protocols.ss7.map.api.MAPException;
import org.mobicents.protocols.ss7.map.api.errors.MAPErrorMessage;
import org.mobicents.protocols.ss7.map.api.errors.MAPErrorMessageAbsentSubscriber;
import org.mobicents.protocols.ss7.map.api.primitives.AddressString;
import org.mobicents.protocols.ss7.map.api.primitives.ISDNAddressString;
import org.mobicents.protocols.ss7.map.api.service.sms.InformServiceCentreRequest;
import org.mobicents.protocols.ss7.map.api.service.sms.MAPDialogSms;
import org.mobicents.protocols.ss7.map.api.service.sms.MWStatus;
import org.mobicents.protocols.ss7.map.api.service.sms.SendRoutingInfoForSMRequest;
import org.mobicents.protocols.ss7.map.api.service.sms.SendRoutingInfoForSMResponse;
import org.mobicents.protocols.ss7.sccp.parameter.ParameterFactory;
import org.mobicents.protocols.ss7.sccp.parameter.SccpAddress;
import org.mobicents.slee.resource.map.events.DialogDelimiter;
import org.mobicents.slee.resource.map.events.DialogNotice;
import org.mobicents.slee.resource.map.events.DialogProviderAbort;
import org.mobicents.slee.resource.map.events.DialogReject;
import org.mobicents.slee.resource.map.events.DialogRequest;
import org.mobicents.slee.resource.map.events.DialogTimeout;
import org.mobicents.slee.resource.map.events.DialogUserAbort;
import org.mobicents.slee.resource.map.events.ErrorComponent;
import org.mobicents.slee.resource.map.events.RejectComponent;
import org.mobicents.smsc.library.CorrelationIdValue;
import org.mobicents.smsc.library.ErrorCode;
import org.mobicents.smsc.library.MessageUtil;
import org.mobicents.smsc.library.SmsSet;
import org.mobicents.smsc.slee.services.mt.InformServiceCenterContainer;
import org.mobicents.smsc.slee.services.mt.MtSbbLocalObject;
import org.mobicents.smsc.slee.services.mt.MtCommonSbb.ErrorAction;

/**
 * 
 * @author amit bhayani
 * @author servey vetyutnev
 * 
 */
public abstract class HrSriClientSbb extends HomeRoutingCommonSbb implements HrSriForwardInterface {

    protected MAPApplicationContextVersion maxMAPApplicationContextVersion = null;

    private static final String className = HrSriClientSbb.class
            .getSimpleName();

    public HrSriClientSbb() {
        super(className);
    }

    public void onDialogRequest(DialogRequest evt, ActivityContextInterface aci) {
        super.onDialogRequest(evt, aci);

    }

    public void onDialogDelimiter(DialogDelimiter evt,
            ActivityContextInterface aci) {
        super.onDialogDelimiter(evt, aci);
    }

    public void onRejectComponent(RejectComponent event,
            ActivityContextInterface aci) {
        super.onRejectComponent(event, aci);

    }

    public void onDialogReject(DialogReject evt, ActivityContextInterface aci) {
        super.onDialogReject(evt, aci);

    }

    public void onDialogUserAbort(DialogUserAbort evt,
            ActivityContextInterface aci) {
        super.onDialogUserAbort(evt, aci);

    }

    public void onDialogProviderAbort(DialogProviderAbort evt,
            ActivityContextInterface aci) {
        super.onDialogProviderAbort(evt, aci);

    }

    public void onDialogNotice(DialogNotice evt, ActivityContextInterface aci) {
        super.onDialogNotice(evt, aci);

    }

    public void onDialogTimeout(DialogTimeout evt, ActivityContextInterface aci) {
        super.onDialogTimeout(evt, aci);

    }

    /**
     * MAP SMS Events
     */

    /**
     * Received SRI request. But this is error, we should never receive this
     * request
     * 
     * @param evt
     * @param aci
     */
    public void onSendRoutingInfoForSMRequest(SendRoutingInfoForSMRequest evt, ActivityContextInterface aci) {
        this.logger.severe("Received SEND_ROUTING_INFO_FOR_SM_REQUEST = " + evt);
    }

    /**
     * Received response for SRI sent earlier
     * 
     * @param evt
     * @param aci
     */
    public void onSendRoutingInfoForSMResponse(SendRoutingInfoForSMResponse evt, ActivityContextInterface aci) {
        if (this.logger.isFineEnabled()) {
            this.logger.fine("\nReceived SEND_ROUTING_INFO_FOR_SM_RESPONSE = " + evt + " Dialog=" + evt.getMAPDialog());
        }

//        if (evt.getMAPDialog().getApplicationContext().getApplicationContextVersion() == MAPApplicationContextVersion.version1
//                && evt.getMwdSet() != null && evt.getMwdSet()) {
//            InformServiceCenterContainer informServiceCenterContainer = new InformServiceCenterContainer();
//            MWStatus mwStatus = evt.getMAPDialog().getService().getMAPProvider().getMAPParameterFactory()
//                    .createMWStatus(false, true, false, false);
//            informServiceCenterContainer.setMwStatus(mwStatus);
//            this.doSetInformServiceCenterContainer(informServiceCenterContainer);
//        }
//
//        this.setSendRoutingInfoForSMResponse(evt);
    }

    public void onInformServiceCentreRequest(InformServiceCentreRequest evt, ActivityContextInterface aci) {
        if (this.logger.isInfoEnabled()) {
            this.logger.info("\nReceived INFORM_SERVICE_CENTER_REQUEST = " + evt + " Dialog=" + evt.getMAPDialog());
        }

//        InformServiceCenterContainer informServiceCenterContainer = new InformServiceCenterContainer();
//        informServiceCenterContainer.setMwStatus(evt.getMwStatus());
//        this.doSetInformServiceCenterContainer(informServiceCenterContainer);
    }

    public void onErrorComponent(ErrorComponent event,
            ActivityContextInterface aci) {
        super.onErrorComponent(event, aci);

        try {
            // we store error into CMP
            MAPErrorMessage mapErrorMessage = event.getMAPErrorMessage();
            this.setErrorResponse(mapErrorMessage);

            if (mapErrorMessage.isEmAbsentSubscriber()) {
                MAPErrorMessageAbsentSubscriber errAs = mapErrorMessage.getEmAbsentSubscriber();
                Boolean mwdSet = errAs.getMwdSet();
                if (mwdSet != null && mwdSet) {
                    MWStatus mwStatus = event.getMAPDialog().getService().getMAPProvider().getMAPParameterFactory().createMWStatus(false, true, false, false);
                    CorrelationIdValue civ = this.getCorrelationIdValue();
                    civ.setMwStatus(mwStatus);
                }
            }
        } catch (Throwable e1) {
            logger.severe("Exception in SriSbb.onErrorComponent when fetching records and issuing events: " + e1.getMessage(), e1);
        }
    }

    /**
     * CMD
     */
    public abstract void setCorrelationIdValue(CorrelationIdValue correlationIdValue);

    public abstract CorrelationIdValue getCorrelationIdValue();

    public abstract void setSendRoutingInfoForSMResponse(SendRoutingInfoForSMResponse sendRoutingInfoForSMResponse);

    public abstract SendRoutingInfoForSMResponse getSendRoutingInfoForSMResponse();

    public abstract void setErrorResponse(MAPErrorMessage errorResponse);

    public abstract MAPErrorMessage getErrorResponse();

    public abstract void setSriMapVersion(int sriMapVersion);

    public abstract int getSriMapVersion();

    /**
     * SBB Local Object Methods
     * 
     */
    @Override
    public void setupSriRequest(CorrelationIdValue correlationIdValue) {
        if (this.logger.isFineEnabled()) {
            this.logger.fine("\nReceived SriRequest: event= " + correlationIdValue);
        }
        // TODO: remove it ....................
        this.logger.info("\nReceived SriRequest: event= " + correlationIdValue);
        // TODO: remove it ....................

        this.setCorrelationIdValue(correlationIdValue);

        this.sendSRI(correlationIdValue.getMsisdn().getAddress(), correlationIdValue.getMsisdn().getAddressNature().getIndicator(), correlationIdValue
                .getMsisdn().getNumberingPlan().getIndicator(), this.getSRIMAPApplicationContext(this.maxMAPApplicationContextVersion));
    }

    @Override
    public void setSbbContext(SbbContext sbbContext) {
        super.setSbbContext(sbbContext);

        this.maxMAPApplicationContextVersion = MAPApplicationContextVersion.getInstance(smscPropertiesManagement
                .getMaxMapVersion());
    }

    private void sendSRI(String destinationAddress, int ton, int npi, MAPApplicationContext mapApplicationContext) {
        // Send out SRI
        MAPDialogSms mapDialogSms = null;
        try {
            // 1. Create Dialog first and add the SRI request to it
            mapDialogSms = this.setupRoutingInfoForSMRequestIndication(destinationAddress, ton, npi,
                    mapApplicationContext);

            // 2. Create the ACI and attach this SBB
            ActivityContextInterface sriDialogACI = this.mapAcif.getActivityContextInterface(mapDialogSms);
            sriDialogACI.attach(this.sbbContext.getSbbLocalObject());

            // 3. Finally send the request
            mapDialogSms.send();
        } catch (MAPException e) {
            if (mapDialogSms != null) {
                mapDialogSms.release();
            }

            String reason = "MAPException when sending SRI from sendSRI() (home routing): " + e.toString();
            this.logger.severe(reason, e);
            ErrorCode smStatus = ErrorCode.SC_SYSTEM_ERROR;
            this.returnSriFailure();
//            this.returnSriFailure(smsSet, ErrorAction.permanentFailure, smStatus, reason, true);
        }
    }

    private MAPDialogSms setupRoutingInfoForSMRequestIndication(String destinationAddress, int ton, int npi,
            MAPApplicationContext mapApplicationContext) throws MAPException {
        // this.mapParameterFactory.creat

        SccpAddress destinationAddr = this.convertAddressFieldToSCCPAddress(destinationAddress, ton, npi);

        MAPDialogSms mapDialogSms = this.mapProvider.getMAPServiceSms().createNewDialog(mapApplicationContext,
                this.getServiceCenterSccpAddress(), null, destinationAddr, null);

        ISDNAddressString isdn = this.getCalledPartyISDNAddressString(destinationAddress, ton, npi);
        AddressString serviceCenterAddress = this.getServiceCenterAddressString();
        boolean sm_RP_PRI = true;
        mapDialogSms.addSendRoutingInfoForSMRequest(isdn, sm_RP_PRI, serviceCenterAddress, null, false, null, null,
                null);
        if (this.logger.isInfoEnabled())
            this.logger.info("\nSending: SendRoutingInfoForSMRequest (home routing): isdn=" + isdn + ", serviceCenterAddress="
                    + serviceCenterAddress + ", sm_RP_PRI=" + sm_RP_PRI);

        return mapDialogSms;
    }

    private void returnSriSuccess() {
        // TODO: implement it
    }

    private void returnSriFailure() {
        // TODO: implement it
    }

    private SccpAddress convertAddressFieldToSCCPAddress(String address, int ton, int npi) {
        return MessageUtil.getSccpAddress(sccpParameterFact, address, ton, npi, smscPropertiesManagement.getHlrSsn(),
                smscPropertiesManagement.getGlobalTitleIndicator(), smscPropertiesManagement.getTranslationType());
    }

    private MAPApplicationContext getSRIMAPApplicationContext(MAPApplicationContextVersion applicationContextVersion) {
        MAPApplicationContext mapApplicationContext = MAPApplicationContext.getInstance(
                MAPApplicationContextName.shortMsgGatewayContext, applicationContextVersion);
        this.setSriMapVersion(applicationContextVersion.getVersion());
        return mapApplicationContext;
    }

}
