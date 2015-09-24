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

import javolution.xml.stream.XMLStreamException;

/**
*
* @author sergey vetyutnev
*
*/
public interface MProcRule {

    /**
     * @return the id of the mproc rule
     */
    int getId();

    /**
     * @return Rule class of the mproc rule ("default" or other when a customer implementation)
     */
    String getRuleClassName();

    /**
     * @return true if the mproc rule fits to a message
     */
    boolean matches(MProcMessage message);

    /**
     * @return true if the mproc rule is used for the phase when a message has just come to SMSC
     */
    boolean isForPostArrivalState();

    /**
     * @return true if the mproc rule is used for the phase when IMSI / NNN has been received from HRL
     */
    boolean isForPostImsiRequestState();

    /**
     * @return true if the mproc rule is used for the phase when a message has just been delivered (or delivery failure)
     */
    boolean isForPostDeliveryState();

    /**
     * the event occurs when a message has just come to SMSC
     */
    void onPostArrival(PostArrivalProcessor factory, MProcMessage message)
            throws Exception;

    /**
     * the event occurs when IMSI / NNN has been received from HRL
     */
    void onPostImsiRequest(PostImsiProcessor factory, MProcMessage message, String imsi,
            String nnnDigits, int nnnTon, int nnnNpi) throws Exception;

    /**
     * the event occurs when a message has just been delivered (or delivery failure)
     */
    void onPostDelivery(PostDeliveryProcessor factory, MProcMessage message,
            boolean isDeliveryFailure) throws Exception;

    /**
     * this method must implement setting of rule parameters as for provided CLI string
     */
    void setRuleParameters(String parametersString);

    /**
     * @return rule parameters as CLI return string
     */
    String getRuleParameters();

    /**
     * implementation of XML deserializing for a customer rule (string into xml config file)
     */
    public void readXml(javolution.xml.XMLFormat.InputElement xml, MProcRule rule) throws XMLStreamException;

    /**
     * implementation of XML serializing for a customer rule (string into xml config file)
     */
    public void writeXml(MProcRule rule, javolution.xml.XMLFormat.OutputElement xml) throws XMLStreamException;

}
