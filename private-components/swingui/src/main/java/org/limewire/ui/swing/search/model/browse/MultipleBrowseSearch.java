package org.limewire.ui.swing.search.model.browse;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.limewire.core.api.endpoint.RemoteHost;
import org.limewire.core.api.search.Search;
import org.limewire.core.api.search.SearchListener;
import org.limewire.core.api.search.SearchResult;
import org.limewire.core.api.search.sponsored.SponsoredResult;
import org.limewire.ui.swing.search.model.BrowseStatusListener;

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
    
    
    private class CombinedSearchListener implements SearchListener {
        private AtomicInteger stoppedBrowses = new AtomicInteger(0);

        @Override
        public void handleSearchResult(Search search, SearchResult searchResult) {
            for (SearchListener listener : searchListeners) {
                listener.handleSearchResult(MultipleBrowseSearch.this, searchResult);
            }
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

        @Override
        public void statusChanged() {
            // TODO handle the status changes
            
        }
        
    }
    
}
