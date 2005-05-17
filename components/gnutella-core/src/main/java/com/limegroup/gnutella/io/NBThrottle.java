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
 * This does not work correctly if the throttle is below 2KB/s.
 *
 * NIODispatcher maintains a list of all active Throttles.  As part
 * of every pending operation, NIODispatcher will 'tick' a Throttle.
 * This will potentially increase the amount of available bytes that the
 * Throttle can give to requestors.  If bytes suddenly become available,
 * the Throttle will inform each ThrottleListener (that previously registered
 * interest) that bandwidth is now available, and record the fact that the listener
 * is now interested.  The listener must then register interest on its ChannelWriter,
 * which will ultimately register interest on the SocketChannel.
 * 
 * NIODispatcher will then perform its select call & inform each Throttle of the
 * available selected keys so that the Throttles can keep track of what interested
 * parties are now ready for writing.  As the Throttle learns of a new ready listener,
 * it increments a counter for that listener.  The listener will then request some
 * bytes to write, try writing, and release the bytes.  If, while releasing, the listener
 * said that it was able to write everything, the ready counter is erased.  However,
 * if there was still more to write, the ready counter is kept around and incremented
 * the next time it becomes ready.
 *
 * In order to determine how much data can be given to a specific listener when it requests,
 * the available bytes are divided by a specific divisor (calculated by the total amount of
 * counts for each ready listener divided by the counts for this listener).  The end result
 * is that those listeners who previously requested data but couldn't write it all are
 * given increasing priority to write more & more.  The idea behind this is to prevent fast
 * connections from hogging all the bandwidth (which would prevent slower connections from
 * writing out any data).
 *
 * A chain of events goes something like this
 *
 *      Throttle                            ThrottleListener                   NIODispatcher
 * 1)                                       Throttle.interest               
 * 2)   <adds to request list>
 * 3)                                                                         Throttle.tick
 * 4)   ThrottleListener.bandwidthAvailable
 * 5)                                       SocketChannel.interest
 * 6)   <moves from request to interest list>
 * 7)                                                                         Selector.select
 * 8)                                                                         Throttle.selectedKeys 
 * 9)   <moves from interest list to ready list>
 * 10)                                                                        NIOSocket.handleWrite
 * 11)                                      Throttle.request
 * 12)                                      SocketChannel.write
 * 13)                                      Throttle.release
 */
public class NBThrottle implements Throttle {
    
    private static final Log LOG = LogFactory.getLog(NBThrottle.class);
    
    /** The number of windows per second. */
    private static final int TICKS_PER_SECOND = 10;
    /** The number of milliseconds in each tick. */
    private static final int MILLIS_PER_TICK = 1000 / TICKS_PER_SECOND;
    /** The maximum amount to ever give anyone. */
    private static final int MAXIMUM_TO_GIVE = 1400;
    /** The minimum amount to ever give anyone. */
    private static final int MINIMUM_TO_GIVE = 30;
    
    /** Whether or not this throttle is for writing. (If false, it's for reading.) */
    private final boolean _write;
    
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
     * Constructs a new Throttle that is either for writing or reading.
     * Use 'true' for writing, 'false' for reading.
     */
    public NBThrottle(boolean forWriting, float bytesPerSecond) {
        _write = forWriting;
        _bytesPerTick = (int)((float)bytesPerSecond / TICKS_PER_SECOND);
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
                        Object attachment = key.attachment();
                        if(_interested.containsKey(attachment))
                            _ready.put(attachment, key);
                    }
                } catch(CancelledKeyException ignored) {
                    i.remove(); // it's cancelled, we can ignore it now & forever.
                }
            }
            
            if(LOG.isTraceEnabled())
                LOG.trace("Interested: " + _interested.size() + ", ready: " + _ready.size());
            
            _active = true;
            for(Iterator i = _interested.entrySet().iterator(); !_ready.isEmpty() && i.hasNext(); ) {
                Map.Entry next = (Map.Entry)i.next();
                ThrottleListener listener = (ThrottleListener)next.getValue();
                Object attachment = next.getKey();
                SelectionKey key = (SelectionKey)_ready.remove(attachment);
                if(!listener.isOpen()) {
                    if(LOG.isTraceEnabled())
                        LOG.trace("Removing closed but interested party: " + next.getKey());
                    i.remove();
                } else if(key != null) {
                    NIODispatcher.instance().process(key, attachment, _write ? SelectionKey.OP_WRITE : SelectionKey.OP_READ);
                    i.remove();
                    if(_available < MINIMUM_TO_GIVE)
                        break;
                } else if(LOG.isTraceEnabled()) {
                    LOG.trace("Interested but not ready: " + attachment);
                }
            }
            _active = false;
        }
    }
    
    /**
     * Interests this ThrottleListener in being notified when bandwidth is available.
     */
    public void interest(ThrottleListener writer, Object attachment) {
        synchronized(_requests) {
            _requests.add(writer);
        }
    }
    
    /**
     * Requests some bytes to write.
     */
    public int request(ThrottleListener writer, Object attachment) {
        if(!_active) // this is gonna happen from NIODispatcher's processing
            return 0;
        
        int ret = Math.min(_available, MAXIMUM_TO_GIVE);
        _available -= ret;
        LOG.trace("GAVE: " + ret + ", REMAINING: " + _available + ", TO: " + attachment);
        return ret; 
    }
    
    /**
     * Marks the ThrottleListener as having finished its request/write cycle.
     * 'amount' bytes are released back (they were unwritten).
     * If 'wroteAll' is true, the throttle wrote all the data it wanted to.
     */
    public void release(int amount, boolean wroteAll, ThrottleListener writer, Object attachment) {
        _available += amount;
        LOG.trace("RETR: " + amount + ", REMAINING: " + _available + ", ALL: " + wroteAll + ", FROM: " + attachment);
    }
    
    /**
     * Notification from NIODispatcher that some time has passed.
     *
     * Returns true if all requests were satisifed.  Returns false if there are
     * still some requests that require further tick notifications.
     */
    void tick(long currentTime) {
        if(currentTime >= _nextTickTime) {
            _available = _bytesPerTick;
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
    
    