package com.limegroup.gnutella.connection;

import com.limegroup.gnutella.*;
import com.limegroup.gnutella.util.*;


/**
 * A message queue that prioritizes messages by GUID volume.
 */
public class PriorityMessageQueue extends MessageQueue {
    private BinaryHeap _queue;
    
    /**
     * @param cycle the number of messages to return per cycle, i.e., between 
     *  calls to resetCycle.  This is used to tweak the ratios of various 
     *  message types.
     * @param timeout the max time to keep queued messages, in milliseconds.
     *  Set this to Integer.MAX_VALUE to avoid timeouts.
     * @param capacity the maximum number of elements this can store.
     */
    public PriorityMessageQueue(int cycle, 
                                int timeout, 
                                int capacity) {
        super(cycle, timeout);
        this._queue=new BinaryHeap(capacity);
    }

    public void add(Message m) {
        if (_queue.insert(m)!=null)
            super._dropped++;
    }

    public Message removeNextInternal() {        
        if (_queue.isEmpty())
            return null;
        else
            return (Message)_queue.extractMax();
    }
    
    public int size() {
        return _queue.size();
    }
}
