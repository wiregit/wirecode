package org.limewire.core.api.search.store;

import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.URN;

/**
 * Defines a track result for a Lime Store result.
 */
public interface StoreTrackResult {

    /**
     * Returns the file extension for the result.
     */
    String getFileExtension();
    
    /**
     * Returns the price for the result.
     */
    String getPrice();
    
    /**
     * Returns a property value for the specified property key.
     */
    Object getProperty(FilePropertyKey key);
    
    /**
     * Returns the total file size in bytes.
     */
    long getSize();
    
    /**
     * Returns the URN for the result.
     */
    URN getUrn();
}