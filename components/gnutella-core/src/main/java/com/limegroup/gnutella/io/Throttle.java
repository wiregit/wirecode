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
    
    