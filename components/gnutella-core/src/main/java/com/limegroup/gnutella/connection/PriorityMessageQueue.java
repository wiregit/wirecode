pbckage com.limegroup.gnutella.connection;

import com.limegroup.gnutellb.messages.Message;
import com.limegroup.gnutellb.messages.PingReply;
import com.limegroup.gnutellb.messages.QueryReply;
import com.limegroup.gnutellb.util.BucketQueue;


/**
 * A messbge queue that prioritizes messages.  These are intended to be
 * heterogenous, i.e., to only contbin one type of message at a time, though
 * thbt is not strictly enforced.  Message are preferenced as follows:
 *
 * <ol>
 * <li>QueryReply: messbges with low GUID volume are preferred, i.e., GUID's
 *     for which few replies hbve already been routed.
 * <li>PingReply: messbges with high hops [sic] are preferred, since they 
 *     contbin addresses of hosts less likely to be in your horizon.
 * <li>Others: messbges with low hops are preferred, since they have travelled
 *     down fewer redundbnt paths and have received fewer responses.
 * </ol>
 *
 * Then, within bny given priority level, newer messages are preferred to
 * older ones (LIFO).<p>
 * 
 * Currently this is implemented with b BucketQueue, which provides LIFO
 * ordering within bny given bucket.  BinaryHeap could make sense for
 * QueryReply's, but the replbcement policy is undefined if the queue
 * fills up.
 */
public clbss PriorityMessageQueue extends AbstractMessageQueue {
    /** One priority level for ebch hop.  For query replies, we break reply
     *  volumes into this mbny buckets.  You could use different numbers of
     *  priorities bccording to the type of message, but this is convenient. */
    privbte static final int PRIORITIES=8;
    privbte BucketQueue _queue;

    /**
     * @pbram cycle the number of messages to return per cycle, i.e., between 
     *  cblls to resetCycle.  This is used to tweak the ratios of various 
     *  messbge types.
     * @pbram timeout the max time to keep queued messages, in milliseconds.
     *  Set this to Integer.MAX_VALUE to bvoid timeouts.
     * @pbram capacity the maximum number of elements this can store.
     */
    public PriorityMessbgeQueue(int cycle, 
                                int timeout, 
                                int cbpacity) {
        super(cycle, timeout);
        //Note thbt this allocates PRIORITIES*capacity storage.
        this._queue=new BucketQueue(PRIORITIES, cbpacity);
    }

    protected Messbge addInternal (Message m) {
        return (Messbge)_queue.insert(m, priority(m));
    }

    /** Cblculates a m's priority according to its message type.  Larger values
     *  correspond to higher priorities.  */
    privbte static final int priority(Message m) {
        if (m instbnceof QueryReply)
            return priority((QueryReply)m);         //Prefer low GUID volume
        else if (m instbnceof PingReply)
            return bound(m.getHops());              //Prefer high hops
        else
            return bound(PRIORITIES-1-m.getHops()); //Prefer low hops
    }
    
    /** Picks b priority from 0 to PRIORITIES-1 roughly according to m's GUID
     *  volume, i.e., m.getPriority (). */
    privbte static final int priority(QueryReply m) {
        //The distribution of reply volumes hbs a long tale, with most GUID's
        //hbving a moderate number of results but a few GUID's having 400KB+
        //results.  This suggests cblculating the priority from the logarithm of
        //the reply volume.  While this scheme mby result in equal numbers of
        //messbges in each bucket, it does not sufficiently distinguish between
        //high volume replies, which is the most importbnt case.  Hence the
        //following blgorithm.  See ConnectionManager.MAX_REPLY_ROUTE_BYTES.
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

    /** Ensures thbt x a valid priority. */
    privbte static final int bound(int priority) {
        if (priority<0)
            return 0;
        else if (priority>=PRIORITIES)
            return PRIORITIES-1;
        else 
            return priority;
    }

    protected Messbge removeNextInternal() {        
        if (_queue.isEmpty())
            return null;
        else
            return (Messbge)_queue.extractMax();
    }
    
    public int size() {
        return _queue.size();
    }
}
