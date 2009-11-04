package org.limewire.core.api.search.store;

import java.io.IOException;

import javax.swing.Icon;

/**
 * Defines a connection to a service that interacts with the Lime Store.
 */
public interface StoreConnection {

    /**
     * Performs a search using the specified query text, and returns the 
     * result as a JSON text string.
     */
    String doQuery(String query) throws IOException;
    
    /**
     * Retrieves an icon using the specified icon URI.
     */
    Icon loadIcon(String iconUri);
    
    /**
     * Retrieves the style for the specified style id, and returns the 
     * result as a JSON text string.
     */
    String loadStyle(String styleId) throws IOException;
    
    /**
     * Retrieves the tracks for the specified album id, and returns the 
     * result as a JSON text string.
     */
    String loadTracks(String albumId, int startTrackNumber) throws IOException;
}
