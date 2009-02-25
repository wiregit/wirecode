package org.limewire.ui.swing.library;

import org.limewire.collection.glazedlists.DelegateList;
import org.limewire.collection.glazedlists.DelegateList.DelegateListener;
import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.ShareListManager;

public class DelegateListChanger {
    
    private final DelegateList<LocalFileItem> delegateList;    
    private final LibraryManager libraryManager;
    private final ShareListManager shareListManager;
    private Friend currentFriend;
    
    public DelegateListChanger(DelegateList<LocalFileItem> delegateList,
            LibraryManager libraryManager, ShareListManager shareListManager) {
        this.delegateList = delegateList;
        this.libraryManager = libraryManager;
        this.shareListManager = shareListManager;
    }
    
    public void addListener(DelegateListener<LocalFileItem> listener) {
        delegateList.addDelegateListListener(listener);
    }
    

    /**
     * Set the current friend to filter with. If no filtering is
     * to occur, set friend to null.
     */
    public void setFriend(Friend friend) {
        currentFriend = friend;
        
        if(friend == null || friend.getId() == null) {
            delegateList.setDelegateList(libraryManager.getLibraryManagedList().getSwingModel());
        } else if(friend.getId().equals(Friend.P2P_FRIEND_ID)) {
            delegateList.setDelegateList(shareListManager.getGnutellaShareList().getSwingModel());
        } else {
            delegateList.setDelegateList(shareListManager.getFriendShareList(friend).getSwingModel());
        }
    }
    
    /**
     * Return the current friend that is being filtered on, null if no filtering is
     * occuring.
     */
    public Friend getCurrentFriend() {
        return currentFriend;
    }

}
