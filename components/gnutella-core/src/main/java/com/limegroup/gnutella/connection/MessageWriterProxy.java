package com.limegroup.gnutella.connection;

import java.io.IOException;

import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.util.CommonUtils;

/**
 * Proxy class the delegates to the approppriate <tt>MessageWriter</tt>
 * implementation.
 */
public final class MessageWriterProxy implements MessageWriter {

    /**
     * Constant for the delegate class that implements all writing methods.
     */
    private final MessageWriter DELEGATE;
    
    /**
     * Creates a new <tt>MessageWriterProxy</tt> instance that delegates to
     * the appropriate <tt>MessageWriter</tt> implementation depending on
     * whether or not we're running on Java 1.4 and whether NIO is active.
     * 
     * @param mc the <tt>ManagedConnection</tt> that ultimately handles some
     *  of the writing
     */
    public MessageWriterProxy(Connection conn) {
        if(CommonUtils.isJava14OrLater() && 
           ConnectionSettings.USE_NIO.getValue()) {
            DELEGATE = NIOMessageWriter.createWriter(conn);       
        } else {
            DELEGATE = BIOMessageWriter.createWriter(conn);
        }    
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.connection.MessageWriter#write()
     */
    public boolean write() throws IOException {
        return DELEGATE.write();
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.connection.MessageWriter#write(com.limegroup.gnutella.messages.Message)
     */
    public boolean write(Message msg) throws IOException {
        return DELEGATE.write(msg);
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.connection.MessageWriter#hasPendingMessage()
     */
    public boolean hasPendingMessage() {
        return DELEGATE.hasPendingMessage();
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.connection.MessageWriter#close()
     */
    public void setClosed(boolean closed) {
        DELEGATE.setClosed(closed);
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.connection.MessageWriter#simpleWrite(com.limegroup.gnutella.messages.Message)
     */
    public void simpleWrite(Message msg) throws IOException {
        DELEGATE.simpleWrite(msg);
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.connection.MessageWriter#flush()
     */
    public void flush() throws IOException {
        DELEGATE.flush();
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.connection.MessageWriter#setRegistered(boolean)
     */
    public void setRegistered(boolean b) {
        DELEGATE.setRegistered(b);
    }

}
