package com.limegroup.gnutella.spoon;

import java.net.URL;

import org.limewire.core.api.search.SearchDetails;

/**
 * Passes Search queries to the Spoon Ad Server and handles 
 * the response from the server.
 */
public interface SpoonSearcher {

    /**
     * Passes a search query to the SpoonAdServer and handles the
     * response from the server.
     */
    public void search(SearchDetails searchDetails, SpoonSearchCallback callback);
    
    /**
     * Callback from a Spoon Query. This is returned after the Spoon
     * Ad lookup has completed.
     */
    public interface SpoonSearchCallback {
        public void handle(URL url);
    }
}
