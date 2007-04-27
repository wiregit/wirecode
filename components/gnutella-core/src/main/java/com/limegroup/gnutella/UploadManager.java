package com.limegroup.gnutella;

import java.net.InetAddress;

import com.limegroup.gnutella.uploader.UploadSlotManager;

public interface UploadManager extends BandwidthTracker {
    
    float getLastMeasuredBandwidth();

    int getNumQueuedUploads();

    int uploadsInProgress();

    boolean hadSuccesfulUpload();

    boolean hasActiveInternetTransfers();

    boolean isConnectedTo(InetAddress addr);

    boolean isServiceable();

    boolean killUploadsForFileDesc(FileDesc fd);

    boolean mayBeServiceable();

    int measuredUploadSpeed();
    
    UploadSlotManager getSlotManager();

}
