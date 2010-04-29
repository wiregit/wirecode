package org.limewire.mojito2.util;

import java.io.IOException;
import java.net.SocketAddress;

import org.limewire.mojito2.io.MessageDispatcher;
import org.limewire.mojito2.io.Transport;
import org.limewire.mojito2.message.Message;

public class NopTransport implements Transport {

    public static final Transport NOP = new NopTransport();
    
    private NopTransport() {}
    
    @Override
    public void bind(MessageDispatcher messageDispatcher) {
    }

    @Override
    public void send(SocketAddress dst, Message message) throws IOException {
    }
}
