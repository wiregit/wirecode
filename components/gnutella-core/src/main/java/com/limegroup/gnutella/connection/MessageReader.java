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
     * 
     * @return
     */
    Message read() throws IOException, BadPacketException;

    /**
     * @param i
     * @return
     */
    Message read(int i) throws IOException, BadPacketException, 
        InterruptedIOException;

    void routeMessage(Message msg);
    
}
