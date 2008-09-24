package org.limewire.ui.swing.search.model;

import org.limewire.core.api.search.SearchResult;

public class NameMatcher implements SearchResultMatcher {

    @Override
    public boolean matches(VisualSearchResult o1, VisualSearchResult o2) {
        for (SearchResult result1 : o1.getCoreSearchResults()) {
            String name1 = result1.getProperty(SearchResult.PropertyKey.NAME).toString();
            for (SearchResult result2 : o2.getCoreSearchResults()) {
                String name2 = result2.getProperty(SearchResult.PropertyKey.NAME).toString();
                int result = name1.compareTo(name2);
                if (result == 0) {
                    return true;
                }
            }
        }

        return false;
    }
}
