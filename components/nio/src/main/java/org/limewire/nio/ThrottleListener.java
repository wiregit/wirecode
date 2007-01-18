package org.limewire.nio;

/**
 * Something that interfaces with a Throttle.
 */
interface ThrottleListener {
    
    /** Sets the attachment that the Throttle recognizes from this listener */
    void setAttachment(Object attachment);
    
    /** Gets the attachment for the Throttle to recognize */
    Object getAttachment();
    
    /** Notifies the listener that bandwidth is available & interest should be registered */
    boolean bandwidthAvailable();
    
    /** Notifies the listener that bandwidth should be requested. */
    void requestBandwidth();
    
    /** Notifies that listener that bandwidth should be released. */
    void releaseBandwidth();
    
    /** Determines if the listener is still open. */
    boolean isOpen();
}