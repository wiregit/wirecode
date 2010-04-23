package org.limewire.mojito.message2;

import org.limewire.mojito.routing.Contact;

public class AbstractLookupResponse extends AbstractRequest 
        implements LookupRequest {

    public AbstractLookupResponse(MessageID messageId, 
            Contact contact) {
        super(messageId, contact);
    }
}
