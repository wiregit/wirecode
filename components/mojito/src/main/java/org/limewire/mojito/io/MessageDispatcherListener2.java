package org.limewire.mojito.io;

import java.net.SocketAddress;

import org.limewire.mojito.KUID;
import org.limewire.mojito.message2.Message;

public interface MessageDispatcherListener2 {

    public void messageSent(KUID contactId, 
            SocketAddress dst, Message message);
    
    public void messageReceived(Message message);
}
