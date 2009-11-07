package org.limewire.core.impl.search.store;

import java.util.List;

import org.limewire.core.api.search.store.StoreResults;
import org.limewire.core.api.search.store.StoreResult;

public class MockStoreResults implements StoreResults {
    
    private final String renderStyle;
    private final List<StoreResult> storeResults;

    public MockStoreResults(String renderStyle, List<StoreResult> storeResults) {
        this.renderStyle = renderStyle;
        this.storeResults = storeResults;
    }

    @Override
    public String getRenderStyle() {
        return renderStyle;
    }

    @Override
    public List<StoreResult> getItems() {
        return storeResults;
    }
}
