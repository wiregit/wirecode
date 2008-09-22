package org.limewire.ui.swing.search.model;

import org.limewire.core.api.search.SearchResult;

public class NamesMatchComparator implements SearchResultComparator {

    @Override
    public int compare(SearchResult o1, SearchResult o2) {
        String name1 = o1.getProperty(SearchResult.PropertyKey.NAME).toString();
        String name2 = o2.getProperty(SearchResult.PropertyKey.NAME).toString();
        return name1.compareTo(name2);
    }

}
