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

    /**
     * Constant for the <tt>SocketChannel</tt> that this writer writes to.
     */
	private final SocketChannel CHANNEL;

    /**
	 * Variable for the current message being written.
	 */
	private ByteBuffer _message;
	
    
    /**
     * Factory method for creating <tt>MessageWriter</tt> instances for 
     * associated channels.
     * 
     * @param channel the <tt>SocketChannel</tt> for this writer
     * @return a new <tt>MessageWriter</tt> instance
     */
    public static MessageWriter createWriter(SocketChannel channel) {
        return new MessageWriter(channel);
    }
    
	/**
	 * Creates a new <tt>MessageWriter</tt> instance for the specified channel.
	 */
	private MessageWriter(SocketChannel channel) {
		CHANNEL = channel;
	}
	
	/**
	 * Returns true if this has any queued data, i.e., if we're in the process
	 * of sending a message.  
	 */
	public synchronized boolean hasPendingMessage() {
		return _message != null;
	}
	
    /**
     * Writes the specified <tt>Message</tt> to this channel.
     * 
     * @param msg the <tt>Message</tt> to write
     * @return <tt>true</tt> if the entire message was successfully written,
     *  otherwise <tt>false</tt>.  A message may not be completely written in 
     *  the case where upstream TCP buffers are full
     * @throws IOException if there is an IO error writing to the channel
     * @throws IllegalStateException if there is a previous message that was 
     *  not fully sent
     */
    public synchronized boolean write(Message msg) throws IOException {
        
        // this method should not be called if there's a previouos message
        // that has not been fully sent -- throw IllegalStateExcepion in this
        // case
        if(hasPendingMessage()) {
            throw new IllegalStateException("previous message pending");
        }
        //System.out.println("MessageWriter::write");
        //Copy m to a ByteBuffer.  TODO: avoid allocating ByteBuffer each time.
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            msg.write(baos);
        } catch (IOException e) {
            Assert.that(false, "IO problem from ByteArrayOutputStream");
            return false;
        }
        _message = ByteBuffer.allocate(msg.getTotalLength());
        _message.put(baos.toByteArray());
        _message.flip();        //prepare for writing        
        
        return write();
    }
    
    /**
     * Writes any messages that have not been fully written to our TCP buffers.
     * This must be called in the case where only part of a message has been
     * sent.  If it is called in this case, <tt>IllegalStateException</tt> will
     * be thrown.
     * 
     * @return <tt>true</tt> if all of the message was successfully written to
     *  our TCP buffers, otherwise <tt>false</tt>
     * @throws IOException if any IO error occurs during the write
     * @throws IllegalStateException if there are no pending messages to be 
     *  sent
     */
    public synchronized boolean write() throws IOException {
    	if(!hasPendingMessage()) {
    		throw new IllegalStateException("no pending message");
    	}
    	CHANNEL.write(_message);
    	if(!_message.hasRemaining()) {
    		_message = null;
    		return true;
    	} else {
    		return false;
    	}
    }
    
}
