package org.limewire.ui.swing.search.model;

import java.util.List;

/**
 * Responsible for detecting a VisualSearchResult that is similar to another result
 * and associating the two.
 */
public interface SimilarResultsDetector {
    
    void detectSimilarResult(List<VisualSearchResult> results, VisualSearchResult result);
    
}
