package com.limegroup.gnutella.util;

/**
 * This class performs bandwidth throttling that doesn't block.  The 
 * disadvantage of this class is that it requires its own thread.
 */
public final class NIOBandwidthThrottle implements Runnable {
    
    /** 
     * The number of windows per second. 
     */
    private static final int TICKS_PER_SECOND = 10;
    
    /**
     * Constant for the number of milliseconds to sleep before clearing sent
     * bytes.
     */
    private static final int SLEEP_TIME = 1000/TICKS_PER_SECOND;

    /**
     * The number of bytes to send per window.
     */
    private final int BYTES_PER_TICK;
        
    /**
     * Variable for the number of bytes written during this interval.
     */
    private volatile int _bytesWritten = 0;
    
    
    /**
     * Factory method for creating new throttles.
     * 
     * @return a new <tt>NIOBandwidthThrottle</tt> instance
     * @throws IllegalArgumentException if <tt>bytesPerSecond</tt> is lower 
     *  than the number of windows per second, which would make the allowed
     *  bandwidth zero
     */
    public static NIOBandwidthThrottle createThrottle(int bytesPerSecond) {
        if(bytesPerSecond < TICKS_PER_SECOND) {
            throw new IllegalArgumentException("bytes per second too low: "+
                bytesPerSecond);
        }
        return new NIOBandwidthThrottle(bytesPerSecond);
    }
    
    /**
     * Private constructor ensures that no other class can instantiate this.
     */
    private NIOBandwidthThrottle(int bytesPerSecond) {
        BYTES_PER_TICK = bytesPerSecond/TICKS_PER_SECOND;
        Thread throttleThread = new Thread(this, "NIOBandwidthThrottle");
        throttleThread.setDaemon(true);
        throttleThread.start();
    }
    
    /**
     * Adds the specified number of bytes to the current data for bytes written
     * during this tick.
     * 
     * @param newBytes the number of bytes to add
     * @throws IllegalArgumentException if <tt>newBytes</tt> puts us over the
     *  limit for the number of bytes we can write during the current window
     */
    public void addBytesWritten(int newBytes) {
        if(newBytes+_bytesWritten > BYTES_PER_TICK) {
            throw new IllegalArgumentException("trying to add too many bytes: "+
                newBytes);
        }
        _bytesWritten += newBytes;
    }
    
    /**
     * Accessor for the number of bytes available during the current "tick".
     * 
     * @return the number of bytes available for writing during the current
     *  "tick"
     */
    public int bytesAvailable() {
        return BYTES_PER_TICK - _bytesWritten;
    }

    /**
     * Thread that continually clears the data for the number of bytes written
     * during the current "tick".
     */
    public void run() {
        while(true) {
            try {
                Thread.sleep(SLEEP_TIME);
                _bytesWritten = 0;
            } catch (InterruptedException e) {
                // this should never happen
                e.printStackTrace();
            }
        }
    }
}
