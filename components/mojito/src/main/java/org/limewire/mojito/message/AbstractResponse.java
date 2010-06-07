package org.limewire.mojito.message;

import org.limewire.mojito.routing.Contact;

public class AbstractResponse extends AbstractMessage 
        implements ResponseMessage {

    public AbstractResponse(MessageID messageId, Contact contact) {
        super(messageId, contact);
    }
}
