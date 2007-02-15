package com.limegroup.gnutella;

import java.net.InetAddress;
import java.net.Socket;

import com.limegroup.gnutella.http.HTTPRequestMethod;
import com.limegroup.gnutella.uploader.HTTPSession;

public interface UploadManager extends BandwidthTracker {
    
    /**
     * Constant for HttpRequestLine parameter
     */
    final String SERVICE_ID = "service_id";

    void acceptUpload(HTTPRequestMethod get, Socket socket, boolean lan);

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

}
