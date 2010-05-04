package org.limewire.mojito2.entity;

import java.util.concurrent.TimeUnit;

import org.limewire.mojito2.routing.Contact;
import org.limewire.security.SecurityToken;

public class DefaultSecurityTokenEntity extends AbstractEntity 
        implements SecurityTokenEntity {

    private final Contact contact;
    
    private final SecurityToken securityToken;
    
    public DefaultSecurityTokenEntity(Contact contact, 
            SecurityToken securityToken, long time, TimeUnit unit) {
        super(time, unit);
        
        this.contact = contact;
        this.securityToken = securityToken;
    }

    @Override
    public Contact getContact() {
        return contact;
    }
    
    @Override
    public SecurityToken getSecurityToken() {
        return securityToken;
    }
}
