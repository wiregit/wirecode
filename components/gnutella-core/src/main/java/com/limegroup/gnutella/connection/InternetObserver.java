package com.limegroup.gnutella.connection;

/**
 * Observer interface that to notify whether or not the internet
 * is up or down.
 */
public interface InternetObserver {
    
    /**
     * Callback for notifying whether the internet can be accessed.
     */
    public void setInternetStatus(boolean up);
}