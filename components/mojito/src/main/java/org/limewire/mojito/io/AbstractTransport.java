package org.limewire.mojito.io;

import java.io.IOException;
import java.net.SocketAddress;

import org.limewire.util.Objects;

/**
 * An abstract implementation of {@link Transport}
 */
public abstract class AbstractTransport implements Transport {

    /**
     * The {@link Callback} handle
     */
    private volatile Callback callback;
    
    /**
     * Creates an unbound {@link AbstractTransport}
     */
    public AbstractTransport() {
    }
    
    /**
     * Creates a bound {@link AbstractTransport}
     */
    public AbstractTransport(Callback callback) throws IOException {
        bind(callback);
    }
    
    @Override
    public synchronized void bind(Callback callback) throws IOException {
        Objects.nonNull(callback, "callback");
        
        if (isBound()) {
            throw new IOException();
        }
        
        this.callback = callback;
    }
    
    @Override
    public void unbind() {
        callback = null;
    }
    
    @Override
    public boolean isBound() {
        return callback != null;
    }
    
    @Override
    public void send(SocketAddress dst, byte[] message) throws IOException {
        send(dst, message, 0, message.length);
    }

    /**
     * Returns the {@link Callback}
     */
    protected Callback getCallback() {
        return callback;
    }
    
    /**
     * Notifies the {@link Callback}
     */
    protected boolean handleMessage(SocketAddress src, byte[] message) 
            throws IOException {
        return handleMessage(src, message, 0, message.length);
    }
         
    /**
     * Notifies the {@link Callback}
     */
    protected boolean handleMessage(SocketAddress src, byte[] message, 
                int offset, int length) throws IOException {
        
        Callback callback = this.callback;
        if (callback != null) {
            callback.handleMessage(src, message, offset, length);
            return true;
        }
        return false;
    }
}