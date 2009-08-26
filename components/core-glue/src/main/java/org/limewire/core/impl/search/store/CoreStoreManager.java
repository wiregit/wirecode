package org.limewire.core.impl.search.store;

import org.limewire.core.api.search.SearchDetails;
import org.limewire.core.api.search.store.StoreListener;
import org.limewire.core.api.search.store.StoreManager;
import org.limewire.core.api.search.store.StoreResult;
import org.limewire.core.api.search.store.StoreTrackResult;

import com.google.inject.Singleton;

/**
 * Implementation of StoreManager for the live core.
 */
@Singleton
public class CoreStoreManager implements StoreManager {

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
    public void startSearch(SearchDetails searchDetails, StoreListener storeListener) {
        // TODO implement
    }
}
