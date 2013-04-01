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
package org.mobicents.smsc.smpp;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javolution.util.FastList;

import com.cloudhopper.smpp.SmppBindType;

/**
 * @author Amit Bhayani
 * 
 */
public class DefaultSmsRoutingRule implements SmsRoutingRule {

	private final EsmeManagement esmeManagement;

	/**
	 * 
	 */
	public DefaultSmsRoutingRule(EsmeManagement esmeManagement) {
		this.esmeManagement = esmeManagement;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mobicents.smsc.smpp.SmsRoutingRule#getSystemId(byte, byte,
	 * java.lang.String)
	 */
	@Override
	public String getEsmeClusterName(byte ton, byte npi, String address) {

		for (FastList.Node<Esme> n = this.esmeManagement.esmes.head(), end = this.esmeManagement.esmes.tail(); (n = n
				.getNext()) != end;) {
			Esme esme = n.getValue();
			SmppBindType sessionBindType = esme.getSmppBindType();

			if (sessionBindType == SmppBindType.TRANSCEIVER || sessionBindType == SmppBindType.RECEIVER) {
				Pattern p = esme.getAddressRangePattern();
				if (p == null) {
					continue;
				}
				Matcher m = p.matcher(address);
				if (m.matches()) {
					return esme.getClusterName();
				}
			}
		}

		return null;
	}

}
