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

package org.mobicents.smsc.mproc.impl;

import java.util.ArrayList;
import java.util.Date;

import org.mobicents.smsc.library.OriginationType;
import org.mobicents.smsc.library.Sms;
import org.mobicents.smsc.mproc.MProcMessage;
import org.mobicents.smsc.mproc.MProcNewMessage;
import org.mobicents.smsc.mproc.MProcRuleException;
import org.mobicents.smsc.mproc.PostArrivalProcessor;

/**
*
* @author sergey vetyutnev
*
*/
public class PostArrivalProcessorImpl implements PostArrivalProcessor {

    private int defaultValidityPeriodHours;
    private int maxValidityPeriodHours;

    private boolean needDropMessage = false;
    private boolean needRejectMessage = false;

    private ArrayList<MProcNewMessage> postedMessages = new ArrayList<MProcNewMessage>();

    public PostArrivalProcessorImpl(int defaultValidityPeriodHours, int maxValidityPeriodHours) {
        this.defaultValidityPeriodHours = defaultValidityPeriodHours;
        this.maxValidityPeriodHours = maxValidityPeriodHours;
    }

    // results of message processing
    public boolean isNeedDropMessage() {
        return needDropMessage;
    }

    public boolean isNeedRejectMessage() {
        return needRejectMessage;
    }

    public ArrayList<MProcNewMessage> getPostedMessages() {
        return postedMessages;
    }

    // message processing methods
    @Override
    public void dropMessage() {
        needDropMessage = true;
    }

    @Override
    public void rejectMessage() {
        needRejectMessage = true;
    }

    @Override
    public void updateMessageNetworkId(MProcMessage message, int newNetworkId) {
        MProcMessageImpl msg = (MProcMessageImpl) message;
        Sms sms = msg.getSmsContent();
        sms.getSmsSet().setNetworkId(newNetworkId);
        sms.getSmsSet().setCorrelationId(null);
    }

    @Override
    public void updateMessageDestAddrTon(MProcMessage message, int newDestTon) throws MProcRuleException {
        MProcUtility.checkDestAddrTon(newDestTon);
        MProcMessageImpl msg = (MProcMessageImpl) message;
        Sms sms = msg.getSmsContent();
        sms.getSmsSet().setDestAddrTon(newDestTon);
        sms.getSmsSet().setCorrelationId(null);
    }

    @Override
    public void updateMessageDestAddrNpi(MProcMessage message, int newDestNpi) throws MProcRuleException {
        MProcUtility.checkDestAddrNpi(newDestNpi);
        MProcMessageImpl msg = (MProcMessageImpl) message;
        Sms sms = msg.getSmsContent();
        sms.getSmsSet().setDestAddrNpi(newDestNpi);
        sms.getSmsSet().setCorrelationId(null);
    }

    @Override
    public void updateMessageDestAddr(MProcMessage message, String newDigits) throws MProcRuleException {
        MProcUtility.checkDestAddr(newDigits);
        MProcMessageImpl msg = (MProcMessageImpl) message;
        Sms sms = msg.getSmsContent();
        sms.getSmsSet().setDestAddr(newDigits);
        sms.getSmsSet().setCorrelationId(null);
    }

    @Override
    public void updateMessageSourceAddrTon(MProcMessage message, int newDestTon) throws MProcRuleException {
        MProcUtility.checkSourceAddrTon(newDestTon);
        MProcMessageImpl msg = (MProcMessageImpl) message;
        Sms sms = msg.getSmsContent();
        sms.setSourceAddrTon(newDestTon);
    }

    @Override
    public void updateMessageSourceAddrNpi(MProcMessage message, int newDestNpi) throws MProcRuleException {
        MProcUtility.checkSourceAddrNpi(newDestNpi);
        MProcMessageImpl msg = (MProcMessageImpl) message;
        Sms sms = msg.getSmsContent();
        sms.setSourceAddrNpi(newDestNpi);
    }

    @Override
    public void updateMessageSourceAddr(MProcMessage message, String newDigits) throws MProcRuleException {
        MProcUtility.checkSourceAddr(newDigits);
        MProcMessageImpl msg = (MProcMessageImpl) message;
        Sms sms = msg.getSmsContent();
        sms.setSourceAddr(newDigits);
    }

    @Override
    public void updateShortMessageText(MProcMessage message, String newShortMessageText) throws MProcRuleException {
        MProcUtility.checkShortMessageText(newShortMessageText);
        MProcMessageImpl msg = (MProcMessageImpl) message;
        Sms sms = msg.getSmsContent();
        sms.setShortMessageText(newShortMessageText);
    }

    @Override
    public void updateShortMessageBin(MProcMessage message, byte[] newShortMessageBin) throws MProcRuleException {
        MProcUtility.checkShortMessageBin(newShortMessageBin);
        MProcMessageImpl msg = (MProcMessageImpl) message;
        Sms sms = msg.getSmsContent();
        sms.setShortMessageBin(newShortMessageBin);
    }

    @Override
    public void updateScheduleDeliveryTime(MProcMessage message, Date scheduleDeliveryTime) {
        MProcMessageImpl msg = (MProcMessageImpl) message;
        Sms sms = msg.getSmsContent();
        scheduleDeliveryTime = MProcUtility.checkScheduleDeliveryTime(sms, scheduleDeliveryTime);
        sms.setScheduleDeliveryTime(scheduleDeliveryTime);
    }

    @Override
    public void updateValidityPeriod(MProcMessage message, Date newValidityPeriod) {
        newValidityPeriod = MProcUtility.checkValidityPeriod(newValidityPeriod, defaultValidityPeriodHours,
                maxValidityPeriodHours);
        MProcMessageImpl msg = (MProcMessageImpl) message;
        Sms sms = msg.getSmsContent();
        sms.setValidityPeriod(newValidityPeriod);
    }

    @Override
    public void updateDataCoding(MProcMessage message, int newDataCoding) {
        MProcMessageImpl msg = (MProcMessageImpl) message;
        Sms sms = msg.getSmsContent();
        sms.setDataCoding(newDataCoding);
    }

    @Override
    public void updateDataCodingGsm7(MProcMessage message) {
        updateDataCoding(message, MProcUtility.DataCodingGsm7);
    }

    @Override
    public void updateDataCodingGsm8(MProcMessage message) {
        updateDataCoding(message, MProcUtility.DataCodingGsm8);
    }

    @Override
    public void updateDataCodingUcs2(MProcMessage message) {
        updateDataCoding(message, MProcUtility.DataCodingUcs2);
    }

    @Override
    public void updateNationalLanguageSingleShift(MProcMessage message, int newNationalLanguageSingleShift) {
        MProcMessageImpl msg = (MProcMessageImpl) message;
        Sms sms = msg.getSmsContent();
        sms.setNationalLanguageSingleShift(newNationalLanguageSingleShift);
    }

    @Override
    public void updateNationalLanguageLockingShift(MProcMessage message, int newNationalLanguageLockingShift) {
        MProcMessageImpl msg = (MProcMessageImpl) message;
        Sms sms = msg.getSmsContent();
        sms.setNationalLanguageLockingShift(newNationalLanguageLockingShift);
    }

    @Override
    public void updateEsmClass(MProcMessage message, int newEsmClass) {
        MProcMessageImpl msg = (MProcMessageImpl) message;
        Sms sms = msg.getSmsContent();
        sms.setEsmClass(newEsmClass);
    }

    @Override
    public void updateEsmClass_ModeDatagram(MProcMessage message) {
        MProcMessageImpl msg = (MProcMessageImpl) message;
        Sms sms = msg.getSmsContent();
        sms.setEsmClass(MProcUtility.setEsmClass_ModeDatagram(sms.getEsmClass()));
    }

    @Override
    public void updateEsmClass_ModeTransaction(MProcMessage message) {
        MProcMessageImpl msg = (MProcMessageImpl) message;
        Sms sms = msg.getSmsContent();
        sms.setEsmClass(MProcUtility.setEsmClass_ModeTransaction(sms.getEsmClass()));
    }

    @Override
    public void updateEsmClass_ModeStoreAndForward(MProcMessage message) {
        MProcMessageImpl msg = (MProcMessageImpl) message;
        Sms sms = msg.getSmsContent();
        sms.setEsmClass(MProcUtility.setEsmClass_ModeStoreAndForward(sms.getEsmClass()));
    }

    @Override
    public void updateEsmClass_TypeNormalMessage(MProcMessage message) {
        MProcMessageImpl msg = (MProcMessageImpl) message;
        Sms sms = msg.getSmsContent();
        sms.setEsmClass(MProcUtility.setEsmClass_TypeNormalMessage(sms.getEsmClass()));
    }

    @Override
    public void updateEsmClass_TypeDeliveryReceipt(MProcMessage message) {
        MProcMessageImpl msg = (MProcMessageImpl) message;
        Sms sms = msg.getSmsContent();
        sms.setEsmClass(MProcUtility.setEsmClass_TypeDeliveryReceipt(sms.getEsmClass()));
    }

    @Override
    public void updateEsmClass_UDHIndicatorPresent(MProcMessage message) {
        MProcMessageImpl msg = (MProcMessageImpl) message;
        Sms sms = msg.getSmsContent();
        sms.setEsmClass(MProcUtility.setEsmClass_UDHIndicatorPresent(sms.getEsmClass()));
    }

    @Override
    public void updateEsmClass_UDHIndicatorAbsent(MProcMessage message) {
        MProcMessageImpl msg = (MProcMessageImpl) message;
        Sms sms = msg.getSmsContent();
        sms.setEsmClass(MProcUtility.setEsmClass_UDHIndicatorAbsent(sms.getEsmClass()));
    }

    @Override
    public void updatePriority(MProcMessage message, int newPriority) {
        MProcMessageImpl msg = (MProcMessageImpl) message;
        Sms sms = msg.getSmsContent();
        sms.setPriority(newPriority);
    }

    @Override
    public void updateRegisteredDelivery(MProcMessage message, int newRegisteredDelivery) {
        MProcMessageImpl msg = (MProcMessageImpl) message;
        Sms sms = msg.getSmsContent();
        sms.setRegisteredDelivery(newRegisteredDelivery);
    }

    @Override
    public void updateRegisteredDelivery_DeliveryReceiptNo(MProcMessage message) {
        MProcMessageImpl msg = (MProcMessageImpl) message;
        Sms sms = msg.getSmsContent();
        sms.setRegisteredDelivery(MProcUtility.setRegisteredDelivery_DeliveryReceiptNo(sms.getRegisteredDelivery()));
    }

    @Override
    public void updateRegisteredDelivery_DeliveryReceiptOnSuccessOrFailure(MProcMessage message) {
        MProcMessageImpl msg = (MProcMessageImpl) message;
        Sms sms = msg.getSmsContent();
        sms.setRegisteredDelivery(MProcUtility.setRegisteredDelivery_DeliveryReceiptOnSuccessOrFailure(sms.getRegisteredDelivery()));
    }

    @Override
    public void updateRegisteredDelivery_DeliveryReceiptOnFailure(MProcMessage message) {
        MProcMessageImpl msg = (MProcMessageImpl) message;
        Sms sms = msg.getSmsContent();
        sms.setRegisteredDelivery(MProcUtility.setRegisteredDelivery_DeliveryReceiptOnFailure(sms.getRegisteredDelivery()));
    }

    @Override
    public void updateRegisteredDelivery_DeliveryReceiptOnSuccess(MProcMessage message) {
        MProcMessageImpl msg = (MProcMessageImpl) message;
        Sms sms = msg.getSmsContent();
        sms.setRegisteredDelivery(MProcUtility.setRegisteredDelivery_DeliveryReceiptOnSuccess(sms.getRegisteredDelivery()));
    }

    @Override
    public MProcNewMessage createNewEmptyMessage(OriginationType originationType) {
        return MProcUtility.createNewEmptyMessage(this.defaultValidityPeriodHours, this.maxValidityPeriodHours, originationType);
    }

    @Override
    public MProcNewMessage createNewCopyMessage(MProcMessage message) {
        return MProcUtility.createNewCopyMessage(message, false, this.defaultValidityPeriodHours, this.maxValidityPeriodHours);
    }

    @Override
    public MProcNewMessage createNewResponseMessage(MProcMessage message) {
        return MProcUtility.createNewCopyMessage(message, true, this.defaultValidityPeriodHours, this.maxValidityPeriodHours);
    }

    @Override
    public void postNewMessage(MProcNewMessage message) {
        postedMessages.add(message);
    }

}
