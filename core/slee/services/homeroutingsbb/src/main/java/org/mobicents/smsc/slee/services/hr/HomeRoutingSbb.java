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

import java.nio.charset.Charset;

import javax.slee.ActivityContextInterface;
import javax.slee.InitialEventSelector;

import org.mobicents.protocols.ss7.map.api.MAPApplicationContextName;
import org.mobicents.slee.resource.map.events.DialogDelimiter;
import org.mobicents.slee.resource.map.events.DialogNotice;
import org.mobicents.slee.resource.map.events.DialogProviderAbort;
import org.mobicents.slee.resource.map.events.DialogReject;
import org.mobicents.slee.resource.map.events.DialogRequest;
import org.mobicents.slee.resource.map.events.DialogTimeout;
import org.mobicents.slee.resource.map.events.DialogUserAbort;
import org.mobicents.slee.resource.map.events.ErrorComponent;
import org.mobicents.slee.resource.map.events.RejectComponent;

/**
 * 
 * @author amit bhayani
 * @author servey vetyutnev
 * 
 */
public abstract class HomeRoutingSbb extends HomeRoutingCommonSbb {

	private static final String className = HomeRoutingSbb.class
			.getSimpleName();

	private static Charset isoCharset = Charset.forName("ISO-8859-1");

	public HomeRoutingSbb() {
		super(className);
	}

	public void onDialogRequest(DialogRequest evt, ActivityContextInterface aci) {
		super.onDialogRequest(evt, aci);

	}

	public void onDialogDelimiter(DialogDelimiter evt,
			ActivityContextInterface aci) {
		super.onDialogDelimiter(evt, aci);
	}

	public void onErrorComponent(ErrorComponent event,
			ActivityContextInterface aci) {
		super.onErrorComponent(event, aci);

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

}
