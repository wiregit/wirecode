package org.limewire.core.impl.search.store;

import org.limewire.core.api.search.SearchDetails;
import org.limewire.core.api.search.store.StoreListener;
import org.limewire.core.api.search.store.StoreManager;
import org.limewire.core.api.search.store.StoreStyle;

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
    public StoreStyle getStoreStyle() {
        // TODO implement
        return null;
    }

    @Override
    public boolean isLoggedIn() {
        // TODO implement
        return false;
    }

    @Override
    public void startSearch(SearchDetails searchDetails) {
        // TODO implement
    }
}
