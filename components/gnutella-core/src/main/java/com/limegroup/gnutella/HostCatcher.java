package com.limegroup.gnutella;

import com.limegroup.gnutella.util.BucketQueue;
import com.limegroup.gnutella.util.UnmodifiableIterator;
import com.sun.java.util.collections.*;
import java.io.*;
import java.net.InetAddress;
import java.util.Date;

/**
 * The host catcher. It caches address in ping replies in two distinct caches.
 * 
 * 1) A main pong cache, which continously caches pongs sent from different
 *    connections.  It expires every few seconds and refills itself in between
 *    expiration times.  It is used to return pongs when receiving a ping request
 *    rather than broadcasting the request every time.
 * 
 * 2) A reserve cache for stores address in pongs from router connections (e.g.,
 *    router.limewire.com) or from older clients (i.e., not implementing the
 *    the pong-caching scheme).  It only expires (or clears out) when the 
 *    user disconnects from the network.  It is an essence, a "backup cache" in
 *    case all connections die and there is nothing in the main cache.  Hence, 
 *    the name, reserve cache.
 */
public class HostCatcher {
    /**
     * We store all the pongs in the main cache as an array of ArrayLists.  We 
     * use an ArrayList (instead of a DoublyLinkedList) because we don't need to
     * remove entries one at a time (we just clear the cache at once).  Also, 
     * ArrayList allows us to return a random element (using the indexOf method)
     * whereas DoublyLinkedList has only basic methods to access the list.  The 
     * Ping Replies are stored as <PingReply,ManagedConnection> in the main 
     * cache.  Each PingReply is stored in the main cache based on the hops 
     * count (since that indicates how many hops away from us this client is).  
     *
     * Note: Only newer clients's pongs are stored in the main cache.  That is, 
     * only clients using protocol version (0.6) or higher.
     */

    /** Cache expire time is 3 seconds */
    private static final long CACHE_EXPIRE_TIME = 3000;
    
    /** next time cache expires again. */
    private long mainCacheExpireTime;

    /** 
     * The main ping reply (a.k.a. pong) cache.  It stores the ping replies 
     * that we receive from different connections.
     * LOCKING: obtain cacheLock
     */
    private ArrayList[] mainCache;
    private Object cacheLock = new Object();

    //used for returning random PingReplies from the main cache.
    private Random random;
    
    /** 
     * Number of sufficent pongs needed in the reserve cache before a Ping 
     * doesnt'have to be broadcasted across the network when creating a new 
     * connection.  Also, used when determing whether create a connection
     * to router or not.
     */
    private static final int  RESERVE_CACHE_SUFFICIENT_CAPACITY = 50;

    /** The number of router pongs to store in the reserve cache. */
    private static final int ROUTER_SIZE=30;
    /** The number of old client pongs to store in the reserve cache. */
    private static final int OLD_CLIENT_SIZE=20;
    /** The number of new client pongs to store in the reserve cache. */
    private static final int NEW_CLIENT_SIZE=60;
    /** The number of private IP pongs to store in the reserve cache. */
    private static final int BAD_SIZE=10;
    private static final int SIZE=ROUTER_SIZE+OLD_CLIENT_SIZE+
        NEW_CLIENT_SIZE+BAD_SIZE;

    /** Priorities for endpoints in reserve cache. */
    private static final int BEST_PRIORITY=3; //new client pongs
    private static final int GOOD_PRIORITY=2; //router pongs
    private static final int NORMAL_PRIORITY=1; //old client pongs
    private static final int BAD_PRIORITY=0; //private IP pongs

    /* reserve cache consists of a set and a queue, both bounded in size.
     * The set lets us quickly check if there are duplicates, while the queue
     * provides ordering.  The elements at the END of the queue have the highest
     * priority. Note that if a priority queue is used instead of a bucket
     * queue, old router pongs must be flushed when reconnecting to the server.
     *
     * INVARIANT: queue contains no duplicates and contains exactly the
     *  same elements as set.
     * LOCKING: obtain cacheLock before modifying reserve cache.
     */
    private BucketQueue /* of Endpoint */ reserveCacheQueue=
        new BucketQueue(new int[] {BAD_SIZE, OLD_CLIENT_SIZE, 
            ROUTER_SIZE, NEW_CLIENT_SIZE});
    private Set /* of Endpoint */ reserveCacheSet=new HashSet();

    private Acceptor acceptor;
    private ConnectionManager manager;
    private ActivityCallback callback;
    private SettingsManager settings=SettingsManager.instance();

    /* True if we've received a pong from a router. 
     * LOCKING: obtain gotRouterPongLock
     */
    private boolean gotRouterPong=false;
    private Object gotRouterPongLock=new Object();

    /** The number of MILLISECONDS to wait before retrying quick-connect. */
    private static final int RETRY_TIME=5*60*1000; //5 minutes
    /** The amount of MILLISECONDS to wait after starting a connection before
     *  trying another. */
    private static final int CONNECT_TIME=6000;  //6 seconds


    /**
     * Creates an empty host catcher.  Must call initialize before using.
     */
    public HostCatcher(ActivityCallback callback) {
        this.callback=callback;

        //array of 0 .. MAX_TTL_FOR_CACHE_REFRESH ArrayLists
        mainCache = new ArrayList[MessageRouter.MAX_TTL_FOR_CACHE_REFRESH];

        for (int i = 0; i < mainCache.length; i++)
        {
            mainCache[i] = new ArrayList();
        }

        random = new Random();
        mainCacheExpireTime = System.currentTimeMillis() + CACHE_EXPIRE_TIME;
    }

    /**
     * Links the HostCatcher up with the other back end pieces
     */
    public void initialize(Acceptor acceptor, ConnectionManager manager) {
        this.acceptor = acceptor;
        this.manager = manager;
    }

    /**
     * Links the HostCatcher up with the other back end pieces, and, if quick
     * connect is not specified in the SettingsManager, loads the hosts in the
     * host list into the maybe set.  (The likelys set is empty.)  If filename
     * does not exist, then no error message is printed and this is initially
     * empty.  The file is expected to contain a sequence of lines in the format
     * "<host>:port\n".  Lines not in this format are silently ignored.
     */
    public void initialize(Acceptor acceptor, ConnectionManager manager,
                           String filename) {
        this.acceptor = acceptor;
        this.manager = manager;
        try {
            read(filename);
        } catch (FileNotFoundException e) {
        } catch (IOException e) {
        }
    }

    /**
     * Reads in endpoints from the given file
     * @modifies reserveCache
     * @effects read hosts from the given file.
     */
    private void read(String filename)
            throws FileNotFoundException, IOException {
        synchronized(cacheLock) {
            BufferedReader in=null;
            in=new BufferedReader(new FileReader(filename));
            for (int i=0; i<SIZE; i++) {
                String line=in.readLine();
                if (line==null)   //nothing left to read?  Done.
                    break;

                //Break the line into host and port.  Skip if badly formatted.
                int index=line.indexOf(':');
                if (index==-1) {
                    continue;
                }
                String host=line.substring(0,index);
                int port=0;
                try {
                    port=Integer.parseInt(line.substring(index+1));
                } catch (NumberFormatException e) {
                    continue;
                } catch (ArrayIndexOutOfBoundsException e) {
                    continue;
                }

                //Everything passed!  Add it.
                Endpoint e = new Endpoint(host, port);
                if (e.isPrivateAddress())
                    e.setWeight(BAD_PRIORITY);
                else
                    e.setWeight(NORMAL_PRIORITY);

                if ((! reserveCacheSet.contains(e)) && 
                    (! Acceptor.isMe(host, port))) {
                    //add e to the head.  Order matters!
                    Object removed=reserveCacheQueue.insert(e);
                    //Shouldn't happen...
                    if (removed!=null)
                        reserveCacheSet.remove(removed);
                    reserveCacheSet.add(e);
                }
            }
            cacheLock.notify();
        }
    }

    /**
     * @modifies the file named filename
     * @effects writes this to the given file.  The file
     *  is prioritized by rough probability of being good.
     */
    public synchronized void write(String filename) throws IOException {
        FileWriter out=new FileWriter(filename);
        //1) Write connections we're connected to--in no particular order.
        //   Also add the connections to a set for step (2 and 3).  Ignore 
        //   incoming connections, since the remote host's port is ephemeral.
        Set connections=new HashSet();
        for (Iterator iter=manager.getInitializedConnections().iterator();
             iter.hasNext(); ) {
            Connection c=(Connection)iter.next();
            if (! c.isOutgoing()) //ignore incoming
                continue;
            Endpoint e=new Endpoint(c.getInetAddress().getHostAddress(),
                                    c.getPort());
            connections.add(e);
            writeInternal(out, e);
        }
        
        //2.) Write out the connections in the main cache if any
        if (mainCacheSize() > 0) {
            for (Iterator iter=getCachedHosts(); iter.hasNext(); ) {
                PingReply pr = ((MainCacheEntry)iter.next()).getPingReply();
                Endpoint e = new Endpoint(pr.getIP(), pr.getPort());
                if (connections.contains(e))
                    continue;
                connections.add(e);
                writeInternal(out, e);
            }
        }
        
        //2.) Write hosts in reserve cache that are not in connections--in 
        //order.
        for (int i=reserveCacheQueue.size()-1; i>=0; i--) {
            Endpoint e=(Endpoint)reserveCacheQueue.extractMax();
            if (connections.contains(e))
                continue;
            writeInternal(out, e);
        }
        out.close();
    }

    private void writeInternal(Writer out, Endpoint e) throws IOException {
        out.write(e.getHostname()+":"+e.getPort()+"\n");
    }

    //////////////////////////////////////////////////////////////////////
    //--------- Main Cache methods

    /** 
     * adds a Pong to the cache, based on its hops.  If the PingReply is from an
     * older client, then don't add it to the cache.  Note: hop() has already
     * been called on this ping reply before we add it to the cache.  Returns
     * true if the pong was successfully added to the cache, false otherwise.
     *
     * requires: PingReply is from a newer client (Gnutella protocol version
     *           0.6 or higher).
     */
    public boolean addToMainCache(PingReply pr, 
                                  ManagedConnection connection) {
        if (GUID.getProtocolVersion(pr.getGUID()) < GUID.GNUTELLA_VERSION_06)
            return false;

        int hops = (int)pr.getHops();
        if (hops > (mainCache.length-1))
            return false; //if greater than Max Hops allowed, do nothing.

        //if private IP address, ignore.
        Endpoint e = new Endpoint(pr.getIPBytes(), pr.getPort());
        if (e.isPrivateAddress())
            return false;

        synchronized(cacheLock) {
            //only add to main cache, if the main cache already doesn't contain 
            //that PingReply (determined by IP, port, hops, and different 
            //managed connections).
            MainCacheEntry newEntry = 
                new MainCacheEntry(pr, connection);
            if (!(mainCache[hops].contains(newEntry)))
                mainCache[hops].add(newEntry);
            cacheLock.notify();
        }
        return true;
    }

    /**
     * Clears out all the pongs currently in the main cache and sets the next
     * time the cache expires.  Also, copies the contents of the main cache
     * into the reserve cache.
     */
    public synchronized void clearMainCache() {
        //first copy contents of main cache into reserve cache.
        copyCacheContents();
        
        //next clear the main cache.
        for (int i = 0; i < mainCache.length; i++) 
            mainCache[i].clear();
        
        mainCacheExpireTime = System.currentTimeMillis() + CACHE_EXPIRE_TIME;
    }

    /**
     * removes all entries in the main cache, without copying over contents to
     * the reserve cache.
     */
    private void resetMainCache() {
        synchronized (cacheLock) {
            for (int i = 0; i < mainCache.length; i++)
                mainCache[i].clear();
        }
    }

    /**
     * Return a main cache entry for a specified hops.  Basically, return a 
     * random cache entry from the ArrayList of that specified hops.  Return 
     * null if the hops is greater than the Max Hops allowed for caching.  If no
     * entries in the cache returns null.  Also, return null, if the retrieved
     * entry is from the connection passed in.
     */
    public PingReply getMainCacheEntry(int hops, Connection conn) {
        if (hops > mainCache.length)
            return null;

        if (mainCacheSize() <= 0)
            return null;

        //if no entries for the passed in hops, return null
        if (mainCache[hops-1].size() == 0)
            return null;

        PingReply pr = null;
        synchronized (cacheLock) {
            ArrayList arrayOfPongs = mainCache[hops-1];
            int index = random.nextInt(arrayOfPongs.size());
            MainCacheEntry entry = 
                (MainCacheEntry)arrayOfPongs.get(index);
            if (entry.getManagedConnection() != conn)
                pr = entry.getPingReply();
        }
        return pr;
    }

    /**
     * Returns the number of pongs we've received so far in the main cache.
     */
    public int mainCacheSize() {
        int numPongs = 0;

        for (int i = 0; i < mainCache.length; i++)
            numPongs += mainCache[i].size();

        return numPongs;
    }

    /**
     * Returns whether or not main cache is expired, based on the expire time.
     */
    public boolean mainCacheExpired() {
        long currentTime = System.currentTimeMillis();
        if (currentTime > mainCacheExpireTime)
            return true;
        else
            return false;
    }

    /**
     * Returns an unmodifiable iterator to access all the main cache entries 
     */
    public Iterator getCachedHosts() {
        ArrayList[] mainCacheClone = new ArrayList[mainCache.length];
        synchronized(cacheLock) {
            for (int i = 0; i < mainCache.length; i++)
            {
                mainCacheClone[i] = new ArrayList(mainCache[i]);
            }
        }

        return (new MainCacheIterator(mainCacheClone));
    }

    /**
     * Returns an unmodifiable iterator to access all the main cache entries
     * for a particular hops.
     */
    public Iterator getCachedHosts(int hops) {
        ArrayList[] mainCacheClone = new ArrayList[mainCache.length];
        synchronized(cacheLock) {
            for (int i = 0; i < mainCache.length; i++)
            {
                mainCacheClone[i] = new ArrayList(mainCache[i]);
            }
        }

        return (new MainCacheIterator(mainCacheClone, hops));
    }
    //--------- end Main cache methods


    //--------- Reserve Cache methods

    /**
     * @modifies this
     * @effects may choose to add hosts listed in pr to this.
     *  If non-null, receivingConnection may be used to prioritize pr.
     */
    public void addToReserveCache(PingReply pr, 
                                  ManagedConnection receivingConnection) {
        Endpoint e=new Endpoint(pr.getIP(), pr.getPort(),
                    pr.getFiles(), pr.getKbytes());

        //Skip if this would connect us to our listening port.
        if (Acceptor.isMe(e.getHostname(), e.getPort()))
            return;

        //Skip if this is a router(e.g., router.limewire.com).
        if (isARouter(pr.getIPBytes())) 
            return;

        //Current policy: "new clients" connections are the best.  After that, 
        //"Pong cache" connections are considered good.  Private addresses are 
        //considered real bad (negative weight).  This means tha the host 
        //catcher will still work on private networks, although we will normally
        //ignore private addresses.  Note that if e is already in this,
        //but with a different weight, we don't bother re-heapifying.
        if (e.isPrivateAddress())
            e.setWeight(BAD_PRIORITY);
        else if (receivingConnection != null 
                 && receivingConnection.isRouterConnection())
            e.setWeight(GOOD_PRIORITY);
        else if (receivingConnection != null && 
                     !receivingConnection.isOldClient())
            e.setWeight(BEST_PRIORITY);                
        else
            e.setWeight(NORMAL_PRIORITY);

        synchronized(cacheLock) {
            if (! (reserveCacheSet.contains(e))) {
                //Adding e may eject an older point from queue, so we have to
                //cleanup the set to maintain rep. invariant.
                reserveCacheSet.add(e);
                Object ejected=reserveCacheQueue.insert(e);
                if (ejected!=null)
                    reserveCacheSet.remove(ejected);

                cacheLock.notify();
            }
        }

        //notify router thread that a pong was received from the router (if 
        //router connection)
        if (e.getWeight() == GOOD_PRIORITY) {
            synchronized(gotRouterPongLock) {
                gotRouterPong = true;
                gotRouterPongLock.notify();
            }
        }
    }

    /**
     * Copies the contents of the main cache into the reserve cache (both
     * the set and queue).
     */ 
    private void copyCacheContents() {
        for (Iterator iter = getCachedHosts(); 
             iter.hasNext(); ) {
            //all addresses from the main cache are considered the "best"
            //endpoints in the reserve cache.
            PingReply pr = ((MainCacheEntry)iter.next()).getPingReply();
            Endpoint e = new Endpoint(pr.getIPBytes(), pr.getPort(), 
                                      pr.getFiles(), pr.getKbytes());
            e.setWeight(BEST_PRIORITY);
            //only add the element if it doesn't exist already
            if (!reserveCacheSet.contains(e)) {
                //Adding e may eject an older point from queue, so we have to
                //cleanup the set to maintain rep. invariant.
                reserveCacheSet.add(e);
                Object ejected=reserveCacheQueue.insert(e);
                if (ejected!=null)
                    reserveCacheSet.remove(ejected);
            }
        }
    }

    /** 
     * return whether the reserve cache size is sufficient enough to not
     * cause a refresh of the cache (i.e., broadcast a ping).  
     */
    public boolean reserveCacheSufficient() {
        if (reserveCacheQueue.size() < RESERVE_CACHE_SUFFICIENT_CAPACITY) 
            return false;
        else 
            return true;
    }

    /**
     * Return the number of hosts in the reserve cache.
     */
    public int reserveCacheSize() {
        return (reserveCacheQueue.size());
    }

    /**
     * @modifies reserve cache
     * @effects removes all entries from reserve cache
     */
    private void resetReserveCache() {
        synchronized (cacheLock) {
            reserveCacheQueue.clear();
            reserveCacheSet.clear();
        }
    }


    /**
     * Returns an iterator of the hosts in reserve cache, in order of priority.
     * This can be modified while iterating through the result, but
     * the modifications will not be observed.
     */
    public Iterator getReserveHosts() {
        Iterator iter;
        synchronized (cacheLock) {
            //clone the queue before iterating.
            iter = new BucketQueue(reserveCacheQueue).iterator();
        }
        return iter;
    }

    /**
     * @requires n>0
     * @effects returns an iterator that yields up the best n endpoints of this.
     *  It's not guaranteed that these are reachable. This can be modified while
     *  iterating through the result, but the modifications will not be
     * observed.  
     */
    public Iterator getBestReserveHosts(int n) {
        Iterator iter;
        synchronized (cacheLock) {
            //clone the queue before iterating.
            iter = new BucketQueue(reserveCacheQueue).iterator(n);
        }
        return iter;
    }

    /**
     *  Remove unwanted or used entries from the reserve cache
     */
    public void removeHost(String host, int port) {
        synchronized (cacheLock) {
            Endpoint e=new Endpoint(host, port);
            boolean removed1=reserveCacheSet.remove(e);
            boolean removed2=reserveCacheQueue.removeAll(e);
            //Check that set.contains(e) <==> queue.contains(e)
            Assert.that(removed1==removed2, 
                "Rep. invariant for HostCatcher broken.");
        }
    }
    //---------- end Reserve cache methods

    /**
     * This thread contacts the pong server and waits until it gets a pong
     * from the pong server before dying.
     */
    private class RouterConnectorThread extends Thread {
        RouterConnectorThread() {
            setName("RouterConnectorThread");
        }

        public void run() {
            //Try connecting every RETRY_TIME milliseconds until we get a
            //router pong 
            try {
                connectUntilPong();
            } catch (InterruptedException e) {
                return; //if exception thrown, exit thread
            }
        }

        /** 
         * Blocks until we get a good pong. Throws InterruptedException if
         * interrupted while waiting.
         */ 
         void connectUntilPong() throws InterruptedException {
            gotRouterPong=false;
            while (!gotRouterPong) {
                //1) Try each quick-connect host until we connect
                String[] hosts=settings.getQuickConnectHosts();
                for (int i=0; i<hosts.length; i++) {
                    //a) Extract hostname+port and try to connect synchronously.
                    Endpoint e;
                    try {
                        e=new Endpoint(hosts[i]);
                    } catch (IllegalArgumentException exc) {
                        continue;
                    }
                    try {
                        manager.createRouterConnection(e.getHostname(),
                                                       e.getPort());
                    } catch (IOException exc) {
                        continue;
                    }

                    //b) Wait CONNECT_TIME milliseconds for router pong.
                    //Note the boolean check to avoid missing notify.
                    synchronized (gotRouterPongLock) {
                        if (! gotRouterPong) {
                            gotRouterPongLock.wait(CONNECT_TIME);
                        }                        
                    }
                }

                //2) If we need to retry, sleep a little first.  Otherwise
                //we'll immediately exit the loop.
                if (! gotRouterPong) 
                    Thread.sleep(RETRY_TIME);
            }
        }
    } //end RouterConnectorThread

    /**
     * @modifies this
     * @effects remove a random host from the main cache.  However, if we cannot
     *  retrieve an elment from the main cache (after trying twice), then try
     *  to obtain an endpoint from the reserve cache.  If the reserve cache is
     *  also empty, wait until an endpoint is available unless an external 
     *  interrupt is caused by an InterruptedException.  
     */
    public Endpoint getAnEndpoint() throws InterruptedException {
        Endpoint endpoint = null;
        while (true) {
            //first, try the main cache two times, in case the first time, a null
            //is returned.
            if (mainCacheSize() > 0) { 
                Random random = new Random();
                int hops;
                for (int i = 0; i < 2; i++) {
                    hops = 
                        random.nextInt(MessageRouter.MAX_TTL_FOR_CACHE_REFRESH);
                    PingReply pr = getMainCacheEntry(hops+1, null);
                    if (pr != null) {
                        endpoint = new Endpoint(pr.getIP(), pr.getPort());
                        return endpoint;
                    }
                }
            }
            //next try the reserve cache
            try {
                endpoint = getAnEndpointInternal();
                return endpoint;
            } catch(NoSuchElementException e) {
                synchronized(cacheLock) {
                    //wait for a host in either the main cache or the reserve cache
                    cacheLock.wait(); 
                }
            }
        }
    }

    /**
     * @effects returns the highest priority endpoint in reserve cache,
     *  regardless of quick-connect settings, etc.  Throws NoSuchElementException
     *  if reserve cache is empty.
     */
    private Endpoint getAnEndpointInternal()
        throws NoSuchElementException {   
        if (! reserveCacheQueue.isEmpty()) {
            //            System.out.println("    GAEI: From "+set+",");
            //pop e from queue and remove from set.
            Endpoint e;
            synchronized (cacheLock) {
                e=(Endpoint)reserveCacheQueue.extractMax();
                boolean ok=reserveCacheSet.remove(e);
                //check that e actually was in set.
                Assert.that(ok, "Rep. invariant for HostCatcher broken.");
            }
            return e;
        } else
            throw new NoSuchElementException();
    }

    /**
     * @modifies this 
     * removes all entries from this 
     */
    public synchronized void reset() {
        resetMainCache();
        resetReserveCache();
    }

    /**
     * Creates seperate thread to connect to a pong cache server (i.e., router.
     * limewire.com) and wait for some pongs.
     */
    public void connectToPongServer() {
        new RouterConnectorThread().start();
    }

    /** Returns true iff ip is the ip address of router.limewire.com or 
     *  gnutellahosts.com
     *      @requires ip.length==4 */
    private boolean isARouter(byte[] ip) {
        //Check for 64.61.25.139-143 or 206.132.188.160
        //TODO: There should be a list of all pong cache servers or "routers" in
        //the settings manager.
        if (ip[0]==(byte)64
            && ip[1]==(byte)61
            && ip[2]==(byte)25
            && ip[3]>=(byte)139
            && ip[3]<=(byte)143)
            return true;
        else if (ip[0]==(byte)206
            && ip[1]==(byte)132
            && ip[2]==(byte)188
            && ip[3]==(byte)160)
            return true;
        else
            return false;
    }

//      /** Unit test: just calls tests.HostCatcherTest, since it
//       *  is too large and complicated for this.
//       */
//      public static void main(String args[]) {
//          String newArgs[]={String.valueOf(STALE_TIME),
//                            String.valueOf(RETRY_TIME),
//                            String.valueOf(CONNECT_TIME)};
//          com.limegroup.gnutella.tests.HostCatcherTest.main(newArgs);
//      }
}

/**
 * Iterator class to access all the PingReplies in the main cache.  Returns the 
 * PingReplies starting with the lowest hops (direct neighbors) to the highest
 * hops (furthest away).
 */
class MainCacheIterator extends UnmodifiableIterator
{
    //actual clone of cache that is being iterated over.
    private ArrayList[] cache;

    private int hopsIndex; //index to current hops list.
    private int i; //index to current object in current hops list.
    private int origHopsIndex; //index if iterator for only one hop list.
    
    public MainCacheIterator(ArrayList[] cache)
    {
        this.cache = cache;
        hopsIndex = 0;
        i = 0;
        origHopsIndex = -1;
    }
    
    public MainCacheIterator(ArrayList[] cache, int hops)
    {
        this.cache = cache;
        hopsIndex = hops-1;
        i = 0;
        origHopsIndex = hopsIndex;
    }
    
    public boolean hasNext()
    {
        //make sure entries in the cache
        if (size() == 0)
            return false;
        
        //if we've gone through the entire cache, then we're done.
        if (hopsIndex >= MessageRouter.MAX_TTL_FOR_CACHE_REFRESH)
            return false;
        
        if (origHopsIndex >= 0) //returning entries for only one hops.
        {
            if (hopsIndex > origHopsIndex) 
                return false;
        }
        else
        {
            //check to make sure not all entries have been used up (i.e., 
            //returned)
            while (cache[hopsIndex].size() == 0)
            {
                hopsIndex++;
                if (hopsIndex >= MessageRouter.MAX_TTL_FOR_CACHE_REFRESH)
                    return false;
            }
        }
        
        return true;
    }

    public Object next() throws NoSuchElementException
    {
        //first check, if we can retrieve the next element.
        if (!hasNext()) 
            throw new NoSuchElementException();
        
        MainCacheEntry cacheEntry = 
            (MainCacheEntry)cache[hopsIndex].get(i);
        i++;
        
        if (i >= cache[hopsIndex].size())
        {
            hopsIndex++;
            i = 0;
        }
        
        return cacheEntry;
    }

    //calculates the number of entries in the cloned cache that we are iterating
    //over
    private int size() 
    {
        int numPongs = 0;

        for (int i = 0; i < cache.length; i++)
            numPongs += cache[i].size();
        
        return numPongs;
    }
}


/**
 * Mapping class which maps a ping reply to a connection.  This class is
 * what is stored in the main cache, as we need to keep each PingReply that
 * was received from which connection it was received.  
 */
class MainCacheEntry
{
    private PingReply pingReply;
    private ManagedConnection connection;

    public MainCacheEntry(PingReply pingReply, ManagedConnection connection)
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

    /**
     * Determines if two pong cache entries are equal by looking at the 
     * connection and ping reply.  This is used to ascertain that the same IP and 
     * port is not added continously in the cache (from the same connection, that 
     * is).
     */
    public boolean equals(Object o)
    {
        if (! (o instanceof MainCacheEntry))
            return false;

        MainCacheEntry entry = (MainCacheEntry)o;
        //first, check if connections are the same, if not then, then the entries
        //are not the same.
        //NOTE: Connection class does not contain an "equals" method since 
        //different classes might interpret two connections being "equal" with
        //different comparators.
        ManagedConnection otherConnection = entry.getManagedConnection();
        if (!connectionsEqual(otherConnection))
            return false;

        PingReply otherReply = entry.pingReply;
        return pingReply.equals(otherReply);
    }

    /**
     * Determines whether another managed connections is equivalent to the one
     * in this class by looking at the orig host and orig port.
     */
    private boolean connectionsEqual(ManagedConnection c)
    {
        String otherHost = c.getOrigHost();
        int otherPort = c.getOrigPort();

        return ( (connection.getOrigHost().equals(otherHost)) &&
                 (connection.getOrigPort() == otherPort) );
    }
}








