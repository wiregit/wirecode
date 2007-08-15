package com.limegroup.gnutella;

/**
 * This interface outlines the functionality that any class wanting to track
 * bandwidth must implement.  Typically a timer periodically calls 
 * measureBandwidth, leaving other threads free to call getMeasuredBandwidth.
 */
public interface BandwidthTracker {
    //TODO: you could have measureBandwidth take a time as an argument.

    /** 
     * Measures the data throughput since the last call to measureBandwidth. 
     * This value can be read by calling getMeasuredBandwidth.
     */
    public void measureBandwidth();

    /**
     * Returns the throughput of this in kilobytes/sec (KB/s) between the last
     * two calls to measureBandwidth, or 0.0 if unknown.  
     */
    public float getMeasuredBandwidth() throws InsufficientDataException;
    
    /**
     * Returns the overall averaged bandwidth between 
     * all calls of measureBandwidth
     */
    public float getAverageBandwidth();
}

