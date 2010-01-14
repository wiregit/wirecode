package org.limewire.activation.impl;

import java.util.Date;

import org.limewire.activation.api.ActivationItem;
import org.limewire.activation.api.ActivationItem.Status;

public interface ActivationItemFactory {

    public ActivationItem createActivationItem(int intID, String licenseName, Date datePurchased, Date dateExpired,
            Status currentStatus);    
}
