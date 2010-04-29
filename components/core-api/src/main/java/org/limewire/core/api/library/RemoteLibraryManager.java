package org.limewire.core.api.library;

import ca.odell.glazedlists.EventList;

public interface RemoteLibraryManager {
    
    /**
     * Returns an {@link EventList} composed of {@link FriendLibrary FriendLibraries}.
     */
    EventList<FriendLibrary> getFriendLibraryList();
}
