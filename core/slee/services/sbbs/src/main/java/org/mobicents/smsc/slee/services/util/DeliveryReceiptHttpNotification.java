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
import org.mobicents.smsc.cassandra.PersistenceException;
import org.mobicents.smsc.domain.HttpUserMBean;
import org.mobicents.smsc.domain.HttpUsersManagement;
import org.mobicents.smsc.domain.HttpUsersManagementMBean;
import org.mobicents.smsc.domain.SmscPropertiesManagement;
import org.mobicents.smsc.library.Sms;
import org.mobicents.smsc.library.SmsExposureLayerData;
import org.mobicents.smsc.slee.resources.persistence.PersistenceRAInterface;
import org.restcomm.slee.ra.httpclient.nio.ratype.HttpClientNIOResourceAdaptorSbbInterface;
import org.restcomm.slee.ra.httpclient.nio.ratype.HttpClientNIOResourceAdaptorType;

import com.google.gson.JsonObject;

/**
 * The Class DeliveryReceiptHttpNotification.
 */
public final class DeliveryReceiptHttpNotification {

    private static final String HTTP_CLIENT_NIO_RA_LINK = "HttpClientNIO";

    private static final String PROPERTY_CORRELATION_ID = "correlationId";
    private static final String PROPERTY_DELIVERY_RECEIPT_STATUS = "deliveryReceiptStatus";
    private static final String PROPERTY_ORIGINAL_REQUEST_USER_NAME = "originalRequestUserName";

    private final Tracer itsTracer;
    private final HttpPost itsDeliveryReceiptHttpMethod;
    private final PersistenceRAInterface itsPersistenceRa;
    private final HttpUsersManagementMBean itsHttpUsersManagement;
    private final HttpClientNIOResourceAdaptorSbbInterface itsHttpClientNio;

    /**
     * Instantiates a new delivery receipt HTTP notification.
     *
     * @param aTracer the tracer
     * @param anSmscProperties the SMSC properties
     * @param aContext the context
     * @param aPersistenceRa the persistence RA
     */
    public DeliveryReceiptHttpNotification(final Tracer aTracer, final SmscPropertiesManagement anSmscProperties,
            final SbbContextExt aContext, final PersistenceRAInterface aPersistenceRa) {
        itsTracer = aTracer;
        itsHttpUsersManagement = HttpUsersManagement.getInstance();
        itsDeliveryReceiptHttpMethod = getPost(anSmscProperties);
        itsHttpClientNio = (HttpClientNIOResourceAdaptorSbbInterface) aContext
                .getResourceAdaptorInterface(HttpClientNIOResourceAdaptorType.ID, HTTP_CLIENT_NIO_RA_LINK);
        itsPersistenceRa = aPersistenceRa;
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
        try {
            final Sms originalRequest = itsPersistenceRa.c2_getRecordArchiveForMessageId(aMessageId);
            if (originalRequest == null) {
                itsTracer.warning("Unable to report DR with HTTP. Original SMS not found.");
                return;
            }
            final HttpUserMBean hu = itsHttpUsersManagement.getHttpUserByNetworkId(originalRequest.getOrigNetworkId());
            if (hu == null) {
                itsTracer.warning("Unable to report DR with HTTP. HTTP User for Orig NetworkId "
                        + originalRequest.getOrigNetworkId() + " is not found.");
                return;
            }
            final String exposureLayerData = originalRequest.getExposureLayerData();
            if (exposureLayerData == null) {
                itsTracer.warning("Unable to report DR with HTTP. Exposure Layer Data is not set.");
                return;
            }
            json.addProperty(PROPERTY_DELIVERY_RECEIPT_STATUS, aStatus);
            json.addProperty(PROPERTY_ORIGINAL_REQUEST_USER_NAME, hu.getUserName());
            json.addProperty(PROPERTY_CORRELATION_ID, parseCorrelationId(new SmsExposureLayerData(exposureLayerData)));
            itsDeliveryReceiptHttpMethod.setEntity(new StringEntity(json.toString(), ContentType.APPLICATION_JSON));
            itsHttpClientNio.execute(itsDeliveryReceiptHttpMethod, null, null);
        } catch (NullPointerException | IllegalStateException | SLEEException | StartActivityException
                | PersistenceException e) {
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

    private static String parseCorrelationId(final SmsExposureLayerData aData) {
        return aData.getCorrelationId();
    }

}
