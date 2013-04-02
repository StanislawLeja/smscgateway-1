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

package org.mobicents.smsc.slee.services.persistence;

import java.util.Date;
import java.util.List;

import org.mobicents.protocols.ss7.map.api.primitives.IMSI;
import org.mobicents.protocols.ss7.map.api.service.sms.LocationInfoWithLMSI;

import com.eaio.uuid.UUID;

/**
 * Business interface for persistence service. This interface defines all methods required for persisting/managing sms data in
 * backend storage
 * 
 * @author baranowb
 * 
 */
public interface Persistence {

	/**
	 * Searching SmsSet for TargetAddress in LIVE table If found - creates
	 * SmsSet for this object If not found - creates new record in LIVE and
	 * SmsSet for this object
	 * 
	 * @param ta
	 *            TargetAddress
	 * @return SmsSet object that represents TargetAddress (must not be null)
	 * @throws PersistenceException
	 */
	public SmsSet obtainSmsSet(TargetAddress ta) throws PersistenceException;

	/**
	 * Set this TargetAddress as scheduled.
	 * If (IN_SYSTEM==2 or IN_SYSTEM==1 and newDueDate > currentDueDate)
	 * IN_SYSTEM=1, DUE_DATE=newDueDate
	 * 
	 * @param smsSet
	 * @param newDueDate
	 */
	public void setScheduled(SmsSet smsSet, Date newDueDate) throws PersistenceException;

	/**
	 * Set destination for SmsSet. Database is not updated for these fields
	 * 
	 * @param smsSet
	 * @param destClusterName
	 * @param destSystemId
	 * @param destEsmeId
	 */
	public void setDestination(SmsSet smsSet, String destClusterName, String destSystemId, String destEsmeId);

	/**
	 * Set routing info for SmsSet. Database is not updated for these fields
	 * 
	 * @param smsSet
	 * @param imsi
	 * @param locationInfoWithLMSI
	 */
	public void setRoutingInfo(SmsSet smsSet, IMSI imsi, LocationInfoWithLMSI locationInfoWithLMSI);

	/**
	 * Update database for setting smsSet to be under delivering processing:
	 * IN_SYSTEM=2, DELIVERY_COUNT++ 
	 * 
	 * @param smsSet
	 * @throws PersistenceException
	 */
	public void setDeliveryStart(SmsSet smsSet) throws PersistenceException;

	/**
	 * Update database for setting smsSet to be out delivering processing with success
	 * IN_SYSTEM=0, DUE_DATE=null, SM_STATUS=0
	 * 
	 * @param smsSet
	 * @throws PersistenceException
	 */
	public void setDeliverySuccess(SmsSet smsSet) throws PersistenceException;

	/**
	 * Update database for setting smsSet to be out delivering processing with delivery failure
	 * IN_SYSTEM=0, SM_STATUS=smStatus, LAST_DELIVERY=lastDelivery, ALERTING_SUPPORTED=alertingSupported
	 * 
	 * @param smsSet
	 * @param smStatus
	 * @param lastDelivery
	 * @param alertingSupported
	 * @throws PersistenceException
	 */
	public void setDeliveryFailure(SmsSet smsSet, ErrorCode smStatus, Date lastDelivery, boolean alertingSupported) throws PersistenceException;

	/**
	 * Deleting SmsSet record from LIVE table. 
	 * The record will not be deleted if there are corresponded records in LIVE_SMS table
	 * 
	 * @param smsSet
	 * @return true if success, false if not deleted because of records in LIVE_SMS table exist
	 * @throws PersistenceException
	 */
	public boolean deleteSmsSet(SmsSet smsSet) throws PersistenceException;


	/**
	 * Creates a record in LIVE_SMS table according to sms parameter
	 * 
	 * @param sms
	 * @throws PersistenceException
	 */
	public void createLiveSms(Sms sms) throws PersistenceException;

	/**
	 * Getting sms from LIVE_SMS table
	 * 
	 * @param dbId
	 * @return sms or null if sms not found
	 * @throws PersistenceException
	 */
	public Sms obtainLiveSms(UUID dbId) throws PersistenceException;

	/**
	 * Getting sms from LIVE_SMS table
	 * 
	 * @param messageId
	 * @return sms or null if sms not found
	 * @throws PersistenceException
	 */
	public Sms obtainLiveSms(long messageId) throws PersistenceException;

	/**
	 * Update modified fields in an existing record in LIVE_SMS table
	 * If record is absent do nothing
	 * 
	 * TODO: not yet implemented
	 * 
	 * @param sms
	 * @throws PersistenceException
	 */
	public void updateLiveSms(Sms sms) throws PersistenceException;

	/**
	 * Move sms from LIVE_SMS to ARCHIVE
	 * SM_STATUS=0
	 * 
	 * @param sms
	 * @throws PersistenceException
	 */
	public void archiveDeliveredSms(Sms sms) throws PersistenceException;

	/**
	 * Move sms from LIVE_SMS to ARCHIVE
	 * SM_STATUS=value from LIVE_SMS record
	 * 
	 * @param sms
	 * @throws PersistenceException
	 */
	public void archiveFailuredSms(Sms sms) throws PersistenceException;


	/**
	 * Get list of SmsSet that DUE_DATE<now and IN_SYSTEM==2
	 * 
	 * @param maxRecordCount
	 * @return
	 * @throws PersistenceException
	 */
	public List<SmsSet> fetchSchedulableSmsSets(int maxRecordCount) throws PersistenceException;

	/**
	 * Fill SmsSet with sms from LIVE_SMS
	 * 
	 * @param smsSet
	 * @throws PersistenceException
	 */
	public void fetchSchedulableSms(SmsSet smsSet) throws PersistenceException;

}
