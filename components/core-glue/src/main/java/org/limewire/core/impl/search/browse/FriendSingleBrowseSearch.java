package org.limewire.core.impl.search.browse;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.limewire.core.api.library.FriendLibrary;
import org.limewire.core.api.library.LibraryState;
import org.limewire.core.api.library.RemoteLibraryManager;
import org.limewire.core.api.search.SearchListener;
import org.limewire.core.api.search.SearchResult;
import org.limewire.core.api.search.browse.BrowseStatus;
import org.limewire.core.api.search.browse.BrowseStatusListener;
import org.limewire.core.api.search.browse.BrowseStatus.BrowseState;
import org.limewire.friend.api.Friend;

import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;

class FriendSingleBrowseSearch extends AbstractBrowseSearch {

    private final Friend friend;
    private final RemoteLibraryManager remoteLibraryManager;
    private final ListEventListener<FriendLibrary> friendLibraryListEventListener = new FriendLibraryListEventListener();

    private final PropertyChangeListener libraryPropertyChangeListener = new LibraryPropertyChangeListener();
    
    private final AtomicReference<FriendLibrary> currentLibrary = new AtomicReference<FriendLibrary>();

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
           fireBrowseStatusChanged(BrowseState.OFFLINE, friend);
           for (SearchListener listener : searchListeners) {
               listener.searchStopped(FriendSingleBrowseSearch.this);
           }  
            
        } else {
            if(library.getState() == LibraryState.LOADING){
                library.addPropertyChangeListener(libraryPropertyChangeListener);                
            } else {
                loadLibrary();
            }
        }
 
    }
    
    /**Loads a snapshot of the available files, alerts BrowseStatusListeners that we have loaded, 
     * and SearchListeners that the search has stopped.*/
    private void loadLibrary(){
        List<SearchResult> remoteFileItems = new ArrayList<SearchResult>(remoteLibraryManager.getFriendLibrary(friend).getModel());
        
        // add all files
        for (SearchResult item : remoteFileItems) {
            for (SearchListener listener : searchListeners) {
                listener.handleSearchResult(this, item);
            }
        }
        
        fireBrowseStatusChanged(BrowseState.LOADED);
        
        for (SearchListener listener : searchListeners) {
            listener.searchStopped(FriendSingleBrowseSearch.this);
        } 
    }
    
    /**Adds friendLibraryListEventListener to the FriendLibraryList*/
    private void installListener(){
        remoteLibraryManager.getFriendLibraryList().addListEventListener(friendLibraryListEventListener);
    }
    
    /**Removes friendLibraryListEventListener from the FriendLibraryList.  
     * Removes libraryPropertyChangeLister from the friend library if necessary.*/
    private void removeListener(){
        remoteLibraryManager.getFriendLibraryList().removeListEventListener(friendLibraryListEventListener);  
        if (currentLibrary.get() != null) {
            currentLibrary.get().removePropertyChangeListener(libraryPropertyChangeListener);
        }
    }
    
    private void fireBrowseStatusChanged(BrowseState state, Friend... friends){
        BrowseStatus status = new BrowseStatus(FriendSingleBrowseSearch.this, state, friends);
        for (BrowseStatusListener listener : browseStatusListeners) {
            listener.statusChanged(status);
        } 
    }

    private class FriendLibraryListEventListener implements ListEventListener<FriendLibrary> {
        @Override
        public void listChanged(ListEvent listChanges) {
            while (listChanges.next()) {
                if (listChanges.getType() == ListEvent.INSERT) {
                    FriendLibrary newLibrary = (FriendLibrary) listChanges.getSourceList().get(listChanges.getIndex());
                    if (newLibrary.getFriend().getId().equals(friend.getId())) {//There is a new library for our friend!
                        //Add a property change listener to the new library and keep a reference to the library so we can remove the listener later.
                        currentLibrary.set(remoteLibraryManager.getFriendLibrary(friend));
                        currentLibrary.get().addPropertyChangeListener(libraryPropertyChangeListener);
                    }
                } else if (listChanges.getType() == ListEvent.DELETE && remoteLibraryManager.getFriendLibrary(friend) == null){   
                    //our friend has logged off
                    currentLibrary.set(null);
                    fireBrowseStatusChanged(BrowseState.OFFLINE, friend);
                }
            }
        }
    }
    
    private class LibraryPropertyChangeListener implements PropertyChangeListener {
        
        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            LibraryState state = (LibraryState)evt.getNewValue();
            if (state != LibraryState.LOADING) {
                // The list has changed - tell the listeners
                if (state == LibraryState.LOADED) {
                    fireBrowseStatusChanged(BrowseState.UPDATED);
                } else {
                    fireBrowseStatusChanged(BrowseState.FAILED, friend);
                }

            }
        }
    }

    @Override
    public void repeat() {
        stop();
        start();
    }
}
