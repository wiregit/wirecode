package org.limewire.core.api.library;

import java.util.Collection;

import org.limewire.core.api.friend.Friend;

/**
 * Manager for all share lists.
 */
public interface ShareListManager {

    // TODO: is this a listener for sharing or managing?
    void addLibraryLisListener(LibraryListListener libraryListener);

    // TODO: is this a listener for sharing or managing?
    void removeLibraryListener(LibraryListListener libraryListener);

    LocalFileList getGnutellaShareList();
    
    Collection<LocalFileList> getAllFriendShareLists(); 

    LocalFileList getOrCreateFriendShareList(Friend friend);
    
    LocalFileList getFriendShareList(Friend friend);

    void removeFriendShareList(Friend friend);
}
