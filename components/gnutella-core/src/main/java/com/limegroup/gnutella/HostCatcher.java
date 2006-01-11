
// Edited for the Learning branch

package com.limegroup.gnutella;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.bootstrap.BootstrapServer;
import com.limegroup.gnutella.bootstrap.BootstrapServerManager;
import com.limegroup.gnutella.bootstrap.UDPHostCache;
import com.limegroup.gnutella.messages.PingReply;
import com.limegroup.gnutella.messages.PingRequest;
import com.limegroup.gnutella.settings.ApplicationSettings;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.util.BucketQueue;
import com.limegroup.gnutella.util.Cancellable;
import com.limegroup.gnutella.util.CommonUtils;
import com.limegroup.gnutella.util.DataUtils;
import com.limegroup.gnutella.util.FixedsizePriorityQueue;
import com.limegroup.gnutella.util.IpPort;
import com.limegroup.gnutella.util.NetworkUtils;

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
public class HostCatcher {

    //done
    
    /** A log we can record lines of text in to see how the program acts when it's running. */
    private static final Log LOG = LogFactory.getLog(HostCatcher.class);

    //do
    
    /**
     * Size of the queue for hosts returned from the GWebCaches.
     */
    static final int CACHE_SIZE = 20;
    
    /**
     * The number of ultrapeer pongs to store.
     */
    static final int GOOD_SIZE = 1000;

    //done

    /*
     * NORMAL_SIZE
     * The number of normal pongs to store.
     * This must be large enough to store all permanent addresses, as permanent addresses when read from disk are stored as normal priority.
     * 
     * PERMANENT_SIZE
     * The number of permanent locations to store in gnutella.net.
     * This must not be greater than NORMAL_SIZE.
     * This is because when we read in endpoints, we add them as NORMAL_PRIORITY.
     * If we have written out more than NORMAL_SIZE hosts, then we guarantee that endpoints will be ejected from the ENDPOINT_QUEUE upon startup.
     * Because we write out best first (and worst last), and thus read in best first (and worst last)
     * this means that we will be ejecting our best endpoints and using our worst ones when starting.
     */

    /** 400, there are up to 400 IP addresses and port numbers in the permanentHosts list and gnutella.net file. */
    static final int NORMAL_SIZE = 400;

    /** 400, there are up to 400 IP addresses and port numbers in the permanentHosts list and gnutella.net file. */
    static final int PERMANENT_SIZE = NORMAL_SIZE;

    //do

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

    //done

    /*
     * ENDPOINT_QUEUE and ENDPOINT_SET hold the list of IP addresses and port numbers we can try to connect to.
     * They are the addresses of remote computers on the Internet running Gnutella software that might be online right now, ready to accept our Gnutella connection.
     * This is our list of hosts to try.
     * 
     * ENDPOINT_QUEUE and ENDPOINT_SET contain exactly the same elements.
     * There are no duplicate elements in the list.
     * Both collections classes are bounded in size. (do)
     * 
     * The Java HashSet ENDPOINT_SET lets us quickly notice a duplicate.
     * The LimeWire BucketQueue ENDPOINT_QUEUE keeps the same ExtendedEndpoint objects in priority sorted order.
     * 
     * We'll keep ExtendedEndpoint objects in both of these lists.
     * Lock on this HostCatcher class before modifying these lists.
     */

    /**
     * A BucketQueue that sorts the ExtendedEndpoint objects by priority and then by when you added them.
     * 
     * ENDPOINT_QUEUE is a LimeWire BucketQueue object.
     * We make it by handing the constructor an array of 3 integers: {400, 1000, 20}.
     * This sets up the BucketQueue to hold ExtendedEndpoints of 3 different priority levels.
     * The BucketQueue can hold up to 400 bad ExtendedEndpoint objects, 1000 middle ones, and 20 of the best. (do) check this order is right
     * Within the bucket for each priority, the most recently added items are first.
     * When you iterate through ENDPOINT_QUEUE, you'll get newest to oldest of the highest priority, then newest to oldest of the next priority, and so on.
     * 
     * Here, the 3 numbers represent {400 ultrapeers, 1000 normal (do), 20 private addresses (do)}.
     * Within each priority level, recent hosts are prioritized over older ones.
     */
    private final BucketQueue ENDPOINT_QUEUE = new BucketQueue(new int[] {NORMAL_SIZE, GOOD_SIZE, CACHE_SIZE});

    /** The same ExtendedEndpoint objects as ENDPOINT_QUEUE in a Java HashSet. */
    private final Set ENDPOINT_SET = new HashSet();

    //do

    /**
     * <tt>Set</tt> of hosts advertising free Ultrapeer connection slots.
     */
    private final Set FREE_ULTRAPEER_SLOTS_SET = new HashSet();
    
    /**
     * <tt>Set</tt> of hosts advertising free leaf connection slots.
     */
    private final Set FREE_LEAF_SLOTS_SET = new HashSet();

    //done

    /**
     * LOCALE_SET_MAP is a data structure that organizes ExtendedEndpoint objects by their language preference.
     * 
     * LOCALE_SET_MAP is a HashMap.
     * The keys will be language codes, like "en" for English.
     * The values will be HashSet objects.
     * 
     * Each HashSet object will hold up to 100 ExtendedEndpoints.
     * The ExtendedEndpoints hold address information about remote computers with that language preference.
     */
    private final Map LOCALE_SET_MAP = new HashMap();

    /** 100, we'll keep up to 100 ExtendedEndpoint objects for each language in LOCALE_SET_MAP. */
    private static final int LOCALE_SET_SIZE = 100;

    /**
     * A list of 400 IP addresses and port numbers we'll try to connect to.
     * This is the list we keep in the gnutella.net file.
     * 
     * This list holds the computers with the highest average daily uptime.
     * We have the best change of contacting them in the future.
     * They don't necessarily have open slots now.
     * They act more like bootstrap hosts than normal pongs.
     * 
     * This list contains no duplicates.
     * We use the HashSet named permanentHostsSet to prevent duplicates.
     * 
     * Lock on this HostCatcher class before modifying this list.
     */
    private FixedsizePriorityQueue permanentHosts = // We'll keep ExtendedEndpoint objects in this FixedsizePriorityQueue
        new FixedsizePriorityQueue(                 // Make a new FixedsizePriorityQueue
            ExtendedEndpoint.priorityComparator(),  // Have it call ExtendedEndpoint.PriorityComparator.compare(a, b)
            PERMANENT_SIZE);                        // When it has 400 ExtendedEndpoints, it will throw out the worst one to add a new one

    /**
     * A copy of the list of 400 IP addresses from gnutella.net that we'll try to connect to.
     * 
     * permanentHosts and permanentHostsSet contain exactly the same elements.
     * We use this HashSet to prevent duplicates in the lists.
     * 
     * Lock on the HostCatcher class before modifying this list.
     */
    private Set permanentHostsSet = new HashSet(); // We'll put ExtendedEndpoint objects in this HashSet

    //do

    /** The GWebCache bootstrap system. */
    private BootstrapServerManager gWebCache = 
        BootstrapServerManager.instance();
    
    /**
     * The pinger that will send the messages
     */
    private UniqueHostPinger pinger;
        
    /** The UDPHostCache bootstrap system. */
    private UDPHostCache udpHostCache;
    
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
     * The scheduled runnable that fetches GWebCache entries if we need them.
     */
    public final Bootstrapper FETCHER = new Bootstrapper();
    
    /**
     * The number of threads waiting to get an endpoint.
     */
    private volatile int _catchersWaiting = 0;
    
    /**
     * The last allowed time that we can continue ranking pongs.
     */
    private long lastAllowedPongRankTime = 0;
    
    /**
     * The amount of time we're allowed to do pong ranking after
     * we click connect.
     */
    private final long PONG_RANKING_EXPIRE_TIME = 20 * 1000;
    
    /**
     * Stop ranking if we have this many connections.
     */
    private static final int MAX_CONNECTIONS = 5;
    
    /**
     * Whether or not hosts have been added since we wrote to disk.
     */
    private boolean dirty = false;
    
	/**
	 * Creates a new <tt>HostCatcher</tt> instance.
	 */
	public HostCatcher() {

        pinger = new UniqueHostPinger();
        udpHostCache = new UDPHostCache(pinger);
    }

    /**
     * Initializes any components required for HostCatcher.
     * Currently, this schedules occasional services.
     */
    public void initialize() {
        LOG.trace("START scheduling");
        
        scheduleServices();
    }
    
    protected void scheduleServices() {
        //Register to send updates every hour (starting in one hour) if we're a
        //supernode and have accepted incoming connections.  I think we should
        //only do this if we also have incoming slots, but John Marshall from
        //Gnucleus says otherwise.
        Runnable updater=new Runnable() {
            public void run() {
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
            }
        };
        
        RouterService.schedule(updater, 
							   BootstrapServerManager.UPDATE_DELAY_MSEC, 
							   BootstrapServerManager.UPDATE_DELAY_MSEC);
        
        Runnable probationRestorer = new Runnable() {
            public void run() {
                LOG.trace("restoring hosts on probation");
                synchronized(HostCatcher.this) {
                    Iterator iter = PROBATION_HOSTS.iterator();
                    while(iter.hasNext()) {
                        Endpoint host = (Endpoint)iter.next();
                        add(host, false);
                    }
                    
                    PROBATION_HOSTS.clear();
                }
            } 
        };
        // Recover hosts on probation every minute.
        RouterService.schedule(probationRestorer, 
            PROBATION_RECOVERY_WAIT_TIME, PROBATION_RECOVERY_TIME);
            
        // Try to fetch GWebCache's whenever we need them.
        // Start it immediately, so that if we have no hosts
        // (because of a fresh installation) we will connect.
        RouterService.schedule(FETCHER, 0, 2*1000);
        LOG.trace("STOP scheduling");
    }

    /**
     * Sends UDP pings to hosts read from disk.
     */
    public void sendUDPPings() {
        // We need the lock on this so that we can copy the set of endpoints.
        synchronized(this) {
            rank(new HashSet(ENDPOINT_SET));
        }
    }
    
    /**
     * Rank the collection of hosts.
     */
    private void rank(Collection hosts) {
        
        if (needsPongRanking()) {
            
            pinger.rank(
                hosts,
                // cancel when connected -- don't send out any more pings
                new Cancellable() {
                    public boolean isCancelled() {
                        return !needsPongRanking();
                    }
                }
            );
        }
    }
    
    /**
     * Determines if UDP Pongs need to be sent out.
     */
    private boolean needsPongRanking() {
        if(RouterService.isFullyConnected())
            return false;
        int have = RouterService.getConnectionManager().
            getInitializedConnections().size();
        if(have >= MAX_CONNECTIONS)
            return false;
            
        long now = System.currentTimeMillis();
        if(now > lastAllowedPongRankTime)
            return false;

        int size;
        if(RouterService.isSupernode())
            size = FREE_ULTRAPEER_SLOTS_SET.size();
        else
            size = FREE_LEAF_SLOTS_SET.size();

        int preferred = RouterService.getConnectionManager().
            getPreferredConnectionCount();
        
        return size < preferred - have;
    }
    
    /**
     * Reads in endpoints from the given file.  This is called by initialize, so
     * you don't need to call it manually.  It is package access for
     * testability.
     *
     * @modifies this
     * @effects read hosts from the given file.
     * 
     *  
     * @param hostFile A File object holding a path like "C:\Documents and Settings\kfaaborg\.limewire\gnutella.net"
     * 
     * @throws FileNotFoundException
     * @throws IOException
     */
    synchronized void read(File hostFile) throws FileNotFoundException, IOException {

        // Record that we're going to start reading the gnutella.net file in the debugging log
        LOG.trace("entered HostCatcher.read(File)");

        // We'll use a Java BufferedReader to read lines from the file
        BufferedReader in = null;

        try {

            // Make a new Java FileReader and BufferReader objects that will open the file at the path and read its contents
            in = new BufferedReader(new FileReader(hostFile));

            // Loop reading lines of text from the file
            while (true) {

                // Read the next line from the file
                String line = in.readLine(); // The lines are ended with just "\n", and readLine() can deal with that
                if (LOG.isTraceEnabled()) LOG.trace("read line: " + line); // Record it in the debugging log
                if (line == null) break; // The last line was the last one in the file, we're done

                try {

                    // See if the BootstrapServerManager named gWebCache will accept this line as being about a GWebCache
                    gWebCache.addBootstrapServer(new BootstrapServer(line));

                    // The gWebCache object accepted it, go to the start of the loop to read the next line
                    continue;

                // The gWebCache object threw a ParseException because line isn't about a GWebCache, ignore it and keep going
                } catch (ParseException ignore) {}

                //Is it a normal endpoint?
                try {

                    add(ExtendedEndpoint.read(line), NORMAL_PRIORITY);

                } catch (ParseException pe) {

                    continue;
                }
            }

        } finally {
            gWebCache.bootstrapServersAdded();
            udpHostCache.hostCachesAdded();
            try {
                if( in != null )
                    in.close();
            } catch(IOException e) {}
        }
        LOG.trace("left HostCatcher.read(File)");
    }

    //done

	/**
     * Writes the gnutella.net file, turning each ExtendedEndpoint in the permanentHosts list into a line of text there.
	 */
	synchronized void write() throws IOException {

        // Get the path to the gnutella.net file, and write the permanentHosts to it
		write(getHostsFile());
	}

    /**
     * Write the gnutella.net file turning each ExtendedEndpoint in the permanentHosts list into a line of text there.
     * A gnutella.net file looks like this:
     * 
     * http://gwcrab.sarcastro.com:8001/
     * http://pokerface.bishopston.net:3558/
     * uhc2.limewire.com:20181,,1131588496406,,,en,0
     * uhc3.limewire.com:51180,,1131588496406,,,en,0
     * 24.47.251.37:32851,51154,1134687004484,,,en,
     * 69.92.140.161:15562,86400,1133997176312,,,en,
     * 69.205.59.212:6346,86276,1134322024359,1134686990578;1134322028968,,en,
     * 67.9.175.234:6346,86400,1132897988921,1134686989046;1134345039890;1133994459812,,en,
     * 
     * In this example, the first 2 lines are written by the GWebCache object.
     * They are the addresses of GWebCache scrips on the Web.
     * The second 2 lines above are UDP host caches.
     * They are written by the udpHostCache object.
     * After that come the ExtendedEndpoints from permanentHosts.
     * They are ordered worst to best.
     * The second to last one above has 3 times that we were able to contact it.
     * It's the best one we've got.
     * 
     * The permanentHosts list is ordered by priority, worst to best.
     * The Iterator we get to loop down it proceedes through it in this order.
     * So, the lines of text in the gnutella.net file are ordered with the computers we'll be most likely to contact last.
     * 
     * @param hostFile A File object with the path to the gnutella.net file
     */
    synchronized void write(File hostFile) throws IOException {

        // If in test mode, make sure the lists are OK
        repOk();

        // If we added some hosts since we last wrote gnutella.net
        if (dirty ||                       // If we added some hosts since we last wrote gnutella.net, or
            gWebCache.isDirty() ||         // The GWebCache object has changed data for gnutella.net, or
            udpHostCache.isWriteDirty()) { // The UDPHostCache object has changed data for gnutella.net

            // Open the gnutella.net file for writing
            FileWriter out = new FileWriter(hostFile);

            // Have the GWebCache and UDP host cache objects write their information first
            gWebCache.write(out);
            udpHostCache.write(out);

            /*
             * Write elements of permanent from worst to best.  Order matters, as it
             * allows read() to put them into queue in the right order without any
             * difficulty.
             */

            // Loop through all the ExtendedEndpoints in the permanentHosts list
            for (Iterator iter = permanentHosts.iterator(); iter.hasNext(); ) {

                // Get one and write it into a line of text in the gnutella.net file
                ExtendedEndpoint e = (ExtendedEndpoint)iter.next();
                e.write(out);
            }

            // Close the gnutella.net file
            out.close();
        }
    }
    
    //do

    ///////////////////////////// Add Methods ////////////////////////////


    /**
     * 
     * 
     * Attempts to add a pong to this, possibly ejecting other elements from the
     * cache.  This method used to be called "spy".
     *
     * @param pr the pong containing the address/port to add
     * @param receivingConnection the connection on which we received
     *  the pong.
     * @return true iff pr was actually added
     * 
     * @param pr The Gnutella pong packet that contains the IP address and port number we want to add
     * 
     */
    public boolean add(PingReply pr) {

        // Make an ExtendedEndpoint from the information in the pong packet
        ExtendedEndpoint endpoint;

        // The pong packet contains the number of seconds the remote computer at the stated address is online during an average day
        if (pr.getDailyUptime() != -1) {

            // Make a new ExtendedEndpoint object with the IP address, port number, and average daily uptime from the pong packet
            endpoint = new ExtendedEndpoint(pr.getAddress(), pr.getPort(), pr.getDailyUptime());

        // The pong packet doesn't contain the average daily uptime of the IP address and port number it's telling us
        } else {

            // Make a new ExtendedEndpoint object with the IP address and port number from the pong packet
            endpoint = new ExtendedEndpoint(pr.getAddress(), pr.getPort()); // endpoint.dailyUptime will remain -1 unknown
        }

        // If the pong has language preference information, copy it into the ExtendedEndpoint object
        if (!pr.getClientLocale().equals("")) endpoint.setClientLocale(pr.getClientLocale());

        // If the information in the pong packet is about a UDP host cache
        if (pr.isUDPHostCache()) {

            // Get the IP address of the UDP host cache from the pong, and change the ExtendedEndpoint's IP address to it
            endpoint.setHostname(pr.getUDPCacheAddress());

            // Mark the ExtendedEndpoint as holding the IP address and port number of a UDP host cache
            endpoint.setUDPHostCache(true);
        }

        // Make sure the IP address and port number look valid
        if (!isValidHost(endpoint)) return false;

        // The pong packet says the computer at the IP address and port number supports unicast GUESS-style queries (do)
        if (pr.supportsUnicast()) {

            // Add it to the QueryUnicaster (do)
            QueryUnicaster.instance().addUnicastEndpoint(pr.getInetAddress(), pr.getPort());
        }

        // if the pong carried packed IP/Ports, add those as their own
        // endpoints.
        rank(pr.getPackedIPPorts());

        for (Iterator i = pr.getPackedIPPorts().iterator(); i.hasNext(); ) {

            IpPort ipp = (IpPort)i.next();
            ExtendedEndpoint ep = new ExtendedEndpoint(ipp.getAddress(), ipp.getPort());

            // If the IP address and port number look valid, add it to the permanentHosts list which gets written to the gnutella.net file
            if (isValidHost(ep)) add(ep, GOOD_PRIORITY);
        }

        // if the pong carried packed UDP host caches, add those as their
        // own endpoints.
        for(Iterator i = pr.getPackedUDPHostCaches().iterator(); i.hasNext(); ) {
            IpPort ipp = (IpPort)i.next();
            ExtendedEndpoint ep = new ExtendedEndpoint(ipp.getAddress(), ipp.getPort());
            ep.setUDPHostCache(true);
            addUDPHostCache(ep);
        }
        
        // if it was a UDPHostCache pong, just add it as that.
        if(endpoint.isUDPHostCache())
            return addUDPHostCache(endpoint);

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
               || //or if the locales match and it has free locale pref. slots
               (ApplicationSettings.LANGUAGE.getValue()
                .equals(pr.getClientLocale()) && pr.getNumFreeLocaleSlots() > 0)) {
                addToFixedSizeSet(endpoint, FREE_ULTRAPEER_SLOTS_SET);
                return true;
            } 
            
            return add(endpoint, GOOD_PRIORITY); 
        } else
            return add(endpoint, NORMAL_PRIORITY);
    }

    //done

    /**
     * Gives an ExtendedEndpoint to the UDPHostCache object.
     * Calls udpHostCache.add(host).
     * 
     * @param host An ExtendedEndpoint we made from a line from gnutella.net that describes the IP address and port number of a UDP host cache
     * @return     True if it added it, false if it already had it
     */
    private boolean addUDPHostCache(ExtendedEndpoint host) {

        // Have the UDPHostCache object take this one
        return udpHostCache.add(host);
    }

    //do

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
        
        // Add host to the permanentHosts list which gets written to the gnutella.net file
        addPermanent(host);
        notify();
    }
    
    //done

    /**
     * Add the given ExtendedEndpoint to the LOCALE_SET_MAP data structure, which organizes the endpoints by their language of choice.
     * 
     * @param endpoint The ExtendedEndpoint to add to the LOCALE_SET_MAP data structure
     */
    private synchronized void addToLocaleMap(ExtendedEndpoint endpoint) {

        // Get the remote computer's language choice, like "en" for English
        String loc = endpoint.getClientLocale();

        // If the LOCALE_SET_MAP has an entry for this language
        if (LOCALE_SET_MAP.containsKey(loc)) { //if set exists for ths locale

            // Get the value stored for the language
            Set s = (Set)LOCALE_SET_MAP.get(loc); // LOCALE_SET_MAP is a HashMap of HashSet objects, and s is the HashSet for the given language

            // Add the given ExtendedEndpoint to the HashSet for computers with that language preference
            if (s.add(endpoint) // If it was unique and added, returns true, keep going

                // If we just added the 101st ExtendedEndpoint of this language
                && s.size() > LOCALE_SET_SIZE)

                    // Remove one ExtendedEndpoint from the HashSet
                    s.remove(s.iterator().next()); // Removes the first one

            /*
             * s.add(endpoint) adds the ExtendedEndpoint.
             * If it's the 101st, s.remove(s.iterator().next()) removes one.
             * 
             * remove() removes the first one, but add() doesn't add any particular location in the list.
             * We can make the following assumptions:
             * add() is placing new ExtendedEndpoints in seemingly random locations in the HashMap.
             * remove() doesn't remove the one that add() just added.
             */

        // LOCALE_SET_MAP doesn't have an entry for this language yet
        } else {

            // Make a new HashSet and add the given ExtendedEndpoint to it
            Set s = new HashSet();
            s.add(endpoint);

            // Store the new HashSet for that language in LOCALE_SET_MAP under the language String like "en"
            LOCALE_SET_MAP.put(loc, s);
        }
    }
    
    //do

    /**
     * Adds a collection of addresses to this.
     */
    public void add(Collection endpoints) {
        rank(endpoints);
        for(Iterator i = endpoints.iterator(); i.hasNext(); )
            add((Endpoint)i.next(), true);
            
    }


    /**
     * Adds an address to this, possibly ejecting other elements from the cache.
     * This method is used when getting an address from headers instead of the
     * normal ping reply.
     *
     * @param pr the pong containing the address/port to add.
     * @param forceHighPriority true if this should always be of high priority
     * @return true iff e was actually added
     */
    public boolean add(Endpoint e, boolean forceHighPriority) {

        // Make sure the IP address and port number look valid
        if (!isValidHost(e)) return false;

        if (forceHighPriority) return add(e, GOOD_PRIORITY);
        else                   return add(e, NORMAL_PRIORITY);
    }

    /**
     * Adds an endpoint.  Use this method if the locale of endpoint is known
     * (used by ConnectionManager.disconnect())
     */
    public boolean add(Endpoint e, boolean forceHighPriority, String locale) {

        // Make sure the IP address and port number look valid
        if (!isValidHost(e)) return false;

        //need ExtendedEndpoint for the locale
        if (forceHighPriority) return add(new ExtendedEndpoint(e.getAddress(), 
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
        if (LOG.isTraceEnabled())
            LOG.trace("adding host "+host);
        if(host instanceof ExtendedEndpoint)
            return add((ExtendedEndpoint)host, priority);
        
        //need ExtendedEndpoint for the locale
        return add(new ExtendedEndpoint(host.getAddress(), 
                                        host.getPort()), 
                   priority);
    }

    /**
     * 
     * 
     * 
     * Adds e to the permanentHosts list which gets written to the gnutella.net file.
     * Adds e to the ENDPOINT_SET HashSet
     * 
     * 
     * 
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
     * 
     * 
     * @param e        An ExtendedEndpoint object
     * @param priority GOOD_PRIORITY if this is the IP address and port number of an ultrapeer, NORMAL_PRIORITY if it's a leaf
     * 
     * @return         True if we added it, false if we didn't because we already have it.
     */
    private boolean add(ExtendedEndpoint e, int priority) {

        // If in test mode, make sure the lists are OK
        repOk();

        // If the line from gnutella.net described a UDP host cache, have the udpHostCache take it
        if (e.isUDPHostCache()) return addUDPHostCache(e); // The udpHostCache.add(e) returns true if it didn't have it and added it

        //Add to permanent list, regardless of whether it's actually in queue.
        //Note that this modifies e.

        // Add host to the permanentHosts list which gets written to the gnutella.net file
        addPermanent(e);

        boolean ret = false;

        synchronized (this) {

            
            if (!(ENDPOINT_SET.contains(e))) {

                ret = true;

                //Add to temporary list. Adding e may eject an older point from
                //queue, so we have to cleanup the set to maintain
                //rep. invariant.
                ENDPOINT_SET.add(e);

                // Add e to the list of hosts we can try to connect to
                Object ejected = ENDPOINT_QUEUE.insert(e, priority); // It will go into the top of the bucket for the specified priority

                // The bucket for that priority was full, the oldest one at the bottom was pushed out
                if (ejected != null) {

                    // Remove it from the HashSet to keep exactly the same ExtendedEndpoint objects in both lists
                    ENDPOINT_SET.remove(ejected);
                }

                this.notify();
            }
        }

        // If in test mode, make sure the lists are OK
        repOk();

        return ret;
    }

    /**
     * Add an ExtendedEndpoint to permanentHosts and permanentHostsSet, which hold the list that gets written to the gnutella.net file.
     * 
     * Adds an address to the permanent list of this without marking it for
     * immediate fetching.  This method is when connecting to a host and reading
     * its Uptime header.  If e is already in the permanent list, it is not
     * re-added, though its key may be adjusted.
     * 
     * @param e The ExtendedEndpoint to add
     * @return  True if we added e, false if we didn't because it's worse than all 400 already in the list
     */
    private synchronized boolean addPermanent(ExtendedEndpoint e) {

        // If this IP address is just a LAN address, don't add it
        if (NetworkUtils.isPrivateAddress(e.getInetAddress())) return false;

        /*
         * TODO: we could adjust the key
         */

        // If we already have this ExtendedEndpoint in permanentHosts and permanentHostsSet, we don't need to add it, leave now
        if (permanentHostsSet.contains(e)) return false; // Didn't add it

        /*
         * We don't have it yet, and will add it.
         */

        // Add e to the HashSet in LOCALE_SET_MAP for e's language preference
        addToLocaleMap(e);

        // Insert e into the permanentHosts FixedsizePriorityQueue, which will put it in sorted order based on how good a host it is
        Object removed = permanentHosts.insert(e); // If we've already got 400, adding this one will remove the lowest priority one of all 401 of them

        /*
         * One of 3 things could have happened:
         * permanentHosts wasn't full, it inserted e and returned null.
         * permanentHosts was full, it inserted e into priority order, and returned the worst one it removed from the list.
         * permanentHosts was full, and e was worse than all 400 in there, insert(e) left the list unchanged and returned e.
         */

        // We added e to the list
        if (removed != e) {

            // Update permanentHostsSet to keep its contents the same as permanentHosts
            permanentHostsSet.add(e);                               // Add it to the HashSet that we use to notice duplicates
            if (removed != null) permanentHostsSet.remove(removed); // Remove the one that got knocked out from the HashSet

            // The permanentHosts list no longer matches the data saved in gnutella.net
            dirty = true; // Set dirty to true so we'll write the file to disk again

            // We added e
            return true;

        // We tried to insert e, but it got spit right back out, it's actually worse than all 400 in there
        } else {

            // Didn't add e
            return false;
        }
    }

    //do

    /** Removes e from permanentHostsSet and permanentHosts. 
     *  @return true iff this was modified */
    private synchronized boolean removePermanent(ExtendedEndpoint e) {
        boolean removed1=permanentHosts.remove(e);
        boolean removed2=permanentHostsSet.remove(e);
        Assert.that(removed1==removed2,
                    "Queue "+removed1+" but set "+removed2);
        if(removed1)
            dirty = true;
        return removed1;
    }

    /**
     * Determine if an IP address and port number looks valid.
     * If it does, we'll add it to our list of hosts to try to connect to.
     * 
     * Verifies that we can read the IP address text as numbers.
     * Verifies the IP address isn't in a range DHCP servers assign on a LAN.
     * Verifies the IP address and port number isn't our own IP address and listening port number.
     * Verifies the IP address isn't on our list of institutional addresses the user doesn't want to connect to.
     * Verifies we haven't failed connecting to this host, and this host hasn't rejected us.
     * 
     * @param host An ExtendedEndpoint with the IP address and port number of a computer that may be online and accepting Gnutella connections right now
     * @return True if the IP address and port number passes all the tests and we'll add it to our list.
     *         False if it fails a test and we shouldn't add it.
     */
    private boolean isValidHost(Endpoint host) {

        // If the given ExtendedEndpoint describes a UDP host cache, return true, it will validate itself (do)
        if (host.isUDPHostCache()) return true;

        // Make sure we can read the IP address of the given ExtendedEndpoint object as an array of 4 bytes
        byte[] addr;
        try {

            // Get the IP address from the given ExtendedEndpoint as an array of 4 bytes
            addr = host.getHostBytes();

        // Calling InetAddress.getByName(text) with the IP address text caused an exception, report false, not a valid host
        } catch (UnknownHostException uhe) { return false; }

        // If the address is in one of the ranges that DHCP servers assign to computers on their LANs, report false, not a valid host
        if (NetworkUtils.isPrivateAddress(addr)) return false;

        /*
         * We used to check that we're not connected to e, but now we do that in
         * ConnectionFetcher after a call to getAnEndpoint. This is not a big
         * deal, since the call to "set.contains(e)" below ensures no duplicates.
         * Skip if this would connect us to our listening port. TODO: I think
         * this check is too strict sometimes, which makes testing difficult.
         */

        // If the address and port number are our address and port number report false, connecting to this host would be connecting to ourselves
        if (NetworkUtils.isMe(addr, host.getPort())) return false;

        // Make sure the IP address isn't on a list of institutional addresses the user does not want to connect to
        if (RouterService.getAcceptor().isBannedIP(addr)) return false;

        // Only let one thread access the EXPIRED_HOSTS and PROBATION_HOSTS lists at a time
        synchronized (this) {

            // Don't add this host if it has previously failed
            if (EXPIRED_HOSTS.contains(host)) return false;

            // Don't add this host if it has previously rejected us
            if (PROBATION_HOSTS.contains(host)) return false;
        }

        // Passed all the tests, report true, the host is valid and we can add it to our list
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
            try {
                _catchersWaiting++;
                wait();  //throws InterruptedException
            } finally {
                _catchersWaiting--;
            }
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

        // Add host to the permanentHosts list which gets written to the gnutella.net file
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
        // Otherwise, might as well use the leaf slots hosts up as well
        // since we added them to the size and they can give us other info
        else if(!FREE_LEAF_SLOTS_SET.isEmpty()) {
            Iterator iter = FREE_LEAF_SLOTS_SET.iterator();
            ExtendedEndpoint ee = (ExtendedEndpoint)iter.next();
            iter.remove();
            return ee;
        }

        // If our list of hosts to try to connect to still has at least one ExtendedEndpoint in it
        if (!ENDPOINT_QUEUE.isEmpty()) {

            // Remove and return (do)
            ExtendedEndpoint e = (ExtendedEndpoint)ENDPOINT_QUEUE.extractMax(); // Remove and return the oldest (do)
            boolean ok = ENDPOINT_SET.remove(e);                                // Also remove it from the HashSet so it still has exactly the same contents
            Assert.that(ok, "Rep. invariant for HostCatcher broken.");          // Make sure that e was actually in the HashSet

            return e;

        } else {

            throw new NoSuchElementException();
        }
    }

    /**
     * tries to return an endpoint that matches the locale of this client
     * from the passed in set.
     */
    private ExtendedEndpoint preferenceWithLocale(Set base) {

        String loc = ApplicationSettings.LANGUAGE.getValue();

        // preference a locale host if we haven't matched any locales yet
        if(!RouterService.getConnectionManager().isLocaleMatched()) {
            if(LOCALE_SET_MAP.containsKey(loc)) {
                Set locales = (Set)LOCALE_SET_MAP.get(loc);
                for(Iterator i = base.iterator(); i.hasNext(); ) {
                    Object next = i.next();
                    if(locales.contains(next)) {
                        i.remove();
                        locales.remove(next);
                        return (ExtendedEndpoint)next;
                    }
                }
            }
        }
        
        Iterator iter = base.iterator();
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
    public synchronized int getNumHosts() {
        return ENDPOINT_QUEUE.size()+FREE_LEAF_SLOTS_SET.size()+
            FREE_ULTRAPEER_SLOTS_SET.size();
    }

    /**
     * Returns the number of marked ultrapeer hosts.
     */
    public synchronized int getNumUltrapeerHosts() {
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
    public synchronized Collection getUltrapeersWithFreeUltrapeerSlots(int num) {
        return getPreferencedCollection(FREE_ULTRAPEER_SLOTS_SET,
                                        ApplicationSettings.LANGUAGE.getValue(),num);
    }

    public synchronized Collection 
        getUltrapeersWithFreeUltrapeerSlots(String locale,int num) {
        return getPreferencedCollection(FREE_ULTRAPEER_SLOTS_SET,
                                        locale,num);
    }
    

    /**
     * Accessor for the <tt>Collection</tt> of 10 Ultrapeers that have 
     * advertised free leaf slots.  The returned <tt>Collection</tt> is a 
     * new <tt>Collection</tt> and can therefore be modified in any way.
     * 
     * @return a <tt>Collection</tt> containing 10 <tt>IpPort</tt> hosts that 
     *  have advertised they have free leaf slots
     */
    public synchronized Collection getUltrapeersWithFreeLeafSlots(int num) {
        return getPreferencedCollection(FREE_LEAF_SLOTS_SET,
                                        ApplicationSettings.LANGUAGE.getValue(),num);
    }
    
    public synchronized Collection
        getUltrapeersWithFreeLeafSlots(String locale,int num) {
        return getPreferencedCollection(FREE_LEAF_SLOTS_SET,
                                        locale,num);
    }

    /**
     * preference the set so we try to return those endpoints that match
     * passed in locale "loc"
     */
    private Collection getPreferencedCollection(Set base, String loc, int num) {
        if(loc == null || loc.equals(""))
            loc = ApplicationSettings.DEFAULT_LOCALE.getValue();

        Set hosts = new HashSet(num);
        Iterator i;

        Set locales = (Set)LOCALE_SET_MAP.get(loc);
        if(locales != null) {
            for(i = locales.iterator(); i.hasNext() && hosts.size() < num; ) {
                Object next = i.next();
                if(base.contains(next))
                    hosts.add(next);
            }
        }
        
        for(i = base.iterator(); i.hasNext() && hosts.size() < num;) {
            hosts.add(i.next());
        }
        
        return hosts;
    }


    /**
     * Notifies this that connect() has been called.  This may decide to give
     * out bootstrap pongs if necessary.
     */
    public synchronized void expire() {
        //Fetch more GWebCache urls once per session.
        //(Well, once per connect really--good enough.)
        long now = System.currentTimeMillis();
        long fetched = ConnectionSettings.LAST_GWEBCACHE_FETCH_TIME.getValue();
        if( fetched + DataUtils.ONE_WEEK <= now ) {
            if(LOG.isDebugEnabled())
                LOG.debug("Fetching more bootstrap servers. " +
                          "Last fetch time: " + fetched);
            gWebCache.fetchBootstrapServersAsync();
        }
        recoverHosts();
        lastAllowedPongRankTime = now + PONG_RANKING_EXPIRE_TIME;
        
        // schedule new runnable to clear the set of endpoints that
        // were pinged while trying to connect
        RouterService.schedule(
                new Runnable() {
                    public void run() {
                        pinger.resetData();
                    }
                },
                PONG_RANKING_EXPIRE_TIME,0);
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
    
    public UDPPinger getPinger() {
        return pinger;
    }

    public String toString() {
        return "[volatile:"+ENDPOINT_QUEUE.toString()
               +", permanent:"+permanentHosts.toString()+"]";
    }

    /**
     * False, disable very slow rep checking. (do)
     * HostCatcherTest sets this to true for testing purposes.
     */
    static boolean DEBUG = false;

    /**
     * If in test mode, makes sure the lists are OK.
     * 
     * Checks invariants.
     * Very slow; method body should be enabled for testing purposes only.
     */
    protected void repOk() {

        // Don't do this, just return now
        if (!DEBUG) return;

        // Not used unless in testing mode
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
        
        LOG.trace("Reading Hosts File");
        
        // Just gnutella.net
        try {
            
            read(getHostsFile());
            
        } catch (IOException e) {
            LOG.debug(getHostsFile(), e);
        }
    }

    //done

    /**
     * The path of the gnutella.net file
     * 
     * @return A File object with a path like "C:\Documents and Settings\kfaaborg\.limewire\gnutella.net"
     */
    private File getHostsFile() {

        // Return a File object with the path to our gnutella.net file
        return new File(                      // Make a new Java File object to hold the path to the gnutella.net file
            CommonUtils.getUserSettingsDir(), // On Windows, returns a File object with a path like "C:\Documents and Settings\kfaaborg\.limewire"
            "gnutella.net");                  // The file name is "gnutella.net"
    }

    //do

    /**
     * Recovers any hosts that we have put in the set of hosts "pending" 
     * removal from our hosts list.
     */
    public synchronized void recoverHosts() {
        LOG.debug("recovering hosts file");
        
        PROBATION_HOSTS.clear();
        EXPIRED_HOSTS.clear();
        _failures = 0;
        FETCHER.resetFetchTime();
        gWebCache.resetData();
        udpHostCache.resetData();
        
        pinger.resetData();
        
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
     * Runnable that looks for GWebCache, UDPHostCache or multicast hosts.
     * This tries, in order:
     * 1) Multicasting a ping.
     * 2) Sending UDP pings to UDPHostCaches.
     * 3) Connecting via TCP to GWebCaches.
     */
    private class Bootstrapper implements Runnable {
        
        /**
         * The next allowed multicast time.
         */
        private long nextAllowedMulticastTime = 0;
        
        /**
         * The next time we're allowed to fetch via GWebCache.
         * Incremented after each succesful fetch.
         */
        private long nextAllowedFetchTime = 0;
        
        /**
        /**
         * The delay to wait before the next time we contact a GWebCache.
         * Upped after each attempt at fetching.
         */
        private int delay = 20 * 1000;
        
        /**
         * How long we must wait after contacting UDP before we can contact
         * GWebCaches.
         */
        private static final int POST_UDP_DELAY = 30 * 1000;
        
        /**
         * How long we must wait after each multicast ping before
         * we attempt a newer multicast ping.
         */
        private static final int POST_MULTICAST_DELAY = 60 * 1000;

        /**
         * Determines whether or not it is time to get more hosts,
         * and if we need them, gets them.
         */
        public synchronized void run() {
            if (ConnectionSettings.DO_NOT_BOOTSTRAP.getValue())
                return;

            // If no one's waiting for an endpoint, don't get any.
            if(_catchersWaiting == 0)
                return;
            
            long now = System.currentTimeMillis();
            
            if(udpHostCache.getSize() == 0 &&
               now < nextAllowedFetchTime &&
               now < nextAllowedMulticastTime)
                return;
                
            //if we don't need hosts, exit.
            if(!needsHosts(now))
                return;
                
            getHosts(now);
        }
        
        /**
         * Resets the nextAllowedFetchTime, so that after we regain a
         * connection to the internet, we can fetch from gWebCaches
         * if needed.
         */
        void resetFetchTime() {
            nextAllowedFetchTime = 0;
        }
        
        /**
         * Determines whether or not we need more hosts.
         */
        private synchronized boolean needsHosts(long now) {
            synchronized(HostCatcher.this) {
                return getNumHosts() == 0 ||
                    (!RouterService.isConnected() && _failures > 100);
            }
        }
        
        /**
         * Fetches more hosts, updating the next allowed time to fetch.
         */
        synchronized void getHosts(long now) {
            // alway try multicast first.
            if(multicastFetch(now))
                return;
                
            // then try udp host caches.
            if(udpHostCacheFetch(now))
                return;
                
            // then try gwebcaches
            if(gwebCacheFetch(now))
                return;
                
            // :-(
        }
        
        /**
         * Attempts to fetch via multicast, returning true
         * if it was able to.
         */
        private boolean multicastFetch(long now) {
            if(nextAllowedMulticastTime < now && 
               !ConnectionSettings.DO_NOT_MULTICAST_BOOTSTRAP.getValue()) {
                LOG.trace("Fetching via multicast");
                PingRequest pr = PingRequest.createMulticastPing();
                MulticastService.instance().send(pr);
                nextAllowedMulticastTime = now + POST_MULTICAST_DELAY;
                return true;
            }
            return false;
        }
        
        /**
         * Attempts to fetch via udp host caches, returning true
         * if it was able to.
         */
        private boolean udpHostCacheFetch(long now) {
            // if we had udp host caches to fetch from, use them.
            if(udpHostCache.fetchHosts()) {
                LOG.trace("Fetching via UDP");
                nextAllowedFetchTime = now + POST_UDP_DELAY;
                return true;
            }
            return false;
        }
        
        /**
         * Attempts to fetch via gwebcaches, returning true
         * if it was able to.
         */
        private boolean gwebCacheFetch(long now) {
            // if we aren't allowed to contact gwebcache's yet, exit.
            if(now < nextAllowedFetchTime)
                return false;
            
            int ret = gWebCache.fetchEndpointsAsync();
            switch(ret) {
            case BootstrapServerManager.FETCH_SCHEDULED:
                delay *= 5;
                nextAllowedFetchTime = now + delay;
                if(LOG.isDebugEnabled())
                    LOG.debug("Fetching hosts.  Next allowed time: " +
                              nextAllowedFetchTime);
                return true;
            case BootstrapServerManager.FETCH_IN_PROGRESS:
                LOG.debug("Tried to fetch, but was already fetching.");
                return true;
            case BootstrapServerManager.CACHE_OFF:
                LOG.debug("Didn't fetch, gWebCache's turned off.");
                return false;
            case BootstrapServerManager.FETCHED_TOO_MANY:
                LOG.debug("We've received a bunch of endpoints already, didn't fetch.");
                MessageService.showError("GWEBCACHE_FETCHED_TOO_MANY");
                return false;
            case BootstrapServerManager.NO_CACHES_LEFT:
                LOG.debug("Already contacted each gWebCache, didn't fetch.");
                MessageService.showError("GWEBCACHE_NO_CACHES_LEFT");
                return false;
            default:
                throw new IllegalArgumentException("invalid value: " + ret);
            }
        }
    }

    //Unit test: tests/com/.../gnutella/HostCatcherTest.java   
    //           tests/com/.../gnutella/bootstrap/HostCatcherFetchTest.java
    //           
}
