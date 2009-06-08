package org.limewire.ui.swing.search.model.browse;

import java.util.concurrent.atomic.AtomicReference;

import org.limewire.core.api.browse.Browse;
import org.limewire.core.api.browse.BrowseFactory;
import org.limewire.core.api.browse.BrowseListener;
import org.limewire.core.api.friend.FriendPresence;
import org.limewire.core.api.search.SearchListener;
import org.limewire.core.api.search.SearchResult;
import org.limewire.ui.swing.search.model.BrowseStatusListener;

class AnonymousSingleBrowseSearch extends AbstractBrowseSearch {
   
    private final FriendPresence friendPresence;
    private final BrowseFactory browseFactory;

    //TODO if start and stop are only called in the EDT, AtomicReference isn't necessary here
    private final AtomicReference<Browse> browse = new AtomicReference<Browse>();

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
        browse.set(browseFactory.createBrowse(friendPresence));
        browse.get().start(new BrowseEventForwarder());
    }

    private void stopAnonymousBrowse() {
        assert (browse.get() != null);
        browse.get().stop();
    }


    /**
     * Forwards browse information to searchListeners.
     */
    private class BrowseEventForwarder implements BrowseListener {

        @Override
        public void browseFinished(final boolean success) {
            for (SearchListener listener : searchListeners) {
                listener.searchStopped(AnonymousSingleBrowseSearch.this);
            }
            
            for (BrowseStatusListener listener : browseStatusListeners) {
                listener.statusChanged();
            }
        }

        @Override
        public void handleBrowseResult(final SearchResult searchResult) {              
            for (SearchListener listener : searchListeners) {
                listener.handleSearchResult(AnonymousSingleBrowseSearch.this, searchResult);
            }

        }
    }

}
