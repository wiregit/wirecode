package org.limewire.mojito.entity;

import org.limewire.mojito.routing.Contact;
import org.limewire.security.SecurityToken;

/**
 * 
 */
public interface SecurityTokenEntity extends Entity {

    /**
     * 
     */
    public Contact getContact();
    
    /**
     * 
     */
    public SecurityToken getSecurityToken();
}
