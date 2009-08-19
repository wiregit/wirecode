package org.limewire.core.api.search.store;

import org.limewire.core.api.search.SearchDetails;

/**
 * Defines a manager for Lime Store.
 */
public interface StoreManager {

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
