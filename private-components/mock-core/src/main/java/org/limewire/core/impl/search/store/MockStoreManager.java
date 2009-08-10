package org.limewire.core.impl.search.store;

import java.util.concurrent.CopyOnWriteArrayList;

import org.limewire.core.api.search.store.StoreListener;
import org.limewire.core.api.search.store.StoreManager;
import org.limewire.core.api.search.store.StoreStyle;
import org.limewire.core.api.search.store.StoreStyle.Type;

import com.google.inject.Singleton;

/**
 * Implementation of StoreManager for the mock core.
 */
@Singleton
public class MockStoreManager implements StoreManager {

    private final CopyOnWriteArrayList<StoreListener> listeners = 
        new CopyOnWriteArrayList<StoreListener>();
    
    private volatile StoreStyle storeStyle;

    @Override
    public void addStoreListener(StoreListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeStoreListener(StoreListener listener) {
        listeners.add(listener);
    }
    
    @Override
    public StoreStyle getStoreStyle() {
        return storeStyle;
    }
    
    @Override
    public void loadStoreStyle() {
        if (storeStyle == null) {
            new Thread(new Runnable() {
                public void run() {
                    try {
                        Thread.sleep(10000);
                    } catch (InterruptedException e) {}

                    storeStyle = new MockStoreStyle(Type.STYLE_A);
                    
                    fireStyleUpdated(storeStyle);
                }
            }).start();
        }
    }
    
    private void fireStyleUpdated(StoreStyle storeStyle) {
        for (StoreListener listener : listeners) {
            listener.styleUpdated(storeStyle);
        }
    }
}
