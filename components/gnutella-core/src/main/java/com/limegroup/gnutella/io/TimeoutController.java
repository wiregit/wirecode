package com.limegroup.gnutella.io;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.util.BinaryHeap;

/**
 * Keeps track of a bunch of things that want to be timed out.
 */
public class TimeoutController {
    
    private static final Log LOG = LogFactory.getLog(TimeoutController.class);
    
    private final BinaryHeap items = new BinaryHeap(20, true);
    
    // used to store timed out items to notify outside of lock.
    private final List timedout = new ArrayList(100);
    
    synchronized int getNumPendingTimeouts() {
        return items.size();
    }
    
    /** Adds a timeoutable to be timed out at timeout + now. */
    public synchronized void addTimeout(Timeoutable t, long now, long timeout) {
        if(LOG.isDebugEnabled())
            LOG.debug("Adding timeoutable: " + t + ", now: " + now + ", timeout: " + timeout);
        items.insert(new Timeout(t, now, timeout));
    }

    /**
     * Processes all the items that can be timed out.
     */
    public void processTimeouts(long now) {
        synchronized(this) {
            while(!items.isEmpty()) {
                Timeout t = (Timeout)items.getMax();
                if(t != null && now >= t.expireTime) {
                    if(LOG.isDebugEnabled())
                        LOG.debug("Timing out: " + t + ", expired: " + t.expireTime + ", now: " + now + ", length: " + t.timeoutLength);
                    timedout.add(t);
                } else {
                    if(t != null && LOG.isDebugEnabled())
                        LOG.debug("Breaking -- next timeout at: " + t.expireTime + ", now: " + now);
                    break;
                }
                items.extractMax(); // remove it -- we only peeked before.
            }
        }
        
        for(int i = 0; i < timedout.size(); i++) {
            Timeout t = (Timeout)timedout.get(i);
            t.timeoutable.notifyTimeout(now, t.expireTime, t.timeoutLength);
        }
        timedout.clear();
    }
    
    /** Gets the time the next timeoutable expires. -1 for never. */
    public synchronized long getNextExpireTime() {
        if(items.isEmpty())
            return -1;
        else
            return ((Timeout)items.getMax()).expireTime;
    }
    
    /** Keep an expireTime & timeoutable together as one happy couple. */
    private static class Timeout implements Comparable {
        private long expireTime;
        private Timeoutable timeoutable;
        private long timeoutLength;
        
        Timeout(Timeoutable timeoutable, long now, long timeout) {
            this.expireTime = now + timeout;
            this.timeoutLength = timeout;
            this.timeoutable = timeoutable;
        }
        
        /**
         * Makes items that expire sooner considered 'larger' so the max BinaryHeap is
         * sorted correctly
         */
        public int compareTo(Object o) {
            Timeout b = (Timeout)o;
            return expireTime < b.expireTime ? 1 : expireTime > b.expireTime ? -1 : 0;
        }
        
        public String toString() {
            return "TimeoutWrapper for: " + timeoutable;
        }
    }
}
