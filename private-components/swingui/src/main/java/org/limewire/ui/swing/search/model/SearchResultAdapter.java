package org.limewire.ui.swing.search.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.limewire.core.api.search.ResultType;
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
        return getDescription() + " with " + coreResults.size() + " sources, in category: " + getCategory() + ", with size: " + getSize() + ", and extension: " + getFileExtension();
    }
    
    @Override
    public ResultType getCategory() {
        return coreResults.get(0).getResultType();
    }
    
    @Override
    public String getDescription() {
        return coreResults.get(0).getDescription();
    }
    
    @Override
    public String getFileExtension() {
        return coreResults.get(0).getFileExtension();
    }
    
    @Override
    public long getSize() {
        return coreResults.get(0).getSize();
    }

}
