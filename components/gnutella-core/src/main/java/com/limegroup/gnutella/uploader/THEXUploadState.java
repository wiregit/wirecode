package com.limegroup.gnutella.uploader;

import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;

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
 * Sends the THEX tree as an HTTP message.
 *
 * The tree is in compliance with the THEX protocol at
 * http://open-content.net/specs/draft-jchapweske-thex-02.html
 * 
 * @author Gregorio Roper
 */
public class THEXUploadState extends UploadState {
    private final HashTree TREE;
    private final StalledUploadWatchdog WATCHDOG;

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
    public THEXUploadState(HTTPUploader uploader, StalledUploadWatchdog dog) {
    	super(uploader);
        TREE = FILE_DESC.getHashTree();
        if(TREE == null)
            throw new NullPointerException("null TREE in THEXUploadState");
        WATCHDOG = dog;
    }

    /**
     * Write HTTP headers
     * 
     * @param os
     *            the <tt>OutputStream</tt> to write to.
     * @throws IOException
     *             if there was a problem writing to the <tt>OutputStream</tt>.
     */
    public void writeMessageHeaders(OutputStream network) throws IOException {
        StringWriter os = new StringWriter();
        
        os.write("HTTP/1.1 200 OK\r\n");

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
            
        HTTPUtils.writeHeader(
            HTTPHeaderName.CONTENT_TYPE,
            TREE.getOutputType(),
            os);
        
        os.write("\r\n");
        
        WATCHDOG.activate(network);
        try {
            network.write(os.toString().getBytes());
        } finally {
            WATCHDOG.deactivate();
        }
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
        // the tree might be large, but the watchdogs allows two minutes,
        // so this is okay, since if an entire tree wasn't written in two
        // minutes, there is a problem.
        WATCHDOG.activate(os);
        try {
            TREE.write(slowStream);
        } finally {
            WATCHDOG.deactivate();
        }
    }

    /**
     * @return <tt>true</tt> if the connection should be closed after writing
     *         the message.
     */
    public boolean getCloseConnection() {
        return false;
    }
}
