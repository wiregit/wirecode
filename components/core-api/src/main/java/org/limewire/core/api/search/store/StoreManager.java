package org.limewire.core.api.search.store;

import org.limewire.core.api.search.SearchDetails;

/**
 * Defines a manager for Lime Store.
 */
public interface StoreManager {

    /**
     * Returns the URI text for the confirm download page.
     */
    String getConfirmURI();
    
    /**
     * Returns the URI text for the login page.
     */
    String getLoginURI();
    
    /**
     * Returns true if the specified store result can be downloaded without
     * further user prompts.
     */
    boolean isDownloadApproved(StoreResult storeResult);
    
    /**
     * Returns true if the specified track result can be downloaded without
     * further user prompts.
     */
    boolean isDownloadApproved(StoreTrackResult trackResult);
    
    /**
     * Returns true if the user is logged in to the store.
     */
    boolean isLoggedIn();
    
    /**
     * Starts search for store results using the specified search details and
     * store listener.
     */
    void startSearch(SearchDetails searchDetails, StoreListener storeListener);
}
