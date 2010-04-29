package org.limewire.ui.swing.search.model;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;

import org.limewire.core.api.Category;
import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.URN;
import org.limewire.core.api.endpoint.RemoteHost;
import org.limewire.core.api.search.GroupedSearchResult;
import org.limewire.core.api.search.SearchResult;
import org.limewire.friend.api.Friend;

/**
 * An implementation of GroupedSearchResult for unit tests.
 */
class MockGroupedSearchResult implements GroupedSearchResult {

    private final URN urn;
    private final String fileName;
    private final Set<RemoteHost> remoteHosts;
    private final List<SearchResult> searchResults;
    
    public MockGroupedSearchResult(URN urn, String fileName) {
        this.urn = urn;
        this.fileName = fileName;
        remoteHosts = new CopyOnWriteArraySet<RemoteHost>();
        searchResults = new CopyOnWriteArrayList<SearchResult>();
        
        addSearchResult(new TestSearchResult(urn.toString(), fileName));
    }
    
    public MockGroupedSearchResult(URN urn, String fileName, Map<FilePropertyKey, Object> properties) {
        this.urn = urn;
        this.fileName = fileName;
        remoteHosts = new CopyOnWriteArraySet<RemoteHost>();
        searchResults = new CopyOnWriteArrayList<SearchResult>();
        
        addSearchResult(new TestSearchResult(urn.toString(), fileName, properties));
    }
    
    void addResult(URN urn, String fileName) {
        assert this.urn.equals(urn);
        addSearchResult(new TestSearchResult(urn.toString(), fileName));
    }
    
    private void addSearchResult(SearchResult result) {
        searchResults.add(result);
        remoteHosts.add(result.getSource());
    }
    
    void setCategory(Category category) {
        ((TestSearchResult) searchResults.get(0)).setCategory(category);
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
}
