package org.limewire.mojito.io;

import java.io.Closeable;
import java.io.IOException;
import java.net.SocketAddress;
import java.util.concurrent.TimeUnit;

import org.limewire.mojito.KUID;
import org.limewire.mojito.io.Transport.Callback;
import org.limewire.mojito.message.Message;
import org.limewire.mojito.message.RequestMessage;
import org.limewire.mojito.message.ResponseMessage;
import org.limewire.mojito.routing.Contact;

/**
 * The {@link MessageDispatcher} is responsible for sending and 
 * processing of {@link Message}s.
 */
public interface MessageDispatcher extends Callback, Closeable {

    /**
     * Returns the {@link Transport} or null.
     */
    public Transport getTransport();
    
    /**
     * Binds the {@link MessageDispatcher} to the given {@link Transport}.
     */
    public void bind(Transport transport) throws IOException;

    /**
     * Unbinds the {@link MessageDispatcher} and returns the {@link Transport}.
     */
    public Transport unbind();

    /**
     * Returns {@code true} if the {@link MessageDispatcher} 
     * is bound to a {@link Transport}.
     */
    public boolean isBound();

    /**
     * Adds a {@link MessageDispatcherListener}.
     */
    public void addMessageDispatcherListener(MessageDispatcherListener l);

    /**
     * Removes a {@link MessageDispatcherListener}.
     */
    public void removeMessageDispatcherListener(MessageDispatcherListener l);

    /**
     * Sends a {@link ResponseMessage} to the given {@link Contact}.
     */
    public void send(Contact dst, ResponseMessage response) throws IOException;

    /**
     * Sends a {@link RequestMessage} to the given {@link Contact}.
     */
    public void send(ResponseHandler callback, Contact dst, RequestMessage request,
            long timeout, TimeUnit unit) throws IOException;

    /**
     * Sends a {@link RequestMessage} to the given {@link SocketAddress}.
     */
    public void send(ResponseHandler callback, KUID contactId, SocketAddress dst,
            RequestMessage request, long timeout, TimeUnit unit) throws IOException;

    /**
     * Handles a message from the given {@link SocketAddress}.
     */
    public void handleMessage(SocketAddress src, byte[] data, 
            int offset, int length) throws IOException;

    /**
     * Handles the given {@link Message}.
     */
    public void handleMessage(Message message) throws IOException;

}