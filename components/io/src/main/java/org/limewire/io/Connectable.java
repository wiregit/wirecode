package org.limewire.io;

/** Exposes all information necessary for connecting to a host. */
public interface Connectable extends IpPort {
    
    /** Determines if the host is capable of receiving incoming TLS connections. */
    boolean isTLSCapable();

}
