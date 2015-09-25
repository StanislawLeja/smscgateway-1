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

package org.mobicents.smsc.mproc;

import org.mobicents.smsc.library.OriginationType;

public interface MProcRuleDefault extends MProcRule {

    int getDestTonMask();

    void setDestTonMask(int destTonMask);

    /**
     * @return mask for destination address numerical type indicator. -1 means any value
     */
    int getDestNpiMask();

    void setDestNpiMask(int destNpiMask);

    /**
     * @return mask (a regular expression) for destination address digits. "-1" means any value (same as "......")
     */
    String getDestDigMask();

    void setDestDigMask(String destDigMask);

    /**
     * @return mask for message originatingMask (SMPP, SIP, MO, HR SS7). null (CLI "-1") means any value
     */
    OriginationType getOriginatingMask();

    void setOriginatingMask(OriginationType originatingMask);

    /**
     * @return mask for message original NetworkId. "-1" means any value.
     */
    int getNetworkIdMask();

    void setNetworkIdMask(int networkIdMask);

    /**
     * @return if !=-1: the new networkId will be assigned to a message
     */
    int getNewNetworkId();

    void setNewNetworkId(int newNetworkId);

    /**
     * @return if !=-1: the new destination address type of number will be assigned to a message
     */
    int getNewDestTon();

    void setNewDestTon(int newDestTon);

    /**
     * @return if !=-1: the new destination address numbering plan indicator will be assigned to a message
     */
    int getNewDestNpi();

    void setNewDestNpi(int newDestNpi);

    /**
     * @return if !="-1" / != null: the specified prefix will be added into a destination address digits of a message
     */
    String getAddDestDigPrefix();

    void setAddDestDigPrefix(String addDestDigPrefix);

    /**
     * @return if true - a copy of a message will be created. All other next rules will be applied only for a copy of a message
     */
    boolean isMakeCopy();

    void setMakeCopy(boolean makeCopy);

}
