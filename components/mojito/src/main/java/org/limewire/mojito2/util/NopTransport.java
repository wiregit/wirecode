package org.limewire.mojito2.util;

import java.io.IOException;
import java.net.SocketAddress;

import org.limewire.mojito2.io.Transport;

/**
 * An implementation of {@link Transport} that does nothing.
 */
public class NopTransport implements Transport {

    public static final Transport NOP = new NopTransport();
    
    private NopTransport() {}

    @Override
    public void bind(Callback callback) throws IOException {
    }

    @Override
    public boolean isBound() {
        return true;
    }

    @Override
    public void send(SocketAddress dst, byte[] message, 
            int offset, int length) throws IOException {
    }

    @Override
    public void send(SocketAddress dst, byte[] message) throws IOException {
    }

    @Override
    public void unbind() {
    }
}
