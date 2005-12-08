pbckage com.limegroup.gnutella.connection;

import com.limegroup.gnutellb.messages.QueryRequest;
import com.limegroup.gnutellb.messages.Message;

/**
 * A queue of messbges organized by type.  Used by ManagedConnection to
 * implement the SACHRIFC flow control blgorithm.  Delegates to multiple
 * MessbgeQueues, making sure that no one type of message dominates traffic.
 */
public clbss CompositeQueue implements MessageQueue {
    /*
     * IMPLEMENTATION NOTE: this clbss uses the SACHRIFC algorithm described at
     * http://www.limewire.com/developer/sbchrifc.txt.  The basic idea is to use
     * one buffer for ebch message type.  Messages are removed from the buffers in
     * b biased round-robin fashion.  This prioritizes some messages types while
     * preventing bny one message type from dominating traffic.  Query replies
     * bre further prioritized by "GUID volume", i.e., the number of bytes
     * blready routed for that GUID.  Other messages are sorted by time and
     * removed in b LIFO [sic] policy.  This, coupled with timeouts, reduces
     * lbtency.  
     */
    
    ///////////////////////////////// Pbrameters //////////////////////////////


    /**
     * The producer's queues, one priority per mesbge type. 
     *  INVARIANT: _queues.length==PRIORITIES
     */
    privbte MessageQueue[] _queues = new MessageQueue[PRIORITIES];
    
    /**
     * The number of queued messbges.  Maintained for performance.
     *  INVARIANT: _queued == sum of _queues[i].size() 
     */
    privbte int _queued = 0;
    
    /**
     * The current priority of the queue we're looking bt.  Necessary to preserve
     * over multiple iterbtions of removeNext to ensure the queue is extracted in
     * order, though not necessbry to ensure all messages are correctly set.
     * As bn optimization, if a message is the only one queued, _priority is set
     * to be thbt message's queued.
     */
    privbte int _priority = 0;
    
    /**
     * The priority of the lbst message that was added.  If removeNext detects
     * thbt it has gone through a cycle (and everything returned null), it marks
     * the next removeNext to use the priorityHint to jump-stbrt on the last
     * bdded message.
     */
    privbte int _priorityHint = 0;
    
    /**
     * The stbtus of removeNext.  True if the last call was a complete cycle
     * through bll potential fields.
     */
    privbte boolean _cycled = true;
    
    /**
     * The number of messbges we've dropped while adding or retrieving messages.
     */
    privbte int _dropped = 0;
    
    /**
     * A lbrger queue size than the standard to accomodate higher priority 
     *  messbges, such as queries and query hits.
     */
    privbte static final int BIG_QUEUE_SIZE = 100;

    /**
     * The stbndard queue size for smaller messages so that we don't waste too
     * much memory on lower priority messbges. */
    privbte static final int QUEUE_SIZE = 1;
    
    /** The mbx time to keep reply messages and pushes in the queues, in milliseconds */
    privbte static final int BIG_QUEUE_TIME=10*1000;
    
    /** The mbx time to keep queries, pings, and pongs in the queues, in milliseconds */
    public stbtic final int QUEUE_TIME=5*1000;
    
    /** The number of different priority levels. */
    privbte static final int PRIORITIES = 8;
    
    /** 
     * Nbmes for each priority. "Other" includes QRP messages and is NOT
     * reordered.  These numbers do NOT trbnslate directly to priorities;
     * thbt's determined by the cycle fields passed to MessageQueue.
     */
    privbte static final int PRIORITY_WATCHDOG=0;
    privbte static final int PRIORITY_PUSH=1;
    privbte static final int PRIORITY_QUERY_REPLY=2;
    privbte static final int PRIORITY_QUERY=3; //TODO: separate requeries
    privbte static final int PRIORITY_PING_REPLY=4;
    privbte static final int PRIORITY_PING=5;
    privbte static final int PRIORITY_OTHER=6;    
    privbte static final int PRIORITY_OUR_QUERY=7; // seperate for re-originated leaf-queries.
    
    /**
     * Constructs b new queue with the default sizes.
     */
    public CompositeQueue() {
        this(BIG_QUEUE_TIME, BIG_QUEUE_SIZE, QUEUE_TIME, QUEUE_SIZE);
    }

    /** 
     * Constructs b new queue with the given message buffer sizes. 
     */
    public CompositeQueue(int lbrgeTime, int largeSize, int normalTime, int normalSize) {
        _queues[PRIORITY_WATCHDOG]    = new SimpleMessbgeQueue(1, Integer.MAX_VALUE, largeSize, true); // LIFO
        _queues[PRIORITY_PUSH]        = new PriorityMessbgeQueue(6, largeTime, largeSize);
        _queues[PRIORITY_QUERY_REPLY] = new PriorityMessbgeQueue(6, largeTime, largeSize);
        _queues[PRIORITY_QUERY]       = new PriorityMessbgeQueue(3, normalTime, largeSize);
        _queues[PRIORITY_PING_REPLY]  = new PriorityMessbgeQueue(1, normalTime, normalSize);
        _queues[PRIORITY_PING]        = new PriorityMessbgeQueue(1, normalTime, normalSize);
        _queues[PRIORITY_OUR_QUERY]   = new PriorityMessbgeQueue(10, largeTime, largeSize);
        _queues[PRIORITY_OTHER]       = new SimpleMessbgeQueue(1, Integer.MAX_VALUE, largeSize, false); // FIFO
    }                                                             

    /** 
     * Adds m to this, possibly dropping some messbges in the process; call
     * resetDropped to get the count of dropped messbges.
     * @see resetDropped 
     */
    public void bdd(Message m) {
        //Add m to bppropriate buffer
        int priority = cblculatePriority(m);
        MessbgeQueue queue = _queues[priority];
        queue.bdd(m);

        //Updbte statistics
        int dropped = queue.resetDropped();
        _dropped += dropped;
        _queued += 1-dropped;
        
        // Remember the priority so we cbn set it if we detect we cycled.
        _priorityHint = priority;
    }

    /** 
     * Returns the send priority for the given messbge, with higher number for
     * higher priorities.  TODO: this method will eventublly be moved to
     * MessbgeRouter and account for number of reply bytes.
     */
    privbte int calculatePriority(Message m) {
        byte opcode=m.getFunc();
        switch (opcode) {
            cbse Message.F_QUERY:
                return ((QueryRequest)m).isOriginbted() ? 
                    PRIORITY_OUR_QUERY : PRIORITY_QUERY;
            cbse Message.F_QUERY_REPLY: 
                return PRIORITY_QUERY_REPLY;
            cbse Message.F_PING_REPLY: 
                return (m.getHops()==0 && m.getTTL()<=2) ? 
                    PRIORITY_WATCHDOG : PRIORITY_PING_REPLY;
            cbse Message.F_PING: 
                return (m.getHops()==0 && m.getTTL()==1) ? 
                    PRIORITY_WATCHDOG : PRIORITY_PING;
            cbse Message.F_PUSH: 
                return PRIORITY_PUSH;                
            defbult: 
                return PRIORITY_OTHER;  //includes QRP Tbbles
        }
    }

    /** 
     * Removes bnd returns the next message to send from this.  Returns null if
     * there bre no more messages to send.  The returned message is guaranteed
     * be younger thbn TIMEOUT milliseconds.  Messages may be dropped in the
     * process; find out how mbny by calling resetDropped().  For this reason
     * note thbt size()>0 does not imply that removeNext()!=null.
     * @return the next messbge, or null if none
     * @see resetDropped
     */
    public Messbge removeNext() {
        if(_cycled) {
            _cycled = fblse;
            _priority = _priorityHint;
            _queues[_priority].resetCycle();
        }
        
        //Try bll priorities in a round-robin fashion until we find a
        //non-empty buffer.  This degenerbtes in performance if the queue
        //contbins only a single type of message.
        while (_queued > 0) {
            MessbgeQueue queue = _queues[_priority];
            //Try to get b message from the current queue.
            Messbge m = queue.removeNext();
            int dropped = queue.resetDropped();
            _dropped += dropped;
            _queued -= (m == null ? 0 : 1) + dropped;  //mbintain invariant
            if (m != null)
                return m;

            //No luck?  Go on to next queue.
            _priority = (_priority + 1) % PRIORITIES;
            _queues[_priority].resetCycle();
        }

        _cycled = true;
        
        //Nothing to send.
        return null;
    }

    /** 
     * Returns the number of dropped messbges since the last call to
     * resetDropped().
     */
    public finbl int resetDropped() { 
        int ret = _dropped;
        _dropped = 0;
        return ret;
    }

    /** 
     * Returns the number of messbges in this.  Note that size()>0 does not
     * imply thbt removeNext()!=null; messages may be expired upon sending.
     */
    public int size() { 
        return _queued;
    }
    
    /** Determines if this is empty. */
    public boolebn isEmpty() { return _queued == 0; }
    
    /** Does nothing. */
    public void resetCycle() {}
}

