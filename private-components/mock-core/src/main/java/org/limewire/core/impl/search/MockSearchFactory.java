package org.limewire.core.impl.search;

import org.limewire.core.api.search.Search;
import org.limewire.core.api.search.SearchDetails;
import org.limewire.core.api.search.SearchFactory;
import org.limewire.core.api.search.store.StoreManager;

import com.google.inject.Inject;

/**
 * Implementation of SearchFactory for the mock core.
 */
public class MockSearchFactory implements SearchFactory {

    private final StoreManager storeManager;
    
    @Inject
    public MockSearchFactory(StoreManager storeManager) {
        this.storeManager = storeManager;
    }
    
    @Override
    public Search createSearch(SearchDetails searchDetails) {
        return new MockSearch(searchDetails, storeManager);
    }

}
