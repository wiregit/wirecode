package com.limegroup.gnutella;

import java.net.InetAddress;

import com.limegroup.gnutella.uploader.HTTPSession;
import com.limegroup.gnutella.uploader.UploadSlotManager;

public interface UploadManager extends BandwidthTracker {
    
//    void acceptUpload(HTTPRequestMethod get, Socket socket, boolean lan);

    float getLastMeasuredBandwidth();

    int getNumQueuedUploads();

    int getPositionInQueue(HTTPSession session);

    float getUploadSpeed();

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
