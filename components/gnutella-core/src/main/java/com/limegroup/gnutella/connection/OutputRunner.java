package com.limegroup.gnutella.connection;

import com.limegroup.gnutella.io.Shutdownable;
import com.limegroup.gnutella.messages.Message;

/**
 * Basic interface allowing various asynchronous message senders.
 */
public interface OutputRunner extends Shutdownable {
    
    public void send(Message m);

}