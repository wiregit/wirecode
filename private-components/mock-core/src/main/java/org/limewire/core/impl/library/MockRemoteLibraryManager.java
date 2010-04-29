package org.limewire.core.impl.library;

import org.limewire.core.api.library.FriendLibrary;
import org.limewire.core.api.library.RemoteLibraryManager;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;

public class MockRemoteLibraryManager implements RemoteLibraryManager {
    
    @Override
    public EventList<FriendLibrary> getFriendLibraryList() {
        return new BasicEventList<FriendLibrary>();
    }
}
