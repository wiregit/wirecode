package org.limewire.mojito.entity;

import org.limewire.mojito.routing.Contact;
import org.limewire.security.SecurityToken;

/**
 * A {@link SecurityTokenEntity} is a special result of a 
 * <tt>FIND_NODE</tt> operation.
 */
public interface SecurityTokenEntity extends Entity {

    /**
     * Returns the {@link Contact} of the {@link SecurityToken}.
     */
    public Contact getContact();
    
    /**
     * Returns the {@link Contact}'s {@link SecurityToken}.
     */
    public SecurityToken getSecurityToken();
}
