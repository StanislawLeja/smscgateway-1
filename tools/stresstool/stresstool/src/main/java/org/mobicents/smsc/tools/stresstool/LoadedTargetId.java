package org.mobicents.smsc.tools.stresstool;

import java.util.Date;

public class LoadedTargetId {

    private String targetId;
    private Date dtx;
    private long dueSlot;

    public String getTargetId() {
        return targetId;
    }

    public Date getDtx() {
        return dtx;
    }

    public long getDueSlot() {
        return dueSlot;
    }

    public LoadedTargetId(String targetId, Date dtx, long dueSlot) {
        this.targetId = targetId;
        this.dtx = dtx;
        this.dueSlot = dueSlot;
    }

}
