package com.limegroup.gnutella.downloader.swarm;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.nio.ContentDecoder;
import org.apache.http.nio.IOControl;
import org.limewire.swarm.http.listener.ResponseContentListener;

import com.limegroup.gnutella.dime.AsyncDimeParser;
import com.limegroup.gnutella.dime.DIMERecord;
import com.limegroup.gnutella.tigertree.HashTree;
import com.limegroup.gnutella.tigertree.HashTreeFactory;
import com.limegroup.gnutella.tigertree.dime.TigerDimeReadUtils;

public class ThexContentListener implements ResponseContentListener {
    
    private static final Log LOG = LogFactory.getLog(ThexContentListener.class);
    
    private final String sha1;

    private final long fileSize;

    private final String root32;

    private final AsyncDimeParser parser;

    private final HashTreeFactory tigerTreeFactory;
    
    private long expectedSize = -1;

    public ThexContentListener(String sha1, long fileSize, String root32,
            HashTreeFactory tigerTreeFactory) {
        this.sha1 = sha1;
        this.fileSize = fileSize;
        this.root32 = root32;
        this.parser = new AsyncDimeParser();
        this.tigerTreeFactory = tigerTreeFactory;
    }
    
    
    public void contentAvailable(ContentDecoder decoder, IOControl ioctrl) throws IOException {
        // Either we actively need to read more, or we're completely done reading...
        if(parser.read(decoder) || decoder.isCompleted())
            return;

        LOG.warn("Parser is done, decoder isn't!");
        
        // Unknown content-length, but parser says we're done -> manually finish
        if(expectedSize < 0) {
            LOG.warn("No known content-length, can't skip extra data");
            // TODO: decoder.setCompleted(true);
            throw new IOException("Parser is finished, decoder isn't... and isn't going to be.");
        } else {
            LOG.warn("Content length known, skipping extra data");
            ByteBuffer tmp = ByteBuffer.allocate(1024);
            while (decoder.read(tmp) > 0) {
                tmp.clear();
            }
        }
    }
    
    public void finished() {
    }
    
    public void initialize(HttpResponse response) throws IOException {
        expectedSize = response.getEntity().getContentLength();
    }

    public HashTree getHashTree() throws IOException {
        List<DIMERecord> records = parser.getRecords();
        return tigerTreeFactory.createHashTree(TigerDimeReadUtils.nodesFromRecords(records.iterator(),
                fileSize, root32), sha1, fileSize);
    }
    
    
    

}
