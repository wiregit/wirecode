/**
 * 
 */
package com.limegroup.gnutella.uploader;

import java.io.IOException;

import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.protocol.HttpContext;

public class MockHTTPUploadSessionManager implements HTTPUploadSessionManager {

    public void addAcceptedUploader(HTTPUploader uploader) {            
    }

    public QueueStatus enqueue(HttpContext context, HttpRequest request) {
        return null;
    }

    public HTTPUploader getOrCreateUploader(HttpRequest request,
            HttpContext context, UploadType type, String filename) {
        return null;
    }

    public void handleFreeLoader(HttpRequest request,
            HttpResponse response, HttpContext context,
            HTTPUploader uploader) throws HttpException, IOException {
    }

    public void sendResponse(HTTPUploader uploader, HttpResponse response) {
    }
    
}