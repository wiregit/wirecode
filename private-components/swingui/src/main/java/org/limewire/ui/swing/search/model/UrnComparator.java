/**
 * 
 */
package org.limewire.ui.swing.search.model;

import org.limewire.core.api.search.SearchResult;

class UrnComparator implements SearchResultComparator {
    @Override
    public int compare(SearchResult o1, SearchResult o2) {
        String urn1 = o1.getUrn();
        String urn2 = o2.getUrn();
        return urn1.compareTo(urn2);
    }
}