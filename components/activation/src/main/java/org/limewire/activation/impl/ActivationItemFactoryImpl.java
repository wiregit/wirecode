package org.limewire.activation.impl;

import java.util.Date;

import org.limewire.activation.api.ActivationItem;
import org.limewire.activation.api.ActivationItem.Status;

import com.google.inject.Singleton;

@Singleton
public class ActivationItemFactoryImpl implements ActivationItemFactory {

    @Override
    public ActivationItem createActivationItem(int intID, String licenseName, Date datePurchased,
            Date dateExpired, Status currentStatus) {
        return new ActivationItemImpl(intID, licenseName, datePurchased, dateExpired, currentStatus);
    }
}
