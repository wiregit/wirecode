package org.limewire.core.impl.search;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.limewire.collection.glazedlists.GlazedListsFactory;
import org.limewire.core.api.URN;
import org.limewire.core.api.search.GroupedSearchResult;
import org.limewire.core.api.search.Search;
import org.limewire.core.api.search.SearchDetails;
import org.limewire.core.api.search.SearchListener;
import org.limewire.core.api.search.SearchResult;
import org.limewire.core.api.search.SearchResultList;
import org.limewire.core.api.search.sponsored.SponsoredResult;
import org.limewire.io.GUID;
import org.limewire.listener.EventListener;
import org.limewire.listener.EventListenerList;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.util.concurrent.Lock;

/**
 * Implementation of SearchResultList for the mock core.
 */
class MockSearchResultList implements SearchResultList {

    private final Search search;
    private final SearchDetails searchDetails;
    
    private final EventListenerList<Collection<GroupedSearchResult>> listListeners;
    private final Comparator<Object> resultFinder;
    private final SearchListener searchListener;
    
    private final EventList<GroupedSearchResult> groupedUrnResultList;
    private final EventList<GroupedSearchResult> threadSafeResultList;
    
    private volatile int resultCount;
    
    /**
     * Constructs a SearchResultList for the specified search.
     */
    public MockSearchResultList(Search search, SearchDetails searchDetails) {
        this.search = search;
        this.searchDetails = searchDetails;
        
        listListeners = new EventListenerList<Collection<GroupedSearchResult>>();
        resultFinder = new UrnResultFinder();
        searchListener = new SearchListenerImpl();
        
        // Create list of grouped results.
        groupedUrnResultList = new BasicEventList<GroupedSearchResult>();
        threadSafeResultList = GlazedListsFactory.threadSafeList(groupedUrnResultList);
        
        // Add search listener.
        search.addSearchListener(searchListener);
    }

    @Override
    public GroupedSearchResult getGroupedResult(URN urn) {
        return null;
    }

    @Override
    public EventList<GroupedSearchResult> getGroupedResults() {
        return threadSafeResultList;
    }

    @Override
    public GUID getGuid() {
        return null;
    }

    @Override
    public int getResultCount() {
        return resultCount;
    }

    @Override
    public Search getSearch() {
        return search;
    }

    @Override
    public String getSearchQuery() {
        return searchDetails.getSearchQuery();
    }

    @Override
    public void addListener(EventListener<Collection<GroupedSearchResult>> listener) {
        listListeners.addListener(listener);
    }

    @Override
    public boolean removeListener(EventListener<Collection<GroupedSearchResult>> listener) {
        return listListeners.removeListener(listener);
    }
    
    @Override
    public void clear() {
        Lock lock = groupedUrnResultList.getReadWriteLock().writeLock();
        lock.lock();
        try {
            groupedUrnResultList.clear();
            resultCount = 0;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void dispose() {
        search.removeSearchListener(searchListener);
    }

    void addResult(SearchResult result) {
        // Create list of new results.
        List<GroupedSearchResult> newResults = new ArrayList<GroupedSearchResult>();
        
        // Obtain write lock on result list.
        Lock lock = groupedUrnResultList.getReadWriteLock().writeLock();
        lock.lock();
        try {
            URN urn = result.getUrn();
            if (urn != null) {
                int idx = Collections.binarySearch(groupedUrnResultList, urn, resultFinder);
                if (idx >= 0) {
                    // Found URN so add result to grouping.
                    MockGroupedSearchResult gsr = (MockGroupedSearchResult) groupedUrnResultList.get(idx);
                    gsr.addNewSource(result, searchDetails.getSearchQuery());
                    groupedUrnResultList.set(idx, gsr);
                    newResults.add(gsr);

                } else {
                    // URN not found so add new result at insertion point.
                    idx = -(idx + 1);
                    GroupedSearchResult gsr = new MockGroupedSearchResult(result,
                            searchDetails.getSearchQuery());
                    groupedUrnResultList.add(idx, gsr);
                    newResults.add(gsr);
                }
                resultCount++;
            }
        } finally {
            // Release lock.
            lock.unlock();
        }
        
        // Forward added results to list listeners.
        if (newResults.size() > 0) {
            notifyResultsAdded(newResults);
        }
    }
    
    private void notifyResultsAdded(Collection<GroupedSearchResult> results) {
        listListeners.broadcast(results);
    }
    
    /**
     * Handler for search events to add results to the list.
     */
    private class SearchListenerImpl implements SearchListener {

        @Override
        public void handleSearchResult(Search search, SearchResult searchResult) {
            addResult(searchResult);
        }

        @Override
        public void handleSearchResults(Search search,
                Collection<? extends SearchResult> searchResults) {
        }

        @Override
        public void handleSponsoredResults(Search search, List<? extends SponsoredResult> sponsoredResults) {
        }

        @Override
        public void handleSpoonResult(URL url) {
        }

        @Override
        public void searchStarted(Search search) {
        }

        @Override
        public void searchStopped(Search search) {
        }
    }

    /**
     * Comparator to search for GroupedSearchResult objects by URN.
     */
    private static class UrnResultFinder implements Comparator<Object> {
        @Override
        public int compare(Object o1, Object o2) {
            return ((GroupedSearchResult) o1).getUrn().compareTo((URN) o2);
        }
    }
}
