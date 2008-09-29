package org.limewire.core.impl.spam;

import org.limewire.core.api.search.SearchResult;
import org.limewire.core.api.spam.SpamManager;
import org.limewire.core.impl.search.MockSearchResult;

public class MockSpamManagerImpl implements SpamManager {

    @Override
    public void clearFilterData() {
        
    }

    @Override
    public void handleUserMarkedGood(SearchResult searchResult) {
        MockSearchResult result = (MockSearchResult)searchResult;
        result.setSpam(false);
    }

    @Override
    public void handleUserMarkedSpam(SearchResult searchResult) {
        MockSearchResult result = (MockSearchResult)searchResult;
        result.setSpam(true);
    }

}
