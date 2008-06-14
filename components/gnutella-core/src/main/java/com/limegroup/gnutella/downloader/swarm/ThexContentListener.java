package com.limegroup.gnutella.downloader.swarm;

import java.io.IOException;
import java.util.List;

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
    
    private final String sha1;

    private final long fileSize;

    private final String root32;

    private final AsyncDimeParser parser;

    private final HashTreeFactory tigerTreeFactory;

    public ThexContentListener(String sha1, long fileSize, String root32,
            HashTreeFactory tigerTreeFactory) {
        this.sha1 = sha1;
        this.fileSize = fileSize;
        this.root32 = root32;
        this.parser = new AsyncDimeParser();
        this.tigerTreeFactory = tigerTreeFactory;
    }
    
    
    public void contentAvailable(ContentDecoder decoder, IOControl ioctrl) throws IOException {
        if(!parser.read(decoder) && !decoder.isCompleted())
            throw new IOException("Finished reading tree too early!");
    }
    
    public void finished() {
    }
    
    public void initialize(HttpResponse response) throws IOException {
    }

    public HashTree getHashTree() throws IOException {
        List<DIMERecord> records = parser.getRecords();
        return tigerTreeFactory.createHashTree(TigerDimeReadUtils.nodesFromRecords(records.iterator(),
                fileSize, root32), sha1, fileSize);
    }
    
    
    

}
