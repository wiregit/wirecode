package com.limegroup.gnutella.connection;

import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.SelectionKey;

import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.messages.Message;

/**
 * Class that takes care of reading messages from blocking sockets.
 */
public class BIOMessageReader extends AbstractMessageReader {

    /**
     * @return
     */
    public static MessageReader createReader() {
        // TODO Auto-generated method stub
        return new BIOMessageReader();
    }
    
    private BIOMessageReader() {
        
    }
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.connection.MessageReader#createMessageFromTCP(java.io.InputStream)
     */
    public Message createMessageFromTCP(InputStream is) 
        throws BadPacketException, IOException {
        return Message.read(is);
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.connection.MessageReader#createMessageFromTCP(java.nio.channels.SelectionKey)
     */
    public Message createMessageFromTCP(SelectionKey key) {
        // TODO Auto-generated method stub
        return null;
    }

}
