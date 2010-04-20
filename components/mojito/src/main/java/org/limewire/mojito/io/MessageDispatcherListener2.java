package org.limewire.mojito.io;

import org.limewire.mojito.messages.DHTMessage;

public interface MessageDispatcherListener2 {

    public void messageSent(DHTMessage message);
    
    public void messageReceived(DHTMessage message);
}
