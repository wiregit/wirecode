package org.limewire.ui.swing.search.model;

import java.util.Collection;
import java.util.List;

import org.limewire.core.api.URN;
import org.limewire.core.api.endpoint.RemoteHost;
import org.limewire.core.api.search.GroupedSearchResult;
import org.limewire.core.api.search.GroupedSearchResultListener;
import org.limewire.core.api.search.SearchResult;
import org.limewire.friend.api.Friend;

class MockGroupedSearchResult implements GroupedSearchResult {

    @Override
    public void addResultListener(GroupedSearchResultListener listener) {
        // TODO Auto-generated method stub
    }

    @Override
    public void removeResultListener(GroupedSearchResultListener listener) {
        // TODO Auto-generated method stub
    }

    @Override
    public String getFileName() {
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
    public List<SearchResult> getSearchResults() {
        // TODO Auto-generated method stub
        return null;
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
