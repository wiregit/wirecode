package org.limewire.core.api.search.store;

import java.net.URI;
import java.net.URISyntaxException;

import org.limewire.core.api.search.SearchDetails;

/**
 * Defines a manager for Lime Store.
 */
public interface StoreManager {
    /**
     * Keys for user attributes.
     */
    public enum AttributeKey {
        COOKIES;
    }
    
    /**
     * Returns the URI for the login page.
     */
    URI getLoginURI() throws URISyntaxException;
    
    /**
     * Validates the specified store result for download, and returns a token
     * indicating the approval status. 
     */
    StoreDownloadToken validateDownload(ReleaseResult releaseResult);
    
    /**
     * Validates the specified track result for download, and returns a token
     * indicating the approval status. 
     */
    StoreDownloadToken validateDownload(TrackResult trackResult) ;
    
    /**
     * Returns true if the user is logged in to the store.
     */
    boolean isLoggedIn();
    
    /**
     * Logs the user out of the store.
     * Fires <code>StoreAuthState</code> events
     */
    void logout();
    
    /**
     * Starts search for store results using the specified search details and
     * store search listener.
     */
    void startSearch(SearchDetails searchDetails, StoreSearchListener storeSearchListener);
}
