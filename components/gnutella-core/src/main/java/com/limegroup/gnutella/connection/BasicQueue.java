package com.limegroup.gnutella.connection;

import java.util.List;
import java.util.LinkedList;
import com.limegroup.gnutella.messages.Message;

/**
 * A very basic queue of messages.
 *
 * All messages are FIFO.
 */
public class BasicQueue implements MessageQueue {
    
    private List QUEUE = new LinkedList();
    
    
    /** Adds a new message */
    public void add(Message m) {
        QUEUE.add(m);
    }
    
    /** Removes the next message */
    public Message removeNext() {
        if(QUEUE.isEmpty())
            return null;
        else
            return (Message)QUEUE.remove(0);
    }
    
    /** No-op. */
    public int resetDropped() { return 0; }
        
    
    /** Returns the number of queued messages. */
    public int size() {
        return QUEUE.size();
    }
    
    /** No op. */
    public void resetCycle() {}
    
    /** Determines if this is empty. */
    public boolean isEmpty() {
        return QUEUE.isEmpty();
    }
    
}