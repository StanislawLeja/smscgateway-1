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

package org.mobicents.smsc.library;

/**
 * Created by mniemiec on 24.08.17.
 */
public class SmsExposureLayerData {

    private String messageId;
    private String correlationId;
    private String userId;

    private long elApiStart;
    private long elApiStop;
    private long elQueStart;
    private long elQueStop;

    private final String DATA_SEPARATOR = ":";
    private final String IDS_SEPARATOR = ",";

    public SmsExposureLayerData(String elDataString) throws NumberFormatException {
        String[] splitted = elDataString.split(DATA_SEPARATOR);
        if (splitted.length >= 7) {
            messageId = splitted[0];
            correlationId = splitted[1];
            userId = splitted[2];
            elApiStart = Long.valueOf(splitted[3]);
            elApiStop = Long.valueOf(splitted[4]);
            elQueStart = Long.valueOf(splitted[5]);
            elQueStop = Long.valueOf(splitted[6]);
        }
    }

    public String getFirstMessageId() {
        if (messageId != null) {
            if (messageId.contains(IDS_SEPARATOR)) {
                return messageId.substring(0, messageId.indexOf(IDS_SEPARATOR));
            }
            return messageId;
        }
        return "";
    }

    public void removeFirstMessageId() {
        if (messageId.contains(IDS_SEPARATOR)) {
            this.messageId = messageId.substring(messageId.indexOf(IDS_SEPARATOR) + 1);
        }
    }

    public String getFirstCorrelationId() {
        if (correlationId != null) {
            if (correlationId.contains(IDS_SEPARATOR)) {
                return correlationId.substring(0, correlationId.indexOf(IDS_SEPARATOR));
            }
            return correlationId;
        }
        return "";
    }

    public void removeFirstCorrelationId() {
        if (correlationId.contains(IDS_SEPARATOR)) {
            this.correlationId = correlationId.substring(correlationId.indexOf(IDS_SEPARATOR) + 1);
        }
    }

    public String getExposureLayerDataString() {
        StringBuilder sb = new StringBuilder();
        sb.append(messageId)
                .append(DATA_SEPARATOR)
                .append(correlationId)
                .append(DATA_SEPARATOR)
                .append(userId)
                .append(DATA_SEPARATOR)
                .append(elApiStart)
                .append(DATA_SEPARATOR)
                .append(elApiStop)
                .append(DATA_SEPARATOR)
                .append(elQueStart)
                .append(DATA_SEPARATOR)
                .append(elQueStop);
        return sb.toString();
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public long getElApiStart() {
        return elApiStart;
    }

    public void setElApiStart(long elApiStart) {
        this.elApiStart = elApiStart;
    }

    public long getElApiStop() {
        return elApiStop;
    }

    public void setElApiStop(long elApiStop) {
        this.elApiStop = elApiStop;
    }

    public long getElQueStart() {
        return elQueStart;
    }

    public void setElQueStart(long elQueStart) {
        this.elQueStart = elQueStart;
    }

    public long getElQueStop() {
        return elQueStop;
    }

    public void setElQueStop(long elQueStop) {
        this.elQueStop = elQueStop;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("SmsExposureLayerData [");
        if (messageId != null) {
            sb.append("messageId=");
            sb.append(messageId);
            sb.append(", ");
        }
        if (correlationId != null) {
            sb.append("correlationId=");
            sb.append(correlationId);
            sb.append(", ");
        }
        if (userId != null) {
            sb.append("userId=");
            sb.append(userId);
            sb.append(", ");
        }
        if (elApiStart != 0) {
            sb.append("elApiStart=");
            sb.append(elApiStart);
            sb.append(", ");
        }
        if (elApiStop != 0) {
            sb.append("elApiStop=");
            sb.append(elApiStop);
            sb.append(", ");
        }
        if (elQueStart != 0) {
            sb.append("elQueStart=");
            sb.append(elQueStart);
            sb.append(", ");
        }
        if (elQueStop != 0) {
            sb.append("elQueStop=");
            sb.append(elQueStop);
            sb.append(", ");
        }
        sb.append("]");

        return sb.toString();
    }
}
