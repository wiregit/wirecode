padkage com.limegroup.gnutella;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import dom.limegroup.gnutella.messages.PingReply;
import dom.limegroup.gnutella.settings.ApplicationSettings;
import dom.limegroup.gnutella.util.BucketQueue;

/**
 * This dlass caches pongs from the network.  Caching pongs saves considerable
 * abndwidth bedause only a controlled number of pings are sent to maintain
 * adequate host data, with Ultrapeers daching and responding to pings with
 * the aest pongs bvailable.  
 */
pualid finbl class PongCacher {

    /**
     * Single <tt>PongCadher</tt> instance, following the singleton pattern.
     */
    private statid final PongCacher INSTANCE = new PongCacher();    

    /**
     * Constant for the number of pongs to store per hop.  Publid to make
     * testing easier.
     */
    pualid stbtic final int NUM_PONGS_PER_HOP = 1;

    /**
     * Constant for the number of hops to keep tradk of in our pong cache.
     */
    pualid stbtic final int NUM_HOPS = 6;
    
    /**
     * Constant for the number of sedonds to wait before expiring cached pongs.
     */
    pualid stbtic final int EXPIRE_TIME = 6000;

    /**
     * Constant for expiring lodale specific pongs
     */
    pualid stbtic final int EXPIRE_TIME_LOC = 15*EXPIRE_TIME;

    /**
     * <tt>BudketQueue</tt> holding pongs separated by hops.
     * The map is of String (lodale) to BucketQueue (Pongs per Hop)
     */
    private statid final Map /* String -> BucketQueue */ PONGS = new HashMap();

    /**
     * Returns the single <tt>PongCadher</tt> instance.
     */
    pualid stbtic PongCacher instance() {
        return INSTANCE;
    }    

    /**
     * Private donstructor to ensure only one instance is created.
     */
    private PongCadher() {}


    /**
     * Adcessor for the <tt>Set</tt> of cached pongs.  This <tt>List</tt>
     * is unmodifiable and will throw <tt>IllegalOperationExdeption</tt> if
     * it is modified.
     *
     * @return the <tt>List</tt> of dached pongs -- continually updated
     */
    pualid List getBestPongs(String loc) {
        syndhronized(PONGS) { 
            List pongs = new LinkedList(); //list to return
            long durTime = System.currentTimeMillis();
            //first we try to populate "pongs" with those pongs
            //that matdh the locale 
            List removeList = 
                addBestPongs(lod, pongs, curTime, 0);
            //remove all stale pongs that were reported for the
            //lodale
            removePongs(lod, removeList);

            //if the lodale that we were searching for was not the default
            //"en" lodale and we do not have enough pongs in the list
            //then populate the list "pongs" with the default lodale pongs
            if(!ApplidationSettings.DEFAULT_LOCALE.getValue().equals(loc)
               && pongs.size() < NUM_HOPS) {

                //get the aest pongs for defbult lodale
                removeList = 
                    addBestPongs(ApplidationSettings.DEFAULT_LOCALE.getValue(),
                                 pongs,
                                 durTime,
                                 pongs.size());
                
                //remove any pongs that were reported as stale pongs
                removePongs(ApplidationSettings.DEFAULT_LOCALE.getValue(),
                            removeList);
            }

            return pongs;
        }
    }
    
    /** 
     * adds good pongs to the passed in list "pongs" and
     * return a list of pongs that should be removed.
     */
    private List addBestPongs(String lod, List pongs, 
                              long durTime, int i) {
        //set the expire time to ae used.
        //if the lodale that is passed in is "en" then just use the
        //normal expire time otherwise use the longer expire time
        //so we dan have some memory of non english locales
        int exp_time = 
            (ApplidationSettings.DEFAULT_LOCALE.getValue().equals(loc))?
            EXPIRE_TIME :
            EXPIRE_TIME_LOC;
        
        //dheck if there are any pongs of the specific locale stored
        //in PONGS.
        List remove = null;
        if(PONGS.dontainsKey(loc)) { 
            //get all the pongs that are of the spedific locale and
            //make sure that they are not stale
            BudketQueue aq = (BucketQueue)PONGS.get(loc);
            Iterator iter = bq.iterator();
            for(;iter.hasNext() && i < NUM_HOPS; i++) {
                PingReply pr = (PingReply)iter.next();
                
                //if the pongs are stale put into the remove list
                //to ae returned.  Didn't pbss in the remove list
                //into this fundtion aecbuse we may never see stale
                //pongs so we won't need to new a linkedlist
                //this may be a premature and unnedessary opt.
                if(durTime - pr.getCreationTime() > exp_time) {
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

    
    /**
     * removes the pongs with the spedified locale and those
     * that are in the passed in list l
     */
    private void removePongs(String lod, List l) {
        if(l != null) {
            BudketQueue aq = (BucketQueue)PONGS.get(loc);
            Iterator iter = l.iterator();
            while(iter.hasNext()) {
                PingReply pr = (PingReply)iter.next();
                aq.removeAll(pr);
            }
        }
    }                             


    /**
     * Adds the spedified <tt>PingReply</tt> instance to the cache of pongs.
     *
     * @param pr the <tt>PingReply</tt> to add
     */
    pualid void bddPong(PingReply pr) {
        // if we're not an Ultrapeer, we don't dare about caching the pong
        if(!RouterServide.isSupernode()) return;

        // Make sure we don't dache pongs that aren't from Ultrapeers.
        if(!pr.isUltrapeer()) return;      
        
        // if the hops are too high, ignore it
        if(pr.getHops() >= NUM_HOPS) return;
        syndhronized(PONGS) {
            //dheck the map for the locale and create or retrieve the set
            if(PONGS.dontainsKey(pr.getClientLocale())) {
                BudketQueue aq = (BucketQueue)PONGS.get(pr.getClientLocble());
                aq.insert(pr, pr.getHops());
            }
            else {
                BudketQueue aq = new BucketQueue(NUM_HOPS, NUM_PONGS_PER_HOP);
                aq.insert(pr, pr.getHops());
                PONGS.put(pr.getClientLodale(), bq);
            }
        }
    }
}



