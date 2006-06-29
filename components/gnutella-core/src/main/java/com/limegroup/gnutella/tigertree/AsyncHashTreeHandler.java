package com.limegroup.gnutella.tigertree;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.util.List;

import com.limegroup.gnutella.dime.AsyncDimeParser;
import com.limegroup.gnutella.dime.DIMERecord;
import com.limegroup.gnutella.io.ReadState;

public class AsyncHashTreeHandler extends ReadState implements ThexReader {
    

    private String sha1;
    private long fileSize;
    private String root32;
    private AsyncDimeParser parser;

    public AsyncHashTreeHandler(String sha1, long fileSize, String root32) {
        this.sha1 = sha1;
        this.fileSize = fileSize;
        this.root32 = root32;
        this.parser = new AsyncDimeParser();
    }

    protected boolean processRead(ReadableByteChannel channel, ByteBuffer buffer) throws IOException {
        return parser.process(channel, buffer);
    }

    public HashTree getHashTree() throws IOException {
        List<DIMERecord> records = parser.getRecords();
        return new HashTree(HashTreeHandler.nodesFromRecords(records.iterator(), fileSize, root32), sha1, fileSize);
    }
    
    public long getAmountProcessed() {
        return parser.getAmountProcessed();
    }
}
