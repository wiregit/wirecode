package org.limewire.mojito.message2;

import org.limewire.mojito.routing.Contact;

public class DefaultValueRequest extends AbstractLookupRequest 
        implements ValueRequest {

    public DefaultValueRequest(MessageID messageId, Contact contact) {
        super(messageId, contact);
    }
}
