package org.limewire.ui.swing.search.model;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

import org.limewire.core.api.browse.Browse;
import org.limewire.core.api.browse.BrowseFactory;
import org.limewire.core.api.browse.BrowseListener;
import org.limewire.core.api.friend.FriendPresence;
import org.limewire.core.api.library.PresenceLibrary;
import org.limewire.core.api.library.RemoteFileItem;
import org.limewire.core.api.library.RemoteLibraryManager;
import org.limewire.core.api.search.SearchCategory;
import org.limewire.core.api.search.SearchListener;
import org.limewire.core.api.search.SearchResult;
import org.limewire.util.NotImplementedException;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;

public class SingleBrowseSearch extends AbstractBrowseSearch {
    private final CopyOnWriteArrayList<SearchListener> searchListeners = new CopyOnWriteArrayList<SearchListener>();
    
    private final FriendPresence friendPresence;
    private final RemoteLibraryManager remoteLibraryManager;
    private final BrowseFactory browseFactory;

    //TODO if start and stop are only called in the EDT, AtomicReference isn't necessary here
    private final AtomicReference<Browse> browse = new AtomicReference<Browse>();

    /**
     * @param friendPresence the person to be browsed. Null will show the
     *        results from all friends.
     */
    public SingleBrowseSearch(RemoteLibraryManager remoteLibraryManager, BrowseFactory browseFactory,
            FriendPresence friendPresence) {
        this.friendPresence = friendPresence;
        this.browseFactory = browseFactory;
        this.remoteLibraryManager = remoteLibraryManager;
    }

    @Override
    public void addSearchListener(SearchListener searchListener) {
        searchListeners.add(searchListener);
    }

    @Override
    public void removeSearchListener(SearchListener searchListener) {
        searchListeners.remove(searchListener);
    }

    @Override
    public SearchCategory getCategory() {
        return SearchCategory.ALL;
    }

    @Override
    public void repeat() {
        throw new NotImplementedException("BrowseSearch.repeat() not implemented");

    }

    @Override
    public void start() {
        for (SearchListener listener : searchListeners) {
            listener.searchStarted(SingleBrowseSearch.this);
        }
        
        if (isAnonymousBrowse()) {
            startAnonymousBrowse();
        } else {
            startFriendBrowse();
        }
    }

    @Override
    public void stop() {
        for (SearchListener listener : searchListeners) {
            listener.searchStopped(SingleBrowseSearch.this);
        }
        if (isAnonymousBrowse()) {
            stopAnonymousBrowse();
        }
    }

    private boolean isAnonymousBrowse() {
        return friendPresence != null && friendPresence.getFriend().isAnonymous();
    }

    private void startAnonymousBrowse() {
        browse.set(browseFactory.createBrowse(friendPresence));
        browse.get().start(new BrowseEventForwarder());
    }

    private void stopAnonymousBrowse() {
        assert (browse.get() != null);
        browse.get().stop();
    }

    private void startFriendBrowse() {
        // TODO: RemoteFileItems are going away. Need a new way to access a
        // snapshot of what is currently shared.
        EventList<RemoteFileItem> remoteFileItems = new BasicEventList<RemoteFileItem>();

        if (friendPresence != null) {
            PresenceLibrary presenceLibrary = remoteLibraryManager
                    .addPresenceLibrary(friendPresence);
            remoteFileItems.addAll(presenceLibrary.getModel());
        } else {
            remoteFileItems.addAll(remoteLibraryManager.getAllFriendsFileList().getModel());

        }

        //add all files
        for (RemoteFileItem item : remoteFileItems) {
            for (SearchListener listener : searchListeners) {
                listener.handleSearchResult(this, item.getSearchResult());
            }
        }
        
        //browse is finished
        for (SearchListener listener : searchListeners) {
            listener.searchStopped(SingleBrowseSearch.this);
        }
    }

    /**
     * Forwards browse information to searchListeners.
     */
    private class BrowseEventForwarder implements BrowseListener {

        @Override
        public void browseFinished(final boolean success) {
            for (SearchListener listener : searchListeners) {
                listener.searchStopped(SingleBrowseSearch.this);
            }
            
            for (BrowseStatusListener listener : browseStatusListeners) {
                listener.statusChanged();
            }
        }

        @Override
        public void handleBrowseResult(final SearchResult searchResult) {              
            for (SearchListener listener : searchListeners) {
                listener.handleSearchResult(SingleBrowseSearch.this, searchResult);
            }

        }
    }

}
