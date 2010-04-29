package org.limewire.mojito.message2;

import org.limewire.mojito.KUID;
import org.limewire.mojito.routing.Contact;

public class DefaultNodeRequest extends AbstractLookupRequest 
        implements NodeRequest {

    public DefaultNodeRequest(MessageID messageId, 
            Contact contact, KUID lookupId) {
        super(messageId, contact, lookupId);
    }
}
