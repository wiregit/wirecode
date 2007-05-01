package com.limegroup.gnutella;

import java.net.InetAddress;

import com.limegroup.gnutella.uploader.UploadSlotManager;

public interface UploadManager extends BandwidthTracker {
    
    float getLastMeasuredBandwidth();

    int getNumQueuedUploads();

    int uploadsInProgress();

    /**
     * Returns true if this has ever successfully uploaded a file during this
     * session.
     * <p>
     * This method was added to adopt more of the BearShare QHD standard.
     */
    boolean hadSuccesfulUpload();

    /**
     * Returns whether there are any active internet (non-multicast) transfers
     * going at speed greater than 0.
     */
    boolean hasActiveInternetTransfers();

    boolean isConnectedTo(InetAddress addr);

    /**
     * Returns whether or not an upload request can be serviced immediately. In
     * particular, if there are more available upload slots than queued uploads
     * this will return true.
     */
    boolean isServiceable();

    /**
     * Stops all uploads that are uploading <code>fd</code>.
     */
    boolean killUploadsForFileDesc(FileDesc fd);

    /**
     * Returns if an incoming query (not actual upload request) may be
     * serviceable.
     */
    boolean mayBeServiceable();

    /**
     * Returns the estimated upload speed in <b>KILOBITS/s</b> [sic] of the
     * next transfer, assuming the client (i.e., downloader) has infinite
     * bandwidth. Returns -1 if not enough data is available for an accurate
     * estimate.
     */
    int measuredUploadSpeed();
    
    UploadSlotManager getSlotManager();

}
