package com.limegroup.gnutella.io;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SelectableChannel;

import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

/**
 * An InputStream that attempts to read from a Buffer.
 *
 * The stream must be notified when data is available in the buffer
 * to be read.
 */
class BufferInputStream extends InputStream implements Shutdownable {
    
    private static final Log LOG = LogFactory.getLog(BufferInputStream.class);
    
    
    /** the lock that reading waits on. */
    private final Object LOCK = new Object();
    
    /** the socket to get soTimeouts for waiting & shutdown on close */
    private final NIOSocket handler;
    
    /** the buffer that has data for reading */
    private final ByteBuffer buffer;
    
    /** the SelectableChannel that the buffer is read from. */
    private final SelectableChannel channel;
    
    /** whether or not this stream has been shutdown. */
    private boolean shutdown = false;
    
    /** whether or not there's no data left to read on this stream. */
    private boolean finished = false;
    
    /**
     * Constructs a new BufferInputStream that reads from the given buffer,
     * using the given socket to retrieve the soTimeouts.
     */
    BufferInputStream(ByteBuffer buffer, NIOSocket handler, SelectableChannel channel) {
        this.handler = handler;
        this.buffer = buffer;
        this.channel = channel;
    }
    
    /** Returns the lock object upon which writing into the buffer should lock */
    Object getBufferLock() {
        return LOCK;
    }
    
    /** Marks this stream as finished -- having no data left to read. */
    void finished() {
        finished = true;
    }
    
    /** Reads a single byte from the buffer. */
    public int read() throws IOException {
        synchronized(LOCK) {
            waitImpl();
            
            if(finished && buffer.position() == 0)
                return -1;
         
            buffer.flip();
            byte read = buffer.get();
            buffer.compact();
            
            // there's room in the buffer now, the channel needs some data.
            NIODispatcher.instance().interestRead(channel, true);
            
            // must &, otherwise implicit cast can change value.
            // (for example, reading the byte -1 is very different than
            //  reading the int -1, which means EOF.)
            return read & 0xFF;
        }
    }
    
    /** Reads a chunk of data from the buffer */
    public int read(byte[] buf, int off, int len) throws IOException {
        if (len == 0)
            return 0;
        synchronized(LOCK) {
            waitImpl();
            
            if(finished && buffer.position() == 0)
                return -1;
                
            buffer.flip();
            int available = Math.min(buffer.remaining(), len);
            buffer.get(buf, off, available);
            
            if (buffer.hasRemaining()) 
                buffer.compact();
            else 
                buffer.clear();
            
            // now that there's room in the buffer, fill up the channel
            NIODispatcher.instance().interestRead(channel, true);
            
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
        int timeout = handler.getSoTimeout();
        boolean looped = false;
        while(buffer.position() == 0 && !finished) {
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

        if(shutdown)
            throw new IOException("socket closed");
    }
    
    /** Closes this InputStream & the Socket that it's associated with */
    public void close() throws IOException  {
        NIODispatcher.instance().shutdown(handler);
    }
    
    /** Shuts down this socket */
    public void shutdown() {
        synchronized(LOCK) {
            shutdown = true;
            LOCK.notify();
        }
    }
    
}
    