package org.limewire.mojito2.message;

import org.limewire.mojito.db.DHTValueEntity;

public interface StoreRequest extends RequestMessage, SecurityTokenProvider {
    
    /**
     * The {@link DHTValueEntity}ies to store.
     */
    public DHTValueEntity[] getValueEntities();
}
