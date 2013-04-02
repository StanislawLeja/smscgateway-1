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

import java.util.List;

import org.mobicents.protocols.ss7.map.api.primitives.ISDNAddressString;
import org.mobicents.protocols.ss7.sccp.parameter.SccpAddress;

/**
 * Business interface for persistence service. This interface defines all methods required for persisting/managing sms data in
 * backend storage
 * 
 * @author baranowb
 * 
 */
public interface Persistence {

//    public PersistableSms createInstance(Sms event, SmType type, SccpAddress scAddress, SccpAddress mxAddress);

    public void create(PersistableSms sms) throws PersistenceException;

    public void updateTargetAddress(PersistableSms sms) throws PersistenceException;

    public void updateDeliveryCount(PersistableSms sms) throws PersistenceException;

    public void markAlertingNotSupported(PersistableSms sms) throws PersistenceException;

    /**
     * Archives SMS, its present in live DB, its removed and moved to archive. If it does not exist, its just archived.
     * 
     * @param sms
     * @throws PersistenceException
     */
    public void archive(PersistableSms sms) throws PersistenceException;

    /**
     * This method fetch SMS's that are in "LIVE" DB and are not currently handled by system. 
     * 
     * @param msisdn
     * @return
     * @throws PersistenceException
     */
    public List<PersistableSms> fetchOutstandingSms(ISDNAddressString msisdn) throws PersistenceException;
    /**
     * 
     * @return
     * @throws PersistenceException
     */
    public List<PersistableSms> fetchSchedulableSms() throws PersistenceException;

    /**
     * Passivates SMS. Making it eligible to be returned by {@link #fetchOutstandingSms(String)} method.
     * Passivation implies update to "DUE_DATE" value in DB.
     * 
     * @param sms
     * @throws PersistenceException
     */
    public void passivate(PersistableSms sms) throws PersistenceException;
    /**
     * Marks SMS as being processed in system.
     * @param sms
     * @throws PersistenceException
     */
    public void activate(PersistableSms sms) throws PersistenceException;

    // TODO: XXX: maybe better to have
    // createInstance
    // create
    // persist
    // archive
    // ?
}
