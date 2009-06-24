package org.limewire.core.impl.search.browse;

import java.util.Collection;

import org.limewire.core.api.endpoint.RemoteHost;
import org.limewire.core.api.search.browse.BrowseSearch;
import org.limewire.core.api.search.browse.BrowseSearchFactory;
import org.limewire.friend.api.Friend;

class MockBrowseSearchFactory implements BrowseSearchFactory {

    @Override
    public BrowseSearch createAllFriendsBrowseSearch() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public BrowseSearch createBrowseSearch(RemoteHost person) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public BrowseSearch createBrowseSearch(Collection<RemoteHost> people) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public BrowseSearch createFriendBrowseSearch(Friend friend) {
        // TODO Auto-generated method stub
        return null;
    }

}
