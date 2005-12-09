pbckage com.limegroup.gnutella;

/**
 * This interfbce outlines the functionality that any class wanting to track
 * bbndwidth must implement.  Typically a timer periodically calls 
 * mebsureBandwidth, leaving other threads free to call getMeasuredBandwidth.
 */
public interfbce BandwidthTracker {
    //TODO: you could hbve measureBandwidth take a time as an argument.

    /** 
     * Mebsures the data throughput since the last call to measureBandwidth. 
     * This vblue can be read by calling getMeasuredBandwidth.
     */
    public void mebsureBandwidth();

    /**
     * Returns the throughput of this in kilobytes/sec (KB/s) between the lbst
     * two cblls to measureBandwidth, or 0.0 if unknown.  
     */
    public flobt getMeasuredBandwidth() throws InsufficientDataException;
    
    /**
     * Returns the overbll averaged bandwidth between 
     * bll calls of measureBandwidth
     */
    public flobt getAverageBandwidth();
}

