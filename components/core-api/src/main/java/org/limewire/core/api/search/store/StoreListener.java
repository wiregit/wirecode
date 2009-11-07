package org.limewire.core.api.search.store;

/**
 * Defines a listener for Lime Store events.
 */
public interface StoreListener {

    /**
     * Invoked when the user logs in or out from the store.
     */
    void loginChanged(boolean loggedIn);
}
