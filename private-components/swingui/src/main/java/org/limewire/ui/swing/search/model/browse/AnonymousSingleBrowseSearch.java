package org.limewire.ui.swing.search.model.browse;

import org.limewire.core.api.browse.Browse;
import org.limewire.core.api.browse.BrowseFactory;
import org.limewire.core.api.browse.BrowseListener;
import org.limewire.core.api.friend.FriendPresence;
import org.limewire.core.api.search.SearchListener;
import org.limewire.core.api.search.SearchResult;
import org.limewire.ui.swing.search.model.BrowseStatusListener;
import org.limewire.ui.swing.search.model.browse.BrowseStatus.BrowseState;

class AnonymousSingleBrowseSearch extends AbstractBrowseSearch {
   
    private final FriendPresence friendPresence;
    private final BrowseFactory browseFactory;

    private Browse browse;

    /**
     * @param friendPresence the person to be browsed - must be anonymous and can not be null; 
     */
    public AnonymousSingleBrowseSearch(BrowseFactory browseFactory, FriendPresence friendPresence) {
        assert(friendPresence != null && friendPresence.getFriend().isAnonymous());
        this.friendPresence = friendPresence;
        this.browseFactory = browseFactory;
    }
   

    @Override
    public void start() {
        for (SearchListener listener : searchListeners) {
            listener.searchStarted(AnonymousSingleBrowseSearch.this);
        }

        startAnonymousBrowse();
    }

    @Override
    public void stop() {
        for (SearchListener listener : searchListeners) {
            listener.searchStopped(AnonymousSingleBrowseSearch.this);
        }
        stopAnonymousBrowse();
    }


    private void startAnonymousBrowse() {
        browse = browseFactory.createBrowse(friendPresence);
        browse.start(new BrowseEventForwarder());
    }

    private void stopAnonymousBrowse() {
        assert (browse != null);
        browse.stop();
    }


    /**
     * Forwards browse information to searchListeners.
     */
    private class BrowseEventForwarder implements BrowseListener {

        @Override
        public void browseFinished(final boolean success) {
            BrowseStatus status = success? 
                    new BrowseStatus(AnonymousSingleBrowseSearch.this, BrowseState.LOADED): 
                    new BrowseStatus(AnonymousSingleBrowseSearch.this, BrowseState.FAILED, friendPresence.getFriend());
            
            for (SearchListener listener : searchListeners) {
                listener.searchStopped(AnonymousSingleBrowseSearch.this);
            }
           
            for (BrowseStatusListener listener : browseStatusListeners) {
                listener.statusChanged(status);
            }
        }

        @Override
        public void handleBrowseResult(final SearchResult searchResult) {              
            for (SearchListener listener : searchListeners) {
                listener.handleSearchResult(AnonymousSingleBrowseSearch.this, searchResult);
            }

        }
    }


    @Override
    public void repeat() {
        stop();
        start();
    }

}
