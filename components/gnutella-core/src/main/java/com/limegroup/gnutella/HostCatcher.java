package com.limegroup.gnutella;

import com.limegroup.gnutella.util.BucketQueue;
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
 * reconnect to the bootstrap server by demoting ultrapeer pongs.  The
 * doneWithEndpoint(e) method helps this connect to only one bootstrap
 * server at a time.
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
     *  Return the number of hosts
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
                e.setWeight(NORMAL_PRIORITY);
                add(e);
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

    /** Unit test */
    /*
    public static void main(String args[]) {
        testBootstraps();
        testExpire();
        testIterators();
    }

    private static void testBootstraps() {
        try {
            //TODO: factor the new HostCatcher code into a setUp() method.
            System.out.println("-Testing bootstrap servers");
            SettingsManager.instance().setQuickConnectHosts(
                new String[] { "r1.b.c.d:6346", "r2.b.c.d:6347"});
            HostCatcher hc=new HostCatcher(new ActivityCallbackStub());
            hc.initialize(new Acceptor(6346, null),
                          new ConnectionManager(null, null));

            hc.add(new Endpoint("128.103.60.3", 6346), false);
            hc.add(new Endpoint("128.103.60.2", 6346), false);
            hc.add(new Endpoint("128.103.60.1", 6346), false);

            Endpoint router1=hc.getAnEndpoint();
            Assert.that(router1.equals(new Endpoint("r1.b.c.d", 6346)));         
            Assert.that(hc.getAnEndpoint().equals(
                new Endpoint("128.103.60.1", 6346)));
            hc.add(new Endpoint("18.239.0.144", 6346), true);
            hc.doneWithEndpoint(router1);    //got pong
            Assert.that(hc.getAnEndpoint().equals(
                new Endpoint("18.239.0.144", 6346)));        

            Endpoint router2=hc.getAnEndpoint();
            Assert.that(router2.equals(new Endpoint("r2.b.c.d", 6347)));        
            Assert.that(hc.getAnEndpoint().equals(
                new Endpoint("128.103.60.2", 6346)));        
            hc.doneWithEndpoint(router2);    //did't get any pongs

            Assert.that(hc.getAnEndpoint().equals(
                new Endpoint("128.103.60.3", 6346))); //no more bootstraps
        } catch (InterruptedException e) {
            Assert.that(false, "Mysterious InterruptedException");
        }
    }

    private static void testExpire() {
        try {
            System.out.println("-Testing expire");
            SettingsManager.instance().setQuickConnectHosts(
                 new String[] { "r1.b.c.d:6346", "r2.b.c.d:6347"});
            HostCatcher hc=new HostCatcher(new ActivityCallbackStub());
            hc.initialize(new Acceptor(6346, null),
                          new ConnectionManager(null, null));

            Assert.that(hc.getAnEndpoint().equals(new Endpoint("r1.b.c.d", 6346)));

            hc.add(new Endpoint("18.239.0.144", 6346), true);
            hc.add(new Endpoint("128.103.60.3", 6346), false);
            hc.add(new Endpoint("192.168.0.1", 6346));
            Assert.that(hc.getNumUltrapeerHosts()==1);

            hc.expire();
            Assert.that(hc.getNumUltrapeerHosts()==0);
            Endpoint e=hc.getAnEndpoint();
            Assert.that(e.equals(new Endpoint("r1.b.c.d", 6346)));
            hc.doneWithEndpoint(e);
            e=hc.getAnEndpoint();
            Assert.that(e.equals(new Endpoint("r2.b.c.d", 6347)));
            hc.doneWithEndpoint(e);
            Assert.that(hc.getAnEndpoint().equals(
                new Endpoint("18.239.0.144", 6346)));
            Assert.that(hc.getAnEndpoint().equals(
                new Endpoint("128.103.60.3", 6346)));
        } catch (InterruptedException e) { 
            Assert.that(false, "Mysterious InterruptedException");
        }
    }

    private static void testIterators() {
        System.out.println("-Testing iterators");
        HostCatcher hc=new HostCatcher(new ActivityCallbackStub());
        hc.initialize(new Acceptor(6346, null),
                      new ConnectionManager(null, null));

        Iterator iter=hc.getNormalHosts(10);
        Assert.that(! iter.hasNext());
        iter=hc.getUltrapeerHosts(10);
        Assert.that(! iter.hasNext());

        Assert.that(hc.getNumUltrapeerHosts()==0);
        hc.add(new Endpoint("18.239.0.1", 6346), true);
        Assert.that(hc.getNumUltrapeerHosts()==1);
        hc.add(new Endpoint("18.239.0.2", 6346), true);
        hc.add(new Endpoint("128.103.60.1", 6346), false);
        hc.add(new Endpoint("128.103.60.2", 6346), false);
        Assert.that(hc.getNumUltrapeerHosts()==2);

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
