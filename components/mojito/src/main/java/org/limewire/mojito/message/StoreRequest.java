package org.limewire.mojito.message;

import org.limewire.mojito.storage.DHTValueEntity;

public interface StoreRequest extends RequestMessage, SecurityTokenProvider {
    
    /**
     * The {@link DHTValueEntity}ies to store.
     */
    public DHTValueEntity[] getValueEntities();
}
