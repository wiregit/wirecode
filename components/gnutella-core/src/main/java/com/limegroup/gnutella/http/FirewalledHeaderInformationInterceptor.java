package com.limegroup.gnutella.http;

import java.io.IOException;

import org.apache.http.Header;
import org.apache.http.HttpException;
import org.apache.http.protocol.HttpContext;
import org.limewire.http.HeaderInterceptor;

import com.limegroup.gnutella.uploader.HTTPUploader;

/**
 * Collects firewalled header information from {@link Header}s and sets them on
 * an {@link HTTPUploader}.
 */
public class FirewalledHeaderInformationInterceptor implements HeaderInterceptor {

    private final HTTPUploader uploader;

    public FirewalledHeaderInformationInterceptor(HTTPUploader uploader) {
        this.uploader = uploader;
    }
    
    public void process(Header header, HttpContext context) throws HttpException, IOException {
        readPushEndPoint(header);
    }

    void readPushEndPoint(Header header) {
    }
    
}
