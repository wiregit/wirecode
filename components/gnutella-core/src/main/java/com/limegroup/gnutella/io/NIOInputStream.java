package com.limegroup.gnutella.io;


import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.net.*;

import org.apache.commons.logging.*;

/**
 * A blocking inputstream that uses non-blocking I/O
 * as its core.
 *
 * Phase-1 in converting to NIO.
 */
class NIOInputStream extends InputStream {
    
    private static final Log LOG = LogFactory.getLog(NIOInputStream.class);
    
    private final ReadHandler handler;
    private final SocketChannel channel;
    private ByteBuffer buffer;
    private volatile boolean interested = false;
    
    private final Object LOCK = new Object();
    
    NIOInputStream(ReadHandler handler, SocketChannel channel) {
        this.handler = handler;
        this.channel = channel;
    }
    
    public void close() throws IOException {
        channel.close();
    }
    
    public int available() throws IOException {
        return 0;
    }
    
    public void mark(int readlimit) {
        throw new UnsupportedOperationException();
    }
    
    public boolean markSupported() {
        return false;
    }
    

    public void reset() {
        throw new UnsupportedOperationException();
    }
    
    public long skip(long n) throws IOException {
        synchronized(LOCK) {
            return super.skip(n);
        }
    }
    
    public int read() throws IOException {
        byte[] b = new byte[1];
        read(b);
        return b[0];
    }
    
    public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }
    
    public int read(byte[] b, int off, int len) throws IOException {
        synchronized(LOCK) {
            if(len != 0) {
                buffer = ByteBuffer.wrap(b, off, len);
                interested = true;
                NIODispatcher.instance().register(handler);
                readImpl();
            }
            
            interested = false;
            return len - buffer.remaining();
        }
    }
    
    private void readImpl() throws IOException {
        if(interested) {
            boolean looped = false;
            int wanted = buffer.remaining();
            while(buffer.remaining() - wanted == 0) {
                while(channel.read(buffer) != 0 && buffer.hasRemaining());
                if(LOG.isTraceEnabled())
                    LOG.trace("Remaining: " + buffer.remaining() + ", wanted: " + wanted);
                if(buffer.remaining() - wanted == 0) {
                    if(looped) {
                        IOException x = new java.io.InterruptedIOException("read timed out");
                        LOG.debug("Timeout while reading", x);
                        throw x;
                    }
                    
                    NIODispatcher.instance().register(handler);
                    try {
                        int timeout = Math.max(0, handler.getSoTimeout());
                        LOCK.wait(timeout);
                        if(timeout != 0)
                            looped = true;
                    } catch(InterruptedException ix) {
                        throw new InterruptedIOException(ix);
                    }
                }
            }
        }
    }
    
    int interestOps() {
        if(interested)
            return SelectionKey.OP_READ;
        else
            return 0;
    }
    
    void bump() {
        synchronized(LOCK) {
            LOCK.notify();
        }
    }
}
                
        
    