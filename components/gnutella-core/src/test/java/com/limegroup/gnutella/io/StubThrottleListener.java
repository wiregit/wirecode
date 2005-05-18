package com.limegroup.gnutella.io;

class StubThrottleListener implements ThrottleListener {
    
    private Object attach;
    private boolean closed;
    private int available;
    
    StubThrottleListener(Object attach) { this.attach = attach; }
    public void setAttachment(Object attachment) { this.attach = attachment; }
    public Object getAttachment() { return attach; }
    public boolean bandwidthAvailable() { available++; return !closed; }
    public boolean isOpen() { return !closed; }
    
    void close() { closed = true; }
    void setClosed(boolean closed) { this.closed = closed; }
    int available() { return available; }
    void clear() { available = 0; closed = false; }
}