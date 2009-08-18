package org.limewire.ui.swing.search.resultpanel;

import org.limewire.core.api.search.store.StoreStyle;
import org.limewire.core.api.search.store.StoreTrackResult;
import org.limewire.ui.swing.search.model.VisualStoreResult;

/**
 * Defines an interface for mediating interactions between the store UI
 * and various core services.
 */
public interface StoreController {

    /**
     * Returns true if store results are pay-as-you-go.
     */
    boolean isPayAsYouGo();
    
    /**
     * Returns the current style for Lime Store results.
     */
    StoreStyle getStoreStyle();
    
    /**
     * Initiates downloading of the specified visual store result.
     */
    void download(VisualStoreResult vsr);
    
    /**
     * Initiates downloading of the specified store track result.
     */
    void downloadTrack(StoreTrackResult str);
    
    /**
     * Initiates streaming of the specified visual store result.
     */
    void stream(VisualStoreResult vsr);
    
    /**
     * Initiates streaming of the specified store track result.
     */
    void streamTrack(StoreTrackResult str);
}
