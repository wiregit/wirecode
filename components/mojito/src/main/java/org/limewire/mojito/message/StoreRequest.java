package org.limewire.mojito.message;

import org.limewire.mojito.storage.ValueTuple;

public interface StoreRequest extends RequestMessage, SecurityTokenProvider {
    
    /**
     * The {@link ValueTuple} to store.
     */
    public ValueTuple[] getValues();
}
