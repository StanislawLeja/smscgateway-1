package org.mobicents.smsc.slee.resources.scheduler;

import javax.slee.resource.ActivityHandle;

import com.eaio.uuid.UUID;

public class SchedulerActivityHandle implements ActivityHandle {

    private final String id;

    public SchedulerActivityHandle() {
        super();
        this.id = new UUID().toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        SchedulerActivityHandle other = (SchedulerActivityHandle) obj;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        return true;
    }

}
