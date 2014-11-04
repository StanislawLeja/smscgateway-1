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

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.slee.ActivityContextInterface;
import javax.slee.CreateException;
import javax.slee.RolledBackContext;
import javax.slee.Sbb;
import javax.slee.SbbContext;
import javax.slee.facilities.Tracer;

import org.mobicents.protocols.ss7.indicator.NatureOfAddress;
import org.mobicents.protocols.ss7.indicator.NumberingPlan;
import org.mobicents.protocols.ss7.map.api.MAPParameterFactory;
import org.mobicents.protocols.ss7.map.api.MAPProvider;
import org.mobicents.protocols.ss7.map.api.primitives.AddressNature;
import org.mobicents.protocols.ss7.map.api.primitives.AddressString;
import org.mobicents.protocols.ss7.map.api.primitives.ISDNAddressString;
import org.mobicents.protocols.ss7.sccp.impl.parameter.ParameterFactoryImpl;
import org.mobicents.protocols.ss7.sccp.parameter.ParameterFactory;
import org.mobicents.protocols.ss7.sccp.parameter.SccpAddress;
import org.mobicents.slee.SbbContextExt;
import org.mobicents.slee.resource.map.MAPContextInterfaceFactory;
import org.mobicents.slee.resource.map.events.DialogAccept;
import org.mobicents.slee.resource.map.events.DialogClose;
import org.mobicents.slee.resource.map.events.DialogDelimiter;
import org.mobicents.slee.resource.map.events.DialogNotice;
import org.mobicents.slee.resource.map.events.DialogProviderAbort;
import org.mobicents.slee.resource.map.events.DialogReject;
import org.mobicents.slee.resource.map.events.DialogRelease;
import org.mobicents.slee.resource.map.events.DialogRequest;
import org.mobicents.slee.resource.map.events.DialogTimeout;
import org.mobicents.slee.resource.map.events.DialogUserAbort;
import org.mobicents.slee.resource.map.events.ErrorComponent;
import org.mobicents.slee.resource.map.events.InvokeTimeout;
import org.mobicents.slee.resource.map.events.RejectComponent;
import org.mobicents.smsc.domain.SmscPropertiesManagement;
import org.mobicents.smsc.domain.SmscStatAggregator;
import org.mobicents.smsc.library.MessageUtil;
import org.mobicents.smsc.slee.resources.scheduler.SchedulerRaSbbInterface;
import org.mobicents.smsc.slee.resources.smpp.server.SmppSessions;

/**
 * 
 * @author amit bhayani
 * @author servey vetyutnev
 * 
 */
public abstract class HomeRoutingCommonSbb implements Sbb {

    protected static final SmscPropertiesManagement smscPropertiesManagement = SmscPropertiesManagement.getInstance();

	private final String className;

	protected Tracer logger;
	protected SbbContextExt sbbContext;

	protected MAPContextInterfaceFactory mapAcif;
	protected MAPProvider mapProvider;
	protected MAPParameterFactory mapParameterFactory;
	protected SmscStatAggregator smscStatAggregator = SmscStatAggregator
			.getInstance();
	protected SchedulerRaSbbInterface scheduler = null;

	protected SmppSessions smppServerSessions = null;
    protected ParameterFactory sccpParameterFact;
    private SccpAddress serviceCenterSCCPAddress = null;
    private AddressString serviceCenterAddress;

	public HomeRoutingCommonSbb(String className) {
		this.className = className;
	}

	/**
	 * MAP Components Events
	 */

	public void onInvokeTimeout(InvokeTimeout evt, ActivityContextInterface aci) {
		this.logger.severe("\nRx :  onInvokeTimeout" + evt);
	}

	public void onErrorComponent(ErrorComponent event,
			ActivityContextInterface aci) {
		this.logger.severe("\nRx :  onErrorComponent " + event + " Dialog="
				+ event.getMAPDialog());
	}

	public void onRejectComponent(RejectComponent event,
			ActivityContextInterface aci) {
		this.logger.severe("\nRx :  onRejectComponent" + event);
	}

	/**
	 * Dialog Events
	 */
	public void onDialogDelimiter(DialogDelimiter evt,
			ActivityContextInterface aci) {
		if (logger.isFineEnabled()) {
			this.logger.fine("\nRx :  onDialogDelimiter=" + evt);
		}
	}

	public void onDialogAccept(DialogAccept evt, ActivityContextInterface aci) {
		if (logger.isFineEnabled()) {
			this.logger.fine("\nRx :  onDialogAccept=" + evt);
		}
	}

	public void onDialogReject(DialogReject evt, ActivityContextInterface aci) {
		this.logger.severe("\nRx :  onDialogReject=" + evt);
	}

	public void onDialogUserAbort(DialogUserAbort evt,
			ActivityContextInterface aci) {
		this.logger.severe("\nRx :  onDialogUserAbort=" + evt);
	}

	public void onDialogProviderAbort(DialogProviderAbort evt,
			ActivityContextInterface aci) {
		this.logger.severe("\nRx :  onDialogProviderAbort=" + evt);
	}

	public void onDialogClose(DialogClose evt, ActivityContextInterface aci) {
		if (logger.isFineEnabled()) {
			this.logger.fine("\nRx :  onDialogClose" + evt);
		}
	}

	public void onDialogNotice(DialogNotice evt, ActivityContextInterface aci) {
		if (logger.isWarningEnabled()) {
			this.logger.warning("\nRx :  onDialogNotice" + evt);
		}
	}

	public void onDialogTimeout(DialogTimeout evt, ActivityContextInterface aci) {
		if (this.logger.isWarningEnabled()) {
			this.logger.warning("\nRx :  onDialogTimeout" + evt);
		}
	}

	public void onDialogRequest(DialogRequest evt, ActivityContextInterface aci) {
		if (logger.isFineEnabled()) {
			this.logger.fine("\nRx :  onDialogRequest" + evt);
		}
	}

	public void onDialogRelease(DialogRelease evt, ActivityContextInterface aci) {
		if (logger.isFineEnabled()) {
			this.logger.fine("\nRx :  DialogRelease" + evt);
		}
	}

	/**
	 * Life cycle methods
	 */

	@Override
	public void sbbActivate() {
		// TODO Auto-generated method stub

	}

	@Override
	public void sbbCreate() throws CreateException {
		// TODO Auto-generated method stub

	}

	@Override
	public void sbbExceptionThrown(Exception arg0, Object arg1,
			ActivityContextInterface arg2) {
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
	public void setSbbContext(SbbContext sbbContext) {
		this.sbbContext = (SbbContextExt) sbbContext;

		try {
			Context ctx = (Context) new InitialContext()
					.lookup("java:comp/env");
			this.mapAcif = (MAPContextInterfaceFactory) ctx
					.lookup("slee/resources/map/2.0/acifactory");
			this.mapProvider = (MAPProvider) ctx
					.lookup("slee/resources/map/2.0/provider");
			this.mapParameterFactory = this.mapProvider
					.getMAPParameterFactory();
            this.sccpParameterFact = new ParameterFactoryImpl();

			this.smppServerSessions = (SmppSessions) ctx
					.lookup("slee/resources/smpp/server/1.0/provider");

			this.logger = this.sbbContext.getTracer(this.className);
		} catch (Exception ne) {
			logger.severe("Could not set SBB context:", ne);
		}
		// TODO : Handle proper error

	}

	@Override
	public void unsetSbbContext() {

	}

//    /**
//     * Get the MAP Dialog Activity
//     * 
//     * @return
//     */
//    protected ActivityContextInterface getMapActivityContextInterface() {
//        ActivityContextInterface[] acis = this.sbbContext.getActivities();
//        for (int count = 0; count < acis.length; count++) {
//            ActivityContextInterface aci = acis[count];
//            Object activity = aci.getActivity();
//            if (activity.getClass().getCanonicalName().equals("org.mobicents.slee.resource.map.service.sms.wrappers.MAPDialogSmsWrapper")) {
//                return aci;
//            }
//        }
//
//        return null;
//    }

    protected SccpAddress getServiceCenterSccpAddress() {
        if (this.serviceCenterSCCPAddress == null) {
            this.serviceCenterSCCPAddress = MessageUtil.getSccpAddress(sccpParameterFact, smscPropertiesManagement.getServiceCenterGt(),
                    NatureOfAddress.INTERNATIONAL.getValue(), NumberingPlan.ISDN_TELEPHONY.getValue(), smscPropertiesManagement.getHlrSsn(),
                    smscPropertiesManagement.getGlobalTitleIndicator(), smscPropertiesManagement.getTranslationType());
        }
        return this.serviceCenterSCCPAddress;
    }

    protected ISDNAddressString getCalledPartyISDNAddressString(String destinationAddress, int ton, int npi) {
        return this.mapParameterFactory.createISDNAddressString(AddressNature.getInstance(ton),
                org.mobicents.protocols.ss7.map.api.primitives.NumberingPlan.getInstance(npi), destinationAddress);
    }

    protected AddressString getServiceCenterAddressString() {

        if (this.serviceCenterAddress == null) {
            this.serviceCenterAddress = this.mapParameterFactory.createAddressString(
                    AddressNature.international_number,
                    org.mobicents.protocols.ss7.map.api.primitives.NumberingPlan.ISDN,
                    smscPropertiesManagement.getServiceCenterGt());
        }
        return this.serviceCenterAddress;
    }

}
