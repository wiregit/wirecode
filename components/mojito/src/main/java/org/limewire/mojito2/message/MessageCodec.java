package org.limewire.mojito2.message;

import java.io.IOException;
import java.net.SocketAddress;

/**
 * An interface to encode and decode {@link Message}s.
 */
public interface MessageCodec {

    /**
     * Encodes the given {@link Message}
     */
    public byte[] encode(SocketAddress dst, Message message) throws IOException;
    
    /**
     * Decodes the given message
     */
    public Message decode(SocketAddress src, byte[] message) throws IOException;
    
    /**
     * Decodes the given message
     */
    public Message decode(SocketAddress src, byte[] message, 
            int offset, int length) throws IOException;
}
