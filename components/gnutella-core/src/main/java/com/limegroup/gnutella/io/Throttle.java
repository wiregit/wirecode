package com.limegroup.gnutella.io;

/**
 * Outline of a Throttle.
 */
public interface Throttle {
    
    /**
     * Interests this listener in receiving Throttle events.
     */
    public void interest(ThrottleListener writer, Object attachment);
    
    /**
     * Requests some data for writing from this Throttle.
     */
    public int request(ThrottleListener writer, Object attachment);
    
    /**
     * Releases some unwritten requested data back to the throttle.
     *
     * If everything was written, wroteAll is true.
     */
    public void release(int amount, boolean wroteAll, ThrottleListener writer, Object attachment);
}
    