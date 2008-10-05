package org.limewire.core.api.library;

import org.limewire.core.api.friend.Friend;
import org.limewire.listener.ListenerSupport;

public interface FriendLibrary extends RemoteFileList, ListenerSupport<PresenceLibraryEvent> {
    Friend getFriend();
}
