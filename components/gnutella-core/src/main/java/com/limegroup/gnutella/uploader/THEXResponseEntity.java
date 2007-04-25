/**
 * 
 */
package com.limegroup.gnutella.uploader;

import java.io.IOException;

import org.limewire.http.AbstractHttpNIOEntity;
import org.limewire.nio.NBThrottle;

import com.limegroup.gnutella.settings.UploadSettings;
import com.limegroup.gnutella.tigertree.HashTree;
import com.limegroup.gnutella.tigertree.ThexWriter;

/**
 * Sends a THEX tree as an HTTP message.
 *
 * The tree is in compliance with the THEX protocol at
 * http://open-content.net/specs/draft-jchapweske-thex-02.html
 */
public class THEXResponseEntity extends AbstractHttpNIOEntity {

    /**
     * Throttle for the speed of THEX uploads, allow up to 0.5K/s
     */
    private static final NBThrottle THROTTLE =
        new NBThrottle(true, UploadSettings.THEX_UPLOAD_SPEED.getValue());

    private HTTPUploader uploader;

    private HashTree tree;

    private ThexWriter writer;

    private long size;

    public THEXResponseEntity(HTTPUploader uploader, HashTree tree, long size) {
        this.uploader = uploader;
        this.tree = tree;
        this.size = size;

        setContentType(tree.getOutputType());
    }

    @Override
    public long getContentLength() {
        return size;
    }

    @Override
    public void initialize() throws IOException {
        this.writer = tree.createAsyncWriter();
        
        THROTTLE.setRate(UploadSettings.THEX_UPLOAD_SPEED.getValue());
        uploader.getSession().getIOSession().setThrottle(THROTTLE);
    }

    @Override
    public boolean handleWrite() throws IOException {
        boolean more = writer.process(this, null);
        writer.getAmountProcessed();
        uploader.setAmountUploaded(writer.getAmountProcessed());
        return more;
    }

    @Override
    public void finished() {
        this.writer = null;
    }

}