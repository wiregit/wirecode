package org.limewire.core.api.spam;

import java.util.List;

import org.limewire.core.api.search.SearchResult;

public interface SpamManager {
    public void clearFilterData();
    public void handleUserMarkedGood(List<SearchResult> searchResults);
    public void handleUserMarkedSpam(List<SearchResult> searchResults);
    
    /**
     * Reloads the IP Filter data & adjusts spam filters when ready.
     */
    public void reloadIPFilter();
}
