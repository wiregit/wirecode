package org.limewire.core.impl.library;

import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.library.FriendLibrary;
import org.limewire.core.api.library.RemoteLibraryManager;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;

public class MockRemoteLibraryManager implements RemoteLibraryManager {

    @Override
    public boolean hasFriendLibrary(Friend friend) {
        // TODO Auto-generated method stub
        return false;
    }
    
    @Override
    public EventList<FriendLibrary> getFriendLibraryList() {
        return new BasicEventList<FriendLibrary>();
    }
    
    @Override
    public EventList<FriendLibrary> getSwingFriendLibraryList() {
        return new BasicEventList<FriendLibrary>();
    }
}
