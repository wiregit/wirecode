package com.limegroup.gnutella.io;


import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.net.*;

import org.apache.commons.logging.*;

/**
 * Manages reading data from the network & piping it to a blocking input stream.
 *
 * This implements WriteHandler because it uses Pipes to write to a blocking
 * input stream.  The Pipe.SinkChannel is writeable and needs to be notified via
 * NIODispatcher when writing is ready.
 */
class NIOInputStream implements WriteHandler {
    
    private static final Log LOG = LogFactory.getLog(NIOInputStream.class);
    
    private final NIOSocket handler;
    private final SocketChannel channel;
    private final InputStream source;
    private final Pipe.SinkChannel sink;
    private ByteBuffer buffer;
    
    /**
     * Constructs a new pipe to allow SocketChannel's reading to funnel
     * to a blocking InputStream.
     */
    NIOInputStream(NIOSocket handler, SocketChannel channel) throws IOException {
        this.handler = handler;
        this.channel = channel;
        
        Pipe pipe = Pipe.open();
        sink = pipe.sink();
        sink.configureBlocking(false);
        pipe.source().configureBlocking(true);
        source = Channels.newInputStream(pipe.source());
    }
    
    /**
     * Initializes the internal ByteBuffer & registers the sink for selection.
     */
    void init() {
        if(buffer != null)
            throw new IllegalStateException("already init'd!");

        this.buffer = ByteBuffer.allocate(8192); // TODO: use a ByteBufferPool
        NIODispatcher.instance().registerWrite(sink, this);
    }
    
    /**
     * Retrieves the InputStream to read from.
     */
    InputStream getInputStream() {
        return source;
    }
    
    /**
     * Notification that a read can happen on the SocketChannel.
     */
    boolean readChannel() throws IOException {
        int read = 0;
        
        // read everything we can.
        while(buffer.hasRemaining() && (read = channel.read(buffer)) > 0);
        if(read == -1)
            throw new IOException("channel closed.");
        
        // If there's data in the buffer, we're interested in writing.
        if(buffer.position() > 0)
            NIODispatcher.instance().interestWrite(sink);

        // if there's room in the buffer, we're interested in more reading ...
        // if not, we're not interested in more reading.
        return buffer.hasRemaining();
    }
    
    /**
     * Notification that a write can happen on the SinkChannel.
     */
    public boolean handleWrite() throws IOException {
        // write everything we can.
        buffer.flip();
        while(buffer.hasRemaining() && sink.write(buffer) > 0);
        buffer.compact();
        
        // If there's room in the buffer, we're interested in reading.
        if(buffer.hasRemaining())
            NIODispatcher.instance().interestRead(channel);
            
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
                
        
    