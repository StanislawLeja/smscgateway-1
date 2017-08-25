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

    private long elApiStart;
    private long elApiStop;
    private long elQueStart;
    private long elQueStop;

    public SmsExposureLayerData(String elDataString) throws NumberFormatException {
        String[] splitted = elDataString.split(":");
        if (splitted.length >= 6) {
            messageId = splitted[0];
            correlationId = splitted[1];
            elApiStart = Long.valueOf(splitted[2]);
            elApiStop = Long.valueOf(splitted[3]);
            elQueStart = Long.valueOf(splitted[4]);
            elQueStop = Long.valueOf(splitted[5]);
        }
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
