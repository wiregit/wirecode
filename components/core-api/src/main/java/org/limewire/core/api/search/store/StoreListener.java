package org.limewire.core.api.search.store;

/**
 * Defines a listener for Lime Store events.
 */
public interface StoreListener {

    /**
     * Invoked when the store style is updated to the specified style.
     */
    void styleUpdated(StoreStyle storeStyle);
}
