package com.limegroup.gnutella;

import com.limegroup.gnutella.util.UnmodifiableIterator;
import com.sun.java.util.collections.*;

/**
 * Singleton cache used to store all the Ping Replies (i.e., Pongs) sent to us.
 * It stores all the pongs as an array of ArrayLists.  We use an ArrayList 
 * (instead of a DoublyLinkedList) because we don't need to remove entries one 
 * at a time (we just clear the cache at once).  Also, ArrayList allows us to 
 * return a random element (using the indexOf method) whereas DoublyLinkedList
 * has only basic methods to access the list.  The Ping Replies are stored as
 * <PingReply,ManagedConnection> in the cache.  Each PingReply is stored in 
 * cache based on the hops count (since that indicates how many hops away
 * from us this client is).
 *
 * Note: Only newer clients's pongs are stored in this cache.  That is, only
 *       clients using protocol version (0.6) or higher.
 *
 * @author Tarun Kapoor
 */
public class PingReplyCache
{    
    /**
     * Increment for number of pongs stored in the cache (per hops).  So if
     * the number of pongs for hops 1 = 10, then number of pongs for hops 2 = 
     * hops 1 number of pongs + increment, hops 3 = hops 2 number of pongs +
     * increment, etc.  This is done because we will store more pongs from
     * hosts that are further away from us (i.e., greater hops), since it is
     * more likely that I will have more pongs from all my neighbors's 
     * neighbors, than from my direct neighbors.
     */
    private static final int CACHE_HOPS_SIZE_INCREMENT = 10;
    /** Number of pongs to store in cache for Hops 1. */
    private static final int CACHE_HOPS_1_SIZE = 10;

    /** Cache expire time is 3 seconds */
    private static final long CACHE_EXPIRE_TIME = 3000;

    /** next time cache expires again. */
    private long expireTime;

    private ArrayList[] pingReplies;

    private Random random; //used for returning random PingReplies from cache.

    //singleton
    private static PingReplyCache instance = null;

    private PingReplyCache(int maxHops) 
    {
        //array of 0 .. maxHops ArrayLists
        pingReplies = new ArrayList[maxHops];

        int numOfPongsAllowed = CACHE_HOPS_1_SIZE;
        for (int i = 0; i < pingReplies.length; i++)
        {
            pingReplies[i] = new ArrayList(numOfPongsAllowed);
            numOfPongsAllowed += CACHE_HOPS_SIZE_INCREMENT;
        }

        random = new Random();
        expireTime = System.currentTimeMillis() + CACHE_EXPIRE_TIME;
    }

    /**
     * Copy constructor (used for returning Iterators over the entire cache)
     */
    private PingReplyCache(PingReplyCache otherCache)
    {
        pingReplies = new ArrayList[otherCache.pingReplies.length];
        
        for (int i = 0; i < pingReplies.length; i++)
        {
            pingReplies[i] = new ArrayList(otherCache.pingReplies[i]);
        }
    }

    /**
     * Returns singleton to access class methods.
     */
    public static PingReplyCache instance()
    {
        if (instance == null)
            instance = 
                new PingReplyCache(MessageRouter.MAX_TTL_FOR_CACHE_REFRESH);

        return instance;
    }

    /** 
     * adds a Pong to the cache, based on its hops.  If the PingReply is from an
     * older client, then don't add it to the cache.
     *
     * requires: PingReply is from a newer client (Gnutella protocol version
     *           0.6 or higher).
     */
    public synchronized void addPingReply(PingReply pr, 
        ManagedConnection connection) 
    {
        if (GUID.getProtocolVersion(pr.getGUID()) < GUID.GNUTELLA_VERSION_06)
            return;

        int hops = (int)pr.getHops();
        if (hops > pingReplies.length)
            return; //if greater than Max Hops allowed, do nothing.

        pingReplies[hops-1].add(new PingReplyCacheEntry(pr,connection));
    }

    /**
     * Clears out all the pongs currently in the cache and sets the next
     * time the cache expires.
     */
    public synchronized void clear() 
    {
        for (int i = 0; i < pingReplies.length; i++) 
                pingReplies[i].clear();

        expireTime = System.currentTimeMillis() + CACHE_EXPIRE_TIME;
    }

    /**
     * Return a cache entry for a specified hops.  Basically, return a random
     * cache entry from the ArrayList of that specified hops.  Return null 
     * if the hops is greater than the Max Hops allowed for caching.  If no
     * entries in the cache returns null.
     */
    public PingReplyCacheEntry getEntry(int hops) 
    {
        if (size() <= 0) 
            return null;

        if (hops > pingReplies.length)
            return null;
        
        //if no entries for the passed in hops, return null
        if (pingReplies[hops-1].size() == 0)
            return null;

        ArrayList arrayOfPongs = pingReplies[hops-1];
        int index = random.nextInt(arrayOfPongs.size());
        return (PingReplyCacheEntry)arrayOfPongs.get(index);
    }

    /**
     * Returns the number of pongs we've received so far in the 
     * cache.
     */
    public int size() 
    {
        int numPongs = 0;

        for (int i = 0; i < pingReplies.length; i++)
            numPongs += pingReplies[i].size();

        return numPongs;
    }

    /**
     * Returns whether or not cache is expired, based on the expire time.
     */
    public boolean expired() 
    {
        long currentTime = System.currentTimeMillis();
        if (currentTime > expireTime)
            return true;
        else
            return false;
    }

    /**
     * Returns string representation of cache (Endpoints only)
     */
    public String toString()
    {
        StringBuffer st = new StringBuffer();

        for (int i = 0; i < pingReplies.length; i++)
            st.append(pingReplies[i].toString());
        
        return st.toString();
    }

    /**
     * private method to create an iterator to access all the cache
     * entries.
     */
    private Iterator getIterator()
    {
        return new PingReplyCacheIterator();
    }

    /**
     * private method to create an iterator to access all the cache entries
     * for a particular hops.
     *
     * @requires - only one hop in count (i.e., only one ArrayList)
     */
    private Iterator getIterator(int hops)
    {
        return new PingReplyCacheIterator(hops);
    }

    /**
     * Returns an unmodifiable iterator to access all the cache entries 
     */
    public synchronized Iterator iterator()
    {
        return (new PingReplyCache(this).getIterator());
    }

    /**
     * Returns an unmodifiable iterator to access all the cache entries
     * for a particular hops.
     */
    public synchronized Iterator iterator(int hops)
    {
        return (new PingReplyCache(this).getIterator(hops));
    }

    /**
     * Iterator class to access all the Endpoints in the cache.  Returns the 
     * Endpoints starting with the lowest hops (direct neighbors) to the highest
     * hops (furthest away).
     */
    private class PingReplyCacheIterator extends UnmodifiableIterator
    {
        int hopsIndex; //index to current hops list.
        int i; //index to current object in current hops list.
        int origHopsIndex; //index if iterator for only one hop list.

        public PingReplyCacheIterator()
        {
            hopsIndex = 0;
            i = 0;
            origHopsIndex = -1;
        }

        public PingReplyCacheIterator(int hops)
        {
            hopsIndex = hops-1;
            i = 0;
            origHopsIndex = hopsIndex;
        }

        public boolean hasNext()
        {
            //if we've gone through the entire cache, then we're done.
            if (hopsIndex >= MessageRouter.MAX_TTL_FOR_CACHE_REFRESH)
                return false;
            
            if (origHopsIndex > 0) //returning entries for only one hops.
            {
                if (hopsIndex > origHopsIndex) 
                    return false;
            }

            return true;
        }

        public Object next() throws NoSuchElementException
        {
            //first check, if we can retrieve the next element.
            if (!hasNext()) 
                throw new NoSuchElementException();

            PingReplyCacheEntry cacheEntry = 
                (PingReplyCacheEntry)pingReplies[hopsIndex].get(i);
            i++;

            if (i >= pingReplies[hopsIndex].size())
            {
                hopsIndex++;
                i = 0;
            }
            
            return cacheEntry;
        }
    }
}

/**
 * Mapping class which maps a ping reply to a connection.  This class is
 * what is stored in the PingReplyCache, as we need to keep each PingReply that
 * was received from which connection it was received.  
 */
class PingReplyCacheEntry
{
    private PingReply pingReply;
    private ManagedConnection connection;

    public PingReplyCacheEntry(PingReply pingReply, ManagedConnection connection)
    {
        this.pingReply = pingReply;
        this.connection = connection;
    }

    public PingReply getPingReply()
    {
        return pingReply;
    }

    public ManagedConnection getManagedConnection()
    {
        return connection;
    }

    public String toString()
    {
        return pingReply.toString();
    }
}























