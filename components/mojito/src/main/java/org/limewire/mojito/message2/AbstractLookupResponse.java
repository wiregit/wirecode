package org.limewire.mojito.message2;

import org.limewire.mojito.routing.Contact;

public class AbstractLookupResponse extends AbstractRequest 
        implements LookupResponse {

    public AbstractLookupResponse(MessageID messageId, 
            Contact contact) {
        super(messageId, contact);
    }
}
