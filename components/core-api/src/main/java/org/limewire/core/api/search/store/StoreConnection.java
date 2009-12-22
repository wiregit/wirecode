package org.limewire.core.api.search.store;

import java.io.IOException;
import java.net.MalformedURLException;

import javax.swing.Icon;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Defines a connection to a service that interacts with the Lime Store.
 * All methods make network calls and block.
 */
public interface StoreConnection {
    
    void logout();

    /**
     * Performs a search using the specified query text, and returns the 
     * result as a JSON text string.
     */
    JSONObject doQuery(String query) throws IOException, JSONException;
    
    /**
     * Retrieves an icon using the specified icon URI.
     */
    Icon loadIcon(String iconUri) throws MalformedURLException;
    
    /**
     * Retrieves the style for the specified style id, and returns the 
     * result as a JSON text string.
     */
    JSONObject loadStyle(String styleId) throws IOException, JSONException;
    
    /**
     * Retrieves the tracks for the specified album id, and returns the 
     * result as a JSON text string.
     */
    JSONObject loadTracks(String albumId, int startTrackNumber) throws IOException, JSONException;
}
