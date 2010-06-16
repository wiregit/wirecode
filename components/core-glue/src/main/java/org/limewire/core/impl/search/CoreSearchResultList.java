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
 * Implementation of SearchResultList for the live core.  CoreSearchResultList
 * handles search results from multiple background threads for Gnutella and 
 * friend results.
 */
class CoreSearchResultList implements SearchResultList {

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
    public CoreSearchResultList(Search search, SearchDetails searchDetails) {
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
    public GUID getGuid() {
        if (search instanceof CoreSearch) {
            return ((CoreSearch) search).getQueryGuid();
        } else {
            return null;
        }
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
    public GroupedSearchResult getGroupedResult(URN urn) {
        Lock lock = groupedUrnResultList.getReadWriteLock().writeLock();
        lock.lock();
        try {
            int idx = Collections.binarySearch(groupedUrnResultList, urn, resultFinder);
            if (idx >= 0) {
                return groupedUrnResultList.get(idx);
            } else {            
                return null;
            }
        } finally {
            lock.unlock();
        }
    }
    
    @Override
    public EventList<GroupedSearchResult> getGroupedResults() {
        return threadSafeResultList;
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
    
    /**
     * Adds the specified result to the list.
     */
    void addResult(SearchResult result) {
        addResultsInternal(Collections.singletonList(result));
    }
    
    /**
     * Adds the specified collection of results to the list.
     */
    void addResults(Collection<? extends SearchResult> results) {
        addResultsInternal(results);
    }
    
    /**
     * Adds the specified collection of results to the list of grouped results.
     * This method obtains a write lock on the list because results may be 
     * generated by different threads for Gnutella and friend results.
     */
    private void addResultsInternal(Collection<? extends SearchResult> results) {
        // Create list of new results.
        List<GroupedSearchResult> newResults = new ArrayList<GroupedSearchResult>();
        
        // Obtain write lock on result list.
        Lock lock = groupedUrnResultList.getReadWriteLock().writeLock();
        lock.lock();
        try {
            for (SearchResult result : results) {
                URN urn = result.getUrn();
                // Some results can be missing a URN, specifically secure results.
                // For now, we drop these.  We should figure out a way to show 
                // them later on.
                if (urn != null) {
                    int idx = Collections.binarySearch(groupedUrnResultList, urn, resultFinder);
                    if (idx >= 0) {
                        // Found URN so add result to grouping.
                        GroupedSearchResultImpl gsr = (GroupedSearchResultImpl) groupedUrnResultList.get(idx);
                        gsr.addNewSource(result, searchDetails.getSearchQuery());
                        groupedUrnResultList.set(idx, gsr);
                        newResults.add(gsr);

                    } else {
                        // URN not found so add new result at insertion point.
                        // This keeps the list in sorted order.
                        idx = -(idx + 1);
                        GroupedSearchResult gsr = new GroupedSearchResultImpl(result,
                                searchDetails.getSearchQuery());
                        groupedUrnResultList.add(idx, gsr);
                        newResults.add(gsr);
                    }
                    resultCount++;
                }
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
    
    /**
     * Forwards the specified collection of added results to all registered
     * listeners. 
     */
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
            addResults(searchResults);
        }

        @Override
        public void handleSponsoredResults(Search search, List<? extends SponsoredResult> sponsoredResults) {
        }

        @Override
        public void searchStarted(Search search) {
        }

        @Override
        public void searchStopped(Search search) {
        }

        @Override
        public void handleSpoonResult(URL url) {
        }
    }
    
    /**
     * Comparator to search for GroupedSearchResult objects by URN.  This is
     * only used to perform a binary search by URN, never to perform the sort,
     * so the compare() method does not need to be symmetric.
     */
    private static class UrnResultFinder implements Comparator<Object> {
        @Override
        public int compare(Object o1, Object o2) {
            return ((GroupedSearchResult) o1).getUrn().compareTo((URN) o2);
        }
    }
}
