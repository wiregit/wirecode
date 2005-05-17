package com.limegroup.gnutella.io;

import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.WeakHashMap;
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
    /** The minimum amount to allow a starved attachment. */
    private static final int MINIMUM_FOR_STARVED = 150;
    /** The minimum amount to allow for a hungry attachment. */
    private static final int MINIMUM_FOR_HUNGRY = 50;
    
    /** Whether or not this throttle is for writing. (If false, it's for reading.) */
    private final boolean _write;
    
    /**
     * Whether or not this is the first bandwidth spread after a tick.
     * If it isn't, we don't give extra data to the hungry & starved fellows.
     */
    private boolean _initial;
    
    /** The amount that is available every tick. */
    private int _bytesPerTick;
    
    /** The amount currently available in this tick. */
    private int _available;
    
    /** The next time a tick should occur. */
    private long _nextTickTime = -1;
    
    /** A lock for protecting requests/interested. */
    private final Object LOCK = new Object();
    
    /**
     * A list of ThrottleListeners that are interested in bandwidthAvailable events.
     *
     * As ThrottleListeners interest themselves interest themselves for writing, 
     * the requests are queued up here.  When bandwidth is available the request is
     * moved over to 'interested' after informing the ThrottleListener that bandwidth
     * is available.  New ThrottleListeners should not be added to this if they are
     * already in interested.
     */
    private Set /* of ThrottleListener */ requests = new HashSet();
    
    /**
     * Set of interested attachments.  
     *
     * After a ThrottleListener is informed that bandwidth is available, its attachment
     * is added to this Set.  When SelectionKeys are ready for writing, they are placed
     * in the ready set iff the attachment was in this interested set.  Objects
     * are removed from the set after requesting some space.
     */
    private Map /* of Object (ThrottleListener.getAttachment()) -> ThrottleListener */ interested = new WeakHashMap();
    
    /**
     * Attachments that are ready-op'd.
     *
     * As they become ready, we add them with an Integer of (1).
     * When someone requests available data for writing, we see how many
     * ops are ready & divide the bandwidth among them all.
     */
    private Map /* of Object (ThrottleListener.getAttachment()) */ ready = new WeakHashMap();
    
    /** The attachment that has the most ready ops. */
    private Object starvedAttachment;
    
    /** The attachment that has the second most ready ops. */
    private Object hungryAttachment;
    
    /**
     * The total sum of interested parties values.
     * MUST BE EQUAL TO ready.values()'s getInt()'s summed.
     */
    private double totalReadies = 0;
    
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
        // We do not need to lock interested because it is only modified on this thread.
        synchronized(LOCK) {
            if(!interested.isEmpty()) {
                starvedAttachment = null;
                hungryAttachment = null;
                int starvedVal = -1;
                int hungryVal = -1;
                totalReadies = 0;
                Set currentReady = new HashSet();
                
                for(Iterator i = keys.iterator(); i.hasNext(); ) {
                    SelectionKey key = (SelectionKey)i.next();
                    try {
                        if(key.isValid() && (_write ? key.isWritable() : key.isReadable())) {
                            Object att = key.attachment();
                            currentReady.add(att);
                            if(interested.remove(att) != null) {
                                MutableInt value = (MutableInt)ready.get(att);
                                if(value == null) {
                                    value = new MutableInt(1);
                                    ready.put(att, value);
                                } else {
                                    value.x += 1;
                                }
                                totalReadies += value.x;
                                LOG.trace("Ready (" + value.x + "): " + att);
                                
                                if(_initial) {
                                    if(value.x > starvedVal) {
                                        starvedAttachment = att;
                                        starvedVal = value.x;
                                    } else if( value.x > hungryVal) {
                                        hungryAttachment = att;
                                        hungryVal = value.x;
                                    }
                                }
                            }
                        }
                    } catch(CancelledKeyException ignored) {}
                }
                
                for(Iterator i = interested.entrySet().iterator(); i.hasNext(); ) {
                    Map.Entry next = (Map.Entry)i.next();
                    if(!((ThrottleListener)next.getValue()).isOpen()) {
                        LOG.trace("Removing closed but interested party: " + next.getKey());
                        i.remove();
                    } else {
                        LOG.trace("Interested but not ready: " + next.getKey());
                    }
                }
                
                Set oldReady = new HashSet(ready.keySet());
                oldReady.removeAll(currentReady);
                LOG.trace("Was ready, but not ready now (" + oldReady.size() + "): " + oldReady);
                    
                
                LOG.trace("Starting new cycle.  Ready: " + ready.size());
            }
        }
    }
    
    /**
     * Interests this ThrottleListener in being notified when bandwidth is available.
     */
    public void interest(ThrottleListener writer, Object attachment) {
        synchronized(LOCK) { // need to lock because interested is mutated elsewhere
            if(!interested.containsKey(attachment))
                requests.add(writer);
        }
    }
    
    /**
     * Requests some bytes to write.
     */
    public int request(ThrottleListener writer, Object attachment) {
        MutableInt value = (MutableInt)ready.get(attachment);
        if(value == null)
            throw new IllegalStateException("requesting without being ready.  attachment: " + attachment);

        int ret = 0;

        if(attachment == starvedAttachment) 
            ret = Math.min(_available, MINIMUM_FOR_STARVED);
        else if(attachment == hungryAttachment)
            ret = Math.min(_available, MINIMUM_FOR_HUNGRY);
            
        double divisor = totalReadies / value.x;
        if(divisor != 0)
            ret = Math.max(ret, (int)Math.floor(_available / divisor));
        
        _available -= ret;
        
        LOG.trace("GAVE: " + ret + ", REMAINING: " + _available + ", TO: " + attachment);
        
        // if this can't write anything this time, leave its interest for the future.
        // This is required because of a bug(?) that does not notify us about the same
        // write availability if we do not act upon it the first time.
        if(ret == 0) {
            synchronized(LOCK) {
                interested.put(attachment, writer);
            }
        }
        
        return ret; 
    }
    
    /**
     * Marks the ThrottleListener as having finished its request/write cycle.
     * 'amount' bytes are released back (they were unwritten).
     * If 'wroteAll' is true, the throttle wrote all the data it wanted to.
     */
    public void release(int amount, boolean wroteAll, ThrottleListener writer, Object attachment) {
        MutableInt value = (MutableInt)ready.get(attachment);
        if(value == null)
            throw new IllegalStateException("requesting without being ready.  attachment: " + attachment);
            
        totalReadies -= value.x;
        
        if(wroteAll)
            ready.remove(attachment);
        
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
        if(currentTime >= _nextTickTime && !requests.isEmpty()) {
            _available = _bytesPerTick;
            _nextTickTime = currentTime + MILLIS_PER_TICK;
            _initial = true;
            spreadBandwidth();
        } else if(_available > 0) {
            _initial = false;
            spreadBandwidth();
        }
    }
    
    /**
     * Notifies all requestors that bandwidth is available.
     */
    private void spreadBandwidth() {
        synchronized(LOCK) {
            for(Iterator i = requests.iterator(); i.hasNext(); ) {
                ThrottleListener req = (ThrottleListener)i.next();
                // channel became closed.
                if(!req.bandwidthAvailable()) {
                    // make sure we clean up the readyset if it was there.
                    Object attachment = req.getAttachment();
                    ready.remove(attachment);
                    interested.remove(attachment);
                } else {
                    interested.put(req.getAttachment(), req);
                }
            }
            requests.clear();
        }
    }
    
    private static final class MutableInt {
        private int x;
        MutableInt(int x) {
            this.x = x;
        }
    }
}
    
    