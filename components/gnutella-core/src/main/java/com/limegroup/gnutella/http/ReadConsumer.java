package com.limegroup.gnutella.http;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.ReadableByteChannel;

import com.limegroup.gnutella.io.IOState;

public class ReadConsumer implements IOState {
    
    private long leftToRead;
    
    public ReadConsumer(long length) {
        this.leftToRead = length;
    }

    public boolean isReading() {
        return true;
    }

    public boolean isWriting() {
        return false;
    }
    
    private void clean(ByteBuffer buffer, int keep) {
        // assume keep is 3, we want to ditch ABCD but keep EFG
        // now: [ABCDEFGX   ] where X is position, ] is limit & capacity
        buffer.flip();
        // now: [XBCDEFGY   ] where X is position, Y is limit, ] is capacity 
        buffer.position(buffer.limit() - keep);
        // now: [ABCDXFGY   ] where X is position, Y is limit, ] is capacity   
        buffer.compact();
        // now: [EFGX       ] where X is position, Y is limit, ] is capacity        
    }

    public boolean process(Channel channel, ByteBuffer buffer) throws IOException {
        ReadableByteChannel rc = (ReadableByteChannel)channel;
        
        // Need to work through the buffer in two stages.
        // #1, clear what already is in there,
        // #2, clear what we can read off the network.
        
        if(buffer.position() <= leftToRead) {
            leftToRead -= buffer.position();
            buffer.clear();
        } else {
            clean(buffer, (int)leftToRead - buffer.position());
        }
        
        int read = 0;
        while(leftToRead > 0 && (read = rc.read(buffer)) > 0) {
            leftToRead -= read;
            if(leftToRead >= 0) { // can completely empty the buffer if we didn't read too much
                buffer.clear();
            } else {
                int keep = (int)leftToRead * -1;
                clean(buffer, keep);
            }
        }
        
        return leftToRead > 0; // requires more processing if still stuff to read.
        
    }

}
