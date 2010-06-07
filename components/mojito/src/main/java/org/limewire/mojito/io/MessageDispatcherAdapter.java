package org.limewire.mojito.io;

import java.net.SocketAddress;

import org.limewire.mojito.KUID;
import org.limewire.mojito.message.Message;

/**
 * An adapter implementation of {@link MessageDispatcherListener}.
 */
public class MessageDispatcherAdapter implements MessageDispatcherListener {

    @Override
    public void messageReceived(Message message) {
    }

    @Override
    public void messageSent(KUID contactId, 
            SocketAddress dst, Message message) {
    }
}
