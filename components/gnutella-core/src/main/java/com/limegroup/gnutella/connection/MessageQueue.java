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

    /**
     * @param lifo true if this uses LIFO order, false if this uses FIFO 
     *  ordering.  This is only meaningful if sorted=false.
     * @param size the number of messages this can hold   
     * @param cycle the number of messages to return per cycle, i.e., between 
     *  calls to resetCycle.  This is used to tweak the ratios of various 
     *  message types.
     * @param sorted true iff these messages are sorted by priority.  Otherwise
     *  they're sorted by age.
     * @param timeout the max time to keep queued messages, in milliseconds.
     *  Set this to Integer.MAX_VALUE to avoid timeouts.
     * @exception IllegalArgumentException any of the int arguments are less 
     *  than zero
     */
    public MessageQueue(boolean lifo,
                        int size,
                        int cycle,
                        boolean sorted, 
                        int timeout) throws IllegalArgumentException {
        if (size<=0)
            throw new IllegalArgumentException("Size too small: "+size);
        if (cycle<=0)
            throw new IllegalArgumentException("Cycle too small: "+cycle);
        if (timeout<=0)
            throw new IllegalArgumentException("Timeout too small: "+cycle);

        if (sorted)
            this._buf=new BinaryHeap(size);
        else
            this._buf=new OrderedBuffer(size, lifo, timeout);
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
 * method in terms of LIFO/FIFO behavior.  Also adds timeouts. This
 * is currently done in addR (instead of remove) because there is
 * no need to get the system time.  Hence some items in the queue
 * may be slightly older than _timeout.
 */
class OrderedBuffer extends Buffer implements FixedSizeCollection {
    private boolean _lifo;
    private int _timeout;
    
    public OrderedBuffer(int capacity, boolean lifo, int timeout) {
        super(capacity);
        this._lifo=lifo;
        this._timeout=timeout;
    }

    public Object remove() {
        if (_lifo)
            return removeLast();
        else
            return removeFirst();
    }

    public int addR(Object x) {
        //Starting from head, purge entries more than timout milliseconds older
        //than x.  TODO3: if this is a bottleneck, you could use binary search
        //to find the youngest message to purge, then use bulk delete.
        long purgeTime=((Message)x).getCreationTime()-_timeout;
        int removed=0;
        while (! isEmpty()) {
            Message m=(Message)first();
            if (m.getCreationTime() < purgeTime) {
                removeFirst();
                removed++;
            } else {
                break;
            }
        }

        //Add x to tail of this.
        if (addLast(x)!=null)
            removed++;
        return removed;
    }
}
