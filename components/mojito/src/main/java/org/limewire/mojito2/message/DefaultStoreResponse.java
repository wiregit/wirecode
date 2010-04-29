package org.limewire.mojito2.message;

import org.limewire.mojito.routing.Contact;

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
