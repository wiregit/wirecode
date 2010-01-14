package org.limewire.activation.api;

import java.util.Comparator;

import org.limewire.activation.api.ActivationItem.Status;

public class ActivationItemComparator implements Comparator<ActivationItem> {
    public int compare(ActivationItem itemA, ActivationItem itemB) {
        if (itemA.getStatus() == Status.ACTIVE && itemB.getStatus() != Status.ACTIVE)
            return -1;
        if (itemA.getStatus() != Status.ACTIVE && itemB.getStatus() == Status.ACTIVE)
            return 1;
        else 
            return 0;
    }
}
