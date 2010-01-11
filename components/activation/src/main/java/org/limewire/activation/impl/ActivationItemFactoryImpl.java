package org.limewire.activation.impl;

import org.limewire.activation.api.ActivationItem;
import org.limewire.activation.api.ActivationItem.Status;
import org.limewire.activation.serial.ActivationMemento;
import org.limewire.io.InvalidDataException;

import com.google.inject.Singleton;

@Singleton
public class ActivationItemFactoryImpl implements ActivationItemFactory {

    @Override
    public ActivationItem createActivationItem(int intID, String licenseName, long datePurchased,
            long dateExpired, Status currentStatus) {
        return new ActivationItemImpl(intID, licenseName, datePurchased, dateExpired, currentStatus);
    }

    @Override
    public ActivationItem createActivationItem(ActivationMemento memento) throws InvalidDataException {
        try {
            if(memento.getDateExpired() == -1)
                throw new InvalidDataException("must have date expired");
            if(memento.getDatePurchased() == -1)
                throw new InvalidDataException("must have date purchased");
            if(memento.getID() == -1)
                throw new InvalidDataException("must have id");
            
            return new ActivationItemImpl(memento.getID(), memento.getLicenseName(), memento.getDatePurchased(),
                    memento.getDateExpired(), memento.getStatus());
        } catch (Throwable t) {
            throw new InvalidDataException("invalid memento!", t);
        }
    }
}
