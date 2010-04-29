package org.limewire.mojito2.message;

import org.limewire.mojito2.routing.Contact;
import org.limewire.security.SecurityToken;

public class DefaultNodeResponse extends AbstractLookupResponse 
        implements NodeResponse {

    private final SecurityToken securityToken;
    
    private final Contact[] contacts;
    
    public DefaultNodeResponse(MessageID messageId, Contact contact, 
            SecurityToken securityToken, Contact[] contacts) {
        super(messageId, contact);
        
        this.securityToken = securityToken;
        this.contacts = contacts;
    }

    @Override
    public SecurityToken getSecurityToken() {
        return securityToken;
    }
    
    @Override
    public Contact[] getContacts() {
        return contacts;
    }
}
