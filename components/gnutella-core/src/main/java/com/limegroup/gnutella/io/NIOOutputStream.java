package com.limegroup.gnutella.io;


import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.net.*;

import org.apache.commons.logging.*;

/**
 * Manages writing data to the network from a piped blocking OutputStream.
 *
 * This implements ReadHandler because it uses Pipes to write in blocking mode,
 * and read in non-blocking mode.
 */
class NIOOutputStream implements ReadHandler {
    
    private static final Log LOG = LogFactory.getLog(NIOOutputStream.class);
    
    private final NIOSocket handler;
    private final SocketChannel channel;
    private OutputStream sink;
    private Pipe.SourceChannel source;
    private ByteBuffer buffer;
    private boolean shutdown;
    
    /**
     * Constructs a new pipe to allow SocketChannel's reading to funnel
     * to a blocking InputStream.
     */
    NIOOutputStream(NIOSocket handler, SocketChannel channel) throws IOException {
        this.handler = handler;
        this.channel = channel;
    }
    
    /**
     * Creates the pipes, buffer & registers channels for interest.
     */
    synchronized void init() throws IOException {
        if(buffer != null)
            throw new IllegalStateException("already init'd!");
            
        if(shutdown)
            throw new IOException("already closed!");
        
        Pipe pipe = Pipe.open();
        pipe.sink().configureBlocking(true);
        sink = Channels.newOutputStream(pipe.sink());
        source = pipe.source();
        source.configureBlocking(false);

        this.buffer = ByteBuffer.allocate(8192); // TODO: use a ByteBufferPool
        NIODispatcher.instance().registerRead(source, this);
        NIODispatcher.instance().interestWrite(channel);
    }
    
    /**
     * Retrieves the OutputStream to write to.
     */
    synchronized OutputStream getOutputStream() throws IOException {
        if(buffer == null)
            init();
        
        return sink;
    }
    
    /**
     * Notification that a read can happen on the SourceChannel.
     */
    public boolean handleRead() throws IOException {
        int read = 0;
        
        // read everything we can.
        while(buffer.hasRemaining() && (read = source.read(buffer)) > 0);
        if(read == -1)
            throw new IOException("closed pipe.");
        
        // If there's data in the buffer, we're interested in writing.
        if(buffer.position() > 0)
            NIODispatcher.instance().interestWrite(channel);
        
        // if there's room in the buffer, we're interested in more reading ...
        // if not, we're not interested in more reading.
        return buffer.hasRemaining();
    }
    
    /**
     * Notification that a write can happen on the SocketChannel.
     */
    boolean writeChannel() throws IOException {// write everything we can.
        buffer.flip();
        while(buffer.hasRemaining() && channel.write(buffer) > 0);
        buffer.compact();
        
        // If there's room in the buffer, we're interested in reading.
        if(buffer.hasRemaining())
            NIODispatcher.instance().interestRead(source);
            
        // if we were able to write everything, we're not interested in more writing.
        // otherwise, we are interested.
        return buffer.position() > 0;
    }
    
    /**
     * Notification that an IOException has occurred on one of these channels.
     */
    public void handleIOException(IOException iox) {
        // Inform the NIOSocket that things are dead.  That will shut us down.
        handler.shutdown();
    }
    
    /**
     * Shuts down all internal channels.
     * The SocketChannel should be shut by NIOSocket.
     */
    synchronized void shutdown() {
        if(shutdown)
            return;

        if(sink != null) {
            try {
                sink.close();
            } catch(IOException ignored) {}
        }
         
        if(source != null) {   
            try {
                source.close();
            } catch(IOException ignored) {}
        }
            
        shutdown = true;
    }
}
