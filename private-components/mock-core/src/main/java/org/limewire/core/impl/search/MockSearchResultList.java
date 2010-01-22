package org.limewire.core.impl.search;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.limewire.collection.glazedlists.GlazedListsFactory;
import org.limewire.core.api.URN;
import org.limewire.core.api.search.GroupedSearchResult;
import org.limewire.core.api.search.Search;
import org.limewire.core.api.search.SearchDetails;
import org.limewire.core.api.search.SearchListener;
import org.limewire.core.api.search.SearchResult;
import org.limewire.core.api.search.SearchResultList;
import org.limewire.core.api.search.SearchResultListListener;
import org.limewire.core.api.search.sponsored.SponsoredResult;
import org.limewire.io.GUID;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.util.concurrent.Lock;

/**
 * Implementation of SearchResultList for the mock core.
 */
class MockSearchResultList implements SearchResultList {

    private final Search search;
    private final SearchDetails searchDetails;
    
    private final List<SearchResultListListener> listListeners;
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
        
        listListeners = new CopyOnWriteArrayList<SearchResultListListener>();
        resultFinder = new UrnResultFinder();
        searchListener = new SearchListenerImpl();
        
        // Create list of grouped results.
        groupedUrnResultList = new BasicEventList<GroupedSearchResult>();
        threadSafeResultList = GlazedListsFactory.threadSafeList(groupedUrnResultList);
        
        // Add search listener.
        search.addSearchListener(searchListener);
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
    public void addListListener(SearchResultListListener listener) {
        listListeners.add(listener);
    }

    @Override
    public void removeListListener(SearchResultListListener listener) {
        listListeners.remove(listener);
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
                    GroupedSearchResultImpl gsr = (GroupedSearchResultImpl) groupedUrnResultList.get(idx);
                    gsr.addNewSource(result, searchDetails.getSearchQuery());
                    groupedUrnResultList.set(idx, gsr);
                    // Notify listeners that result changed.
                    gsr.notifyNewSource();

                } else {
                    // URN not found so add new result at insertion point.
                    idx = -(idx + 1);
                    GroupedSearchResult gsr = new GroupedSearchResultImpl(result,
                            searchDetails.getSearchQuery());
                    groupedUrnResultList.add(idx, gsr);
                    newResults.add(gsr);
                }

                resultCount += result.getSources().size();
            }
        } finally {
            // Release lock.
            lock.unlock();
        }
        
        // Forward new results to list listeners.
        notifyResultsCreated(newResults);
    }
    
    private void notifyResultsCreated(Collection<GroupedSearchResult> results) {
        for (SearchResultListListener listener : listListeners) {
            listener.resultsCreated(results);
        }
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
        public void handleSponsoredResults(Search search, List<SponsoredResult> sponsoredResults) {
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
