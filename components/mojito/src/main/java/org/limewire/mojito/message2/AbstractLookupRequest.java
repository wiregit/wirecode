package org.limewire.mojito.message2;

import org.limewire.mojito.routing.Contact;

public class AbstractLookupRequest extends AbstractResponse 
        implements LookupResponse {

    public AbstractLookupRequest(MessageID messageId, 
            Contact contact) {
        super(messageId, contact);
    }
}
