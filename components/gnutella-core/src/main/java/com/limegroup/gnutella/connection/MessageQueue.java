package com.limegroup.gnutella.connection;

import com.limegroup.gnutella.*;
import com.limegroup.gnutella.util.*;

/**
 * A priority queue for messages.  Used by ManagedConnection to implement the
 * SACHRIFC flow-control algorithm.  NOT THREAD SAFE.<p>
 *
 * This class is designed for speed; hence the somewhat awkward use of
 * resetCycle/extractMax instead of a simple iterator.
 */
public final class MessageQueue {
    private Buffer _buf;
    private boolean _lifo;

    private int _cycleSize;
    private int _leftInCycle;
    
    /**
     * @param lifo true if this uses LIFO order, false if this uses FIFO ordering
     * @param timeout the max time to keep queued messages, in milliseconds.
     *  Set this to Integer.MAX_VALUE to avoid timeouts.
     * @param cycle the number of messages to return per cycle, i.e., between 
     *  calls to resetCycle.  This is used to tweak the ratios of various 
     *  message types.
     * @param sorted true iff these messages are sorted by priority.  Otherwise
     *  they're sorted by age.
     * @exception IllegalArgumentException size or cycle less than or equal to 0
     */
    public MessageQueue(boolean lifo,
                        int size,
                        int cycle) {
        if (size<=0)
            throw new IllegalArgumentException("Size too small: "+size);
        if (cycle<=0)
            throw new IllegalArgumentException("Cycle too small: "+cycle);

        
        this._buf=new Buffer(size);
        this._lifo=lifo;
        this._cycleSize=cycle;
        this._leftInCycle=cycle;
    }

    /** Adds m to this, returning the number of messages dropped in the process. */
    public int add(Message m) {
        Object dropped=_buf.addLast(m);
        return dropped==null ? 0 : 1;
    }
           
    /** Resets the cycle counter used to control removeNext(). */
    public void resetCycle() {
        this._leftInCycle=_cycleSize;
    }

    /** 
     * Removes and returns the next message to send from this during this cycle.
     * Returns null if there are no more messages to send in this cycle.
     */
    public Message removeNext() {
        if (_leftInCycle==0)
            return null;
        _leftInCycle--;

        if (_buf.isEmpty())
            return null;
        
        if (_lifo) 
            return (Message)_buf.removeLast();
        else
            return (Message)_buf.removeFirst();
    }  

    /** Returns the number of queued messages. */
    public int size() {
        return _buf.size();
    }

    //No unit tests; this code is covered by ManagedConnection.
}
