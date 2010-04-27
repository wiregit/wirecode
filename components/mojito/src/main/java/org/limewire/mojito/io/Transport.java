package org.limewire.mojito.io;

import java.io.IOException;
import java.net.SocketAddress;

import org.limewire.mojito.message2.Message;

/**
 * 
 */
public interface Transport {
    
    /**
     * 
     */
    public void bind(MessageDispatcher2 messageDispatcher);
    
    /**
     * 
     */
    public void send(SocketAddress dst, Message message) throws IOException;
}