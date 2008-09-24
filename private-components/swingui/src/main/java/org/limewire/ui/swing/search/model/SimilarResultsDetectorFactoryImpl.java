package org.limewire.ui.swing.search.model;

public class SimilarResultsDetectorFactoryImpl implements SimilarResultsDetectorFactory {
    public SimilarResultsDetector newSimilarResultsDetector() {
        return new SimilarResultsMatchingDetector(new NameMatcher());
    }
}
