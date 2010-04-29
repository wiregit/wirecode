package org.limewire.mojito2.message;

import org.limewire.mojito.KUID;
import org.limewire.mojito.db.DHTValueEntity;
import org.limewire.mojito2.routing.Contact;

public class DefaultValueResponse extends AbstractLookupResponse 
        implements ValueResponse {

    private final float requestLoad;
    
    private final KUID[] secondaryKeys;
    
    private final DHTValueEntity[] entities;
    
    public DefaultValueResponse(MessageID messageId, Contact contact,
            float requestLoad, KUID[] secondaryKeys, DHTValueEntity[] entities) {
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
    public DHTValueEntity[] getValueEntities() {
        return entities;
    }
}
