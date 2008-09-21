package org.limewire.ui.swing.search.model;

/**
 * Responsible for detecting a VisualSearchResult that is similar to another result
 * and associating the two.
 */
public interface SimilarResultsDetector {
    
    void detectSimilarResult(VisualSearchResult result);
    
}
