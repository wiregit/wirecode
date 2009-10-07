package org.limewire.core.api.search.store;

/**
 * Defines a connection to a service that interacts with the Lime Store.
 */
public interface StoreConnection {

    /**
     * Performs a search using the specified query text, and returns the 
     * result as a JSON text string.
     */
    String doQuery(String query);
    
    /**
     * Retrieves the style for the specified style id, and returns the 
     * result as a JSON text string.
     */
    String loadStyle(String styleId);
    
    /**
     * Retrieves the tracks for the specified album id, and returns the 
     * result as a JSON text string.
     */
    String loadTracks(String albumId);
}
