package org.limewire.mojito.io;

import java.net.SocketAddress;

import org.limewire.mojito.KUID;
import org.limewire.mojito.message.Message;

/**
 * A callback interface that is being called by the {@link MessageDispatcher}.
 */
public interface MessageDispatcherListener {

    /**
     * Called for each {@link Message} that has been sent.
     */
    public void messageSent(KUID contactId, 
            SocketAddress dst, Message message);
    
    /**
     * Called for each {@link Message} that has been received.
     */
    public void messageReceived(Message message);
}
