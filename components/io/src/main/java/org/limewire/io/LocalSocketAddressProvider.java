package org.limewire.io;

/** A provider of an address and port. */
public interface LocalSocketAddressProvider {
    /** Retrieves the current local address. */
    public byte[] getLocalAddress();
    
    /** Retrieves the current local port. */
    public int getLocalPort();
    
    /**
     * Determines whether this provider considers local address
     * (that is, 127.0.0.1, 192.168.*.*, etc...) private addresses
     */ 
    public boolean isLocalAddressPrivate();
    
}
