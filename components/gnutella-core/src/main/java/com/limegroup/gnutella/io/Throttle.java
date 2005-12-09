pbckage com.limegroup.gnutella.io;

/**
 * Outline of b Throttle.
 */
public interfbce Throttle {
    
    /**
     * Interests this listener in receiving b bandwidthAvailable callback.
     */
    public void interest(ThrottleListener writer);
    
    /**
     * Requests some dbta for writing from this Throttle.
     */
    public int request();
    
    /**
     * Relebses some unwritten requested data back to the throttle.
     */
    public void relebse(int amount);
}
    