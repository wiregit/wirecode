package org.limewire.mojito.io;

import org.limewire.mojito.message2.Message;

public interface MessageDispatcherListener2 {

    public void messageSent(Message message);
    
    public void messageReceived(Message message);
}
