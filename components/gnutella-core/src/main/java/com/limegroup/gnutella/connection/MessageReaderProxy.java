package com.limegroup.gnutella.connection;

import java.io.IOException;
import java.io.InterruptedIOException;
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
    
    /**
     * Creates a new <tt>MessageReaderProxy</tt> for the specified connection.
     * 
     * @param conn the <tt>Connection</tt> this proxy will read messages for
     */
    public MessageReaderProxy(Connection conn) {
        if(CommonUtils.isJava14OrLater() &&
           ConnectionSettings.USE_NIO.getValue()) {
            DELEGATE = NIOMessageReader.createReader(conn);       
        } else {
            DELEGATE = BIOMessageReader.createReader(conn);
        }
    }

    // inherit doc comment
    public Message createMessageFromTCP(SelectionKey key) 
        throws BadPacketException, IOException {
        return DELEGATE.createMessageFromTCP(key);
    }
    
    // inherit doc comment
    public void startReading() throws IOException {
        DELEGATE.startReading();
    }
    
    // inherit doc comment
    public Message read() throws IOException, BadPacketException {
        return DELEGATE.read();
    }
    
    // inherit doc comment
    public Message read(int i) throws IOException, BadPacketException, 
        InterruptedIOException {
        return DELEGATE.read(i);
    }

    public void routeMessage(Message msg) {
        DELEGATE.routeMessage(msg);
    }

    // inherit doc comment
    public void handleMessage(SelectionKey key) {
        DELEGATE.handleMessage(key);
    }
}




