package com.limegroup.gnutella.io;

import com.limegroup.gnutella.util.BinaryHeap;

/**
 * Keeps track of a bunch of things that want to be timed out.
 */
public class TimeoutController {
    
    private final BinaryHeap items = new BinaryHeap(20, true);
    
    /** Adds a timeoutable to be timed out at timeout + now. */
    public void addTimeout(Timeoutable t, long now, long timeout) {
        items.insert(new Timeout(t, now, timeout));
    }

    /**
     * Processes all the items that can be timed out.
     */
    public void processTimeouts(long now) {
        while(!items.isEmpty()) {
            Timeout t = (Timeout)items.getMax();
            if(t != null && now >= t.expireTime)
                t.timeoutable.notifyTimeout(now, t.expireTime, t.timeoutLength);
            else
                break;
            items.extractMax(); // remove it -- we only peeked before.
        }
    }
    
    /** Gets the length of time till the next timeoutable expires. -1 for never. */
    public long getNextExpireTime() {
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
    }
}
