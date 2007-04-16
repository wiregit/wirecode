/**
 * 
 */
package com.limegroup.gnutella.uploader;

import java.io.IOException;

import org.limewire.http.AbstractHttpNIOEntity;

import com.limegroup.gnutella.Uploader;
import com.limegroup.gnutella.tigertree.HashTree;
import com.limegroup.gnutella.tigertree.ThexWriter;

/**
 * Sends a THEX tree as an HTTP message.
 *
 * The tree is in compliance with the THEX protocol at
 * http://open-content.net/specs/draft-jchapweske-thex-02.html
 */
public class THEXResponseEntity extends AbstractHttpNIOEntity {

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
        uploader.setState(Uploader.THEX_REQUEST);
    }

    @Override
    public boolean handleWrite() throws IOException {
        boolean more = writer.process(this, null);
        // TODO uploader.addAmountUploaded(...);
        return more;
    }

    @Override
    public void finished() throws IOException {
    }

}