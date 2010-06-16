package com.limegroup.gnutella.spoon;

import java.net.URL;

/**
 * Passes Search queries to the Spoon Ad Server and handles 
 * the response from the server.
 */
public interface SpoonSearcher {

    /**
     * Passes a search query to the SpoonAdServer and handles the
     * response from the server.
     */
    public void search(String query, SpoonSearchCallback callback);
    
    /**
     * Callback from a Spoon Query. This is returned after the Spoon
     * Ad lookup has completed.
     */
    public interface SpoonSearchCallback {
        public void handle(URL url);
    }
}
