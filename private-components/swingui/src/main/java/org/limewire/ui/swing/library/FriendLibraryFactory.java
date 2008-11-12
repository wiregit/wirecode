package org.limewire.ui.swing.library;

import javax.swing.JComponent;

import org.limewire.core.api.friend.Friend;

public interface FriendLibraryFactory {
    
    JComponent createFriendLibrary(Friend friend);
}
