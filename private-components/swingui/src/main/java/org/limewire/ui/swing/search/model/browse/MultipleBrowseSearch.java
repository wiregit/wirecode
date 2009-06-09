package org.limewire.ui.swing.search.model.browse;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.limewire.core.api.endpoint.RemoteHost;
import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.search.Search;
import org.limewire.core.api.search.SearchListener;
import org.limewire.core.api.search.SearchResult;
import org.limewire.core.api.search.sponsored.SponsoredResult;
import org.limewire.ui.swing.search.model.BrowseStatusListener;
import org.limewire.ui.swing.search.model.browse.BrowseStatus.BrowseState;

class MultipleBrowseSearch extends AbstractBrowseSearch {

    private final CombinedSearchListener combinedSearchListener = new CombinedSearchListener();
    private final CombinedBrowseStatusListener combinedBrowseStatusListener = new CombinedBrowseStatusListener();
    
    private List<BrowseSearch> browses;

    private final BrowseSearchFactory browseSearchFactory;

    /**
     * @param hosts the people to be browsed. Can not be null.
     */
    public MultipleBrowseSearch(BrowseSearchFactory browseSearchFactory,
            Collection<RemoteHost> hosts) {
        this.browseSearchFactory = browseSearchFactory;
        initialize(hosts);
    }

    private void initialize(Collection<RemoteHost> hosts){
        browses = new ArrayList<BrowseSearch>(hosts.size());
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
        private AtomicInteger stoppedBrowses = new AtomicInteger(0);

        @Override
        public void handleSearchResult(Search search, SearchResult searchResult) {
            for (SearchListener listener : searchListeners) {
                listener.handleSearchResult(MultipleBrowseSearch.this, searchResult);
            }
        }

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
        private List<Friend> failedList = new ArrayList<Friend>();
        private boolean hasUpdated;
        private boolean hasLoaded;
        
        @Override
        public void statusChanged(BrowseStatus status) {
            if(status.getState() == BrowseState.FAILED){
                failedList.addAll(status.getFailed());
            } else if(status.getState() == BrowseState.UPDATED){
                hasUpdated = true;
            } else if (status.getState() == BrowseState.LOADED){
                hasLoaded = true;
            }
            
           BrowseState state = getReleventMultipleBrowseState(status);
           
           if (state != null){
               BrowseStatus browseStatus = new BrowseStatus(MultipleBrowseSearch.this, state, failedList.toArray(new Friend[failedList.size()]));
               for (BrowseStatusListener listener : browseStatusListeners){
                   listener.statusChanged(browseStatus);
               }
           }
        }        
        
        public void clear(){
            hasUpdated = false;
            hasLoaded = false;
            failedList.clear();
        }
        
        /**
         * @return can be null
         */
        private BrowseState getReleventMultipleBrowseState(BrowseStatus status){
            if(failedList.size() == browses.size()){
                return BrowseState.FAILED;
            } else if(failedList.size() > 0){
                if(hasLoaded){
                    if(hasUpdated){
                        return BrowseState.UPDATED_PARTIAL_FAIL;
                    } else {
                        return BrowseState.PARTIAL_FAIL;
                    }
                }
            } else if (hasUpdated){
                return BrowseState.UPDATED;
            }
            return null;
        }
    }
    
}
