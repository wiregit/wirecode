package org.limewire.mojito2.io;

import java.io.IOException;
import java.net.SocketAddress;

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
    public void send(SocketAddress dst, byte[] message, 
            int offset, int length) throws IOException;
}