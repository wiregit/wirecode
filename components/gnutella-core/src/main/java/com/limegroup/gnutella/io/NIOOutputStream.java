package com.limegroup.gnutella.io;


import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.net.*;

import org.apache.commons.logging.*;

/**
 * A blocking outputstream that uses non-blocking I/O
 * as its core.
 *
 * Phase-1 in converting to NIO.
 */
class NIOOutputStream extends OutputStream {
    
    private static final Log LOG = LogFactory.getLog(NIOOutputStream.class);
    
    private final NIOHandler handler;
    private final SocketChannel channel;
    private ByteBuffer buffer;
    private volatile boolean interested = false;
    
    private final Object LOCK = new Object();
    
    NIOOutputStream(NIOHandler handler, SocketChannel channel) {
        this.handler = handler;
        this.channel = channel;
    }
    
    public void close() throws IOException {
        channel.close();
    }
    
    public void flush() throws IOException {
        synchronized(LOCK) {
            writeImpl();
            interested = false;
        }
    }
    
    public void write(int a) throws IOException {
        write(new byte[] { (byte)(a & 0xFF) } );
    }
    
    public void write(byte[] b) throws IOException {
        write(b, 0, b.length);
    }
    
    public void write(byte[] b, int off, int len) throws IOException {
        synchronized(LOCK) {
            if(len != 0) {
                buffer = ByteBuffer.wrap(b, off, len);
                interested = true;
                writeImpl();
            }        
            interested = false;
        }
    }
    
    private void writeImpl() throws IOException {
        if(interested) {
            while(buffer.hasRemaining()) {
                while(buffer.hasRemaining() && channel.write(buffer) != 0);
                if(buffer.hasRemaining()) {
                    if(LOG.isTraceEnabled())
                        LOG.trace("Remaining: " + buffer.remaining());
                    NIODispatcher.instance().register(handler);
                    try {
                        wait();
                    } catch(InterruptedException ix) {
                        throw new InterruptedIOException(ix);
                    }
                }
            }
        }
    }
    
    int interestOps() {
        if(interested)
            return SelectionKey.OP_WRITE;
        else
            return 0;
    }
    
    void bump() {
        synchronized(LOCK) {
            LOCK.notify();
        }
    }
}
                
        
    