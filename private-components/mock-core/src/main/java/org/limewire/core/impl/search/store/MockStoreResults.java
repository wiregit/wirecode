package org.limewire.core.impl.search.store;

import java.util.List;

import org.limewire.core.api.search.store.StoreResults;
import org.limewire.core.api.search.store.ReleaseResult;

public class MockStoreResults implements StoreResults {
    
    private final String renderStyle;
    private final List<ReleaseResult> releaseResults;

    public MockStoreResults(String renderStyle, List<ReleaseResult> releaseResults) {
        this.renderStyle = renderStyle;
        this.releaseResults = releaseResults;
    }

    @Override
    public String getRenderStyle() {
        return renderStyle;
    }

    @Override
    public List<ReleaseResult> getResults() {
        return releaseResults;
    }
}
