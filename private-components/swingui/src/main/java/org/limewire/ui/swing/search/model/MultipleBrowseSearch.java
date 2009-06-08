package org.limewire.ui.swing.search.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.limewire.core.api.browse.BrowseFactory;
import org.limewire.core.api.endpoint.RemoteHost;
import org.limewire.core.api.library.RemoteLibraryManager;
import org.limewire.core.api.search.Search;
import org.limewire.core.api.search.SearchListener;
import org.limewire.core.api.search.SearchResult;
import org.limewire.core.api.search.sponsored.SponsoredResult;

public class MultipleBrowseSearch extends AbstractBrowseSearch {
    
    private final CombinedSearchListener combinedSearchListener = new CombinedSearchListener();
    
    private final RemoteLibraryManager remoteLibraryManager;
    private final BrowseFactory browseFactory;
    private List<BrowseSearch> browses;
    /**
     * @param hosts the people to be browsed. Can not be null.
     */
    public MultipleBrowseSearch(RemoteLibraryManager remoteLibraryManager, BrowseFactory browseFactory,
            Collection<RemoteHost> hosts) {
        this.browseFactory = browseFactory;
        this.remoteLibraryManager = remoteLibraryManager;
        
        initialize(hosts);
    }
    
    private void initialize(Collection<RemoteHost> hosts){
        browses = new ArrayList<BrowseSearch>(hosts.size());
        for(RemoteHost host : hosts){
            BrowseSearch browseSearch = new SingleBrowseSearch(remoteLibraryManager, browseFactory, host.getFriendPresence());
            browseSearch.addSearchListener(combinedSearchListener);
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
