package com.limegroup.gnutella;

import com.limegroup.gnutella.util.BucketQueue;
import com.sun.java.util.collections.*;
import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Date;

/**
 * The host catcher.  This peeks at pong messages coming on the
 * network and snatches IP addresses of other Gnutella peers.  IP
 * addresses may also be added to it from a file (usually
 * "gnutella.net").  The servent may then connect to these addresses
 * as necessary to maintain full connectivity.<p>
 *
 * The HostCatcher currently prioritizes pongs as follows.  Note that supernode
 * with a private address is still highest priority; hopefully this may allow
 * you to find local supernodes.
 * <ol>
 * <li> Supernodes.  Supernodes are identified because the number of files they
 *      are sharing is an exact power of two--a dirty but effective hack.
 * <li> Normal pongs.
 * <li> Private addresses.  This means that the host catcher will still 
 *      work on private networks, although we will normally ignore private
 *      addresses.        
 * </ol>
 * 
 * The HostCatcher may initiate outgoing "router" connections.  This behavior is
 * confusing and will likely be refactored in the future.  
 */
public class HostCatcher {    
    /** The number of supernode pongs to store. */
    private static final int GOOD_SIZE=400;
    /** The number of normal pongs to store. */
    private static final int NORMAL_SIZE=100;
    /** The number of private IP pongs to store. */
    private static final int BAD_SIZE=15;
    private static final int SIZE=GOOD_SIZE+NORMAL_SIZE+BAD_SIZE;

    public static final int GOOD_PRIORITY=2;
    public static final int NORMAL_PRIORITY=1;
    public static final int BAD_PRIORITY=0;

    /* Our representation consists of a set and a queue, both bounded in size.
     * The set lets us quickly check if there are duplicates, while the queue
     * provides ordering.  The elements at the END of the queue have the highest
     * priority. Note that if a priority queue is used instead of a bucket
     * queue, old router pongs must be flushed when reconnecting to the server.
     *
     * INVARIANT: queue contains no duplicates and contains exactly the
     *  same elements as set.
     * LOCKING: obtain this' monitor before modifying either.
     */
    private BucketQueue /* of Endpoint */ queue=
        new BucketQueue(new int[] {BAD_SIZE, NORMAL_SIZE, GOOD_SIZE});
    private Set /* of Endpoint */ set=new HashSet();

    private Acceptor acceptor;
    private ConnectionManager manager;
    private ActivityCallback callback;
    private SettingsManager settings=SettingsManager.instance();

    private Thread routerConnectorThread;
    /* True if the cache has expired and the host catcher has not yet tried
     * (successfully or not) to connect to the pong cache.  It is also used to
     * force the first connection fetchers to wait for the initial connection to
     * the router.  LOCKING: obtain staleLock.
     */
    private boolean stale=true;
    private Object staleLock=new Object();
    /* The number of threads waiting for stale to become false.  This is used as
     * an optimization to avoid reconnecting to the server unless someone needs
     * it. LOCKING: obtain staleWaitersLock.  
     */
    private int staleWaiters=0;
    private Object staleWaitersLock=new Object();
    /* True iff we have gotten a good pong since setting stale=false.
     * LOCKING: obtain gotGoodPongLock.
     */
    private boolean gotGoodPong=false;
    private Object gotGoodPongLock=new Object();

	// TODO1 - This methodology just does not scale.
	// It is set to 48 hours to suppress the hits on central server.
    /** The number of MILLISECONDS a server's pong is valid for. */
    private static final int STALE_TIME=48*60*60*1000; 
    /** The number of MILLISECONDS to wait before retrying quick-connect. */
    private static final int RETRY_TIME=5*60*1000; //5 minutes
    /** The amount of MILLISECONDS to wait after starting a connection before
     *  trying another. */
    private static final int CONNECT_TIME=6000;  //6 seconds

    //whether or not to always notify the activity callback implementor that
    //a host was added to the host catcher.  This is used when the hostcatcher
    //is used with the SimplePongCacheServer to always notify when a host was
    //added.
    private boolean alwaysNotifyKnownHost=false;

	/**
	 * Boolean value for whether or not the thread to connect to the 
	 * router has been started or not.
	 */
	private boolean routerConnectorThreadStarted = false;

    /**
     * Creates an empty host catcher.  Must call initialize before using.
     */
    public HostCatcher(ActivityCallback callback) {
        this.callback=callback;
        routerConnectorThread=new RouterConnectorThread();
    }

    /**
     * Links the HostCatcher up with the other back end pieces
     */
    public void initialize(Acceptor acceptor, ConnectionManager manager) {
        this.acceptor = acceptor;
        this.manager = manager;
    }

	/**
	 * Connects to the router in a background thread.
	 */
	public void connectToRouter() {
		routerConnectorThread.setDaemon(true);
        routerConnectorThread.start();
		routerConnectorThreadStarted = true;
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

            if ((! set.contains(e)) && (! isMe(host, port))) {
                //add e to the head.  Order matters!
                Object removed=queue.insert(e, e.getWeight());
                //Shouldn't happen...
                if (removed!=null)
                    set.remove(removed);
                set.add(e);
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
        //   Also add the connections to a set for step (2).  Ignore incoming
        //   connections, since the remote host's port is ephemeral.
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

        //2.) Write hosts in this that are not in connections--in order.
        for (int i=queue.size()-1; i>=0; i--) {
            Endpoint e=(Endpoint)queue.extractMax();
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
     * Attempts to add a pong to this, possibly ejecting other elements from the
     * cache.  This method used to be called "spy".
     *
     * @param pr the pong containing the address/port to add
     * @param receivingConnection the connection on which we received
     *  the pong.
     * @return true iff pr was actually added 
     */
    public boolean add(PingReply pr, ManagedConnection receivingConnection) {
        //Convert to endpoint
        Endpoint e=new Endpoint(pr.getIP(), pr.getPort(),
                    pr.getFiles(), pr.getKbytes());

        //Add the endpoint, forcing it to be high priority if marked pong from a
        //supernode..
        return add(e, pr.isMarked());
    }

    /**
     * Adds an address to this, possibly ejecting other elements from the cache.
     * This method is used when getting an address from headers, instad of the
     * normal ping reply.
     *
     * @param pr the pong containing the address/port to add
     * @param forceHighPriority true if this should always be of high priority
     * @return true iff e was actually added
     */
    public boolean add(Endpoint e, boolean forceHighPriority) {
        //See preamble for a discussion of priorities
        if (forceHighPriority)
            e.setWeight(GOOD_PRIORITY);
        else if (e.isPrivateAddress())
            e.setWeight(BAD_PRIORITY);
        else
            e.setWeight(NORMAL_PRIORITY);
        return add(e);
    }

    /**
     * Adds the passed endpoint to the set of hosts maintained. The endpoint 
     * may not get added due to various reasons (including it might be our
     * address itself, we migt be connected to it etc.). Also adding this
     * endpoint may lead to the removal of some other endpoint from the
     * cache.
     *
     * @param e Endpoint to be added
     * @return true iff e was actually added
     */
    private boolean add(Endpoint e) {
        //Skip if we're connected to it.
        if (manager.isConnected(e))
            return false;

        //Skip if this would connect us to our listening port.
        if (isMe(e.getHostname(), e.getPort()))
            return false;

        //Skip if this is the router.
        try {
            if (isRouter(e.getHostBytes())) 
                return false;
        }
        catch(UnknownHostException uhe){
            //return in this case, without adding the host
            return false;
        }

        boolean ret=false;
        boolean notifyGUI=false;
        synchronized(this) {
            if (! (set.contains(e))) {
                ret=true;
                //Adding e may eject an older point from queue, so we have to
                //cleanup the set to maintain rep. invariant.
                set.add(e);
                Object ejected=queue.insert(e, e.getWeight());
                if (ejected!=null)
                    set.remove(ejected);

                 //If this is not full, notify the callback.  If this is full,
                 //the GUI's display of the host catcher will differ from this.
                 //This is acceptable; the user really doesn't need to see so
                 //many hosts, and implementing the alternatives would require
                 //many changes to ActivityCallback and probably a more efficient
                 //representation on the GUI side.
                 if (ejected==null)
                     notifyGUI=true;

                this.notify();
            }
        }

        //If we're trying to connect to pong, notify the router connection
        //thread.  This will in turn notify any connection fetchers waiting for
        //the cache to refresh.  We used to do this only for router pongs.  Now
        //GOOD_PRIORITY is redefined to mean supernode.  Is this new code ok?
        synchronized (gotGoodPongLock) {
            gotGoodPong=true;
            gotGoodPongLock.notify();
        }

        //we notify the callback in two different situations.  One situation, we
        //always notify the GUI (e.g., a SimplePongCacheServer which needs to know
        //when a new host was added).  The second situation is if the host catcher
        //is not full, so that the endpoint is added to the GUI for the user to
        //view and use.  The second situation occurs the majority of times and
        //only in special cases such as a SimplePongCacheServer would the first 
        //situation occur.
        if (alwaysNotifyKnownHost) {
            callback.knownHost(e);
        }
        else {
            if (notifyGUI)
                callback.knownHost(e);
        }
        return ret;
    }

    /**
     *  @requires this' monitor held by caller
     *  @effects returns true iff this has a host with weight GOOD_PRIORITY
     */
    private boolean hasRouterHost() {
        //Because priorities are only -1, 0, or 1, it suffices to look at the
        //head of the queue.  If this were not the case, we would likely have
        //to augment the state of this.
        if (! queue.isEmpty()) {
            Endpoint e=(Endpoint)queue.getMax();
            return e.getWeight()==GOOD_PRIORITY;
        } else {
            return false;
        }
    }

    /**
     * This thread loops forever, contacting the pong server about every 30
     * minutes.  An earlier implementation created a new thread every time a
     * reconnect was needed, but that proved somewhat complicated.
     */
    private class RouterConnectorThread extends Thread {
        RouterConnectorThread() {
            setDaemon(true);
            setName("RouterConnectorThread");
        }

        /** Repeatedly contacts the pong server at most every STALE_TIME
         *  milliseconds. */
        public void run() {
            while(true) {  
                stale=true;
                //1. Wait until someone is waiting on staleLock.  (Really!) The
                //following code is here solely as an optimization to avoid
                //connecting to the pong server every STALE_TIME minutes if not
                //necessary.
                synchronized (staleWaitersLock) {
                    while (staleWaiters==0) { 
                        try {
                            staleWaitersLock.wait();
                        } catch (InterruptedException e) {
                            continue;
                        }
                    }
                }

                //2. Try connecting every RETRY_TIME milliseconds until we get a
                //good pong...
                try {
                    connectUntilPong();
                } catch (InterruptedException e) {
                    continue;
                }

                //3. Sleep until the cache expires and try again.
                try {
                    Thread.sleep(STALE_TIME);
                } catch (InterruptedException e) {
                    continue;
                }
            }
        }

        /** Blocks until we get a good pong. Throws InterruptedException
         *  if interrupted while waiting. */
        private void connectUntilPong() throws InterruptedException {
            gotGoodPong=false;
            while (! gotGoodPong) {
                //1) Try each quick-connect host until we connect
                String[] hosts=settings.getQuickConnectHosts();
                for (int i=0; i<hosts.length && !gotGoodPong; i++) {
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

                    //b) Wait CONNECT_TIME milliseconds for good pong.
                    //Note the boolean check to avoid missing notify.
                    synchronized (gotGoodPongLock) {
                        if (! gotGoodPong) {
                            gotGoodPongLock.wait(CONNECT_TIME);
                        }                        
                    }
                }

                //2) Regardless of whether we connected or not, anyone waiting
                //for the cache to be refreshed can continue.  The check for
                //stale is just a mini optimization.
                if (stale) {
                    synchronized (staleLock) {
                        stale=false;
                        staleLock.notifyAll();
                    }
                }
                    
                //3) If we need to retry, sleep a little first.  Otherwise
                //we'll immediately exit the loop.
                if (! gotGoodPong) 
                    Thread.sleep(RETRY_TIME);
            }
        }
    } //end RouterConnectorThread

    /**
     * @modifies this
     * @effects atomically removes and returns the highest priority host in
     *  this.  If no host is available, blocks until one is.  If the calling
     *  thread is interrupted during this process, throws InterruptedException.
     */
    public Endpoint getAnEndpoint() throws InterruptedException {
        //If cache has expired, wait for reconnect to finish (normally or
        //timeout).  See RouterConnectionThread.run().
        if(settings.getUseQuickConnect() && stale) {
            try {
                synchronized (staleWaitersLock) {
                    staleWaiters++;
                    staleWaitersLock.notify();
                }
                synchronized(staleLock) {
                    if (stale) {
                        //When you exit the synchronized block, the host catcher
                        //may still be stale.  Big deal.  This is better than
                        //waiting for too long here because Java socket timeouts
                        //are slow.
                        staleLock.wait(CONNECT_TIME);
                    }
                }
            } finally {
                synchronized (staleWaitersLock) {
                    staleWaiters--;
                }
            }
        }

        Endpoint endpoint = null; 
        synchronized (this) { 
			while (true)  {
				try { 
					endpoint = getAnEndpointInternal(); 
					break; 
				} catch (NoSuchElementException e) {
					wait(); //throws InterruptedException 
				} 
			} 
        } 
        return endpoint;

//        synchronized (this) {
//            while (true) {                                
//                try {
//                    return getAnEndpointInternal();
//                } catch (NoSuchElementException e) { 
//                    wait(); //throws InterruptedException
//                }
//            }
//        }
    }

    /**
     * @requires this' monitor held
     * @modifies this
     * @effects returns the highest priority endpoint in queue, regardless
     *  of quick-connect settings, etc.  Throws NoSuchElementException if
     *  this is empty.
     */
    private Endpoint getAnEndpointInternal()
            throws NoSuchElementException {
        if (! queue.isEmpty()) {
            //            System.out.println("    GAEI: From "+set+",");
            //pop e from queue and remove from set.
            Endpoint e=(Endpoint)queue.extractMax();
            boolean ok=set.remove(e);
            //check that e actually was in set.
            Assert.that(ok, "Rep. invariant for HostCatcher broken.");
            return e;
        } else
            throw new NoSuchElementException();
    }

    /**
     *  Return the number of hosts
     */
    public int getNumHosts() {
        return( queue.size() );
    }

    /**
     * Returns an iterator of the hosts in this, in order of priority.
     * This can be modified while iterating through the result, but
     * the modifications will not be observed.
     */
    public synchronized Iterator getHosts() {
        //Clone the queue before iterating.
        return (new BucketQueue(queue)).iterator();
    }

    /**
     *  Returns an iterator of the (at most) n best ultrapeer endpoints of this.
     *  It's not guaranteed that these are reachable. This can be modified while
     *  iterating through the result, but the modifications will not be
     *  observed.  
     */
    public synchronized Iterator getUltrapeerHosts(int n) {
        //TODO2: do we really need to clone this?!
        BucketQueue clone=new BucketQueue(queue);
        return clone.iterator(GOOD_PRIORITY,
                              Math.min(n, clone.size(GOOD_PRIORITY)));
    }

    /**
     *  Returns an iterator of the (at most) n best non-ultrapeer endpoints of
     *  this.  It's not guaranteed that these are reachable. This can be
     *  modified while iterating through the result, but the modifications will
     *  not be observed.  
     */
    public synchronized Iterator getNormalHosts(int n) {
        //TODO2: do we really need to clone this?!
        BucketQueue clone=new BucketQueue(queue);
        return clone.iterator(NORMAL_PRIORITY, n);
    }

    /**
     *  Remove unwanted or used entries
     */
    public synchronized void removeHost(String host, int port) {
        Endpoint e=new Endpoint(host, port);
        boolean removed1=set.remove(e);
        boolean removed2=queue.removeAll(e);
        //Check that set.contains(e) <==> queue.contains(e)
        Assert.that(removed1==removed2, "Rep. invariant for HostCatcher broken.");
    }

    /**
     * @modifies this
     * @effects removes all entries from this
     */
    public synchronized void clear() {
        queue.clear();
        set.clear();
        expire();
    }

    /**
     * @modifies this
     * @effects removes all entries from this.  Does not wake up fetcher.
     */
    public synchronized void silentClear() {
        queue.clear();
        set.clear();
    }

    /**
     * @modifies this
     * @effects ensures that the next call to getAnEndpoint will attempt to
     *  contact the pong server.
     */
    public synchronized void expire() {
		if(routerConnectorThreadStarted)
			routerConnectorThread.interrupt();
		else {
			connectToRouter();
		}
    }

    /**
     * If host is not a valid host address, returns false.
     * Otherwise, returns true if connecting to host:port would connect to
     *  the manager's listening port.
     */
    private boolean isMe(String host, int port) {
        //Don't allow connections to yourself.  We have to special
        //case connections to "localhost" or "127.*.*.*" since
        //they are aliases this machine.
        byte[] cIP;
        try {
            cIP=InetAddress.getByName(host).getAddress();
        } catch (IOException e) {
            return false;
        }

        if (cIP[0]==(byte)127) {
            return port == acceptor.getPort();
        } else {
            byte[] managerIP = acceptor.getAddress();
            return (Arrays.equals(cIP, managerIP) &&
                    (port==acceptor.getPort()));
        }
    }

    /** Returns true iff ip is the ip address of router.limewire.com.
     *      @requires ip.length==4 */
    private static boolean isRouter(byte[] ip) {
        //Check for 64.61.25.139-143 and 64.61.25.171
        return ip[0]==(byte)64
            && ip[1]==(byte)61
            && ip[2]==(byte)25
            && (ip[3]==(byte)171
                    || (ip[3]>=(byte)139 && ip[3]<=(byte)143) );
                    
    }

    public String toString() {
        return queue.toString();
    }

    public void setAlwaysNotifyKnownHost(boolean notifyKnownHost) {
        alwaysNotifyKnownHost = notifyKnownHost;
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

//      public static void main(String args[]) {
//          Assert.that(! HostCatcher.isRouter(
//              new byte[] {(byte)127, (byte)0, (byte)0, (byte)1}));
//          Assert.that(! HostCatcher.isRouter(
//              new byte[] {(byte)18, (byte)239, (byte)0, (byte)1}));
//          Assert.that(HostCatcher.isRouter(
//              new byte[] {(byte)64, (byte)61, (byte)25, (byte)171}));
//          Assert.that(HostCatcher.isRouter(
//              new byte[] {(byte)64, (byte)61, (byte)25, (byte)139}));
//          Assert.that(HostCatcher.isRouter(
//              new byte[] {(byte)64, (byte)61, (byte)25, (byte)143}));
//          Assert.that(! HostCatcher.isRouter(
//              new byte[] {(byte)64, (byte)61, (byte)25, (byte)138}));
//          Assert.that(! HostCatcher.isRouter(
//              new byte[] {(byte)64, (byte)61, (byte)25, (byte)170}));
//      }


    /** A simpler unit test */
    /*
    public static void main(String args[]) {
        HostCatcher hc=new HostCatcher(new Main());
        hc.initialize(new Acceptor(6346, null),
                      new ConnectionManager(null, null));

        Iterator iter=hc.getNormalHosts(10);
        Assert.that(! iter.hasNext());
        iter=hc.getUltrapeerHosts(10);
        Assert.that(! iter.hasNext());

        hc.add(new Endpoint("18.239.0.1", 6346), true);
        hc.add(new Endpoint("18.239.0.2", 6346), true);
        hc.add(new Endpoint("128.103.60.1", 6346), false);
        hc.add(new Endpoint("128.103.60.2", 6346), false);

        iter=hc.getUltrapeerHosts(100);
        Assert.that(iter.hasNext());
        Assert.that(iter.next().equals(new Endpoint("18.239.0.2", 6346)));
        Assert.that(iter.hasNext());
        Assert.that(iter.next().equals(new Endpoint("18.239.0.1", 6346)));
        Assert.that(! iter.hasNext());

        iter=hc.getUltrapeerHosts(1);
        Assert.that(iter.hasNext());
        Assert.that(iter.next().equals(new Endpoint("18.239.0.2", 6346)));
        Assert.that(! iter.hasNext());

        iter=hc.getNormalHosts(100);
        Assert.that(iter.hasNext());
        Assert.that(iter.next().equals(new Endpoint("128.103.60.2", 6346)));
        Assert.that(iter.hasNext());
        Assert.that(iter.next().equals(new Endpoint("128.103.60.1", 6346)));
        Assert.that(! iter.hasNext());

        iter=hc.getNormalHosts(1);
        Assert.that(iter.hasNext());
        Assert.that(iter.next().equals(new Endpoint("128.103.60.2", 6346)));
        Assert.that(! iter.hasNext());
    }
    */
}
