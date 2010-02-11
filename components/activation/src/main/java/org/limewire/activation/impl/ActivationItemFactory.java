package org.limewire.activation.impl;

import java.util.Date;

import org.limewire.activation.api.ActivationItem;
import org.limewire.activation.api.ActivationItem.Status;

/**
 * Creates ActivationItems from a set of data.
 */
interface ActivationItemFactory {

    /**
     * Create's an ActivationItem from the json string sent from the server.
     */
    public ActivationItem createActivationItem(int intID, String licenseName, Date datePurchased, Date dateExpired,
            Status currentStatus);    
    
    /**
     * Create's an ActivationItem from the json string read from disk. Unlike the server, date expired
     * is checked against system time for authenticating the valididy of the modules.
     */
    public ActivationItem createActivationItemFromDisk(int intID, String licenseName, Date datePurchased,
            Date dateExpired, Status currentStatus);
}
