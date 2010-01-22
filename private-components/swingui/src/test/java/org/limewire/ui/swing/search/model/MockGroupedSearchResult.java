package org.limewire.ui.swing.search.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

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
    private final List<SearchResult> searchResults;
    
    public MockGroupedSearchResult(URN urn, String fileName) {
        this.urn = urn;
        this.fileName = fileName;
        
        searchResults = new ArrayList<SearchResult>();
        searchResults.add(new TestSearchResult(urn.toString(), fileName));
    }
    
    public MockGroupedSearchResult(URN urn, String fileName, Map<FilePropertyKey, Object> properties) {
        this.urn = urn;
        this.fileName = fileName;
        
        searchResults = new ArrayList<SearchResult>();
        searchResults.add(new TestSearchResult(urn.toString(), fileName, properties));
    }
    
    void setCategory(Category category) {
        ((TestSearchResult) searchResults.get(0)).setCategory(category);
    }
    
    @Override
    public void addResultListener(GroupedSearchResultListener listener) {
    }

    @Override
    public void removeResultListener(GroupedSearchResultListener listener) {
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
        return Collections.emptyList();
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
