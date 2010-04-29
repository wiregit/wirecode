package org.limewire.mojito.message2;

import org.limewire.mojito.KUID;
import org.limewire.mojito.db.DHTValueType;
import org.limewire.mojito.routing.Contact;

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
