package org.limewire.mojito2.io;

import java.io.IOException;

import org.limewire.mojito2.message.Message;

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
    public boolean handleMessage(Message message) throws IOException {
        MessageDispatcher messageDispatcher = this.messageDispatcher;
        
        if (messageDispatcher != null) {
            messageDispatcher.handleMessage(message);
            return true;
        }
        
        return false;
    }
}