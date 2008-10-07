package org.limewire.core.api.library;

import org.limewire.core.api.friend.Friend;

import ca.odell.glazedlists.EventList;

public interface RemoteLibraryManager {
    
    EventList<FriendLibrary> getFriendLibraryList();
    
    boolean hasFriendLibrary(Friend friend);

    /** A list of all friend's libraries suitable for use in Swing. */
    EventList<FriendLibrary> getSwingFriendLibraryList();
}
