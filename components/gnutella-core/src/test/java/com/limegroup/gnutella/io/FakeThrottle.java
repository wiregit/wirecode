package com.limegroup.gnutella.io;

import java.nio.channels.SelectionKey;
import java.nio.channels.CancelledKeyException;

/** A fake throttle, for testing ThrottleWriter. */
public class FakeThrottle implements Throttle {
    private int _available;
    private ThrottleListener _listener;
    private boolean _wroteAll;
    
    public void interest(ThrottleListener writer, Object attachment) {
        _listener = writer;
    }
    
    public int request(ThrottleListener writer, Object attachment) {
        int av = _available;
        _available = 0;
        return av;
    }
    
    public void release(int amount, boolean wroteAll, ThrottleListener writer, Object attachment) {
        _available += amount;
        _wroteAll = wroteAll;
        _listener = null;
    }
    
    void setAvailable(int av) { _available = av; }
    int getAvailable() { return _available; }
    boolean isInterested() { return _listener != null; }
    boolean isAllWrote() { return _wroteAll; }
    void interestOff() { _listener = null; }
    void clear() { _available = 0; _listener = null; _wroteAll = false; }
}
    
    