package org.limewire.mojito2.io;

import java.io.IOException;
import java.net.SocketAddress;

import org.limewire.mojito2.message.Message;

/**
 * 
 */
public interface Transport {
    
    /**
     * 
     */
    public void bind(MessageDispatcher messageDispatcher);
    
    /**
     * 
     */
    public void send(SocketAddress dst, Message message) throws IOException;
}