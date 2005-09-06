package com.limegroup.gnutella.io;

import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Collection;
import java.util.Iterator;

import java.nio.channels.SelectionKey;
import java.nio.channels.CancelledKeyException;

import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

/**
 * A throttle that can be applied to non-blocking reads & writes.
 *
 * Throttles work by giving amounts to interested parties in a FIFO
 * queue, ensuring that no one party uses all of the available bandwidth
 * on every tick.
 *
 * Listeners must adhere to the following contract in order for the Throttle to be effective.
 
 * In order:
 * 1) When a listener wants to write, it must interest ONLY the Throttle.
 *    Call: Throttle.interest(listener)
 *
 * 2) When the throttle informs the listener that bandwidth is available, it must interest
 *    the next party in the chain (ultimately the Socket).
 *    Callback: ThrottleListener.bandwidthAvailable()
 *
 * 3) The listener must request data prior to writing, and write out only the amount
 *    that it requested.
 *    Call: Throttle.request()
 *
 * 4) The listener must release data that it was given from a request but did not write.
 *    Call: Throttle.release(amount)
 *
 * Extraneous: The ThrottleListener must have an 'attachment' set that is the same attachment
 *             as the one used on the SelectionKey for the SelectableChannel.  This is
 *             necessary so that Throttle can match up SelectionKey ready events
 *             with ThrottleListener interest.
 *
 * The flow of a Throttle works like:
 *      Throttle                            ThrottleListener                   NIODispatcher
 * 1)                                       Throttle.interest               
 * 2)   <adds to request list>
 * 3)                                                                         Throttle.tick
 * 4)   ThrottleListener.bandwidthAvailable
 * 5)                                       SocketChannel.interest
 * 6)   <moves from request to interest list>
 * 7)                                                                         Selector.select
 * 8)                                                                         Throttle.selectableKeys 
 * 9)                                       Throttle.request
 * 10)                                      SocketChannel.write
 * 11)                                      Throttle.release
 * 12)  <remove from interest>
 *
 * If there are multiple listeners, steps 4 & 5 are repeated for each request, and steps 9 through 12
 * are performed on interested parties until there is no bandwidth available.  Another tick will
 * generate more bandwidth, which will allow previously interested parties to request/write/release.
 * Because interested parties are processed in FIFO order, all parties will receive equal access to
 * the bandwidth.
 *
 * Note that due to the nature of Throttle & NIODispatcher, ready parties may be told to handleWrite
 * twice during each selection event.  The latter will always return 0 to a request.
 */
public class NBThrottle implements Throttle {
    
    private static final Log LOG = LogFactory.getLog(NBThrottle.class);
    
    /** The maximum amount to ever give anyone. */
    private static final int MAXIMUM_TO_GIVE = 1400;
    /** The minimum amount to ever give anyone. */
    private static final int MINIMUM_TO_GIVE = 30;

    private static final int DEFAULT_TICK_TIME = 100;
    
    /** The number of milliseconds in each tick. */
    private final int MILLIS_PER_TICK;
    
    /** Whether or not this throttle is for writing. (If false, it's for reading.) */
    private final boolean _write;
    
    /** The op that this uses when processing. */
    private final int _processOp;
    
    /** The amount that is available every tick. */
    private int _bytesPerTick;
    
    /** The amount currently available in this tick. */
    private int _available;
    
    /** The next time a tick should occur. */
    private long _nextTickTime = -1;
    
    /**
     * A list of ThrottleListeners that are interested in bandwidthAvailable events.
     *
     * As ThrottleListeners interest themselves interest themselves for writing, 
     * the requests are queued up here.  When bandwidth is available the request is
     * moved over to 'interested' after informing the ThrottleListener that bandwidth
     * is available.  New ThrottleListeners should not be added to this if they are
     * already in interested.
     */
    private Set /* of ThrottleListener */ _requests = new HashSet();
    
    /**
     * Attachments that are interested -> ThrottleListener that owns the attachment.
     *
     * As new items become interested, they are added to the bottom of the set.
     * When something is written, so long as it writes > 0, it is removed from the
     * list (and put back at the bottom).
     */
    private Map /* of Object (ThrottleListener.getAttachment()) -> ThrottleListener */ _interested = new LinkedHashMap();
    
    /**
     * Attachments that are ready-op'd.
     *
     * This is temporary per each selectableKeys call, but is cached to avoid regenerating
     * each time.
     */
    private Map /* of Object (ThrottleListener.getAttachment()) */ _ready = new HashMap();
    
    /** Whether or not we're currently active in the selectableKeys portion. */
    private boolean _active = false;
    
    /**
     * Constructs a throttle using the default values for latency & availability.
     */
    public NBThrottle(boolean forWriting, float bytesPerSecond) {
        this(forWriting, bytesPerSecond, true, DEFAULT_TICK_TIME);
    }

    /**
     * Constructs a throttle that is either for reading or reading with the maximum bytesPerSecond.
     *
     * The Throttle is tuned to expect 'maxRequestors' requesting data, allowing only the 'maxLatency'
     * delay between serviced requests for any given requestor.
     *
     * The values are only recommendations and may be ignored (within limits) by the Throttle
     * in order to ensure that the Throttle behaves correctly.
     */
    public NBThrottle(boolean forWriting, float bytesPerSecond, int maxRequestors, int maxLatency) {
        this(forWriting, bytesPerSecond, true,  maxRequestors == 0 ? DEFAULT_TICK_TIME : maxLatency / maxRequestors);
    }
    
    /**
     * Constructs a new Throttle that is either for writing or reading, allowing
     * the given bytesPerSecond.
     *
     * Use 'true' for writing, 'false' for reading.
     *
     * If addToDispatcher is false, NIODispatcher is not notified about the Throttle,
     * so it will not be automatically ticked or told of selectable keys.
     *
     * The throttle will allow bandwidth spreading every millisPerTick, after
     * enforcing it's between 50 & 100.
     */
    protected NBThrottle(boolean forWriting, float bytesPerSecond, 
                         boolean addToDispatcher, int millisPerTick) {
        MILLIS_PER_TICK = Math.min(100, Math.max(50,millisPerTick));
        int ticksPerSecond = 1000 / millisPerTick;
        _write = forWriting;
        _processOp = forWriting ? SelectionKey.OP_WRITE : SelectionKey.OP_READ;
        _bytesPerTick = (int)((float)bytesPerSecond / ticksPerSecond);
        if(addToDispatcher)
            NIODispatcher.instance().addThrottle(this);
    }
    
    /**
     * Notification from the NIODispatcher that a bunch of keys are now selectable.
     */
    void selectableKeys(Collection /* of SelectionKey */ keys) {
        if(_available >= MINIMUM_TO_GIVE && !_interested.isEmpty()) {
            for(Iterator i = keys.iterator(); i.hasNext(); ) {
                SelectionKey key = (SelectionKey)i.next();
                try {
                    if(key.isValid() && (_write ? key.isWritable() : key.isReadable())) {
                        Object attachment = NIODispatcher.instance().attachment(key.attachment());
                        if(_interested.containsKey(attachment))
                            _ready.put(attachment, key);
                    }
                } catch(CancelledKeyException ignored) {
                    i.remove(); // it's cancelled, we can ignore it now & forever.
                }
            }
            
            //LOG.trace("Interested: " + _interested.size() + ", ready: " + _ready.size());
            
            _active = true;
            for(Iterator i = _interested.entrySet().iterator(); !_ready.isEmpty() && i.hasNext(); ) {
                Map.Entry next = (Map.Entry)i.next();
                ThrottleListener listener = (ThrottleListener)next.getValue();
                Object attachment = next.getKey();
                SelectionKey key = (SelectionKey)_ready.remove(attachment);
                if(!listener.isOpen()) {
                    //LOG.trace("Removing closed but interested party: " + next.getKey());
                    i.remove();
                } else if(key != null) {
                    NIODispatcher.instance().process(key, key.attachment(), _processOp);
                    i.remove();
                    if(_available < MINIMUM_TO_GIVE)
                        break;
                }
            }
            _active = false;
        }
    }
    
    /**
     * Interests this ThrottleListener in being notified when bandwidth is available.
     */
    public void interest(ThrottleListener writer) {
        synchronized(_requests) {
            _requests.add(writer);
        }
    }
    
    /**
     * Requests some bytes to write.
     */
    public int request() {
        if(!_active) // this is gonna happen from NIODispatcher's processing
            return 0;
        
        int ret = Math.min(_available, MAXIMUM_TO_GIVE);
        _available -= ret;
        //LOG.trace("GAVE: " + ret + ", REMAINING: " + _available + ", TO: " + attachment);
        return ret; 
    }
    
    /**
     * Releases some unwritten bytes back to the available pool.
     */
    public void release(int amount) {
        _available += amount;
        //LOG.trace("RETR: " + amount + ", REMAINING: " + _available + ", ALL: " + wroteAll + ", FROM: " + attachment);
    }
    
    /**
     * Notification from NIODispatcher that some time has passed.
     *
     * Returns true if all requests were satisifed.  Returns false if there are
     * still some requests that require further tick notifications.
     */
    void tick(long currentTime) {
        if(currentTime >= _nextTickTime) {
            float elapsedTicks = 1 + ((float)(currentTime - _nextTickTime)) / MILLIS_PER_TICK;
            elapsedTicks = Math.min(elapsedTicks, 2);
            _available = (int)(_bytesPerTick * elapsedTicks);
            _nextTickTime = currentTime + MILLIS_PER_TICK;
            spreadBandwidth();
        } else if(_available > MINIMUM_TO_GIVE) {
            spreadBandwidth();
        }
    }
    
    /**
     * Notifies all requestors that bandwidth is available.
     */
    private void spreadBandwidth() {
        synchronized(_requests) {
            if(!_requests.isEmpty()) {
                for(Iterator i = _requests.iterator(); i.hasNext(); ) {
                    ThrottleListener req = (ThrottleListener)i.next();
                    Object attachment = req.getAttachment();
                    if(!_interested.containsKey(attachment)) {
                        if(req.bandwidthAvailable())
                            _interested.put(attachment, req);
                        // else it'll be cleared when we loop later on.
                    }
                }
                _requests.clear();
            }
        }
    }
}
    
    
