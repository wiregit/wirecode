package org.limewire.mojito.message;

import org.limewire.mojito.KUID;
import org.limewire.mojito.db.ValueType;

/**
 * An interface for <tt>FIND_VALUE</tt> lookup request {@link Message}s.
 */
public interface ValueRequest extends LookupRequest {

    /**
     * Returns the list of secondary {@link KUID}s we're looking for.
     */
    public KUID[] getSecondaryKeys();
    
    /**
     * Returns the {@link ValueType} of the value we're looking for.
     */
    public ValueType getValueType();
}
