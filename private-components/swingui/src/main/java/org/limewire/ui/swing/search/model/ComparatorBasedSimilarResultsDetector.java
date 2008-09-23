package org.limewire.ui.swing.search.model;

import java.util.List;

/**
 * Responsible for detecting a VisualSearchResult that is similar to another
 * result and associating the two. Uses a comparator checking for compare result
 * == 0, on successful match, the parent for that group is found and the items
 * are updated to reflect this.
 */
public class ComparatorBasedSimilarResultsDetector implements SimilarResultsDetector {
    private final SearchResultComparator searchResultComparator;

    public ComparatorBasedSimilarResultsDetector(SearchResultComparator searchResultComparator) {
        this.searchResultComparator = searchResultComparator;
    }

    public void detectSimilarResult(List<VisualSearchResult> results, VisualSearchResult result) {
        for (VisualSearchResult existingResult : results) {
            if (existingResult != result && compareMatched(existingResult, result)) {
                update(result, existingResult);
            }
        }

    }

    private boolean compareMatched(VisualSearchResult result, VisualSearchResult result2) {
        return searchResultComparator.compare(result, result2) == 0;
    }

    private void update(VisualSearchResult addedItem, VisualSearchResult existingItem) {
        VisualSearchResult parent = findParent(addedItem, existingItem);
        updateParent(addedItem, parent);
        updateParent(existingItem, parent);
        updateVisibility(addedItem, parent);
        updateVisibility(existingItem, parent);
    }

    /**
     * Update visibilities of newly changed parents.
     */
    private void updateVisibility(VisualSearchResult child, VisualSearchResult parent) {
        parent.setChildrenVisible(child.isChildrenVisible() || parent.isChildrenVisible());
        child.setChildrenVisible(false);
    }

    /**
     * Updates the child to use the given parent. The parent is set, the
     * children are moved, and the visibility is copied.
     */
    private void updateParent(VisualSearchResult child, VisualSearchResult parent) {
        if (child != parent) {
            ((SearchResultAdapter) parent).setSimilarityParent(null);
            ((SearchResultAdapter) child).setSimilarityParent(parent);
            ((SearchResultAdapter) parent).addSimilarSearchResult(child);
            moveChildren(child, parent);
        }
    }

    /**
     * Moves the children from the child to the parent.
     */
    private void moveChildren(VisualSearchResult updateItem, VisualSearchResult parent) {
        for (VisualSearchResult child : updateItem.getSimilarResults()) {
            updateParent(child, parent);
            ((SearchResultAdapter) updateItem).removeSimilarSearchResult(child);
            ((SearchResultAdapter) parent).addSimilarSearchResult(child);
        }
    }

    /**
     * Returns which item should be the parent between the two similar search
     * results. Currently the item with the most core results, is considered the
     * parent.
     */
    @SuppressWarnings("null")
    private VisualSearchResult findParent(VisualSearchResult item1, VisualSearchResult item2) {
        VisualSearchResult parent = null;

        VisualSearchResult parent1 = item1;
        VisualSearchResult parent2 = item2;
        VisualSearchResult parent3 = item1.getSimilarityParent();
        VisualSearchResult parent4 = item2.getSimilarityParent();
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
