package org.limewire.core.api.connection;

/** The strength of your Gnutella connection. */
public enum ConnectionStrength {
    /** You aren't connected to the internet at all. */
    NO_INTERNET,
    
    /** You might be connected to the internet, but not connected to Gnutella. */
    DISCONNECTED,
    
    /** You are attempting to connect to Gnutella. */
    CONNECTING,
    
    /** You have a week connection to Gnutella. */
    WEAK,
    
    /** Your connection to Gnutella is OK, but could be better. */
    MEDIUM, 
    
    /** You are fully connected to Gnutella. */
    FULL, 
    
    /** You have a kickass connection to Gnutella. */
    TURBO;
}