package org.limewire.mojito2.message;

import org.limewire.mojito2.KUID;
import org.limewire.mojito2.routing.Contact;

public class DefaultNodeRequest extends AbstractLookupRequest 
        implements NodeRequest {

    public DefaultNodeRequest(MessageID messageId, 
            Contact contact, KUID lookupId) {
        super(messageId, contact, lookupId);
    }
}
