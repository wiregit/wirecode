package com.limegroup.gnutella.connection;

import com.limegroup.gnutella.Connection;
import com.limegroup.gnutella.messages.Message;

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
public final class CompositeQueue {
    /*
     * IMPLEMENTATION NOTE: this class uses the SACHRIFC algorithm described at
     * http://www.limewire.com/developer/sachrifc.txt.  The basic idea is to use
     * one buffer for each message type.  Messages are removed from the buffers 
     * in a biased round-robin fashion.  This prioritizes some messages types 
     * while preventing any one message type from dominating traffic.  Query 
     * replies are further prioritized by "GUID volume", i.e., the number of 
     * bytes already routed for that GUID.  Other messages are sorted by time 
     * and removed in a LIFO [sic] policy.  This, coupled with timeouts, reduces
     * latency.  
     */
    
    
	/** 
	 * The size of the queue per priority. Larger values tolerate larger bursts
	 *  of producer traffic, though they waste more memory. This queue is 
	 *  slightly larger than the standard to accomodate higher priority 
	 *  messages, such as queries and query hits. 
	 */
    public static final int BIG_QUEUE_SIZE = 100;

	/** 
     * The size of the queue per priority. Larger values tolerate larger bursts
	 *  of producer traffic, though they waste more memory. This queue is
	 *  smaller so that we don't waste too much memory on lower
	 *  priority messages. 
	 */
    public static final int QUEUE_SIZE = 1;
    
    /** 
     * The max time to keep reply messages and pushes in the queues, in
     * milliseconds. 
     */
    static final int BIG_QUEUE_TIME = 10*1000;
    
    /** 
     * The max time to keep queries, pings, and pongs in the queues, in
     * milliseconds.  Public for testing purposes only! 
     */
    public static int QUEUE_TIME = 5*1000;
    
    /** 
     * The number of different priority levels. 
     */
    static final int PRIORITIES = 7;
    
    /** 
     * Names for each priority. "Other" includes QRP messages and is NOT
     * reordered.  These numbers do NOT translate directly to priorities;
     * that's determined by the cycle fields passed to MessageQueue. 
     */
    private static final int PRIORITY_WATCHDOG    = 0;
    private static final int PRIORITY_PUSH        = 1;
    private static final int PRIORITY_QUERY_REPLY = 2;
    private static final int PRIORITY_QUERY       = 3; 
    private static final int PRIORITY_PING_REPLY  = 4;
    private static final int PRIORITY_PING        = 5;
    private static final int PRIORITY_OTHER       = 6;       


    /** 
     * The producer's queues, one priority per mesage type. 
     *  INVARIANT: QUEUES.length==PRIORITIES 
     */
    private MessageQueue[] _queues;
    
    /** 
     * The priority of the last message added to _outputQueue. This is an
     * optimization to keep OutputRunner from iterating through all priorities.
     * This value is only a hint and can be legally set to any priority.  Hence
     * no locking is necessary.
     * INVARIANT: 0<=_priority<PRIORITIES 
     */
    private int _priority = 0;
    
    /** 
     * The number of queued messages.  Maintained to make size() efficient.
     *  INVARIANT: _size==sum of QUEUES[i].size()  
     */
    private int _size = 0;
    
    /**
     * Constant for the <tt>ManagedConnection</tt> instance that uses this 
     * queue.
     */
    private final Connection CONNECTION;
    
    /**
     * Lock for the output queue.
     */
    private final Object QUEUE_LOCK;
    
    /**
     * Creates a new <tt>CompositeQueue</tt> instance.
     * 
     * @param conn the <tt>Connection</tt> that uses the queue
     * @return a new <tt>CompositeQueue</tt> instance
     */
    public static CompositeQueue createQueue(Connection conn, 
        Object queueLock) {
        return new CompositeQueue(conn, queueLock);
    }
    
    /** 
     * Constructs a new queue with the default message buffers. 
     * 
     * @param mc the <tt>Connection</tt> associated with this queue
     */
    private CompositeQueue(Connection conn, Object queueLock) {
        CONNECTION = conn;
        QUEUE_LOCK = queueLock;
    }
    
    /**
     * Static utility method for lazily constructing the message queues.
     * 
     * @return a new array of <tt>MessageQueue</tt>s, with one queue for each
     *  message priority
     */
    private static MessageQueue[] createQueues() {
        // Instantiate queues.  
        // TODO: for ultrapeer->leaf connections, we can
        // save a fair bit of memory by not using buffering at all. 
        MessageQueue[] queues = new MessageQueue[PRIORITIES];
        queues[PRIORITY_WATCHDOG]     //LIFO, no timeout or priorities
            = new SimpleMessageQueue(1, Integer.MAX_VALUE, BIG_QUEUE_SIZE,
                true);
        queues[PRIORITY_PUSH]
            = new PriorityMessageQueue(6, BIG_QUEUE_TIME, BIG_QUEUE_SIZE);
        queues[PRIORITY_QUERY_REPLY]
            = new PriorityMessageQueue(6, BIG_QUEUE_TIME, BIG_QUEUE_SIZE);
        queues[PRIORITY_QUERY]      
            = new PriorityMessageQueue(3, QUEUE_TIME, BIG_QUEUE_SIZE);
        queues[PRIORITY_PING_REPLY] 
            = new PriorityMessageQueue(1, QUEUE_TIME, QUEUE_SIZE);
        queues[PRIORITY_PING]       
            = new PriorityMessageQueue(1, QUEUE_TIME, QUEUE_SIZE);
        queues[PRIORITY_OTHER]        //FIFO, no timeout
            = new SimpleMessageQueue(1, Integer.MAX_VALUE, BIG_QUEUE_SIZE, 
                false);
        return queues;
    }                                                      

    /** 
     * Adds m to this, possibly dropping some messages in the process; call
     * resetDropped to get the count of dropped messages.
     * @see resetDropped 
     */
    public void add(Message msg) {
        repOk();

        // create queues if they haven't yet been created
        if(_queues == null) {
            _queues = createQueues();
        }
        
        synchronized (QUEUE_LOCK) {
            int priority = calculatePriority(msg);
            
            // add msg to appropriate buffer
            _queues[priority].add(msg);
    
            //Update statistics
            int dropped = _queues[priority].resetDropped();
            CONNECTION.stats().addSentDropped(dropped);
            _size += 1-dropped;
            
            // optimization -- make sure we start with a priority that actually
            // has a message
            _priority = priority;
        }
        repOk();
    }

    /**
     * Retrieves the next message in the queue.
     * 
     * @return the next message in the queue, or <tt>null</tt> if there are
     */
    public Message removeNext() {
        // if there are no buffered messages, return null immediately
        if(size() == 0) {
            return null;
        }
        int start = _priority;
        
        do {
            MessageQueue queue = _queues[_priority];
            while(true) {
                Message msg = null;
                synchronized (QUEUE_LOCK) {
                    msg = queue.removeNext();
                    int dropped = queue.resetDropped();
                    CONNECTION.stats().addSentDropped(dropped);
                    _size -= (msg==null?0:1) + dropped;  //maintain invariant
                    
                    if(msg != null) {
                        
                        // if this is the last message, reset the cycle.  This
                        // is necessary because otherwise this queue's cycle
                        // would not get reset until it returned null on the
                        // next pass, even though it may have messages.
                        if(queue.size() == 0) {
                            queue.resetCycle();
                            
                            // move the priority along to make sure we go to 
                            // the next queue on the next pass
                            _priority = (_priority+1)%PRIORITIES;
                        }
                        return msg;
                    } else {
                        // tricky part -- either we've come to the end of the 
                        // queue's currect cycle, or we've emptied the queue.  
                        // Reset the cycle to ensure that this queue can still
                        // be read from in the future
                        queue.resetCycle();
                        
                        // do this inside the lock so that _priority 
                        // cannot be corrupted by another thread
                        _priority = (_priority+1)%PRIORITIES;
                        if(_size == 0) {
                            // optimization -- return null if there are no 
                            // more messages left
                            return null;
                        }
                        break; 
                    }
                }     
            }
            
            // TODO: still a threading issue with _priority -- an add can
            // change it here
        } while(_priority != start);
        
        // if we get here, there are no messages to send, so return null
        return null;
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
        /*
        Assert.that(QUEUES.length==PRIORITIES);
        Assert.that(_priority>=0 && _priority<PRIORITIES,
                    "Invalid priority: "+_priority);

        //Check _size invariant (easiest to break).
        int sum=0;
        for (int i=0; i<QUEUES.length; i++) {
            Assert.that(QUEUES[i]!=null, "Queue "+i+" is null");
            sum+=QUEUES[i].size();
        }
        Assert.that(sum==_size, "Expected "+sum+", got "+_size);
        */
    }

    /**
     * Utility method used only for testing.
     */    
    void resetPriority() {
        _priority = 0;
    }
    
    /** 
     * Returns the send priority for the given message, with higher numbers for
     * higher priorities.
     * 
     * @param msg the <tt>Message</tt> whose priority should be calculated
     * @return the send priority for the message
     */
    private static int calculatePriority(Message msg) {
        switch (msg.getFunc()) {
            case Message.F_QUERY:
                return PRIORITY_QUERY;
            case Message.F_QUERY_REPLY: 
                return PRIORITY_QUERY_REPLY;
            case Message.F_PING_REPLY: 
                return (msg.getHops()==0 && msg.getTTL()<=2) ? 
                    PRIORITY_WATCHDOG : PRIORITY_PING_REPLY;
            case Message.F_PING: 
                return (msg.getHops()==0 && msg.getTTL()<=2) ? 
                    PRIORITY_WATCHDOG : PRIORITY_PING;
            case Message.F_PUSH: 
                return PRIORITY_PUSH;                
            default: 
                return PRIORITY_OTHER;  //includes QRP Tables
        }
    }
}

