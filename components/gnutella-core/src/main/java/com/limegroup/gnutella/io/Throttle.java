package com.limegroup.gnutella.io;

import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.Collection;
import java.util.Iterator;

import java.nio.channels.SelectionKey;
import java.nio.channels.CancelledKeyException;

import com.limegroup.gnutella.util.IntWrapper;

/**
 * A throttle that can be applied to non-blocking reads & writes.
 *
 * The Throttle is NOT THREAD SAFE.  All calls should be done on the
 * NIODispatch thread.
 */
public class Throttle {
    
    /** The number of windows per second. */
    private static final int TICKS_PER_SECOND = 10;
    /** The number of milliseconds in each tick. */
    private static final int MILLIS_PER_TICK = 1000 / TICKS_PER_SECOND;
    
    /** Whether or not this throttle is for writing. (If false, it's for reading.) */
    private final boolean _write;
    
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
    private Set /* of Object (ThrottleListener.getAttachment()) */ interested = new HashSet();
    
    /**
     * Attachments that are ready-op'd.
     *
     * As they become ready, we add them with an Integer of (1).
     * When someone requests available data for writing, we see how many
     * ops are ready & divide the bandwidth among them all.
     */
    private Map /* of Object (ThrottleListener.getAttachment()) */ ready = new HashMap();
    
    /**
     * The total sum of interested parties values.
     * MUST BE EQUAL TO ready.values()'s getInt()'s summed.
     */
    private int totalReadies = 0;
    
    /**
     * Constructs a new Throttle that is either for writing or reading.
     * Use 'true' for writing, 'false' for reading.
     */
    public Throttle(boolean forWriting, float bytesPerSecond) {
        _write = forWriting;
        _bytesPerTick = (int)((float)bytesPerSecond / TICKS_PER_SECOND);
        NIODispatcher.instance().addThrottle(this);
    }
    
    /**
     * Notification from the NIODispatcher that a bunch of keys are now selectable.
     */
    void selectableKeys(Collection /* of SelectionKey */ keys) {
        System.out.println("Getting selectable keys");
        synchronized(LOCK) {
            if(!interested.isEmpty()) {
                for(Iterator i = keys.iterator(); i.hasNext(); ) {
                    SelectionKey key = (SelectionKey)i.next();
                    try {
                        if(key.isValid() && (_write ? key.isWritable() : key.isReadable())) {
                            Object att = key.attachment();
                            if(interested.contains(att)) {
                                IntWrapper value = (IntWrapper)ready.get(att);
                                if(value == null) {
                                    System.out.println("New interested ready: " + att);
                                    value = new IntWrapper(1);
                                    ready.put(att, value);
                                } else {
                                    System.out.println("Existing interested ready: " + att + ", at: " + value);
                                    value.addInt(1);
                                }
                                totalReadies++;
                            }
                        }
                    } catch(CancelledKeyException ignored) {}
                }
            }
        }
    }
    
    /**
     * Interests this ThrottleListener in being notified when bandwidth is available.
     */
    void interest(ThrottleListener writer, Object attachment) {
        System.out.println("Marking interest: " + attachment);
        synchronized(LOCK) {
            if(!interested.contains(attachment))
                requests.add(writer);
        }
    }
    
    /**
     * Requests some bytes to write.
     */
    int request(ThrottleListener writer, Object attachment) {
        IntWrapper value = (IntWrapper)ready.get(attachment);
        if(value == null)
            throw new IllegalStateException("requesting without being ready.  attachment: " + attachment);
            
        int divisor = totalReadies / value.getInt();
        int ret = 0;
        if(divisor != 0) {
            ret = _available / divisor;
            _available -= ret;
        }
        
        System.out.println("allowing: " + attachment + " to have: " + ret + " .. tr: " + totalReadies + ", div: " + divisor + ", left: " + _available);
        return ret; 
    }
    
    /**
     * Marks the ThrottleListener as having finished its request/write cycle.
     * 'amount' bytes are released back (they were unwritten).
     * If 'wroteAll' is true, the throttle wrote all the data it wanted to.
     */
    void release(int amount, boolean wroteAll, ThrottleListener writer, Object attachment) {
        IntWrapper value = (IntWrapper)ready.get(attachment);
        if(value == null)
            throw new IllegalStateException("requesting without being ready.  attachment: " + attachment);
            
        int val = value.getInt();
        if(wroteAll) {
            totalReadies -= val;
            ready.remove(attachment);
        }

        System.out.println("releasing " + attachment + ". am: " + amount + " wa: " + wroteAll + " total readies now: " + totalReadies);
        
        _available += amount;
        
        synchronized(LOCK) {
            interested.remove(attachment);
        }
    }
    
    /**
     * Notification from NIODispatcher that some time has passed.
     *
     * Returns true if all requests were satisifed.  Returns false if there are
     * still some requests that require further tick notifications.
     */
    public boolean tick(long currentTime) {
        if(currentTime >= _nextTickTime && !requests.isEmpty())
            spreadBandwidth(currentTime);
        else
            System.out.println("ignoring tick: " + currentTime);
        return requests.isEmpty();
    }
    
    /**
     * Notifies all requestors that bandwidth is available.
     */
    private void spreadBandwidth(long now) {
        System.out.println("ticked: " + now + ", prior avail: " + _available + ", setting to: " + _bytesPerTick);
        _available = _bytesPerTick;
        _nextTickTime = now + MILLIS_PER_TICK;
        
        synchronized(LOCK) {
            for(Iterator i = requests.iterator(); i.hasNext(); ) {
                ThrottleListener req = (ThrottleListener)i.next();
                // channel became closed.
                if(!req.bandwidthAvailable()) {
                    // make sure we clean up the readyset if it was there.
                    Object attachment = req.getAttachment();
                    IntWrapper value = (IntWrapper)ready.remove(attachment);
                    if(value != null)
                        totalReadies -= value.getInt();
                    interested.remove(attachment);
                    System.out.println("channel (" + attachment + ") closed, cleaning up.");
                } else {
                    System.out.println("Setting interest: " + req.getAttachment());
                    interested.add(req.getAttachment());
                }
            }
            requests.clear();
        }
    }
}
    
    