package com.limegroup.gnutella.connection;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import com.limegroup.gnutella.Assert;
import com.limegroup.gnutella.messages.Message;

/**
 * Writes Gnutella messages to SocketChannel.  Used by Connnection to send
 * messages in a non-blocking manner (which requires state).  Queues at most
 * one message at a time.  Thread-safe; in a true single-thread core, this
 * would not be necessary.
 */
public final class MessageWriter {

	private final SocketChannel CHANNEL;

    /**
	 * Variable for the current message being written.
	 */
	private ByteBuffer _message;
	
	/**
	 * Ensure that this class cannot be constructed.
	 */
	public MessageWriter(SocketChannel channel) {
		CHANNEL = channel;
	}
	
	/**
	 * Returns true if this has any queued data, i.e., if we're in the process
	 * of sending a message.  
	 */
	public synchronized boolean hasQueued() {
		return _message != null;
	}
	
    public synchronized boolean write(Message msg) throws IOException {
        //System.out.println("MessageWriter::write");
        //Copy m to a ByteBuffer.  TODO: avoid allocating ByteBuffer each time.
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            msg.write(baos);
        } catch (IOException e) {
            Assert.that(false, "IO problem from ByteArrayOutputStream");
            return false;
        }
		int n = msg.getTotalLength();
        _message = ByteBuffer.allocate(n);
        _message.put(baos.toByteArray());
        _message.flip();        //prepare for writing        
        
        return write();
    }
    
    
    public synchronized boolean write() throws IOException {
    	if(!hasQueued()) {
    		return false;
    	}
    	CHANNEL.write(_message);
    	if(!_message.hasRemaining()) {
    		_message = null;
    		return false;
    	} else {
    		return true;
    	}
    }
    
}
