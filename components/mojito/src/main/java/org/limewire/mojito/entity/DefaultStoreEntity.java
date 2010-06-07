package org.limewire.mojito.entity;

import java.util.concurrent.TimeUnit;

import org.limewire.mojito.routing.Contact;

/**
 * The default implementation of {@link StoreEntity}.
 */
public class DefaultStoreEntity extends AbstractEntity implements StoreEntity {

    private final Contact[] contacts;
    
    public DefaultStoreEntity(Contact[] contacts, long time, TimeUnit unit) {
        super(time, unit);
        
        this.contacts = contacts;
    }

    @Override
    public Contact[] getContacts() {
        return contacts;
    }
}
