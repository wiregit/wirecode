package org.limewire.core.api.library;

import org.limewire.core.api.friend.Friend;

/**
 * Manager for all share lists.
 */
public interface ShareListManager {

    /** Returns a {@link LocalFileList} for all files shared with Gnutella. */
    LocalFileList getGnutellaShareList();

    /** Gets a {@link LocalFileList} for the given friend, creating one if it doesn't exist.  */
    LocalFileList getOrCreateFriendShareList(Friend friend);
    
    /** Returns a {@link LocalFileList} for the given friend, returns null if none exist. */
    LocalFileList getFriendShareList(Friend friend);
    
    /**
     * Returns a {@link FileList} that is a combined list of all shared files.
     * This filters out duplicates, so a File shared with two friends is listed
     * only once.
     */
    FileList<LocalFileItem> getCombinedFriendShareLists();

    /** Removes a {@link LocalFileList} from being counted as a share list for the given friend. */
    void removeFriendShareList(Friend friend);
}
