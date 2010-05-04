package org.limewire.mojito2.entity;

import org.limewire.mojito2.routing.Contact;
import org.limewire.security.SecurityToken;

public interface SecurityTokenEntity extends Entity {

    public Contact getContact();
    
    public SecurityToken getSecurityToken();
}
