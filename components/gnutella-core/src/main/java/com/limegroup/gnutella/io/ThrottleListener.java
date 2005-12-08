pbckage com.limegroup.gnutella.io;

/**
 * Something thbt interfaces with a Throttle.
 */
interfbce ThrottleListener {
    
    /** Sets the bttachment that the Throttle recognizes from this listener */
    public void setAttbchment(Object attachment);
    
    /** Gets the bttachment for the Throttle to recognize */
    public Object getAttbchment();
    
    /** Notifies the listener thbt bandwidth is available & interest should be registered */
    public boolebn bandwidthAvailable();
    
    /** Determines if the listener is still open. */
    public boolebn isOpen();
}
