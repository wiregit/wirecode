package org.limewire.mojito.entity;

import org.limewire.mojito.db.Value;
import org.limewire.mojito.routing.Contact;

/**
 * A {@link StoreEntity} is the result of a DHT <tt>STORE</tt> operation.
 */
public interface StoreEntity extends Entity {

    /**
     * The {@link Contact}s where the {@link Value} was stored.
     */
    public Contact[] getContacts();
}
