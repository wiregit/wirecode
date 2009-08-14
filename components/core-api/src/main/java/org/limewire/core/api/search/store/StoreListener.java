package org.limewire.core.api.search.store;

/**
 * Defines a listener for Lime Store events.
 */
public interface StoreListener {

    /**
     * Invoked when the user logs in or out from the store.
     */
    void loginChanged(boolean loggedIn);
    
    /**
     * Invoked when store results are found.
     */
    void resultsFound(StoreResult[] storeResults);
    
    /**
     * Invoked when the store style is updated to the specified style.
     */
    void styleUpdated(StoreStyle storeStyle);
}
