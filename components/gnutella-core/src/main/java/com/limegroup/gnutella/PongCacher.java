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
public final class PongCacher implements Runnable {

    /**
     * Single <tt>PongCacher</tt> instance, following the singleton pattern.
     */
    private static final PongCacher INSTANCE = new PongCacher();

    /**
     * <tt>BucketQueue</tt> holding pongs separated by hops.
     */
    private static BucketQueue _pongs;

    /**
     * Variable for the <tt>Set</tt> of the best cached pongs currently 
     * available.
     */
    private static Set _cachedPongs = 
        Collections.unmodifiableSet(new HashSet());

    /**
     * Constant for the number of pongs to store at each hop.
     */
    private static final int PONGS_PER_HOP = 20;

    /**
     * Flag for whether or not we've received a new pong -- allows slight
     * optimization where we don't rebuild the cached pong set when no new
     * pongs have come in.
     */
    private static volatile boolean _newPong = false;

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
     * Starts the thread that continually updates the most recent cached pongs.
     */
    public void start() {
        Thread pongCacher = new Thread(this, "pong cacher");
        pongCacher.setDaemon(true);
        pongCacher.start();
    }


    public void run() {
        try {
            while(true) {
                if(RouterService.isSupernode() && _newPong) {
                    _cachedPongs = Collections.unmodifiableSet(updatePongs());
                    _newPong = false;
                }
                Thread.sleep(600);
            }
        } catch(Throwable t) {
            ErrorService.error(t);
        }
    }

    /**
     * Accessor for the <tt>Set</tt> of cached pongs.  This <tt>Set</tt>
     * is unmodifiable and will throw <tt>IllegalOperationException</tt> if
     * it is modified.
     *
     * @return the <tt>Set</tt> of cached pongs -- continually updated
     */
    public Set getBestPongs() {
        return _cachedPongs;
    }

    /**
     * Adds the specified <tt>PingReply</tt> instance to the cache of pongs.
     *
     * @param pr the <tt>PingReply</tt> to add
     */
    public void addPong(PingReply pr) {
        
        // if we're not an Ultrapeer, we don't care about caching the pong
        if(!RouterService.isSupernode()) return;

        // lazily construct the queue
        if(_pongs == null) {
            _pongs = new BucketQueue(8, PONGS_PER_HOP);
        }
        synchronized(_pongs) {
            _pongs.insert(pr, pr.getHops());
        }
        _newPong = true;
    }

    /**
     * Updates the <tt>Set</tt> of cached pongs being sent in response to pings.
     *
     * @return the <tt>Set</tt> of cached pongs 
     */
    private Set updatePongs() {
        synchronized(_pongs) {
            Iterator iter = _pongs.iterator();
            int i = 0;
            Set pongs = new HashSet();
            while(iter.hasNext() && i<10) {
                pongs.add((PingReply)iter.next());
            }
            return pongs;
        }
    }
}
