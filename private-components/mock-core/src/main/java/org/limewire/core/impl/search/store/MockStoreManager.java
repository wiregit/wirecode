package org.limewire.core.impl.search.store;

import org.limewire.core.api.search.store.StoreManager;
import org.limewire.core.api.search.store.StoreStyle;
import org.limewire.core.api.search.store.StoreStyle.Type;

/**
 * Implementation of StoreManager for the mock core.
 */
public class MockStoreManager implements StoreManager {

    private StoreStyle storeStyle = new MockStoreStyle(Type.STYLE_A);
    
    @Override
    public StoreStyle getStoreStyle() {
        return storeStyle;
    }

}
