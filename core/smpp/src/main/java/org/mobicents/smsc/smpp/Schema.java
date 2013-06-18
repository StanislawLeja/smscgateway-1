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

package org.mobicents.smsc.smpp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Helper class to store all that CQL mumbo jumbo
 * 
 * @author baranowb
 * @author sergey vetyutnev
 * 
 */
public class Schema {

    //SmsRoutingRule tables columns and names
    public static final String COLUMN_ID = "ID";
    public static final String COLUMN_ADDRESS = "ADDRESS";
    public static final String COLUMN_SYSTEM_ID = "SYSTEM_ID";

    public static final String FAMILY_SMS_ROUTING_RULE = "SMS_ROUTING_RULE";

    public static final List<String> COLUMNS_SMS_ROUTING_RULE;

    static {
        List<String> tmp = new ArrayList<String>();
        tmp.add(COLUMN_ID);

        tmp.add(COLUMN_ADDRESS);
        tmp.add(COLUMN_SYSTEM_ID);

        COLUMNS_SMS_ROUTING_RULE = Collections.unmodifiableList(tmp);
    }

}
