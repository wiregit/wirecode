package com.limegroup.gnutella.connection;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import com.limegroup.gnutella.Assert;
import com.limegroup.gnutella.Connection;
import com.limegroup.gnutella.messages.Message;

/**
 * Writes Gnutella messages to SocketChannel.  Used by Connnection to send
 * messages in a non-blocking manner (which requires state).  Queues at most
 * one message at a time.  Thread-safe; in a true single-thread core, this
 * would not be necessary.
 */
public final class NIOMessageWriter implements MessageWriter {

    /**
     * Constant for the <tt>SocketChannel</tt> that this writer writes to.
     */
	private final SocketChannel CHANNEL;
    
    /**
     * Constant for the <tt>Connection</tt> that uses this writer
     * for sending messages.
     */
    private final Connection CONNECTION;

    /**
	 * Variable for the current message being written.
	 */
	//private Message _message;
    
    /**
     * <tt>ByteBuffer</tt> for the current message being sent.
     */
    private ByteBuffer _message;
    
    
    private final CompositeQueue QUEUE;
    
    /** 
     * A lock to protect changes to the message queue for this connection. 
     */
    private final Object QUEUE_LOCK = new Object();
    
    /**
     * Flag for whether or not the <tt>NIODispatcher</tt> has already registered
     * this writer for write events.
     */
    private volatile boolean _registered = false;
	
    
    /**
     * Factory method for creating <tt>MessageWriter</tt> instances for 
     * associated channels.
     * 
     * @param channel the <tt>SocketChannel</tt> for this writer
     * @return a new <tt>MessageWriter</tt> instance
     */
    public static NIOMessageWriter createWriter(Connection conn) {
        return new NIOMessageWriter(conn);
    }
    
	/**
	 * Creates a new <tt>MessageWriter</tt> instance for the specified channel.
	 */
	private NIOMessageWriter(Connection conn) {
		CHANNEL = conn.getSocket().getChannel();
        CONNECTION = conn;
        QUEUE = CompositeQueue.createQueue(conn, QUEUE_LOCK);
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
        
        // pending messages indicate that the TCP upstream buffer has filled,
        // so we should start our application-level buffering of messages to
        // make sure we prioritize certain traffic over others
        if(hasPendingMessage()) {
            QUEUE.add(msg);
                
            // should already be registered, but just in case...    
            register();
            return false;
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
     * @return <tt>true</tt> if all messages for this connection have been 
     *  successfully written to our TCP buffers, otherwise <tt>false</tt>
     * @throws IOException if any IO error occurs during the write
     * @throws IllegalStateException if there are no pending messages to be 
     *  sent
     */
    public synchronized boolean write() throws IOException {
    	if(!hasPendingMessage()) {
            if(QUEUE.size() == 0) {
                return true;
            }
    		Message msg = QUEUE.removeNext();
            Assert.that(msg != null, "should have obtained queued message");
            return write(msg);
    	} else if(!CHANNEL.isConnected()) {
            throw new IOException("connection closed");
        } else {
    	    CHANNEL.write(_message);
        	if(!_message.hasRemaining()) {
        		_message = null;
                return QUEUE.size() == 0;
        		//return true;
        	} else {
                register();
        		return false;
        	}
        }
    }

    private void register() {
        if(!_registered) {
            NIODispatcher.instance().addWriter(CONNECTION);
            _registered = true;
        }      
    }
    
    /**
     * Closes the NIO writer.
     * 
     * @see com.limegroup.gnutella.connection.MessageWriter#close()
     */
    public void close() {
        // TODO implement this method if necessary
        
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.connection.MessageWriter#simpleWrite(com.limegroup.gnutella.messages.Message)
     */
    public void simpleWrite(Message msg) throws IOException {
        write(msg);   
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.connection.MessageWriter#flush()
     */
    public void flush() throws IOException {
        // TODO Auto-generated method stub
        
    }

    /**
     * Sets the flag for whether or not this writer is registered for write 
     * events with the <tt>NIODispatcher</tt>,
     * 
     * @param b boolean specifying registration status
     */
    public void setRegistered(boolean b) {
        _registered = b;
    }
    
}
