package org.limewire.mojito2.io;

import java.io.IOException;
import java.net.SocketAddress;

/**
 * An interfaces that makes the DHT transport agnostic (DatagramSockets,
 * Apache MINA, Netty, LimeWire's NIODispatcher, ...).
 */
public interface Transport {
    
    /**
     * Binds the {@link Transport} to the given {@link Callback}.
     */
    public void bind(Callback callback) throws IOException;
    
    /**
     * Unbinds the {@link Transport}
     */
    public void unbind();
    
    /**
     * Returns true if the {@link Transport} is bound to a {@link Callback}
     */
    public boolean isBound();
    
    /**
     * Sends the given message
     */
    public void send(SocketAddress dst, byte[] message) throws IOException;
    
    /**
     * Sends the given message
     */
    public void send(SocketAddress dst, byte[] message, 
            int offset, int length) throws IOException;
    
    /**
     * A callback that is notified for every received message
     */
    public static interface Callback {
        
        /**
         * Called for every received message
         */
        public void handleMessage(SocketAddress src, byte[] message, 
                int offset, int length) throws IOException;
    }
}