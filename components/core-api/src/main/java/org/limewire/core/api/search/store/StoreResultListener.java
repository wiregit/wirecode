package org.limewire.core.api.search.store;

import java.util.List;

import javax.swing.Icon;

/**
 * Defines a listener for store result update events.
 */
public interface StoreResultListener {

    /**
     * Invoked when the store style is updated to the specified style.
     */
    void albumIconUpdated(Icon icon);
    
    /**
     * Invoked when the store style is updated to the specified style.
     */
    void tracksUpdated(List<TrackResult> tracks);
}
