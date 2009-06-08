package org.limewire.ui.swing.library;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.limewire.collection.glazedlists.PluggableList;
import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.SharedFileList;
import org.limewire.core.api.library.SharedFileListManager;

import ca.odell.glazedlists.EventList;

/**
 * Allows My Library to be filtered based on a friend. When a friend
 * is selected, only files that are shared with that friend are 
 * displayed.
 */
public class LibraryListSourceChanger implements ListSourceChanger {
    
    private final PluggableList<LocalFileItem> delegateList;    
    private final LibraryManager libraryManager;
    private final SharedFileListManager sharedFileListManager;
//    private final ShareListManager shareListManager;
    private final List<ListChangedListener> listeners = new CopyOnWriteArrayList<ListChangedListener>();
    
    private String currentListId;
//    private Friend currentFriend;
    
    public LibraryListSourceChanger(PluggableList<LocalFileItem> delegateList,
            LibraryManager libraryManager, SharedFileListManager sharedFileListManager) {
        this.delegateList = delegateList;
        this.libraryManager = libraryManager;
        this.sharedFileListManager = sharedFileListManager;
    }
    
    public void addListener(ListChangedListener listener) {
        listeners.add(listener);
    }

    /**
     * Set the current friend to filter with. If no filtering is
     * to occur, set friend to null.
     */
    public void setCurrentList(String currentListId) {//Friend friend) {
        System.out.println("selecting " + currentListId);
        this.currentListId = currentListId;
        
        if(currentListId == null)
            delegateList.setSource(libraryManager.getLibraryManagedList().getSwingModel());
        else //TODO: this locks, should be in a different thread.
            delegateList.setSource(sharedFileListManager.getSharedFileList(currentListId).getSwingModel());
//        if(friend == null || friend.getId() == null) {
//            delegateList.setSource(libraryManager.getLibraryManagedList().getSwingModel());
//        } else if(friend.getId().equals(Friend.P2P_FRIEND_ID)) {
//            delegateList.setSource(shareListManager.getGnutellaShareList().getSwingModel());
//        } else {
//            delegateList.setSource(shareListManager.getFriendShareList(friend).getSwingModel());
//        }
        
        for(ListChangedListener listener : listeners) {
            listener.idChanged(currentListId);
        }
    }
    
    public String getCurrentListID() {
        return currentListId;
    }
}
