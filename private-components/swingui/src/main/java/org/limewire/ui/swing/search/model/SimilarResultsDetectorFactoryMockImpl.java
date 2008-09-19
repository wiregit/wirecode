package org.limewire.ui.swing.search.model;

import java.util.HashMap;
import java.util.Map;

import org.limewire.core.impl.search.MockSearch;

public class SimilarResultsDetectorFactoryMockImpl implements SimilarResultsDetectorFactory {
    
    @Override
    public SimilarResultsDetector newSimilarResultsDetector() {
        return new SimilarResultsDetector() {
            private Map<String, SearchResultAdapter> urnToVSRMap = new HashMap<String, SearchResultAdapter>();
            @Override
            public void detectSimilarResult(VisualSearchResult result) {
                String urn = result.getCoreSearchResults().get(0).getUrn();
                if (result instanceof SearchResultAdapter) {
                    SearchResultAdapter adapter = (SearchResultAdapter)result;
                    urnToVSRMap.put(urn, adapter);
                    if (urn.startsWith(MockSearch.SIMILAR_RESULT_PREFIX)) {
                        SearchResultAdapter parent = urnToVSRMap.get(urn.substring(MockSearch.SIMILAR_RESULT_PREFIX.length()));
                        if (parent != null) {
                            associateParentAndChild(parent, adapter);
                        }
                    } else {
                        SearchResultAdapter child = urnToVSRMap.get(MockSearch.SIMILAR_RESULT_PREFIX + urn);
                        if (child != null) {
                            associateParentAndChild(adapter, child);
                        }
                    }
                }
            }
            
            private void associateParentAndChild(SearchResultAdapter parent, SearchResultAdapter child) {
                parent.addSimilarSearchResult(child);
                child.setSimilarityParent(parent);
                child.setVisible(false);
            }
        };
    }
}
