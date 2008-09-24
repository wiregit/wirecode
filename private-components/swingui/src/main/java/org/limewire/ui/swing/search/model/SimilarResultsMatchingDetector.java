package org.limewire.ui.swing.search.model;

import java.util.List;

/**
 * Responsible for detecting a VisualSearchResult that is similar to another
 * result and associating the two. Uses a matcher, on successful match, the
 * parent for that group is found and the items are updated to reflect this.
 */
public class SimilarResultsMatchingDetector implements SimilarResultsDetector {
    private final SearchResultMatcher searchResultComparator;

    /**
     * Builds the SimilarResultDetector using the given supplied matching
     * algorithm.
     */
    public SimilarResultsMatchingDetector(SearchResultMatcher searchResultMatcher) {
        this.searchResultComparator = searchResultMatcher;
    }

    @Override
    public void detectSimilarResult(List<VisualSearchResult> results, VisualSearchResult eventItem) {
        for (VisualSearchResult result : results) {
            if (result != eventItem &&  searchResultComparator.matches(result, eventItem)) {
                update(eventItem, result);
            }
        }
    }

    /**
     * Finds the parent correlating the two searchResults and then moves these
     * search results under that parent. Then updates the visibility of these
     * items based on their parents visibilities.
     */
    private void update(VisualSearchResult o1, VisualSearchResult o2) {

        VisualSearchResult parent = findParent(o1, o2);

        boolean childrenVisible = o1.isChildrenVisible() || o2.isChildrenVisible()
                || parent.isChildrenVisible() || o1.getSimilarityParent() != null
                && o1.getSimilarityParent().isChildrenVisible() || o2.getSimilarityParent() != null
                && o2.getSimilarityParent().isChildrenVisible()
                || parent.getSimilarityParent() != null
                && parent.getSimilarityParent().isChildrenVisible();

        updateParent(o1, parent);
        updateParent(o2, parent);
        updateVisibility(parent, childrenVisible);
    }

    /**
     * Update visibilities of newly changed parents.
     */
    private void updateVisibility(VisualSearchResult parent, boolean childrenVisible) {
        parent.setChildrenVisible(childrenVisible);
        for (VisualSearchResult similarResult : parent.getSimilarResults()) {
            similarResult.setChildrenVisible(false);
        }
    }

    /**
     * Updates the child to use the given parent. The parent is set, the
     * children are moved, and the visibility is copied. Also the given child is
     * checked to see if it already has a parent, if so its parent is also
     * updated to be a child of the given parent.
     */
    private void updateParent(VisualSearchResult child, VisualSearchResult parent) {
        if (child.getSimilarityParent() != null && child.getSimilarityParent() != parent) {
            updateFields(child.getSimilarityParent(), parent);
        }

        if (child!= null && child != parent) {
            updateFields(child, parent);
        }

    }

    /**
     * Updates the fields of the given child and parent to reflect their
     * relationship.
     */
    private void updateFields(VisualSearchResult child, VisualSearchResult parent) {
        ((SearchResultAdapter) parent).setSimilarityParent(null);
        ((SearchResultAdapter) child).setSimilarityParent(parent);
        ((SearchResultAdapter) parent).addSimilarSearchResult(child);
        moveChildren(child, parent);
    }

    /**
     * Moves the children from the child to the parent.
     */
    private void moveChildren(VisualSearchResult child, VisualSearchResult parent) {
        ((SearchResultAdapter) child).removeSimilarSearchResult(parent);
        for (VisualSearchResult item : child.getSimilarResults()) {
            updateFields(item, parent);
            ((SearchResultAdapter) child).removeSimilarSearchResult(item);
            ((SearchResultAdapter) parent).addSimilarSearchResult(item);

        }
    }

    /**
     * Returns which item should be the parent between the two similar search
     * results. Currently the item with the most core results, is considered the
     * parent.
     */
    private VisualSearchResult findParent(VisualSearchResult o1, VisualSearchResult o2) {
        VisualSearchResult parent = null;

        VisualSearchResult parent1 = o1;
        VisualSearchResult parent2 = o2;
        VisualSearchResult parent3 = o1.getSimilarityParent();
        VisualSearchResult parent4 = o2.getSimilarityParent();
        int parent1Count = parent1 == null ? 0 : parent1.getCoreSearchResults().size();
        int parent2Count = parent2 == null ? 0 : parent2.getCoreSearchResults().size();
        int parent3Count = parent3 == null ? 0 : parent3.getCoreSearchResults().size();
        int parent4Count = parent4 == null ? 0 : parent4.getCoreSearchResults().size();
        
        if (parent4Count > parent3Count && parent4Count > parent2Count
                && parent4Count > parent1Count) {
            parent = parent4;
        } else if (parent3Count > parent2Count && parent3Count > parent1Count) {
            parent = parent3;
        } else if (parent2Count > parent1Count) {
            parent = parent2;
        } else {
            parent = parent1;
        }

        return parent;
    }

}
