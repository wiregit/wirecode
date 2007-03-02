package com.limegroup.gnutella.uploader;

import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.protocol.HttpContext;

public interface HTTPUploadSessionManager {

    HTTPUploader getOrCreateUploader(HttpContext context,
            UploadType type, String filename);

    void enqueue(HttpContext context, HttpRequest request, HttpResponse response);

}
