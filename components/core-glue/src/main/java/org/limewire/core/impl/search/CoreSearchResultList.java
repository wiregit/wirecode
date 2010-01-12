package org.limewire.core.impl.search;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.limewire.collection.glazedlists.GlazedListsFactory;
import org.limewire.core.api.URN;
import org.limewire.core.api.endpoint.RemoteHost;
import org.limewire.core.api.search.GroupedSearchResult;
import org.limewire.core.api.search.Search;
import org.limewire.core.api.search.SearchDetails;
import org.limewire.core.api.search.SearchListener;
import org.limewire.core.api.search.SearchResult;
import org.limewire.core.api.search.SearchResultList;
import org.limewire.core.api.search.SearchResultListListener;
import org.limewire.core.api.search.sponsored.SponsoredResult;
import org.limewire.friend.api.Friend;
import org.limewire.io.GUID;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;

/**
 * Implementation of SearchResultList for the live core.
 */
class CoreSearchResultList implements SearchResultList {

    private final Search search;
    private final SearchDetails searchDetails;
    
    private final List<SearchResultListListener> listListeners;
    private final Comparator<GroupedSearchResult> resultFinder;
    private final SearchListener searchListener;
    
    private final EventList<GroupedSearchResult> groupedUrnResultList;
    private final EventList<SearchResult> threadSafeResultList;
    
    private int resultCount;
    
    /**
     * Constructs a SearchResultList for the specified search.
     */
    public CoreSearchResultList(Search search, SearchDetails searchDetails) {
        this.search = search;
        this.searchDetails = searchDetails;
        
        listListeners = new CopyOnWriteArrayList<SearchResultListListener>();
        resultFinder = new UrnResultFinder();
        searchListener = new SearchListenerImpl();
        
        // Create list of results.
        threadSafeResultList = GlazedListsFactory.threadSafeList(new BasicEventList<SearchResult>());
        
        // Create list of grouped results.
        groupedUrnResultList = GlazedListsFactory.threadSafeList(
                GlazedListsFactory.sortedList(new BasicEventList<GroupedSearchResult>(), resultFinder));
        
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
    public EventList<SearchResult> getSearchResults() {
        return threadSafeResultList;
    }
    
    @Override
    public EventList<GroupedSearchResult> getGroupedResults() {
        return groupedUrnResultList;
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
    public void dispose() {
        search.removeSearchListener(searchListener);
        listListeners.clear();
    }
    
    /**
     * Adds the specified result to the list.
     */
    void addResult(SearchResult result) {
        addResults(Collections.singletonList(result));
    }
    
    /**
     * Adds the specified collection of results to the list.
     */
    void addResults(Collection<? extends SearchResult> results) {
        for (SearchResult result : results) {
            // Some results can be missing a URN, specifically secure results.
            // For now, we drop these.  We should figure out a way to show 
            // them later on.
            if (result.getUrn() != null) {
                threadSafeResultList.add(result);
            }
        }
        
        // Add results to grouped results.
        // TODO maybe create distinct Thread and use ListQueuer?
        addResultsInternal(results);
    }
    
    /**
     * Adds the specified collection of results to the list.
     */
    private void addResultsInternal(Collection<? extends SearchResult> results) {
        for (SearchResult result : results) {
            URN urn = result.getUrn();
            // Some results can be missing a URN, specifically secure results.
            // For now, we drop these.  We should figure out a way to show 
            // them later on.
            if (urn != null) {
                int idx = Collections.binarySearch(groupedUrnResultList, new UrnResult(urn), resultFinder);
                if (idx >= 0) {
                    // Found URN so add result to grouping.
                    GroupedSearchResultImpl gsr = (GroupedSearchResultImpl) groupedUrnResultList.get(idx);
                    gsr.addNewSource(result, searchDetails.getSearchQuery());
                    groupedUrnResultList.set(idx, gsr);
                    // Notify listeners that result changed.
                    for (SearchResultListListener listener : listListeners) {
                        listener.resultChanged(gsr, "new-sources", null, null);
                    }
                    
                } else {
                    // URN not found so add new result at insertion point.
                    idx = -(idx + 1);
                    GroupedSearchResult gsr = new GroupedSearchResultImpl(result,
                            searchDetails.getSearchQuery());
                    groupedUrnResultList.add(idx, gsr);
                    // Notify listeners that new result was created.
                    for (SearchResultListListener listener : listListeners) {
                        listener.resultCreated(gsr);
                    }
                }
                
                resultCount += result.getSources().size();
            }
        }
    }
    
    // TODO uncomment or delete
    /**
     * Task for aggregating search results and adding them to the list.
     */
//    private class ListQueuer implements Runnable {
//        private final Object LOCK = new Object();
//        private final List<SearchResult> queue = new ArrayList<SearchResult>();
//        private final ArrayList<SearchResult> transferQ = new ArrayList<SearchResult>();
//        private boolean scheduled = false;
//        
//        void add(SearchResult result) {
//            synchronized(LOCK) {
//                queue.add(result);
//                schedule();
//            }
//        }
//        
//        void addAll(Collection<? extends SearchResult> results) {
//            synchronized(LOCK) {
//                queue.addAll(results);
//                schedule();
//            }
//        }
//        
//        void clear() {
//            synchronized(LOCK) {
//                queue.clear();
//                SwingUtilities.invokeLater(new Runnable() {
//                    @Override
//                    public void run() {
////                        cleared = true;
//                        groupedUrnResultList.clear();
////                        for (SearchResultListListener listener : listListeners) {
////                            listener.resultsCleared();
////                        }
//                    }
//                });
//            }
//        }
//        
//        private void schedule() {
//            if(!scheduled) {
//                scheduled = true;
//                // purposely SwingUtilities & not SwingUtils so
//                // that we force it to the back of the stack
//                SwingUtilities.invokeLater(this);
//            }
//        }
//        
//        @Override
//        public void run() {
//            // move to transferQ inside lock
//            transferQ.clear();
//            synchronized(LOCK) {
//                transferQ.addAll(queue);
//                queue.clear();
//            }
//            
//            // move to searchResults outside lock,
//            // so we don't hold a lock while allSearchResults
//            // triggers events.
//            addResultsInternal(transferQ);
//            transferQ.clear();
//            
//            synchronized(LOCK) {
//                scheduled = false;
//                // If the queue wasn't empty, we need to reschedule
//                // ourselves, because something got added to the queue
//                // without scheduling itself, since we had scheduled=true
//                if(!queue.isEmpty()) {
//                    schedule();
//                }
//            }
//        }
//    }

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
     * Comparator to order GroupedSearchResult objects by URN.
     */
    private static class UrnResultFinder implements Comparator<GroupedSearchResult> {
        @Override
        public int compare(GroupedSearchResult o1, GroupedSearchResult o2) {
            return o1.getUrn().compareTo(o2.getUrn());
        }
    }
    
    /**
     * A wrapper result to compare URN values.
     */
    private static class UrnResult implements GroupedSearchResult {
        private final URN urn;
        
        public UrnResult(URN urn) {
            this.urn = urn;
        }

        @Override
        public List<SearchResult> getCoreSearchResults() {
            return Collections.emptyList();
        }

        @Override
        public Collection<Friend> getFriends() {
            return Collections.emptyList();
        }

        @Override
        public float getRelevance() {
            return 0;
        }

        @Override
        public Collection<RemoteHost> getSources() {
            return Collections.emptyList();
        }

        @Override
        public URN getUrn() {
            return urn;
        }

        @Override
        public boolean isAnonymous() {
            return true;
        }
    }
}
