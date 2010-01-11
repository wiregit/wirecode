package org.limewire.core.impl.search;

import java.util.Collection;
import java.util.List;

import org.limewire.collection.glazedlists.GlazedListsFactory;
import org.limewire.core.api.search.Search;
import org.limewire.core.api.search.SearchListener;
import org.limewire.core.api.search.SearchResult;
import org.limewire.core.api.search.SearchResultList;
import org.limewire.core.api.search.sponsored.SponsoredResult;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;

/**
 * Implementation of SearchResultList for the live core.
 */
class CoreSearchResultList implements SearchResultList {

    private final Search search;
    private final EventList<SearchResult> threadSafeResultList;
    private final SearchListener searchListener;
    
    /**
     * Constructs a SearchResultList for the specified search.
     */
    public CoreSearchResultList(Search search) {
        this.search = search;
        
        // Create list of results.
        this.threadSafeResultList = GlazedListsFactory.threadSafeList(new BasicEventList<SearchResult>());
        
        // Add search listener.
        searchListener = new SearchListenerImpl();
        search.addSearchListener(searchListener);
    }
    
    @Override
    public Search getSearch() {
        return search;
    }
    
    @Override
    public EventList<SearchResult> getSearchResults() {
        return threadSafeResultList;
    }

    @Override
    public void dispose() {
        search.removeSearchListener(searchListener);
    }
    
    /**
     * Handler for search events.
     */
    private class SearchListenerImpl implements SearchListener {

        @Override
        public void handleSearchResult(Search search, SearchResult searchResult) {
            // Some results can be missing a URN, specifically secure results.
            // For now, we drop these.  We should figure out a way to show 
            // them later on.
            if (searchResult.getUrn() == null) {
                return;
            }
            
            threadSafeResultList.add(searchResult);
        }

        @Override
        public void handleSearchResults(Search search,
                Collection<? extends SearchResult> searchResults) {
            for (SearchResult result : searchResults) {
                if (result.getUrn() != null) {
                    threadSafeResultList.add(result);
                }
            }
        }

        @Override
        public void handleSponsoredResults(Search search, List<SponsoredResult> sponsoredResults) {
        }

        @Override
        public void searchStarted(Search search) {
        }

        @Override
        public void searchStopped(Search search) {
        }
    }
    
    //TODO just stubbing this out here for tests, delete later when its coded above for real
    public void addSearchResult(SearchResult result) {
        threadSafeResultList.add(result);
    }
}
