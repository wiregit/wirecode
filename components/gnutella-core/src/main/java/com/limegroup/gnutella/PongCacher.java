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
     * Constant for the number of pongs to store per hop.  Public to make
     * testing easier.
     */
    public static final int NUM_PONGS_PER_HOP = 1;

    /**
     * Constant for the number of hops to keep track of in our pong cache.
     */
    public static final int NUM_HOPS = 6;

    /**
     * <tt>BucketQueue</tt> holding pongs separated by hops.
     */
    private static final BucketQueue PONGS =
        new BucketQueue(NUM_HOPS, NUM_PONGS_PER_HOP);

    /**
     * Variable for the last time a pong was added, in milliseconds.
     * This is used to determine whether or not we should cache pongs 
     * from old connections.
     */
    private static long _lastPongAddTime = 0L; 

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
            for(;iter.hasNext() && i<NUM_HOPS; i++) {
                pongs.add((PingReply)iter.next());
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
            _lastPongAddTime = System.currentTimeMillis();
        }
    }

    /**
     * Returns whether or not we need pongs for the cacher.
     *
     * @return <tt>true</tt> if we need new pongs to cache, otherwise
     *  <tt>false</tt>
     */
    public boolean needsPongs() {
        // if we're not an Ultrapeer, we don't care about caching the pong
        if(!RouterService.isSupernode()) return false;

        
        if(System.currentTimeMillis() - _lastPongAddTime > 1000) {
            return true;
        }

        synchronized(PONGS) {
            return PONGS.size() < NUM_PONGS_PER_HOP*NUM_HOPS;
        }
    }
}



