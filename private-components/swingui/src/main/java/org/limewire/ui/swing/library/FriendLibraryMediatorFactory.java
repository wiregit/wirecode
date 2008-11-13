package org.limewire.ui.swing.library;

import org.limewire.core.api.friend.Friend;

public interface FriendLibraryMediatorFactory {

    FriendLibraryMediator createFriendLibraryBasePanel(Friend friend);
}
