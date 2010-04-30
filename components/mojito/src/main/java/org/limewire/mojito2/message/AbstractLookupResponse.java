package org.limewire.mojito2.message;

import org.limewire.mojito2.routing.Contact;

public class AbstractLookupResponse extends AbstractResponse 
        implements LookupResponse {

    public AbstractLookupResponse(MessageID messageId, 
            Contact contact) {
        super(messageId, contact);
    }
}
