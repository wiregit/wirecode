package org.limewire.ui.swing.search.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.limewire.core.api.search.SearchResult;

class SearchResultAdapter implements VisualSearchResult {
    
    private final List<SearchResult> coreResults;

    public SearchResultAdapter(List<SearchResult> sourceValue) {
        this.coreResults = sourceValue;
    }

    
    @Override
    public List<SearchResult> getCoreSearchResults() {
        return coreResults;
    }
    
    @Override
    public Map<Object, Object> getProperties() {
        Map<Object, Object> properties = new HashMap<Object, Object>();
        for(SearchResult result : coreResults) {
            properties.putAll(result.getProperties());
        }
        return properties;
    }
    
    @Override
    public List<VisualSearchResult> getSimiliarResults() {
        return Collections.emptyList();
    }
    
    @Override
    public List<SearchResultSource> getSources() {
        return null;
    }
    
    @Override
    public String toString() {
        return "list of "  + coreResults;
    }
    

}
