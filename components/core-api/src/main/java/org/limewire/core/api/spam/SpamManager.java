package org.limewire.core.api.spam;

import org.limewire.core.api.search.SearchResult;

public interface SpamManager {
    public void clearFilterData();
    public void handleUserMarkedSpam(SearchResult searchResult);
    public void handleUserMarkedGood(SearchResult searchResult);
}
