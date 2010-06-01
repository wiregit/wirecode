package org.limewire.mojito.entity;

import org.limewire.mojito.ValueKey;
import org.limewire.mojito.storage.ValueTuple;

/**
 * A {@link ValueEntity} is the result of a DHT <tt>FIND_VALUE</tt> operation.
 */
public interface ValueEntity extends LookupEntity {

    /**
     * Returns the {@link ValueKey} that was used to retrieve 
     * the {@link ValueTuple}s.
     */
    public ValueKey getValueKey();
    
    /**
     * Returns the {@link ValueTuple}s.
     */
    public ValueTuple[] getValues();
    
    /**
     * Returns the {@link ValueKey}s that may be used to retrieve 
     * more values.
     */
    public ValueKey[] getValueKeys();
}
