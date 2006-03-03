
// Commented for the Learning branch

package com.limegroup.gnutella;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.limegroup.gnutella.messages.PingReply;
import com.limegroup.gnutella.settings.ApplicationSettings;
import com.limegroup.gnutella.util.BucketQueue;

/**
 * The PongCacher caches pongs we've seen, and can return a small group of them that traveled a varity of hops to get to us.
 * 
 * This class caches pongs from the network. Caching pongs saves considerable
 * bandwidth because only a controlled number of pings are sent to maintain
 * adequate host data, with Ultrapeers caching and responding to pings with
 * the best pongs available.
 * 
 * === How to use the pong cache ===
 * 
 * To use the pong cache, first call instance() to get the PongCacher object.
 * There are 2 public methods you can call on the PongCacher object:
 * pongCacher.addPong(pong)
 * pongCacher.getBestPongs(language)
 * MessageRouter.handlePingReply(pong, connection) calls pongCatcher.addPong(pong) to add a pong we received to the cache.
 * MessageRouter.respondToPingRequest(ping, connection) calls pongCatcher.getBestPongs(language) to get the pongs we'll send back to the computer that pinged us.
 * 
 * === What the pong cache does ===
 * 
 * When you call getBestPongs("en"), the pong cache will give you up to 6 pongs.
 * They will match the language preference you specify, and may include some extra English ones if you requested a rare foreign language.
 * Each of the pongs you get back will have a different hops count.
 * That means they traveled a different distance to get here.
 * This provides a healthy varity of pongs, with some nearby, and others distant and exotic.
 * 
 * === What the pong cache looks like ===
 * 
 * A pong has information about a remote computer on the Internet running Gnutella software.
 * The pong cache uses 2 pieces of information from the pong to store it:
 * The remote computer's language preference, like "en" for English or "fr" for French.
 * The number of hops the pong traveled to get to us.
 * 
 * The pong cache looks like this:
 * 
 * "en" English
 * 
 *   0 hops: pong
 *   1 hop:  pong
 *   2 hops: pong
 *   3 hops:
 *   4 hops: pong
 *   5 hops:
 * 
 * "fr" French
 * 
 *   0 hops:
 *   1 hop:  pong
 *   2 hops: pong
 *   3 hops: pong (a)
 *   4 hops:
 *   5 hops:
 * 
 * "es" Spanish
 * 
 *   0 hops:
 *   1 hop:  pong
 *   2 hops: pong
 *   3 hops: pong
 *   4 hops:
 *   5 hops: pong
 * 
 * First, it sorts the pongs by language preference.
 * There is a separate section for each language.
 * Within a language section, there are 6 places that can hold only one pong each.
 * A place can be empty, or have one pong in it.
 * The places are numbered 0 through 5.
 * These numbers match the number of hops the pong traveled to get here.
 * 
 * If we get a French pong with a hops count of 3, we'll put it in the slot marked (a) above.
 * If there's already a pong there, the new one will replace it.
 * 
 * === The collections classes the pong cache uses ===
 * 
 * PONGS is a HashMap().
 * The keys are String objects like "en", and the values are LimeWire BucketQueue objects.
 * A BucketQueue has a certain number of buckets used for sorting.
 * Here, there are 6 buckets, numbered 0 through 5, and they sort pongs based on their hops count.
 * When you set up a BucketQueue, you specify how many objects the buckets will hold.
 * When a bucket fills up, it discards the object that's been in it the longest.
 * Here, each bucket can only hold 1 pong.
 * When you add a pong to a bucket that already has one, it discards it.
 * 
 * === Pongs and the ultrapeer system ===
 * 
 * On the Gnutella network, only ultrapeers accept connections from other computers.
 * You can't connect to a leaf, even if it happens to have an Internet connection that isn't firewalled.
 * Pongs advertise computers to connect to, so all pongs should be about ultrapeers.
 * The addPong() method checks to make sure a pong describes an ultrapeer before adding it to this cache.
 * Also, we only make this cache and keep it up to date when we're running in ultrapeer mode.
 */
public final class PongCacher {

    /** The program's single PongCacher object. */
    private static final PongCacher INSTANCE = new PongCacher();

    /** 1 pong, the pong cache will only store a single pong for each hop count for each language. */
    public static final int NUM_PONGS_PER_HOP = 1;

    /** 6, the pong cache will have 6 buckets, holding pongs that have 0 through 5 hops. */
    public static final int NUM_HOPS = 6;

    /** 6000 milliseconds, 6 seconds, English pongs will last 6 seconds in the cache. */
    public static final int EXPIRE_TIME = 6000;

    /** 15 * EXPIRE TIME, 90 seconds, foreign language pongs will last 90 seconds in the cache. */
    public static final int EXPIRE_TIME_LOC = 15 * EXPIRE_TIME;

    /**
     * The pong cache.
     * PONGS is a HashMap with String keys like "en" and a BucketQueue for each language preference.
     */
    private static final Map PONGS = new HashMap();

    /**
     * Get the program's single PongCacher object.
     * 
     * @return The PongCacher object.
     */
    public static PongCacher instance() {

        // Return the object that static code made
        return INSTANCE;
    }

    /** Private constructor to make sure only this class can make the one PongCacher object. */
    private PongCacher() {}

    /**
     * Get 6 pongs from the pong cache that traveled a varity of distances to get to us.
     * 
     * getBestPongs() returns up to 6 pongs, all of which have a different hops count.
     * This gives you a healthy varity of pongs, with some nearby and others exotic.
     * 
     * It takes a language preference, and returns pongs that match that language preference.
     * If you want French pongs and the cache doesn't have 6, getBestPongs() grabs some more from the English list.
     * 
     * @param loc A language preference, like "en" for English, to get pongs that match
     * @return    A List of PingReply objects that are the best pongs we have
     */
    public List getBestPongs(String loc) {

        // Only let one thread access the PONGS map at a time
        synchronized (PONGS) {

            // Make the objects that addBestPongs() will need
            List pongs = new LinkedList();             // addBestPongs() will put the best pongs it finds in this pongs List
            long curTime = System.currentTimeMillis(); // addBestPongs() needs to know what time it is

            /*
             * first we try to populate "pongs" with those pongs
             * that match the locale
             */

            /*
             * Look at the up to 6 pongs the pong cache has that have the requested language preference and each have a unique hops count.
             * If we've had a pong less than 6 seconds, add it to the pongs list, if we've had it more than 6 seconds, return it.
             */

            // Sort the pongs in the cache into pongs or removeList, based on how long we've had them
            List removeList = addBestPongs( // Returns a list of stale pongs we should remove from the cache
                loc,                        // Only examines pongs with this language preference
                pongs,                      // Adds fresh pongs to the pongs list
                curTime,                    // The time right now
                0);                         // Don't filter on hops, look at pongs with 0 hops and more
            removePongs(loc, removeList);   // Remove the stale pongs we found

            /*
             * if the locale that we were searching for was not the default
             * "en" locale and we do not have enough pongs in the list
             * then populate the list "pongs" with the default locale pongs
             */

            // If we weren't looking at the English pongs and we got less than 6, get more from the English list
            if (!ApplicationSettings.DEFAULT_LOCALE.getValue().equals(loc) && pongs.size() < NUM_HOPS) {

                // Sort the pongs in the list into pongs or remove List, based on how long we've had them
                removeList = addBestPongs( // Returns a list of stale pongs we should remove from the cache
                    ApplicationSettings.DEFAULT_LOCALE.getValue(), // Look at the "en" English pongs
                    pongs,                 // Adds fresh pongs to the pongs list
                    curTime,               // The time right now
                    pongs.size());         // If there are 2 pongs already there, only look at English pongs with 5, 4, 3, and 2 hops for a possible total of 6
                removePongs(ApplicationSettings.DEFAULT_LOCALE.getValue(), removeList); // Remove the stale pongs we found
            }

            // Return the list of good pongs we've found
            return pongs;
        }
    }

    /**
     * Examine the pongs of a particular language in the cache, sorting them into a recent list to add and an expired list to remove.
     * 
     * Two of the parameters limit which pongs in the cache this method will examine.
     * loc is a language preference, like "en" for English.
     * The method will only examine the pongs with that language preference.
     * i is a hops count, like 3.
     * The method will only examine the pongs with that number of hops, and more.
     * 
     * Examines the pongs in the cache which make it through that filter.
     * Looks at how long we've had a pong, and adds it to one of 2 lists.
     * If we've had it too long, adds it to the bad list.
     * If it's still fresh, adds it to the good list.
     * 
     * The good list is passed in as the pongs argument.
     * The bad list is returned.
     * 
     * @param loc     Only look at pongs with this language preference
     * @param pongs   A List this method will add the good pongs to
     * @param curTime The time right now
     * @param i       Only look at pongs with this number of hops and more
     * @return        A List of bad pongs that you should remove
     */
    private List addBestPongs(String loc, List pongs, long curTime, int i) {

        /*
         * set the expire time to be used.
         * if the locale that is passed in is "en" then just use the
         * normal expire time otherwise use the longer expire time
         * so we can have some memory of non english locales
         */

        // Decide which expiration time we'll use
        int exp_time =
            (ApplicationSettings.DEFAULT_LOCALE.getValue().equals(loc)) ? // If the caller is having us act on the "en" English BucketQueue
            EXPIRE_TIME :                                                 // Use 6 seconds, there are plenty of English computers out there
            EXPIRE_TIME_LOC;                                              // Otherwise, use 90 seconds to keep rare foreign language pongs around longer

        /*
         * check if there are any pongs of the specific locale stored
         * in PONGS.
         */

        // We'll list the pongs we want to remove here
        List remove = null;

        // Only do something if we have a BucketQueue for the requested language
        if (PONGS.containsKey(loc)) {

            /*
             * get all the pongs that are of the specific locale and
             * make sure that they are not stale
             */

            // Loop through the pongs of the specified language
            BucketQueue bq = (BucketQueue)PONGS.get(loc);
            Iterator iter = bq.iterator();                 // Returns the 5 hop pong first, then 4, 3, 2, 1, and 0 last.
            for ( ; iter.hasNext() && i < NUM_HOPS; i++) { // If i is 3, the loop will run 3 times, looking at the pongs with hops 5, 4, and 3
                PingReply pr = (PingReply)iter.next();

                /*
                 * if the pongs are stale put into the remove list
                 * to be returned.  Didn't pass in the remove list
                 * into this function because we may never see stale
                 * pongs so we won't need to new a linkedlist.
                 * this may be a premature and unnecessary opt.
                 */

                // We've had this pong for longer than the expiration time we chose above
                if (curTime - pr.getCreationTime() > exp_time) { // If we're in the English BucketQueue, it's been more than 6 seconds since we got this pong

                    // Add it to the bad list
                    if (remove == null) remove = new LinkedList();
                    remove.add(pr);

                // This pong is still fresh
                } else {

                    // Add it to the good list
                    pongs.add(pr);
                }
            }
        }

        // Return the list of stale pongs to remove
        return remove;
    }

    /**
     * Remove a given list of pongs from the pong cache.
     * Takes a language preference, and only acts on that part of the pong cache.
     * 
     * @param loc A locale like "en", selecting just the BucketQueue in the pong cache for that language
     * @param l   A List of PingReply objects, the pongs to remove from the BucketQueue for that language
     */
    private void removePongs(String loc, List l) {

        // Make sure the caller gave us a List
        if (l != null) {

            // Get the BucketQueue for the specified language
            BucketQueue bq = (BucketQueue)PONGS.get(loc);

            // Loop for each pong in the given list
            Iterator iter = l.iterator();
            while (iter.hasNext()) {
                PingReply pr = (PingReply)iter.next();

                // Remove the pong from the BucketQueue, even getting multiple copies
                bq.removeAll(pr);
            }
        }
    }

    /**
     * Add a given pong to the pong cache.
     * MessageRouter.handlePingReply() calls this when we get a pong.
     * 
     * @param pr The pong
     */
    public void addPong(PingReply pr) {

        // We only cache pongs when we're an ultrapeer, and we only cache pongs that are about ultrapeers
        if (!RouterService.isSupernode()) return; // If we're a leaf, don't add it
        if (!pr.isUltrapeer())            return; // If the given pong is about a leaf, don't add anything

        // We only have buckets for pongs with hops 0 through 5
        if (pr.getHops() >= NUM_HOPS) return; // If the given pong has a hops of 6 or more, we don't have a bucket for it

        // Only let one thread access the PONGS map at once
        synchronized (PONGS) {

            /*
             * check the map for the locale and create or retrieve the set
             */

            // The PONGS map has a BucketQueue for this pong's language already
            if (PONGS.containsKey(pr.getClientLocale())) {

                // Put the pong in it
                BucketQueue bq = (BucketQueue)PONGS.get(pr.getClientLocale()); // Get the BucketQueue for pongs with this pong's language preference
                bq.insert(pr, pr.getHops());                                   // If the pong has 2 hops, put it in the bucket numbered 2

            // The PONGS map doesn't have a BucketQueue for this pong's language yet
            } else {

                // Make a BucketQueue for the new language, and put the pong in it
                BucketQueue bq = new BucketQueue(NUM_HOPS, NUM_PONGS_PER_HOP); // Make a new BucketQueue with 6 buckets that hold 1 pong each
                bq.insert(pr, pr.getHops());                                   // If the pong has 2 hops, put it in the bucked numbered 2
                PONGS.put(pr.getClientLocale(), bq);                           // Add the BucketQueue in PONGS under the new language like "fr" for French
            }
        }
    }
}
