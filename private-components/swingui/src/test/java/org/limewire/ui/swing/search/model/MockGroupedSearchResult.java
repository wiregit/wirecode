package org.limewire.ui.swing.search.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.limewire.core.api.Category;
import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.URN;
import org.limewire.core.api.endpoint.RemoteHost;
import org.limewire.core.api.search.GroupedSearchResult;
import org.limewire.core.api.search.GroupedSearchResultListener;
import org.limewire.core.api.search.SearchResult;
import org.limewire.friend.api.Friend;

class MockGroupedSearchResult implements GroupedSearchResult {

    private final URN urn;
    private final String fileName;
    private final Set<RemoteHost> remoteHosts;
    private final List<GroupedSearchResultListener> resultListeners;
    private final List<SearchResult> searchResults;
    
    public MockGroupedSearchResult(URN urn, String fileName) {
        this.urn = urn;
        this.fileName = fileName;
        remoteHosts = new HashSet<RemoteHost>();
        resultListeners = new ArrayList<GroupedSearchResultListener>();
        searchResults = new ArrayList<SearchResult>();
        
        addSearchResult(new TestSearchResult(urn.toString(), fileName));
    }
    
    public MockGroupedSearchResult(URN urn, String fileName, Map<FilePropertyKey, Object> properties) {
        this.urn = urn;
        this.fileName = fileName;
        remoteHosts = new HashSet<RemoteHost>();
        resultListeners = new ArrayList<GroupedSearchResultListener>();
        searchResults = new ArrayList<SearchResult>();
        
        addSearchResult(new TestSearchResult(urn.toString(), fileName, properties));
    }
    
    void addResult(URN urn, String fileName) {
        assert this.urn.equals(urn);
        addSearchResult(new TestSearchResult(urn.toString(), fileName));
        
        for (GroupedSearchResultListener listener : resultListeners) {
            listener.sourceAdded();
        }
    }
    
    private void addSearchResult(SearchResult result) {
        searchResults.add(result);
        for (RemoteHost host : result.getSources()) {
            remoteHosts.add(host);
        }
    }
    
    void setCategory(Category category) {
        ((TestSearchResult) searchResults.get(0)).setCategory(category);
    }
    
    @Override
    public void addResultListener(GroupedSearchResultListener listener) {
        resultListeners.add(listener);
    }

    @Override
    public void removeResultListener(GroupedSearchResultListener listener) {
        resultListeners.add(listener);
    }

    @Override
    public String getFileName() {
        return fileName;
    }

    @Override
    public Collection<Friend> getFriends() {
        return Collections.emptyList();
    }

    @Override
    public float getRelevance() {
        return 0;
    }

    @Override
    public List<SearchResult> getSearchResults() {
        return searchResults;
    }

    @Override
    public Collection<RemoteHost> getSources() {
        return remoteHosts;
    }

    @Override
    public URN getUrn() {
        return urn;
    }

    @Override
    public boolean isAnonymous() {
        return true;
    }

}
