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
import com.limegroup.gnutella.tigertree.HashTree;
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
    private final HashTree TREE;

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
        TREE = FILE_DESC.getHashTree();
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

        // write the URN in case the caller wants it
        HTTPUtils.writeHeader(
            HTTPHeaderName.GNUTELLA_CONTENT_URN,
            FILE_DESC.getSHA1Urn(),
            os);

        HTTPUtils.writeHeader(
            HTTPHeaderName.CONTENT_LENGTH,
            TREE.getOutputLength(),
            os);        
        
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
        TREE.write(slowStream);
    }

    /**
     * @return <tt>true</tt> if the connection should be closed after writing
     *         the message.
     */
    public boolean getCloseConnection() {
        return false;
    }
}
