package org.limewire.core.impl.browse;

import org.limewire.core.api.browse.Browse;
import org.limewire.core.api.browse.BrowseFactory;
import org.limewire.friend.api.FriendPresence;
import org.limewire.io.Address;
import org.limewire.io.Connectable;

class MockBrowseFactory implements BrowseFactory {

    @Override
    public Browse createBrowse(FriendPresence friendPresence) {
        return null;
    }

    @Override
    public Browse createBrowse(Connectable connectable) {
        return null;
    }

    @Override
    public Browse createBrowse(Address address, byte[] guid) {
        return null;
    }
}
