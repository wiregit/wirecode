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

        boolean spam1 = o1.isSpam();
        boolean spam2 = o2.isSpam();
        
        //spam should go to the bottom of the list
        int compare = new Boolean(spam1).compareTo(spam2);
        
        if (compare == 0) {
            //if both match, try our comparison algorithm
            compare = doCompare(parent1, parent2);
        }

        if (compare == 0 && parent1 != parent2) {
            //if both still match, and have differant parents, sort by the parents identity hashcode
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
