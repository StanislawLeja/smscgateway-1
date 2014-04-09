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

/**
 * 
 * @author Amit Bhayani
 * 
 */
public interface EsmeMBean extends DefaultSmppSessionMXBean {
	boolean isStarted();

	String getClusterName();

	/**
	 * Defines ESME TON. if SMPP Session Type is CLIENT this TON will be used in
	 * BIND request, if SMPP Session Type is SERVER, incoming BIND request
	 * should have same TON as configured here. If configured is null(-1), SMSC
	 * will ignore in both the cases
	 * 
	 * @return
	 */
	int getEsmeTon();

	void setEsmeTon(int esmeTon);

	/**
	 * Defines ESME NPI. if SMPP Session Type is CLIENT this NPI will be used in
	 * BIND request, if SMPP Session Type is SERVER, incoming BIND request
	 * should have same NPI as configured here. If configured is null(-1), SMSC
	 * will ignore in both the cases
	 * 
	 * @return
	 */
	int getEsmeNpi();

	void setEsmeNpi(int esmeNpi);

	/**
	 * Defines ESME Address Range. if SMPP Session Type is CLIENT this address
	 * range will be used in BIND request, if SMPP Session Type is SERVER,
	 * incoming BIND request should have same address range as configured here.
	 * If configured is null, SMSC will ignore in both the cases
	 * 
	 * @return
	 */
	String getEsmeAddressRange();

	void setEsmeAddressRange(String sourceAddressRange);

	String getHost();

	int getPort();

	boolean isChargingEnabled();

	void setChargingEnabled(boolean chargingEnabled);

	/**
	 * every SMS coming into SMSC via this ESME should have same source_addr_ton
	 * as configured here. If the value here is null(-1) or it's not null and
	 * match's, SMSC will compare source_addr_npi and source_addr as mentioned
	 * below. If it doesn't match SMSC will reject this SMS with error code
	 * "0x0000000A" - Invalid Source Address.
	 * 
	 * @return
	 */
	int getSourceTon();

	void setSourceTon(int sourceTon);

	/**
	 * every SMS coming into SMSC via this ESME should have same source_addr_npi
	 * as configured here. If the value here is null(-1)or it's not null and
	 * match's, SMSC will compare source_addr as mentioned below. If it doesn't
	 * match SMSC will reject this SMS with error code "0x0000000A" - Invalid
	 * Source Address.
	 * 
	 * @return
	 */
	int getSourceNpi();

	void setSourceNpi(int sourceNpi);

	/**
	 * every SMS coming into SMSC via this ESME should have same source_addr as
	 * mentioned here. This is regular java expression. Default value is
	 * ^[0-9a-zA-Z]* If it match's, SMSC will accept incoming SMS and process
	 * further. If it doesn't match SMSC will reject this SMS with error code
	 * "0x0000000A" - Invalid Source Address.
	 * 
	 * @return
	 */
	String getSourceAddressRange();

	void setSourceAddressRange(String sourceAddressRange);

	/**
	 * The {@link DefaultSmsRoutingRule} will try to match the dest_addr_ton of
	 * outgoing SMS with one configured here. If configured value is null(-1) or
	 * it's not null and match's, SMSC will compare dest_addr_npi and
	 * destination_addr as below. It it doesn't match, SMSC will select next
	 * ESME in list for matching routing rule
	 * 
	 * @return
	 */
	int getRoutingTon();

	void setRoutingTon(int routingTon);

	/**
	 * The {@link DefaultSmsRoutingRule} will try to match the dest_addr_npi
	 * with one configured here. If configured value is null(-1)or it's not null
	 * and match's, SMSC will compare destination_addr as below. It it doesn't
	 * match, SMSC will select next ESME in list for matching routing rule
	 * 
	 * @return
	 */
	int getRoutingNpi();

	void setRoutingNpi(int sourceNpi);

	/**
	 * The {@link DefaultSmsRoutingRule} will try to match destination_addr
	 * here. This is regular java expression. Default value is ^[0-9a-zA-Z]*. If
	 * it match's, SMSC will send the SMS out over this SMPP connection. If it
	 * doesn't match, SMSC will select next ESME in list for matching routing
	 * rule
	 * 
	 * @return
	 */
	String getRoutingAddressRange();

	void setRoutingAddressRange(String sourceAddressRange);
}
