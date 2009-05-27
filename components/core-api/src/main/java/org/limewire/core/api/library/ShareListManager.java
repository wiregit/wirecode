package org.limewire.core.api.library;

import org.limewire.core.api.friend.Friend;

/**
 * Manager for all share lists.
 */
// TODO: This is going to have to change when the UI uses collections.
//       Probably into something like
//    EventList<FileList<LocalFileItem>> getCollections();
//    int getSharedFileCount();
//    void addListener(...);
public interface ShareListManager {
    
    /**
     * Returns a {@link FileList} for all shared files.
     * This is a combined list of all shared files.
     */
    FileList<LocalFileItem> getCombinedShareList();

    /** Returns a {@link LocalFileList} for all files shared with Gnutella. */
    FriendFileList getGnutellaShareList();

    /** Gets a {@link LocalFileList} for the given friend, creating one if it doesn't exist.  */
    FriendFileList getOrCreateFriendShareList(Friend friend);
    
    /** Returns a {@link LocalFileList} for the given friend, returns null if none exist. */
    FriendFileList getFriendShareList(Friend friend);
}
