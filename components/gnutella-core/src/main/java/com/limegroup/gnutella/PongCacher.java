package com.limegroup.gnutella;

import com.limegroup.gnutella.util.*;
import com.limegroup.gnutella.messages.*;
import com.limegroup.gnutella.settings.ApplicationSettings;
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
     * Constant for the number of seconds to wait before expiring cached pongs.
     */
    public static final int EXPIRE_TIME = 6000;

    /**
     * <tt>BucketQueue</tt> holding pongs separated by hops.
     */
    private static final Map PONGS = new HashMap();
    //new BucketQueue(NUM_HOPS, NUM_PONGS_PER_HOP);

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
    public List getBestPongs(String loc) {
        synchronized(PONGS) {
            List pongs = new LinkedList();
            long curTime = System.currentTimeMillis();
            List removeList = 
                addBestPongs(loc, pongs, curTime, 0);
            
            removePongs(loc, removeList);
            
            if(!ApplicationSettings.DEFAULT_LOCALE.getValue().equals(loc)
               && pongs.size() < NUM_HOPS) {

                removeList = 
                    addBestPongs(ApplicationSettings.DEFAULT_LOCALE.getValue(),
                                 pongs,
                                 curTime,
                                 pongs.size());
                
                removePongs(ApplicationSettings.DEFAULT_LOCALE.getValue(),
                            removeList);
            }

            return pongs;
        }
    }
    
    /** .return to remove list
     */
    private List addBestPongs(String loc, List pongs, 
                              long curTime, int i) {
        List remove = null;
        if(PONGS.containsKey(loc)) { 
            BucketQueue bq = (BucketQueue)PONGS.get(loc);
            Iterator iter = bq.iterator();
            for(;iter.hasNext() && i < NUM_HOPS; i++) {
                PingReply pr = (PingReply)iter.next();
                
                if(curTime - pr.getCreationTime() > EXPIRE_TIME) {
                    if(remove == null) 
                        remove = new LinkedList();
                    remove.add(pr);
                }
                else {
                    pongs.add(pr);
                }
            }
        }
        
        return remove;
    }

    
    private void removePongs(String loc, List l) {
        if(l != null) {
            BucketQueue bq = (BucketQueue)PONGS.get(loc);
            Iterator iter = l.iterator();
            while(iter.hasNext()) {
                PingReply pr = (PingReply)iter.next();
                bq.removeAll(pr);
            }
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

        // Make sure we don't cache pongs that aren't from Ultrapeers.
        if(!pr.isUltrapeer()) return;      
        
        // if the hops are too high, ignore it
        if(pr.getHops() >= NUM_HOPS) return;
        synchronized(PONGS) {
            //PONGS.insert(pr, pr.getHops());
            if(PONGS.containsKey(pr.getClientLocale())) {
                BucketQueue bq = (BucketQueue)PONGS.get(pr.getClientLocale());
                bq.insert(pr, pr.getHops());
            }
            else {
                BucketQueue bq = new BucketQueue(NUM_HOPS, NUM_PONGS_PER_HOP);
                bq.insert(pr, pr.getHops());
                PONGS.put(pr.getClientLocale(), bq);
            }
        }
    }
}



