package org.limewire.mojito.util;

import java.io.IOException;
import java.net.SocketAddress;

import org.limewire.mojito.io.MessageDispatcher2;
import org.limewire.mojito.io.Transport;
import org.limewire.mojito.message2.Message;

public class NopTransport implements Transport {

    public static final Transport NOP = new NopTransport();
    
    private NopTransport() {}
    
    @Override
    public void bind(MessageDispatcher2 messageDispatcher) {
    }

    @Override
    public void send(SocketAddress dst, Message message) throws IOException {
    }
}
