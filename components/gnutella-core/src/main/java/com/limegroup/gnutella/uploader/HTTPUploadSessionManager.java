package com.limegroup.gnutella.uploader;

import java.io.IOException;

import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.protocol.HttpContext;

public interface HTTPUploadSessionManager {
    
    enum QueueStatus { UNKNOWN, BYPASS, REJECTED, QUEUED, ACCEPTED, BANNED };

    HTTPUploader getOrCreateUploader(HttpRequest request, HttpContext context,
            UploadType type, String filename);

    QueueStatus enqueue(HttpContext context, HttpRequest request, HttpResponse response);

    void addAcceptedUploader(HTTPUploader uploader);

    void addToGUI(HTTPUploader uploader);

    void handleFreeLoader(HttpRequest request, HttpResponse response,
            HttpContext context, HTTPUploader uploader) throws HttpException, IOException;
    
}
