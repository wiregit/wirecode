package org.limewire.mojito.message;

import java.io.IOException;
import java.net.SocketAddress;

/**
 * An abstract implmenetation of {@link MessageCodec}
 */
public abstract class AbstractMessageCodec implements MessageCodec {

    @Override
    public Message decode(SocketAddress src, byte[] message) throws IOException {
        return decode(src, message, 0, message.length);
    }
}
