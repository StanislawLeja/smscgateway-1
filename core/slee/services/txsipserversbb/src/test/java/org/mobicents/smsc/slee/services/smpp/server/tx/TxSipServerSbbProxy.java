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

package org.mobicents.smsc.slee.services.smpp.server.tx;

import org.mobicents.slee.ChildRelationExt;
import org.mobicents.smsc.library.Sms;
import org.mobicents.smsc.library.SmscProcessingException;
import org.mobicents.smsc.library.TargetAddress;
import org.mobicents.smsc.slee.resources.persistence.PersistenceRAInterface;
import org.mobicents.smsc.slee.services.sip.server.tx.TxSipServerSbb;

/**
 * 
 * @author servey vetyutnev
 * 
 */
public class TxSipServerSbbProxy extends TxSipServerSbb {

    public Sms createSmsEvent(String fromUser, byte[] message, TargetAddress ta, PersistenceRAInterface store) throws SmscProcessingException {
        return super.createSmsEvent(fromUser, message, ta, store, null);
    }

    @Override
    public ChildRelationExt getChargingSbb() {
        // TODO Auto-generated method stub
        return null;
    }

}
