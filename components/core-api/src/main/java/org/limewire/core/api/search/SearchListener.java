package org.limewire.core.api.search;

import java.util.Collection;
import java.util.List;

import org.limewire.core.api.search.sponsored.SponsoredResult;
import org.limewire.core.api.search.store.ReleaseResult;
import org.limewire.core.api.search.store.StoreStyle;

/** A listener for a search. */
public interface SearchListener {
    
    /** Notification a new search result is received for the search. */
    void handleSearchResult(Search search, SearchResult searchResult);
    
    /** Adds many search results at once. */
    void handleSearchResults(Search search, Collection<? extends SearchResult> searchResults);
    
    /** Notification the search has started. */
    void searchStarted(Search search);
    
    /** Notification the search has stopped. */
    void searchStopped(Search search);
    
    /** Notification that sponsored results have been received for the search. */
    void handleSponsoredResults(Search search, List<SponsoredResult> sponsoredResults);
    
    /** Notification when a new store result is received for the search. */
    void handleStoreResult(Search search, ReleaseResult releaseResult);
    
    /** Notification when the store style is updated for the search. */
    void handleStoreStyle(Search search, StoreStyle storeStyle);

}
