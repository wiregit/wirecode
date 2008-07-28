package org.limewire.core.api.browse;

public interface Browse {
    /** Starts the search. */
    void start(BrowseListener searchListener);
    
    /** Stops the search. */
    void stop();
}
