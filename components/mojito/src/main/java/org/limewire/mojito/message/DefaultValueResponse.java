package org.limewire.mojito.message;

import org.limewire.mojito.KUID;
import org.limewire.mojito.db.ValueTuple;
import org.limewire.mojito.routing.Contact;

/**
 * The default implementation of a {@link ValueResponse}.
 */
public class DefaultValueResponse extends AbstractLookupResponse 
        implements ValueResponse {

    private final float requestLoad;
    
    private final KUID[] secondaryKeys;
    
    private final ValueTuple[] entities;
    
    public DefaultValueResponse(MessageID messageId, Contact contact,
            float requestLoad, KUID[] secondaryKeys, ValueTuple[] entities) {
        super(messageId, contact);
        
        this.requestLoad = requestLoad;
        this.secondaryKeys = secondaryKeys;
        this.entities = entities;
    }

    @Override
    public float getRequestLoad() {
        return requestLoad;
    }

    @Override
    public KUID[] getSecondaryKeys() {
        return secondaryKeys;
    }

    @Override
    public ValueTuple[] getValues() {
        return entities;
    }
}
