package com.limegroup.gnutella.connection;

import com.limegroup.gnutella.*;
import com.limegroup.gnutella.routing.RouteTableMessage;

import java.nio.*;
import java.nio.channels.*;
import java.io.*;

/**
 * Writes Gnutella messages to SocketChannel.  Used by Connnection to send
 * messages in a non-blocking manner (which requires state).  Queues at most
 * one message at a time.  Not thread-safe.
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
     * Returns true if this has any queued data, i.e., if we're in the process
     * of sending a message.  
     */
    public boolean hasQueued() {
        return message!=null;
    }

    /**
     * Attempts to send m, or as much as possible.  First attempts to add m to
     * this' send queue, possibly discarding other queued messages (or m) for
     * which sending has not yet started.  (Unfortunately we don't convey if
     * messages have been dropped.)  Then attempts to send as much data to the
     * network as possible.
     * @return true iff this still has unsent queued data, i.e., hasQueued().
     *  If true, the caller must subsequently call write() 
     * @exception IOException the underlying socket was closed
     */
    public boolean write(Message m) throws IOException {
        if (hasQueued())
            return true;
        
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

        //Actually send data.  TODO: we don't want to do this on blocking channels
        return write();
    }

    /**
     * Sends as much queued data as possible, if any.
     * @return true iff this still has unsent queued data, i.e., hasQueued().
     *  If true, the caller must subsequently call write() again
     * @exception IOException the underlying socket was closed
     */
    public boolean write() throws IOException {
        if (! hasQueued())
            return false;

        //Try one send.  No looping; that's for higher levels.
        int n=channel.write(message);

        if (! message.hasRemaining()) {
            message=null;
            return false;
        } else {
            return true;
        }
    }
}
