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
    
    private static final Log LOG = LogFactory.getLog(NIOInputStream.class);
    
    private final NIOSocket handler;
    private final SocketChannel channel;
    private final OutputStream sink;
    private final Pipe.SourceChannel source;
    private final ByteBuffer buffer;
    
    /**
     * Constructs a new pipe to allow SocketChannel's reading to funnel
     * to a blocking InputStream.
     */
    NIOOutputStream(NIOSocket handler, SocketChannel channel) throws IOException {
        this.handler = handler;
        this.channel = channel;
        this.buffer = ByteBuffer.allocate(8192); // TODO: use a ByteBufferPool
        
        Pipe pipe = Pipe.open();
        pipe.sink().configureBlocking(true);
        sink = Channels.newOutputStream(pipe.sink());
        source = pipe.source();
        source.configureBlocking(false);
        NIODispatcher.instance().registerRead(source, this);
    }
    
    /**
     * Retrieves the OutputStream to write to.
     */
    OutputStream getOutputStream() {
        return sink;
    }
    
    /**
     * Notification that a read can happen on the SourceChannel.
     */
    public boolean handleRead() throws IOException {
        // read everything we can.
        while(buffer.hasRemaining() && source.read(buffer) != 0);
        
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
    boolean writeChannel() throws IOException {
        // write everything we can.
        buffer.flip();
        while(buffer.hasRemaining() && channel.write(buffer) != 0);
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
    void shutdown() {
        try {
            sink.close();
        } catch(IOException ignored) {}
            
        try {
            source.close();
        } catch(IOException ignored) {}
    }
}
