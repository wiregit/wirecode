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
    /** The underlying data structure.  Usually implemented by a OrderedBuffer 
     *  or a BinaryHeap. */
    private FixedSizeCollection _buf;

    private int _cycleSize;
    private int _leftInCycle;
    
    /** Same as this(lifo, size, cycle, false) */
    public MessageQueue(boolean lifo,
                        int size,
                        int cycle) throws IllegalArgumentException {
        this(lifo, size, cycle, false);
    }

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
                        int cycle,
                        boolean sorted) throws IllegalArgumentException {
        if (size<=0)
            throw new IllegalArgumentException("Size too small: "+size);
        if (cycle<=0)
            throw new IllegalArgumentException("Cycle too small: "+cycle);

        if (sorted)
            this._buf=new BinaryHeap(size);
        else
            this._buf=new OrderedBuffer(size, lifo);
        this._cycleSize=cycle;
        this._leftInCycle=cycle;
    }

    /** Adds m to this, returning the number of messages dropped in the process. */
    public int add(Message m) {
        return _buf.addR(m);
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
        
        return (Message)_buf.remove();
    }  

    /** Returns the number of queued messages. */
    public int size() {
        return _buf.size();
    }

    public String toString() {
        return _buf.toString();
    }

    //No unit tests; this code is covered by tests in ManagedConnection.
    //(Actually most of this code used to be in ManagedConnection.)
}


/**
 * Adapts Buffer to implement FixedSizeCollection, defining the add
 * method in terms of LIFO/FIFO behavior.
 */
class OrderedBuffer extends Buffer implements FixedSizeCollection {
    private boolean _lifo;

    public OrderedBuffer(int capacity, boolean lifo) {
        super(capacity);
        this._lifo=lifo;
    }

    public Object remove() {
        if (_lifo)
            return removeLast();
        else
            return removeFirst();
    }

    public int addR(Object x) {
        return addLast(x)==null ? 0 : 1;
    }
}
