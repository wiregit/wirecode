package org.limewire.mojito.message2;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketAddress;

public class MessageInputStream extends FilterInputStream {

    public MessageInputStream(InputStream in) {
        super(in);
    }
    
    public Message readMessage(SocketAddress src) throws IOException {
        return null;
    }
}
