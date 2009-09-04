package org.limewire.core.impl.search.store;

import org.limewire.core.api.search.SearchDetails;
import org.limewire.core.api.search.store.StoreListener;
import org.limewire.core.api.search.store.StoreManager;
import org.limewire.core.api.search.store.StoreResult;
import org.limewire.core.api.search.store.StoreSearchListener;
import org.limewire.core.api.search.store.StoreTrackResult;

import com.google.inject.Singleton;

/**
 * Implementation of StoreManager for the live core.
 */
@Singleton
public class CoreStoreManager implements StoreManager {

    @Override
    public void addStoreListener(StoreListener listener) {
        // TODO implement
    }

    @Override
    public void removeStoreListener(StoreListener listener) {
        // TODO implement
    }
    
    @Override
    public String getConfirmURI() {
        // TODO implement
        return null;
    }

    @Override
    public String getLoginURI() {
        // TODO implement
        return null;
    }

    @Override
    public boolean isDownloadApproved(StoreResult storeResult) {
        // TODO implement
        return false;
    }

    @Override
    public boolean isDownloadApproved(StoreTrackResult trackResult) {
        // TODO implement
        return false;
    }
    
    @Override
    public boolean isLoggedIn() {
        // TODO implement
        return false;
    }

    @Override
    public Object getUserAttribute(AttributeKey key) {
        // TODO implement
        return null;
    }

    @Override
    public void setUserAttribute(AttributeKey key, Object attribute) {
        // TODO implement
    }

    @Override
    public void logout() {
        // TODO implement
    }

    @Override
    public void startSearch(SearchDetails searchDetails, StoreSearchListener storeSearchListener) {
        // TODO implement
    }
}
