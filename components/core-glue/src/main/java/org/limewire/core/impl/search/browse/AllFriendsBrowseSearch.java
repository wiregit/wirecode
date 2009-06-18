package org.limewire.core.impl.search.browse;

import java.util.ArrayList;
import java.util.List;

import org.limewire.core.api.library.RemoteFileItem;
import org.limewire.core.api.library.RemoteLibraryManager;
import org.limewire.core.api.search.SearchListener;
import org.limewire.core.api.search.browse.BrowseStatus;
import org.limewire.core.api.search.browse.BrowseStatusListener;
import org.limewire.core.api.search.browse.BrowseStatus.BrowseState;
import org.limewire.core.impl.library.CoreRemoteFileItem;
import org.limewire.core.settings.FriendBrowseSettings;

import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;

class AllFriendsBrowseSearch extends AbstractBrowseSearch {
    
    private final RemoteLibraryManager remoteLibraryManager;
    private final ListEventListener<RemoteFileItem> listEventListener = new AllFriendsListEventListener<RemoteFileItem>();

  
    public AllFriendsBrowseSearch(RemoteLibraryManager remoteLibraryManager) {
        this.remoteLibraryManager = remoteLibraryManager;
    }


    @Override
    public void start() {
        for (SearchListener listener : searchListeners) {
            listener.searchStarted(AllFriendsBrowseSearch.this);
        }

        loadSnapshot();
        installListener();
    }

    @Override
    public void stop() {
        for (SearchListener listener : searchListeners) {
            listener.searchStopped(AllFriendsBrowseSearch.this);
        }    
        
        removeListener();
    }


    private void loadSnapshot() {
        // TODO: RemoteFileItems are going away. Need a new way to access a
        // snapshot of what is currently shared.
        List<RemoteFileItem> remoteFileItems = new ArrayList<RemoteFileItem>(remoteLibraryManager.getAllFriendsFileList().getModel());
        
        //add all files
        for (RemoteFileItem item : remoteFileItems) {
            for (SearchListener listener : searchListeners) {
                listener.handleSearchResult(this, ((CoreRemoteFileItem)item).getSearchResult());
            }
        }
        
        
        if(remoteFileItems.size() > 0){
            FriendBrowseSettings.HAS_BROWSED_ALL_FRIENDS.set(true);
        } 
        
        //browse is finished
        for (SearchListener listener : searchListeners) {
            listener.searchStopped(AllFriendsBrowseSearch.this);
        }
        
        BrowseStatus status = (remoteFileItems.size() > 0) ? new BrowseStatus(AllFriendsBrowseSearch.this, BrowseState.LOADED) :
            new BrowseStatus(AllFriendsBrowseSearch.this, BrowseState.NO_FRIENDS_SHARING);
        for (BrowseStatusListener listener : browseStatusListeners){
            listener.statusChanged(status);
        }

    }
    
    private void installListener(){
        remoteLibraryManager.getAllFriendsFileList().getModel().addListEventListener(listEventListener);
    }
    
    private void removeListener(){
        remoteLibraryManager.getAllFriendsFileList().getModel().removeListEventListener(listEventListener);        
    }
    
    private class AllFriendsListEventListener<E> implements ListEventListener<E> {
        @Override
        public void listChanged(ListEvent listChanges) {
            BrowseStatus status = new BrowseStatus(AllFriendsBrowseSearch.this, BrowseState.UPDATED);
            for (BrowseStatusListener listener : browseStatusListeners){
                listener.statusChanged(status);
            }
        }        
    }

    @Override
    public void repeat() {
        loadSnapshot();
    }

}
