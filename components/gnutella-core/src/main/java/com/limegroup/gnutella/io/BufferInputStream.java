package com.limegroup.gnutella.io;

import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.net.*;

import org.apache.commons.logging.*;

import com.limegroup.gnutella.RouterService;

/**
 * An InputStream that attempts to read from a Buffer.
 *
 * The stream must be notified when data is available in the buffer
 * to be read.
 */
 class BufferInputStream extends InputStream {
    
    
    /** the lock that reading waits on. */
    private final Object LOCK = new Object();
    
    /** the socket to get soTimeouts for waiting & shutdown on close */
    private final NIOSocket handler;
    
    /** the buffer that has data for reading */
    private final ByteBuffer buffer;
    
    /** whether or not this stream has been shutdown. */
    private boolean shutdown = false;
    
    /**
     * Constructs a new BufferInputStream that reads from the given buffer,
     * using the given socket to retrieve the soTimeouts.
     */
    BufferInputStream(ByteBuffer buffer, NIOSocket handler) {
        this.handler = handler;
        this.buffer = buffer;
    }
    
    /** Returns the lock object upon which writing into the buffer should lock */
    Object getBufferLock() {
        return LOCK;
    }
    
    /** Reads a single byte from the buffer. */
    public int read() throws IOException {
        synchronized(LOCK) {
            waitImpl();
         
            buffer.flip();
            byte read = buffer.get();
            buffer.compact();
            return read;
        }
    }
    
    /** Reads a chunk of data from the buffer */
    public int read(byte[] buf, int off, int len) throws IOException {
        synchronized(LOCK) {
            waitImpl();
                
            buffer.flip();
            int available = Math.min(buffer.remaining(), len);
            buffer.get(buf, off, available);
            buffer.compact();
            return available; // the amount we read.
        }
    }
    
    /** Determines how much data can be read without blocking */
    public int available() throws IOException {
        synchronized(LOCK) {
            return buffer.position();
        }
    }
    
    /** Waits the soTimeout amount of time. */
    private void waitImpl() throws IOException {
        if(shutdown)
            throw new IOException("socket closed");
            
        int timeout = handler.getSoTimeout();
        boolean looped = false;
        while(buffer.position() == 0) {
            if(shutdown)
                throw new IOException("socket closed");
                
            if(looped && timeout != 0)
                throw new java.io.InterruptedIOException("read timed out (" + timeout + ")");
            try {
                LOCK.wait(timeout);
            } catch(InterruptedException ix) {
                throw new InterruptedIOException(ix);
            }
            looped = true;
        }
    }
    
    /** Closes this InputStream & the Socket that it's associated with */
    public void close() throws IOException  {
        handler.shutdown();
    }
    
    /** Shuts down this socket */
    void shutdown() {
        synchronized(LOCK) {
            shutdown = true;
            LOCK.notify();
        }
    }
    
}
    