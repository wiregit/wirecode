package com.limegroup.gnutella.connection;

import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.SelectionKey;

import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.messages.Message;

/**
 * Interface for Gnutella message reading.
 */
public interface MessageReader {
    
    Message createMessageFromTCP(InputStream is) 
        throws BadPacketException, IOException;
    
    Message createMessageFromTCP(SelectionKey key)
        throws BadPacketException, IOException;
}
