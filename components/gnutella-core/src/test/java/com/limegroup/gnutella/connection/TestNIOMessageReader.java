package com.limegroup.gnutella.connection;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.nio.channels.SelectionKey;

import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.messages.*;
import com.sun.java.util.collections.LinkedList;
import com.sun.java.util.collections.List;

/**
 * Test class for performing message reading tests on NIO code.
 */
public class TestNIOMessageReader extends NIOMessageReader {

    private final List MESSAGES = new LinkedList();

    /**
     * Read lock for testing buffering.
     */
    private final Object READ_LOCK = new Object();
        
    public static NIOMessageReader createReader(Connection conn) {
        
        return new TestNIOMessageReader(conn);
    }

    private TestNIOMessageReader(Connection conn) {
        super(conn);
    }
    
    /**
     * Creates a new <tt>Message</tt> from incoming data over TCP.
     * 
     * @param key the <tt>SelectionKey</tt> instance containing access to the 
     *  channel and the <tt>Connection</tt> for the message
     * @return a new <tt>Message</tt> instance read in from the network
     * @throws IOException if there is an IO error reading the message, such as 
     *  the connection being broken
     * @throws BadPacketException if the message has an unexpected form
     */
    public Message createMessageFromTCP(SelectionKey key) 
        throws IOException, BadPacketException {
        synchronized(READ_LOCK) {
            Message curMsg = super.createMessageFromTCP(key);
            if(curMsg != null) {
                MESSAGES.add(curMsg);
                READ_LOCK.notify();
            }
            return curMsg;
        }
    } 

    /**
     * Used only for testing.  
     */
    public Message read() throws IOException, BadPacketException {
        synchronized(READ_LOCK) {
            while(MESSAGES.isEmpty()) {
                try {
                    READ_LOCK.wait();
                    return (Message)MESSAGES.remove(0);
                } catch (InterruptedException e) {
                    // should never happen
                    e.printStackTrace();
                    return null;
                }
            }
            return (Message)MESSAGES.remove(0);
        }
    }

    /**
     * Used only for testing.
     */
    public Message read(int i) throws IOException, BadPacketException, 
        InterruptedIOException {
        synchronized(READ_LOCK) {
            if(MESSAGES.isEmpty()) {
                try {
                    READ_LOCK.wait(i);
                    if(MESSAGES.isEmpty()) {
                        throw new InterruptedIOException("no message read");
                    }
                    return (Message)MESSAGES.remove(0);
                } catch (InterruptedException e) {
                    throw new InterruptedIOException("null message read");
                }
            } else {
                return (Message)MESSAGES.remove(0);
            }
        }  
    } 

    /**
     * Does nothing since test readers don't route messages.
     */
    public void routeMessage(Message msg) {}
}



