package org.limewire.ui.swing.search.model;

import org.limewire.core.api.search.store.StoreResult;
import org.limewire.core.api.search.store.StoreResult.SortPriority;

/**
 * Defines an interface for a displayed result from the Lime Store.
 */
public interface VisualStoreResult extends VisualSearchResult {

    /**
     * Returns true if tracks are displayed.
     */
    boolean isShowTracks();
    
    /**
     * Sets an indicator to determine if tracks are displayed.
     */
    void setShowTracks(boolean showTracks);
    
    /**
     * Returns the sort priority for this result.
     */
    SortPriority getSortPriority();
    
    /**
     * Returns the Lime Store result associated with this result.
     */
    StoreResult getStoreResult();
}
