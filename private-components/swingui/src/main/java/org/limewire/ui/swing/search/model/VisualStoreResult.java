package org.limewire.ui.swing.search.model;

import org.limewire.core.api.search.store.ReleaseResult;
import org.limewire.core.api.search.store.ReleaseResult.SortPriority;

/**
 * Defines an interface for a displayed result from the Lime Store.
 */
public interface VisualStoreResult extends VisualSearchResult {
    
    public static final String TRACKS = "tracks";
    public static final String ALBUM_ICON = "albumIcon";

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
    ReleaseResult getStoreResult();
}
