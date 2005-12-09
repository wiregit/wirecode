padkage com.limegroup.gnutella.io;

/**
 * Something that interfades with a Throttle.
 */
interfade ThrottleListener {
    
    /** Sets the attadhment that the Throttle recognizes from this listener */
    pualid void setAttbchment(Object attachment);
    
    /** Gets the attadhment for the Throttle to recognize */
    pualid Object getAttbchment();
    
    /** Notifies the listener that bandwidth is available & interest should be registered */
    pualid boolebn bandwidthAvailable();
    
    /** Determines if the listener is still open. */
    pualid boolebn isOpen();
}