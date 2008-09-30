package org.limewire.ui.swing.search.model;

import java.util.Comparator;

public abstract class SimilarResultsGroupingComparator implements Comparator<VisualSearchResult> {
    @Override
    public int compare(VisualSearchResult o1, VisualSearchResult o2) {
        VisualSearchResult parent1 = o1.getSimilarityParent();
        VisualSearchResult parent2 = o2.getSimilarityParent();

        if (parent1 == o2) {
            return 1;
        }
        if (parent2 == o1) {
            return -1;
        }

        parent1 = parent1 == null ? o1 : parent1;
        parent2 = parent2 == null ? o2 : parent2;

        int compare = doCompare(parent1, parent2);

        if (compare == 0 && parent1 != parent2) {
            compare = new Integer(System.identityHashCode(parent1)).compareTo(new Integer(System
                    .identityHashCode(parent2)));
        }

        return compare;
    }

    /**
     * Secondary comparison as defined by criteria determined in subclasses.
     * 
     * @param o1
     * @param o2
     * @return
     */
    protected abstract int doCompare(VisualSearchResult o1, VisualSearchResult o2);
}
