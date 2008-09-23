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
            if (existingResult != result
                    && searchResultComparator.compare(existingResult, result) == 0) {
                update(result, existingResult);
                break;
            }
        }
    }

    private void update(VisualSearchResult addedItem, VisualSearchResult existingItem) {
        VisualSearchResult parent = findParent(addedItem, existingItem);

        if (parent == existingItem) {
            updateParent(addedItem, parent);
        } else {
            updateParent(existingItem, parent);
            updateVisibility(existingItem, parent);
        }
    }

    /**
     * Update visibilities of newly changed parents.
     */
    private void updateVisibility(VisualSearchResult existingItem, VisualSearchResult parent) {
        parent.setChildrenVisible(existingItem.isChildrenVisible());
        existingItem.setChildrenVisible(false);
    }

    /**
     * Updates the updateItem to use the given parent. The parent is set, the
     * children are moved, and the visibility is copied.
     */
    private void updateParent(VisualSearchResult updateItem, VisualSearchResult parent) {
        if (updateItem != parent) {
            ((SearchResultAdapter) updateItem).setSimilarityParent(parent);
            ((SearchResultAdapter) parent).addSimilarSearchResult(updateItem);
            moveChildren(updateItem, parent);
        }
    }

    /**
     * Moves the children from the udateItem to the parent.
     */
    private void moveChildren(VisualSearchResult updateItem, VisualSearchResult parent) {
        for (VisualSearchResult res : updateItem.getSimilarResults()) {
            ((SearchResultAdapter) updateItem).removeSimilarSearchResult(res);
            ((SearchResultAdapter) parent).addSimilarSearchResult(res);
        }
    }

    /**
     * Returns which item should be the parent between the two similar search
     * results. Currently the item with the most core results, is considered the
     * parent item
     */
    private VisualSearchResult findParent(VisualSearchResult addedItem,
            VisualSearchResult existingItem) {
        VisualSearchResult parent = existingItem.getSimilarityParent();
        if (parent == null) {
            parent = existingItem;
        }
        if (parent.getCoreSearchResults().size() < addedItem.getCoreSearchResults().size()) {
            parent = addedItem;
        }
        return parent;
    }

}
