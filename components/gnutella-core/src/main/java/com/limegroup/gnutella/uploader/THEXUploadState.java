package com.limegroup.gnutella.uploader;

import java.io.IOException;
import java.io.OutputStream;

import com.limegroup.gnutella.FileDesc;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.http.ConstantHTTPHeaderValue;
import com.limegroup.gnutella.http.HTTPHeaderValueCollection;
import com.limegroup.gnutella.http.HTTPConstants;
import com.limegroup.gnutella.http.HTTPHeaderName;
import com.limegroup.gnutella.http.HTTPMessage;
import com.limegroup.gnutella.http.HTTPUtils;
import com.limegroup.gnutella.settings.ChatSettings;
import com.limegroup.gnutella.util.ThrottledOutputStream;
import com.limegroup.gnutella.util.BandwidthThrottle;
import com.sun.java.util.collections.HashSet;
import com.sun.java.util.collections.Iterator;
import com.sun.java.util.collections.Set;

/**
 * This class calculates the TigerTree hash of any file on the fly and sends
 * the hash in compliance to the THEX proposal:
 * http://open-content.net/specs/draft-jchapweske-thex-02.html
 * 
 * @author Gregorio Roper
 */
public class THEXUploadState implements HTTPMessage {
    private final FileDesc FILE_DESC;
    private final HTTPUploader UPLOADER;

    /**
     * Throttle for the speed of THEX uploads, allow up to 0.5K/s
     */
    private static final BandwidthThrottle THROTTLE =
        new BandwidthThrottle(512);

    /**
     * Constructs a new TigerTreeUploadState
     * 
     * @param uploader
     *            the <tt>HTTPUploader</tt> that sends this message
     */
    public THEXUploadState(HTTPUploader uploader) {
        UPLOADER = uploader;
        FILE_DESC = uploader.getFileDesc();
    }

    /**
     * Write HTTP headers
     * 
     * @param os
     *            the <tt>OutputStream</tt> to write to.
     * @throws IOException
     *             if there was a problem writing to the <tt>OutputStream</tt>.
     */
    public void writeMessageHeaders(OutputStream os) throws IOException {
        String str = "HTTP/1.1 200 OK\r\n";
        os.write(str.getBytes());
        HTTPUtils.writeHeader(
            HTTPHeaderName.SERVER,
            ConstantHTTPHeaderValue.SERVER_VALUE,
            os);
        if (FILE_DESC != null) {
            // write the URN in case the caller wants it
            URN sha1 = FILE_DESC.getSHA1Urn();
            if (sha1 != null) {
                HTTPUtils.writeHeader(
                    HTTPHeaderName.GNUTELLA_CONTENT_URN,
                    FILE_DESC.getSHA1Urn(),
                    os);
            }
        }

        if (UPLOADER.isFirstReply()) {
            // write x-features header once because the downloader is
            // supposed to cache that information anyway
            Set features = new HashSet();
            features.add(ConstantHTTPHeaderValue.BROWSE_FEATURE);
            if (ChatSettings.CHAT_ENABLED.getValue())
                features.add(ConstantHTTPHeaderValue.CHAT_FEATURE);
            // Write X-Features header.
            if (features.size() > 0) {
                HTTPUtils.writeHeader(HTTPHeaderName.X_FEATURES,
                        new HTTPHeaderValueCollection(features),
                                      os);
            }
            // write X-Thex-URI header
            if (FILE_DESC.getHashTree() != null)
                HTTPUtils.writeHeader(HTTPHeaderName.X_THEX_URI,
                                      FILE_DESC.getHashTree(),
                                      os);
        }
        str = "\r\n";
        os.write(str.getBytes());
    }

    /**
     * Write HTTP message body
     * 
     * @param os
     *            the <tt>OutputStream</tt> to write to.
     * @throws IOException
     *             if there was a problem writing to the <tt>OutputStream</tt>.
     */
    public void writeMessageBody(OutputStream os) throws IOException {
        OutputStream slowStream = new ThrottledOutputStream(os, THROTTLE);
        FILE_DESC.getHashTree().getMessage().write(slowStream);
    }

    /**
     * @return <tt>true</tt> if the connection should be closed after writing
     *         the message.
     */
    public boolean getCloseConnection() {
        return false;
    }
}
