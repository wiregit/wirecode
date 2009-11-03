package org.limewire.core.api.download;

/**
 * Stores details for an active download peer;  
 */
public interface DownloadSourceInfo {
    
    /**
     * Returns this peers ip address.
     */
    String getIPAddress();
    
    /**
     * Whether the peer connection is encypted or not.
     */
    boolean isEncyrpted();
    
    /**
     * The name of the peer's client.
     */
    String getClientName();
    
    /**
     * Returns the current total upload speed to this peer in bytes/sec.
     */
    float getUploadSpeed();
    
    /**
     * Returns the current total download speed from this peer in bytes/sec.
     */
    float getDownloadSpeed();
}
