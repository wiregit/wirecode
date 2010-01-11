package org.limewire.activation.impl;

import org.limewire.activation.api.ActivationItem;
import org.limewire.activation.api.ActivationItem.Status;
import org.limewire.activation.serial.ActivationMemento;

import com.google.inject.Singleton;

@Singleton
public class ActivationItemFactoryImpl implements ActivationItemFactory {

    @Override
    public ActivationItem createActivationItem(int intID, String licenseName, long datePurchased,
            long dateExpired, Status currentStatus) {
        return new ActivationItemImpl(intID, licenseName, datePurchased, dateExpired, currentStatus);
    }

    @Override
    public ActivationItem createActivationItem(ActivationMemento memento) {
        // TODO Auto-generated method stub
        return null;
    }

}
