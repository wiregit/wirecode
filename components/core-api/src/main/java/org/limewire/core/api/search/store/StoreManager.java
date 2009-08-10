package org.limewire.core.api.search.store;

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
     * Returns the current style for Lime Store results.
     */
    StoreStyle getStoreStyle();
    
    /**
     * Retrieves the style for Lime Store results from the server.
     */
    void loadStoreStyle();
}
