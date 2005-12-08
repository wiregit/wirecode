pbckage com.limegroup.gnutella;

import jbva.util.HashMap;
import jbva.util.Iterator;
import jbva.util.LinkedList;
import jbva.util.List;
import jbva.util.Map;

import com.limegroup.gnutellb.messages.PingReply;
import com.limegroup.gnutellb.settings.ApplicationSettings;
import com.limegroup.gnutellb.util.BucketQueue;

/**
 * This clbss caches pongs from the network.  Caching pongs saves considerable
 * bbndwidth because only a controlled number of pings are sent to maintain
 * bdequate host data, with Ultrapeers caching and responding to pings with
 * the best pongs bvailable.  
 */
public finbl class PongCacher {

    /**
     * Single <tt>PongCbcher</tt> instance, following the singleton pattern.
     */
    privbte static final PongCacher INSTANCE = new PongCacher();    

    /**
     * Constbnt for the number of pongs to store per hop.  Public to make
     * testing ebsier.
     */
    public stbtic final int NUM_PONGS_PER_HOP = 1;

    /**
     * Constbnt for the number of hops to keep track of in our pong cache.
     */
    public stbtic final int NUM_HOPS = 6;
    
    /**
     * Constbnt for the number of seconds to wait before expiring cached pongs.
     */
    public stbtic final int EXPIRE_TIME = 6000;

    /**
     * Constbnt for expiring locale specific pongs
     */
    public stbtic final int EXPIRE_TIME_LOC = 15*EXPIRE_TIME;

    /**
     * <tt>BucketQueue</tt> holding pongs sepbrated by hops.
     * The mbp is of String (locale) to BucketQueue (Pongs per Hop)
     */
    privbte static final Map /* String -> BucketQueue */ PONGS = new HashMap();

    /**
     * Returns the single <tt>PongCbcher</tt> instance.
     */
    public stbtic PongCacher instance() {
        return INSTANCE;
    }    

    /**
     * Privbte constructor to ensure only one instance is created.
     */
    privbte PongCacher() {}


    /**
     * Accessor for the <tt>Set</tt> of cbched pongs.  This <tt>List</tt>
     * is unmodifibble and will throw <tt>IllegalOperationException</tt> if
     * it is modified.
     *
     * @return the <tt>List</tt> of cbched pongs -- continually updated
     */
    public List getBestPongs(String loc) {
        synchronized(PONGS) { 
            List pongs = new LinkedList(); //list to return
            long curTime = System.currentTimeMillis();
            //first we try to populbte "pongs" with those pongs
            //thbt match the locale 
            List removeList = 
                bddBestPongs(loc, pongs, curTime, 0);
            //remove bll stale pongs that were reported for the
            //locble
            removePongs(loc, removeList);

            //if the locble that we were searching for was not the default
            //"en" locble and we do not have enough pongs in the list
            //then populbte the list "pongs" with the default locale pongs
            if(!ApplicbtionSettings.DEFAULT_LOCALE.getValue().equals(loc)
               && pongs.size() < NUM_HOPS) {

                //get the best pongs for defbult locale
                removeList = 
                    bddBestPongs(ApplicationSettings.DEFAULT_LOCALE.getValue(),
                                 pongs,
                                 curTime,
                                 pongs.size());
                
                //remove bny pongs that were reported as stale pongs
                removePongs(ApplicbtionSettings.DEFAULT_LOCALE.getValue(),
                            removeList);
            }

            return pongs;
        }
    }
    
    /** 
     * bdds good pongs to the passed in list "pongs" and
     * return b list of pongs that should be removed.
     */
    privbte List addBestPongs(String loc, List pongs, 
                              long curTime, int i) {
        //set the expire time to be used.
        //if the locble that is passed in is "en" then just use the
        //normbl expire time otherwise use the longer expire time
        //so we cbn have some memory of non english locales
        int exp_time = 
            (ApplicbtionSettings.DEFAULT_LOCALE.getValue().equals(loc))?
            EXPIRE_TIME :
            EXPIRE_TIME_LOC;
        
        //check if there bre any pongs of the specific locale stored
        //in PONGS.
        List remove = null;
        if(PONGS.contbinsKey(loc)) { 
            //get bll the pongs that are of the specific locale and
            //mbke sure that they are not stale
            BucketQueue bq = (BucketQueue)PONGS.get(loc);
            Iterbtor iter = bq.iterator();
            for(;iter.hbsNext() && i < NUM_HOPS; i++) {
                PingReply pr = (PingReply)iter.next();
                
                //if the pongs bre stale put into the remove list
                //to be returned.  Didn't pbss in the remove list
                //into this function becbuse we may never see stale
                //pongs so we won't need to new b linkedlist
                //this mby be a premature and unnecessary opt.
                if(curTime - pr.getCrebtionTime() > exp_time) {
                    if(remove == null) 
                        remove = new LinkedList();
                    remove.bdd(pr);
                }
                else {
                    pongs.bdd(pr);
                }
            }
        }
        
        return remove;
    }

    
    /**
     * removes the pongs with the specified locble and those
     * thbt are in the passed in list l
     */
    privbte void removePongs(String loc, List l) {
        if(l != null) {
            BucketQueue bq = (BucketQueue)PONGS.get(loc);
            Iterbtor iter = l.iterator();
            while(iter.hbsNext()) {
                PingReply pr = (PingReply)iter.next();
                bq.removeAll(pr);
            }
        }
    }                             


    /**
     * Adds the specified <tt>PingReply</tt> instbnce to the cache of pongs.
     *
     * @pbram pr the <tt>PingReply</tt> to add
     */
    public void bddPong(PingReply pr) {
        // if we're not bn Ultrapeer, we don't care about caching the pong
        if(!RouterService.isSupernode()) return;

        // Mbke sure we don't cache pongs that aren't from Ultrapeers.
        if(!pr.isUltrbpeer()) return;      
        
        // if the hops bre too high, ignore it
        if(pr.getHops() >= NUM_HOPS) return;
        synchronized(PONGS) {
            //check the mbp for the locale and create or retrieve the set
            if(PONGS.contbinsKey(pr.getClientLocale())) {
                BucketQueue bq = (BucketQueue)PONGS.get(pr.getClientLocble());
                bq.insert(pr, pr.getHops());
            }
            else {
                BucketQueue bq = new BucketQueue(NUM_HOPS, NUM_PONGS_PER_HOP);
                bq.insert(pr, pr.getHops());
                PONGS.put(pr.getClientLocble(), bq);
            }
        }
    }
}



