package org.limewire.activation.impl;

import java.util.Date;

import org.limewire.activation.api.ActivationItem;
import org.limewire.activation.api.ActivationItem.Status;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
class ActivationItemFactoryImpl implements ActivationItemFactory {
    
    @Inject
    public ActivationItemFactoryImpl() {
    }
    
    @Override
    public ActivationItem createActivationItem(int intID, String licenseName, Date datePurchased,
            Date dateExpired, Status currentStatus) {
        return new ActivationItemImpl(intID, licenseName, datePurchased, dateExpired, currentStatus);
    }
    
    @Override
    public ActivationItem createActivationItemFromDisk(int intID, String licenseName, Date datePurchased,
            Date dateExpired, Status currentStatus) {
        return new ActivationItemImpl(intID, licenseName, datePurchased, dateExpired, currentStatus, true);
    }
}
