package org.limewire.mojito2.entity;

import org.limewire.mojito2.routing.Contact;

public interface StoreEntity extends Entity {

    public Contact[] getContacts();
}
