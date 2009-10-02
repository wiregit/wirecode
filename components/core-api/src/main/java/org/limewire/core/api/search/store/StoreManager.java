package org.limewire.core.api.search.store;

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
     * Adds a listener to the list that is notified when store events occur.
     */
    void addStoreListener(StoreListener listener);
    
    /**
     * Removes a listener from the list that is notified when store events occur.
     */
    void removeStoreListener(StoreListener listener);
    
    /**
     * Returns the URI text for the login page.
     */
    String getLoginURI();
    
    /**
     * Validates the specified store result for download, and returns a token
     * indicating the approval status. 
     */
    StoreDownloadToken validateDownload(StoreResult storeResult);
    
    /**
     * Validates the specified track result for download, and returns a token
     * indicating the approval status. 
     */
    StoreDownloadToken validateDownload(TrackResult trackResult);
    
    /**
     * Returns true if the user is logged in to the store.
     */
    boolean isLoggedIn();
    
    /**
     * Returns the user attribute for the specified key.
     */
    Object getUserAttribute(AttributeKey key);
    
    /**
     * Sets the user attribute for the specified key.
     */
    void setUserAttribute(AttributeKey key, Object attribute);
    
    /**
     * Logs the user out of the store.
     */
    void logout();
    
    /**
     * Starts search for store results using the specified search details and
     * store search listener.
     */
    void startSearch(SearchDetails searchDetails, StoreSearchListener storeSearchListener);
}
