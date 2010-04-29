package org.limewire.mojito2.message;

import org.limewire.mojito2.routing.Contact;

public class AbstractRequest extends AbstractMessage 
        implements RequestMessage {

    public AbstractRequest(MessageID messageId, Contact contact) {
        super(messageId, contact);
    }
}
