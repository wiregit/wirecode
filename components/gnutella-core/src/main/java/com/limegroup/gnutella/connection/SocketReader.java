package com.limegroup.gnutella.connection;

import com.limegroup.gnutella.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.io.*;
import java.util.*;
import java.net.*;

public class SocketReader implements WritableByteChannel, ReadHandler {
    
    private ByteBuffer buffer;
    private SocketChannel channel;
    private WritableByteChannel chain;
    private NIOHandler handler;
    
    public SocketReader(Socket socket, WritableByteChannel chain) {
        this.channel = socket.getChannel();
        this.chain = chain;
        this.handler = (NIOHandler)socket;
        NIODispatcher.instance().interestRead(channel, true);
    }
    
    /**
     * Notification that a read can happen on the SocketChannel.
     */
    public void handleRead() throws IOException {
        // read everything we can.        
        int read = 0;
        while(true) {
            while(buffer.hasRemaining() && (read = channel.read(buffer)) > 0);
            
            if(buffer.position() > 0) {
                buffer.flip();
                chain.write(buffer);
                buffer.compact();
            } else {
                break;
            }
        }


        if(read == -1) {
            handler.shutdown();
            return;
        }

        // if there's room in the buffer, we're interested in more reading ...
        // if not, we're not interested in more reading.
        if(!buffer.hasRemaining())
            throw new IllegalStateException("can't handle no space left!");
    }
    
    /**
     * Writes data into this SocketReader's internal buffer.
     */
    public int write(ByteBuffer buffer) throws IOException {
        this.buffer = buffer;
        
        // funnel it to our chain.
        if(buffer.hasRemaining())
            return chain.write(buffer);
        else
            return 0;
    }
    
    /**
     * Notification that an IOException occurred while writing.
     */
    public void handleIOException(IOException iox) {
        handler.shutdown();
    }
    
    /**
     * Shuts this service down.
     */
    public void shutdown() {
        buffer = null;
        
        try {
            chain.close();
        } catch(IOException ignored) {}
    }
    
    /**
     * Determines if this reader is open.
     */
    public boolean isOpen() {
        return channel.isOpen();
    }
    
    /**
     * Closes this channel.
     */
    public void close() throws IOException {
        shutdown();
    }
}
    
    