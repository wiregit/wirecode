package org.limewire.mojito.message;

import org.limewire.mojito.KUID;
import org.limewire.mojito.routing.Contact;
import org.limewire.mojito.storage.ValueType;

public class DefaultValueRequest extends AbstractLookupRequest 
        implements ValueRequest {

    private final KUID[] secondaryKeys;
    
    private final ValueType valueType;
    
    public DefaultValueRequest(MessageID messageId, Contact contact, 
            KUID lookupId, KUID[] secondaryKeys, ValueType valueType) {
        super(messageId, contact, lookupId);
        
        this.secondaryKeys = secondaryKeys;
        this.valueType = valueType;
    }

    @Override
    public KUID[] getSecondaryKeys() {
        return secondaryKeys;
    }

    @Override
    public ValueType getValueType() {
        return valueType;
    }
}
