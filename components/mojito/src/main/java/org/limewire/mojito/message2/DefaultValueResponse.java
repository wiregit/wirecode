package org.limewire.mojito.message2;

import org.limewire.mojito.routing.Contact;

public class DefaultValueResponse extends AbstractLookupResponse 
        implements ValueResponse {

    public DefaultValueResponse(MessageID messageId, Contact contact) {
        super(messageId, contact);
    }
}
