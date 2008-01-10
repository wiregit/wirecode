package com.limegroup.gnutella.downloader.serial.conversion;

import com.limegroup.gnutella.URN;

class SerialInNetworkDownloader extends SerialManagedDownloader {
    private static final long serialVersionUID = -7785186190441081641L;
    
    /** The size of the completed file. */    
    private long size;
    
    /** The URN to persist throughout sessions, even if no RFDs are remembered. */
    private URN urn;
    
    /** The TigerTree root for this download. */
    private String ttRoot;
    
    /** The number of times we have attempted this download */
    private int downloadAttempts;
    
    /** The time we created this download */
    private long startTime;

    public long getSize() {
        return size;
    }

    public URN getUrn() {
        return urn;
    }

    public String getTtRoot() {
        return ttRoot;
    }

    public int getDownloadAttempts() {
        return downloadAttempts;
    }

    public long getStartTime() {
        return startTime;
    }
    
    
    
}
