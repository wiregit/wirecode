package com.limegroup.gnutella.connection;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.nio.channels.SelectionKey;

import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.messages.Message;

/**
 * Interface for Gnutella message reading.
 */
public interface MessageReader {
    
    /**
     * Creates a new <tt>Message</tt> subclass from data read in from the 
     * channel of the specified key.  This is only used in the NIO message
     * reading code.
     * 
     * @param key the <tt>SelectionKey</tt> obtained from the selector, which
     *  contains the connection as an attachment
     * @return a new subclass of <tt>Message</tt>, constructed from the data
     *  read in on the channel
     * @throws BadPacketException if the data read in from the channel does not
     *  match any of the expected message formats
     * @throws IOException if there was an IO error reading data from the 
     *  channel
     */
    Message createMessageFromTCP(SelectionKey key)
        throws BadPacketException, IOException;
        
        
    /**
     * Notifies the <tt>MessageReader</tt> that it should start reading
     * messages.
     * 
     * @throws IOException if there is an IO error reading messages
     */    
    void startReading() throws IOException;

    /**
     * Creates a new <tt>Message</tt> subclass from data read in from the 
     * socket or other input source.
     * 
     * @return a new <tt>Message</tt> subclass from the read data
     * @throws IOException if there is an IO error reading any of the data
     * @throws BadPacketException if the data does not conform to any of the
     *  expected message formats
     */
    Message read() throws IOException, BadPacketException;

    /**
     * Creates a new <tt>Message</tt> subclass from data read in from the 
     * socket or other input source.  The only difference between this and the
     * read() method that takes no arguments is that this method will close the
     * connection if no data is read for the specified number of milliseconds.
     * 
     * @param timeout the number of milliseconds to wait for data to be sent
     *  before closing the connection
     * @return a new <tt>Message</tt> subclass from the read data
     * @throws IOException if there is an IO error reading any of the data
     * @throws BadPacketException if the data does not conform to any of the
     *  expected message formats
     */
    Message read(int timeout) throws IOException, BadPacketException, 
        InterruptedIOException;

    /**
     * Routes the specified message in the appropriate way.  This method is 
     * necessary because both the blocking and non-blocking reading 
     * implementations need to do extra processing before handing the message
     * off to <tt>MessageRouter</tt>, but they need to make this call at 
     * different times because they read messages differently.
     * 
     * @param msg the <tt>Message</tt> instance to route
     */
    void routeMessage(Message msg);
    
}
