package org.limewire.ui.swing.search.model;

public class SimilarResultsDetectorFactoryImpl implements SimilarResultsDetectorFactory {
    public SimilarResultsDetector newSimilarResultsDetector() {
        return new SimilarResultsMatchingDetector(new SearchResultMatcher() {

            @Override
            public boolean matches(VisualSearchResult o1, VisualSearchResult o2) {
                return true;
            }
        });
    }
}
