package com.limegroup.gnutella.connection;

import com.limegroup.gnutella.*;

/**
 * A queue of messages organized by type.  Used by ManagedConnection to
 * implement the SACHRIFC flow control algorithm.  Delegates to multiple
 * MessageQueues, making sure that no one type of message dominates traffic.
 * Thread-safe.<p>
 *
 * Design note: despite the name, this does not subclass MessageQueue.
 * MessageQueue provides some methods for cycles which this does not need.  It's
 * possible to extract a common interface, but I don't think it's worth it.
 */
public class CompositeQueue {
    /*
     * IMPLEMENTATION NOTE: this class uses the SACHRIFC algorithm described at
     * http://www.limewire.com/developer/sachrifc.txt.  The basic idea is to use
     * one buffer for each message type.  Messages are removed from the buffers in
     * a biased round-robin fashion.  This prioritizes some messages types while
     * preventing any one message type from dominating traffic.  Query replies
     * are further prioritized by "GUID volume", i.e., the number of bytes
     * already routed for that GUID.  Other messages are sorted by time and
     * removed in a LIFO [sic] policy.  This, coupled with timeouts, reduces
     * latency.  
     */
    
    ///////////////////////////////// Parameters //////////////////////////////

    /** The size of the queue per priority. Larger values tolerate larger bursts
     *  of producer traffic, though they waste more memory. */
    static final int QUEUE_SIZE=100;
    /** The max time to keep reply messages and pushes in the queues, in
     *  milliseconds. */
    static final int BIG_QUEUE_TIME=10*1000;
    /** The max time to keep queries, pings, and pongs in the queues, in
     *  milliseconds.  Package-access for testing purposes only! */
    static int QUEUE_TIME=5*1000;
    
    /** The number of different priority levels. */
    private static final int PRIORITIES=7;
    /** Names for each priority. "Other" includes QRP messages and is NOT
     * reordered.  These numbers do NOT translate directly to priorities;
     * that's determined by the cycle fields passed to MessageQueue. */
    private static final int PRIORITY_WATCHDOG=0;
    private static final int PRIORITY_PUSH=1;
    private static final int PRIORITY_QUERY_REPLY=2;
    private static final int PRIORITY_QUERY=3; //TODO: add requeries
    private static final int PRIORITY_PING_REPLY=4;
    private static final int PRIORITY_PING=5;
    private static final int PRIORITY_OTHER=6;       


    ///////////////////////////////// Fields /////////////////////////////////

    /** The producer's queues, one priority per mesage type. 
     *  INVARIANT: _queues.length==PRIORITIES */
    private MessageQueue[] _queues=new MessageQueue[PRIORITIES];
    /** The current priority level to send.  Rotates through all 
     *  entries in a round-robin fashion. 
     *  INVARIANT: 0<=_priority<PRIORITIES */
    private int _priority=0;
    /** The number of queued messages.  Maintained to make size() efficient.
     *  INVARIANT: _size==sum of _queues[i].size()  */
    private int _size=0;
    /** The number of dropped messages since the last call to resetDropped. */
    private int _dropped=0;
    

    /** 
     * Constructs a new queue with the default message buffers. 
     */
    public CompositeQueue() {
        _queues[PRIORITY_WATCHDOG]     //LIFO, no timeout or priorities
            = new SimpleMessageQueue(1, Integer.MAX_VALUE, QUEUE_SIZE, true);
        _queues[PRIORITY_PUSH]
            = new PriorityMessageQueue(3, BIG_QUEUE_TIME, QUEUE_SIZE);
        _queues[PRIORITY_QUERY_REPLY]
            = new PriorityMessageQueue(2, BIG_QUEUE_TIME, QUEUE_SIZE);
        _queues[PRIORITY_QUERY]      
            = new PriorityMessageQueue(1, QUEUE_TIME, QUEUE_SIZE);
        _queues[PRIORITY_PING_REPLY] 
            = new PriorityMessageQueue(1, QUEUE_TIME, QUEUE_SIZE);
        _queues[PRIORITY_PING]       
            = new PriorityMessageQueue(1, QUEUE_TIME, QUEUE_SIZE);
        _queues[PRIORITY_OTHER]        //FIFO, no timeout
            = new SimpleMessageQueue(1, Integer.MAX_VALUE, QUEUE_SIZE, false);
        repOk();
    }                                                             

    /** 
     * Adds m to this, possibly dropping some messages in the process; call
     * resetDropped to get the count of dropped messages.
     * @see resetDropped 
     */
    public synchronized void add(Message m) {
        repOk();

        //Add m to appropriate buffer
        int priority=calculatePriority(m);
        _queues[priority].add(m);

        //Update statistics
        int dropped=_queues[priority].resetDropped();
        _dropped+=dropped;
        _size+=1-dropped;
        
        //An optimization to make removeBest faster in the common case that this
        //only has a single element m.
        if (size()==1)
            _priority=priority;

        repOk();
    }

    /** 
     * Returns the send priority for the given message, with higher numbers for
     * higher priorities.  TODO: this method will eventually be moved to
     * MessageRouter and account for number of reply bytes.
     */
    private int calculatePriority(Message m) {
        byte opcode=m.getFunc();
        boolean watchdog=m.getHops()==0 && m.getTTL()<=2;
        switch (opcode) {
        case Message.F_QUERY: 
            return PRIORITY_QUERY;
        case Message.F_QUERY_REPLY: 
            return PRIORITY_QUERY_REPLY;
        case Message.F_PING_REPLY: 
            return watchdog ? PRIORITY_WATCHDOG : PRIORITY_PING_REPLY;
        case Message.F_PING: 
            return watchdog ? PRIORITY_WATCHDOG : PRIORITY_PING;
        case Message.F_PUSH: 
            return PRIORITY_PUSH;
        default: 
            return PRIORITY_OTHER;  //includes QRP Tables
        }
    }

    /** 
     * Removes and returns the next message to send from this.  Returns null if
     * there are no more messages to send.  The returned message is guaranteed
     * be younger than TIMEOUT milliseconds.  Messages may be dropped in the
     * process; find out how many by calling resetDropped().  For this reason
     * note that size()>0 does not imply that removeBest()!=null.
     * @return the next message, or null if none
     * @see resetDropped
     */
    public synchronized Message removeNext() {        
        try { 
            repOk();

            //Try all priorities in a round-robin fashion until we find a
            //non-empty buffer.  This degenerates in performance if the queue
            //contains only a single type of message.
            while (size()>0) {
                //Try to get a message from the current queue.
                Message m=_queues[_priority].removeNext();
                int dropped=_queues[_priority].resetDropped();
                _dropped+=dropped;
                _size-=(m==null?0:1)+dropped;  //maintain invariant
                if (m!=null)
                    return m;

                //No luck?  Go on to next queue.
                _priority=(_priority+1) % PRIORITIES;
                _queues[_priority].resetCycle();
            }

            //Nothing to send.
            return null;

        } finally { 
            repOk(); 
        }
    }

    /** 
     * Returns the number of dropped messages since the last call to
     * resetDropped().
     */
    public synchronized final int resetDropped() { 
        int ret=_dropped;
        _dropped=0;
        return ret;
    }

    /** 
     * Returns the number of messages in this.  Note that size()>0 does not
     * imply that removeBest()!=null; messages may be expired upon sending.
     */
    public synchronized int size() { 
        return _size;
    }
    
    /** 
     * Tests representation invariants.  For performance reasons, this is
     * private and final.  Make protected if ManagedConnection is subclassed.
     */
    private final void repOk() {
        Assert.that(_queues.length==PRIORITIES);
        Assert.that(_priority>=0 && _priority<PRIORITIES,
                    "Invalid priority: "+_priority);

        //Check _size invariant (easiest to break).
        int sum=0;
        for (int i=0; i<_queues.length; i++) {
            Assert.that(_queues[i]!=null, "Queue "+i+" is null");
            sum+=_queues[i].size();
        }
        Assert.that(sum==_size, "Expected "+sum+", got "+_size);
    }

}

