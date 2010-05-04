package org.limewire.mojito2.message;

import org.limewire.mojito2.KUID;
import org.limewire.mojito2.routing.Contact;
import org.limewire.mojito2.storage.DHTValueType;

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