package org.limewire.core.impl.search.browse;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.limewire.core.api.endpoint.RemoteHost;
import org.limewire.core.api.search.Search;
import org.limewire.core.api.search.SearchListener;
import org.limewire.core.api.search.SearchResult;
import org.limewire.core.api.search.browse.BrowseSearch;
import org.limewire.core.api.search.browse.BrowseSearchFactory;
import org.limewire.core.api.search.browse.BrowseStatus;
import org.limewire.core.api.search.browse.BrowseStatusListener;
import org.limewire.core.api.search.browse.BrowseStatus.BrowseState;
import org.limewire.core.api.search.sponsored.SponsoredResult;
import org.limewire.friend.api.Friend;

/**
 * Aggregates multiple AnonymousSingleBrowseSearches and FriendSingleBrowseSearches.
 *
 */
class MultipleBrowseSearch extends AbstractBrowseSearch {

    private final CombinedSearchListener combinedSearchListener = new CombinedSearchListener();
    private final CombinedBrowseStatusListener combinedBrowseStatusListener = new CombinedBrowseStatusListener();
    
    private final List<BrowseSearch> browses = new CopyOnWriteArrayList<BrowseSearch>();

    private final BrowseSearchFactory browseSearchFactory;

    /**
     * @param hosts the people to be browsed. Can not be null.
     */
    public MultipleBrowseSearch(BrowseSearchFactory browseSearchFactory,
            Collection<RemoteHost> hosts) {
        this.browseSearchFactory = browseSearchFactory;
        initialize(hosts);
    }
    
    /**Creates a BrowseSearch for each of the hosts, adds the necessary listeners to it, 
     * and adds it to the list of BrowseSearches.*/
    private void initialize(Collection<RemoteHost> hosts){
        for(RemoteHost host : hosts){
            BrowseSearch browseSearch = browseSearchFactory.createBrowseSearch(host);
            browseSearch.addSearchListener(combinedSearchListener);
            browseSearch.addBrowseStatusListener(combinedBrowseStatusListener);
            browses.add(browseSearch);
        }
    }
 
    @Override
    public void start() {
        for (BrowseSearch browse: browses){
            browse.start();
        }
    }

    @Override
    public void stop() {
        for (BrowseSearch browse: browses){
            browse.stop();
        }
    }
    

    @Override
    public void repeat() {        
        combinedBrowseStatusListener.clear();
        combinedSearchListener.clear();
        
        for (BrowseSearch browseSearch : browses){
            browseSearch.repeat();
        }
    }
    
    private class CombinedSearchListener implements SearchListener {
        /**Keeps count of how many browses have completed*/
        private AtomicInteger stoppedBrowses = new AtomicInteger(0);

        @Override
        public void handleSearchResult(Search search, SearchResult searchResult) {
            for (SearchListener listener : searchListeners) {
                listener.handleSearchResult(MultipleBrowseSearch.this, searchResult);
            }
        }
        
        /**Clears the count of completed browses*/
        public void clear() {
            stoppedBrowses.set(0);
        }

        @Override
        public void handleSponsoredResults(Search search, List<SponsoredResult> sponsoredResults) {
            for (SearchListener listener : searchListeners) {
                listener.handleSponsoredResults(MultipleBrowseSearch.this, sponsoredResults);
            }
        }

        @Override
        public void searchStarted(Search search) {
            for (SearchListener listener : searchListeners) {
                listener.searchStarted(MultipleBrowseSearch.this);
            }
        }

        @Override
        public void searchStopped(Search search) {
            if (stoppedBrowses.incrementAndGet() == browses.size()) {
                //all of our browses have completed
                for (SearchListener listener : searchListeners) {
                    listener.searchStopped(MultipleBrowseSearch.this);
                }
            }
        }        
    }
    
    private class CombinedBrowseStatusListener implements BrowseStatusListener {
        /**List of all failed browses (Friends includes anonymous*/
        private List<Friend> failedList = new CopyOnWriteArrayList<Friend>();
        /**The number of BrowseSearches in the LOADED state*/
        private AtomicInteger loaded = new AtomicInteger(0);
        /**Whether or not there are updates in any of the browses*/
        private AtomicBoolean hasUpdated = new AtomicBoolean(false);
        
        @Override
        public void statusChanged(BrowseStatus status) {
            if(status.getState() == BrowseState.FAILED || status.getState() == BrowseState.OFFLINE){
                //getFailedFriends() will only return 1 person 
                //since status is from a single browse
                failedList.addAll(status.getFailedFriends());
            } else if(status.getState() == BrowseState.UPDATED){
                hasUpdated.set(true);
            } else if (status.getState() == BrowseState.LOADED){
                loaded.incrementAndGet();
            }
            
           BrowseState state = getReleventMultipleBrowseState(status);
           
           if (state != null){
               BrowseStatus browseStatus = new BrowseStatus(MultipleBrowseSearch.this, state, failedList.toArray(new Friend[failedList.size()]));
               for (BrowseStatusListener listener : browseStatusListeners){
                   listener.statusChanged(browseStatus);
               }
           }
        }        
        
        /**
         * Clears all cached data about the browses
         */
        public void clear(){
            hasUpdated.set(false);
            loaded.set(0);
            failedList.clear();
        }
        
        /**
         * @return The aggregated status of the browses.  For example if the browses are a mix of 
         * FAILED and LOADED, the status will be PARTIAL_FAIL.  If the browses contain, FAILED, 
         * LOADED, and UPDATED, it will be UPDATED_PARTIAL_FAIL.
         */
        private BrowseState getReleventMultipleBrowseState(BrowseStatus status){
            if(loaded.get() == browses.size()){
                return BrowseState.LOADED;
            } else if(failedList.size() == browses.size()){
                return BrowseState.FAILED;
            } else if(failedList.size() > 0){
                if(loaded.get() > 0){
                    if(hasUpdated.get()){
                        return BrowseState.UPDATED_PARTIAL_FAIL;
                    } else {
                        return BrowseState.PARTIAL_FAIL;
                    }
                }
            } else if (hasUpdated.get()){
                return BrowseState.UPDATED;
            }
            return BrowseState.LOADING;
        }
    }
    
}
