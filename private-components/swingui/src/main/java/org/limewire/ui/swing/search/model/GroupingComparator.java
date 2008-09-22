/**
 * 
 */
package org.limewire.ui.swing.search.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.limewire.core.api.search.SearchResult;

class GroupingComparator implements SearchResultComparator {

    public class Group {
        private final Set<SearchResult> items;

        private List<String> urns;

        public Group() {
            this.items = new HashSet<SearchResult>();
            this.urns = new ArrayList<String>();
        }

        public void add(SearchResult searchResult) {
            items.add(searchResult);
            urns.add(searchResult.getUrn());
        }

        public boolean remove(SearchResult searchResult) {
            boolean removed = items.remove(searchResult);
            if (removed) {
                urns.remove(searchResult.getUrn());
            }
            return removed;
        }

        public boolean isInGroup(SearchResult searchResult) {
            return items.contains(searchResult);
        }

        public boolean belongsInGroup(SearchResult searchResult) {
            return hasResultUrn(searchResult);
        }

        public boolean hasResultUrn(SearchResult searchResult) {
            return urns.contains(searchResult.getUrn());
        }

        public void addAll(Group group) {
            for (SearchResult result : group.items) {
                add(result);
            }
        }

    }

    private final Map<SearchResult, Group> matchingResults;

    private final SearchResultComparator searchResultComparator;

    public GroupingComparator(SearchResultComparator searchResultComparator) {
        this.searchResultComparator = searchResultComparator;
        this.matchingResults = new HashMap<SearchResult, Group>();
    }

    @Override
    public int compare(SearchResult o1, SearchResult o2) {

        int result = searchResultComparator.compare(o1, o2);

        boolean hasGroup1 = hasGroup(o1);
        boolean hasGroup2 = hasGroup(o2);

        if (hasGroup1 && !hasGroup2 && result == 0) {
            Group group1 = getGroup(o1);
            group1.add(o2);
        } else if (!hasGroup1 && hasGroup2 && result == 0) {
            Group group2 = getGroup(o1);
            group2.add(o1);
        } else if (result == 0) {
            Group group1 = getGroup(o1);
            group1.addAll(getGroup(o2));
            matchingResults.put(o2, group1);
        }

        return result;
    }

    private boolean hasGroup(SearchResult o1) {
        return matchingResults.containsKey(o1);
    }

    private Group getGroup(SearchResult o1) {
        Group group = matchingResults.get(o1);
        if (group == null) {
            group = new Group();
            group.add(o1);
            matchingResults.put(o1, group);
        }
        return group;
    }

}