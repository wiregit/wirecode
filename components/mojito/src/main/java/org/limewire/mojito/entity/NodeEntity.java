package org.limewire.mojito.entity;

import java.util.Map.Entry;

import org.limewire.mojito.routing.Contact;
import org.limewire.security.SecurityToken;

/**
 * A {@link NodeEntity} is the result of a DHT <tt>FIND_NODE</tt> operation.
 */
public interface NodeEntity extends LookupEntity {
    
    /**
     * Returns all {@link Contact}s.
     */
    public Entry<Contact, SecurityToken>[] getContacts();
    
    /**
     * Returns the K-closest {@link Contact}s.
     */
    public Entry<Contact, SecurityToken>[] getClosest();
    
    /**
     * Returns {@link Contact}s that collide with the localhost.
     */
    public Contact[] getCollisions();
    
    /**
     * Returns the number of hops that were performed.
     */
    public int getHop();
}
