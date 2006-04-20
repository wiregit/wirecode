package com.limegroup.gnutella.http;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;

import com.limegroup.gnutella.io.BufferUtils;
import com.limegroup.gnutella.io.ReadState;

public class ReadConsumer extends ReadState {
    
    private long leftToRead;
    
    public ReadConsumer(long length) {
        this.leftToRead = length;
    }
    
    protected boolean processRead(ReadableByteChannel rc, ByteBuffer buffer) throws IOException {
        
        leftToRead = BufferUtils.delete(buffer, leftToRead);     
        int read = 0;
        while(leftToRead > 0 && (read = rc.read(buffer)) > 0)
            leftToRead = BufferUtils.delete(buffer, leftToRead);
        
        if(leftToRead > 0 && read == -1)
            throw new IOException("EOF");
        else
            return leftToRead > 0; // requires more processing if still stuff to read.
    }

    public long getAmountProcessed() {
        return -1;
    }

}
