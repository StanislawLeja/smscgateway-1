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
package org.mobicents.smsc.cassandra;

import org.apache.log4j.Logger;

/**
 * 
 * @author amit bhayani
 * 
 */
public class CdrGenerator {
	private static final Logger logger = Logger.getLogger(CdrGenerator.class);

	public static final String CDR_SEPARATOR = ",";
    public static final String CDR_SUCCESS = "success";
    public static final String CDR_PARTIAL = "partial";
    public static final String CDR_FAILED = "failed";
    public static final String CDR_TEMP_FAILED = "temp_failed";

    public static final String CDR_SUCCESS_ESME = "success_esme";
    public static final String CDR_PARTIAL_ESME = "partial_esme";
    public static final String CDR_FAILED_ESME = "failed_esme";
    public static final String CDR_TEMP_FAILED_ESME = "temp_failed_esme";

    public static final String CDR_SUCCESS_NO_REASON = "";

	public static void generateCdr(String message) {
		logger.debug(message);
	}

	public static void generateCdr(Sms smsEvent, String status, String reason) {
        // Format is
        // SUBMIT_DATE,SOURCE_ADDRESS,SOURCE_TON,SOURCE_NPI,DESTINATION_ADDRESS,DESTINATION_TON,DESTINATION_NPI,STATUS,SYSTEM-ID,MESSAGE-ID,First
        // 20 char of SMS, REASON

        StringBuffer sb = new StringBuffer();
        sb.append(smsEvent.getSubmitDate()).append(CdrGenerator.CDR_SEPARATOR).append(smsEvent.getSourceAddr())
                .append(CdrGenerator.CDR_SEPARATOR).append(smsEvent.getSourceAddrTon())
                .append(CdrGenerator.CDR_SEPARATOR).append(smsEvent.getSourceAddrNpi())
                .append(CdrGenerator.CDR_SEPARATOR).append(smsEvent.getSmsSet().getDestAddr())
                .append(CdrGenerator.CDR_SEPARATOR).append(smsEvent.getSmsSet().getDestAddrTon())
                .append(CdrGenerator.CDR_SEPARATOR).append(smsEvent.getSmsSet().getDestAddrNpi())
                .append(CdrGenerator.CDR_SEPARATOR).append(status).append(CdrGenerator.CDR_SEPARATOR)
                .append(smsEvent.getOrigSystemId()).append(CdrGenerator.CDR_SEPARATOR).append(smsEvent.getMessageId())
                .append(CdrGenerator.CDR_SEPARATOR).append(getFirst20CharOfSMS(smsEvent.getShortMessage()))
                .append(CdrGenerator.CDR_SEPARATOR).append(reason);

        // TODO : remove this !!!!!!!
//        logger.error(sb.toString());
//        Logger logger2 = Logger.getLogger(DBOperations_C2.class);
//        logger2.error(sb.toString());
        // TODO : remove this !!!!!!!

        CdrGenerator.generateCdr(sb.toString());
    }

    private static String getFirst20CharOfSMS(byte[] rawSms) {
        String first20CharOfSms = new String(rawSms);
        if (first20CharOfSms.length() > 20) {
            first20CharOfSms = first20CharOfSms.substring(0, 20);
        }
        return first20CharOfSms;
    }
}
