package org.limewire.core.api.library;

import java.util.Collection;

import org.limewire.core.api.friend.Friend;

/**
 * Manager for all share lists.
 */
public interface ShareListManager {

    LocalFileList getGnutellaShareList();
    
    Collection<LocalFileList> getAllFriendShareLists(); 

    LocalFileList getOrCreateFriendShareList(Friend friend);
    
    LocalFileList getFriendShareList(Friend friend);

    void removeFriendShareList(Friend friend);
}
