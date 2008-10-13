package org.limewire.ui.swing.search.model;

/**
 * Matcher that indicates two results match if any of the supplied matchers
 * think they match.
 */
public class OrSearchResultMatcher implements SearchResultMatcher {

    private final SearchResultMatcher searchResultMatcher;

    private final SearchResultMatcher[] otherSearchResultMatchers;

    public OrSearchResultMatcher(SearchResultMatcher searchResultMatcher,
            SearchResultMatcher... otherSearchResultMatchers) {
        this.searchResultMatcher = searchResultMatcher;
        this.otherSearchResultMatchers = otherSearchResultMatchers;
    }

    @Override
    public boolean matches(VisualSearchResult o1, VisualSearchResult o2) {
        if (searchResultMatcher.matches(o1, o2)) {
            return true;
        }

        for (SearchResultMatcher otherMatcher : otherSearchResultMatchers) {
            if (otherMatcher.matches(o1, o2)) {
                return true;
            }
        }
        return false;
    }
}