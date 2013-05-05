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

package org.mobicents.smsc.slee.services.mt;

import java.io.Serializable;

import org.mobicents.protocols.ss7.map.errors.MAPErrorMessageAbsentSubscriberImpl;
import org.mobicents.protocols.ss7.map.errors.MAPErrorMessageAbsentSubscriberSMImpl;
import org.mobicents.protocols.ss7.map.errors.MAPErrorMessageCallBarredImpl;
import org.mobicents.protocols.ss7.map.errors.MAPErrorMessageExtensionContainerImpl;
import org.mobicents.protocols.ss7.map.errors.MAPErrorMessageFacilityNotSupImpl;
import org.mobicents.protocols.ss7.map.errors.MAPErrorMessageSystemFailureImpl;
import org.mobicents.protocols.ss7.map.errors.MAPErrorMessageUnknownSubscriberImpl;

/**
 * 
 * @author sergey vetyutnev
 *
		systemFailure |
		dataMissing |
		unexpectedDataValue |
		facilityNotSupported |
		unknownSubscriber |
		teleserviceNotProvisioned |
		callBarred |
		AbsentSubscriber,
		absentSubscriberSM}

 */
public class ErrorContainer implements Serializable {

	private static final long serialVersionUID = 3306171791735598079L;

	private MAPErrorMessageSystemFailureImpl systemFailure;
	private MAPErrorMessageFacilityNotSupImpl facilityNotSupported;
	private MAPErrorMessageUnknownSubscriberImpl unknownSubscriber;
	private MAPErrorMessageCallBarredImpl callBarred;
	private MAPErrorMessageAbsentSubscriberImpl absentSubscriber;
	private MAPErrorMessageAbsentSubscriberSMImpl absentSubscriberSM;
	private MAPErrorMessageExtensionContainerImpl errorsExtCont;

	public MAPErrorMessageSystemFailureImpl getSystemFailure() {
		return systemFailure;
	}

	public void setSystemFailure(MAPErrorMessageSystemFailureImpl systemFailure) {
		this.systemFailure = systemFailure;
	}

	public MAPErrorMessageFacilityNotSupImpl getFacilityNotSupported() {
		return facilityNotSupported;
	}

	public void setFacilityNotSupported(MAPErrorMessageFacilityNotSupImpl facilityNotSupported) {
		this.facilityNotSupported = facilityNotSupported;
	}

	public MAPErrorMessageUnknownSubscriberImpl getUnknownSubscriber() {
		return unknownSubscriber;
	}

	public void setUnknownSubscriber(MAPErrorMessageUnknownSubscriberImpl unknownSubscriber) {
		this.unknownSubscriber = unknownSubscriber;
	}
	
	public MAPErrorMessageCallBarredImpl getCallBarred() {
		return callBarred;
	}
	
	public void setCallBarred(MAPErrorMessageCallBarredImpl callBarred) {
		this.callBarred = callBarred;
	}

	public MAPErrorMessageAbsentSubscriberImpl getAbsentSubscriber() {
		return absentSubscriber;
	}

	public void setAbsentSubscriber(MAPErrorMessageAbsentSubscriberImpl absentSubscriber) {
		this.absentSubscriber = absentSubscriber;
	}

	public MAPErrorMessageAbsentSubscriberSMImpl getAbsentSubscriberSM() {
		return absentSubscriberSM;
	}

	public void setAbsentSubscriberSM(MAPErrorMessageAbsentSubscriberSMImpl absentSubscriberSM) {
		this.absentSubscriberSM = absentSubscriberSM;
	}

	public MAPErrorMessageExtensionContainerImpl getErrorsExtCont() {
		return errorsExtCont;
	}

	public void setErrorsExtCont(MAPErrorMessageExtensionContainerImpl errorsExtCont) {
		this.errorsExtCont = errorsExtCont;
	}

}
