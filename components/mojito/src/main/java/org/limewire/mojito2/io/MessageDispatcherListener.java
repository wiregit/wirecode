package org.limewire.mojito2.io;

import java.net.SocketAddress;

import org.limewire.mojito2.KUID;
import org.limewire.mojito2.message.Message;

public interface MessageDispatcherListener {

    public void messageSent(KUID contactId, 
            SocketAddress dst, Message message);
    
    public void messageReceived(Message message);
}
