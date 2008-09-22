/**
 * 
 */
package org.limewire.ui.swing.search.model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.limewire.core.api.search.SearchResult;

class GroupingComparator implements SearchResultComparator {

    private final Map<SearchResult, Set<SearchResult>> matchingResults;

    private final SearchResultComparator searchResultComparator;

    public GroupingComparator(SearchResultComparator searchResultComparator) {
        this.searchResultComparator = searchResultComparator;
        this.matchingResults = new HashMap<SearchResult, Set<SearchResult>>();
    }

    @Override
    public int compare(SearchResult o1, SearchResult o2) {
        int result = searchResultComparator.compare(o1, o2);

        if (result == 0 || matchFound(o1, o2)) {
            addMatch(o1, o2);
            return 0;
        }
        return result;
    }

    private boolean matchFound(SearchResult o1, SearchResult o2) {
        if (inSameGroup(o1, o2)) {
            return true;
        }
        //TODO optimize, this is expensive
        Set<SearchResult> l1 = getSet(o1);
        Set<SearchResult> l2 = getSet(o2);
        for (SearchResult searchResult : l1) {
            Set<SearchResult> set = getSet(searchResult);
            if (set.contains(o2)) {
                return true;
            }
        }
        for (SearchResult searchResult : l2) {
            Set<SearchResult> set = getSet(searchResult);
            if (set.contains(o1)) {
                return true;
            }
        }
        return false;
    }

    private boolean inSameGroup(SearchResult o1, SearchResult o2) {
        if (getSet(o1).contains(o2) || getSet(o2).contains(o1)) {
            return true;
        }
        return false;
    }

    private void addMatch(SearchResult o1, SearchResult o2) {
        //TODO optimize, this is expensive
        Set<SearchResult> l1 = getSet(o1);
        Set<SearchResult> l2 = getSet(o2);
        l1.add(o2);
        for (SearchResult searchResult : l1) {
            Set<SearchResult> set = getSet(searchResult);
            set.add(o2);
        }
        l2.add(o1);
        for (SearchResult searchResult : l2) {
            Set<SearchResult> set = getSet(searchResult);
            set.add(o1);
        }
    }

    private Set<SearchResult> getSet(SearchResult o1) {
        Set<SearchResult> set = matchingResults.get(o1);
        if (set == null) {
            set = new HashSet<SearchResult>();
            matchingResults.put(o1, set);
        }
        return set;
    }
}