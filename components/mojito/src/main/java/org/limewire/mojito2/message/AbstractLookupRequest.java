package org.limewire.mojito2.message;

import org.limewire.mojito2.KUID;
import org.limewire.mojito2.routing.Contact;

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
