
// Edited for the Learning branch

package com.limegroup.gnutella.io;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.io.IOException;

/**
 * Adapter that forwards InterestWriteChannel.interest(..)
 * calls on to NIODispatcher, as well as forwarding handleWrite
 * events to the last party that was interested.  All WritableByteChannel
 * calls are delegated to the SocketChannel.
 * 
 * 
 * Extends and Implements
 * InterestWriteChannel: The object that gives us data can tell us it has some, interest().
 * WriteObserver:        NIO can tell this SocketInterestWriteAdapter to get data and write now, handleWrite().
 */
class SocketInterestWriteAdapater implements InterestWriteChannel {
    
    /** the last party that was interested.  null if none. */
    private volatile WriteObserver interested;
    /** the SocketChannel this is proxying. */
    private SocketChannel channel;
    /** whether or not we're shutdown. */
    private boolean shutdown = false;
    
    /** Constructs a new SocketInterestWriteAdapater */
    SocketInterestWriteAdapater(SocketChannel channel) {
        this.channel = channel;
    }
    
    /** Writes the buffer to the underlying SocketChannel, returning the amount written. */
    public int write(ByteBuffer buffer) throws IOException {

        /*
         * Tour Point
         * 
         * This is where LimeWire actually writes data to the remote computer.
         * channel is a java.nio.channels.SocketChannel object.
         */

        return channel.write(buffer);
    }
    
    /** Closes the SocketChannel */
    public void close() throws IOException {
        channel.close();
    }
    
    /** Determines if the SocketChannel is open */
    public boolean isOpen() {
        return channel.isOpen();
    }
    
    /**
     * Marks the given observer as either interested or not interested in receiving
     * write events from the socket.
     */
    public synchronized void interest(WriteObserver observer, boolean on) {
        if(!shutdown) {
            interested = on ? observer : null;
            NIODispatcher.instance().interestWrite(channel, on);
        }
    }
    
    /**
     * Forwards the write event to the last observer who was interested.
     */
    public boolean handleWrite() throws IOException {
        WriteObserver chain = interested;
        if(chain != null) 
            return chain.handleWrite();
        else
            return false;
    }
    
    /**
     * Shuts down the next link if the chain, if there is any.
     */
    public void shutdown() {
        synchronized(this) {
            if(shutdown)
                return;
            shutdown = true;
        }

        Shutdownable chain = interested;
        if(chain != null)
            chain.shutdown();
        interested = null;
    }
    
    /** Unused, Unsupported. */
    public void handleIOException(IOException x) {
        throw new RuntimeException("unsupported", x);
    }
}