package org.limewire.mojito2.message;

import org.limewire.mojito2.routing.Contact;

public class AbstractResponse extends AbstractMessage 
        implements ResponseMessage {

    public AbstractResponse(MessageID messageId, Contact contact) {
        super(messageId, contact);
    }
}
