package org.limewire.mojito.message;

import org.limewire.mojito.routing.Contact;

/**
 * An abstract implementation of {@link RequestMessage}.
 */
public class AbstractRequest extends AbstractMessage 
        implements RequestMessage {

    public AbstractRequest(MessageID messageId, Contact contact) {
        super(messageId, contact);
    }
}
