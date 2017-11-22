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

import com.google.gson.JsonObject;
import org.apache.http.client.methods.HttpPost;
import org.mobicents.slee.SbbContextExt;
import org.mobicents.smsc.cassandra.PersistenceException;
import org.mobicents.smsc.domain.HttpUserMBean;
import org.mobicents.smsc.domain.HttpUsersManagement;
import org.mobicents.smsc.domain.HttpUsersManagementMBean;
import org.mobicents.smsc.domain.SmscPropertiesManagement;
import org.mobicents.smsc.library.Sms;
import org.mobicents.smsc.library.SmsExposureLayerData;
import org.mobicents.smsc.slee.resources.exposurelayer.persistence.ExposureLayerPersistenceRAInterface;
import org.mobicents.smsc.slee.resources.persistence.PersistenceRAInterface;
import org.mobicents.smsc.slee.resources.exposurelayer.persistence.DlrUpdateEntity;

import javax.slee.SLEEException;
import javax.slee.facilities.Tracer;
import javax.slee.resource.ResourceAdaptorTypeID;



/**
 * The Class DeliveryReceiptHttpNotification.
 */
public final class DeliveryReceiptHttpNotification {

    //private static final String HTTP_CLIENT_NIO_RA_LINK = "HttpClientNIO";
    private static final String El_PERSISTENCE_RA_LINK = "ExposureLayerPersistenceResourceAdaptor";

    private static final ResourceAdaptorTypeID El_PERSISTENCE_RA_ID = new ResourceAdaptorTypeID(
            "ExposureLayerPersistenceResourceAdaptorType", "org.mobicents", "1.0");

    private static final String PROPERTY_CORRELATION_ID = "correlationId";
    private static final String PROPERTY_DELIVERY_RECEIPT_STATUS = "deliveryReceiptStatus";
    private static final String PROPERTY_ORIGINAL_REQUEST_USER_NAME = "originalRequestUserName";
    private static final String PROPERTY_EXPOSURE_LAYER_MESSAGE_ID = "exposureLayerMessageId";
    private static final String PROPERTY_EXPOSURE_LAYER_USER_NAME = "exposureLayerUserName";
    private static final String PROPERTY_EXPOSURE_LAYER_APPLICATION_SID = "appSid";

    private final Tracer itsTracer;
    private final HttpPost itsDeliveryReceiptHttpMethod;
    private final String itsDeliveryReceiptApplicationSid;
    private final PersistenceRAInterface itsPersistenceRa;
    private final HttpUsersManagementMBean itsHttpUsersManagement;
    /*private final HttpClientNIOResourceAdaptorSbbInterface itsHttpClientNio;*/
    private final ExposureLayerPersistenceRAInterface itsElDBO;

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
        itsDeliveryReceiptApplicationSid = anSmscProperties.getDeliveryReceiptHttpNotificationAppSid();
       /* itsHttpClientNio = (HttpClientNIOResourceAdaptorSbbInterface) aContext
                .getResourceAdaptorInterface(HttpClientNIOResourceAdaptorType.ID, HTTP_CLIENT_NIO_RA_LINK);*/
        itsPersistenceRa = aPersistenceRa;
        itsElDBO = (ExposureLayerPersistenceRAInterface) aContext.getResourceAdaptorInterface(El_PERSISTENCE_RA_ID,El_PERSISTENCE_RA_LINK);
    }

    /**
     * Handle delivery receipt data.
     *
     * @param anOriginalMessageId the original message ID
     * @param aData the data
     */
    public void handleDeliveryReceiptData(final long aMessageId, final String aStatus) {
        /*if (itsDeliveryReceiptHttpMethod == null) {
            return;
        }*/
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
            final SmsExposureLayerData eld = new SmsExposureLayerData(exposureLayerData);
            json.addProperty(PROPERTY_EXPOSURE_LAYER_MESSAGE_ID, eld.getMessageId());
            json.addProperty(PROPERTY_CORRELATION_ID, eld.getCorrelationId());
            json.addProperty(PROPERTY_EXPOSURE_LAYER_USER_NAME, eld.getUserId());
            json.addProperty(PROPERTY_ORIGINAL_REQUEST_USER_NAME, hu.getUserName());
            json.addProperty(PROPERTY_DELIVERY_RECEIPT_STATUS, aStatus);
            if (itsDeliveryReceiptApplicationSid != null) {
                json.addProperty(PROPERTY_EXPOSURE_LAYER_APPLICATION_SID, itsDeliveryReceiptApplicationSid);
            }
            /*itsDeliveryReceiptHttpMethod.setEntity(new StringEntity(json.toString(), ContentType.APPLICATION_JSON));*/
            /*itsHttpClientNio.execute(itsDeliveryReceiptHttpMethod, null, null);*/
            DlrUpdateEntity dlrUpdateEntity = new DlrUpdateEntity();
            dlrUpdateEntity.setCorrelationId(eld.getCorrelationId());
            dlrUpdateEntity.setDeliveryReceiptStatus(aStatus);
            dlrUpdateEntity.setExposureLayerMessageId(Long.getLong(eld.getMessageId()));//??
            dlrUpdateEntity.setExposureLayerUserName(eld.getUserId());
            dlrUpdateEntity.setOriginalRequestUserName(hu.getUserName());
            itsElDBO.upadateDlrState(dlrUpdateEntity);
        } catch (NullPointerException | IllegalStateException | SLEEException | PersistenceException e) {
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
