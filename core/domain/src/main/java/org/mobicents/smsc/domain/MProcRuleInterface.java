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
package org.mobicents.smsc.domain;

import org.mobicents.smsc.library.Sms;

/**
 * <p>
 * Message processing rules (mproc rules) can be useful for changing parameters
 * of incoming messages that fit to some configurable conditions, for routing
 * such messages to another subnetwork area (networkId)
 * </p>
 * <p>
 * You can create one or more mproc rules, modify them, show them and remove
 * some of them. Each mproc rule has it's unique digital Id. Mproc rule are
 * sorted by this Id value. This means that for applying rules for a message
 * SMSC GW starts with looking at a mproc rule with the least Id and then at a
 * next mproc rule with next Id etc. When checking if mproc rule condition
 * matches to a rule, updated at the previous applied rules are taken into
 * account.
 * </p>
 * <p>
 * For any incoming message (from SMPP, SIP or SS7 connectors) SMSC GW firstly
 * invokes a Diameter server (if Diameter server is configured). Once Diameter
 * server has responded positively, SMSC GW will accept the message and mproc
 * rules are applied to the message. After this the message will be stored to
 * the database or delivered to a destination.
 * </p>
 * 
 * @author amit bhayani
 *
 */
public interface MProcRuleInterface {

	/**
	 * @return the id of the mproc
	 */
	int getId();

	int getDestTonMask();

	/**
	 * The incoming SMS destination TON should match for SMS to be processed.
	 * Set destTonMask to -1 as wild card to match any TON of incoming SMS. If
	 * TON doesn't match, MProc rule will not be applied
	 * 
	 * @param destTonMask
	 */
	void setDestTonMask(int destTonMask);

	/**
	 * @return mask for destination address numerical type indicator. -1 means
	 *         any value
	 */
	int getDestNpiMask();

	/**
	 * The incoming SMS destination NPI should match for it to be processed. Set
	 * destNpiMask to -1 as wild card to match any TON of incoming SMS. If TON
	 * doesn't match, MProc rule will not be applied
	 * 
	 * @param destNpiMask
	 */
	void setDestNpiMask(int destNpiMask);

	/**
	 * @return mask (a regular expression) for destination address digits. "-1"
	 *         means any value (same as "......")
	 */
	String getDestDigMask();

	/**
	 * The incoming SMS destination should match for it to be processed.
	 * destDigMask is Java regular expression. If destDigMask doesn't match,
	 * MProc rule will not be applied
	 * 
	 * @param destDigMask
	 */
	void setDestDigMask(String destDigMask);

	/**
	 * @return mask for message originatingMask (SMPP, SIP, MO, HR SS7). null
	 *         (CLI "-1") means any value
	 */
	Sms.OriginationType getOriginatingMask();

	/**
	 * SMS can be originated from SMPP, SS7 Mobile Originated, SS7 Home Routing
	 * or SIP. Set originatingMask to appropriate value to only apply this MProc
	 * rule if SMS is originated from that medium. Set to null as wild card to
	 * match all the medium
	 * 
	 * @param originatingMask
	 */
	void setOriginatingMask(Sms.OriginationType originatingMask);

	/**
	 * @return mask for message original NetworkId. "-1" means any value.
	 */
	int getNetworkIdMask();

	void setNetworkIdMask(int networkIdMask);

	/**
	 * @return if !=-1: the new networkId will be assigned to a message
	 */
	int getNewNetworkId();

	/**
	 * The incoming SMS networkId should match for it to be processed. Set
	 * newNetworkId to -1 as wild card to match any networkId of incoming SMS.
	 * If newNetworkId doesn't match, MProc rule will not be applied
	 * 
	 * @param newNetworkId
	 */
	void setNewNetworkId(int newNetworkId);

	/**
	 * @return if !=-1: the new destination address type of number will be
	 *         assigned to a message
	 */
	int getNewDestTon();

	/**
	 * If MProc rule should be applied, newDestTon will be set for this SMS. If
	 * the value is -1, means original newDestTon will be kept
	 * 
	 * @param newDestTon
	 */
	void setNewDestTon(int newDestTon);

	/**
	 * @return if !=-1: the new destination address numbering plan indicator
	 *         will be assigned to a message
	 */
	int getNewDestNpi();

	/**
	 * If MProc rule should be applied, newDestNpi will be set for this SMS. If
	 * the value is -1, means original newDestNpi will be kept
	 * 
	 * @param newDestNpi
	 */
	void setNewDestNpi(int newDestNpi);

	/**
	 * @return if !="-1" / != null: the specified prefix will be added into a
	 *         destination address digits of a message
	 */
	String getAddDestDigPrefix();

	/**
	 * If MProc rule should be applied, adddestdigprefix will be added at
	 * begining of a message destination address digits, For example if
	 * adddestdigprefix is "22" and destination address digits are "3333333",
	 * then the new value of destination address digits will be "223333333".
	 * "-1" means that destination address digits of the message will not be
	 * changed.
	 * 
	 * @param addDestDigPrefix
	 */
	void setAddDestDigPrefix(String addDestDigPrefix);

	/**
	 * @return if true - a copy of a message will be created. All other next
	 *         rules will be applied only for a copy of a message
	 */
	boolean isMakeCopy();

	void setMakeCopy(boolean makeCopy);

	/**
	 * Returns true if the received destTon, destNpi, destDig, originatingType
	 * and networkId match's with this MProc rules values as explained for each
	 * of the parameters above
	 * 
	 * @param destTon
	 * @param destNpi
	 * @param destDig
	 * @param originatingType
	 * @param networkId
	 * @return
	 */
	boolean isMessageMatchToRule(int destTon, int destNpi, String destDig,
			Sms.OriginationType originatingType, int networkId);

}
