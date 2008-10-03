package org.limewire.ui.swing.library;

import org.limewire.core.api.Category;
import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.library.RemoteFileItem;

import ca.odell.glazedlists.EventList;

public interface FriendLibraryFactory {
    
    FriendLibraryPanel createFriendLibrary(Friend friend, Category category, EventList<RemoteFileItem> eventList);
}
