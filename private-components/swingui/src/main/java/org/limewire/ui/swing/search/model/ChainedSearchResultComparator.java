/**
 * 
 */
package org.limewire.ui.swing.search.model;

import java.util.Arrays;
import java.util.List;

import org.limewire.core.api.search.SearchResult;

class ChainedSearchResultComparator implements SearchResultComparator {
    private final List<SearchResultComparator> comparators;

    public ChainedSearchResultComparator(SearchResultComparator... comparators) {
        this.comparators = Arrays.asList(comparators);
    }

    @Override
    public int compare(SearchResult o1, SearchResult o2) {

        for (SearchResultComparator comparator : comparators) {
            int result = comparator.compare(o1, o2);
            if (result == 0) {
                return 0;
            }
        }

        return new UrnComparator().compare(o1, o2);
    }
}