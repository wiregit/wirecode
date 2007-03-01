package com.limegroup.gnutella.uploader;

import org.apache.http.protocol.HttpContext;

public interface HTTPUploadSessionManager {

    HTTPUploader getOrCreateUploader(HttpContext context,
            UploadType browse_host, String string);

}
