package com.limegroup.gnutella.connection;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import com.limegroup.gnutella.Assert;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.util.NIOBandwidthThrottle;

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
	private Message _curMessage;
    
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
     * Variable used to open and close this writer.  ONLY USED FOR TESTING.
     */
    private boolean _closed = false;
	
    /**
     * Throttle that makes sure we don't send any more data than desired.
     */
    private static final NIOBandwidthThrottle THROTTLE = 
        NIOBandwidthThrottle.createThrottle(8*1024);
    
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
        if(_closed) return true;
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
     * @throws NullPointerException if the <tt>msg</tt> argument is 
     *  <tt>null</tt>
     */
    public synchronized boolean write(Message msg) throws IOException {
        if(msg == null) {
            throw new NullPointerException("cannot add null message");
        }
        // pending messages indicate that the TCP upstream buffer has filled,
        // so we should start our application-level buffering of messages to
        // make sure we prioritize certain traffic over others
        if(hasPendingMessage()) {// || !(QUEUE.size() == 0)) {
            QUEUE.add(msg);
                
            // should already be registered, but just in case...    
            register();
            return false;
        }
        
        //Copy m to a ByteBuffer.  TODO: avoid allocating ByteBuffer each time.
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            // TODO:: add a method to message that writes directly to a 
            // ByteBuffer
            msg.write(baos);
        } catch (IOException e) {
            Assert.that(false, "IO problem from ByteArrayOutputStream");
            return false;
        }
        _curMessage = msg;
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
            if(msg == null) {
                // This indicates that there were no more messages left, so 
                // we've sent all we can on this connection.  Note that this 
                // can happen even if QUEUE.size() == 0 in the call above
                // because some messages could have expired since that call
                // was made.
                return true;
            }
            return write(msg);
    	} else if(!CHANNEL.isConnected()) {
            throw new IOException("connection closed");
        } else {
    	    CHANNEL.write(_message);
        	if(!_message.hasRemaining()) {
                CONNECTION.stats().addBytesSent(_curMessage.getTotalLength());
                CONNECTION.stats().addSent();
        		_message = null;
                return QUEUE.size() == 0;
        	} else {
                register();
        		return false;
        	}
        }
    }

    /**
     * Registers this connection for write events if it's not already 
     * registered.
     */
    private void register() {
        if(!CONNECTION.writeRegistered() && !_closed) {
            NIODispatcher.instance().addWriter(CONNECTION);
        }      
    }
    

    /**
     * Same as write(Message msg) in the NIO case.
     */
    public void simpleWrite(Message msg) throws IOException {
        write(msg);   
    }

    /**
     * Does nothing in the NIO case.
     */
    public void flush() throws IOException {}


    /** 
     * Sets the state of this message writer to closed.  Used primarily for
     * TESTING, particularly in ManagedConnectionBufferTest.
     */
    public void setClosed(boolean closed) {
        _closed = closed;
        if(!closed) {
            try {
                while(!write()) {}
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        
    }
    
}
