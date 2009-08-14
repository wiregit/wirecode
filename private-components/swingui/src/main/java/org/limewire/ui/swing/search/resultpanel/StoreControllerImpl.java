package org.limewire.ui.swing.search.resultpanel;

import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.search.store.StoreManager;
import org.limewire.core.api.search.store.StoreStyle;
import org.limewire.core.api.search.store.StoreTrackResult;
import org.limewire.ui.swing.search.model.VisualStoreResult;
import org.limewire.ui.swing.search.resultpanel.list.DefaultListStoreStyle;

import com.google.inject.Inject;

/**
 * Default implementation of StoreController. 
 */
public class StoreControllerImpl implements StoreController {

    private final StoreManager storeManager;
    
    /**
     * Constructs a StoreController using the specified services.
     */
    @Inject
    public StoreControllerImpl(StoreManager storeManager) {
        this.storeManager = storeManager;
    }
    
    @Override
    public boolean isLoggedIn() {
        return storeManager.isLoggedIn();
    }

    @Override
    public StoreStyle getStoreStyle() {
        StoreStyle storeStyle = storeManager.getStoreStyle();
        if (storeStyle == null) {
            storeStyle = new DefaultListStoreStyle();
        }
        return storeStyle;
    }
    
    @Override
    public void download(VisualStoreResult vsr) {
        // TODO implement
        System.out.println("StoreController.download: " + vsr.getHeading());
    }

    @Override
    public void downloadTrack(StoreTrackResult str) {
        // TODO implement
        System.out.println("StoreController.downloadTrack: " + str.getProperty(FilePropertyKey.NAME));
    }

    @Override
    public void stream(VisualStoreResult vsr) {
        // TODO implement
        System.out.println("StoreController.stream: " + vsr.getHeading());
    }

    @Override
    public void streamTrack(StoreTrackResult str) {
        // TODO implement
        System.out.println("StoreController.streamTrack: " + str.getProperty(FilePropertyKey.NAME));
    }
}
