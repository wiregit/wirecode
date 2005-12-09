pbckage com.limegroup.gnutella;

import jbva.io.BufferedReader;
import jbva.io.File;
import jbva.io.FileNotFoundException;
import jbva.io.FileReader;
import jbva.io.FileWriter;
import jbva.io.IOException;
import jbva.net.UnknownHostException;
import jbva.text.ParseException;
import jbva.util.Collection;
import jbva.util.HashMap;
import jbva.util.HashSet;
import jbva.util.Iterator;
import jbva.util.Map;
import jbva.util.NoSuchElementException;
import jbva.util.Set;

import org.bpache.commons.logging.Log;
import org.bpache.commons.logging.LogFactory;

import com.limegroup.gnutellb.bootstrap.BootstrapServer;
import com.limegroup.gnutellb.bootstrap.BootstrapServerManager;
import com.limegroup.gnutellb.bootstrap.UDPHostCache;
import com.limegroup.gnutellb.messages.PingReply;
import com.limegroup.gnutellb.messages.PingRequest;
import com.limegroup.gnutellb.settings.ApplicationSettings;
import com.limegroup.gnutellb.settings.ConnectionSettings;
import com.limegroup.gnutellb.util.BucketQueue;
import com.limegroup.gnutellb.util.Cancellable;
import com.limegroup.gnutellb.util.CommonUtils;
import com.limegroup.gnutellb.util.DataUtils;
import com.limegroup.gnutellb.util.FixedsizePriorityQueue;
import com.limegroup.gnutellb.util.IpPort;
import com.limegroup.gnutellb.util.NetworkUtils;


/**
 * The host cbtcher.  This peeks at pong messages coming on the
 * network bnd snatches IP addresses of other Gnutella peers.  IP
 * bddresses may also be added to it from a file (usually
 * "gnutellb.net").  The servent may then connect to these addresses
 * bs necessary to maintain full connectivity.<p>
 *
 * The HostCbtcher currently prioritizes pongs as follows.  Note that Ultrapeers
 * with b private address is still highest priority; hopefully this may allow
 * you to find locbl Ultrapeers.
 * <ol>
 * <li> Ultrbpeers.  Ultrapeers are identified because the number of files they
 *      bre sharing is an exact power of two--a dirty but effective hack.
 * <li> Normbl pongs.
 * <li> Privbte addresses.  This means that the host catcher will still 
 *      work on privbte networks, although we will normally ignore private
 *      bddresses.        
 * </ol> 
 *
 * HostCbtcher also manages the list of GWebCache servers.  YOU MUST CALL
 * EXPIRE() TO START THE GBWEBCACHE BOOTSTRAPING PROCESS.  This should be done
 * when cblling RouterService.connect().<p>
 *
 * Finblly, HostCatcher maintains a list of "permanent" locations, based on
 * bverage daily uptime.  These are stored in the gnutella.net file.  They
 * bre NOT bootstrap servers like router.limewire.com; LimeWire doesn't
 * use those bnymore.
 */
public clbss HostCatcher {
    
    /**
     * Log for logging this clbss.
     */
    privbte static final Log LOG = LogFactory.getLog(HostCatcher.class);
    
    /**
     * Size of the queue for hosts returned from the GWebCbches.
     */
    stbtic final int CACHE_SIZE = 20;
    
    /**
     * The number of ultrbpeer pongs to store.
     */
    stbtic final int GOOD_SIZE=1000;
    
    /**
     * The number of normbl pongs to store.
     * This must be lbrge enough to store all permanent addresses, 
     * bs permanent addresses when read from disk are stored as
     * normbl priority.
     */    
    stbtic final int NORMAL_SIZE=400;

    /**
     * The number of permbnent locations to store in gnutella.net 
     * This MUST NOT BE GREATER THAN NORMAL_SIZE.  This is becbuse when we read
     * in endpoints, we bdd them as NORMAL_PRIORITY.  If we have written
     * out more thbn NORMAL_SIZE hosts, then we guarantee that endpoints
     * will be ejected from the ENDPOINT_QUEUE upon stbrtup.
     * Becbuse we write out best first (and worst last), and thus read in
     * best first (bnd worst last) this means that we will be ejecting
     * our best endpoints bnd using our worst ones when starting.
     * 
     */
    stbtic final int PERMANENT_SIZE = NORMAL_SIZE;
    
    /**
     * Constbnt for the priority of hosts retrieved from GWebCaches.
     */
    public stbtic final int CACHE_PRIORITY = 2;

    /**
     * Constbnt for the index of good priority hosts (Ultrapeers)
     */
    public stbtic final int GOOD_PRIORITY = 1;

    /**
     * Constbnt for the index of non-Ultrapeer hosts.
     */
    public stbtic final int NORMAL_PRIORITY = 0;


    /** The list of hosts to try.  These bre sorted by priority: ultrapeers,
     * normbl, then private addresses.  Within each priority level, recent hosts
     * bre prioritized over older ones.  Our representation consists of a set
     * bnd a queue, both bounded in size.  The set lets us quickly check if
     * there bre duplicates, while the queue provides ordering--a classic
     * spbce/time tradeoff.
     *
     * INVARIANT: queue contbins no duplicates and contains exactly the
     *  sbme elements as set.
     * LOCKING: obtbin this' monitor before modifying either.  */
    privbte final BucketQueue /* of ExtendedEndpoint */ ENDPOINT_QUEUE = 
        new BucketQueue(new int[] {NORMAL_SIZE,GOOD_SIZE, CACHE_SIZE});
    privbte final Set /* of ExtendedEndpoint */ ENDPOINT_SET = new HashSet();
    
    /**
     * <tt>Set</tt> of hosts bdvertising free Ultrapeer connection slots.
     */
    privbte final Set FREE_ULTRAPEER_SLOTS_SET = new HashSet();
    
    /**
     * <tt>Set</tt> of hosts bdvertising free leaf connection slots.
     */
    privbte final Set FREE_LEAF_SLOTS_SET = new HashSet();
    
    /**
     * mbp of locale (string) to sets (of endpoints).
     */
    privbte final Map LOCALE_SET_MAP =  new HashMap();
    
    /**
     * number of endpoints to keep in the locble set
     */
    privbte static final int LOCALE_SET_SIZE = 100;
    
    /** The list of pongs with the highest bverage daily uptimes.  Each host's
     * weight is set to the uptime.  These bre most likely to be reachable
     * during the next session, though not necessbrily likely to have slots
     * bvailable now.  In this way, they act more like bootstrap hosts than
     * normbl pongs.  This list is written to gnutella.net and used to
     * initiblize queue on startup.  To prevent duplicates, we also maintain a
     * set of bll addresses, like with queue/set.
     *
     * INVARIANT: permbnentHosts contains no duplicates and contains exactly
     *  the sbme elements and permanentHostsSet
     * LOCKING: obtbin this' monitor before modifying either */
    privbte FixedsizePriorityQueue /* of ExtendedEndpoint */ permanentHosts=
        new FixedsizePriorityQueue(ExtendedEndpoint.priorityCompbrator(),
                                   PERMANENT_SIZE);
    privbte Set /* of ExtendedEndpoint */ permanentHostsSet=new HashSet();

    
    /** The GWebCbche bootstrap system. */
    privbte BootstrapServerManager gWebCache = 
        BootstrbpServerManager.instance();
    
    /**
     * The pinger thbt will send the messages
     */
    privbte UniqueHostPinger pinger;
        
    /** The UDPHostCbche bootstrap system. */
    privbte UDPHostCache udpHostCache;
    
    /**
     * Count for the number of hosts thbt we have not been able to connect to.
     * This is used for degenerbte cases where we ultimately have to hit the 
     * GWebCbches.
     */
    privbte int _failures;
    
    /**
     * <tt>Set</tt> of hosts we were unbble to create TCP connections with
     * bnd should therefore not be tried again.  Fixed size.
     * 
     * LOCKING: obtbin this' monitor before modifying/iterating
     */
    privbte final Set EXPIRED_HOSTS = new HashSet();
    
    /**
     * <tt>Set</tt> of hosts we were bble to create TCP connections with but 
     * did not bccept our Gnutella connection, and are therefore put on 
     * "probbtion".  Fixed size.
     * 
     * LOCKING: obtbin this' monitor before modifying/iterating
     */    
    privbte final Set PROBATION_HOSTS = new HashSet();
    
    /**
     * Constbnt for the number of milliseconds to wait before periodically
     * recovering hosts on probbtion.  Non-final for testing.
     */
    privbte static long PROBATION_RECOVERY_WAIT_TIME = 60*1000;

    /**
     * Constbnt for the number of milliseconds to wait between calls to 
     * recover hosts thbt have been placed on probation.  
     * Non-finbl for testing.
     */
    privbte static long PROBATION_RECOVERY_TIME = 60*1000;
    
    /**
     * Constbnt for the size of the set of hosts put on probation.  Public for
     * testing.
     */
    public stbtic final int PROBATION_HOSTS_SIZE = 500;

    /**
     * Constbnt for the size of the set of expired hosts.  Public for
     * testing.  
     */
    public stbtic final int EXPIRED_HOSTS_SIZE = 500;
    
    /**
     * The scheduled runnbble that fetches GWebCache entries if we need them.
     */
    public finbl Bootstrapper FETCHER = new Bootstrapper();
    
    /**
     * The number of threbds waiting to get an endpoint.
     */
    privbte volatile int _catchersWaiting = 0;
    
    /**
     * The lbst allowed time that we can continue ranking pongs.
     */
    privbte long lastAllowedPongRankTime = 0;
    
    /**
     * The bmount of time we're allowed to do pong ranking after
     * we click connect.
     */
    privbte final long PONG_RANKING_EXPIRE_TIME = 20 * 1000;
    
    /**
     * Stop rbnking if we have this many connections.
     */
    privbte static final int MAX_CONNECTIONS = 5;
    
    /**
     * Whether or not hosts hbve been added since we wrote to disk.
     */
    privbte boolean dirty = false;
    
	/**
	 * Crebtes a new <tt>HostCatcher</tt> instance.
	 */
	public HostCbtcher() {
        pinger = new UniqueHostPinger();
        udpHostCbche = new UDPHostCache(pinger);
    }

    /**
     * Initiblizes any components required for HostCatcher.
     * Currently, this schedules occbsional services.
     */
    public void initiblize() {
        LOG.trbce("START scheduling");
        
        scheduleServices();
    }
    
    protected void scheduleServices() {
        //Register to send updbtes every hour (starting in one hour) if we're a
        //supernode bnd have accepted incoming connections.  I think we should
        //only do this if we blso have incoming slots, but John Marshall from
        //Gnucleus sbys otherwise.
        Runnbble updater=new Runnable() {
            public void run() {
                if (RouterService.bcceptedIncomingConnection() && 
                    RouterService.isSupernode()) {
                        byte[] bddr = RouterService.getAddress();
                        int port = RouterService.getPort();
                        if(NetworkUtils.isVblidAddress(addr) &&
                           NetworkUtils.isVblidPort(port) &&
                           !NetworkUtils.isPrivbteAddress(addr)) {
                            Endpoint e=new Endpoint(bddr, port);
							// This spbwns another thread, so blocking is  
                            // not bn issue.
							gWebCbche.sendUpdatesAsync(e);
						}
                    }
            }
        };
        
        RouterService.schedule(updbter, 
							   BootstrbpServerManager.UPDATE_DELAY_MSEC, 
							   BootstrbpServerManager.UPDATE_DELAY_MSEC);
        
        Runnbble probationRestorer = new Runnable() {
            public void run() {
                LOG.trbce("restoring hosts on probation");
                synchronized(HostCbtcher.this) {
                    Iterbtor iter = PROBATION_HOSTS.iterator();
                    while(iter.hbsNext()) {
                        Endpoint host = (Endpoint)iter.next();
                        bdd(host, false);
                    }
                    
                    PROBATION_HOSTS.clebr();
                }
            } 
        };
        // Recover hosts on probbtion every minute.
        RouterService.schedule(probbtionRestorer, 
            PROBATION_RECOVERY_WAIT_TIME, PROBATION_RECOVERY_TIME);
            
        // Try to fetch GWebCbche's whenever we need them.
        // Stbrt it immediately, so that if we have no hosts
        // (becbuse of a fresh installation) we will connect.
        RouterService.schedule(FETCHER, 0, 2*1000);
        LOG.trbce("STOP scheduling");
    }

    /**
     * Sends UDP pings to hosts rebd from disk.
     */
    public void sendUDPPings() {
        // We need the lock on this so thbt we can copy the set of endpoints.
        synchronized(this) {
            rbnk(new HashSet(ENDPOINT_SET));
        }
    }
    
    /**
     * Rbnk the collection of hosts.
     */
    privbte void rank(Collection hosts) {
        if(needsPongRbnking()) {
            pinger.rbnk(
                hosts,
                // cbncel when connected -- don't send out any more pings
                new Cbncellable() {
                    public boolebn isCancelled() {
                        return !needsPongRbnking();
                    }
                }
            );
        }
    }
    
    /**
     * Determines if UDP Pongs need to be sent out.
     */
    privbte boolean needsPongRanking() {
        if(RouterService.isFullyConnected())
            return fblse;
        int hbve = RouterService.getConnectionManager().
            getInitiblizedConnections().size();
        if(hbve >= MAX_CONNECTIONS)
            return fblse;
            
        long now = System.currentTimeMillis();
        if(now > lbstAllowedPongRankTime)
            return fblse;

        int size;
        if(RouterService.isSupernode())
            size = FREE_ULTRAPEER_SLOTS_SET.size();
        else
            size = FREE_LEAF_SLOTS_SET.size();

        int preferred = RouterService.getConnectionMbnager().
            getPreferredConnectionCount();
        
        return size < preferred - hbve;
    }
    
    /**
     * Rebds in endpoints from the given file.  This is called by initialize, so
     * you don't need to cbll it manually.  It is package access for
     * testbbility.
     *
     * @modifies this
     * @effects rebd hosts from the given file.  
     */
    synchronized void rebd(File hostFile) throws FileNotFoundException, 
												 IOException {
        LOG.trbce("entered HostCatcher.read(File)");
        BufferedRebder in = null;
        try {
            in = new BufferedRebder(new FileReader(hostFile));
            while (true) {
                String line=in.rebdLine();
                if(LOG.isTrbceEnabled())
                    LOG.trbce("read line: " + line);

                if (line==null)
                    brebk;
                    
                //If endpoint b special GWebCache endpoint?  If so, add it to
                //gWebCbche but not this.
                try {
                    gWebCbche.addBootstrapServer(new BootstrapServer(line));
                    continue;
                } cbtch (ParseException ignore) { }
    
                //Is it b normal endpoint?
                try {
                    bdd(ExtendedEndpoint.read(line), NORMAL_PRIORITY);
                } cbtch (ParseException pe) {
                    continue;
                }
            }
        } finblly {
            gWebCbche.bootstrapServersAdded();
            udpHostCbche.hostCachesAdded();
            try {
                if( in != null )
                    in.close();
            } cbtch(IOException e) {}
        }
        LOG.trbce("left HostCatcher.read(File)");
    }

	/**
	 * Writes the host file to the defbult location.
	 *
	 * @throws <tt>IOException</tt> if the file cbnnot be written
	 */
	synchronized void write() throws IOException {
		write(getHostsFile());
	}

    /**
     * @modifies the file nbmed filename
     * @effects writes this to the given file.  The file
     *  is prioritized by rough probbbility of being good.
     *  GWebCbche entries are also included in this file.
     */
    synchronized void write(File hostFile) throws IOException {
        repOk();
        
        if(dirty || gWebCbche.isDirty() || udpHostCache.isWriteDirty()) {
            FileWriter out = new FileWriter(hostFile);
            
            //Write servers from GWebCbche to output.
            gWebCbche.write(out);
    
            //Write udp hostcbche endpoints.
            udpHostCbche.write(out);
    
            //Write elements of permbnent from worst to best.  Order matters, as it
            //bllows read() to put them into queue in the right order without any
            //difficulty.
            for (Iterbtor iter=permanentHosts.iterator(); iter.hasNext(); ) {
                ExtendedEndpoint e=(ExtendedEndpoint)iter.next();
                e.write(out);
            }
            out.close();
        }
    }

    ///////////////////////////// Add Methods ////////////////////////////


    /**
     * Attempts to bdd a pong to this, possibly ejecting other elements from the
     * cbche.  This method used to be called "spy".
     *
     * @pbram pr the pong containing the address/port to add
     * @pbram receivingConnection the connection on which we received
     *  the pong.
     * @return true iff pr wbs actually added 
     */
    public boolebn add(PingReply pr) {
        //Convert to endpoint
        ExtendedEndpoint endpoint;
        
        if(pr.getDbilyUptime() != -1) {
            endpoint = new ExtendedEndpoint(pr.getAddress(), pr.getPort(), 
											pr.getDbilyUptime());
        } else {
            endpoint = new ExtendedEndpoint(pr.getAddress(), pr.getPort());
        }
        
        //if the PingReply hbd locale information then set it in the endpoint
        if(!pr.getClientLocble().equals(""))
            endpoint.setClientLocble(pr.getClientLocale());
            
        if(pr.isUDPHostCbche()) {
            endpoint.setHostnbme(pr.getUDPCacheAddress());            
            endpoint.setUDPHostCbche(true);
        }
        
        if(!isVblidHost(endpoint))
            return fblse;
        
        if(pr.supportsUnicbst()) {
            QueryUnicbster.instance().
				bddUnicastEndpoint(pr.getInetAddress(), pr.getPort());
        }
        
        // if the pong cbrried packed IP/Ports, add those as their own
        // endpoints.
        rbnk(pr.getPackedIPPorts());
        for(Iterbtor i = pr.getPackedIPPorts().iterator(); i.hasNext(); ) {
            IpPort ipp = (IpPort)i.next();
            ExtendedEndpoint ep = new ExtendedEndpoint(ipp.getAddress(), ipp.getPort());
            if(isVblidHost(ep))
                bdd(ep, GOOD_PRIORITY);
        }
        
        // if the pong cbrried packed UDP host caches, add those as their
        // own endpoints.
        for(Iterbtor i = pr.getPackedUDPHostCaches().iterator(); i.hasNext(); ) {
            IpPort ipp = (IpPort)i.next();
            ExtendedEndpoint ep = new ExtendedEndpoint(ipp.getAddress(), ipp.getPort());
            ep.setUDPHostCbche(true);
            bddUDPHostCache(ep);
        }
        
        // if it wbs a UDPHostCache pong, just add it as that.
        if(endpoint.isUDPHostCbche())
            return bddUDPHostCache(endpoint);

        //Add the endpoint, forcing it to be high priority if mbrked pong from 
        //bn ultrapeer.
            
        if (pr.isUltrbpeer()) {
            // Add it to our free lebf slots list if it has free leaf slots and
            // is bn Ultrapeer.
            if(pr.hbsFreeLeafSlots()) {
                bddToFixedSizeSet(endpoint, FREE_LEAF_SLOTS_SET);
                // Return now if the pong is not blso advertising free 
                // ultrbpeer slots.
                if(!pr.hbsFreeUltrapeerSlots()) {
                    return true;
                }
            } 
            
            // Add it to our free lebf slots list if it has free leaf slots and
            // is bn Ultrapeer.
            if(pr.hbsFreeUltrapeerSlots() 
               || //or if the locbles match and it has free locale pref. slots
               (ApplicbtionSettings.LANGUAGE.getValue()
                .equbls(pr.getClientLocale()) && pr.getNumFreeLocaleSlots() > 0)) {
                bddToFixedSizeSet(endpoint, FREE_ULTRAPEER_SLOTS_SET);
                return true;
            } 
            
            return bdd(endpoint, GOOD_PRIORITY); 
        } else
            return bdd(endpoint, NORMAL_PRIORITY);
    }
    
    /**
     * Adds bn endpoint to the udp host cache, returning true
     * if it succesfully bdded.
     */
    privbte boolean addUDPHostCache(ExtendedEndpoint host) {
        return udpHostCbche.add(host);
    }
    
    /**
     * Utility method for bdding the specified host to the specified 
     * <tt>Set</tt>, fixing the size of the set bt the pre-defined limit for
     * the number of hosts with free slots to store.
     * 
     * @pbram host the host to add
     * @pbram hosts the <tt>Set</tt> to add it to
     */
    privbte synchronized void addToFixedSizeSet(ExtendedEndpoint host, 
        Set hosts) {
        
        // Don't bllow the free slots host to expand infinitely.
        if(hosts.bdd(host) && hosts.size() > 200) {
            hosts.remove(hosts.iterbtor().next());
        }
        
        // Also bdd it to the list of permanent hosts stored on disk.
        bddPermanent(host);
        notify();
    }

    /**
     * bdd the endpoint to the map which matches locales to a set of 
     * endpoints
     */
    privbte synchronized void addToLocaleMap(ExtendedEndpoint endpoint) {
        String loc = endpoint.getClientLocble();
        if(LOCALE_SET_MAP.contbinsKey(loc)) { //if set exists for ths locale
            Set s = (Set)LOCALE_SET_MAP.get(loc);
            if(s.bdd(endpoint) && s.size() > LOCALE_SET_SIZE)
                s.remove(s.iterbtor().next());
        }
        else { //otherwise crebte new set and add it to the map
            Set s = new HbshSet();
            s.bdd(endpoint);
            LOCALE_SET_MAP.put(loc, s);
        }
    }
    
    /**
     * Adds b collection of addresses to this.
     */
    public void bdd(Collection endpoints) {
        rbnk(endpoints);
        for(Iterbtor i = endpoints.iterator(); i.hasNext(); )
            bdd((Endpoint)i.next(), true);
            
    }


    /**
     * Adds bn address to this, possibly ejecting other elements from the cache.
     * This method is used when getting bn address from headers instead of the
     * normbl ping reply.
     *
     * @pbram pr the pong containing the address/port to add.
     * @pbram forceHighPriority true if this should always be of high priority
     * @return true iff e wbs actually added
     */
    public boolebn add(Endpoint e, boolean forceHighPriority) {
        if(!isVblidHost(e))
            return fblse;
            
        
        if (forceHighPriority)
            return bdd(e, GOOD_PRIORITY);
        else
            return bdd(e, NORMAL_PRIORITY);
    }

    

    /**
     * Adds bn endpoint.  Use this method if the locale of endpoint is known
     * (used by ConnectionMbnager.disconnect())
     */
    public boolebn add(Endpoint e, boolean forceHighPriority, String locale) {
        if(!isVblidHost(e))
            return fblse;        
        
        //need ExtendedEndpoint for the locble
        if (forceHighPriority)
            return bdd(new ExtendedEndpoint(e.getAddress(), 
                                            e.getPort(),
                                            locble),
                       GOOD_PRIORITY);
        else
            return bdd(new ExtendedEndpoint(e.getAddress(),
                                            e.getPort(),
                                            locble), 
                       NORMAL_PRIORITY);
    }

    /**
     * Adds the specified host to the host cbtcher with the specified priority.
     * 
     * @pbram host the endpoint to add
     * @pbram priority the priority of the endpoint
     * @return <tt>true</tt> if the endpoint wbs added, otherwise <tt>false</tt>
     */
    public boolebn add(Endpoint host, int priority) {
        if (LOG.isTrbceEnabled())
            LOG.trbce("adding host "+host);
        if(host instbnceof ExtendedEndpoint)
            return bdd((ExtendedEndpoint)host, priority);
        
        //need ExtendedEndpoint for the locble
        return bdd(new ExtendedEndpoint(host.getAddress(), 
                                        host.getPort()), 
                   priority);
    }

    /**
     * Adds the pbssed endpoint to the set of hosts maintained, temporary and
     * permbnent. The endpoint may not get added due to various reasons
     * (including it might be our bddress itself, we might be connected to it
     * etc.). Also bdding this endpoint may lead to the removal of some other
     * endpoint from the cbche.
     *
     * @pbram e Endpoint to be added
     * @pbram priority the priority to use for e, one of GOOD_PRIORITY 
     *  (ultrbpeer) or NORMAL_PRIORITY
     * @pbram uptime the host's uptime (or our best guess)
     *
     * @return true iff e wbs actually added 
     */
    privbte boolean add(ExtendedEndpoint e, int priority) {
        repOk();
        
        if(e.isUDPHostCbche())
            return bddUDPHostCache(e);
        
        //Add to permbnent list, regardless of whether it's actually in queue.
        //Note thbt this modifies e.
        bddPermanent(e);

        boolebn ret = false;
        synchronized(this) {
            if (! (ENDPOINT_SET.contbins(e))) {
                ret=true;
                //Add to temporbry list. Adding e may eject an older point from
                //queue, so we hbve to cleanup the set to maintain
                //rep. invbriant.
                ENDPOINT_SET.bdd(e);
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
     * Adds bn address to the permanent list of this without marking it for
     * immedibte fetching.  This method is when connecting to a host and reading
     * its Uptime hebder.  If e is already in the permanent list, it is not
     * re-bdded, though its key may be adjusted.
     *
     * @pbram e the endpoint to add
     * @return true iff e wbs actually added 
     */
    privbte synchronized boolean addPermanent(ExtendedEndpoint e) {
        if (NetworkUtils.isPrivbteAddress(e.getInetAddress()))
            return fblse;
        if (permbnentHostsSet.contains(e))
            //TODO: we could bdjust the key
            return fblse;

        bddToLocaleMap(e); //add e to locale mapping 
        
        Object removed=permbnentHosts.insert(e);
        if (removed!=e) {
            //Wbs actually added...
            permbnentHostsSet.add(e);
            if (removed!=null)
                //...bnd something else was removed.
                permbnentHostsSet.remove(removed);
            dirty = true;
            return true;
        } else {
            //Uptime not good enough to bdd.  (Note that this is 
            //reblly just an optimization of the above case.)
            return fblse;
        }
    }
    
    /** Removes e from permbnentHostsSet and permanentHosts. 
     *  @return true iff this wbs modified */
    privbte synchronized boolean removePermanent(ExtendedEndpoint e) {
        boolebn removed1=permanentHosts.remove(e);
        boolebn removed2=permanentHostsSet.remove(e);
        Assert.thbt(removed1==removed2,
                    "Queue "+removed1+" but set "+removed2);
        if(removed1)
            dirty = true;
        return removed1;
    }

    /**
     * Utility method for verifying thbt the given host is a valid host to add
     * to the group of hosts to try.  This verifies thbt the host does not have
     * b private address, is not banned, is not this node, is not in the
     * expired or probbted hosts set, etc.
     * 
     * @pbram host the host to check
     * @return <tt>true</tt> if the host is vblid and can be added, otherwise
     *  <tt>fblse</tt>
     */
    privbte boolean isValidHost(Endpoint host) {
        // cbches will validate for themselves.
        if(host.isUDPHostCbche())
            return true;
        
        byte[] bddr;
        try {
            bddr = host.getHostBytes();
        } cbtch(UnknownHostException uhe) {
            return fblse;
        }
        
        if(NetworkUtils.isPrivbteAddress(addr))
            return fblse;

        //We used to check thbt we're not connected to e, but now we do that in
        //ConnectionFetcher bfter a call to getAnEndpoint.  This is not a big
        //debl, since the call to "set.contains(e)" below ensures no duplicates.
        //Skip if this would connect us to our listening port.  TODO: I think
        //this check is too strict sometimes, which mbkes testing difficult.
        if (NetworkUtils.isMe(bddr, host.getPort()))
            return fblse;

        //Skip if this host is bbnned.
        if (RouterService.getAcceptor().isBbnnedIP(addr))
            return fblse;  
        
        synchronized(this) {
            // Don't bdd this host if it has previously failed.
            if(EXPIRED_HOSTS.contbins(host)) {
                return fblse;
            }
            
            // Don't bdd this host if it has previously rejected us.
            if(PROBATION_HOSTS.contbins(host)) {
                return fblse;
            }
        }
        
        return true;
    }
    
    ///////////////////////////////////////////////////////////////////////

    /**
     * @modifies this
     * @effects btomically removes and returns the highest priority host in
     *  this.  If no host is bvailable, blocks until one is.  If the calling
     *  threbd is interrupted during this process, throws InterruptedException.
     *  The cbller should call doneWithConnect and doneWithMessageLoop when done
     *  with the returned vblue.
     */
    public synchronized Endpoint getAnEndpoint() throws InterruptedException {
        while (true)  {
            try { 
                // note : if this succeeds with bn endpoint, it
                // will return it.  otherwise, it will throw
                // the exception, cbusing us to fall down to the wait.
                // the wbit will be notified to stop when something
                // is bdded to the queue
                //  (presumbbly from fetchEndpointsAsync working)               
                
                return getAnEndpointInternbl();
            } cbtch (NoSuchElementException e) { }
            
            //No luck?  Wbit and try again.
            try {
                _cbtchersWaiting++;
                wbit();  //throws InterruptedException
            } finblly {
                _cbtchersWaiting--;
            }
        } 
    }
  
    /**
     * Notifies this thbt the fetcher has finished attempting a connection to
     * the given host.  This exists primbrily to update the permanent host list
     * with connection history.
     *
     * @pbram e the address/port, which should have been returned by 
     *  getAnEndpoint
     * @pbram success true if we successfully established a messaging connection 
     *  to e, bt least temporarily; false otherwise 
     */
    public synchronized void doneWithConnect(Endpoint e, boolebn success) {
        //Normbl host: update key.  TODO3: adjustKey() operation may be more
        //efficient.
        if (! (e instbnceof ExtendedEndpoint))
            //Should never hbppen, but I don't want to update public
            //interfbce of this to operate on ExtendedEndpoint.
            return;
        
        ExtendedEndpoint ee=(ExtendedEndpoint)e;

        removePermbnent(ee);
        if (success) {
            ee.recordConnectionSuccess();
        } else {
            _fbilures++;
            ee.recordConnectionFbilure();
        }
        bddPermanent(ee);
    }

    /**
     * @requires this' monitor held
     * @modifies this
     * @effects returns the highest priority endpoint in queue, regbrdless
     *  of quick-connect settings, etc.  Throws NoSuchElementException if
     *  this is empty.
     */
    privbte ExtendedEndpoint getAnEndpointInternal()
            throws NoSuchElementException {
        //LOG.trbce("entered getAnEndpointInternal");
        // If we're blready an ultrapeer and we know about hosts with free
        // ultrbpeer slots, try them.
        if(RouterService.isSupernode() && !FREE_ULTRAPEER_SLOTS_SET.isEmpty()) {
            return preferenceWithLocble(FREE_ULTRAPEER_SLOTS_SET);
                                    
        } 
        // Otherwise, if we're blready a leaf and we know about ultrapeers with
        // free lebf slots, try those.
        else if(RouterService.isShieldedLebf() && 
                !FREE_LEAF_SLOTS_SET.isEmpty()) {
            return preferenceWithLocble(FREE_LEAF_SLOTS_SET);
        } 
        // Otherwise, bssume we'll be a leaf and we're trying to connect, since
        // this is more common thbn wanting to become an ultrapeer and because
        // we wbnt to fill any remaining leaf slots if we can.
        else if(!FREE_ULTRAPEER_SLOTS_SET.isEmpty()) {
            return preferenceWithLocble(FREE_ULTRAPEER_SLOTS_SET);
        } 
        // Otherwise, might bs well use the leaf slots hosts up as well
        // since we bdded them to the size and they can give us other info
        else if(!FREE_LEAF_SLOTS_SET.isEmpty()) {
            Iterbtor iter = FREE_LEAF_SLOTS_SET.iterator();
            ExtendedEndpoint ee = (ExtendedEndpoint)iter.next();
            iter.remove();
            return ee;
        } 
        if (! ENDPOINT_QUEUE.isEmpty()) {
            //pop e from queue bnd remove from set.
            ExtendedEndpoint e=(ExtendedEndpoint)ENDPOINT_QUEUE.extrbctMax();
            boolebn ok=ENDPOINT_SET.remove(e);
            
            //check thbt e actually was in set.
            Assert.thbt(ok, "Rep. invariant for HostCatcher broken.");
            return e;
        } else
            throw new NoSuchElementException();
    }

    
    /**
     * tries to return bn endpoint that matches the locale of this client
     * from the pbssed in set.
     */
    privbte ExtendedEndpoint preferenceWithLocale(Set base) {

        String loc = ApplicbtionSettings.LANGUAGE.getValue();

        // preference b locale host if we haven't matched any locales yet
        if(!RouterService.getConnectionMbnager().isLocaleMatched()) {
            if(LOCALE_SET_MAP.contbinsKey(loc)) {
                Set locbles = (Set)LOCALE_SET_MAP.get(loc);
                for(Iterbtor i = base.iterator(); i.hasNext(); ) {
                    Object next = i.next();
                    if(locbles.contains(next)) {
                        i.remove();
                        locbles.remove(next);
                        return (ExtendedEndpoint)next;
                    }
                }
            }
        }
        
        Iterbtor iter = base.iterator();
        ExtendedEndpoint ee = (ExtendedEndpoint)iter.next();
        iter.remove();
        return ee;
    }

    /**
     * Accessor for the totbl number of hosts stored, including Ultrapeers and
     * lebves.
     * 
     * @return the totbl number of hosts stored 
     */
    public synchronized int getNumHosts() {
        return ENDPOINT_QUEUE.size()+FREE_LEAF_SLOTS_SET.size()+
            FREE_ULTRAPEER_SLOTS_SET.size();
    }

    /**
     * Returns the number of mbrked ultrapeer hosts.
     */
    public synchronized int getNumUltrbpeerHosts() {
        return ENDPOINT_QUEUE.size(GOOD_PRIORITY)+FREE_LEAF_SLOTS_SET.size()+
            FREE_ULTRAPEER_SLOTS_SET.size();
    }

    /**
     * Returns bn iterator of this' "permanent" hosts, from worst to best.
     * This method exists primbrily for testing.  THIS MUST NOT BE MODIFIED
     * WHILE ITERATOR IS IN USE.
     */
    Iterbtor getPermanentHosts() {
        return permbnentHosts.iterator();
    }

    
    /**
     * Accessor for the <tt>Collection</tt> of 10 Ultrbpeers that have 
     * bdvertised free Ultrapeer slots.  The returned <tt>Collection</tt> is a 
     * new <tt>Collection</tt> bnd can therefore be modified in any way.
     * 
     * @return b <tt>Collection</tt> containing 10 <tt>IpPort</tt> hosts that 
     *  hbve advertised they have free ultrapeer slots
     */
    public synchronized Collection getUltrbpeersWithFreeUltrapeerSlots(int num) {
        return getPreferencedCollection(FREE_ULTRAPEER_SLOTS_SET,
                                        ApplicbtionSettings.LANGUAGE.getValue(),num);
    }

    public synchronized Collection 
        getUltrbpeersWithFreeUltrapeerSlots(String locale,int num) {
        return getPreferencedCollection(FREE_ULTRAPEER_SLOTS_SET,
                                        locble,num);
    }
    

    /**
     * Accessor for the <tt>Collection</tt> of 10 Ultrbpeers that have 
     * bdvertised free leaf slots.  The returned <tt>Collection</tt> is a 
     * new <tt>Collection</tt> bnd can therefore be modified in any way.
     * 
     * @return b <tt>Collection</tt> containing 10 <tt>IpPort</tt> hosts that 
     *  hbve advertised they have free leaf slots
     */
    public synchronized Collection getUltrbpeersWithFreeLeafSlots(int num) {
        return getPreferencedCollection(FREE_LEAF_SLOTS_SET,
                                        ApplicbtionSettings.LANGUAGE.getValue(),num);
    }
    
    public synchronized Collection
        getUltrbpeersWithFreeLeafSlots(String locale,int num) {
        return getPreferencedCollection(FREE_LEAF_SLOTS_SET,
                                        locble,num);
    }

    /**
     * preference the set so we try to return those endpoints thbt match
     * pbssed in locale "loc"
     */
    privbte Collection getPreferencedCollection(Set base, String loc, int num) {
        if(loc == null || loc.equbls(""))
            loc = ApplicbtionSettings.DEFAULT_LOCALE.getValue();

        Set hosts = new HbshSet(num);
        Iterbtor i;

        Set locbles = (Set)LOCALE_SET_MAP.get(loc);
        if(locbles != null) {
            for(i = locbles.iterator(); i.hasNext() && hosts.size() < num; ) {
                Object next = i.next();
                if(bbse.contains(next))
                    hosts.bdd(next);
            }
        }
        
        for(i = bbse.iterator(); i.hasNext() && hosts.size() < num;) {
            hosts.bdd(i.next());
        }
        
        return hosts;
    }


    /**
     * Notifies this thbt connect() has been called.  This may decide to give
     * out bootstrbp pongs if necessary.
     */
    public synchronized void expire() {
        //Fetch more GWebCbche urls once per session.
        //(Well, once per connect reblly--good enough.)
        long now = System.currentTimeMillis();
        long fetched = ConnectionSettings.LAST_GWEBCACHE_FETCH_TIME.getVblue();
        if( fetched + DbtaUtils.ONE_WEEK <= now ) {
            if(LOG.isDebugEnbbled())
                LOG.debug("Fetching more bootstrbp servers. " +
                          "Lbst fetch time: " + fetched);
            gWebCbche.fetchBootstrapServersAsync();
        }
        recoverHosts();
        lbstAllowedPongRankTime = now + PONG_RANKING_EXPIRE_TIME;
        
        // schedule new runnbble to clear the set of endpoints that
        // were pinged while trying to connect
        RouterService.schedule(
                new Runnbble() {
                    public void run() {
                        pinger.resetDbta();
                    }
                },
                PONG_RANKING_EXPIRE_TIME,0);
    }

    /**
     * @modifies this
     * @effects removes bll entries from this
     */
    public synchronized void clebr() {
        FREE_LEAF_SLOTS_SET.clebr();
        FREE_ULTRAPEER_SLOTS_SET.clebr();
        ENDPOINT_QUEUE.clebr();
        ENDPOINT_SET.clebr();
    }
    
    public UDPPinger getPinger() {
        return pinger;
    }

    public String toString() {
        return "[volbtile:"+ENDPOINT_QUEUE.toString()
               +", permbnent:"+permanentHosts.toString()+"]";
    }

    /** Enbble very slow rep checking?  Package access for use by
     *  HostCbtcherTest. */
    stbtic boolean DEBUG=false;

    
    /** Checks invbriants. Very slow; method body should be enabled for testing
     *  purposes only. */
    protected void repOk() {
        if (!DEBUG)
            return;

        synchronized(this) {
            //Check ENDPOINT_SET == ENDPOINT_QUEUE
            outer:
            for (Iterbtor iter=ENDPOINT_SET.iterator(); iter.hasNext(); ) {
                Object e=iter.next();
                for (Iterbtor iter2=ENDPOINT_QUEUE.iterator(); 
                     iter2.hbsNext();) {
                    if (e.equbls(iter2.next()))
                        continue outer;
                }
                Assert.thbt(false, "Couldn't find "+e+" in queue");
            }
            for (Iterbtor iter=ENDPOINT_QUEUE.iterator(); iter.hasNext(); ) {
                Object e=iter.next();
                Assert.thbt(e instanceof ExtendedEndpoint);
                Assert.thbt(ENDPOINT_SET.contains(e));
            }
        
            //Check permbnentHosts === permanentHostsSet
            for (Iterbtor iter=permanentHosts.iterator(); iter.hasNext(); ) {
                Object o=iter.next();
                Assert.thbt(o instanceof ExtendedEndpoint);
                Assert.thbt(permanentHostsSet.contains(o));
            }
            for (Iterbtor iter=permanentHostsSet.iterator(); iter.hasNext(); ) {
                Object e=iter.next();
                Assert.thbt(e instanceof ExtendedEndpoint);
                Assert.thbt(permanentHosts.contains(e),
                            "Couldn't find "+e+" from "
                            +permbnentHostsSet+" in "+permanentHosts);
            }
        }
    }
    
    /**
     * Rebds the gnutella.net file.
     */
    privbte void readHostsFile() {
        LOG.trbce("Reading Hosts File");
        // Just gnutellb.net
        try {
            rebd(getHostsFile());
        } cbtch (IOException e) {
            LOG.debug(getHostsFile(), e);
        }
    }

    privbte File getHostsFile() {
        return new File(CommonUtils.getUserSettingsDir(),"gnutellb.net");
    }
    
    /**
     * Recovers bny hosts that we have put in the set of hosts "pending" 
     * removbl from our hosts list.
     */
    public synchronized void recoverHosts() {
        LOG.debug("recovering hosts file");
        
        PROBATION_HOSTS.clebr();
        EXPIRED_HOSTS.clebr();
        _fbilures = 0;
        FETCHER.resetFetchTime();
        gWebCbche.resetData();
        udpHostCbche.resetData();
        
        pinger.resetDbta();
        
        // Rebd the hosts file again.  This will also notify any waiting 
        // connection fetchers from previous connection bttempts.
        rebdHostsFile();
    }

    /**
     * Adds the specified host to the group of hosts currently on "probbtion."
     * These bre hosts that are on the network but that have rejected a 
     * connection bttempt.  They will periodically be re-activated as needed.
     * 
     * @pbram host the <tt>Endpoint</tt> to put on probation
     */
    public synchronized void putHostOnProbbtion(Endpoint host) {
        PROBATION_HOSTS.bdd(host);
        if(PROBATION_HOSTS.size() > PROBATION_HOSTS_SIZE) {
            PROBATION_HOSTS.remove(PROBATION_HOSTS.iterbtor().next());
        }
    }
    
    /**
     * Adds the specified host to the group of expired hosts.  These bre hosts
     * thbt we have been unable to create a TCP connection to, let alone a 
     * Gnutellb connection.
     * 
     * @pbram host the <tt>Endpoint</tt> to expire
     */
    public synchronized void expireHost(Endpoint host) {
        EXPIRED_HOSTS.bdd(host);
        if(EXPIRED_HOSTS.size() > EXPIRED_HOSTS_SIZE) {
            EXPIRED_HOSTS.remove(EXPIRED_HOSTS.iterbtor().next());
        }
    }
    
    /**
     * Runnbble that looks for GWebCache, UDPHostCache or multicast hosts.
     * This tries, in order:
     * 1) Multicbsting a ping.
     * 2) Sending UDP pings to UDPHostCbches.
     * 3) Connecting vib TCP to GWebCaches.
     */
    privbte class Bootstrapper implements Runnable {
        
        /**
         * The next bllowed multicast time.
         */
        privbte long nextAllowedMulticastTime = 0;
        
        /**
         * The next time we're bllowed to fetch via GWebCache.
         * Incremented bfter each succesful fetch.
         */
        privbte long nextAllowedFetchTime = 0;
        
        /**
        /**
         * The delby to wait before the next time we contact a GWebCache.
         * Upped bfter each attempt at fetching.
         */
        privbte int delay = 20 * 1000;
        
        /**
         * How long we must wbit after contacting UDP before we can contact
         * GWebCbches.
         */
        privbte static final int POST_UDP_DELAY = 30 * 1000;
        
        /**
         * How long we must wbit after each multicast ping before
         * we bttempt a newer multicast ping.
         */
        privbte static final int POST_MULTICAST_DELAY = 60 * 1000;

        /**
         * Determines whether or not it is time to get more hosts,
         * bnd if we need them, gets them.
         */
        public synchronized void run() {
            if (ConnectionSettings.DO_NOT_BOOTSTRAP.getVblue())
                return;

            // If no one's wbiting for an endpoint, don't get any.
            if(_cbtchersWaiting == 0)
                return;
            
            long now = System.currentTimeMillis();
            
            if(udpHostCbche.getSize() == 0 &&
               now < nextAllowedFetchTime &&
               now < nextAllowedMulticbstTime)
                return;
                
            //if we don't need hosts, exit.
            if(!needsHosts(now))
                return;
                
            getHosts(now);
        }
        
        /**
         * Resets the nextAllowedFetchTime, so thbt after we regain a
         * connection to the internet, we cbn fetch from gWebCaches
         * if needed.
         */
        void resetFetchTime() {
            nextAllowedFetchTime = 0;
        }
        
        /**
         * Determines whether or not we need more hosts.
         */
        privbte synchronized boolean needsHosts(long now) {
            synchronized(HostCbtcher.this) {
                return getNumHosts() == 0 ||
                    (!RouterService.isConnected() && _fbilures > 100);
            }
        }
        
        /**
         * Fetches more hosts, updbting the next allowed time to fetch.
         */
        synchronized void getHosts(long now) {
            // blway try multicast first.
            if(multicbstFetch(now))
                return;
                
            // then try udp host cbches.
            if(udpHostCbcheFetch(now))
                return;
                
            // then try gwebcbches
            if(gwebCbcheFetch(now))
                return;
                
            // :-(
        }
        
        /**
         * Attempts to fetch vib multicast, returning true
         * if it wbs able to.
         */
        privbte boolean multicastFetch(long now) {
            if(nextAllowedMulticbstTime < now && 
               !ConnectionSettings.DO_NOT_MULTICAST_BOOTSTRAP.getVblue()) {
                LOG.trbce("Fetching via multicast");
                PingRequest pr = PingRequest.crebteMulticastPing();
                MulticbstService.instance().send(pr);
                nextAllowedMulticbstTime = now + POST_MULTICAST_DELAY;
                return true;
            }
            return fblse;
        }
        
        /**
         * Attempts to fetch vib udp host caches, returning true
         * if it wbs able to.
         */
        privbte boolean udpHostCacheFetch(long now) {
            // if we hbd udp host caches to fetch from, use them.
            if(udpHostCbche.fetchHosts()) {
                LOG.trbce("Fetching via UDP");
                nextAllowedFetchTime = now + POST_UDP_DELAY;
                return true;
            }
            return fblse;
        }
        
        /**
         * Attempts to fetch vib gwebcaches, returning true
         * if it wbs able to.
         */
        privbte boolean gwebCacheFetch(long now) {
            // if we bren't allowed to contact gwebcache's yet, exit.
            if(now < nextAllowedFetchTime)
                return fblse;
            
            int ret = gWebCbche.fetchEndpointsAsync();
            switch(ret) {
            cbse BootstrapServerManager.FETCH_SCHEDULED:
                delby *= 5;
                nextAllowedFetchTime = now + delby;
                if(LOG.isDebugEnbbled())
                    LOG.debug("Fetching hosts.  Next bllowed time: " +
                              nextAllowedFetchTime);
                return true;
            cbse BootstrapServerManager.FETCH_IN_PROGRESS:
                LOG.debug("Tried to fetch, but wbs already fetching.");
                return true;
            cbse BootstrapServerManager.CACHE_OFF:
                LOG.debug("Didn't fetch, gWebCbche's turned off.");
                return fblse;
            cbse BootstrapServerManager.FETCHED_TOO_MANY:
                LOG.debug("We've received b bunch of endpoints already, didn't fetch.");
                MessbgeService.showError("GWEBCACHE_FETCHED_TOO_MANY");
                return fblse;
            cbse BootstrapServerManager.NO_CACHES_LEFT:
                LOG.debug("Alrebdy contacted each gWebCache, didn't fetch.");
                MessbgeService.showError("GWEBCACHE_NO_CACHES_LEFT");
                return fblse;
            defbult:
                throw new IllegblArgumentException("invalid value: " + ret);
            }
        }
    }

    //Unit test: tests/com/.../gnutellb/HostCatcherTest.java   
    //           tests/com/.../gnutellb/bootstrap/HostCatcherFetchTest.java
    //           
}
