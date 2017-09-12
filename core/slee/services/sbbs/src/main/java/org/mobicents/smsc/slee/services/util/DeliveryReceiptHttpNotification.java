/*
 * Telestax, Open Source Cloud Communications Copyright 2011-2017,
 * Telestax Inc and individual contributors by the @authors tag.
 * See the copyright.txt in the distribution for a full listing of
 * individual contributors.
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
package org.mobicents.smsc.slee.services.util;

import javax.slee.SLEEException;
import javax.slee.facilities.Tracer;
import javax.slee.resource.StartActivityException;

import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.mobicents.slee.SbbContextExt;
import org.mobicents.smsc.domain.SmscPropertiesManagement;
import org.restcomm.slee.ra.httpclient.nio.ratype.HttpClientNIOResourceAdaptorSbbInterface;
import org.restcomm.slee.ra.httpclient.nio.ratype.HttpClientNIOResourceAdaptorType;

import com.google.gson.JsonObject;

/**
 * The Class DeliveryReceiptHttpNotification.
 */
public final class DeliveryReceiptHttpNotification {

    private static final String HTTP_CLIENT_NIO_RA_LINK = "HttpClientNioRa";

    private static final String PROPERTY_MESSAGE_ID = "messageId";
    private static final String PROPERTY_STATUS = "status";

    private final Tracer itsTracer;
    private final HttpPost itsDeliveryReceiptHttpMethod;
    private final HttpClientNIOResourceAdaptorSbbInterface itsHttpClientNio;

    /**
     * Instantiates a new delivery receipt HTTP notification.
     *
     * @param aTracer the tracer
     * @param anSmscProperties the SMSC properties
     * @param aContext the context
     */
    public DeliveryReceiptHttpNotification(final Tracer aTracer, final SmscPropertiesManagement anSmscProperties,
            final SbbContextExt aContext) {
        itsTracer = aTracer;
        itsDeliveryReceiptHttpMethod = getPost(anSmscProperties);
        itsHttpClientNio = (HttpClientNIOResourceAdaptorSbbInterface) aContext
                .getResourceAdaptorInterface(HttpClientNIOResourceAdaptorType.ID, HTTP_CLIENT_NIO_RA_LINK);
    }

    /**
     * Handle delivery receipt data.
     *
     * @param anOriginalMessageId the original message ID
     * @param aData the data
     */
    public void handleDeliveryReceiptData(final long aMessageId, final String aStatus) {
        if (itsDeliveryReceiptHttpMethod == null) {
            return;
        }
        final JsonObject json = new JsonObject();
        json.addProperty(PROPERTY_MESSAGE_ID, aMessageId);
        json.addProperty(PROPERTY_STATUS, aStatus);
        itsDeliveryReceiptHttpMethod.setEntity(new StringEntity(json.toString(), ContentType.APPLICATION_JSON));
        try {
            itsHttpClientNio.execute(itsDeliveryReceiptHttpMethod, null, null);
        } catch (NullPointerException | IllegalStateException | SLEEException | StartActivityException e) {
            itsTracer.warning("Unable to report DR with HTTP. Message: " + e.getMessage() + ".", e);
        }
    }

    private HttpPost getPost(final SmscPropertiesManagement anSmscProperties) {
        final String url = anSmscProperties.getDeliveryReceiptHttpNotificationUrl();
        if (url == null) {
            return null;
        }
        if (url.isEmpty()) {
            return null;
        }
        return new HttpPost(url);
    }

}
