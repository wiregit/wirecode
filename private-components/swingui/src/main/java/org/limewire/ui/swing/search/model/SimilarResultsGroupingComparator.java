package org.limewire.ui.swing.search.model;

import java.util.Comparator;

public abstract class SimilarResultsGroupingComparator<T extends Comparable<? super T>> implements Comparator<VisualSearchResult> {

    @Override
    public int compare(VisualSearchResult o1, VisualSearchResult o2) {
        VisualSearchResult parent1 = o1.getSimilarityParent();
        VisualSearchResult parent2 = o2.getSimilarityParent();

        if (parent1 == o2) return 1;
        if (parent2 == o1) return -1;
        
        T key1 = getSecondaryComparable(parent1 == null ? o1 : parent1);
        T key2 = getSecondaryComparable(parent2 == null ? o2 : parent2);
        
        return key1.compareTo(key2);
     }

    protected abstract T getSecondaryComparable(VisualSearchResult result);
 }
