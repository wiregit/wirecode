package org.limewire.mojito.message;

import org.limewire.mojito.db.ValueTuple;
import org.limewire.mojito.routing.Contact;
import org.limewire.security.SecurityToken;

/**
 * The default implementation of a {@link StoreRequest}.
 */
public class DefaultStoreRequest extends AbstractRequest 
        implements StoreRequest {

    private final SecurityToken securityToken;
    
    private final ValueTuple[] values;
    
    public DefaultStoreRequest(MessageID messageId, Contact contact, 
            SecurityToken securityToken, ValueTuple[] values) {
        super(messageId, contact);
        
        this.securityToken = securityToken;
        this.values = values;
    }

    @Override
    public ValueTuple[] getValues() {
        return values;
    }

    @Override
    public SecurityToken getSecurityToken() {
        return securityToken;
    }
}
