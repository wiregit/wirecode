package org.limewire.mojito.message2;

import org.limewire.mojito.routing.Contact;

public class DefaultNodeResponse extends AbstractLookupResponse 
        implements NodeResponse {

    public DefaultNodeResponse(MessageID messageId, Contact contact) {
        super(messageId, contact);
    }
}
