package com.limegroup.gnutella;

import com.limegroup.gnutella.util.BucketQueue;
import com.sun.java.util.collections.*;
import java.io.*;
import java.net.InetAddress;
import java.util.Date;

/**
 * The host catcher.  In essence, this is a reserve cache to store endpoints
 * which might be used for connections later.  It has four distinct rnakings of 
 * endpoints added to it.  "New client" endpoints, (i.e., GNUTELLA Protocol
 * Version 0.6 and higher) are stored with the best ranking, followed by
 * endpoints from router pongs, followed by "older clients", and the worst
 * ranking is for private IP endpoints.  This class is also used to intiate
 * connections to "GNUTELLA routers" such as router.limewire.com, based on
 * certain conditions, such as clearing out the reserve cache (expiring it) 
 * and if the size of the cache is less than a certain amount, etc.
 */
public class HostCatcher {
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
     * LOCKING: obtain this' monitor before modifying either.
     */
    private BucketQueue /* of Endpoint */ reserveCacheQueue=
        new BucketQueue(new int[] {BAD_SIZE, OLD_CLIENT_SIZE, 
            ROUTER_SIZE, NEW_CLIENT_SIZE});
    private Set /* of Endpoint */ reserveCacheSet=new HashSet();
    private static final byte[] LOCALHOST={(byte)127, (byte)0, (byte)0,
                                           (byte)1};

    private Acceptor acceptor;
    private ConnectionManager manager;
    private ActivityCallback callback;
    private SettingsManager settings=SettingsManager.instance();

    private Thread routerConnectorThread;
    /* True if we've received a pong from a router. 
     * LOCKING: obtain gotRouterPongLock
     */
    private boolean gotRouterPong=false;
    private Object gotRouterPongLock=new Object();
    /* True iff we need to connect to the router.
     * LOCKING: obtain needRouterConnectionLock.
     */
    private boolean needRouterConnection=true;
    private Object needRouterConnectionLock=new Object();

    /* True iff ConnectionManager has been initialized.  We need to do this
     * so we don't try to connect to the router before the Connection Manager
     * has been initialized, otherwise a potential race condition could occur
     * (NullPointerException before initialization race condition).
     * LOCKING: obtain mangerInitializedLock
     */
    private boolean managerInitialized = false;
    private Object managerInitializedLock=new Object();

    /** The number of MILLISECONDS to wait before connecting to a router when
     *  the number of hosts in the reserve cache is not sufficent enough. */
    private static final int WAIT_TIME=2*60*1000; //2 minutes
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
        routerConnectorThread=new RouterConnectorThread();
        routerConnectorThread.start();
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
     * @modifies this
     * @effects read hosts from the given file.
     */
    private synchronized void read(String filename)
            throws FileNotFoundException, IOException {
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
                notify();
            }
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

        //2.) Write out the connections in the Ping Reply cache if any
        PingReplyCache pongCache = PingReplyCache.instance();
        if (pongCache.size() > 0) {
            for (Iterator iter=pongCache.iterator(); iter.hasNext(); ) {
                PingReply pr = ((PingReplyCacheEntry)iter.next()).
                    getPingReply();
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

        synchronized(this) {
            if (! (reserveCacheSet.contains(e))) {
                //Adding e may eject an older point from queue, so we have to
                //cleanup the set to maintain rep. invariant.
                reserveCacheSet.add(e);
                Object ejected=reserveCacheQueue.insert(e);
                if (ejected!=null)
                    reserveCacheSet.remove(ejected);

                this.notify();
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

        //if now, we have enough hosts in the reserve cache, then we need to 
        //notify the RouterConnectorThread not to connect to router.
        if (reserveCacheSufficient()) {
            synchronized(needRouterConnectionLock) {
                needRouterConnection = false;
                needRouterConnectionLock.notify();
            }
        }
    }

    /**
     * Copies the contents of the PingReplyCache into the reserve cache (both
     * the set and queue).
     */ 
    public void copyCacheContents() {
        synchronized(this) {
            for (Iterator iter = PingReplyCache.instance().iterator(); 
                 iter.hasNext(); ) {
                //all endpoints from the PingReplyCache are considered the "best"
                //endpoints in the reserve cache.
                PingReply pr = ((PingReplyCacheEntry)iter.next()).getPingReply();
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
            this.notify();
        }
    }

    /**
     * This thread loops forever, contacting the pong server on startup, or
     * everytime the reserve cache needs more pongs.  Once getting a 
     * router's pong it will sleep 2 minutes before waiting again to get 
     * connections from router.
     */
    private class RouterConnectorThread extends Thread {
        RouterConnectorThread() {
            setDaemon(true);
            setName("RouterConnectorThread");
        }

        /** Repeatedly contacts the pong server when there are not enough host
         *  in the reserve cache or if expire is called.
         */
        public void run() {
            //first, wait until manager is initialized before trying to create
            //connections to a GNUTELLA router.
            synchronized(managerInitializedLock) {
                while (!managerInitialized) {
                    try {
                        managerInitializedLock.wait();
                    }
                    catch(InterruptedException ie) {
                        continue; //try again
                    }
                }
            }
                
            while(true) {
                //To avoid continously connecting to the pong server, we check
                //to make sure a connection to router is needed, if not, we 
                //wait until it is needed.
                synchronized (needRouterConnectionLock) {
                    while (!needRouterConnection) {
                        try {
                            needRouterConnectionLock.wait();
                        } catch (InterruptedException e) {
                            continue;
                        }
                    }
                }
                
                //2. Try connecting every RETRY_TIME milliseconds until we get a
                //router pong 
                try {
                    connectUntilPong();
                } catch (InterruptedException e) {
                    continue;
                }
                
                //sleep for a while, and try again in WAIT_TIME minutes
                try {
                    Thread.sleep(WAIT_TIME);
                }
                catch (InterruptedException e) {
                    continue;
                }
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
     * @effects atomically removes and returns the highest priority host in
     *  this.  If no host is available, blocks until one is.  If no elements
     * exist in the reserve cache (or if only private IPs exists in the
     * reserve cache), throws InterrupedException.  Also, if there are no 
     * "router" or "new client" pongs, then indicate to the router thread
     * to create a connecion to router.
     */
    public Endpoint getAnEndpoint() throws NoSuchElementException {
        Endpoint endpoint = null; 
        synchronized (this) { 
			while (true)  {
                endpoint = getAnEndpointInternal();
                //if the endpoint we got is from an "old" client or is a 
                //"private IP" address, then it means we should get fresher
                //data from router.
                if (endpoint.getWeight() < GOOD_PRIORITY) {
                    synchronized(needRouterConnectionLock) {
                        needRouterConnection = true;
                        needRouterConnectionLock.notify();
                    }
                }
                break; 
			} 
        } 
        return endpoint;
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
     * @effects returns the highest priority endpoint in queue, regardless
     *  of quick-connect settings, etc.  Throws NoSuchElementException if
     *  this is empty.
     */
    private Endpoint getAnEndpointInternal()
            throws NoSuchElementException {
        if (! reserveCacheQueue.isEmpty()) {
            //            System.out.println("    GAEI: From "+set+",");
            //pop e from queue and remove from set.
            Endpoint e=(Endpoint)reserveCacheQueue.extractMax();
            boolean ok=reserveCacheSet.remove(e);
            //check that e actually was in set.
            Assert.that(ok, "Rep. invariant for HostCatcher broken.");
            return e;
        } else
            throw new NoSuchElementException();
    }

    /**
     * Return the number of hosts in the reserve cache.
     */
    public int getNumReserveHosts() {
        return (reserveCacheQueue.size());
    }

    /**
     * Returns an iterator of the hosts in reserve cache, in order of priority.
     * This can be modified while iterating through the result, but
     * the modifications will not be observed.
     */
    public synchronized Iterator getReserveHosts() {
        //Clone the queue before iterating.
        return (new BucketQueue(reserveCacheQueue)).iterator();
    }

    /**
     * @requires n>0
     * @effects returns an iterator that yields up the best n endpoints of this.
     *  It's not guaranteed that these are reachable. This can be modified while
     *  iterating through the result, but the modifications will not be
     *  observed.  
     */
    public synchronized Iterator getBestHosts(int n) {
        //Clone the queue before iterating.
        return (new BucketQueue(reserveCacheQueue)).iterator(n);
    }

    /**
     *  Remove unwanted or used entries
     */
    public synchronized void removeHost(String host, int port) {
        Endpoint e=new Endpoint(host, port);
        boolean removed1=reserveCacheSet.remove(e);
        boolean removed2=reserveCacheQueue.removeAll(e);
        //Check that set.contains(e) <==> queue.contains(e)
        Assert.that(removed1==removed2, "Rep. invariant for HostCatcher broken.");
    }

    /**
     * @modifies this
     * @effects removes all entries from this, but does not create a connection
     * to a router.
     */
    public synchronized void silentClear() {
        reserveCacheQueue.clear();
        reserveCacheSet.clear();
    }

    /**
     * @modifies this 
     * removes all entries from this and ensures that a connection to a router
     * is made.
     */
    public synchronized void expire() {
        reserveCacheQueue.clear();
        reserveCacheSet.clear();
        synchronized (needRouterConnectionLock) {
            needRouterConnection = true;
            needRouterConnectionLock.notify();
        }
        //interrupt thread (in case it's sleeping).
        routerConnectorThread.interrupt();
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

    public String toString() {
        return reserveCacheQueue.toString();
    }

    /** 
     * Sets the manager intialized flag to true and wakes up the router thread
     * who is waiting for the connection manager to be initialized.
     */
    public void setConnectionManagerInitialized() {
        synchronized(managerInitializedLock) {
            managerInitialized = true;
            managerInitializedLock.notify();
        }
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







