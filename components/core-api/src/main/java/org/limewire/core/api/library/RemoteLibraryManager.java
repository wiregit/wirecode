package org.limewire.core.api.library;

import org.limewire.core.api.friend.Friend;

/** Manager for all remote libraries. */
public interface RemoteLibraryManager {

    RemoteFileList getOrCreateFriendLibrary(Friend friend);

    void removeFriendLibrary(Friend friend);
    
}
