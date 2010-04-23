package org.limewire.mojito.message2;

import org.limewire.mojito.routing.Contact;

public class DefaultStoreResponse extends AbstractResponse 
        implements StoreResponse {

    public DefaultStoreResponse(MessageID messageId, Contact contact) {
        super(messageId, contact);
    }
}
