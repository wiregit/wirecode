package org.limewire.mojito2.io;

import java.io.IOException;
import java.net.SocketAddress;

/**
 * 
 */
public abstract class AbstractTransport implements Transport {

    /**
     * 
     */
    private volatile MessageDispatcher messageDispatcher;
    
    @Override
    public void bind(MessageDispatcher messageDispatcher) {
        this.messageDispatcher = messageDispatcher;
    }
    
    /**
     * 
     */
    public boolean isBound() {
        return messageDispatcher != null;
    }
    
    /**
     * 
     */
    public boolean handleMessage(SocketAddress src, byte[] message, 
            int offset, int length) throws IOException {
        
        MessageDispatcher messageDispatcher = this.messageDispatcher;
        if (messageDispatcher != null) {
            messageDispatcher.handleMessage(src, message, offset, length);
            return true;
        }
        
        return false;
    }
}