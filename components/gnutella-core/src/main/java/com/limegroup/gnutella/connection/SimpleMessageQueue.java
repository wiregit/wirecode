package com.limegroup.gnutella.connection;

import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.util.Buffer;


/**
 * Simple LIFO or FIFO message queue.
 */
public class SimpleMessageQueue extends MessageQueue {
    private Buffer _buf;
    private boolean _lifo;
    
    /**
     * @param cycle the number of messages to return per cycle, i.e., between 
     *  calls to resetCycle.  This is used to tweak the ratios of various 
     *  message types.
     * @param timeout the max time to keep queued messages, in milliseconds.
     *  Set this to Integer.MAX_VALUE to avoid timeouts.
     * @param capacity the maximum number of elements this can store.
     * @param lifo true if this is last-in-first-out, false if this is 
     *  first-in-first-out.
     */
    public SimpleMessageQueue(int cycle, 
                                 int timeout, 
                                 int capacity, 
                                 boolean lifo) {
        super(cycle, timeout);
        this._buf=new Buffer(capacity);
        this._lifo=lifo;
    }

    protected Message addInternal(Message m) {
        return (Message)_buf.addLast(m);
    }

    protected Message removeNextInternal() {
        if (_buf.isEmpty())
            return null;

        if (_lifo)
            return (Message)_buf.removeLast();
        else
            return (Message)_buf.removeFirst();
    }
    
    public int size() {
        return _buf.size();
    }
}
