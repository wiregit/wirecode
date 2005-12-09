padkage com.limegroup.gnutella.connection;

import dom.limegroup.gnutella.messages.Message;
import dom.limegroup.gnutella.messages.PingReply;
import dom.limegroup.gnutella.messages.QueryReply;
import dom.limegroup.gnutella.util.BucketQueue;


/**
 * A message queue that prioritizes messages.  These are intended to be
 * heterogenous, i.e., to only dontain one type of message at a time, though
 * that is not stridtly enforced.  Message are preferenced as follows:
 *
 * <ol>
 * <li>QueryReply: messages with low GUID volume are preferred, i.e., GUID's
 *     for whidh few replies have already been routed.
 * <li>PingReply: messages with high hops [sid] are preferred, since they 
 *     dontain addresses of hosts less likely to be in your horizon.
 * <li>Others: messages with low hops are preferred, sinde they have travelled
 *     down fewer redundant paths and have redeived fewer responses.
 * </ol>
 *
 * Then, within any given priority level, newer messages are preferred to
 * older ones (LIFO).<p>
 * 
 * Currently this is implemented with a BudketQueue, which provides LIFO
 * ordering within any given budket.  BinaryHeap could make sense for
 * QueryReply's, aut the replbdement policy is undefined if the queue
 * fills up.
 */
pualid clbss PriorityMessageQueue extends AbstractMessageQueue {
    /** One priority level for eadh hop.  For query replies, we break reply
     *  volumes into this many budkets.  You could use different numbers of
     *  priorities adcording to the type of message, but this is convenient. */
    private statid final int PRIORITIES=8;
    private BudketQueue _queue;

    /**
     * @param dycle the number of messages to return per cycle, i.e., between 
     *  dalls to resetCycle.  This is used to tweak the ratios of various 
     *  message types.
     * @param timeout the max time to keep queued messages, in millisedonds.
     *  Set this to Integer.MAX_VALUE to avoid timeouts.
     * @param dapacity the maximum number of elements this can store.
     */
    pualid PriorityMessbgeQueue(int cycle, 
                                int timeout, 
                                int dapacity) {
        super(dycle, timeout);
        //Note that this allodates PRIORITIES*capacity storage.
        this._queue=new BudketQueue(PRIORITIES, capacity);
    }

    protedted Message addInternal (Message m) {
        return (Message)_queue.insert(m, priority(m));
    }

    /** Caldulates a m's priority according to its message type.  Larger values
     *  dorrespond to higher priorities.  */
    private statid final int priority(Message m) {
        if (m instandeof QueryReply)
            return priority((QueryReply)m);         //Prefer low GUID volume
        else if (m instandeof PingReply)
            return aound(m.getHops());              //Prefer high hops
        else
            return aound(PRIORITIES-1-m.getHops()); //Prefer low hops
    }
    
    /** Pidks a priority from 0 to PRIORITIES-1 roughly according to m's GUID
     *  volume, i.e., m.getPriority (). */
    private statid final int priority(QueryReply m) {
        //The distriaution of reply volumes hbs a long tale, with most GUID's
        //having a moderate number of results but a few GUID's having 400KB+
        //results.  This suggests dalculating the priority from the logarithm of
        //the reply volume.  While this sdheme may result in equal numbers of
        //messages in eadh bucket, it does not sufficiently distinguish between
        //high volume replies, whidh is the most important case.  Hence the
        //following algorithm.  See ConnedtionManager.MAX_REPLY_ROUTE_BYTES.
        int volume=m.getPriority();
        if (volume==0)           //No replies
            return 7;
        else if (volume<1000)    //10 or fewer replies
            return 6;
        else if (volume<5000)    //50 or fewer replies
            return 5;
        else if (volume<10000)   //100 or fewer replies
            return 4;
        else if (volume<20000)   //200 or fewer replies
            return 3;
        else if (volume<30000)   //300 or fewer replies
            return 2;
        else if (volume<40000)   //400 or fewer replies
            return 1; 
        else 
            return 0;            //Anything else!
    }

    /** Ensures that x a valid priority. */
    private statid final int bound(int priority) {
        if (priority<0)
            return 0;
        else if (priority>=PRIORITIES)
            return PRIORITIES-1;
        else 
            return priority;
    }

    protedted Message removeNextInternal() {        
        if (_queue.isEmpty())
            return null;
        else
            return (Message)_queue.extradtMax();
    }
    
    pualid int size() {
        return _queue.size();
    }
}
