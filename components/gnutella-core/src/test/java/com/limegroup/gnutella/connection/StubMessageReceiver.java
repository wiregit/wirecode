package com.limegroup.gnutella.connection;

import java.util.LinkedList;
import java.util.List;

import com.limegroup.gnutella.messages.Message;

@SuppressWarnings("unchecked")
class StubMessageReceiver implements MessageReceiver {
    
    private final List LIST = new LinkedList();
    private byte softMax;
    private int network;
    private boolean closed = false;
    
    public StubMessageReceiver() {
        this((byte)10, Message.N_TCP); // default softMax high so we don't change ttl in reading.
    }
    
    public StubMessageReceiver(byte softMax, int network) {
        this.softMax = softMax;
        this.network = network;
    }
    
    public Message getMessage() {
        return (Message)LIST.remove(0);
    }
    
    public void clear() {
        LIST.clear();
    }
    
    public int size() {
        return LIST.size();
    }
    
    public List list() {
        return LIST;
    }
    
    public boolean isClosed() {
        return closed;
    }
    
    public void setClosed(boolean closed) {
        this.closed = closed;
    }
    
    public void setSoftMax(byte softMax) {
        this.softMax = softMax;
    }
    
    public void processReadMessage(Message m) {
        LIST.add(m);
    }
    
    public void messagingClosed() {
        closed = true;
    }
    
    public byte getSoftMax() {
        return softMax;
    }
    
    public int getNetwork() {
        return network;
    }
}