package org.limewire.ui.swing.search.model.browse;

import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.library.FriendLibrary;
import org.limewire.core.api.library.RemoteFileItem;
import org.limewire.core.api.library.RemoteLibraryManager;
import org.limewire.core.api.search.SearchListener;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;

class FriendSingleBrowseSearch extends AbstractBrowseSearch {
    
    private final Friend friend;
    private final RemoteLibraryManager remoteLibraryManager;

    /**
     * @param friend the person to be browsed - can not be anonymous. Null will show the
     *        results from all friends.
     */
    public FriendSingleBrowseSearch(RemoteLibraryManager remoteLibraryManager, Friend friend) {
        assert(friend == null || !friend.isAnonymous());
        this.friend = friend;
        this.remoteLibraryManager = remoteLibraryManager;
    }


    @Override
    public void start() {
        for (SearchListener listener : searchListeners) {
            listener.searchStarted(FriendSingleBrowseSearch.this);
        }

        startFriendBrowse();
    }

    @Override
    public void stop() {
        for (SearchListener listener : searchListeners) {
            listener.searchStopped(FriendSingleBrowseSearch.this);
        }       
    }


    private void startFriendBrowse() {
        // TODO: RemoteFileItems are going away. Need a new way to access a
        // snapshot of what is currently shared.
        EventList<RemoteFileItem> remoteFileItems = new BasicEventList<RemoteFileItem>();

        if (friend != null) {
            FriendLibrary library = remoteLibraryManager.getFriendLibrary(friend);
            if (library != null) {
                System.out.println(remoteLibraryManager.getFriendLibrary(friend).getSwingModel());
                remoteFileItems.addAll(remoteLibraryManager.getFriendLibrary(friend).getSwingModel());
            }
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
            listener.searchStopped(FriendSingleBrowseSearch.this);
        }
    }

}
