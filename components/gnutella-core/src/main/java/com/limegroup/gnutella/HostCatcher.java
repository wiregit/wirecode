package com.limegroup.gnutella;

import com.limegroup.gnutella.messages.*;
import com.limegroup.gnutella.util.*;
import com.limegroup.gnutella.bootstrap.*;
import com.limegroup.gnutella.settings.ApplicationSettings;
import com.sun.java.util.collections.*;
 
import java.io.*;
import java.text.ParseException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


/**
 * The host catcher.  This peeks at pong messages coming on the
 * network and snatches IP addresses of other Gnutella peers.  IP
 * addresses may also be added to it from a file (usually
 * "gnutella.net").  The servent may then connect to these addresses
 * as necessary to maintain full connectivity.<p>
 *
 * The HostCatcher currently prioritizes pongs as follows.  Note that Ultrapeers
 * with a private address is still highest priority; hopefully this may allow
 * you to find local Ultrapeers.
 * <ol>
 * <li> Ultrapeers.  Ultrapeers are identified because the number of files they
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
public class HostCatcher implements HostListener {    
    
    /**
     * Log for logging this class.
     */
    private static final Log LOG = LogFactory.getLog(HostCatcher.class);
        
    //These constants are package-access for testing.  
    //That's ok as they're final.

    /** The number of milliseconds to wait after trying gnutella.net entries
     *  before resorting to GWebCache HOSTFILE requests. */
    public static final int GWEBCACHE_DELAY=6000;  //6 seconds    
    
    /**
     * Size of the queue for hosts returned from the GWebCaches.
     */
    static final int CACHE_SIZE = 20;
    
    /**
     * The number of ultrapeer pongs to store.
     */
    static final int GOOD_SIZE=1000;
    
    /**
     * The number of normal pongs to store.
     * This must be large enough to store all permanent addresses, 
     * as permanent addresses when read from disk are stored as
     * normal priority.
     */    
    static final int NORMAL_SIZE=400;

    /**
     * The number of permanent locations to store in gnutella.net 
     * This MUST NOT BE GREATER THAN NORMAL_SIZE.  This is because when we read
     * in endpoints, we add them as NORMAL_PRIORITY.  If we have written
     * out more than NORMAL_SIZE hosts, then we guarantee that endpoints
     * will be ejected from the ENDPOINT_QUEUE upon startup.
     * Because we write out best first (and worst last), and thus read in
     * best first (and worst last) this means that we will be ejecting
     * our best endpoints and using our worst ones when starting.
     * 
     */
    static final int PERMANENT_SIZE = NORMAL_SIZE;
    
    /**
     * Constant for the priority of hosts retrieved from GWebCaches.
     */
    public static final int CACHE_PRIORITY = 2;

    /**
     * Constant for the index of good priority hosts (Ultrapeers)
     */
    public static final int GOOD_PRIORITY = 1;

    /**
     * Constant for the index of non-Ultrapeer hosts.
     */
    public static final int NORMAL_PRIORITY = 0;


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
    private final BucketQueue /* of ExtendedEndpoint */ ENDPOINT_QUEUE = 
        new BucketQueue(new int[] {NORMAL_SIZE,GOOD_SIZE, CACHE_SIZE});
    private final Set /* of ExtendedEndpoint */ ENDPOINT_SET = new HashSet();
    
    /**
     * <tt>Set</tt> of hosts advertising free Ultrapeer connection slots.
     */
    private final Set FREE_ULTRAPEER_SLOTS_SET = new HashSet();
    
    /**
     * <tt>Set</tt> of hosts advertising free leaf connection slots.
     */
    private final Set FREE_LEAF_SLOTS_SET = new HashSet();    

    
    /**
     * map of locale (string) to sets (of endpoints).
     */
    private final Map LOCALE_2_SET =  new HashMap();
    
    /**
     * number of endpoints to keep in the locale set
     */
    private static final int NUM_2_KEEP_LOCALE_SET = 100;

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
    private BootstrapServerManager gWebCache = 
        BootstrapServerManager.instance();
    
    /** The time we're next allowed to send a HOSTFILE request because of no
     *  fresh ultrapeer pongs.  The default value of MAX_VALUE means we're not
     *  initially allowed to. */
    private long nextAllowedFetchTime=Long.MAX_VALUE;

	/**
	 * Constant for the host file to read from and write to.
	 */
	private final File HOST_FILE;

    /**
     * Count for the number of hosts that we have not been able to connect to.
     * This is used for degenerate cases where we ultimately have to hit the 
     * GWebCaches.
     */
    private int _failures;
    
    /**
     * <tt>Set</tt> of hosts we were unable to create TCP connections with
     * and should therefore not be tried again.  Fixed size.
     * 
     * LOCKING: obtain this' monitor before modifying/iterating
     */
    private final Set EXPIRED_HOSTS = new HashSet();
    
    /**
     * <tt>Set</tt> of hosts we were able to create TCP connections with but 
     * did not accept our Gnutella connection, and are therefore put on 
     * "probation".  Fixed size.
     * 
     * LOCKING: obtain this' monitor before modifying/iterating
     */    
    private final Set PROBATION_HOSTS = new HashSet();

    /**
     * Flag for whether or not we've already hit the GWebCaches.
     */
    private boolean _hitCaches;
    
    /**
     * Constant for the number of milliseconds to wait before periodically
     * recovering hosts on probation.  Non-final for testing.
     */
    private static long PROBATION_RECOVERY_WAIT_TIME = 60*1000;

    /**
     * Constant for the number of milliseconds to wait between calls to 
     * recover hosts that have been placed on probation.  
     * Non-final for testing.
     */
    private static long PROBATION_RECOVERY_TIME = 60*1000;
    
    /**
     * Constant for the size of the set of hosts put on probation.  Public for
     * testing.
     */
    public static final int PROBATION_HOSTS_SIZE = 500;

    /**
     * Constant for the size of the set of expired hosts.  Public for
     * testing.  
     */
    public static final int EXPIRED_HOSTS_SIZE = 500;
    
	/**
	 * Creates a new <tt>HostCatcher</tt> instance with a constant setting
	 * for the host file location.
	 */
	public HostCatcher() {
		HOST_FILE = 
			new File(CommonUtils.getUserSettingsDir(), "gnutella.net");
	}

    /**
     * Links the HostCatcher up with the other back end pieces, and, if quick
     * connect is not specified in the SettingsManager, loads the hosts in the
     * host list into the maybe set.  (The likelys set is empty.)  If filename
     * does not exist, then no error message is printed and this is initially
     * empty.  The file is expected to contain a sequence of lines in the format
     * "<host>:port\n".  Lines not in this format are silently ignored.
     */
    public void initialize() {
        //Read gnutella.net
        readHostsFile();
        
        sendUDPPings();
        
        //Register to send updates every hour (starting in one hour) if we're a
        //supernode and have accepted incoming connections.  I think we should
        //only do this if we also have incoming slots, but John Marshall from
        //Gnucleus says otherwise.
        Runnable updater=new Runnable() {
            public void run() {
                try {
                    if (RouterService.acceptedIncomingConnection() && 
                        RouterService.isSupernode()) {
                            byte[] addr = RouterService.getAddress();
                            int port = RouterService.getPort();
                            if(NetworkUtils.isValidAddress(addr) &&
                               NetworkUtils.isValidPort(port) &&
                               !NetworkUtils.isPrivateAddress(addr)) {
                                Endpoint e=new Endpoint(addr, port);
								// This spawns another thread, so blocking is  
                                // not an issue.
								gWebCache.sendUpdatesAsync(e);
							}
                        }
                } catch(Throwable t) {
                    ErrorService.error(t);
                }
            }
        };
        
        RouterService.schedule(updater, 
							   BootstrapServerManager.UPDATE_DELAY_MSEC, 
							   BootstrapServerManager.UPDATE_DELAY_MSEC);
        
        Runnable probationRestorer = new Runnable() {
            public void run() {
                try {
                    LOG.trace("restoring hosts on probation");
                    synchronized(HostCatcher.this) {
                        Iterator iter = PROBATION_HOSTS.iterator();
                        while(iter.hasNext()) {
                            Endpoint host = (Endpoint)iter.next();
                            add(host, false);
                        }
                        
                        PROBATION_HOSTS.clear();
                    }
                } catch(Throwable t) {
                    ErrorService.error(t);
                }
            } 
        };
        
        // Recover hosts on probation every minute.
        RouterService.schedule(probationRestorer, 
            PROBATION_RECOVERY_WAIT_TIME, PROBATION_RECOVERY_TIME);
    }

    /**
     * Sends UDP pings to hosts read from disk.
     */
    private void sendUDPPings() {
        // We need the lock on this so that we can copy the set of endpoints.
        synchronized(this) {
            UDPHostRanker.rank(new HashSet(ENDPOINT_SET), this);
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
    synchronized void read(File hostFile) throws FileNotFoundException, 
												 IOException {
        LOG.trace("entered HostCatcher.read(File)");
        BufferedReader in = null;
        try {
            in = new BufferedReader(new FileReader(hostFile));
            while (true) {
                String line=in.readLine();
                if (line==null)
                    break;
                    
                //If endpoint a special GWebCache endpoint?  If so, add it to
                //gWebCache but not this.
                try {
                    gWebCache.addBootstrapServer(new BootstrapServer(line));
                    continue;
                } catch (ParseException ignore) { }
    
                //Is it a normal endpoint?
                try {
                    add(ExtendedEndpoint.read(line), NORMAL_PRIORITY);
                } catch (ParseException pe) {
                    continue;
                }
            }
        } finally {
            try {
                if( in != null )
                    in.close();
            } catch(IOException e) {}
        }
        LOG.trace("left HostCatcher.read(File)");
    }

	/**
	 * Writes the host file to the default location.
	 *
	 * @throws <tt>IOException</tt> if the file cannot be written
	 */
	synchronized void write() throws IOException {
		write(HOST_FILE);
	}

    /**
     * @modifies the file named filename
     * @effects writes this to the given file.  The file
     *  is prioritized by rough probability of being good.
     *  GWebCache entries are also included in this file.
     */
    synchronized void write(File hostFile) throws IOException {
        repOk();
        FileWriter out = new FileWriter(hostFile);       
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
    public boolean add(PingReply pr) {
        //Convert to endpoint
        ExtendedEndpoint endpoint;
        
        if(pr.getDailyUptime() != -1) {
            endpoint = new ExtendedEndpoint(pr.getAddress(), pr.getPort(), 
											pr.getDailyUptime());
        } else {
            endpoint = new ExtendedEndpoint(pr.getAddress(), pr.getPort());
        }
        
        //if the PingReply had locale information then set it in the endpoint
        if(!pr.getClientLocale().equals(""))
            endpoint.setClientLocale(pr.getClientLocale());
        
        if(!isValidHost(endpoint)) return false;
        
        if(pr.supportsUnicast()) {
            QueryUnicaster.instance().
				addUnicastEndpoint(pr.getInetAddress(), pr.getPort());
        }


        //Add the endpoint, forcing it to be high priority if marked pong from 
        //an ultrapeer.
            
        if (pr.isUltrapeer()) {
            // Add it to our free leaf slots list if it has free leaf slots and
            // is an Ultrapeer.
            if(pr.hasFreeLeafSlots()) {
                addToFixedSizeSet(endpoint, FREE_LEAF_SLOTS_SET);
                // Return now if the pong is not also advertising free 
                // ultrapeer slots.
                if(!pr.hasFreeUltrapeerSlots()) {
                    return true;
                }
            } 
            
            // Add it to our free leaf slots list if it has free leaf slots and
            // is an Ultrapeer.
            if(pr.hasFreeUltrapeerSlots() 
               ||
               (ApplicationSettings.LANGUAGE.getValue()
                .equals(pr.getClientLocale()) && pr.getNumFreeLocaleSlots() > 0)) {
                addToFixedSizeSet(endpoint, FREE_ULTRAPEER_SLOTS_SET);
                return true;
            } 
            
            return add(endpoint, GOOD_PRIORITY); 
        } else
            return add(endpoint, NORMAL_PRIORITY);
    }
    
    /**
     * Utility method for adding the specified host to the specified 
     * <tt>Set</tt>, fixing the size of the set at the pre-defined limit for
     * the number of hosts with free slots to store.
     * 
     * @param host the host to add
     * @param hosts the <tt>Set</tt> to add it to
     */
    private synchronized void addToFixedSizeSet(ExtendedEndpoint host, 
        Set hosts) {
        
        // Don't allow the free slots host to expand infinitely.
        if(hosts.add(host) && hosts.size() > 200) {
            hosts.remove(hosts.iterator().next());
        }
        
        // Also add it to the list of permanent hosts stored on disk.
        addPermanent(host);
        notify();
    }

    /**
     * add the endpoint to the map which matches locales to a set of 
     * endpoints
     */
    private synchronized void addToLocaleMap(ExtendedEndpoint endpoint) {
        String loc = endpoint.getClientLocale();
        if(LOCALE_2_SET.containsKey(loc)) { //if set exists for ths locale
            Set s = (Set)LOCALE_2_SET.get(loc);
            if(s.add(endpoint) && s.size() > NUM_2_KEEP_LOCALE_SET)
                s.remove(s.iterator().next());
        }
        else { //otherwise create new set and add it to the map
            Set s = new HashSet();
            s.add(endpoint);
            LOCALE_2_SET.put(loc, s);
        }
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
        if (forceHighPriority)
            return add(e, GOOD_PRIORITY);
        else
            return add(e, NORMAL_PRIORITY);
    }

    

    /**
     * Adds an endpoint.  Use this method if the locale of endpoint is known
     * (used by ConnectionManager.disconnect())
     */
    public boolean add(Endpoint e, boolean forceHighPriority, String locale) {
        //need ExtendedEndpoint for the locale
        if (forceHighPriority)
            return add(new ExtendedEndpoint(e.getAddress(), 
                                            e.getPort(),
                                            locale),
                       GOOD_PRIORITY);
        else
            return add(new ExtendedEndpoint(e.getAddress(),
                                            e.getPort(),
                                            locale), 
                       NORMAL_PRIORITY);
    }

    /**
     * Adds the specified host to the host catcher with the specified priority.
     * 
     * @param host the endpoint to add
     * @param priority the priority of the endpoint
     * @return <tt>true</tt> if the endpoint was added, otherwise <tt>false</tt>
     */
    public boolean add(Endpoint host, int priority) {
        //need ExtendedEndpoint for the locale
        LOG.trace("adding host");
        return add(new ExtendedEndpoint(host.getAddress(), 
                                        host.getPort()), 
                   priority);
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
     *  (ultrapeer) or NORMAL_PRIORITY
     * @param uptime the host's uptime (or our best guess)
     *
     * @return true iff e was actually added 
     */
    private boolean add(ExtendedEndpoint e, int priority) {
        repOk();
        if(!isValidHost(e)) return false;
        
        //Add to permanent list, regardless of whether it's actually in queue.
        //Note that this modifies e.
        addPermanent(e);

        boolean ret = false;
        synchronized(this) {
            if (! (ENDPOINT_SET.contains(e))) {
                ret=true;
                //Add to temporary list. Adding e may eject an older point from
                //queue, so we have to cleanup the set to maintain
                //rep. invariant.
                ENDPOINT_SET.add(e);
                Object ejected=ENDPOINT_QUEUE.insert(e, priority);
                if (ejected!=null) {
                    ENDPOINT_SET.remove(ejected);
                }         
                
                this.notify();
            }
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
        addToLocaleMap(e); //add e to locale mapping 
        if (NetworkUtils.isPrivateAddress(e.getInetAddress()))
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

    /**
     * Utility method for verifying that the given host is a valid host to add
     * to the group of hosts to try.  This verifies that the host does not have
     * a private address, is not banned, is not this node, is not in the
     * expired or probated hosts set, etc.
     * 
     * @param host the host to check
     * @return <tt>true</tt> if the host is valid and can be added, otherwise
     *  <tt>false</tt>
     */
    private boolean isValidHost(ExtendedEndpoint host) {
        if(host.isPrivateAddress()) return false;
        //We used to check that we're not connected to e, but now we do that in
        //ConnectionFetcher after a call to getAnEndpoint.  This is not a big
        //deal, since the call to "set.contains(e)" below ensures no duplicates.
        //Skip if this would connect us to our listening port.  TODO: I think
        //this check is too strict sometimes, which makes testing difficult.
        if (NetworkUtils.isMe(host.getAddress(), host.getPort()))
            return false;

        //Skip if this host is banned.
        if (RouterService.getAcceptor().isBannedIP(host.getAddress()))
            return false;  
        
        synchronized(this) {
            // Don't add this host if it has previously failed.
            if(EXPIRED_HOSTS.contains(host)) {
                return false;
            }
            
            // Don't add this host if it has previously rejected us.
            if(PROBATION_HOSTS.contains(host)) {
                return false;
            }
        }
        
        return true;
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
            
            //If we've completely run out of hosts, asynchronously contact a
            //GWebCache server to get more addresses.  Note, however, that this
            //will not do anything if we're currently connecting to a GWebCache.
            //TODO: do we need rate-limiting code?
            if(getNumHosts()==0 || 
               (!RouterService.isConnected() && _failures>200 && !_hitCaches)) {
                LOG.debug("getNumHosts() == 0, fetching endpoints");
                _hitCaches = true;
                gWebCache.fetchEndpointsAsync();
            }
            //If there are no good, fresh ultrapeer pongs--these exclude
            //gnutella.net entries--schedule a fetch in GWEBCACHE_DELAY
            //milliseconds if it's still needed then.
            else if (getNumUltrapeerHosts()==0) {
                LOG.debug("getNumUltrapeerHosts() == 0");
                long now=System.currentTimeMillis();
                //Be patient; maybe some gnutella.net entries will work.
                if (now < nextAllowedFetchTime) {
                    nextAllowedFetchTime=Math.min(
                        nextAllowedFetchTime, now+GWEBCACHE_DELAY);
                    if(LOG.isDebugEnabled())
                        LOG.debug("delaying fetch time till " + 
                                  nextAllowedFetchTime);
                } 
                //Give up and use GWebCache.
                else {
                    LOG.debug("fetching more endpoints");
                    gWebCache.fetchEndpointsAsync();
                    nextAllowedFetchTime=Long.MAX_VALUE;
                }
            }

            try { 
                // note : if this succeeds with an endpoint, it
                // will return it.  otherwise, it will throw
                // the exception, causing us to fall down to the wait.
                // the wait will be notified to stop when something
                // is added to the queue
                //  (presumably from fetchEndpointsAsync working)               
                
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
        if (success) {
            ee.recordConnectionSuccess();
        } else {
            _failures++;
            ee.recordConnectionFailure();
        }
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
        //LOG.trace("entered getAnEndpointInternal");
        // If we're already an ultrapeer and we know about hosts with free
        // ultrapeer slots, try them.
        if(RouterService.isSupernode() && !FREE_ULTRAPEER_SLOTS_SET.isEmpty()) {
            return preferenceWithLocale(FREE_ULTRAPEER_SLOTS_SET);
                                    
        } 
        // Otherwise, if we're already a leaf and we know about ultrapeers with
        // free leaf slots, try those.
        else if(RouterService.isShieldedLeaf() && 
                !FREE_LEAF_SLOTS_SET.isEmpty()) {
            return preferenceWithLocale(FREE_LEAF_SLOTS_SET);
        } 
        // Otherwise, assume we'll be a leaf and we're trying to connect, since
        // this is more common than wanting to become an ultrapeer and because
        // we want to fill any remaining leaf slots if we can.
        else if(!FREE_ULTRAPEER_SLOTS_SET.isEmpty()) {
            return preferenceWithLocale(FREE_ULTRAPEER_SLOTS_SET);
        } 
        if (! ENDPOINT_QUEUE.isEmpty()) {
            //pop e from queue and remove from set.
            ExtendedEndpoint e=(ExtendedEndpoint)ENDPOINT_QUEUE.extractMax();
            boolean ok=ENDPOINT_SET.remove(e);
            
            //check that e actually was in set.
            Assert.that(ok, "Rep. invariant for HostCatcher broken.");
            return e;
        } else
            throw new NoSuchElementException();
    }

    
    /**
     * tries to return an endpoint that matches the locale of this client
     * from the passed in set.
     */
    private ExtendedEndpoint preferenceWithLocale(Set s) {

        String loc = ApplicationSettings.LANGUAGE.getValue();

        if(LOCALE_2_SET.containsKey(loc)) {
            Set locales = (Set)LOCALE_2_SET.get(loc);
            Set retain = new HashSet(s);
            retain.retainAll(locales);

            if(retain.size() != 0) { //preferenced 
                Iterator itr = retain.iterator();
                ExtendedEndpoint ee = (ExtendedEndpoint)itr.next();
                locales.remove(ee);
                s.remove(ee);
                return ee;
            }//else we just return the first endpoint in the passed in set
        }
        
        Iterator iter = s.iterator();
        ExtendedEndpoint ee = (ExtendedEndpoint)iter.next();
        iter.remove();
        return ee;
    }

    /**
     * Accessor for the total number of hosts stored, including Ultrapeers and
     * leaves.
     * 
     * @return the total number of hosts stored 
     */
    public int getNumHosts() {
        return ENDPOINT_QUEUE.size()+FREE_LEAF_SLOTS_SET.size()+
            FREE_ULTRAPEER_SLOTS_SET.size();
    }

    /**
     * Returns the number of marked ultrapeer hosts.
     */
    public int getNumUltrapeerHosts() {
        return ENDPOINT_QUEUE.size(GOOD_PRIORITY)+FREE_LEAF_SLOTS_SET.size()+
            FREE_ULTRAPEER_SLOTS_SET.size();
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
     * Accessor for the <tt>Collection</tt> of 10 Ultrapeers that have 
     * advertised free Ultrapeer slots.  The returned <tt>Collection</tt> is a 
     * new <tt>Collection</tt> and can therefore be modified in any way.
     * 
     * @return a <tt>Collection</tt> containing 10 <tt>IpPort</tt> hosts that 
     *  have advertised they have free ultrapeer slots
     */
    public synchronized Collection getUltrapeersWithFreeUltrapeerSlots() {
        return getPreferencedCollection(FREE_ULTRAPEER_SLOTS_SET,
                                        ApplicationSettings.LANGUAGE.getValue());
    }

    public synchronized Collection 
        getUltrapeersWithFreeUltrapeerSlots(String locale) {
        return getPreferencedCollection(FREE_ULTRAPEER_SLOTS_SET,
                                        locale);
    }
    

    /**
     * Accessor for the <tt>Collection</tt> of 10 Ultrapeers that have 
     * advertised free leaf slots.  The returned <tt>Collection</tt> is a 
     * new <tt>Collection</tt> and can therefore be modified in any way.
     * 
     * @return a <tt>Collection</tt> containing 10 <tt>IpPort</tt> hosts that 
     *  have advertised they have free leaf slots
     */
    public synchronized Collection getUltrapeersWithFreeLeafSlots() {
        return getPreferencedCollection(FREE_LEAF_SLOTS_SET,
                                        ApplicationSettings.LANGUAGE.getValue());
    }
    
    public synchronized Collection
        getUltrapeersWithFreeLeafSlots(String locale) {
        return getPreferencedCollection(FREE_LEAF_SLOTS_SET,
                                        locale);
    }

    /**
     * preference the set so we try to return those endpoints that match
     * passed in locale "loc"
     */
    private Collection getPreferencedCollection(Set s, String loc) {
        if(loc == null || loc.equals(""))
            loc = ApplicationSettings.DEFAULT_LOCALE.getValue();
        int i = 0;
        Set returnSet = new HashSet();
        Set copy;
        Iterator itr;

        if(LOCALE_2_SET.containsKey(loc)) { //try to preference
            Set locales = (Set)LOCALE_2_SET.get(loc);
            copy = new HashSet(s);
            copy.retainAll(locales);
            itr = copy.iterator();
            for(;itr.hasNext() && i < 10; i++) 
                returnSet.add(itr.next());
        }

        if(i < 10) {
            copy = new HashSet(s);
            copy.removeAll(returnSet); //make sure we don't have duplicates
            itr = copy.iterator();
            for(;itr.hasNext() && i < 10; i++)
                returnSet.add(itr.next());
        }
        
        return returnSet;
    }


    /**
     * Notifies this that connect() has been called.  This may decide to give
     * out bootstrap pongs if necessary.
     */
    public synchronized void expire() {
        //Fetch more GWebCache urls once per session.
        //(Well, once per connect really--good enough.)
        gWebCache.fetchBootstrapServersAsync();
    }

    /**
     * @modifies this
     * @effects removes all entries from this
     */
    public synchronized void clear() {
        FREE_LEAF_SLOTS_SET.clear();
        FREE_ULTRAPEER_SLOTS_SET.clear();
        ENDPOINT_QUEUE.clear();
        ENDPOINT_SET.clear();
    }

    public String toString() {
        return "[volatile:"+ENDPOINT_QUEUE.toString()
               +", permanent:"+permanentHosts.toString()+"]";
    }

    /** Enable very slow rep checking?  Package access for use by
     *  HostCatcherTest. */
    static boolean DEBUG=false;

    
    /** Checks invariants. Very slow; method body should be enabled for testing
     *  purposes only. */
    protected void repOk() {
        if (!DEBUG)
            return;

        synchronized(this) {
            //Check ENDPOINT_SET == ENDPOINT_QUEUE
            outer:
            for (Iterator iter=ENDPOINT_SET.iterator(); iter.hasNext(); ) {
                Object e=iter.next();
                for (Iterator iter2=ENDPOINT_QUEUE.iterator(); 
                     iter2.hasNext();) {
                    if (e.equals(iter2.next()))
                        continue outer;
                }
                Assert.that(false, "Couldn't find "+e+" in queue");
            }
            for (Iterator iter=ENDPOINT_QUEUE.iterator(); iter.hasNext(); ) {
                Object e=iter.next();
                Assert.that(e instanceof ExtendedEndpoint);
                Assert.that(ENDPOINT_SET.contains(e));
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
    }
    
    /**
     * Reads the gnutella.net file.
     */
    private void readHostsFile() {
        // Just gnutella.net
        try {
            read(HOST_FILE);
        } catch (FileNotFoundException e) {
            LOG.error(HOST_FILE, e);
        } catch (IOException e) {
            LOG.error(HOST_FILE, e);
        }    
    }

    /**
     * Recovers any hosts that we have put in the set of hosts "pending" 
     * removal from our hosts list.
     */
    public synchronized void recoverHosts() {
        LOG.debug("recovering hosts file");
        
        PROBATION_HOSTS.clear();
        EXPIRED_HOSTS.clear();
        _hitCaches = false;
        _failures = 0;
        
        // Read the hosts file again.  This will also notify any waiting 
        // connection fetchers from previous connection attempts.
        readHostsFile();
    }

    /**
     * Adds the specified host to the group of hosts currently on "probation."
     * These are hosts that are on the network but that have rejected a 
     * connection attempt.  They will periodically be re-activated as needed.
     * 
     * @param host the <tt>Endpoint</tt> to put on probation
     */
    public synchronized void putHostOnProbation(Endpoint host) {
        PROBATION_HOSTS.add(host);
        if(PROBATION_HOSTS.size() > PROBATION_HOSTS_SIZE) {
            PROBATION_HOSTS.remove(PROBATION_HOSTS.iterator().next());
        }
    }
    
    /**
     * Adds the specified host to the group of expired hosts.  These are hosts
     * that we have been unable to create a TCP connection to, let alone a 
     * Gnutella connection.
     * 
     * @param host the <tt>Endpoint</tt> to expire
     */
    public synchronized void expireHost(Endpoint host) {
        EXPIRED_HOSTS.add(host);
        if(EXPIRED_HOSTS.size() > EXPIRED_HOSTS_SIZE) {
            EXPIRED_HOSTS.remove(EXPIRED_HOSTS.iterator().next());
        }       
    }

    /**
     * Adds the specified host to the group of hosts to try.
     * 
     * @param host the <tt>IpPort</tt> for the new host
     * @throws NullPointerException if the <tt>host</tt> argument is 
     *  <tt>null</tt>
     */
    public void addHost(IpPort host) {
        if(host == null) {
            throw new NullPointerException("null host");
        }
        if(host instanceof PingReply) {
            add((PingReply)host);
        }
    }
    
    //Unit test: tests/com/.../gnutella/HostCatcherTest.java   
    //           tests/com/.../gnutella/bootstrap/HostCatcherFetchTest.java
    //           
}
