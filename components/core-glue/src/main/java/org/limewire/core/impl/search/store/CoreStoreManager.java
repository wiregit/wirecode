package org.limewire.core.impl.search.store;

import org.limewire.core.api.search.SearchDetails;
import org.limewire.core.api.search.store.StoreListener;
import org.limewire.core.api.search.store.StoreManager;

import com.google.inject.Singleton;

/**
 * Implementation of StoreManager for the live core.
 */
@Singleton
public class CoreStoreManager implements StoreManager {

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
