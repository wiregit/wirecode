package com.limegroup.gnutella.connection;



import com.limegroup.gnutella.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.io.*;
import java.util.*;
import java.net.*;
import java.util.zip.*;

public class InflaterReader implements WritableByteChannel {
    
    private ByteBuffer buffer;
    private WritableByteChannel chain;
    private Inflater inflater;
    
    public InflaterReader(Inflater inflater, WritableByteChannel channel) {
        this.chain = channel;
        this.inflater = inflater;
        buffer = ByteBuffer.wrap(new byte[512]);
    }
    
    /**
     * Writes data into this' internal buffer, attempting to inflate it and pass it on.
     */
    public int write(ByteBuffer inBuffer) throws IOException {
        
        byte[] data = inBuffer.array();
        int off = inBuffer.position();
        int limit = inBuffer.limit();
        int len = limit - off;
        inflater.setInput(data, off, len);
        inBuffer.position(limit); // we used the buffer in that setInput operation.
        
        // now try to inflate & pass it on to the chain.
        byte[] inflated = buffer.array();
        while(true) {
            int read;
            try {
                read = inflater.inflate(inflated);
            } catch(DataFormatException dfe) {
                IOException x = new IOException();
                x.initCause(dfe);
                throw x;
            }
            
            if(read == 0)
                break;
                
            buffer.position(0);
            buffer.limit(read);
            int wrote = chain.write(buffer);
            if(wrote != read)
                throw new IllegalStateException("expected chain to gobble all data.");
        }
        
        return len;
    }
    
    /**
     * Determines if this reader is open.
     */
    public boolean isOpen() {
        return chain.isOpen();
    }
    
    /**
     * Closes this channel.
     */
    public void close() throws IOException {
        chain.close();
    }
}
    
    