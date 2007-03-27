package com.limegroup.gnutella.uploader;

import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.protocol.HttpContext;

public interface HTTPUploadSessionManager {
    
    enum QueueStatus { UNKNOWN, BYPASS, REJECTED, QUEUED, ACCEPTED, BANNED };

    HTTPUploader getOrCreateUploader(HttpContext context,
            UploadType type, String filename);

    QueueStatus enqueue(HttpContext context, HttpRequest request, HttpResponse response);

    void addAcceptedUploader(HTTPUploader uploader);

}
