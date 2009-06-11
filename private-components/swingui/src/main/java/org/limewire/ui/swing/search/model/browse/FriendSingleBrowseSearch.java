package org.limewire.ui.swing.search.model.browse;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.library.FriendLibrary;
import org.limewire.core.api.library.LibraryState;
import org.limewire.core.api.library.RemoteFileItem;
import org.limewire.core.api.library.RemoteLibraryManager;
import org.limewire.core.api.search.SearchListener;
import org.limewire.ui.swing.search.model.BrowseStatusListener;
import org.limewire.ui.swing.search.model.browse.BrowseStatus.BrowseState;
import org.limewire.ui.swing.util.SwingUtils;

import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;

class FriendSingleBrowseSearch extends AbstractBrowseSearch {

    private final Friend friend;
    private final RemoteLibraryManager remoteLibraryManager;
    private final ListEventListener<FriendLibrary> friendLibraryListEventListener = new FriendLibraryListEventListener();

    private PropertyChangeListener libraryPropertyChangeListener = new LibraryPropertyChangeListener();
    
    private FriendLibrary currentLibrary;

    /**
     * @param friend the person to be browsed - can not be anonymous or null
     */
    public FriendSingleBrowseSearch(RemoteLibraryManager remoteLibraryManager, Friend friend) {
        assert(friend != null && !friend.isAnonymous());
        this.friend = friend;
        this.remoteLibraryManager = remoteLibraryManager;
    }


    @Override
    public void start() {
        for (SearchListener listener : searchListeners) {
            listener.searchStarted(FriendSingleBrowseSearch.this);
        }

        startFriendBrowse();
        installListener();
    }

    @Override
    public void stop() {
        for (SearchListener listener : searchListeners) {
            listener.searchStopped(FriendSingleBrowseSearch.this);
        }       
        removeListener();
    }


    private void startFriendBrowse() {
        // TODO: RemoteFileItems are going away. Need a new way to access a
        // snapshot of what is currently shared.

        FriendLibrary library = remoteLibraryManager.getFriendLibrary(friend);
        
        if (library == null) {
            // Failed!
            //TODO: should be something about being offline?
            BrowseStatus status = createBrowseStatus(BrowseState.FAILED, friend);

            for (BrowseStatusListener listener : browseStatusListeners) {
                listener.statusChanged(status);
            } 
            
        } else {
            if(library.getState() == LibraryState.LOADING){
                library.addPropertyChangeListener(libraryPropertyChangeListener);                
            } else {
                loadLibrary();
            }
        }
 
    }
    
    private void loadLibrary(){
        List<RemoteFileItem> remoteFileItems = new ArrayList<RemoteFileItem>(remoteLibraryManager.getFriendLibrary(friend).getSwingModel());
        
        // add all files
        for (RemoteFileItem item : remoteFileItems) {
            for (SearchListener listener : searchListeners) {
                listener.handleSearchResult(this, item.getSearchResult());
            }
        }
        
        BrowseStatus status = createBrowseStatus(BrowseState.LOADED);

        for (BrowseStatusListener listener : browseStatusListeners) {
            listener.statusChanged(status);
        } 
        
        for (SearchListener listener : searchListeners) {
            listener.searchStopped(FriendSingleBrowseSearch.this);
        } 
    }
    
    private void installListener(){
        remoteLibraryManager.getSwingFriendLibraryList().addListEventListener(friendLibraryListEventListener);
    }
    
    private void removeListener(){
        remoteLibraryManager.getSwingFriendLibraryList().removeListEventListener(friendLibraryListEventListener);  
        if (currentLibrary != null) {
            currentLibrary.removePropertyChangeListener(libraryPropertyChangeListener);
        }
    }
    
    private BrowseStatus createBrowseStatus(BrowseState state, Friend... friends){
        return new BrowseStatus(FriendSingleBrowseSearch.this, state, friends);
    }

    private class FriendLibraryListEventListener implements ListEventListener<FriendLibrary> {
        @Override
        public void listChanged(ListEvent listChanges) {
            while (listChanges.next()) {
                if (listChanges.getType() == ListEvent.INSERT) {
                    FriendLibrary newLibrary = (FriendLibrary) listChanges.getSourceList().get(listChanges.getIndex());
                    if (newLibrary.getFriend().getId().equals(friend.getId())) {
                        currentLibrary = newLibrary;
                        currentLibrary.addPropertyChangeListener(libraryPropertyChangeListener);
                    }
                } else if (listChanges.getType() == ListEvent.DELETE && currentLibrary != null && remoteLibraryManager.getFriendLibrary(friend) == null){
                    currentLibrary.removePropertyChangeListener(libraryPropertyChangeListener);
                    currentLibrary = null;
                }
            }
        }
    }
    
    private class LibraryPropertyChangeListener implements PropertyChangeListener {
        
        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            FriendLibrary library = (FriendLibrary)evt.getSource();
            if (library.getState() != LibraryState.LOADING) {
                library.removePropertyChangeListener(LibraryPropertyChangeListener.this);
                // The list has changed - tell the listeners
                
                final BrowseStatus status = library.getState() == LibraryState.LOADED ? 
                        createBrowseStatus(BrowseState.UPDATED) : 
                            createBrowseStatus(BrowseState.FAILED, friend);
                  SwingUtils.invokeLater(new Runnable(){
                    @Override
                    public void run() {
                        for (BrowseStatusListener listener : browseStatusListeners) {
                            listener.statusChanged(status);
                        }
                    }
                });  
                
            }
        }
    }

    @Override
    public void repeat() {
        start();
    }
}
