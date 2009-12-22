package org.limewire.core.api.search.store;

import java.net.URI;
import java.net.URISyntaxException;

import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.URN;

/**
 * Defines a track result for a Lime Store result.
 */
public interface TrackResult {

    /**
     * Returns the album identifier.
     */
    String getAlbumId();
    
    /**
     * Returns the file extension for the result.
     */
    String getFileExtension();
    
    /**
     * Returns the file name for the result.
     */
    String getFileName();
    
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
     * Returns the URI of the stream for the result.
     */
    URI getStreamURI() throws URISyntaxException;
    
    /**
     * Returns the URN for the result.
     */
    URN getUrn();
}
