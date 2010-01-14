package org.limewire.ui.swing.search.model;

import java.util.Collection;
import java.util.List;

import org.limewire.core.api.URN;
import org.limewire.core.api.endpoint.RemoteHost;
import org.limewire.core.api.search.GroupedSearchResult;
import org.limewire.core.api.search.SearchResult;
import org.limewire.friend.api.Friend;

class MockGroupedSearchResult implements GroupedSearchResult {

    @Override
    public List<SearchResult> getCoreSearchResults() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection<Friend> getFriends() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public float getRelevance() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public Collection<RemoteHost> getSources() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public URN getUrn() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isAnonymous() {
        // TODO Auto-generated method stub
        return false;
    }

}
