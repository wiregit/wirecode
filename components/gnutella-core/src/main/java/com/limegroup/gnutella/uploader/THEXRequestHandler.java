package com.limegroup.gnutella.uploader;

import java.io.IOException;

import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.limewire.http.AbstractHttpNIOEntity;

import com.limegroup.gnutella.FileDesc;
import com.limegroup.gnutella.Uploader;
import com.limegroup.gnutella.tigertree.HashTree;
import com.limegroup.gnutella.tigertree.ThexWriter;

/**
 * Sends the THEX tree as an HTTP message.
 *
 * The tree is in compliance with the THEX protocol at
 * http://open-content.net/specs/draft-jchapweske-thex-02.html
 */
public class THEXRequestHandler implements HttpRequestHandler {

    private HTTPUploader uploader;

    private FileDesc fd;

    public THEXRequestHandler(HTTPUploader uploader, FileDesc fd) {
        this.uploader = uploader;
        this.fd = fd;
    }

    public void handle(HttpRequest request, HttpResponse response,
            HttpContext context) throws HttpException, IOException {
        HashTree tree = fd.getHashTree();
        assert tree != null;

        // see CORE-174
        //response.addHeader(HTTPHeaderName.GNUTELLA_CONTENT_URN.create(fd.getSHA1Urn()));
        
        uploader.setState(Uploader.THEX_REQUEST);
        
        response.setStatusCode(HttpStatus.SC_OK);
        response.setEntity(new THEXResponseEntity(uploader, tree));
    }

    private class THEXResponseEntity extends AbstractHttpNIOEntity {

        private HTTPUploader uploader;

        private HashTree tree;

        private ThexWriter writer;

        public THEXResponseEntity(HTTPUploader uploader, HashTree tree) {
            this.uploader = uploader;
            this.tree = tree;

            setContentType(tree.getOutputType());
        }

        @Override
        public long getContentLength() {
            return tree.getOutputLength();
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

}
