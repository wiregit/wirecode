package com.limegroup.gnutella.http;

import java.io.IOException;

import org.apache.http.Header;
import org.apache.http.HttpException;
import org.apache.http.protocol.HttpContext;
import org.limewire.http.HeaderInterceptor;
import org.limewire.io.NetworkUtils;

import com.limegroup.gnutella.GUID;
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
        if (readPushProxies(header))
            ;
        else if (readClientGUID(header))
            ;
        else if (readFWTPort(header))
            ;
    }

    boolean readFWTPort(Header header) {
        if (HTTPHeaderName.FWTPORT.matches(header)) {
            try {
                int port = Integer.parseInt(header.getValue());
                if (NetworkUtils.isValidPort(port)) {
                    uploader.setFWTPort(port);
                }
            } catch (NumberFormatException nfe) { }
            return true;
        } else {
            return false;
        }
    }

    boolean readClientGUID(Header header) {
        if (HTTPHeaderName.CLIENT_GUID.matches(header)) {
            String guidHex = header.getValue();
            try {
                byte[] guid = GUID.fromHexString(guidHex);
                uploader.setClientGUID(guid);
            } catch (IllegalArgumentException iae) { }
            return true;
        } else {
            return false;
        }
    }

    boolean readPushProxies(Header header) {
        return false;
    }

}
