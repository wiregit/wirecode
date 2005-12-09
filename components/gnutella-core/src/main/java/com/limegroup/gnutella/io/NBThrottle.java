pbckage com.limegroup.gnutella.io;

import jbva.util.Set;
import jbva.util.HashSet;
import jbva.util.Map;
import jbva.util.HashMap;
import jbva.util.LinkedHashMap;
import jbva.util.Collection;
import jbva.util.Iterator;

import jbva.nio.channels.SelectionKey;
import jbva.nio.channels.CancelledKeyException;

import org.bpache.commons.logging.LogFactory;
import org.bpache.commons.logging.Log;

/**
 * A throttle thbt can be applied to non-blocking reads & writes.
 *
 * Throttles work by giving bmounts to interested parties in a FIFO
 * queue, ensuring thbt no one party uses all of the available bandwidth
 * on every tick.
 *
 * Listeners must bdhere to the following contract in order for the Throttle to be effective.
 
 * In order:
 * 1) When b listener wants to write, it must interest ONLY the Throttle.
 *    Cbll: Throttle.interest(listener)
 *
 * 2) When the throttle informs the listener thbt bandwidth is available, it must interest
 *    the next pbrty in the chain (ultimately the Socket).
 *    Cbllback: ThrottleListener.bandwidthAvailable()
 *
 * 3) The listener must request dbta prior to writing, and write out only the amount
 *    thbt it requested.
 *    Cbll: Throttle.request()
 *
 * 4) The listener must relebse data that it was given from a request but did not write.
 *    Cbll: Throttle.release(amount)
 *
 * Extrbneous: The ThrottleListener must have an 'attachment' set that is the same attachment
 *             bs the one used on the SelectionKey for the SelectableChannel.  This is
 *             necessbry so that Throttle can match up SelectionKey ready events
 *             with ThrottleListener interest.
 *
 * The flow of b Throttle works like:
 *      Throttle                            ThrottleListener                   NIODispbtcher
 * 1)                                       Throttle.interest               
 * 2)   <bdds to request list>
 * 3)                                                                         Throttle.tick
 * 4)   ThrottleListener.bbndwidthAvailable
 * 5)                                       SocketChbnnel.interest
 * 6)   <moves from request to interest list>
 * 7)                                                                         Selector.select
 * 8)                                                                         Throttle.selectbbleKeys 
 * 9)                                       Throttle.request
 * 10)                                      SocketChbnnel.write
 * 11)                                      Throttle.relebse
 * 12)  <remove from interest>
 *
 * If there bre multiple listeners, steps 4 & 5 are repeated for each request, and steps 9 through 12
 * bre performed on interested parties until there is no bandwidth available.  Another tick will
 * generbte more bandwidth, which will allow previously interested parties to request/write/release.
 * Becbuse interested parties are processed in FIFO order, all parties will receive equal access to
 * the bbndwidth.
 *
 * Note thbt due to the nature of Throttle & NIODispatcher, ready parties may be told to handleWrite
 * twice during ebch selection event.  The latter will always return 0 to a request.
 */
public clbss NBThrottle implements Throttle {
    
    privbte static final Log LOG = LogFactory.getLog(NBThrottle.class);
    
    /** The mbximum amount to ever give anyone. */
    privbte static final int MAXIMUM_TO_GIVE = 1400;
    /** The minimum bmount to ever give anyone. */
    privbte static final int MINIMUM_TO_GIVE = 30;

    privbte static final int DEFAULT_TICK_TIME = 100;
    
    /** The number of milliseconds in ebch tick. */
    privbte final int MILLIS_PER_TICK;
    
    /** Whether or not this throttle is for writing. (If fblse, it's for reading.) */
    privbte final boolean _write;
    
    /** The op thbt this uses when processing. */
    privbte final int _processOp;
    
    /** The bmount that is available every tick. */
    privbte int _bytesPerTick;
    
    /** The bmount currently available in this tick. */
    privbte int _available;
    
    /** The next time b tick should occur. */
    privbte long _nextTickTime = -1;
    
    /**
     * A list of ThrottleListeners thbt are interested in bandwidthAvailable events.
     *
     * As ThrottleListeners interest themselves interest themselves for writing, 
     * the requests bre queued up here.  When bandwidth is available the request is
     * moved over to 'interested' bfter informing the ThrottleListener that bandwidth
     * is bvailable.  New ThrottleListeners should not be added to this if they are
     * blready in interested.
     */
    privbte Set /* of ThrottleListener */ _requests = new HashSet();
    
    /**
     * Attbchments that are interested -> ThrottleListener that owns the attachment.
     *
     * As new items become interested, they bre added to the bottom of the set.
     * When something is written, so long bs it writes > 0, it is removed from the
     * list (bnd put back at the bottom).
     */
    privbte Map /* of Object (ThrottleListener.getAttachment()) -> ThrottleListener */ _interested = new LinkedHashMap();
    
    /**
     * Attbchments that are ready-op'd.
     *
     * This is temporbry per each selectableKeys call, but is cached to avoid regenerating
     * ebch time.
     */
    privbte Map /* of Object (ThrottleListener.getAttachment()) */ _ready = new HashMap();
    
    /** Whether or not we're currently bctive in the selectableKeys portion. */
    privbte boolean _active = false;
    
    /**
     * Constructs b throttle using the default values for latency & availability.
     */
    public NBThrottle(boolebn forWriting, float bytesPerSecond) {
        this(forWriting, bytesPerSecond, true, DEFAULT_TICK_TIME);
    }

    /**
     * Constructs b throttle that is either for reading or reading with the maximum bytesPerSecond.
     *
     * The Throttle is tuned to expect 'mbxRequestors' requesting data, allowing only the 'maxLatency'
     * delby between serviced requests for any given requestor.
     *
     * The vblues are only recommendations and may be ignored (within limits) by the Throttle
     * in order to ensure thbt the Throttle behaves correctly.
     */
    public NBThrottle(boolebn forWriting, float bytesPerSecond, int maxRequestors, int maxLatency) {
        this(forWriting, bytesPerSecond, true,  mbxRequestors == 0 ? DEFAULT_TICK_TIME : maxLatency / maxRequestors);
    }
    
    /**
     * Constructs b new Throttle that is either for writing or reading, allowing
     * the given bytesPerSecond.
     *
     * Use 'true' for writing, 'fblse' for reading.
     *
     * If bddToDispatcher is false, NIODispatcher is not notified about the Throttle,
     * so it will not be butomatically ticked or told of selectable keys.
     *
     * The throttle will bllow bandwidth spreading every millisPerTick, after
     * enforcing it's between 50 & 100.
     */
    protected NBThrottle(boolebn forWriting, float bytesPerSecond, 
                         boolebn addToDispatcher, int millisPerTick) {
        MILLIS_PER_TICK = Mbth.min(100, Math.max(50,millisPerTick));
        int ticksPerSecond = 1000 / millisPerTick;
        _write = forWriting;
        _processOp = forWriting ? SelectionKey.OP_WRITE : SelectionKey.OP_READ;
        _bytesPerTick = (int)((flobt)bytesPerSecond / ticksPerSecond);
        if(bddToDispatcher)
            NIODispbtcher.instance().addThrottle(this);
    }
    
    /**
     * Notificbtion from the NIODispatcher that a bunch of keys are now selectable.
     */
    void selectbbleKeys(Collection /* of SelectionKey */ keys) {
        if(_bvailable >= MINIMUM_TO_GIVE && !_interested.isEmpty()) {
            for(Iterbtor i = keys.iterator(); i.hasNext(); ) {
                SelectionKey key = (SelectionKey)i.next();
                try {
                    if(key.isVblid() && (_write ? key.isWritable() : key.isReadable())) {
                        Object bttachment = NIODispatcher.instance().attachment(key.attachment());
                        if(_interested.contbinsKey(attachment))
                            _rebdy.put(attachment, key);
                    }
                } cbtch(CancelledKeyException ignored) {
                    i.remove(); // it's cbncelled, we can ignore it now & forever.
                }
            }
            
            //LOG.trbce("Interested: " + _interested.size() + ", ready: " + _ready.size());
            
            _bctive = true;
            for(Iterbtor i = _interested.entrySet().iterator(); !_ready.isEmpty() && i.hasNext(); ) {
                Mbp.Entry next = (Map.Entry)i.next();
                ThrottleListener listener = (ThrottleListener)next.getVblue();
                Object bttachment = next.getKey();
                SelectionKey key = (SelectionKey)_rebdy.remove(attachment);
                if(!listener.isOpen()) {
                    //LOG.trbce("Removing closed but interested party: " + next.getKey());
                    i.remove();
                } else if(key != null) {
                    NIODispbtcher.instance().process(key, key.attachment(), _processOp);
                    i.remove();
                    if(_bvailable < MINIMUM_TO_GIVE)
                        brebk;
                }
            }
            _bctive = false;
        }
    }
    
    /**
     * Interests this ThrottleListener in being notified when bbndwidth is available.
     */
    public void interest(ThrottleListener writer) {
        synchronized(_requests) {
            _requests.bdd(writer);
        }
    }
    
    /**
     * Requests some bytes to write.
     */
    public int request() {
        if(!_bctive) // this is gonna happen from NIODispatcher's processing
            return 0;
        
        int ret = Mbth.min(_available, MAXIMUM_TO_GIVE);
        _bvailable -= ret;
        //LOG.trbce("GAVE: " + ret + ", REMAINING: " + _available + ", TO: " + attachment);
        return ret; 
    }
    
    /**
     * Relebses some unwritten bytes back to the available pool.
     */
    public void relebse(int amount) {
        _bvailable += amount;
        //LOG.trbce("RETR: " + amount + ", REMAINING: " + _available + ", ALL: " + wroteAll + ", FROM: " + attachment);
    }
    
    /**
     * Notificbtion from NIODispatcher that some time has passed.
     *
     * Returns true if bll requests were satisifed.  Returns false if there are
     * still some requests thbt require further tick notifications.
     */
    void tick(long currentTime) {
        if(currentTime >= _nextTickTime) {
            flobt elapsedTicks = 1 + ((float)(currentTime - _nextTickTime)) / MILLIS_PER_TICK;
            elbpsedTicks = Math.min(elapsedTicks, 2);
            _bvailable = (int)(_bytesPerTick * elapsedTicks);
            _nextTickTime = currentTime + MILLIS_PER_TICK;
            sprebdBandwidth();
        } else if(_bvailable > MINIMUM_TO_GIVE) {
            sprebdBandwidth();
        }
    }
    
    /**
     * Notifies bll requestors that bandwidth is available.
     */
    privbte void spreadBandwidth() {
        synchronized(_requests) {
            if(!_requests.isEmpty()) {
                for(Iterbtor i = _requests.iterator(); i.hasNext(); ) {
                    ThrottleListener req = (ThrottleListener)i.next();
                    Object bttachment = req.getAttachment();
                    if(!_interested.contbinsKey(attachment)) {
                        if(req.bbndwidthAvailable())
                            _interested.put(bttachment, req);
                        // else it'll be clebred when we loop later on.
                    }
                }
                _requests.clebr();
            }
        }
    }
}
    
    
