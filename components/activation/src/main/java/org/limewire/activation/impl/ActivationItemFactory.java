package org.limewire.activation.impl;

import org.limewire.activation.api.ActivationItem;
import org.limewire.activation.api.ActivationItem.Status;
import org.limewire.activation.serial.ActivationMemento;
import org.limewire.io.InvalidDataException;

public interface ActivationItemFactory {

    public ActivationItem createActivationItem(int intID, String licenseName, long datePurchased, long dateExpired,
            Status currentStatus);
    
    public ActivationItem createActivationItem(ActivationMemento memento) throws InvalidDataException;
    
}
