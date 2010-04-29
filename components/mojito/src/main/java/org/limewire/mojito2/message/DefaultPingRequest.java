package org.limewire.mojito2.message;

import org.limewire.mojito.routing.Contact;

public class DefaultPingRequest extends AbstractRequest 
        implements PingRequest {

    public DefaultPingRequest(MessageID messageId, Contact contact) {
        super(messageId, contact);
    }
}
