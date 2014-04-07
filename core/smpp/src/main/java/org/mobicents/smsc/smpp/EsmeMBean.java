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

import com.cloudhopper.smpp.jmx.DefaultSmppSessionMXBean;
import com.cloudhopper.smpp.type.Address;

/**
 * 
 * @author Amit Bhayani
 * 
 */
public interface EsmeMBean extends DefaultSmppSessionMXBean {
	public boolean isStarted();

	public String getClusterName();

	public Address getAddress();

	public String getHost();

	public int getPort();

	public boolean isChargingEnabled();

	public void setChargingEnabled(boolean chargingEnabled);

	/**
	 * every SMS coming into SMSC should have same source_addr_ton as mentioned
	 * here. If the value here is -1, means SMSC doesn't care
	 * 
	 * @return
	 */
	public int getSourceTon();

	public void setSourceTon(int sourceTon);

	/**
	 * every SMS coming into SMSC should have same source_addr_npi as mentioned
	 * here. If the value here is -1, means SMSC doesn't care
	 * 
	 * @return
	 */
	public int getSourceNpi();

	public void setSourceNpi(int sourceNpi);

	/**
	 * every SMS coming into SMSC should have same source_addr as mentioned
	 * here. This is regular java expression. Default value is ^[0-9a-zA-Z]*
	 * 
	 * @return
	 */
	public String getSourceAddressRange();

	public void setSourceAddressRange(String sourceAddressRange);
}
