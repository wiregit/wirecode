package org.limewire.core.api.search.store;

/**
 * Defines a listener for Lime Store search result events.
 */
public interface StoreSearchListener {

    /**
     * Invoked when store results are found.
     */
    void resultsFound(ReleaseResult[] releaseResults);
    
    /**
     * Invoked when the store style is updated to the specified style.
     */
    void styleUpdated(StoreStyle storeStyle);
}
