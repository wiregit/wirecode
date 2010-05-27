package org.limewire.mojito.message;

import org.limewire.mojito.routing.Contact;

public class AbstractLookupResponse extends AbstractResponse 
        implements LookupResponse {

    public AbstractLookupResponse(MessageID messageId, 
            Contact contact) {
        super(messageId, contact);
    }
}
