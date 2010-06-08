package org.limewire.mojito.message;

import org.limewire.mojito.routing.Contact;

/**
 * The default implementation of a {@link StoreResponse}.
 */
public class DefaultStoreResponse extends AbstractResponse 
        implements StoreResponse {

    private final StoreStatusCode[] codes;
    
    public DefaultStoreResponse(MessageID messageId, 
            Contact contact, StoreStatusCode[] codes) {
        super(messageId, contact);
        
        this.codes = codes;
    }

    @Override
    public StoreStatusCode[] getStoreStatusCodes() {
        return codes;
    }
}
