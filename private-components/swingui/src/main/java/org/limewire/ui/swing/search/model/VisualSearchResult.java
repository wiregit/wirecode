package org.limewire.ui.swing.search.model;

import java.util.List;
import java.util.Map;

import org.limewire.core.api.search.SearchResult;

public interface VisualSearchResult {
    
    List<SearchResult> getCoreSearchResults();
    List<VisualSearchResult> getSimiliarResults();
    Map<Object, Object> getProperties();
    List<SearchResultSource> getSources();

}
