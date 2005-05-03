package com.limegroup.gnutella.connection;

import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.io.Shutdownable;

/**
 * Basic interface allowing various asynchronous message senders.
 */
public interface OutputRunner extends Shutdownable {
    
    public void send(Message m);

}