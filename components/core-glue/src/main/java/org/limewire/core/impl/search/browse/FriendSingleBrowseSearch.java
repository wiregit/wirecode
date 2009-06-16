package org.limewire.core.impl.search.browse;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.library.FriendLibrary;
import org.limewire.core.api.library.LibraryState;
import org.limewire.core.api.library.RemoteFileItem;
import org.limewire.core.api.library.RemoteLibraryManager;
import org.limewire.core.api.search.SearchListener;
import org.limewire.core.api.search.browse.BrowseStatus;
import org.limewire.core.api.search.browse.BrowseStatusListener;
import org.limewire.core.api.search.browse.BrowseStatus.BrowseState;
import org.limewire.core.impl.library.CoreRemoteFileItem;

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
    
    /**Loads a snapshot of the available files, alerts BrowseStatusListeners that we have loaded, 
     * and SearchListeners that the search has stopped.*/
    private void loadLibrary(){
        List<RemoteFileItem> remoteFileItems = new ArrayList<RemoteFileItem>(remoteLibraryManager.getFriendLibrary(friend).getModel());
        
        // add all files
        for (RemoteFileItem item : remoteFileItems) {
            for (SearchListener listener : searchListeners) {
                listener.handleSearchResult(this, ((CoreRemoteFileItem)item).getSearchResult());
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
    
    private BrowseStatus createBrowseStatus(BrowseState state, Friend... friends){
        return new BrowseStatus(FriendSingleBrowseSearch.this, state, friends);
    }

    private class FriendLibraryListEventListener implements ListEventListener<FriendLibrary> {
        @Override
        public void listChanged(ListEvent listChanges) {
            while (listChanges.next()) {
                if (listChanges.getType() == ListEvent.INSERT) {
                    FriendLibrary newLibrary = (FriendLibrary) listChanges.getSourceList().get(listChanges.getIndex());
                    if (newLibrary.getFriend().getId().equals(friend.getId())) {//There is a new library for our friend!
                        //Add a property change listener to the new library and keep a reference to the library so we can remove the listener later.
                        currentLibrary.set(newLibrary);
                        currentLibrary.get().addPropertyChangeListener(libraryPropertyChangeListener);
                    }
                } else if (listChanges.getType() == ListEvent.DELETE && currentLibrary.get() != null && remoteLibraryManager.getFriendLibrary(friend) == null){
                    //Our friend's library is gone from remoteLibraryManager and we still have a reference to it.
                    //Remove the listener and the reference.
                    currentLibrary.get().removePropertyChangeListener(libraryPropertyChangeListener);
                    currentLibrary.set(null);
                    //TODO fire something here - our friend logged out
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

                for (BrowseStatusListener listener : browseStatusListeners) {
                    listener.statusChanged(status);
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
