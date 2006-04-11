package com.limegroup.gnutella.tigertree;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.ReadableByteChannel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.io.BufferUtils;
import com.limegroup.gnutella.io.IOState;
import com.limegroup.gnutella.io.NIODispatcher;

public class ThexReader implements IOState {
    
    private static final Log LOG = LogFactory.getLog(ThexReader.class);

    private final ByteBuffer treeBuffer;
    private final URN urn;
    private final String root32;
    private final long fileSize;
    private HashTree tree;
    
    public ThexReader(int length, URN sha1, String root32, long fileSize) {
        treeBuffer = NIODispatcher.instance().getBufferCache().getHeap(length);
        this.urn = sha1;
        this.root32 = root32;
        this.fileSize = fileSize;
    }  


    public boolean isReading() {
        return true;
    }

    public boolean isWriting() {
        return false;
    }

    public boolean process(Channel channel, ByteBuffer buffer) throws IOException {
        ReadableByteChannel rc = (ReadableByteChannel)channel;
        BufferUtils.transfer(buffer, treeBuffer);
        
        int read = 0;
        while(treeBuffer.hasRemaining() && (read = rc.read(treeBuffer)) > 0);
        
        if(!treeBuffer.hasRemaining()) {
            LOG.debug("Tree buffer full (" + new String(treeBuffer.array(), 0, treeBuffer.limit()) + "), creating hash tree.");
            ByteArrayInputStream input = new ByteArrayInputStream(treeBuffer.array(), 0, treeBuffer.limit());
            try {
                tree = HashTree.createHashTree(input, urn.httpStringValue(), root32, fileSize);
            } catch(IOException ignored) {
                LOG.warn("Error while creating hash tree", ignored);
            }
            return false;
        } else {
            if(read == -1)
                throw new IOException("unexpected EOF");
            return true;
        }
    }
    
    public HashTree getHashTree() {
        return tree;
    }

}
