package org.limewire.mojito.message;

import org.limewire.mojito.KUID;
import org.limewire.mojito.storage.ValueTuple;

/**
 * An interface for <tt>FIND_VALUE</tt> lookup response {@link Message}s.
 */
public interface ValueResponse extends LookupResponse {

    /**
     * Returns the request load of the found value.
     */
    public float getRequestLoad();
    
    /**
     * Returns the found {@link ValueTuple}s.
     */
    public ValueTuple[] getValues();
    
    /**
     * Returns the secondary {@link KUID}s.
     */
    public KUID[] getSecondaryKeys();
}
