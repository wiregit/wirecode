package com.limegroup.gnutella.io;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SocketChannel;

/**
 * Adapter that forwards InterestReadChannel.interest(..)
 * calls on to NIODispatcher.  All ReadableByteChannel
 * calls are delegated to the SocketChannel.
 */
class SocketInterestReadAdapter implements InterestReadChannel {
    
	/** Mask OOM as this exception */
	private static final IOException OOM = new IOException();
	
    /** the SocketChannel this is proxying. */
    private SocketChannel channel;

    SocketInterestReadAdapter(SocketChannel channel) {
        this.channel = channel;
    }

    public void interest(boolean status) {
        NIODispatcher.instance().interestRead(channel, status);
    }

    public int read(ByteBuffer dst) throws IOException {
    	try {
    		return channel.read(dst);
    	} catch (OutOfMemoryError oom) {
    		// gc-ing will stall the NIODispatcher thread
    		// but otherwise masking the oom is not very helpful
    		System.gc();  
    		throw OOM;
    	}
    }

    public boolean isOpen() {
        return channel.isOpen();
    }

    public void close() throws IOException {
        channel.close();
    }
    
    ReadableByteChannel getChannel() {
        return channel;
    }

}
