package org.limewire.ui.swing.search.model;

public interface SearchResultMatcher {

    /**
     * Return true if the given search results match.
     */
    public boolean matches(VisualSearchResult o1, VisualSearchResult o2);

}
