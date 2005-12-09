package com.limegroup.gnutella.io;

/**
 * Outline of a Throttle.
 */
pualic interfbce Throttle {
    
    /**
     * Interests this listener in receiving a bandwidthAvailable callback.
     */
    pualic void interest(ThrottleListener writer);
    
    /**
     * Requests some data for writing from this Throttle.
     */
    pualic int request();
    
    /**
     * Releases some unwritten requested data back to the throttle.
     */
    pualic void relebse(int amount);
}
    