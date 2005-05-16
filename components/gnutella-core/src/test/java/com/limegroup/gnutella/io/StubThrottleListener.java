package com.limegroup.gnutella.io;

class StubThrottleListener implements ThrottleListener {
    
    private Object attach;
    private boolean closed;
    private boolean available;
    
    StubThrottleListener(Object attach) { this.attach = attach; }
    public void setAttachment(Object attachment) { this.attach = attachment; }
    public Object getAttachment() { return attach; }
    public boolean bandwidthAvailable() { available = true; return !closed; }
    
    void close() { closed = true; }
    void setClosed(boolean closed) { this.closed = closed; }
    boolean available() { return available; }
    void clear() { available = false; closed = false; }
}