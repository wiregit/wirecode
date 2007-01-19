package com.limegroup.gnutella.uploader;

import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.io.BandwidthThrottle;
import org.limewire.io.ThrottledOutputStream;

import com.limegroup.gnutella.http.ConstantHTTPHeaderValue;
import com.limegroup.gnutella.http.HTTPHeaderName;
import com.limegroup.gnutella.http.HTTPUtils;
import com.limegroup.gnutella.settings.UploadSettings;
import com.limegroup.gnutella.tigertree.HashTree;

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

    private static final Log LOG = LogFactory.getLog(THEXUploadState.class);
    
    /**
     * Throttle for the speed of THEX uploads, allow up to 0.5K/s
     */
    private static final BandwidthThrottle THROTTLE =
        new BandwidthThrottle(UploadSettings.THEX_UPLOAD_SPEED.getValue());

    /**
     * Constructs a new TigerTreeUploadState
     * 
     * @param uploader
     *            the <tt>HTTPUploader</tt> that sends this message
     */
    public THEXUploadState(HTTPUploader uploader, StalledUploadWatchdog dog) {
    	super(uploader);
    	LOG.debug("creating thex upload state");

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
    	LOG.debug("writing thex headers");
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
    	LOG.debug("writing message body");
        THROTTLE.setRate(UploadSettings.THEX_UPLOAD_SPEED.getValue());
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
