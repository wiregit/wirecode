package com.limegroup.gnutella;

import com.limegroup.gnutella.util.*;
import com.sun.java.util.collections.*;
import java.io.*;
import java.net.InetAddress;
import java.util.Date;

/**
 * The host catcher.   Caches address of other clients from ping replies.
 * These addresses are used for fetching outgoing connections, for
 * responding to pings (pong-caching), and for implementing 
 * "reject connections".<p>
 * 
 * This version of HostCatcher segregates pongs from "new" clients (those
 * supporting query routing and pong caching) from "old" clients (those who
 * don't).  Note that the getAnEndpoint method takes a boolean parameter.  This
 * makes it easier for clients to implement separate new and old connection
 * fetchers.<p>
 *
 * Pongs from new clients ("main cache") are further divided into "fresh" (those
 * arriving in the last CACHE_EXPIRE_TIME seconds) and "expired".  The former is
 * used to answer ping requets, rather than broadcasting the request every time.
 * Both fresh and expired new pongs are used for connection fetching, but they
 * are treated differently; getAnEndpoint(true) marks fresh pongs as "fetched"--
 * without removing them from the cache--while it actually removes expired 
 * pongs.<p>
 *
 * The old pongs ("reserve cache") only empties when the user disconnects from
 * the network.  getAnEndpoint(false) removes old pongs from the HostCatcher. 
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
     * Basically: main cache is a bucket of pongs (indexed or sorted by hops).  
     * Each bucket of pongs is indexed by the hops count (i.e., 0 to main cache
     * length where the index i is the bucket of pongs that are i+1 hops away).

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
    private ArrayList[] /* of MainCacheEntry */ mainCache;
    private Buffer /* of Endpoint */ mainCacheExpired;
    private Object cacheLock = new Object();

    //used for returning random PingReplies from the main cache.
    private Random random;
    

    /** The number of old client pongs to store in the reserve cache. */
    private static final int NORMAL_SIZE=100;
    /** The number of new client pongs to store in the reserve cache. */
    private static final int ROUTER_SIZE=40;
    /** The number of private IP pongs to store in the reserve cache. */
    private static final int BAD_SIZE=10;
    private static final int SIZE=BAD_SIZE+ROUTER_SIZE+NORMAL_SIZE;

    /** Priorities for endpoints in reserve cache. */
    private static final int NORMAL_PRIORITY=2;   //old client pongs
    private static final int ROUTER_PRIORITY=1; //router pongs
    private static final int BAD_PRIORITY=0;    //private IP pongs

    /* reserve cache consists of a queue bounded in size.  The elements at the
     * END of the queue have the highest priority. 
     *
     * LOCKING: obtain cacheLock before modifying reserve cache.  */
    private BucketQueue /* of Endpoint */ reserveCacheQueue=
        new BucketQueue(new int[] {BAD_SIZE, ROUTER_SIZE, NORMAL_SIZE});

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
    /** The number of MILLISECONDS to wait when trying to return random pongs.
     *  This ensures that we are not waiting forever to return pongs. */
    private static final long MAX_WAIT_TIME_GETTING_PONGS=100;


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
        mainCacheExpired=new Buffer(NORMAL_SIZE);
        

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

                if (! Acceptor.isMe(host, port)) {
                    //add e to the head.  Order matters!
                    reserveCacheQueue.insert(e);
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
        if (cacheSize() > 0) {
            for (Iterator iter=getCachedHosts(null, mainCache.length, false); 
                 iter.hasNext(); ) {
                PingReply pr = (PingReply)iter.next();
                Endpoint e = new Endpoint(pr.getIP(), pr.getPort());
                if (connections.contains(e))
                    continue;
                connections.add(e);
                writeInternal(out, e);
            }
        }
        
        //3.) Write hosts in reserve cache that are not in connections--in 
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
     * adds a Pong to the main cache, based on its hops.  If the PingReply is 
     * from an older client, then don't add it to the cache.  Note: hop() has 
     * already been called on this ping reply before we add it to the cache.  
     * Returns true if the pong was successfully added to the cache, false 
     * otherwise.
     *
     */
    private boolean addToMainCache(PingReply pr, 
                                   ManagedConnection connection) {
        if (GUID.getProtocolVersion(pr.getGUID()) < GUID.GNUTELLA_VERSION_06)
            return false;

        int hops = (int)pr.getHops();
        if (hops >= mainCache.length)
            return false; //if greater than Max Hops allowed, do nothing.
        if (hops==0)
            hops=1;

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
            if (!(mainCache[hops-1].contains(newEntry)))
                mainCache[hops-1].add(newEntry);
            cacheLock.notify();
        }
        return true;
    }

    /**
     * Clears out all the pongs currently in the main cache and sets the next
     * time the cache expires.  
     */
    public synchronized void clearCache() {
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
     * null if there are no entries in the main cache for that particular hops.
     */
    private MainCacheEntry getCacheEntry(int hops) {
        //if no entries for the passed in hops, return null
        if (mainCache[hops-1].size() == 0)
            return null;

        MainCacheEntry entry = null;
        synchronized (cacheLock) {
            ArrayList arrayOfPongs = mainCache[hops-1];
            int index = random.nextInt(arrayOfPongs.size());
            entry = (MainCacheEntry)arrayOfPongs.get(index);
        }
        return entry;
    }

    /**
     * Returns the number of pongs we've received so far in the main cache.
     */
    public int cacheSize() {
        int numPongs = 0;

        for (int i = 0; i < mainCache.length; i++)
            numPongs += mainCache[i].size();

        return numPongs;
    }

    /**
     * Returns whether or not main cache is expired, based on the expire time.
     */
    public boolean cacheExpired() {
        long currentTime = System.currentTimeMillis();
        if (currentTime > mainCacheExpireTime)
            return true;
        else
            return false;
    }

    /**
     * Returns an unmodifiable iterator to access all the pongs in the main
     * cache.  However, only returns pongs which are at most maxHops away.  If
     * filterFetched is true, pongs already returned by getAnEndpoint() are not
     * returned. 
     */
    private Iterator getCachedHosts(Connection conn, int maxHops,
                                    boolean filterFetched) {
        ArrayList[] mainCacheClone = new ArrayList[maxHops];
        synchronized(cacheLock) {
            for (int i = 0; i < mainCacheClone.length; i++) {
                mainCacheClone[i] = new ArrayList();
                Iterator iter = mainCache[i].iterator();
                while (iter.hasNext()) {
                    MainCacheEntry entry = (MainCacheEntry)iter.next();
                    if (filterFetched && entry.wasPreviouslyReturned())
                        continue;
                    //only copy ping replies not from the Connection passed in
                    if (entry.getManagedConnection() != conn) 
                        mainCacheClone[i].add(entry.getPingReply());
                }
            }
        }
        return (new MainCacheIterator(mainCacheClone));
    }
    //--------- end Main cache methods


    //--------- Reserve Cache methods

    /**
     * Adds a ping reply to the reserve cache.
     */
    private void addToReserveCache(Endpoint e,
                                   ManagedConnection receivingConnection) {
        //Skip if this would connect us to our listening port.
        if (Acceptor.isMe(e.getHostname(), e.getPort()))
            return;

        try {
            //Skip if this is a router(e.g., router.limewire.com).
            if (isARouter(e.getHostBytes())) 
                return;
        } catch (java.net.UnknownHostException exc) {
            //Will never happen.
        }

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
            e.setWeight(ROUTER_PRIORITY);
        else
            e.setWeight(NORMAL_PRIORITY);

        synchronized(cacheLock) {
            reserveCacheQueue.insert(e);
            cacheLock.notify();
        }

        //notify router thread that a pong was received from the router (if 
        //router connection)
        if (e.getWeight() == ROUTER_PRIORITY) {
            synchronized(gotRouterPongLock) {
                gotRouterPong = true;
                gotRouterPongLock.notify();
            }
        }
    }

    /**
     * Copies the contents of mainCache into mainCacheExpired.
     * Note that mainCache is not modified.
     */ 
    private void copyCacheContents() {
        for (Iterator iter = getCachedHosts(null, mainCache.length, true); 
             iter.hasNext(); ) {
            //all addresses from the main cache are considered the "best"
            //endpoints in the reserve cache.
            PingReply pr = (PingReply)iter.next();
            Endpoint e = new Endpoint(pr.getIP(), pr.getPort(), 
                pr.getFiles(), pr.getKbytes());

            //don't add the host if the address and port would connect us to 
            //ourselves
            if (Acceptor.isMe(e.getHostname(), e.getPort()))
                continue;

            mainCacheExpired.add(e);
        }
    }

    /**
     * @modifies reserve cache
     * @effects removes all entries from reserve cache
     */
    private void resetReserveCache() {
        synchronized (cacheLock) {
            reserveCacheQueue.clear();
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
     * Returns and removes the highest priority endpoint in reserve cache.
     * The caller MUST hold cacheLock before calling this.
     * @exception NoSuchElementException if reserve cache is empty.  
     */
    private Endpoint getAnEndpointOld() throws NoSuchElementException {   
        return (Endpoint)reserveCacheQueue.extractMax();
    }

    /**
     * Returns the highest priority endpoint in main cache, ensuring it won't be
     * returned again.  The caller MUST hold cacheLock before calling this.
     * @exception NoSuchElementException if reserve cache is empty.  
     */
    private Endpoint getAnEndpointNew() throws NoSuchElementException {
        //1. First check mainCache for an unexpired endpoint
        for (int hops=MessageRouter.MAX_TTL_FOR_CACHE_REFRESH-1;
             hops>=0; hops--) {
            for (int i=0; i<mainCache[hops].size(); i++) {
                MainCacheEntry entry=(MainCacheEntry)mainCache[hops].get(i);
                if (entry != null) {
                    //make sure we didn't return this host previously during
                    //this cache cyle.  TODO: synchronize!
                    if (entry.wasPreviouslyReturned())
                        continue;
                    PingReply pr = entry.getPingReply();
                    entry.markPreviouslyReturned();
                    return new Endpoint(pr.getIP(), pr.getPort());
                }
            }
        }
        
        //2. If that failed, check mainCacheExpired for a new but stale
        //endpoint.  Because these are only used for connection fetchers, it's
        //ok to remove the address.
        return (Endpoint)mainCacheExpired.removeFirst();        
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

    /**
     * Creates a clone of the main cache with random entries (i.e., pongs)  
     * gotten from the main cache which are at most maxHops away and were
     * not sent from the Connection passed in.  Up to n random pongs are
     * returned in the cached clone.
     */
    private ArrayList[] createMainCacheClone(Connection conn, int n, 
                                             int maxHops) {
        //first, instantiate a new array of ArrayLists (clone)
        ArrayList[] cacheClone = new ArrayList[mainCache.length];
        for (int i = 0; i < cacheClone.length; i++) 
            cacheClone[i] = new ArrayList();
            
        //only add to clone if there are some pongs in the main cache
        if (cacheSize() > 0) {
            int count = 0;
            int hops = random.nextInt(maxHops);
            //max time to wait for adding pongs.  We use this to ensure
            //that we are not waiting forever to get some pongs
            long waitTime = System.currentTimeMillis() + 
                MAX_WAIT_TIME_GETTING_PONGS;
            
            //return n random entries from the main cache or as many entries
            //until we timeout for returning pongs.
            while ( (count < n) && 
                (System.currentTimeMillis() <= waitTime)) {
                MainCacheEntry entry = getCacheEntry(hops+1);
                if (entry == null) {
                    hops++;
                    if (hops >= maxHops)
                        hops = 0;
                    continue;
                }
                if (entry.getManagedConnection() != conn) {
                    cacheClone[hops].add(entry.getPingReply());
                    count++;
                }
            }
        }
        return cacheClone;
    }

    //--------- public access methods

    /**
     * If receivingConnection is a new connection, tries to add pr to the main
     * cache.  Otherwise, adds pr to the reserve cache and return false.  TODO:
     * what the heck is the return value supposed to mean?
     */
    public boolean addToCache(PingReply pr, 
                              ManagedConnection receivingConnection) {
        //if received from an old client or from a router, place the PingReply
        //in the reserve cache (i.e., hostcatcher).
        if (receivingConnection.isOldClient()) {
            Endpoint e=new Endpoint(pr.getIP(), pr.getPort(),
                                    pr.getFiles(), pr.getKbytes());            
            addToReserveCache(e, receivingConnection);
            return false; //always return false when adding to reserve cache.
        }
        else
            return addToMainCache(pr, receivingConnection);

    }

    /** 
     * Adds the pongs in receivingConnection's old-pongs header, if any, to the
     * reserve cache of this.
     */
    public void addOldPongs(ManagedConnection receivingConnection) {
        String pongs=receivingConnection.getProperty("Old-Pongs");
        if (pongs==null)
            return;
        String[] pongsA=StringUtils.split(pongs, ",");
        for (int i=0; i<pongsA.length; i++) {
            try {
                //The Endpoint(String) constructor may be removed at some point,
                //but we'll use it for now.
                Endpoint e=new Endpoint(pongsA[i]);
                addToReserveCache(e, receivingConnection);
            } catch (IllegalArgumentException e) {
                continue;
            }
        }
    }


    /**
     *  Blocks until an address is available, and then returns that address.
     *  getAnEndpoint will (almost) never return the same address twice. 
     *
     *  @param newOnly true if this should return a (possibly stale) address of 
     *   a "new" host, or false if this should return an address of an "old" host. 
     *  @exception InterruptedException if the calling thread is interrupted
     *   while waiting
     */
    public Endpoint getAnEndpoint(boolean newOnly) throws InterruptedException {
        Endpoint endpoint = null;
        while (true) {
            synchronized (cacheLock) {
                //Try grabbing a new or old endpoint.
                try {
                    return newOnly ? getAnEndpointNew() : getAnEndpointOld();
                } catch (NoSuchElementException e) { }
                
                
                //Wait for addresses (new or old) to come in.  TODO3: could use
                //finer-grained locking.
                cacheLock.wait(); 
            }
        }
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

    /**
     * Returns an iterator of up to N Ping Replies which were not received from
     * conn.  These ping replies are only retrieved from the main cache.
     */
    public Iterator getNPingReplies(Connection conn, int n) {
        //if not enough pongs in the cache, return all of them, except for the
        //pongs received from the connection passed in.
        if (n >= cacheSize())  
            return getCachedHosts(conn, mainCache.length, false);
        else {
            ArrayList[] cacheClone = 
                createMainCacheClone(conn, n, mainCache.length);
            return new MainCacheIterator(cacheClone);
        }
    }


    /**
     * Returns an iterator of up to N Ping Replies which were not received from
     * conn.  However, the ping replies returned will not be more than maxHops
     * away from us.  These ping replies are only retrieved from the main cache.
     */
    public Iterator getNPingReplies(Connection conn, int n, int maxHops) {
        //if not enough pongs in the cache, return all of them, except for the
        //pongs received from the connection passed in.
        if (n >= cacheSize())
            return getCachedHosts(conn, maxHops, false);
        else {
            ArrayList[] cacheClone = createMainCacheClone(conn, n, maxHops);
            return new MainCacheIterator(cacheClone);
        }
    }

//      /** Unit tests */
//      public static void main(String args[]) {
//          //Populate hc with "new" and "old" pongs
//          HostCatcher hc=new HostCatcher(new Main());
//          ManagedConnection bogus=new ManagedConnection("fake.limewire.com", 6346, null);
//          for (int i=0; i<4; i++) {
//              hc.addToMainCache(new PingReply(GUID.makeGuid(),
//                                       (byte)2,
//                                       6346,
//                                       new byte[] {(byte)1, (byte)1, (byte)1, (byte)i},
//                                       0l, 0l),
//                         bogus);
//          }
//          for (int i=0; i<4; i++) {
//              hc.addToReserveCache(new Endpoint("2.2.2."+i, 6346), bogus);
//          }
//          //Fetch "new" pongs, making sure each is unique
//          List /* of Endpoint */ fetched=new ArrayList();
//          for (int i=0; i<2; i++) {
//              try {
//                  Endpoint e=hc.getAnEndpoint(true);
//                  Assert.that(e.getHostname().startsWith("1.1.1."));
//                  Assert.that(! fetched.contains(e));
//                  fetched.add(e);                
//              } catch (NoSuchElementException e) { 
//                  break;
//              } catch (InterruptedException e) {
//                  Assert.that(false);
//              }
//          }
//          //Expire new cache, then fetch stale "new" pongs, making sure each is unique
//          hc.clearCache();
//          Assert.that(hc.cacheSize()==0);
//          Assert.that(hc.mainCacheExpired.size()==2, "Size: "+hc.mainCacheExpired.size());
//          for (int i=0; i<2; i++) {
//              try {
//                  Endpoint e=hc.getAnEndpoint(true);
//                  Assert.that(e.getHostname().startsWith("1.1.1."));
//                  Assert.that(! fetched.contains(e));
//                  fetched.add(e);      
//              } catch (NoSuchElementException e) { 
//                  break;
//              } catch (InterruptedException e) {
//                  Assert.that(false);
//              }
//          }
//          Assert.that(hc.mainCacheExpired.size()==0);
//          //Fetch "old" pongs
//          for (int i=0; i<4; i++) {
//              try {
//                  Endpoint e=hc.getAnEndpoint(false);
//                  Assert.that(e.getHostname().startsWith("2.2.2."));
//                  Assert.that(! fetched.contains(e));
//                  fetched.add(e);      
//              } catch (NoSuchElementException e) { 
//                  break;
//              } catch (InterruptedException e) {
//                  Assert.that(false);
//              }
//          }
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
    
    public MainCacheIterator(ArrayList[] cache)
    {
        this.cache = cache;
        hopsIndex = 0;
        i = 0;
    }
    
    public boolean hasNext()
    {
        //make sure entries in the cache
        if (size() == 0)
            return false;
        
        //if we've gone through the entire cache, then we're done.
        if (hopsIndex >= cache.length)
            return false;
        
        //check to make sure not all entries have been used up (i.e., 
        //returned)
        while (cache[hopsIndex].size() == 0)
        {
            hopsIndex++;
            if (hopsIndex >= cache.length)
                return false;
        }
        
        return true;
    }

    public Object next() throws NoSuchElementException
    {
        //first check, if we can retrieve the next element.
        if (!hasNext()) 
            throw new NoSuchElementException();
        
        PingReply pr = (PingReply)cache[hopsIndex].get(i);
        i++;
        
        if (i >= cache[hopsIndex].size())
        {
            hopsIndex++;
            i = 0;
        }
        
        return pr;
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
    //this is used when returning pingreplies from the cache to the Connection
    //fetchers.  If the entry was usedBefore, it means that another Connection
    //fetcher tried to connect to it.  We use this flag to ensure that we don't
    //return the same host to more than one ConnectionFetcher (during one 
    //expiration time or cache cycle, that is).
    private boolean previouslyReturned;

    public MainCacheEntry(PingReply pingReply, ManagedConnection connection)
    {
        this.pingReply = pingReply;
        this.connection = connection;
        previouslyReturned = false;
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
     * Once previouslyReturned is set to true once, it says true for the 
     * lifetime of this entry in the main cache.
     */
    public void markPreviouslyReturned()
    {
        previouslyReturned = true;
    }

    /**
     * Returns whether this entry in the main cache has already been returned to
     * a Connection Fetcher previously, ensuring that we don't return the same
     * entry twice.
     */
    public boolean wasPreviouslyReturned()
    {
        return previouslyReturned;
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








