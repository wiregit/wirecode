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
        for (VisualSearchResult test : results) {
            if (test != result && searchResultComparator.compare(test, result) == 0) {
                update(result, test);
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
        }
    }

    private void updateParent(VisualSearchResult updateItem, VisualSearchResult parent) {
        if (updateItem != parent) {
            ((SearchResultAdapter) updateItem).setSimilarityParent(parent);
            ((SearchResultAdapter) parent).addSimilarSearchResult(updateItem);
            moveChildren(updateItem, parent);
        }
    }

    private void moveChildren(VisualSearchResult updateItem, VisualSearchResult parent) {
        for (VisualSearchResult res : updateItem.getSimilarResults()) {
            ((SearchResultAdapter) updateItem).removeSimilarSearchResult(res);
            ((SearchResultAdapter) parent).addSimilarSearchResult(res);
        }
    }

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
