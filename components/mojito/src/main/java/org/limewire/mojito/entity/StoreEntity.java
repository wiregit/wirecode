package org.limewire.mojito.entity;

import org.limewire.mojito.routing.Contact;

public interface StoreEntity extends Entity {

    public Contact[] getContacts();
}
