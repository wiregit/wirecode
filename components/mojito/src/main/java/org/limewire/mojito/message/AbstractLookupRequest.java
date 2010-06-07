package org.limewire.mojito.message;

import org.limewire.mojito.KUID;
import org.limewire.mojito.routing.Contact;

public abstract class AbstractLookupRequest extends AbstractResponse 
        implements LookupRequest {

    private final KUID lookupId;
    
    public AbstractLookupRequest(MessageID messageId, 
            Contact contact, KUID lookupId) {
        super(messageId, contact);
        
        this.lookupId = lookupId;
    }

    @Override
    public KUID getLookupId() {
        return lookupId;
    }
}
