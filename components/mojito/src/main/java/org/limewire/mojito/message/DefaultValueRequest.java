package org.limewire.mojito.message;

import org.limewire.mojito.KUID;
import org.limewire.mojito.routing.Contact;
import org.limewire.mojito.storage.DHTValueType;

public class DefaultValueRequest extends AbstractLookupRequest 
        implements ValueRequest {

    private final KUID[] secondaryKeys;
    
    private final DHTValueType valueType;
    
    public DefaultValueRequest(MessageID messageId, Contact contact, 
            KUID lookupId, KUID[] secondaryKeys, DHTValueType valueType) {
        super(messageId, contact, lookupId);
        
        this.secondaryKeys = secondaryKeys;
        this.valueType = valueType;
    }

    @Override
    public KUID[] getSecondaryKeys() {
        return secondaryKeys;
    }

    @Override
    public DHTValueType getValueType() {
        return valueType;
    }
}
