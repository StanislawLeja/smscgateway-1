/*
 * TeleStax, Open Source Cloud Communications  Copyright 2012. 
 * TeleStax and individual contributors
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

package org.mobicents.smsc.slee.services.alert;

import java.util.Date;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.slee.ActivityContextInterface;
import javax.slee.CreateException;
import javax.slee.InitialEventSelector;
import javax.slee.RolledBackContext;
import javax.slee.SLEEException;
import javax.slee.Sbb;
import javax.slee.SbbContext;
import javax.slee.TransactionRequiredLocalException;
import javax.slee.facilities.Tracer;

import org.mobicents.protocols.ss7.map.api.MAPApplicationContext;
import org.mobicents.protocols.ss7.map.api.MAPApplicationContextName;
import org.mobicents.protocols.ss7.map.api.MAPApplicationContextVersion;
import org.mobicents.protocols.ss7.map.api.MAPException;
import org.mobicents.protocols.ss7.map.api.MAPParameterFactory;
import org.mobicents.protocols.ss7.map.api.MAPProvider;
import org.mobicents.protocols.ss7.map.api.primitives.AddressString;
import org.mobicents.protocols.ss7.map.api.primitives.ISDNAddressString;
import org.mobicents.protocols.ss7.map.api.service.sms.AlertServiceCentreRequest;
import org.mobicents.protocols.ss7.map.api.service.sms.MAPDialogSms;
import org.mobicents.slee.ChildRelationExt;
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
import org.mobicents.smsc.slee.services.persistence.Persistence;
import org.mobicents.smsc.slee.services.persistence.PersistenceException;
import org.mobicents.smsc.slee.services.persistence.SmsSet;
import org.mobicents.smsc.slee.services.persistence.TargetAddress;

/**
 * 
 * @author amit bhayani
 * @author sergey vetyutnev
 * 
 */
public abstract class AlertSbb implements Sbb {

	protected Tracer logger;
	protected SbbContextExt sbbContext;

	protected MAPContextInterfaceFactory mapAcif;
	protected MAPProvider mapProvider;
	protected MAPParameterFactory mapParameterFactory;

	protected Persistence persistence;


	public AlertSbb() {
	}

	// -------------------------------------------------------------
    // Child relations
    // -------------------------------------------------------------
	public abstract ChildRelationExt getStoreSbb();
	
	public Persistence getStore() throws TransactionRequiredLocalException, SLEEException, CreateException {
		if (persistence == null) {
			ChildRelationExt childRelation = getStoreSbb();
			persistence = (Persistence) childRelation.get(ChildRelationExt.DEFAULT_CHILD_NAME);
			if (persistence == null) {
				persistence = (Persistence) childRelation.create(ChildRelationExt.DEFAULT_CHILD_NAME);
			}
		}
		return persistence;
	}

	/**
	 * MAP Components Events
	 */

	public void onInvokeTimeout(InvokeTimeout evt, ActivityContextInterface aci) {
		if (logger.isInfoEnabled()) {
			this.logger.info("Rx :  onInvokeTimeout" + evt);
		}
	}

	public void onErrorComponent(ErrorComponent event, ActivityContextInterface aci) {
		this.logger.severe("Rx :  onErrorComponent" + event);
	}

	public void onRejectComponent(RejectComponent event, ActivityContextInterface aci) {
		this.logger.severe("Rx :  onRejectComponent" + event);
	}

	/**
	 * Dialog Events
	 */

	public void onDialogDelimiter(DialogDelimiter evt, ActivityContextInterface aci) {
		if (logger.isFineEnabled()) {
			this.logger.fine("Rx :  onDialogDelimiter=" + evt);
		}
	}

	public void onDialogAccept(DialogAccept evt, ActivityContextInterface aci) {
		if (logger.isFineEnabled()) {
			this.logger.fine("Rx :  onDialogAccept=" + evt);
		}
	}

	public void onDialogReject(DialogReject evt, ActivityContextInterface aci) {
		this.logger.severe("Rx :  onDialogReject=" + evt);
	}

	public void onDialogUserAbort(DialogUserAbort evt, ActivityContextInterface aci) {
		this.logger.severe("Rx :  onDialogUserAbort=" + evt);
	}

	public void onDialogProviderAbort(DialogProviderAbort evt, ActivityContextInterface aci) {
		this.logger.severe("Rx :  onDialogProviderAbort=" + evt);
	}

	public void onDialogClose(DialogClose evt, ActivityContextInterface aci) {
		if (logger.isFineEnabled()) {
			this.logger.fine("Rx :  onDialogClose" + evt);
		}
	}

	public void onDialogNotice(DialogNotice evt, ActivityContextInterface aci) {
		this.logger.severe("Rx :  onDialogNotice=" + evt);
	}

	public void onDialogTimeout(DialogTimeout evt, ActivityContextInterface aci) {
		this.logger.severe("Rx :  onDialogTimeout" + evt);
	}

	public void onDialogRequest(DialogRequest evt, ActivityContextInterface aci) {
		if (logger.isFineEnabled()) {
			this.logger.fine("Rx :  onDialogRequest" + evt);
		}
	}

	public void onDialogRelease(DialogRelease evt, ActivityContextInterface aci) {
		if (logger.isFineEnabled()) {
			this.logger.fine("Rx :  onDialogRelease" + evt);
		}
	}

	public void onAlertServiceCentreRequest(AlertServiceCentreRequest evt, ActivityContextInterface aci) {
		if (this.logger.isInfoEnabled()) {
			this.logger.info("Received onAlertServiceCentreRequest= " + evt);
		}

		try {
			MAPDialogSms mapDialogSms = evt.getMAPDialog();
			MAPApplicationContext mapApplicationContext = mapDialogSms.getApplicationContext();
			if (mapApplicationContext.getApplicationContextVersion() == MAPApplicationContextVersion.version2) {
				// Send back response only for V2
				mapDialogSms.addAlertServiceCentreResponse(evt.getInvokeId());
				mapDialogSms.close(false);
			} else {
				mapDialogSms.release();
			}

			this.setupAlert(evt.getMsisdn(), evt.getServiceCentreAddress());
		} catch (MAPException e) {
			logger.severe("Exception while trying to send back AlertServiceCentreResponse", e);
		}
	}

	private void setupAlert(ISDNAddressString msisdn, AddressString serviceCentreAddress) {
		Persistence pers;
		try {
			pers = this.getStore();
		} catch (TransactionRequiredLocalException e1) {
			this.logger.severe("TransactionRequiredLocalException when getting Persistence object in setupAlert(): " + e1.getMessage(), e1);
			return;
		} catch (SLEEException e1) {
			this.logger.severe("SLEEException when getting Persistence object in setupAlert(): " + e1.getMessage(), e1);
			return;
		} catch (CreateException e1) {
			this.logger.severe("CreateException when getting Persistence object in setupAlert(): " + e1.getMessage(), e1);
			return;
		}

		int addrTon = msisdn.getAddressNature().getIndicator();
		int addrNpi = msisdn.getNumberingPlan().getIndicator();
		String addr = msisdn.getAddress();
		TargetAddress lock = pers.obtainSynchroObject(new TargetAddress(addrTon, addrNpi, addr));
		try {
			synchronized (lock) {

				try {
					boolean b1 = pers.checkSmsSetExists(lock);
					if (!b1) {
						if (this.logger.isInfoEnabled()) {
							this.logger.info("AlertServiceCentre received but no SmsSet is present: addr=" + addr + ", ton=" + addrTon + ", npi=" + addrNpi);
						}
						return;
					}

					SmsSet smsSet = pers.obtainSmsSet(lock);
					if (smsSet.getInSystem() == 2) {
						if (this.logger.isInfoEnabled()) {
							this.logger.info("AlertServiceCentre received but no SmsSet is already in active state (InSystem==2): addr=" + addr + ", ton="
									+ addrTon + ", npi=" + addrNpi);
						}
						return;
					}
					if (smsSet.getInSystem() == 0) {
						if (this.logger.isInfoEnabled()) {
							this.logger.info("AlertServiceCentre received but no SmsSet is already in passive state (InSystem==0): addr=" + addr + ", ton="
									+ addrTon + ", npi=" + addrNpi);
						}
						return;
					}

					pers.setDeliveringProcessScheduled(smsSet, new Date(), 0);
				} catch (PersistenceException e) {
					this.logger.severe("PersistenceException when setupAlert()" + e.getMessage(), e);
				}
			}
		} finally {
			pers.releaseSynchroObject(lock);
		}
	}

	/**
	 * Life cycle
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
	public void setSbbContext(SbbContext sbbContext) {
		this.sbbContext = (SbbContextExt) sbbContext;
		try {
			Context ctx = (Context) new InitialContext().lookup("java:comp/env");
			this.mapAcif = (MAPContextInterfaceFactory) ctx.lookup("slee/resources/map/2.0/acifactory");
			this.mapProvider = (MAPProvider) ctx.lookup("slee/resources/map/2.0/provider");
			this.mapParameterFactory = this.mapProvider.getMAPParameterFactory();

			this.logger = this.sbbContext.getTracer(AlertSbb.class.getSimpleName());

		} catch (Exception ne) {
			logger.severe("Could not set SBB context:", ne);
		}
	}

	@Override
	public void unsetSbbContext() {
		// TODO Auto-generated method stub

	}

	/**
	 * Initial event selector method to check if the Event should initalize the
	 */
	public InitialEventSelector initialEventSelect(InitialEventSelector ies) {
		Object event = ies.getEvent();
		DialogRequest dialogRequest = null;

		if (event instanceof DialogRequest) {
			dialogRequest = (DialogRequest) event;

			if (MAPApplicationContextName.shortMsgAlertContext == dialogRequest.getMAPDialog().getApplicationContext()
					.getApplicationContextName()) {
				ies.setInitialEvent(true);
				ies.setActivityContextSelected(true);
			} else {
				ies.setInitialEvent(false);
			}
		}

		return ies;
	}

}
