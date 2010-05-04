package org.limewire.mojito2.io;

import java.net.SocketAddress;

import org.limewire.mojito2.KUID;
import org.limewire.mojito2.message.Message;

public class MessageDispatcherAdapter implements MessageDispatcherListener {

    @Override
    public void messageReceived(Message message) {
    }

    @Override
    public void messageSent(KUID contactId, 
            SocketAddress dst, Message message) {
    }
}
