package org.limewire.nio.ssl;

/**
 * Defines an interface to allow implementations to query the produced/consumed 
 * statistics of an <code>SSLEngine</code> without having access to the 
 * engine, or knowing what is causing the statistical changes.
 */
public interface SSLBandwidthTracker {

    /** Returns the total number of bytes that this has produced from unwrapping reads. */
    public long getReadBytesProduced();
    
    /** Returns the total number of bytes that this has consumed while unwrapping reads. */
    public long getReadBytesConsumed();
    
    /** Returns the total number of bytes that this has produced from wrapping writes. */
    public long getWrittenBytesProduced();
    
    /** Returns the total number of bytes that this has consumed while wrapping writes. */
    public long getWrittenBytesConsumed();
    
}
