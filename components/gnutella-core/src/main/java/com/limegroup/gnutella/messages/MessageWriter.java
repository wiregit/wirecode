package com.limegroup.gnutella.messages;

import com.limegroup.gnutella.*;
import com.limegroup.gnutella.routing.RouteTableMessage;

import java.nio.*;
import java.nio.channels.*;
import java.io.*;

/**
 * Writes Gnutella messages to SocketChannel.  Used by Connnection to send
 * messages in a non-blocking manner (which requires state).  Not thread-safe.
 */
public class MessageWriter {
    /** Where to get data */
    private SocketChannel channel;
    /** The message being sent, or null if none being sent. */ 
    private ByteBuffer message;
    
    /** Writes messages to channel.  No other thread may write to channel. */
    public MessageWriter(SocketChannel channel) {
        this.channel=channel;
    }

    /**
     * Queues m for sending, if a message is not currently being sent.
     * @return true if this was modified, i.e., if m was queued
     */
    public boolean queue(Message m) {
        if (message!=null)
            return false;
        
        //Copy m to a ByteBuffer.  TODO: avoid allocating ByteBuffer each time.
        int n=m.getTotalLength();
        ByteArrayOutputStream baos=new ByteArrayOutputStream();
        try {
            m.write(baos);
        } catch (IOException e) {
            Assert.that(false, "IO problem from ByteArrayOutputStream");
            return false;
        }
        message=ByteBuffer.allocate(n);
        message.put(baos.toByteArray());
        message.flip();        //prepare for writing        
        return true;
    }

    /**
     * Sends as much queued data as possible, if any.  Returns true if a message
     * was successfully sent.  
     * @exception IOException the underlying socket was closed
     */
    public boolean write() throws IOException {
        if (message==null)
            return false;

        //Try one send.  No looping; that's for higher levels.
        channel.write(message);

        if (! message.hasRemaining()) {
            message=null;
            return true;
        } else {
            return false;
        }
    }
}
