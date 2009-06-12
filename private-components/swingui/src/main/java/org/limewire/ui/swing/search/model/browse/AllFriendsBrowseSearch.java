package org.limewire.ui.swing.search.model.browse;

import java.util.ArrayList;
import java.util.List;

import org.limewire.core.api.library.RemoteFileItem;
import org.limewire.core.api.library.RemoteLibraryManager;
import org.limewire.core.api.search.SearchListener;
import org.limewire.ui.swing.search.model.BrowseStatusListener;
import org.limewire.ui.swing.search.model.browse.BrowseStatus.BrowseState;

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
                listener.handleSearchResult(this, item.getSearchResult());
            }
        }
        
        //browse is finished
        for (SearchListener listener : searchListeners) {
            listener.searchStopped(AllFriendsBrowseSearch.this);
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
