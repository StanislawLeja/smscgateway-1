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

package org.mobicents.smsc.mproc;

import java.util.Date;

/**
*
* @author sergey vetyutnev
*
*/
public interface MProcNewMessage {

    // source address part
    int getSourceAddrTon();

    void setSourceAddrTon(int val);

    int getSourceAddrNpi();

    void setSourceAddrNpi(int val);

    String getSourceAddr();

    void setSourceAddr(String val);

    // dest address part
    int getDestAddrTon();

    void setDestAddrTon(int val);

    int getDestAddrNpi();

    void setDestAddrNpi(int val);

    String getDestAddr();

    void setDestAddr(String val);

    // message content part
    String getShortMessageText();

    void setShortMessageText(String val);

    byte[] getShortMessageBin();

    void setShortMessageBin(byte[] val);

    // other options
    int getNetworkId();

    void setNetworkId(int val);

    Date getScheduleDeliveryTime();

    void setScheduleDeliveryTime(Date val);

    Date getValidityPeriod();

    void setValidityPeriod(Date val);

    int getDataCoding();

    void setDataCoding(int val);

    int getNationalLanguageSingleShift();

    void setNationalLanguageSingleShift(int val);

    int getNationalLanguageLockingShift();

    void setNationalLanguageLockingShift(int val);

    int getEsmClass();

    void setEsmClass(int val);

    int getPriority();

    void setPriority(int val);

    int getRegisteredDelivery();

    void setRegisteredDelivery(int val);

    // private int sourceAddrTon;
    // private int sourceAddrNpi;
    // private String sourceAddr;
    // private int destAddrTon;
    // private int destAddrNpi;
    // private String destAddr;
    //
    // private String shortMessageText;
    // private byte[] shortMessageBin;
    //
    // private Date scheduleDeliveryTime;
    // private Date validityPeriod;

    // private int networkId;
    // private int dataCoding;
    // private int nationalLanguageSingleShift;
    // private int nationalLanguageLockingShift;
    // private int esmClass;
    // private int priority;
    // private int registeredDelivery;

    // **** this is uncovered

    // private OriginationType originationType;
    // private TlvSet tlvSet = new TlvSet();

}
