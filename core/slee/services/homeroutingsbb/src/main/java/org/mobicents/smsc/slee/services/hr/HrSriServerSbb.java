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
import javax.slee.InitialEventSelector;

import org.mobicents.protocols.ss7.map.api.MAPApplicationContextName;
import org.mobicents.protocols.ss7.map.api.primitives.AddressString;
import org.mobicents.protocols.ss7.map.api.primitives.ISDNAddressString;
import org.mobicents.protocols.ss7.map.api.service.sms.SendRoutingInfoForSMRequest;
import org.mobicents.protocols.ss7.map.api.service.sms.SendRoutingInfoForSMResponse;
import org.mobicents.slee.ChildRelationExt;
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

/**
 * 
 * @author amit bhayani
 * @author servey vetyutnev
 * 
 */
public abstract class HrSriServerSbb extends HomeRoutingCommonSbb {

    private static final String className = HrSriServerSbb.class.getSimpleName();

    public HrSriServerSbb() {
        super(className);
    }

	public void onDialogRequest(DialogRequest evt, ActivityContextInterface aci) {
		super.onDialogRequest(evt, aci);

	}

    public void onDialogDelimiter(DialogDelimiter evt, ActivityContextInterface aci) {
        super.onDialogDelimiter(evt, aci);

    }

    public void onErrorComponent(ErrorComponent event, ActivityContextInterface aci) {
        super.onErrorComponent(event, aci);

    }

    public void onRejectComponent(RejectComponent event, ActivityContextInterface aci) {
        super.onRejectComponent(event, aci);

    }

    public void onDialogReject(DialogReject evt, ActivityContextInterface aci) {
        super.onDialogReject(evt, aci);

    }

    public void onDialogUserAbort(DialogUserAbort evt, ActivityContextInterface aci) {
        super.onDialogUserAbort(evt, aci);

    }

    public void onDialogProviderAbort(DialogProviderAbort evt, ActivityContextInterface aci) {
        super.onDialogProviderAbort(evt, aci);

    }

    public void onDialogNotice(DialogNotice evt, ActivityContextInterface aci) {
        super.onDialogNotice(evt, aci);

    }

    public void onDialogTimeout(DialogTimeout evt, ActivityContextInterface aci) {
        super.onDialogTimeout(evt, aci);

    }

	/**
	 * Initial event selector method to check if the Event should initalize the
	 */
	public InitialEventSelector initialEventSelect(InitialEventSelector ies) {
		Object event = ies.getEvent();
		DialogRequest dialogRequest = null;

		if (event instanceof DialogRequest) {
			dialogRequest = (DialogRequest) event;

			if (MAPApplicationContextName.shortMsgGatewayContext == dialogRequest
					.getMAPDialog().getApplicationContext()
					.getApplicationContextName()) {
				ies.setInitialEvent(true);
				ies.setActivityContextSelected(true);
			} else {
				ies.setInitialEvent(false);
			}
		}

		return ies;
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
        if (this.logger.isFineEnabled()) {
            this.logger.fine("\nReceived SEND_ROUTING_INFO_FOR_SM_REQUEST = " + evt + " Dialog=" + evt.getMAPDialog());
        }

        setupSriRequest(evt.getMsisdn(), evt.getServiceCentreAddress());
    }

    /**
     * Received response for SRI sent earlier
     * 
     * @param evt
     * @param aci
     */
    public void onSendRoutingInfoForSMResponse(SendRoutingInfoForSMResponse evt, ActivityContextInterface aci) {
        this.logger.severe("Received SEND_ROUTING_INFO_FOR_SM_RESPONSE = " + evt);
    }

    private void setupSriRequest(ISDNAddressString msisdn, AddressString serviceCentreAddress) {
        HrSriClientSbbLocalObject hrSriClientSbbLocalObject = this.getHrSriClientSbbLocalObject();
        if (hrSriClientSbbLocalObject != null) {
//            // Attach MtSbb to Scheduler ActivityContextInterface
//            ActivityContextInterface aci = this.getMapActivityContextInterface();
//            aci.attach(hrSriClientSbbLocalObject);


            // TODO: implement it
            String correlationID = "000000000011111";
            // TODO: implement it

            CorrelationIdValue correlationIdValue = new CorrelationIdValue(correlationID, msisdn, serviceCentreAddress);

            hrSriClientSbbLocalObject.setupSriRequest(correlationIdValue);
        }
    }

    /**
     * Get HrSriClientSbb child SBB
     * 
     * @return
     */
    public abstract ChildRelationExt getHrSriClientSbb();

    private HrSriClientSbbLocalObject getHrSriClientSbbLocalObject() {
        ChildRelationExt relation = getHrSriClientSbb();

        HrSriClientSbbLocalObject ret = (HrSriClientSbbLocalObject) relation.get(ChildRelationExt.DEFAULT_CHILD_NAME);
        if (ret == null) {
            try {
                ret = (HrSriClientSbbLocalObject) relation.create(ChildRelationExt.DEFAULT_CHILD_NAME);
            } catch (Exception e) {
                if (this.logger.isSevereEnabled()) {
                    this.logger.severe("Exception while trying to creat HrSriClientSbb child", e);
                }
            }
        }
        return ret;
    }

}
