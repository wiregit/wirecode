package org.limewire.ui.swing.search.resultpanel;

import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.search.store.StoreManager;
import org.limewire.core.api.search.store.StoreStyle;
import org.limewire.core.api.search.store.StoreTrackResult;
import org.limewire.ui.swing.search.model.SearchResultsModel;
import org.limewire.ui.swing.search.model.VisualStoreResult;
import org.limewire.ui.swing.search.resultpanel.list.DefaultListStoreStyle;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

/**
 * A class that mediates interactions between the Store UI and various core
 * services.
 */
public class StoreController {

    private final SearchResultsModel searchResultsModel;
    private final StoreManager storeManager;
    
    /**
     * Constructs a StoreController using the specified services.
     */
    @Inject
    public StoreController(
            @Assisted SearchResultsModel searchResultsModel,
            StoreManager storeManager) {
        this.searchResultsModel = searchResultsModel;
        this.storeManager = storeManager;
    }
    
    /**
     * Returns true if store results are pay-as-you-go.
     */
    public boolean isPayAsYouGo() {
        // TODO also return true if user owns pay-as-you-go account
        return !storeManager.isLoggedIn();
    }

    /**
     * Returns the current style for Lime Store results.
     */
    public StoreStyle getStoreStyle() {
        StoreStyle storeStyle = searchResultsModel.getStoreStyle();
        if (storeStyle == null) {
            storeStyle = new DefaultListStoreStyle();
        }
        return storeStyle;
    }
    
    /**
     * Initiates downloading of the specified visual store result.
     */
    public void download(VisualStoreResult vsr) {
        // TODO implement
        System.out.println("StoreController.download: " + vsr.getHeading());
    }

    /**
     * Initiates downloading of the specified store track result.
     */
    public void downloadTrack(StoreTrackResult str) {
        // TODO implement
        System.out.println("StoreController.downloadTrack: " + str.getProperty(FilePropertyKey.NAME));
    }

    /**
     * Initiates streaming of the specified visual store result.
     */
    public void stream(VisualStoreResult vsr) {
        // TODO implement
        System.out.println("StoreController.stream: " + vsr.getHeading());
    }

    /**
     * Initiates streaming of the specified store track result.
     */
    public void streamTrack(StoreTrackResult str) {
        // TODO implement
        System.out.println("StoreController.streamTrack: " + str.getProperty(FilePropertyKey.NAME));
    }
}
