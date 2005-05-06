package com.limegroup.gnutella.connection;

import java.util.List;
import java.util.LinkedList;
import com.limegroup.gnutella.messages.Message;

class StubSentHandler implements SentMessageHandler {
    
    private List SENT = new LinkedList();
    
    public void processSentMessage(Message m) { SENT.add(m); }
    
    public List list() { return SENT; }
    
    public Message next() { return (Message)SENT.remove(0); }
    
    public int size() { return SENT.size(); }
    
    public void clear() { SENT.clear(); }
    
}