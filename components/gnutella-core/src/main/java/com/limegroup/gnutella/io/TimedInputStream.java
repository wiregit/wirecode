package com.limegroup.gnutella.io;

import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.net.*;

import org.apache.commons.logging.*;

/**
 * An InputStream that wraps itself around a blocking ReadableByteChannel.
 *
 * This handles ensuring that if the read takes longer than the soTimeout,
 * it will be closed.
 */
 class TimedInputStream extends InputStream {
    
    /** the channel to read from */
    private final ReadableByteChannel channel;
    /** the socket that provides soTimeout values */
    private final Socket handler;
    
    TimedInputStream(ReadableByteChannel channel, Socket handler) {
        this.channel = channel;
        this.handler = handler;
    }
    
    public int read() throws IOException {
        byte[] buf = new byte[1];
        int read = read(buf, 0, 1);
        if(read == -1)
            return read;
        else
            return buf[0];
    }
    
    
    public int read(byte[] buf, int off, int len) throws IOException {
        ByteBuffer buffer = ByteBuffer.wrap(buf, off, len);
        return channel.read(buffer);
    }
    
    public void close() throws IOException  {
        channel.close();
    }
    
}
    