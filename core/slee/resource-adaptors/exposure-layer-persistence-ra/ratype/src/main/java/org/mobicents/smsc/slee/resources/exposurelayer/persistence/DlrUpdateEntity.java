package org.mobicents.smsc.slee.resources.exposurelayer.persistence;

/**
 * Created by rafal on 27.09.17.
 */
public class DlrUpdateEntity {
    private Long exposureLayerMessageId;
    private String exposureLayerUserName;
    private String deliveryReceiptStatus;
    private String correlationId;
    private String appSid;
    private String originalRequestUserName;

    public DlrUpdateEntity() {
    }

    public Long getExposureLayerMessageId() {
        return exposureLayerMessageId;
    }

    public void setExposureLayerMessageId(Long exposureLayerMessageId) {
        this.exposureLayerMessageId = exposureLayerMessageId;
    }

    public String getExposureLayerUserName() {
        return exposureLayerUserName;
    }

    public void setExposureLayerUserName(String exposureLayerUsername) {
        this.exposureLayerUserName = exposureLayerUsername;
    }

    public String getDeliveryReceiptStatus() {
        return deliveryReceiptStatus;
    }

    public void setDeliveryReceiptStatus(String deliveryReceiptStatus) {
        this.deliveryReceiptStatus = deliveryReceiptStatus;
    }

    public String getAppSid() {
        return appSid;
    }

    public void setAppSid(String appSid) {
        this.appSid = appSid;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public String getOriginalRequestUserName() {
        return originalRequestUserName;
    }

    public void setOriginalRequestUserName(String originalRequestUserName) {
        this.originalRequestUserName = originalRequestUserName;
    }

    @Override
    public String toString() {
        return "DlrUpdateEntity{" +
                "exposureLayerMessageId=" + exposureLayerMessageId +
                ", exposureLayerUsername='" + exposureLayerUserName + '\'' +
                ", deliveryReceiptStatus='" + deliveryReceiptStatus + '\'' +
                ", correlationId='" + correlationId + '\'' +
                ", appSid='" + appSid + '\'' +
                ", originalRequestUserName='" + originalRequestUserName + '\'' +
                '}';
    }
}
