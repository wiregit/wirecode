package com.limegroup.gnutella;

import com.limegroup.gnutella.util.*;
import com.limegroup.gnutella.messages.*;
import com.sun.java.util.collections.*;

/**
 * This class caches pongs from the network.  Caching pongs saves considerable
 * bandwidth because only a controlled number of pings are sent to maintain
 * adequate host data, with Ultrapeers caching and responding to pings with
 * the best pongs available.  
 */
public final class PongCacher {

    /**
     * Single <tt>PongCacher</tt> instance, following the singleton pattern.
     */
    private static final PongCacher INSTANCE = new PongCacher();    

    /**
     * Constant for the number of cached pongs returned in response to
     * pings.  Public to make testing easier.
     */
    public static final int NUM_CACHED_PONGS = 4;

    /**
     * Constant for the number of hops to keep track of in our pong cache.
     */
    private static final int NUM_HOPS = 8;

    /**
     * <tt>BucketQueue</tt> holding pongs separated by hops.
     */
    private static final BucketQueue PONGS =
        new BucketQueue(NUM_HOPS, NUM_CACHED_PONGS);

    /**
     * Returns the single <tt>PongCacher</tt> instance.
     */
    public static PongCacher instance() {
        return INSTANCE;
    }    

    /**
     * Private constructor to ensure only one instance is created.
     */
    private PongCacher() {}


    /**
     * Accessor for the <tt>Set</tt> of cached pongs.  This <tt>List</tt>
     * is unmodifiable and will throw <tt>IllegalOperationException</tt> if
     * it is modified.
     *
     * @return the <tt>List</tt> of cached pongs -- continually updated
     */
    public List getBestPongs() {
        synchronized(PONGS) {
            Iterator iter = PONGS.iterator();
            int i = 0;
            List pongs = new LinkedList();
            while(iter.hasNext() && i<NUM_CACHED_PONGS) {
                pongs.add((PingReply)iter.next());
                i++;
            }
            return pongs;
        }
    }

    /**
     * Adds the specified <tt>PingReply</tt> instance to the cache of pongs.
     *
     * @param pr the <tt>PingReply</tt> to add
     */
    public void addPong(PingReply pr) {
        
        // if we're not an Ultrapeer, we don't care about caching the pong
        if(!RouterService.isSupernode()) return;

        // if the hops are too high, ignore it
        if(pr.getHops() >= NUM_HOPS) return;
        synchronized(PONGS) {
            PONGS.insert(pr, pr.getHops());
        }
    }
}


