package org.limewire.ui.swing.search.model;

import org.limewire.core.api.search.SearchResult;

public abstract class AbstractSearchResultMatcher implements SearchResultMatcher {

    public AbstractSearchResultMatcher() {
        super();
    }

    @Override
    public boolean matches(VisualSearchResult o1, VisualSearchResult o2) {
        for (SearchResult result1 : o1.getCoreSearchResults()) {
                for (SearchResult result2 : o2.getCoreSearchResults()) {
                    if(matches(result1, result2)) {
                        return true;
                    }       
                }
        }
        return false;
    }

    /**
     * Compares two search results against each other and returns true if they match.
     */
    public abstract boolean matches(SearchResult result1, SearchResult result2);
}