package org.limewire.core.api.search.store;

import org.limewire.core.api.search.SearchDetails;

/**
 * Defines a manager for Lime Store.
 */
public interface StoreManager {

    /**
     * Adds the specified listener to the list that is notified on store 
     * events.
     */
    void addStoreListener(StoreListener listener);
    
    /**
     * Removes the specified listener from the list that is notified on store 
     * events.
     */
    void removeStoreListener(StoreListener listener);
    
    /**
     * Returns true if the user is logged in to the store.
     */
    boolean isLoggedIn();
    
    /**
     * Returns the current style for Lime Store results.
     */
    StoreStyle getStoreStyle();
    
    /**
     * Starts search for store results using specified search details.
     */
    void startSearch(SearchDetails searchDetails);
}
