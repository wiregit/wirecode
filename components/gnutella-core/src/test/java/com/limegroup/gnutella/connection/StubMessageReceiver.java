package com.limegroup.gnutella.connection;

import java.util.List;
import java.util.LinkedList;
import com.limegroup.gnutella.messages.Message;

class StubMessageReceiver implements MessageReceiver {
    
    private final List LIST = new LinkedList();
    private byte softMax;
    private int network;
    private boolean closed = false;
    
    public StubMessageReceiver() {
        this((byte)5, Message.N_TCP);
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
    
    public void processMessage(Message m) {
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