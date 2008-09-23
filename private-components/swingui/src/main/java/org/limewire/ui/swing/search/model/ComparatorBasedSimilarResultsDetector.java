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
        if (child.getSimilarityParent() != null && child.getSimilarityParent() != parent) {
            up(child.getSimilarityParent(), parent);
        }
        
        if (child != parent) {
            up(child, parent);
        }

      
    }

    private void up(VisualSearchResult child, VisualSearchResult parent) {
        ((SearchResultAdapter) parent).setSimilarityParent(null);
        ((SearchResultAdapter) child).setSimilarityParent(parent);
        ((SearchResultAdapter) parent).addSimilarSearchResult(child);
        moveChildren(child, parent);
    }

    /**
     * Moves the children from the child to the parent.
     */
    private void moveChildren(VisualSearchResult child, VisualSearchResult parent) {
        for (VisualSearchResult item : child.getSimilarResults()) {
            up(item, parent);
            ((SearchResultAdapter) child).removeSimilarSearchResult(item);
            ((SearchResultAdapter) parent).addSimilarSearchResult(item);
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

        VisualSearchResult parent1 = item1.getSimilarityParent() == null ? item1 : item1
                .getSimilarityParent();
        VisualSearchResult parent2 = item2.getSimilarityParent() == null ? item2 : item2
                .getSimilarityParent();

        if (parent1 != null && parent2 == null) {
            parent = parent1;
        } else if (parent1 == null && parent2 != null) {
            parent = parent2;
        } else if (parent1.getCoreSearchResults().size() < parent2.getCoreSearchResults().size()) {
            parent = parent2;
        } else {
            parent = parent1;
        }

        return parent;
    }

}
