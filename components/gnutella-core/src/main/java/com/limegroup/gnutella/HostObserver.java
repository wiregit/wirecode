package com.limegroup.gnutella;


/**
 * Interface for any class that wishes to be notified of host consumption
 * information.
 */
public interface HostObserver {
    
    /**
     * Notification that there are no more hosts to connect to.
     */
    void noHostsToUse();
}

