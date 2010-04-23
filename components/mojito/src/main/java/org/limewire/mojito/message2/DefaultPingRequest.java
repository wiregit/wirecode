package org.limewire.mojito.message2;

import org.limewire.mojito.routing.Contact;

public class DefaultPingRequest extends AbstractRequest 
        implements PingRequest {

    public DefaultPingRequest(MessageID messageId, Contact contact) {
        super(messageId, contact);
    }
}
