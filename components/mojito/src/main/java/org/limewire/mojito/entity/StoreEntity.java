package org.limewire.mojito.entity;

import org.limewire.mojito.routing.Contact;
import org.limewire.mojito.storage.Value;

/**
 * A {@link StoreEntity} is the result of a DHT <tt>STORE</tt> operation.
 */
public interface StoreEntity extends Entity {

    /**
     * The {@link Contact}s where the {@link Value} was stored.
     */
    public Contact[] getContacts();
}
