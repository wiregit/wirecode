package org.limewire.ui.swing.activation;

import java.util.Comparator;
import java.util.Locale;

import org.limewire.activation.api.ActivationItem;
import org.limewire.activation.api.ActivationItem.Status;

public class ActivationItemComparator implements Comparator<ActivationItem> {
    public int compare(ActivationItem itemA, ActivationItem itemB) {
        if (itemA.getStatus() == Status.ACTIVE && itemB.getStatus() != Status.ACTIVE)
            return -1;
        if (itemA.getStatus() != Status.ACTIVE && itemB.getStatus() == Status.ACTIVE)
            return 1;
        else {
            // if same status, sort alphabetically
            return itemA.getLicenseName().toLowerCase(Locale.US).compareTo(itemB.getLicenseName().toLowerCase(Locale.US));
        }
    }
}
