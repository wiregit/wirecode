package org.limewire.mojito.message;

import org.limewire.mojito.db.ValueTuple;

/**
 * An interface for <tt>STORE</tt> request {@link Message}s.
 */
public interface StoreRequest extends RequestMessage, SecurityTokenProvider {
    
    /**
     * The {@link ValueTuple} to store.
     */
    public ValueTuple[] getValues();
}
