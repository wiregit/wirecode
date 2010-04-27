package org.limewire.mojito.io;

import java.io.IOException;

import org.limewire.mojito.message2.Message;

/**
 * 
 */
public abstract class AbstractTransport implements Transport {

    /**
     * 
     */
    private volatile MessageDispatcher2 messageDispatcher;
    
    @Override
    public void bind(MessageDispatcher2 messageDispatcher) {
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
        MessageDispatcher2 messageDispatcher = this.messageDispatcher;
        
        if (messageDispatcher != null) {
            messageDispatcher.handleMessage(message);
            return true;
        }
        
        return false;
    }
}