package org.limewire.core.impl.search;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.limewire.collection.glazedlists.GlazedListsFactory;
import org.limewire.core.api.URN;
import org.limewire.core.api.endpoint.RemoteHost;
import org.limewire.core.api.search.GroupedSearchResult;
import org.limewire.core.api.search.GroupedSearchResultListener;
import org.limewire.core.api.search.Search;
import org.limewire.core.api.search.SearchDetails;
import org.limewire.core.api.search.SearchListener;
import org.limewire.core.api.search.SearchResult;
import org.limewire.core.api.search.SearchResultList;
import org.limewire.core.api.search.sponsored.SponsoredResult;
import org.limewire.friend.api.Friend;
import org.limewire.io.GUID;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;

/**
 * Implementation of SearchResultList for the mock core.
 */
class MockSearchResultList implements SearchResultList {

    private final Search search;
    private final SearchDetails searchDetails;
    
    private final Comparator<GroupedSearchResult> resultFinder;
    private final SearchListener searchListener;
    
    private final EventList<GroupedSearchResult> groupedUrnResultList;
    
    private volatile int resultCount;
    
    /**
     * Constructs a SearchResultList for the specified search.
     */
    public MockSearchResultList(Search search, SearchDetails searchDetails) {
        this.search = search;
        this.searchDetails = searchDetails;
        
        resultFinder = new UrnResultFinder();
        searchListener = new SearchListenerImpl();
        
        // Create list of grouped results.
        groupedUrnResultList = GlazedListsFactory.threadSafeList(
                GlazedListsFactory.sortedList(new BasicEventList<GroupedSearchResult>(), resultFinder));
        
        // Add search listener.
        search.addSearchListener(searchListener);
    }

    @Override
    public EventList<GroupedSearchResult> getGroupedResults() {
        return groupedUrnResultList;
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
    public void clear() {
        groupedUrnResultList.clear();
        resultCount = 0;
    }

    @Override
    public void dispose() {
        search.removeSearchListener(searchListener);
    }

    void addResult(SearchResult result) {
        URN urn = result.getUrn();
        if (urn != null) {
            int idx = Collections.binarySearch(groupedUrnResultList, new UrnResult(urn), resultFinder);
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
            }
            
            resultCount += result.getSources().size();
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
        public String getFileName() {
            return null;
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
        public List<SearchResult> getSearchResults() {
            return Collections.emptyList();
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

        @Override
        public void addResultListener(GroupedSearchResultListener listener) {
        }

        @Override
        public void removeResultListener(GroupedSearchResultListener listener) {
        }
    }
}
