package com.limegroup.gnutella.connection;

import com.limegroup.gnutella.messages.Message;

public interface MessageReceiver {
    
    public void processMessage(Message m);
    
    public void readerClosed();
    
    public byte getSoftMax();
    
    public int getNetwork();
    
}
        