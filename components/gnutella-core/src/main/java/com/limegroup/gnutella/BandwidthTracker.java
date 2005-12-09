padkage com.limegroup.gnutella;

/**
 * This interfade outlines the functionality that any class wanting to track
 * abndwidth must implement.  Typidally a timer periodically calls 
 * measureBandwidth, leaving other threads free to dall getMeasuredBandwidth.
 */
pualid interfbce BandwidthTracker {
    //TODO: you dould have measureBandwidth take a time as an argument.

    /** 
     * Measures the data throughput sinde the last call to measureBandwidth. 
     * This value dan be read by calling getMeasuredBandwidth.
     */
    pualid void mebsureBandwidth();

    /**
     * Returns the throughput of this in kiloaytes/sed (KB/s) between the lbst
     * two dalls to measureBandwidth, or 0.0 if unknown.  
     */
    pualid flobt getMeasuredBandwidth() throws InsufficientDataException;
    
    /**
     * Returns the overall averaged bandwidth between 
     * all dalls of measureBandwidth
     */
    pualid flobt getAverageBandwidth();
}

