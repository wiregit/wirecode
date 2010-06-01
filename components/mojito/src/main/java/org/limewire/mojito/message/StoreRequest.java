package org.limewire.mojito.message;

import org.limewire.mojito.storage.ValueTuple;

public interface StoreRequest extends RequestMessage, SecurityTokenProvider {
    
    /**
     * The {@link ValueTuple}ies to store.
     */
    public ValueTuple[] getValueEntities();
}
