package com.limegroup.gnutella;

import com.limegroup.gnutella.util.*;
import com.limegroup.gnutella.bootstrap.*;
import com.sun.java.util.collections.*;
import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Date;
import java.text.ParseException;


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
 * HostCatcher also manages the list of GWebCache servers.  YOU MUST CALL
 * EXPIRE() TO START THE GBWEBCACHE BOOTSTRAPING PROCESS.  This should be done
 * when calling RouterService.connect().<p>
 *
 * Finally, HostCatcher maintains a list of "permanent" locations, based on
 * average daily uptime.  These are stored in the gnutella.net file.  They
 * are NOT bootstrap servers like router.limewire.com; LimeWire doesn't
 * use those anymore.
 */
public class HostCatcher {    
    //These constants are package-access for testing.  
    //That's ok as they're final.

    /** The number of supernode pongs to store. */
    static final int GOOD_SIZE=400;
    /** The number of normal pongs to store. 
     *  This should be large enough to store all permanent addresses. */
    static final int NORMAL_SIZE=1000;
    /** The number of private IP pongs to store. */
    static final int BAD_SIZE=15;
    static final int SIZE=GOOD_SIZE+NORMAL_SIZE+BAD_SIZE;

    /** The number of permanent locations to store in gnutella.net */
    static final int PERMANENT_SIZE=NORMAL_SIZE;

    static final int GOOD_PRIORITY=2;
    static final int NORMAL_PRIORITY=1;
    static final int BAD_PRIORITY=0;


    /** The list of hosts to try.  These are sorted by priority: ultrapeers,
     * normal, then private addresses.  Within each priority level, recent hosts
     * are prioritized over older ones.  Our representation consists of a set
     * and a queue, both bounded in size.  The set lets us quickly check if
     * there are duplicates, while the queue provides ordering--a classic
     * space/time tradeoff.
     *
     * INVARIANT: queue contains no duplicates and contains exactly the
     *  same elements as set.
     * LOCKING: obtain this' monitor before modifying either.  */
    private BucketQueue /* of ExtendedEndpoint */ queue=
        new BucketQueue(new int[] {BAD_SIZE, NORMAL_SIZE, GOOD_SIZE});
    private Set /* of ExtendedEndpoint */ set=new HashSet();


    /** The list of pongs with the highest average daily uptimes.  Each host's
     * weight is set to the uptime.  These are most likely to be reachable
     * during the next session, though not necessarily likely to have slots
     * available now.  In this way, they act more like bootstrap hosts than
     * normal pongs.  This list is written to gnutella.net and used to
     * initialize queue on startup.  To prevent duplicates, we also maintain a
     * set of all addresses, like with queue/set.
     *
     * INVARIANT: permanentHosts contains no duplicates and contains exactly
     *  the same elements and permanentHostsSet
     * LOCKING: obtain this' monitor before modifying either */
    private FixedsizePriorityQueue /* of ExtendedEndpoint */ permanentHosts=
        new FixedsizePriorityQueue(ExtendedEndpoint.priorityComparator(),
                                   PERMANENT_SIZE);
    private Set /* of ExtendedEndpoint */ permanentHostsSet=new HashSet();

    
    /** The GWebCache bootstrap system. */
    private BootstrapServerManager gWebCache=new BootstrapServerManager(this);
    
    /** The bootstrap hosts from the QUICK_CONNECT_HOSTS property, e.g.,
     * router.limewire.com.  We try these hosts serially (starting with the
     * head) until we get some good endpoints, e.g. size(GOOD_SIZE)>0.  */
    private LinkedList /* of Endpoint */ bootstrapHosts=new LinkedList();
    /** The bootstrap host we're trying to connect from, or null if none.
     * Prevents us from connecting to more than one bootstrap host at a time.
     * This value is set when removing entries from bootstrapHosts.  It's
     * cleared in doneWithEndpoint(). */
    private Endpoint bootstrapHostInProgress=null;

	/**
	 * Constant for the <tt>SettingsManager</tt>.
	 */
    private SettingsManager settings=SettingsManager.instance();

    /**
     * whether or not to always notify the activity callback implementor that
     * a host was added to the host catcher.  This is used when the hostcatcher
     * is used with the SimplePongCacheServer to always notify when a host was
     * added.
     */
    private boolean alwaysNotifyKnownHost=false;

	/**
	 * Constant for the <tt>QueryUnicaster</tt> instance.
	 */
	private final QueryUnicaster UNICASTER = QueryUnicaster.instance();

    /**
     * Links the HostCatcher up with the other back end pieces, and, if quick
     * connect is not specified in the SettingsManager, loads the hosts in the
     * host list into the maybe set.  (The likelys set is empty.)  If filename
     * does not exist, then no error message is printed and this is initially
     * empty.  The file is expected to contain a sequence of lines in the format
     * "<host>:port\n".  Lines not in this format are silently ignored.
     */
    public void initialize() {
		String filename = settings.getHostList();

        //Read gnutella.net
        try {
            if (filename!=null)
                read(filename);
        } catch (FileNotFoundException e) {
        } catch (IOException e) {
        }

        //Register to send updates every hour (starting in one hour) if we're a
        //supernode and have accepted incoming connections.  I think we should
        //only do this if we also have incoming slots, but John Marshall from
        //Gnucleus says otherwise.
        Runnable updater=new Runnable() {
            public void run() {
                try {
                    if (RouterService.acceptedIncomingConnection() && 
                        RouterService.isSupernode()) {
                            Endpoint e=new Endpoint(RouterService.getAddress(),
                                                    RouterService.getPort());
                            //This spawn another thread, so blocking is not an issue.
                            gWebCache.sendUpdatesAsync(e);
                        }
                } catch(Throwable t) {
                    RouterService.error(t);
                }
            }
        };
        RouterService.schedule(updater, 
							   BootstrapServerManager.UPDATE_DELAY_MSEC, 
							   BootstrapServerManager.UPDATE_DELAY_MSEC);
    }


    /**
     * Reads in endpoints from the given file.  This is called by initialize, so
     * you don't need to call it manually.  It is package access for
     * testability.
     *
     * @modifies this
     * @effects read hosts from the given file.  
     */
    synchronized void read(String filename)
            throws FileNotFoundException, IOException {
        BufferedReader in=null;
        in=new BufferedReader(new FileReader(filename));
        while (true) {
            String line=in.readLine();
            if (line==null)
                break;

            //If endpoint a special GWebCache endpoint?  If so, add it to
            //gWebCache but not this.
            try {
                BootstrapServer e=new BootstrapServer(line);
                gWebCache.addBootstrapServer(e);
                continue;
            } catch (ParseException ignore) { }

            //Is it a normal endpoint?
            try {
                ExtendedEndpoint e=ExtendedEndpoint.read(line);
                add(e, priority(e));
            } catch (ParseException pe) {
                continue;
            }
        }
    }

    /**
     * @modifies the file named filename
     * @effects writes this to the given file.  The file
     *  is prioritized by rough probability of being good.
     *  GWebCache entries are also included in this file.
     */
    synchronized void write(String filename) throws IOException {
        repOk();
        FileWriter out=new FileWriter(filename);       
        //Write servers from GWebCache to output.
        synchronized (gWebCache) {
            for (Iterator iter=gWebCache.getBootstrapServers();iter.hasNext();){
                BootstrapServer e=(BootstrapServer)iter.next();
                out.write(e.toString());
                out.write(ExtendedEndpoint.EOL);
            }
        }
        //Write elements of permanent from worst to best.  Order matters, as it
        //allows read() to put them into queue in the right order without any
        //difficulty.
        for (Iterator iter=permanentHosts.iterator(); iter.hasNext(); ) {
            ExtendedEndpoint e=(ExtendedEndpoint)iter.next();
            e.write(out);
        }
        out.close();
    }

    ///////////////////////////// Add Methods ////////////////////////////


    /**
     * Attempts to add a pong to this, possibly ejecting other elements from the
     * cache.  This method used to be called "spy".
     *
     * @param pr the pong containing the address/port to add
     * @param receivingConnection the connection on which we received
     *  the pong.
     * @return true iff pr was actually added 
     */
    public boolean add(PingReply pr, ReplyHandler receivingConnection) {
        //Convert to endpoint
        ExtendedEndpoint endpoint;
        boolean supportsUnicast = false;
        try {
			supportsUnicast = pr.supportsUnicast();
            endpoint = new ExtendedEndpoint(pr.getIP(), pr.getPort(), 
											pr.getDailyUptime());
        } catch (BadPacketException bpe) {
            endpoint = new ExtendedEndpoint(pr.getIP(), pr.getPort());
        }

        if(supportsUnicast) {
            try {
                UNICASTER.addUnicastEndpoint(InetAddress.getByName(pr.getIP()), 
                                             pr.getPort());
            } catch(UnknownHostException e) {
                // nothing we can do if the host is not recognized -- this
                // should never happen for raw IP addresses, as there is
                // no DNS lookup anyway after Java 1.1 (1.1.8 does NOT
                // do a DNS lookup).
            }
        }

        //Add the endpoint, forcing it to be high priority if marked pong from a
        //supernode.
        if (pr.isMarked())
            return add(endpoint, GOOD_PRIORITY);
        else
            return add(endpoint, priority(endpoint));
    }

    /**
     * Adds an address to this, possibly ejecting other elements from the cache.
     * This method is used when getting an address from headers instead of the
     * normal ping reply.
     *
     * @param pr the pong containing the address/port to add.  MODIFIES:
     *  e.getWeight().  Caller should not modify this afterwards.
     * @param forceHighPriority true if this should always be of high priority
     * @return true iff e was actually added
     */
    public boolean add(Endpoint e, boolean forceHighPriority) {
        ExtendedEndpoint ee=new ExtendedEndpoint(e.getHostname(), e.getPort());
        //See preamble for a discussion of priorities
        if (forceHighPriority)
            return add(ee, GOOD_PRIORITY);
        else
            return add(ee, priority(ee));
    }

    /**
     * Adds the passed endpoint to the set of hosts maintained, temporary and
     * permanent. The endpoint may not get added due to various reasons
     * (including it might be our address itself, we might be connected to it
     * etc.). Also adding this endpoint may lead to the removal of some other
     * endpoint from the cache.
     *
     * @param e Endpoint to be added
     * @param priority the priority to use for e, one of GOOD_PRIORITY 
     *  (ultrapeer), NORMAL_PRIORITY, or BAD_PRIORITY (private address)
     * @param uptime the host's uptime (or our best guess)
     *
     * @return true iff e was actually added 
     */
    private boolean add(ExtendedEndpoint e, int priority) {
        repOk();
        //We used to check that we're not connected to e, but now we do that in
        //ConnectionFetcher after a call to getAnEndpoint.  This is not a big
        //deal, since the call to "set.contains(e)" below ensures no duplicates.

        //Skip if this would connect us to our listening port.  TODO: I think
        //this check is too strict sometimes, which makes testing difficult.
        if (isMe(e.getHostname(), e.getPort()))
            return false;

        //Skip if this host is banned.
        if (RouterService.getAcceptor().isBannedIP(e.getHostname()))
            return false;

        //Skip if this is the router.
        if (ManagedConnection.isRouter(e.getHostname())) 
            return false;

        //Add to permanent list, regardless of whether it's actually in queue.
        //Note that this modifies e.
        addPermanent(e);

        boolean ret=false;
        boolean notifyGUI=false;
        synchronized(this) {
            if (! (set.contains(e))) {
                ret=true;
                //Add to temporary list. Adding e may eject an older point from
                //queue, so we have to cleanup the set to maintain
                //rep. invariant.
                set.add(e);
                Object ejected=queue.insert(e, priority);
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

        //we notify the callback in two different situations.  One situation, we
        //always notify the GUI (e.g., a SimplePongCacheServer which needs to know
        //when a new host was added).  The second situation is if the host catcher
        //is not full, so that the endpoint is added to the GUI for the user to
        //view and use.  The second situation occurs the majority of times and
        //only in special cases such as a SimplePongCacheServer would the first 
        //situation occur.
        if (alwaysNotifyKnownHost || notifyGUI) {
            RouterService.getCallback().knownHost(e);
		}
        repOk();
        return ret;
    }

    /**
     * Adds an address to the permanent list of this without marking it for
     * immediate fetching.  This method is when connecting to a host and reading
     * its Uptime header.  If e is already in the permanent list, it is not
     * re-added, though its key may be adjusted.
     *
     * @param e the endpoint to add
     * @return true iff e was actually added 
     */
    private synchronized boolean addPermanent(ExtendedEndpoint e) {
        if (e.isPrivateAddress())
            return false;
        if (permanentHostsSet.contains(e))
            //TODO: we could adjust the key
            return false;
        
        Object removed=permanentHosts.insert(e);
        if (removed!=e) {
            //Was actually added...
            permanentHostsSet.add(e);
            if (removed!=null)
                //...and something else was removed.
                permanentHostsSet.remove(removed);
            return true;
        } else {
            //Uptime not good enough to add.  (Note that this is 
            //really just an optimization of the above case.)
            return false;
        }
    }
    
    /** Removes e from permanentHostsSet and permanentHosts. 
     *  @return true iff this was modified */
    private synchronized boolean removePermanent(ExtendedEndpoint e) {
        boolean removed1=permanentHosts.remove(e);
        boolean removed2=permanentHostsSet.remove(e);
        Assert.that(removed1==removed2,
                    "Queue "+removed1+" but set "+removed2);
        return removed1;
    }

    /** Returns BAD_PRIORITY if e private, NORMAL_PRIORITY otherwise. */
    private static int priority(Endpoint e) {
        return e.isPrivateAddress() ? BAD_PRIORITY : NORMAL_PRIORITY;        
    }


    ///////////////////////////////////////////////////////////////////////

    /**
     * @modifies this
     * @effects atomically removes and returns the highest priority host in
     *  this.  If no host is available, blocks until one is.  If the calling
     *  thread is interrupted during this process, throws InterruptedException.
     *  The caller should call doneWithConnect and doneWithMessageLoop when done
     *  with the returned value.
     */
    public synchronized Endpoint getAnEndpoint() throws InterruptedException {
        while (true)  {
            //If we've run out of good hosts, asynchronously contact a GWebCache
            //server to get more addresses.  Note, however, that this will not
            //do anything if we're already in the process of connecting.  See
            //also expire().
            if (getNumUltrapeerHosts()==0) 
                gWebCache.fetchEndpointsAsync();

            try { 
                return getAnEndpointInternal();
            } catch (NoSuchElementException e) { }
            
            //No luck?  Wait and try again.
            wait();  //throws InterruptedException          
        } 
    }
  
    /**
     * Notifies this that the fetcher has finished attempting a connection to
     * the given host.  This exists primarily to update the permanent host list
     * with connection history.
     *
     * @param e the address/port, which should have been returned by 
     *  getAnEndpoint
     * @param success true if we successfully established a messaging connection 
     *  to e, at least temporarily; false otherwise 
     */
    public synchronized void doneWithConnect(Endpoint e, boolean success) {
        //Normal host: update key.  TODO3: adjustKey() operation may be more
        //efficient.
        if (! (e instanceof ExtendedEndpoint))
            //Should never happen, but I don't want to update public
            //interface of this to operate on ExtendedEndpoint.
            return;
        ExtendedEndpoint ee=(ExtendedEndpoint)e;

        removePermanent(ee);
        if (success)
            ee.recordConnectionSuccess();
        else
            ee.recordConnectionFailure();
        addPermanent(ee);
    }

    /**
     * @requires this' monitor held
     * @modifies this
     * @effects returns the highest priority endpoint in queue, regardless
     *  of quick-connect settings, etc.  Throws NoSuchElementException if
     *  this is empty.
     */
    private ExtendedEndpoint getAnEndpointInternal()
            throws NoSuchElementException {
        if (! queue.isEmpty()) {
            //            System.out.println("    GAEI: From "+set+",");
            //pop e from queue and remove from set.
            ExtendedEndpoint e=(ExtendedEndpoint)queue.extractMax();
            boolean ok=set.remove(e);
            //check that e actually was in set.
            Assert.that(ok, "Rep. invariant for HostCatcher broken.");
            return e;
        } else
            throw new NoSuchElementException();
    }

    /**
     *  Return the number of hosts, i.e.,
     *  getNumUltrapeerHosts()+getNumNormalHosts()+getNumPrivateHosts().  
     */
    public int getNumHosts() {
        return( queue.size() );
    }

    /**
     * Returns the number of marked ultrapeer hosts.
     */
    public int getNumUltrapeerHosts() {
        return queue.size(GOOD_PRIORITY);
    }
    
    /**
     * Returns the number of non-marked non-private hosts.
     */
    int getNumNormalHosts() {
        return queue.size(NORMAL_PRIORITY);
    }

    /**
     * Returns the number of non-marked private hosts.
     */
    int getNumPrivateHosts() {
        return queue.size(BAD_PRIORITY);
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
     * Returns an iterator of this' "permanent" hosts, from worst to best.
     * This method exists primarily for testing.  THIS MUST NOT BE MODIFIED
     * WHILE ITERATOR IS IN USE.
     */
    Iterator getPermanentHosts() {
        return permanentHosts.iterator();
    }

    /**
     *  Returns an iterator of the (at most) n best ultrapeer endpoints of this.
     *  It's not guaranteed that these are reachable. This can be modified while
     *  iterating through the result, but the modifications will not be
     *  observed.  
     */
    public synchronized Iterator getUltrapeerHosts(int n) {
        //Make n the # of hosts to return--never more than the # of ultrapeers.
        n=Math.min(n, queue.size(GOOD_PRIORITY));
        //Copy n best hosts into temporary buffer.
        ArrayList /* of ExtendedEndpoint */ buf=new ArrayList(n);
        for (Iterator iter=queue.iterator(GOOD_PRIORITY, n); iter.hasNext(); )
            buf.add(iter.next());
        //And return iterator of contents.
        return buf.iterator();
    }

    /**
     *  Returns an iterator of the (at most) n best non-ultrapeer endpoints of
     *  this.  It's not guaranteed that these are reachable. This can be
     *  modified while iterating through the result, but the modifications will
     *  not be observed.  
     */
    public synchronized Iterator getNormalHosts(int n) {
        //Copy n best hosts into temporary buffer.
        ArrayList /* of ExtendedEndpoint */ buf=new ArrayList(n);
        for (Iterator iter=queue.iterator(NORMAL_PRIORITY, n); iter.hasNext(); )
            buf.add(iter.next());
        //And return iterator of contents.
        return buf.iterator();
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
     * Notifies this that connect() has been called.  This may decide to give
     * out bootstrap pongs if necessary.
     */
    public synchronized void expire() {
        //Fetch more GWebCache urls once per session.
        //(Well, once per connect really--good enough.)
        gWebCache.fetchBootstrapServersAsync();
        //It's not strictly necessary to fetch endpoints here, since
        //getAnEndpoint() does that when there are no more good endpoints.  But
        //this can help get fresh values if LimeWire has been disconnected for a
        //while.
        gWebCache.fetchEndpointsAsync();
    }

    /**
     * @modifies this
     * @effects removes all entries from this
     */
    public synchronized void clear() {
        queue.clear();
        set.clear();
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
            return port == RouterService.getPort();
        } else {
            byte[] managerIP = RouterService.getAddress();
            return (Arrays.equals(cIP, managerIP) &&
                    (port==RouterService.getPort()));
        }
    }

    public String toString() {
        return "[volatile:"+queue.toString()
               +", permanent:"+permanentHosts.toString()+"]";
    }

    public void setAlwaysNotifyKnownHost(boolean notifyKnownHost) {
        alwaysNotifyKnownHost = notifyKnownHost;
    }

    /** Enable very slow rep checking?  Package access for use by
     *  HostCatcherTest. */
    static boolean DEBUG=false;
    /** Checks invariants. Very slow; method body should be enabled for testing
     *  purposes only. */
    protected void repOk() {
        if (!DEBUG)
            return;

        //Check set == queue
        outer:
        for (Iterator iter=set.iterator(); iter.hasNext(); ) {
            Object e=iter.next();
            for (Iterator iter2=queue.iterator(); iter2.hasNext(); ) {
                if (e.equals(iter2.next()))
                    continue outer;
            }
            Assert.that(false, "Couldn't find "+e+" in queue");
        }
        for (Iterator iter=queue.iterator(); iter.hasNext(); ) {
            Object e=iter.next();
            Assert.that(e instanceof ExtendedEndpoint);
            Assert.that(set.contains(e));
        }

        //Check permanentHosts === permanentHostsSet
        for (Iterator iter=permanentHosts.iterator(); iter.hasNext(); ) {
            Object o=iter.next();
            Assert.that(o instanceof ExtendedEndpoint);
            Assert.that(permanentHostsSet.contains(o));
        }
        for (Iterator iter=permanentHostsSet.iterator(); iter.hasNext(); ) {
            Object e=iter.next();
            Assert.that(e instanceof ExtendedEndpoint);
            Assert.that(permanentHosts.contains(e),
                        "Couldn't find "+e+" from "
                        +permanentHostsSet+" in "+permanentHosts);
        }
    }

    //Unit test: tests/com/.../gnutella/HostCatcherTest.java   
}
