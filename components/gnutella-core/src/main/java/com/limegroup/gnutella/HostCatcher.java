package com.limegroup.gnutella;

import com.limegroup.gnutella.util.*;
import com.sun.java.util.collections.*;
import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Date;
import com.limegroup.gnutella.tests.stubs.ActivityCallbackStub;


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
 * HostCatcher also manages the list of permanent bootstrap servers like
 * <tt>router.limewire.com</tt>.  The expire() method can force this to
 * reconnect to the bootstrap server by demoting ultrapeer pongs.  YOU MUST CALL
 * EXPIRE() TO GET BOOTSTRAP PONGS.  This should be done when calling
 * RouterService.connect().  The doneWithEndpoint(e) method helps this connect
 * to only one bootstrap server at a time.<p>
 *
 * Finally, HostCatcher maintains a list of "permanent" locations, based on
 * average daily uptime.  These are stored in the gnutella.net file.  
 */
public class HostCatcher {    
    /** The number of supernode pongs to store. */
    private static final int GOOD_SIZE=400;
    /** The number of normal pongs to store. */
    private static final int NORMAL_SIZE=100;
    /** The number of private IP pongs to store. */
    private static final int BAD_SIZE=15;
    private static final int SIZE=GOOD_SIZE+NORMAL_SIZE+BAD_SIZE;

    /** The number of permanent locations to store in gnutella.net */
    private static final int PERMANENT_SIZE=1000;

    static final int GOOD_PRIORITY=2;
    static final int NORMAL_PRIORITY=1;
    static final int BAD_PRIORITY=0;


    /** The list of hosts to try.  These are sorted by priority: ultrapeers,
     * normal, then private addresses.  Within each priority level, recent hosts
     * are prioritized over older ones.  Our representation consists of a set
     * and a queue, both bounded in size.  The set lets us quickly check if
     * there are duplicates, while the queue provides ordering.
     *
     * INVARIANT: queue contains no duplicates and contains exactly the
     *  same elements as set.
     * LOCKING: obtain this' monitor before modifying either.  */
    private BucketQueue /* of Endpoint */ queue=
        new BucketQueue(new int[] {BAD_SIZE, NORMAL_SIZE, GOOD_SIZE});
    private Set /* of Endpoint */ set=new HashSet();


    /** The list of pongs with the highest average daily uptimes.  Each host's
     * weight is set to the uptime.  These are most likely to be reachable
     * during the next session, though not necessarily likely to have slots
     * available now.  In this way, they act more like bootstrap hosts than
     * normal pongs.  This list is written to gnutella.net and used to
     * initialize queue on startup.
     *
     * TODO: BinaryHeap is not the right data structure to use.  When full
     * it does not necessarily remove the smallest element.  Better to
     * use a tree with fixed size.  */
    private BinaryHeap /* of Endpoint */ permanentHosts=
        new BinaryHeap(PERMANENT_SIZE);
    

    /** The bootstrap hosts from the QUICK_CONNECT_HOSTS property, e.g.,
     * router.limewire.com.  We try these hosts serially (starting with the
     * head) until we get some good endpoints, e.g. size(GOOD_SIZE)>0.  */
    private LinkedList /* of Endpoint */ bootstrapHosts=new LinkedList();
    /** The bootstrap host we're trying to connect from, or null if none.
     * Prevents us from connecting to more than one bootstrap host at a time.
     * This value is set when removing entries from bootstrapHosts.  It's
     * cleared in doneWithEndpoint(). */
    private Endpoint bootstrapHostInProgress=null;

    private Acceptor acceptor;
    private ConnectionManager manager;
    private ActivityCallback callback;
    private SettingsManager settings=SettingsManager.instance();

    /**
     * whether or not to always notify the activity callback implementor that
     * a host was added to the host catcher.  This is used when the hostcatcher
     * is used with the SimplePongCacheServer to always notify when a host was
     * added.
     */
    private boolean alwaysNotifyKnownHost=false;


    /**
     * Creates an empty host catcher.  Must call initialize before using.
     */
    public HostCatcher(ActivityCallback callback) {
        this.callback=callback;
    }

    /**
     * Links the HostCatcher up with the other back end pieces
     */
    public void initialize(Acceptor acceptor, ConnectionManager manager) {
        initialize(acceptor, manager, null);
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

        //Read gnutella.net
        try {
            if (filename!=null)
                read(filename);
        } catch (FileNotFoundException e) {
        } catch (IOException e) {
        }
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
        for (int i=0; i<SIZE; i++) {
            String line=in.readLine();
            if (line==null)   //nothing left to read?  Done.
                break;

            //Break the line into host, port, and optional average daily uptime.
            //Skip if badly formatted.
            String[] linea=StringUtils.split(line, ": ");
            if (linea.length==0)
                continue;
            String host=linea[0];
            int port=6346;      //Assume port
            if (linea.length>=2) {
                try {
                    port=Integer.parseInt(linea[1]);
                } catch (NumberFormatException nfe) {
                    continue;
                }
            }
            Endpoint e = new Endpoint(host, port);
            int uptime=expectedUptime(e);
            if (linea.length>=3) {
                try {
                    uptime=Integer.parseInt(linea[2]);
                } catch (NumberFormatException nfe) {
                    continue;
                }
            }

            //Everything passed!  Add it.  Note that first elements read are
            //the worst elements, so end up at the tail of the queue.
            add(e, priority(e), uptime);
        }
    }

    /**
     * @modifies the file named filename
     * @effects writes this to the given file.  The file
     *  is prioritized by rough probability of being good.
     */
    synchronized void write(String filename) throws IOException {
        FileWriter out=new FileWriter(filename);       
        //Write elements of permanent from best to worst.  Order matters, as it
        //allows read() to put them into queue in the right order without any
        //difficulty.  So first we copy into an external list...
        Endpoint[] permanentHostsCopy=new Endpoint[permanentHosts.size()];  
        Iterator iter=permanentHosts.iterator();
        for (int i=0; i<permanentHostsCopy.length; i++) 
            permanentHostsCopy[i]=(Endpoint)iter.next();
        //...sort by uptime (weight)
        Arrays.sort(permanentHostsCopy);                        
        //...and write from head (worst) to tail (best).
        for (int i=0; i<permanentHostsCopy.length; i++) {
            Endpoint e=(Endpoint)permanentHostsCopy[i];
            writeInternal(out, e, e.getWeight());
        }

        out.close();
    }

    private void writeInternal(Writer out, Endpoint e, int uptime) 
            throws IOException {
        out.write(e.getHostname());
        out.write(":");
        out.write(Integer.toString(e.getPort()));
        out.write(" ");
        out.write(Integer.toString(uptime));
        out.write("\n");
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
    public boolean add(PingReply pr, ManagedConnection receivingConnection) {
        //Convert to endpoint
        Endpoint e=new Endpoint(pr.getIP(), pr.getPort(),
                    pr.getFiles(), pr.getKbytes());

        int uptime;
        try {
            uptime=pr.getDailyUptime();
        } catch (BadPacketException bpe) {
            uptime=expectedUptime(e);
        }

        //Add the endpoint, forcing it to be high priority if marked pong from a
        //supernode.
        if (pr.isMarked())
            return add(e, GOOD_PRIORITY, uptime);
        else
            return add(e, priority(e), uptime);
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
        //See preamble for a discussion of priorities
        if (forceHighPriority)
            return add(e, GOOD_PRIORITY, expectedUptime(e));
        else
            return add(e, priority(e), expectedUptime(e));
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
    private boolean add(Endpoint e, int priority, int uptime) {
        //We used to check that we're not connected to e, but now we do that in
        //ConnectionFetcher after a call to getAnEndpoint.  This is not a big
        //deal, since the call to "set.contains(e)" below ensures no duplicates.

        //Skip if this would connect us to our listening port.  TODO: I think
        //this check is too strict sometimes, which makes testing difficult.
        if (isMe(e.getHostname(), e.getPort()))
            return false;

        //Skip if this host is banned.
        if (acceptor.isBannedIP(e.getHostname()))
            return false;

        //Skip if this is the router.
        if (ManagedConnection.isRouter(e.getHostname())) 
            return false;

        //Add to permanent list, regardless of whether it's actually in queue.
        //Note that this modifies e.
        addPermanent(e, uptime);

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
        if (alwaysNotifyKnownHost || notifyGUI) 
            callback.knownHost(e);
        return ret;
    }

    /**
     * Adds an address to the permanent list of this without marking it for
     * immediate fetching.  This method is when connecting to a host and reading
     * its Uptime header.
     *
     * @param pr the pong containing the address/port to add.  MODIFIES:
     *  e.getWeight().  Caller should not modify this afterwards.
     * @param uptimeSeconds the time this has been up, in seconds.
     * @return true iff e was actually added 
     */
    private synchronized boolean addPermanent(Endpoint e, int uptimeSeconds) {
        //TODO: should we check that e does not exist in permanentHosts with a
        //different key?
        e.setWeight(uptimeSeconds);
        return permanentHosts.insert(e)!=e;  //was element other than e rejected?
    }
    
    /** Returns BAD_PRIORITY if e private, NORMAL_PRIORITY otherwise. */
    private static int priority(Endpoint e) {
        return e.isPrivateAddress() ? BAD_PRIORITY : NORMAL_PRIORITY;        
    }

    /** Returns the expected average daily uptime of the given node. */
    private static int expectedUptime(Endpoint e) {
        //By looking at version logs, we find that the average session uptime
        //for a host is about 8.1 minutes.  A study of connection uptimes
        //(http://www.limewire.com/developer/lifetimes/) confirms this.
        //Furthermore, we estimate that users connect to the network about 0.71
        //times per day, for a total of 8.1*60*0.71=345 seconds of uptime per
        //day.
        //
        //Why not use 0?  If you have to choose between a node with an unknown
        //uptime and one with a confirmed low uptime, you'll gamble on the
        //former; it's unlikely to be worse!
        return 345;   //8.1 minutes * 60 sec/minute
    }


    ///////////////////////////////////////////////////////////////////////

    /**
     * @modifies this
     * @effects atomically removes and returns the highest priority host in
     *  this.  If no host is available, blocks until one is.  If the calling
     *  thread is interrupted during this process, throws InterruptedException.
     *  The caller should call doneWithEndpoint(..) when done with the 
     *  returned value.
     */
    public synchronized Endpoint getAnEndpoint() throws InterruptedException {
        while (true)  {
            try { 
                //a) If we have a good endpoint, use that.
                if (queue.size(GOOD_PRIORITY)>0) 
                    return getAnEndpointInternal(); 
                //b) If we're not currently connecting to a bootstrap server and
                //there are servers left to try, do so.
                else if (bootstrapHostInProgress==null
                         && !bootstrapHosts.isEmpty()) {                    
                    Endpoint ret=(Endpoint)bootstrapHosts.removeFirst();
                    bootstrapHostInProgress=ret;
                    return ret;
                }
                //c) Try one of the crap pongs.
                else
                    return getAnEndpointInternal();
            } catch (NoSuchElementException e) { }
            
            //No luck?  Wait and try again.
            wait();  //throws InterruptedException 			
        } 
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
     * Notifies this that the fetcher is done with the fetched connection to
     * host.  This exists primarily to tell if we're done with
     * router.limewire.com.
     */
    public synchronized void doneWithEndpoint(Endpoint e) {
        if (e==bootstrapHostInProgress) {  //not .equals
            bootstrapHostInProgress=null;
            notifyAll();  //may be able to try other bootstrap servers
        }
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
        //Add bootstrap hosts IN ORDER.
        bootstrapHostInProgress=null;
        bootstrapHosts.clear();
        String[] hosts=settings.getQuickConnectHosts();
        for (int i=0; i<hosts.length; i++) {
            Endpoint e=new Endpoint(hosts[i]);
            bootstrapHosts.addLast(e);
        }
        
        //Move the N ultrapeer hosts from GOOD to NORMAL.  This forces
        //getAnEndpointInternal() to return a bootstrap pong next.  This is a
        //little weird, because these hosts really still are ultrapeer
        //pongs--just lesser priority.
        int n=getNumUltrapeerHosts();
        for (int i=0; i<n; i++) {
            try {
                Endpoint e=getAnEndpointInternal();
                //Uptime doesn't matter, since e already in permanent.
                add(e, NORMAL_PRIORITY, 0);
            } catch (NoSuchElementException e) {
                Assert.that(false, 
                    i+"'th getAnEndpointInternal not consistent with "+n);
            }
        }
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
            return port == acceptor.getPort();
        } else {
            byte[] managerIP = acceptor.getAddress();
            return (Arrays.equals(cIP, managerIP) &&
                    (port==acceptor.getPort()));
        }
    }

    public String toString() {
        return queue.toString();
    }

    public void setAlwaysNotifyKnownHost(boolean notifyKnownHost) {
        alwaysNotifyKnownHost = notifyKnownHost;
    }

    //Unit test: tests/com/.../gnutella/HostCatcherTest.java   
}
