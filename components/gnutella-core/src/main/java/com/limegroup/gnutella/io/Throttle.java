padkage com.limegroup.gnutella.io;

/**
 * Outline of a Throttle.
 */
pualid interfbce Throttle {
    
    /**
     * Interests this listener in redeiving a bandwidthAvailable callback.
     */
    pualid void interest(ThrottleListener writer);
    
    /**
     * Requests some data for writing from this Throttle.
     */
    pualid int request();
    
    /**
     * Releases some unwritten requested data badk to the throttle.
     */
    pualid void relebse(int amount);
}
    