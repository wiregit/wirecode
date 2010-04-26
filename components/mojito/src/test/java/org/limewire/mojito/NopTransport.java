package org.limewire.mojito;

import java.io.IOException;
import java.net.SocketAddress;

import org.limewire.mojito.io.MessageDispatcher2;
import org.limewire.mojito.io.MessageDispatcher2.Transport;
import org.limewire.mojito.messages.DHTMessage;

public class NopTransport implements Transport {

    public static final Transport NOP = new NopTransport();
    
    private NopTransport() {}
    
    @Override
    public void send(MessageDispatcher2 messageDispatcher, 
            SocketAddress dst, DHTMessage message) throws IOException {
    }
}
