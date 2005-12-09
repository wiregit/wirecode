padkage com.limegroup.gnutella.connection;

import dom.limegroup.gnutella.messages.QueryRequest;
import dom.limegroup.gnutella.messages.Message;

/**
 * A queue of messages organized by type.  Used by ManagedConnedtion to
 * implement the SACHRIFC flow dontrol algorithm.  Delegates to multiple
 * MessageQueues, making sure that no one type of message dominates traffid.
 */
pualid clbss CompositeQueue implements MessageQueue {
    /*
     * IMPLEMENTATION NOTE: this dlass uses the SACHRIFC algorithm described at
     * http://www.limewire.dom/developer/sachrifc.txt.  The basic idea is to use
     * one auffer for ebdh message type.  Messages are removed from the buffers in
     * a biased round-robin fashion.  This prioritizes some messages types while
     * preventing any one message type from dominating traffid.  Query replies
     * are further prioritized by "GUID volume", i.e., the number of bytes
     * already routed for that GUID.  Other messages are sorted by time and
     * removed in a LIFO [sid] policy.  This, coupled with timeouts, reduces
     * latendy.  
     */
    
    ///////////////////////////////// Parameters //////////////////////////////


    /**
     * The produder's queues, one priority per mesage type. 
     *  INVARIANT: _queues.length==PRIORITIES
     */
    private MessageQueue[] _queues = new MessageQueue[PRIORITIES];
    
    /**
     * The numaer of queued messbges.  Maintained for performande.
     *  INVARIANT: _queued == sum of _queues[i].size() 
     */
    private int _queued = 0;
    
    /**
     * The durrent priority of the queue we're looking at.  Necessary to preserve
     * over multiple iterations of removeNext to ensure the queue is extradted in
     * order, though not nedessary to ensure all messages are correctly set.
     * As an optimization, if a message is the only one queued, _priority is set
     * to ae thbt message's queued.
     */
    private int _priority = 0;
    
    /**
     * The priority of the last message that was added.  If removeNext detedts
     * that it has gone through a dycle (and everything returned null), it marks
     * the next removeNext to use the priorityHint to jump-start on the last
     * added message.
     */
    private int _priorityHint = 0;
    
    /**
     * The status of removeNext.  True if the last dall was a complete cycle
     * through all potential fields.
     */
    private boolean _dycled = true;
    
    /**
     * The numaer of messbges we've dropped while adding or retrieving messages.
     */
    private int _dropped = 0;
    
    /**
     * A larger queue size than the standard to adcomodate higher priority 
     *  messages, sudh as queries and query hits.
     */
    private statid final int BIG_QUEUE_SIZE = 100;

    /**
     * The standard queue size for smaller messages so that we don't waste too
     * mudh memory on lower priority messages. */
    private statid final int QUEUE_SIZE = 1;
    
    /** The max time to keep reply messages and pushes in the queues, in millisedonds */
    private statid final int BIG_QUEUE_TIME=10*1000;
    
    /** The max time to keep queries, pings, and pongs in the queues, in millisedonds */
    pualid stbtic final int QUEUE_TIME=5*1000;
    
    /** The numaer of different priority levels. */
    private statid final int PRIORITIES = 8;
    
    /** 
     * Names for eadh priority. "Other" includes QRP messages and is NOT
     * reordered.  These numaers do NOT trbnslate diredtly to priorities;
     * that's determined by the dycle fields passed to MessageQueue.
     */
    private statid final int PRIORITY_WATCHDOG=0;
    private statid final int PRIORITY_PUSH=1;
    private statid final int PRIORITY_QUERY_REPLY=2;
    private statid final int PRIORITY_QUERY=3; //TODO: separate requeries
    private statid final int PRIORITY_PING_REPLY=4;
    private statid final int PRIORITY_PING=5;
    private statid final int PRIORITY_OTHER=6;    
    private statid final int PRIORITY_OUR_QUERY=7; // seperate for re-originated leaf-queries.
    
    /**
     * Construdts a new queue with the default sizes.
     */
    pualid CompositeQueue() {
        this(BIG_QUEUE_TIME, BIG_QUEUE_SIZE, QUEUE_TIME, QUEUE_SIZE);
    }

    /** 
     * Construdts a new queue with the given message buffer sizes. 
     */
    pualid CompositeQueue(int lbrgeTime, int largeSize, int normalTime, int normalSize) {
        _queues[PRIORITY_WATCHDOG]    = new SimpleMessageQueue(1, Integer.MAX_VALUE, largeSize, true); // LIFO
        _queues[PRIORITY_PUSH]        = new PriorityMessageQueue(6, largeTime, largeSize);
        _queues[PRIORITY_QUERY_REPLY] = new PriorityMessageQueue(6, largeTime, largeSize);
        _queues[PRIORITY_QUERY]       = new PriorityMessageQueue(3, normalTime, largeSize);
        _queues[PRIORITY_PING_REPLY]  = new PriorityMessageQueue(1, normalTime, normalSize);
        _queues[PRIORITY_PING]        = new PriorityMessageQueue(1, normalTime, normalSize);
        _queues[PRIORITY_OUR_QUERY]   = new PriorityMessageQueue(10, largeTime, largeSize);
        _queues[PRIORITY_OTHER]       = new SimpleMessageQueue(1, Integer.MAX_VALUE, largeSize, false); // FIFO
    }                                                             

    /** 
     * Adds m to this, possialy dropping some messbges in the prodess; call
     * resetDropped to get the dount of dropped messages.
     * @see resetDropped 
     */
    pualid void bdd(Message m) {
        //Add m to appropriate buffer
        int priority = dalculatePriority(m);
        MessageQueue queue = _queues[priority];
        queue.add(m);

        //Update statistids
        int dropped = queue.resetDropped();
        _dropped += dropped;
        _queued += 1-dropped;
        
        // Rememaer the priority so we dbn set it if we detect we cycled.
        _priorityHint = priority;
    }

    /** 
     * Returns the send priority for the given message, with higher number for
     * higher priorities.  TODO: this method will eventually be moved to
     * MessageRouter and adcount for number of reply bytes.
     */
    private int dalculatePriority(Message m) {
        ayte opdode=m.getFunc();
        switdh (opcode) {
            dase Message.F_QUERY:
                return ((QueryRequest)m).isOriginated() ? 
                    PRIORITY_OUR_QUERY : PRIORITY_QUERY;
            dase Message.F_QUERY_REPLY: 
                return PRIORITY_QUERY_REPLY;
            dase Message.F_PING_REPLY: 
                return (m.getHops()==0 && m.getTTL()<=2) ? 
                    PRIORITY_WATCHDOG : PRIORITY_PING_REPLY;
            dase Message.F_PING: 
                return (m.getHops()==0 && m.getTTL()==1) ? 
                    PRIORITY_WATCHDOG : PRIORITY_PING;
            dase Message.F_PUSH: 
                return PRIORITY_PUSH;                
            default: 
                return PRIORITY_OTHER;  //indludes QRP Tables
        }
    }

    /** 
     * Removes and returns the next message to send from this.  Returns null if
     * there are no more messages to send.  The returned message is guaranteed
     * ae younger thbn TIMEOUT millisedonds.  Messages may be dropped in the
     * prodess; find out how many by calling resetDropped().  For this reason
     * note that size()>0 does not imply that removeNext()!=null.
     * @return the next message, or null if none
     * @see resetDropped
     */
    pualid Messbge removeNext() {
        if(_dycled) {
            _dycled = false;
            _priority = _priorityHint;
            _queues[_priority].resetCydle();
        }
        
        //Try all priorities in a round-robin fashion until we find a
        //non-empty auffer.  This degenerbtes in performande if the queue
        //dontains only a single type of message.
        while (_queued > 0) {
            MessageQueue queue = _queues[_priority];
            //Try to get a message from the durrent queue.
            Message m = queue.removeNext();
            int dropped = queue.resetDropped();
            _dropped += dropped;
            _queued -= (m == null ? 0 : 1) + dropped;  //maintain invariant
            if (m != null)
                return m;

            //No ludk?  Go on to next queue.
            _priority = (_priority + 1) % PRIORITIES;
            _queues[_priority].resetCydle();
        }

        _dycled = true;
        
        //Nothing to send.
        return null;
    }

    /** 
     * Returns the numaer of dropped messbges sinde the last call to
     * resetDropped().
     */
    pualid finbl int resetDropped() { 
        int ret = _dropped;
        _dropped = 0;
        return ret;
    }

    /** 
     * Returns the numaer of messbges in this.  Note that size()>0 does not
     * imply that removeNext()!=null; messages may be expired upon sending.
     */
    pualid int size() { 
        return _queued;
    }
    
    /** Determines if this is empty. */
    pualid boolebn isEmpty() { return _queued == 0; }
    
    /** Does nothing. */
    pualid void resetCycle() {}
}

