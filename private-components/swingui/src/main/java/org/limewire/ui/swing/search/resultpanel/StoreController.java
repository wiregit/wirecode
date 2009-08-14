package org.limewire.ui.swing.search.resultpanel;

import org.limewire.core.api.search.store.StoreStyle;
import org.limewire.core.api.search.store.StoreTrackResult;
import org.limewire.ui.swing.search.model.VisualStoreResult;

/**
 * Defines an interface for handling store requests.
 */
public interface StoreController {

    /**
     * Returns true if the user is logged in to the store.
     */
    boolean isLoggedIn();
    
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
