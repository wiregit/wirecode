package com.limegroup.gnutella.connection;

import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.SelectionKey;

import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.util.CommonUtils;

/**
 * Proxy class that simply delegates to the appropriate message reading 
 * implementation, depending on whether we're using NIO or blocking IO.
 */
public class MessageReaderProxy implements MessageReader {

    private final MessageReader DELEGATE;
    
    public MessageReaderProxy() {
        if(CommonUtils.isJava14OrLater() &&
           ConnectionSettings.USE_NIO.getValue()) {
            DELEGATE = NIOMessageReader.createReader();       
        } else {
            DELEGATE = BIOMessageReader.createReader();
        }
    }
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.connection.MessageReader#createMessageFromTCP(java.io.InputStream)
     */
    public Message createMessageFromTCP(InputStream is) 
        throws BadPacketException, IOException {
        return DELEGATE.createMessageFromTCP(is);
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.connection.MessageReader#createMessageFromTCP(java.nio.channels.SelectionKey)
     */
    public Message createMessageFromTCP(SelectionKey key) 
        throws BadPacketException, IOException {
        return DELEGATE.createMessageFromTCP(key);
    }

}
