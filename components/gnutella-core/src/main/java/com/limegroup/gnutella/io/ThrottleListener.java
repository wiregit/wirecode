package com.limegroup.gnutella.io;

/**
 * Something that interfaces with a Throttle.
 */
interface ThrottleListener {
    
    /** Sets the attachment that the Throttle recognizes from this listener */
    public void setAttachment(Object attachment);
    
    /** Gets the attachment for the Throttle to recognize */
    public Object getAttachment();
    
    /** Notifies the listener that bandwidth is available & interest should be registered */
    public boolean bandwidthAvailable();
    
    /** Determines if the listener is still open. */
    public boolean isOpen();
}