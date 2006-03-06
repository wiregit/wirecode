
// Commented for the Learning branch

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
 * The HostCatcher object keeps a list of IP addresses and port numbers of remote computers we can try to connect to.
 * 
 * When you run LimeWire, the program needs to connect to other computers on the Internet running Gnutella software.
 * The HostCatcher object keeps a list of IP addresses and port numbers it can try.
 * We've gotten these addresses from a variety of sources.
 * They are the addresses of computers like us that are likely to be online accepting new Gnutella connections right now.
 * 
 * To save the list between times you run LimeWire, the program writes the data to disk in a file named gnutella.net.
 * 
 * We get these addresses from several sources.
 * The "X-Try-Ultrapeers" header in the Gnutella handshake contains some.
 * Pong packets contain a handful of addresses, and information about the sender.
 * GWebCaches are PHP scripts on the Web we can tell our address, and get more.
 * UDP host caches are high-performance successors to GWebCache scripts.
 * We can send UDP Gnutella ping packets to them, and they will reply with pongs with host addresses inside.
 * We can also send UDP Gnutella ping packets to regular computers running recent Gnutella software, and they will respond in the same way.
 * 
 * RouterService.catcher creates a new HostCatcher() object, and then RouterService.start() calls catcher.initialize().
 * This schedules code for the RouterService to run on different intervals.
 * Every 2 minutes, the RouterService will call Bootstrapper.run().
 * Bootstrapper.run() contacts external services to get more hosts.
 * It uses a multicast packet, UDP host caches, and then GWebCache scripts.
 * 
 * If you've received a pong packet that has some IP addresses in it, call add(PingReply).
 * When you get IP addresses in the "X-Try-Ultrapeers" header, call add(Collection).
 * 
 * ConnectionManager.ConnectionFetcher.managedRun() calls getAnEndpoint() to get an IP address to try.
 * It later calls doneWithConnect(e, success) to tell us if it worked or not.
 * 
 * The HostCatcher object uses a LimeWire BucketQueue named ENDPOINT_QUEUE to keep the list of IP addresses.
 * It mirrors the same contents in a HashSet called ENDPOINT_SET, and uses it to detect duplicates quickly.
 * A third collections object, LOCALE_SET_MAP, organizes all the hosts by their language preference.
 * 
 * The add() methods put hosts advertising free ultrapeer connection slots in FREE_ULTRAPEER_SLOTS_SET instead of these lists.
 * Likewise, hosts that want more leaves are diverted to FREE_LEAF_SLOTS_SET.
 * 
 * In these lists, each host is represented by an ExtendedEndpoint object.
 * The ExtendedEndpoint holds the IP address, port number, and statistics about uptime and availability.
 * 
 * HostCatcher makes several important objects to help it communicate with bootstrapping services.
 * BootstrapServerManager gWebCache communicates with GWebCache scripts for us.
 * UniqueHostPinger pinger can send a Gnutella ping packet to a list of UDP host caches.
 * UDPHostCache udpHostCache keeps the list of UDP host caches.
 * 
 * In many of the names here, the word rank appears.
 * Ranking is the process of deciding which remote Gnutella PCs to contact first.
 * To rank, we'll contact UDP host caches and have them tell us the IP addresses of other Gnutella PCs to contact.
 * 
 * The host catcher.
 * This peeks at pong messages coming on the network and snatches IP addresses of other Gnutella peers.
 * IP addresses may also be added to it from a file (usually "gnutella.net").
 * The servent may then connect to these addresses as necessary to maintain full connectivity.
 * 
 * The HostCatcher currently prioritizes pongs as follows.
 * Note that Ultrapeers with a private address is still highest priority;
 * hopefully this may allow you to find local Ultrapeers.
 * 
 * (1) Ultrapeers. Ultrapeers are identified because the number of files they are sharing is an exact power of two - a dirty but effective hack.
 * (2) Normal pongs.
 * (3) Private addresses. This means that the host catcher will still work on private networks, although we will normally ignore private addresses.
 * 
 * HostCatcher also manages the list of GWebCache servers.
 * You must call expire() to start the GWebCache bootstrapping process.
 * You should do this when calling RouterService.connect().
 * 
 * HostCatcher maintains a list of permanent locations, based on average daily uptime.
 * These are stored in the gnutella.net file.
 * They are NOT bootstrap servers like router.limewire.com.
 * LimeWire doesn't use those anymore.
 */
public class HostCatcher {

    /** A log we can record lines of text in to see how the program acts when it's running. */
    private static final Log LOG = LogFactory.getLog(HostCatcher.class);

    /** 20, the BucketQueue ENDPOINT_QUEUE will hold up to 20 hosts returned from GWebCaches. */
    static final int CACHE_SIZE = 20;
    
    /** 1000, the BucketQueue ENDPOINT_QUEUE will hold the IP addresses and port numbers of up to 1000 ultrapeers. */
    static final int GOOD_SIZE = 1000;

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

    /*
     * The BucketQueue ENDPOINT_QUEUE has 3 buckets, with priorities 0, 1, and 2.
     * This class defines NORMAL_PRIORITY 0, GOOD_PRIORITY 1, and CACHE_PRIORITY 2 as names for these buckets.
     */

    /** 2, we put the hosts we get from GWebCaches in the bucket with priority number 2. */
    public static final int CACHE_PRIORITY = 2;

    /** 1, we put ultrapeers in the bucket with priority number 1. */
    public static final int GOOD_PRIORITY = 1;

    /** 0, we put leaves in the bucket with priority number 0. */
    public static final int NORMAL_PRIORITY = 0;

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
     * Here, the 3 numbers represent {400 leaves, 1000 ultrapeers, 20 addresses returned from GWebCaches}.
     * Within each priority level, recent hosts are prioritized over older ones.
     */
    private final BucketQueue ENDPOINT_QUEUE = new BucketQueue(new int[] {NORMAL_SIZE, GOOD_SIZE, CACHE_SIZE});

    /** The same ExtendedEndpoint objects as ENDPOINT_QUEUE in a Java HashSet. */
    private final Set ENDPOINT_SET = new HashSet();

    /** Set of hosts advertising free ultrapeer connection slots. */
    private final Set FREE_ULTRAPEER_SLOTS_SET = new HashSet();

    /** Set of hosts advertising free leaf connection slots. */
    private final Set FREE_LEAF_SLOTS_SET = new HashSet();

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
    private FixedsizePriorityQueue permanentHosts = new FixedsizePriorityQueue( // Make a new FixedsizePriorityQueue, we'll keep ExtendedEndpoint objects in it
        ExtendedEndpoint.priorityComparator(), // Have it call ExtendedEndpoint.PriorityComparator.compare(a, b)
        PERMANENT_SIZE);                       // When it has 400 ExtendedEndpoints, it will throw out the worst one to add a new one

    /**
     * A copy of the list of 400 IP addresses from gnutella.net that we'll try to connect to.
     * 
     * permanentHosts and permanentHostsSet contain exactly the same elements.
     * We use this HashSet to prevent duplicates in the lists.
     * 
     * Lock on the HostCatcher class before modifying this list.
     */
    private Set permanentHostsSet = new HashSet(); // We'll put ExtendedEndpoint objects in this HashSet

    /** A reference to the BootstrapServerManager, the object that helps us communicate with GWebCache scripts. */
    private BootstrapServerManager gWebCache = BootstrapServerManager.instance();

    /** The UniqueHostPinger object we'll use to send a UDP Gnutella ping packet to a list of UDP host caches or PCs running Gnutella software. */
    private UniqueHostPinger pinger;

    /** The UDPHostCache object that keeps our list of UDP host caches. */
    private UDPHostCache udpHostCache;

    /**
     * The number of hosts we were unable to connect a TCP socket to.
     * If we have a lot of failures, we'll resort to using the GWebCaches.
     */
    private int _failures;

    /**
     * Set of hosts we were unable to create TCP connections with, and we therefore shouldn't try again.
     * Locking: Synchronize on this HostCatcher object before modifying or iterating.
     */
    private final Set EXPIRED_HOSTS = new HashSet();

    /**
     * We were able to create TCP connections with these hosts, but then they didn't accept our Gnutella connection.
     * Locking: Synchronize on this HostCatcher object before modifying or iterating.
     */
    private final Set PROBATION_HOSTS = new HashSet();

    /**
     * 1 minute.
     * After 1 minute, we'll restore hosts that we connected to, but that refused us in the Gnutella handshake.
     * Not final for testing.
     */
    private static long PROBATION_RECOVERY_WAIT_TIME = 60 * 1000;

    /**
     * 1 minute.
     * Every minute, we'll restore hosts that we connected to, but that refused us in the Gnutella handshake.
     * Not final for testing.
     */
    private static long PROBATION_RECOVERY_TIME = 60 * 1000;

    /**
     * 500, we'll keep the PROBATION_HOSTS set to this capacity.
     * Public for testing.
     */
    public static final int PROBATION_HOSTS_SIZE = 500;

    /**
     * 500, we'll keep the EXPIRED_HOSTS set to this capacity.
     * Public for testing.
     */
    public static final int EXPIRED_HOSTS_SIZE = 500;

    /** FETCHER is a Bootstrapper object that we schedule to communicate with GWebCache scripts. */
    public final Bootstrapper FETCHER = new Bootstrapper();

    /**
     * The number of threads waiting to get an IP address and port number from our list, which is empty.
     * 
     * If a thread calls getAnEndpoint() and the list is empty, it will sleep there until another thread adds one to our list.
     * _catchersWaiting is the count of how many threads are stuck in getAnEndpoint().
     */
    private volatile int _catchersWaiting = 0;

    /**
     * The time when we're no longer allowed to contact UDP host caches.
     * 
     * We'll only let ourselves contact UDP host caches for the first 20 seconds we're trying to connect.
     * lastAllowedPongRankTime is the time 20 seconds from now, after which we're not allowed to contact UDP host caches.
     */
    private long lastAllowedPongRankTime = 0;

    /** 20 seconds, the amount of time we're allowed to contact UDP host caches after we click connect. */
    private final long PONG_RANKING_EXPIRE_TIME = 20 * 1000;

    /** 5, if we already have 5 connections, we shouldn't bother UDP host caches anymore. */
    private static final int MAX_CONNECTIONS = 5;

    /** True if we've changed the list and need to save the new data to the gnutella.net file. */
    private boolean dirty = false;

	/**
     * Make the HostCatcher object for the program.
     * The RouterService makes one HostCatcher for the program.
     * It saves it as RouterService.catcher.
     * 
     * Makes the UniqueHostPinger and UDPHostCache objects.
	 */
	public HostCatcher() {

        // Make the UniqueHostPinger object we'll use to send a Gnutella ping packet to a list of IP addresses using UDP
        pinger = new UniqueHostPinger();

        // Make the UDPHostCache object which will keep our list of UDP host caches and let us contact a small group at a time
        udpHostCache = new UDPHostCache(pinger);
    }

    /**
     * Schedules code for the RouterService to run on different intervals.
     * Every hour, if we're an externally contactable ultrapeer, we'll tell the GWebCache servers our IP address.
     * Every minute, we'll move the hosts that refused us in the handshake back onto the main list where we can try them again.
     * Every 2 minutes, the RouterService will call Bootstrapper.run(), which uses the GWebCaches.
     * 
     * RouterService.start() calls this when the program runs.
     */
    public void initialize() {

        // Setup the run() methods the RouterService will call
        LOG.trace("START scheduling");
        scheduleServices();
    }

    /**
     * Schedules code for the RouterService to run on different intervals.
     * Every hour, if we're an externally contactable ultrapeer, we'll tell the GWebCache servers our IP address.
     * Every minute, we'll move the hosts that refused us in the handshake back onto the main list where we can try them again.
     * Every 2 minutes, the RouterService will call Bootstrapper.run(), which uses the GWebCaches.
     * 
     * Only initialize() above calls this.
     */
    protected void scheduleServices() {

        /*
         * Register to send updates every hour (starting in one hour) if we're a
         * supernode and have accepted incoming connections.  I think we should
         * only do this if we also have incoming slots, but John Marshall from
         * Gnucleus says otherwise.
         */

        // Define a new Runnable class named updater right here
        Runnable updater = new Runnable() {

            /**
             * If we're an externally contactable ultrapeer, tell the GWebCaches our IP address and port number.
             * The RouterService will have a thread call this run() method every hour.
             */
            public void run() {

                // Only do something if we're an externally contactable ultrapeer
                if (RouterService.acceptedIncomingConnection() && RouterService.isSupernode()) {

                    // Get our IP address and port number
                    byte[] addr = RouterService.getAddress();
                    int port = RouterService.getPort();

                    // If our IP address and port number look valid
                    if (NetworkUtils.isValidAddress(addr) && NetworkUtils.isValidPort(port) && !NetworkUtils.isPrivateAddress(addr)) {

                        // Wrap them in a new Endpoint object
                        Endpoint e = new Endpoint(addr, port);

                        /*
                         * This spawns another thread, so blocking is not an issue.
                         */

                        // Tell the GWebCache servers our IP address
                        gWebCache.sendUpdatesAsync(e);
                    }
                }
            }
        };

        // Have the RouterService execute the run() method above 1 hour from now, and every hour after that
        RouterService.schedule(updater, BootstrapServerManager.UPDATE_DELAY_MSEC, BootstrapServerManager.UPDATE_DELAY_MSEC);

        // Define a new Runnable class named probationRestorer right here
        Runnable probationRestorer = new Runnable() {

            /**
             * Restore all the hosts we put on probation.
             * 
             * If we connected to a remote computer, but then it refused us in the Gnutella handshake, we added it to the PROBATION_HOSTS list.
             * The RouterService will call this run() method every minute.
             * It moves all the hosts on the PROBATION_HOSTS list back onto the main list.
             */
            public void run() {

                // Make a note in the debugging log
                LOG.trace("restoring hosts on probation");

                // Synchronize on the HostCatcher object before accessing the PROBATION_HOSTS list
                synchronized (HostCatcher.this) {

                    // Loop through each host in the probation hosts list
                    Iterator iter = PROBATION_HOSTS.iterator();
                    while (iter.hasNext()) {

                        // Move it back onto the main list
                        Endpoint host = (Endpoint)iter.next();
                        add(host, false);
                    }

                    // Now that we've copied all the hosts back on the main list, clear the probation list
                    PROBATION_HOSTS.clear();
                }
            }
        };

        // Have the router service move the hosts on the probation list back to the main list every minute
        RouterService.schedule(probationRestorer, PROBATION_RECOVERY_WAIT_TIME, PROBATION_RECOVERY_TIME);

        /*
         * Try to fetch GWebCache's whenever we need them.
         * Start it immediately, so that if we have no hosts
         * (because of a fresh installation) we will connect.
         */

        // Have the router service call Bootstrapper.run() every 2 minutes
        RouterService.schedule(FETCHER, 0, 2 * 1000);

        // We're done setting up the methods that tell GWebCaches our IP address and restore the hosts we put on probation
        LOG.trace("STOP scheduling");
    }

    /**
     * Send a UDP Gnutella ping packet to all the PCs from the gnutella.net file, using them as though they were UDP host caches.
     */
    public void sendUDPPings() {

        // We need the lock on this object so that we can copy ENDPOINT_SET
        synchronized (this) {

            // Send a UDP Gnutella ping packet to all the remote computers in our list (do) does this really try to contact all of them?
            rank(new HashSet(ENDPOINT_SET)); // This uses these Gnutella PCs as though they were UDP host caches
        }
    }

    /**
     * Send a UDP Gnutella ping packet to the IP addresses of some remote computers running Gnutella software.
     * These remote computers aren't UDP host caches, but if they're running modern Gnutella software like LimeWire, we can talk to them as though they were.
     * We don't have TCP socket connections to these remote computers and aren't connecting to them here, we're just using them as UDP host caches.
     * 
     * Pong ranking is the process of deciding which remote Gnutella computers to try to connect to.
     * To get the IP addresses of some highly-ranked ones, we'll talk to some remote Gnutella computers as though they are UDP host caches.
     * 
     * @param hosts A list of ExtendedEndpoint objects that have the IP addresses of remote computers on the Internet running Gnutella software
     */
    private void rank(Collection hosts) {

        // If we're not connected to the Gnutella network yet, we can justify bothering remote computers for more IP addresses to try
        if (needsPongRanking()) {

            /*
             * pinger.rank() sends a UDP Gnutella ping packet to all the IP addreses in the list.
             * The list can be of UDP host caches, or just regular Gnutella PCs.
             * LimeWire supports UDP Gnutella ping and pong packets, and will respond just like a UDP host cache.
             * 
             * In this method, the hosts list is always a list of regular Gnutella PCs, not UDP host caches.
             */

            // Send a Gnutella ping packet to the group of computers over UDP
            pinger.rank(

                // The IP addresses of the PCs to contact
                hosts,

                // The pinger will wait a little while, then call this isCancelled() method to see if we actually want to do this
                new Cancellable() {

                    /**
                     * Tell the pinger if it should contact the PCs or not.
                     * It calls this method after waiting for enough time to expire to not contact computers too quickly, but before it contacts this batch.
                     * 
                     * @return True if we're fully connected to the Gnutella network, it would be bad to bother more computers, and the pinger should not do this.
                     *         False if we're still not connected to the Gnutella network, and the pinger should go ahead with the plan.
                     */
                    public boolean isCancelled() {

                        // If we connected to the Gnutella network while the pinger was waiting, cancel the request
                        return !needsPongRanking();
                    }
                }
            );
        }
    }

    /**
     * Determine if we are allowed to contact UDP host caches right now.
     * 
     * Pong ranking is the process of deciding which remote Gnutella computers to try to connect to.
     * To get the IP addresses of some highly-ranked ones, we'll talk to UDP host caches.
     * 
     * @return True if we don't have many Gnutella connections yet and talking to some UDP host caches still makes sense
     *         False if we're fully connected to the Gnutella network, so it would be bad for us to bother UDP host caches now
     */
    private boolean needsPongRanking() {

        // If we have all the Gnutella connections we need, we wouldn't benefit from contacting more UDP host caches, and shouldn't bother them
        if (RouterService.isFullyConnected()) return false;

        // If we have more than 5 Gnutella connections, we wouldn't benefit from contacting more UDP host caches, and shouldn't bother them
        int have = RouterService.getConnectionManager().getInitializedConnections().size();
        if (have >= MAX_CONNECTIONS) return false;

        // There's only a 20 second interval at the start during which we're allowed to contact UDP host caches
        long now = System.currentTimeMillis();
        if (now > lastAllowedPongRankTime) return false; // It's over, we can't bother them

        // Find out how many hosts in our list want to connect to a computer that's in the same network role as us
        int size;
        if (RouterService.isSupernode()) size = FREE_ULTRAPEER_SLOTS_SET.size(); // We're an ultrapeer, set size to the number that want ultrapeer connections
        else size = FREE_LEAF_SLOTS_SET.size();                                  // We're a leaf, set size to the number that want leaf connections

        // Find out how many ultrapeers we're trying to be connected to
        int preferred = RouterService.getConnectionManager().getPreferredConnectionCount();

        // Return true if we need more additional connections than we have hosts in the list
        return size < preferred - have; // The number (preferred - have) is how many additional ultrapeer connections we need
    }

    /**
     * Read the gnutella.net file, turning lines of text with IP addresses and port numbers into ExtendedEndpoint objects in the list the HostCatcher keeps.
     * Package access for testing.
     * 
     * @param hostFile A File object holding a path like "C:\Documents and Settings\kfaaborg\.limewire\gnutella.net"
     */
    synchronized void read(File hostFile) throws FileNotFoundException, IOException {

        // Record in the debugging log that we're going to start reading the gnutella.net file
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

                    // See if the gWebCache manager will accept this line as being about a GWebCache
                    gWebCache.addBootstrapServer(new BootstrapServer(line));

                    // The gWebCache object accepted it, go to the start of the loop to read the next line
                    continue;

                // The gWebCache object threw a ParseException because line isn't about a GWebCache, keep going
                } catch (ParseException ignore) {}

                try {

                    // The line is about a UDP host cache or a PC running Gnutella software, add it to the list
                    add(ExtendedEndpoint.read(line), NORMAL_PRIORITY);

                // If reading that line threw an exception, go to the top of the loop to try the next line
                } catch (ParseException pe) { continue; }
            }

        } finally {

            // Tell the object that manages GWebCache servers for us that we're done telling it about all of those we know about
            gWebCache.bootstrapServersAdded();

            // If the UDPHostCache object's list is empty, add some hard-coded IP addresses
            udpHostCache.hostCachesAdded();

            try {

                // Close the gnutella.net file
                if (in != null) in.close();

            } catch (IOException e) {}
        }

        // We're done reading the gnutella.net file
        LOG.trace("left HostCatcher.read(File)");
    }

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

    /*
     * ///////////////////////////// Add Methods ////////////////////////////
     */

    /**
     * Adds all the IP addresses and port numbers in a Gnutella pong packet we've received.
     * 
     * @param pr The Gnutella pong packet that contains the IP address and port number we want to add.
     * @return   True if we got some IP addresses and port numbers for our list.
     *           False if we didn't add any because we already have them.
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

        /*
         * if the pong carried packed IP/Ports, add those as their own
         * endpoints.
         */

        // Send a UDP Gnutella ping packet to all the IP addresses in the pong, using those Gnutella PCs as though they were UDP host caches
        rank(pr.getPackedIPPorts());

        // Loop through the IP addresses and port numbers in the pong packet
        for (Iterator i = pr.getPackedIPPorts().iterator(); i.hasNext(); ) {
            IpPort ipp = (IpPort)i.next();

            // Make an ExtendedEndpoint out of it
            ExtendedEndpoint ep = new ExtendedEndpoint(ipp.getAddress(), ipp.getPort());

            // If the IP address and port number look valid, add it to the permanentHosts list which gets written to the gnutella.net file
            if (isValidHost(ep)) add(ep, GOOD_PRIORITY);
        }

        // If the pong has information about UDP host caches, add them too
        for (Iterator i = pr.getPackedUDPHostCaches().iterator(); i.hasNext(); ) {
            IpPort ipp = (IpPort)i.next();

            // Make an ExtendedEndpoint out of it
            ExtendedEndpoint ep = new ExtendedEndpoint(ipp.getAddress(), ipp.getPort());
            ep.setUDPHostCache(true); // Mark it as holding the IP address and port number of a UDP host cache

            // Add it to the list of UDP host caches that the UDPHostCache object keeps
            addUDPHostCache(ep);
        }

        // If the pong packet is from a UDPHostCache, add it to our list of UDP host caches
        if (endpoint.isUDPHostCache()) return addUDPHostCache(endpoint);

        /*
         * This pong packet is from a Gnutella PC, not a UDP host cache.
         * Add it to our list, marking it as high priority if it's from an ultrapeer.
         */

        // An ultrapeer sent us this pong packet
        if (pr.isUltrapeer()) {

            // The ultrapeer that sent us this pong packet has free slots for leaves
            if (pr.hasFreeLeafSlots()) {

                // Add its IP address and port number to our list of remote Gnutella comptuers with free leaf slots
                addToFixedSizeSet(endpoint, FREE_LEAF_SLOTS_SET);

                // If the ultrapeer that sent us this pong packet doesn't have free slots for ultrapeers, leave now
                if (!pr.hasFreeUltrapeerSlots()) {

                    // We added some IP addresses and port numbers to our list
                    return true;
                }
            }

            /*
             * An ultrapeer sent us this pong packet.
             * It doesn't have free slots for leaves.
             * Or, it does, and also has free slots for ultrapeers.
             */

            // Add it to our free leaf slots list if it has free leaf slots and
            // is an Ultrapeer.
            
            // If the ultrapeer that sent us this packet has free ultrapeer slots, or if our language preferences match and it has free locale preferenced slots
            if (pr.hasFreeUltrapeerSlots() || (ApplicationSettings.LANGUAGE.getValue().equals(pr.getClientLocale()) && pr.getNumFreeLocaleSlots() > 0)) {

                // Add its IP address and port number to our list of remote Gnutella computers with free ultrapeer slots
                addToFixedSizeSet(endpoint, FREE_ULTRAPEER_SLOTS_SET);
                return true;
            }

            // Add the IP address of the ultrapeer that sent us this pong packet to our list, marking it as good priority
            return add(endpoint, GOOD_PRIORITY);

        // The pong packet is from a leaf
        } else {

            // Add the IP address of the leaf that sent us this pong packet to our list, marking it as normal priority
            return add(endpoint, NORMAL_PRIORITY);
        }
    }

    /**
     * Gives an ExtendedEndpoint to the UDPHostCache object.
     * Calls udpHostCache.add(host).
     * 
     * @param host An ExtendedEndpoint we got from the gnutella.net file or a pong packet that describes the IP address and port number of a UDP host cache
     * @return     True if it added it, false if it already had it
     */
    private boolean addUDPHostCache(ExtendedEndpoint host) {

        // Have the UDPHostCache object take this one
        return udpHostCache.add(host);
    }

    /**
     * Add an ExtendedEndpoint to a list of them like FREE_LEAF_SLOTS_SET or FREE_ULTRAPEER_SLOTS_SET.
     * Only the add() method above uses this method.
     * 
     * @param host  The ExtendedEndpoint to add
     * @param hosts The Set to add it to
     */
    private synchronized void addToFixedSizeSet(ExtendedEndpoint host, Set hosts) {

        // Don't allow the free slots host to expand infinitely.
        
        // Add host to the Set, if it took it, and now holds more than 200 objects
        if (hosts.add(host) && hosts.size() > 200) {

            // Remove one to bring the total back down to 200
            hosts.remove(hosts.iterator().next());
        }

        // Also add it to the list of permanent hosts stored on disk.

        // Also add host to the permanentHosts list which gets written to the gnutella.net file
        addPermanent(host);

        // Wake up the the thread sleeping in getAnEndpoint(), there will be one for it to get now
        notify();
    }

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

    /**
     * Add a list of IP addresses and port numbers of remote computers running Gnutella software to the list of them we keep and write to the gnutella.net file.
     * If the list is full, adding these may eject some.
     * 
     * This method is used to bring in the IP addresses and port numbers a remote computer told us during the Gnutella handshake.
     * ConnectionManager.updateHostCache(HandshakeResponse) calls this.
     * 
     * @param endpoints A list of ExtendedEndpoint objects made from the IP addresses and port numbers in a remote computer's handshake headers
     */
    public void add(Collection endpoints) {

        // Send a UDP Gnutella ping packet to these remote computers, using them as though they are UDP host caches
        rank(endpoints);

        // Add all the given ExtendedEndpoint objects to the list with GOOD_PRIORITY
        for (Iterator i = endpoints.iterator(); i.hasNext(); ) add((Endpoint)i.next(), true); // True to list these has having better than normal priority
    }

    /**
     * Add an IP address and port number of a remote computer running Gnutella software to the list of them we keep and write to the gnutella.net file.
     * If the list is full, adding this one will eject one.
     * 
     * This method is used to bring in the IP addresses and port numbers a remote computer told us during the Gnutella handshake.
     * 
     * @param e                 An ExtendedEndpoint with the IP address and port number to add
     * @param forceHighPriority True to mark the information as good priority in the list, false to just mark it normal priority
     * @return                  True if we added e, false if we didn't
     */
    public boolean add(Endpoint e, boolean forceHighPriority) {

        // Make sure the IP address and port number look valid
        if (!isValidHost(e)) return false;

        // Add the ExtendedEndpoint with good or normal priority, depending on what the caller wants
        if (forceHighPriority) return add(e, GOOD_PRIORITY);
        else                   return add(e, NORMAL_PRIORITY);
    }

    /**
     * Add the IP address and port number of a remote computer running Gnutella software to the list of them the HostCatcher object keeps.
     * Use this method if you know the language preference of the remote computer.
     * ConnectionManager.disconnect() and ConnectionManager.ConnectionFetcher.managedRun() use this method.
     * 
     * @param e                 The ExtendedEndpoint to add
     * @param forceHighPriority True to list with GOOD_PRIORITY, false to use NORMAL_PRIORITY
     * @param locale            The computer's language preference, like "en" for English
     * @return                  True if we added e, false if we didn't
     */
    public boolean add(Endpoint e, boolean forceHighPriority, String locale) {

        // Make sure the IP address and port number look valid
        if (!isValidHost(e)) return false;

        // Add the IP address and port number along with the given language preference and priority level
        if (forceHighPriority) return add(new ExtendedEndpoint(e.getAddress(), e.getPort(), locale), GOOD_PRIORITY);
        else                   return add(new ExtendedEndpoint(e.getAddress(), e.getPort(), locale), NORMAL_PRIORITY);
    }

    /**
     * Add the IP address and port number of a remote computer running Gnutella software to the list of them the HostCatcher object keeps.
     * 
     * @param host     The IP address and port number in an Endpoint or ExtendedEndpoint object
     * @param priority The priority level, like NORMAL_PRIORITY or GOOD_PRIORITY
     * @return         True if we added e, false if we didn't
     */
    public boolean add(Endpoint host, int priority) {

        // Make a note in the debugging log
        if (LOG.isTraceEnabled()) LOG.trace("adding host " + host);

        // If the given object is an ExtendedEndpoint, add it
        if (host instanceof ExtendedEndpoint) return add((ExtendedEndpoint)host, priority);

        // It's just an Endpoint, not an ExtendedEndpoint, make an ExtendedEndpoint from it and add that
        return add(new ExtendedEndpoint(host.getAddress(), host.getPort()), priority);
    }

    /**
     * Add e to the permanentHosts list which gets written to the gnutella.net file.
     * Adds it to the HashSet ENDPOINT_SET and BucketQueue ENDPOINT_QUEUE lists the program keeps in memory.
     * 
     * @param e        An ExtendedEndpoint object
     * @param priority GOOD_PRIORITY if this is the IP address and port number of an ultrapeer, NORMAL_PRIORITY if it's a leaf
     * @return         True if we added it, false if we didn't because we already have it
     */
    private boolean add(ExtendedEndpoint e, int priority) {

        // If in test mode, make sure the lists are OK
        repOk();

        // If the line from gnutella.net described a UDP host cache, have the udpHostCache take it
        if (e.isUDPHostCache()) return addUDPHostCache(e); // The udpHostCache.add(e) returns true if it didn't have it and added it

        /*
         * Add to permanent list, regardless of whether it's actually in queue.
         * Note that this modifies e.
         */

        // Add host to the permanentHosts list which gets written to the gnutella.net file
        addPermanent(e);

        // Make a boolean for the value we'll return
        boolean ret = false; // No, we haven't added it yet

        // Make sure only one thread can access the ENDPOINT_QUEUE and ENDPOINT_SET lists at a time
        synchronized (this) {

            // If we don't have this IP address and port number in our list yet
            if (!(ENDPOINT_SET.contains(e))) {

                // We're going to add it, and will return true
                ret = true;

                /*
                 * Add to temporary list. Adding e may eject an older point from
                 * queue, so we have to cleanup the set to maintain
                 * rep. invariant.
                 */

                // Add e to the list of hosts we can try to connect to
                ENDPOINT_SET.add(e); // Add it to the HashSet we used to detect new duplicates
                Object ejected = ENDPOINT_QUEUE.insert(e, priority); // It will go into the top of the bucket for the specified priority

                // The bucket for that priority was full, the oldest one at the bottom was pushed out
                if (ejected != null) {

                    // Remove it from the HashSet to keep exactly the same ExtendedEndpoint objects in both lists
                    ENDPOINT_SET.remove(ejected);
                }

                // Wake up the thread waiting in getAnEndpoint(), the list has one for it now
                this.notify();
            }
        }

        // If in test mode, make sure the lists are OK
        repOk();

        // Return true if we added e, false if we didn't because we already had it
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

    /**
     * Remove an IP address and port number from the list of them the HostCatcher object keeps.
     * 
     * @param e An ExtendedEndpoint to remove from the list
     * @return  True if we found e and removed it, false if not found
     */
    private synchronized boolean removePermanent(ExtendedEndpoint e) {

        // Remove e from both the FixedsizePriorityQueue and the HashSet
        boolean removed1 = permanentHosts.remove(e);
        boolean removed2 = permanentHostsSet.remove(e);

        // Make sure it was found in both or not found in both
        Assert.that(removed1 == removed2, "Queue " + removed1 + " but set " + removed2);

        // If we found and removed e, the list has changed and we need to save it to the gnutella.net file
        if (removed1) dirty = true;

        // Return true if we found and removed e, false if not found
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
     * @return     True if the IP address and port number passes all the tests and we'll add it to our list.
     *             False if it fails a test and we shouldn't add it.
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

    /*
     * ///////////////////////////////////////////////////////////////////////
     */

    /**
     * Picks a host from the list the HostCatcher keeps of IP addreses and port numbers of remote computers running Gnutella software on the internet.
     * Removes it from the list and returns it.
     * If the list is empty, blocks here until another thread adds an IP address and port number for us to take. 
     * 
     * Picks the highest priority host we have.
     * Takes ultrapeer and leaf mode into account, and also language preferencing.
     * 
     * The caller should call doneWithConnect() and doneWithMessageLoop() when done with the returned value. (do)
     * 
     * @return An ExtendedEndpoint from our list that you can try to connect to
     */
    public synchronized Endpoint getAnEndpoint() throws InterruptedException {

        // Loop until we get an ExtendedEndpoint from the list, waiting until another thread adds one if necessary
        while (true) {

            try {

                /*
                 * note : if this succeeds with an endpoint, it
                 * will return it.  otherwise, it will throw
                 * the exception, causing us to fall down to the wait.
                 * the wait will be notified to stop when something
                 * is added to the queue
                 * (presumably from fetchEndpointsAsync working)               
                 */

                // Pick an ExtendedEndpoint from our list, taking ultrapeer mode and language preferencing into account
                return getAnEndpointInternal();

            // The list is empty, ignore the exception and keep going
            } catch (NoSuchElementException e) {}

            // Have this thread wait here and try again when the list isn't empty anymore
            try {

                // Record that we're one more catcher waiting here in getAnEndpoint() for the program to add one to the list
                _catchersWaiting++;

                // Have this thread wait here until add() or addToFixedSizeSet() adds an ExtendedEndpoint for us to grab
                wait(); // This is the method that can throw InterruptedException

            // Another thread added an ExtendedEndpoint to the list and called notify()
            } finally {

                // Record that we're not waiting here anymore
                _catchersWaiting--;
                
                /*
                 * Loop back up to the top to try again.
                 */
            }
        } 
    }

    /**
     * Call this when you've finished trying to connect to a given host.
     * Makes a note that we had another success or failure with this host.
     * That information will get saved into the gnutella.net file.
     * 
     * ConnectionManager.ConnectionFetcher.managedRun() calls this.
     * If the remote computer refuses us in the Gnutella handshake, managedRun() still calls doneWithConnect(e, true).
     * It passes true because we established a TCP socket connection.
     * 
     * @param e       The IP address and port number you tried to connect to.
     *                The getAndEndpoint() method gave you this information.
     * @param success True if you established an outgoing TCP socket connection, false if you were unable to connect.
     */
    public synchronized void doneWithConnect(Endpoint e, boolean success) {

        /*
         * Normal host: update key.  TODO3: adjustKey() operation may be more
         * efficient.
         * 
         * Should never happen, but I don't want to update public
         * interface of this to operate on ExtendedEndpoint.
         */

        // Make sure e is an ExtendedEndpoint, not just an Endpoint, and cast it up to one
        if (!(e instanceof ExtendedEndpoint)) return;
        ExtendedEndpoint ee = (ExtendedEndpoint)e;

        // Remove it from the list to edit it
        removePermanent(ee);

        // We connected to this computer
        if (success) {

            // Increment the ExtendedEndpoint object's count of successes
            ee.recordConnectionSuccess();

        // We tried to connect to this computer, but our attempt timed out
        } else {

            // Increment our total count of failures
            _failures++;

            // Increment the ExtendedEndpoint object's count of failures
            ee.recordConnectionFailure();
        }

        // Add it back to the list
        addPermanent(ee);
    }

    /**
     * Have this HostCatcher pick a host from its list that you can try to connect to.
     * Removes the ExtendedEndpoint from the list and returns it.
     * 
     * Returns the best host we have information about.
     * Takes our role as an ultrapeer or a leaf into account, as well as matching language preference.
     * 
     * Synchronize on this object before calling this method.
     * Only getAnEndpoint() calls this method.
     * 
     * @return An ExtendedEndpoint from our list that you can try connecting to
     * @throws NoSuchElementException if we don't have an ExtendedEndpoint to return
     */
    private ExtendedEndpoint getAnEndpointInternal() throws NoSuchElementException {

        // If we're already an ultrapeer and we know about hosts with free ultrapeer slots, try them
        if (RouterService.isSupernode() && !FREE_ULTRAPEER_SLOTS_SET.isEmpty()) {

            // From the list of hosts advertising free ultrapeer slots, choose one that matches our language preference
            return preferenceWithLocale(FREE_ULTRAPEER_SLOTS_SET);

        // If we're already a leaf and we know about ultrapeers with free leaf slots, try one of those
        } else if (RouterService.isShieldedLeaf() && !FREE_LEAF_SLOTS_SET.isEmpty()) {

            // From the list of hosts advertising free leaf slots, choose one that matches our language preference
            return preferenceWithLocale(FREE_LEAF_SLOTS_SET);

        // If we know about remote computers that want more ultrapeer connections
        } else if (!FREE_ULTRAPEER_SLOTS_SET.isEmpty()) {

            /*
             * Otherwise, assume we'll be a leaf and we're trying to connect, since
             * this is more common than wanting to become an ultrapeer and because
             * we want to fill any remaining leaf slots if we can.
             */

            // From the list of hosts advertising free ultrapeer slots, choose one that matches our language preference
            return preferenceWithLocale(FREE_ULTRAPEER_SLOTS_SET);

        // If we know about remote computers that want more leaf connections
        } else if (!FREE_LEAF_SLOTS_SET.isEmpty()) {

            /*
             * Otherwise, might as well use the leaf slots hosts up as well
             * since we added them to the size and they can give us other info
             */

            // Pick a host that wants another leaf
            Iterator iter = FREE_LEAF_SLOTS_SET.iterator();      // Get an iterator on the list of hosts with free leaf slots
            ExtendedEndpoint ee = (ExtendedEndpoint)iter.next(); // Pick the first one
            iter.remove();                                       // Remove it from the FREE_LEAF_SLOTS_SET list
            return ee;                                           // Return it
        }

        // If our list of hosts to try to connect to still has at least one ExtendedEndpoint in it
        if (!ENDPOINT_QUEUE.isEmpty()) {

            // Remove and return the lowest priority element that we added most recently
            ExtendedEndpoint e = (ExtendedEndpoint)ENDPOINT_QUEUE.extractMax(); // Remove and return the lowest priority element that we added most recently
            boolean ok = ENDPOINT_SET.remove(e);                                // Also remove it from the HashSet so it still has exactly the same contents
            Assert.that(ok, "Rep. invariant for HostCatcher broken.");          // Make sure that e was actually in the HashSet
            return e;                                                           // Return it

        // Our list of hosts to try to connect to is empty
        } else {

            // We don't have an ExtendedEndpoint to return, throw an exception instead
            throw new NoSuchElementException();
        }
    }

    /**
     * Picks an ExtendedEndpoint from the list for us to try to connect to.
     * Chooses one from the set you pass it.
     * Tries to pick one that matches our language preference.
     * 
     * @param base A selection of the ExtendedEndpoints in the list for us to choose from
     * @return     An ExtendedEndpoint from the list that we can try connecting to
     */
    private ExtendedEndpoint preferenceWithLocale(Set base) {

        // Get our language preference, like "en" for English
        String loc = ApplicationSettings.LANGUAGE.getValue();

        /*
         * Preference a locale host if we haven't matched any locales yet
         */

        // If we don't have any connections that match our language preference yet
        if (!RouterService.getConnectionManager().isLocaleMatched()) {

            // If we have an ExtendedEndpoint in our list that matches our language preference
            if (LOCALE_SET_MAP.containsKey(loc)) {

                // Loop through the ExtendedEndpoint objects in that part of the list
                Set locales = (Set)LOCALE_SET_MAP.get(loc);         // Point locales at the set of ExtendedEndpoint objects that match our language preference
                for (Iterator i = base.iterator(); i.hasNext(); ) { // Loop through the ExtendedEndpoint objects in the given list
                    Object next = i.next();                         // Get the next object in the given list

                    // We found an ExtendedEndpoint object that's both in the given base list, and in the matching language preference list
                    if (locales.contains(next)) {

                        // Remove it from the given list and the language preference list, and return it
                        i.remove();
                        locales.remove(next);
                        return (ExtendedEndpoint)next;
                    }
                }
            }
        }

        /*
         * We do have some connections that match our language preference, or we don't and,
         * we couldn't find one from base that did.
         */

        // Just pick one from the given base list
        Iterator iter = base.iterator();                     // Get an iterator to move through the base list
        ExtendedEndpoint ee = (ExtendedEndpoint)iter.next(); // Get the first ExtendedEndpoint from that list
        iter.remove();                                       // Remove it from the list
        return ee;                                           // Return it
    }

    /**
     * The number of hosts in our list.
     * This includes ultrapeers and leaves.
     * It's a total of the ExtendedEndpoint objects in our main list, and those in our free leaf and free ultrapeer slots lists.
     * 
     * @return The total number of computers we have in our list
     */
    public synchronized int getNumHosts() {

        // The total number of hosts in our list is the total size of the main list and the free leaf and ultrapeer slots lists
        return
            ENDPOINT_QUEUE.size() +          // The number of hosts in the main list, and
            FREE_LEAF_SLOTS_SET.size() +     // The number of ultrapeers that tell us they want more leaves, and
            FREE_ULTRAPEER_SLOTS_SET.size(); // The number of ultrapeers that tell us they want more ultrapeers
    }

    /**
     * The number of ultrapeers in our list.
     * 
     * @return The number of computers we have in our list that we know are running in ultrapeer mode
     */
    public synchronized int getNumUltrapeerHosts() {

        // Total up the number of ultrapeers that we have the IP addresses and port numbers of
        return
            ENDPOINT_QUEUE.size(GOOD_PRIORITY) + // The number of ultrapeers in the main list, and
            FREE_LEAF_SLOTS_SET.size() +         // The number of ultrapeers that tell us they want more leaves, and
            FREE_ULTRAPEER_SLOTS_SET.size();     // The number of ultrapeers that tell us they want more ultrapeers
    }

    /**
     * Get an iterator you can use to move down the permanent hosts list.
     * This is the list we write to the gnutella.net file.
     * The hosts will be in priority order, from worst to best.
     * 
     * This method exists for testing.
     * Do not modify the list while you are using the iterator.
     * 
     * @return An Iterator on the permanentHosts list
     */
    Iterator getPermanentHosts() {

        // Let the caller move down the permanentHosts list for testing purposes
        return permanentHosts.iterator();
    }

    /**
     * Make a list of hosts to try that have free ultrapeer slots, choosing ones that match our language preference first.
     * 
     * @param num How many hosts we want in the list
     * @return    A new Collection of ExtendedEndpoint objects from the list
     */
    public synchronized Collection getUltrapeersWithFreeUltrapeerSlots(int num) {

        // Choose only hosts that want more ultrapeer connections, and pick those that match our language preference first
        return getPreferencedCollection(FREE_ULTRAPEER_SLOTS_SET, ApplicationSettings.LANGUAGE.getValue(), num);
    }

    /**
     * Make a list of hosts to try that have free ultrapeer slots, choosing ones that match the given language preference first.
     * 
     * @param locale The language preference we want, like "en" for English
     * @param num    How many hosts we want in the list
     * @return       A new Collection of ExtendedEndpoint objects from the list
     */
    public synchronized Collection getUltrapeersWithFreeUltrapeerSlots(String locale, int num) {

        // Choose only hosts that want more ultrapeer connections, and pick those that match the given language preference first
        return getPreferencedCollection(FREE_ULTRAPEER_SLOTS_SET, locale, num);
    }

    /**
     * Make a list of hosts to try that have free leaf slots, choosing ones that match our language preference first.
     * 
     * @param num How many hosts we want in the list
     * @return    A new Collection of ExtendedEndpoint objects from the list
     */
    public synchronized Collection getUltrapeersWithFreeLeafSlots(int num) {

        // Choose only hosts that want more leaf connections, and pick those that match our language preference first
        return getPreferencedCollection(FREE_LEAF_SLOTS_SET, ApplicationSettings.LANGUAGE.getValue(), num);
    }

    /**
     * Make a list of hosts to try that have free leaf slots, choosing ones that match the given language preference first.
     * 
     * @param locale The language preference we want, like "en" for English
     * @param num    How many hosts we want in the list
     * @return       A new Collection of ExtendedEndpoint objects from the list
     */
    public synchronized Collection getUltrapeersWithFreeLeafSlots(String locale, int num) {

        // Choose only hosts that want more leaf connections, and pick those that match the given language preference first
        return getPreferencedCollection(FREE_LEAF_SLOTS_SET, locale, num);
    }

    /**
     * Prepare a collection of hosts for us to try, choosing from among a given base list and trying to match our language preference.
     * 
     * @param base A list of ExtendedEndpoint objects to choose from
     * @param loc  Our language preference, like "en" for English
     * @param num  The number of ExtendedEndpoint objects we want in the returned list
     * @return     A new Collection of ExtendedEndpoint objects chosen from base, most of which match our language preference
     */
    private Collection getPreferencedCollection(Set base, String loc, int num) {

        // If the caller didn't give us a language preference, use our own, like "en" for English
        if (loc == null || loc.equals("")) loc = ApplicationSettings.DEFAULT_LOCALE.getValue();

        // Make a new empty HashSet called hosts that can hold up to num ExtendedEndpoint objects
        Set hosts = new HashSet(num);
        Iterator i;

        // Point locales at the Set of hosts in our list that have that language preference
        Set locales = (Set)LOCALE_SET_MAP.get(loc);

        // If we have some hosts with our language preference
        if (locales != null) {

            // Loop through them, stopping when we run out or hosts grows to hold num
            for (i = locales.iterator(); i.hasNext() && hosts.size() < num; ) {
                Object next = i.next();

                // If this host is in the given base list, add it to the hosts list we're putting together
                if (base.contains(next)) hosts.add(next);
            }
        }

        // Copy ExtendedEndpoint objects from base to hosts to fill up the remaining space
        for (i = base.iterator(); i.hasNext() && hosts.size() < num;) {

            hosts.add(i.next());
        }

        // Return the list we put together
        return hosts;
    }

    /**
     * Reads in the gnutella.net file.
     * 
     * If it's been more than a week since we've contacted the GWebCache servers, contacts them.
     * Reads everything from the gnutella.net file.
     * Starts the 20 second period during which we're allowed to contact UDP host caches, and schedules a call to pinger.resetData() for the end of that period.
     * 
     * ConnectionManger.connect() calls this.
     * 
     * Notifies this that connect() has been called.
     * This may decide to give out bootstrap pongs if necessary.
     */
    public synchronized void expire() {

        /*
         * Fetch more GWebCache urls once per session.
         * (Well, once per connect really--good enough.)
         */

        // Get the time now, and the time when we last bothered the GWebCaches
        long now = System.currentTimeMillis();
        long fetched = ConnectionSettings.LAST_GWEBCACHE_FETCH_TIME.getValue(); // Stored in settings so we have data from the previous times the program has run

        // If it's been a week since we bothered the GWebCaches
        if (fetched + DataUtils.ONE_WEEK <= now ) {

            // Contact the GWebCaches to get more hosts for us to try
            if (LOG.isDebugEnabled()) LOG.debug("Fetching more bootstrap servers. " + "Last fetch time: " + fetched);
            gWebCache.fetchBootstrapServersAsync();
        }

        // Clear our records of what's happened with the host list, and read it from the gnutella.net file again
        recoverHosts();

        // For the next 20 seconds, we're allowed to contact UDP host caches
        lastAllowedPongRankTime = now + PONG_RANKING_EXPIRE_TIME;

        /*
         * schedule new runnable to clear the set of endpoints that
         * were pinged while trying to connect
         */

        // Have the router service run this code once, 20 seconds from now
        RouterService.schedule(

            // Define a new unnamed class that implements the Runnable interface right here
            new Runnable() {

                /**
                 * Clear our record of the IP addresses of UDP host caches that we sent UDP Gnutella ping packets to.
                 * The router service will run this code one time, 20 seconds from now.
                 */
                public void run() {

                    // Clear our record of the IP addresses of UDP host caches that we sent UDP Gnutella ping packets to
                    pinger.resetData();
                }
            },

            // Run this once 20 seconds from now
            PONG_RANKING_EXPIRE_TIME,

            // Don't run it after that
            0
        );
    }

    /**
     * Clear the list of hosts the HostCatcher keeps.
     * 
     * A host is our record of an IP address and port number of a remote computer that may be online running Gnutella software right now.
     * We keep this information in an ExtendedEndpoint object, and then keep the ExtendedEndpoint objects in lists.
     * The main lists are ENDPOINT_QUEUE and ENDPOINT_SET.
     * Some of the hosts in those lists are also listed in FREE_LEAF_SLOTS_SET and FREE_ULTRAPEER_SLOTS_SET.
     * This method clears all of them.
     */
    public synchronized void clear() {

        // Clear our lists of the computers we know want more leaf and ultrapeer connections
        FREE_LEAF_SLOTS_SET.clear();
        FREE_ULTRAPEER_SLOTS_SET.clear();

        // Clear our list of hosts itself
        ENDPOINT_QUEUE.clear(); // The main list of hosts
        ENDPOINT_SET.clear();   // The same hosts in a HashSet to detect duplicates easily
        
        /*
         * Doesn't delete permanentHosts or permanentHostsSet
         */
    }

    /**
     * Get the UniqueHostPinger object the HostCatcher uses to send a UDP Gnutella ping packet to a list of IP addresses and port numbers.
     * 
     * @return A reference to the HostCatcher's UniqueHostPinger object
     */
    public UDPPinger getPinger() {

        // Return a reference to the object we made in the constructor
        return pinger;
    }

    /**
     * Express the hosts in the list as text.
     * Lists the hosts in ENDPOINT_QUEUE and permanentHosts.
     * 
     * @return Text like (do)
     */
    public String toString() {

        // Compose text from the ENDPOINT_QUEUE and permanentHosts lists
        return "[volatile:" + ENDPOINT_QUEUE.toString() + ", permanent:" + permanentHosts.toString() + "]";
    }

    /**
     * False, don't take the time to check the lists are not corrupted.
     * HostCatcherTest sets this to true for testing purposes.
     */
    static boolean DEBUG = false;

    /**
     * If in test mode, makes sure the lists are OK.
     * 
     * Checks invariants.
     * Very slow.
     * The method body should be enabled for testing purposes only.
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
     * Only recoverHosts() below calls this.
     */
    private void readHostsFile() {

        // Make a note in the log
        LOG.trace("Reading Hosts File");

        try {

            // Read the gnutella.net file and build the list of hosts this object keeps
            read(getHostsFile());

        } catch (IOException e) { LOG.debug(getHostsFile(), e); }
    }

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

    /**
     * Clear our records of what's happened with the host list, and read it from the gnutella.net file again.
     * 
     * Recovers any hosts that we have put in the set of hosts pending removal from our hosts list.
     */
    public synchronized void recoverHosts() {

        // Make a note in the log
        LOG.debug("recovering hosts file");

        // Clear our records of which hosts in the list we had trouble connecting to
        PROBATION_HOSTS.clear(); // Hosts we connected a TCP socket to, but that refused us in the handshake
        EXPIRED_HOSTS.clear();   // Hosts we couldn't connect a TCP socket to

        // Reset our count of the number of hosts we tried to connect to, but were unable to
        _failures = 0;

        // Reset the system within this HostCatcher that talks to GWebCache servers
        FETCHER.resetFetchTime();
        gWebCache.resetData();

        // Set UDP host caches that have failed 6 times back to 5, and move them back into the main list
        udpHostCache.resetData();

        // Clear our record of IP addresses we sent pings to
        pinger.resetData();

        /*
         * Read the hosts file again.  This will also notify any waiting 
         * connection fetchers from previous connection attempts.
         */

        // Read the IP addresses and port numbers from the gnutella.net file into the lists this object keeps again
        readHostsFile();
    }

    /**
     * Add the given host to our group of hosts on probation.
     * These are hosts that we created a TCP connection with, but then rejected us in the Gnutella handshake.
     * We may use them if we need more hosts.
     * 
     * @param host The ExtendedEndpoint in the list that we connected to and got rejected by
     */
    public synchronized void putHostOnProbation(Endpoint host) {

        // Add the given host to the list
        PROBATION_HOSTS.add(host);

        // If we put the 501st host on probation
        if (PROBATION_HOSTS.size() > PROBATION_HOSTS_SIZE) {

            // Remove one to bring the list back down to 500
            PROBATION_HOSTS.remove(PROBATION_HOSTS.iterator().next());
        }
    }

    /**
     * Add the given host to our group of expired hosts.
     * These are hosts that we have been unable to create a TCP connection to.
     * 
     * @param host The ExtendedEndpoint in the list with the IP address and port number we couldn't connect to
     */
    public synchronized void expireHost(Endpoint host) {

        // Add the given host to the list
        EXPIRED_HOSTS.add(host);

        // If we just expired the 501st host
        if (EXPIRED_HOSTS.size() > EXPIRED_HOSTS_SIZE) {

            // Remove one to bring the list back down to 500
            EXPIRED_HOSTS.remove(EXPIRED_HOSTS.iterator().next());
        }
    }

    /**
     * Every 2 minutes, gets more hosts if necessary from external services like UDP host caches and GWebCaches.
     * 
     * This nested class named Bootstrapper implements the Runnable interface.
     * This means it has a run() method.
     * The RouterService calls run() every 2 minutes.
     * 
     * The run() method determines if we need more hosts, and if we've waited long enough since bothering external bootstrapping services.
     * If so, it gets hosts 3 different ways:
     * Send a multicast ping packet.
     * Send a UDP Gnutella ping packet to UDP host caches.
     * Connect a TCP socket to GWebCache scripts on the Web.
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
         * 20 seconds, then 1 minute 40 seconds, then 8 minutes 20 seconds, and so on.
         * The time we have to wait before bothering a GWebCache script again.
         * Each time this expires, we'll make it 5 times longer.
         */
        private int delay = 20 * 1000;

        /** 30 seconds, after we contact UDP host caches, we'll wait 30 seconds before we start contacting GWebCaches. */
        private static final int POST_UDP_DELAY = 30 * 1000;

        /** 1 minute, we'll wait a minute between sending multicast pings. */
        private static final int POST_MULTICAST_DELAY = 60 * 1000;

        /**
         * Contacts external bootstrapping services to get more hosts for our list.
         * The RouterService will call this run() method every 2 minutes.
         * 
         * Determines if we need more hosts.
         * Determines if we've waited long enough to give ourselves permission to contact an external bootstrapping service.
         * If so, calls getHosts(), which tries multicast, UDP host caches, and then GWebCache scripts to get more hosts.
         */
        public synchronized void run() {

            // If connection settings prevent us from using GWebCache scripts, do nothing and leave now
            if (ConnectionSettings.DO_NOT_BOOTSTRAP.getValue()) return;

            // If there aren't any threads sleeping in getAnEndpoint(), we don't need to bother a GWebCache for some
            if (_catchersWaiting == 0) return;

            // Only continue here if we've waited long enough
            long now = System.currentTimeMillis(); // Get the time right now
            if (udpHostCache.getSize() == 0 &&     // Our list of UDP host caches is empty, and
                now < nextAllowedFetchTime &&      // We haven't waited long enough to be able to contact a GWebCache, and
                now < nextAllowedMulticastTime)    // We haven't waited long enough yet to get hosts through multicast (do)
                return;                            // Try again later

            // Only continue if our list of hosts is empty
            if (!needsHosts(now)) return;

            // Get more hosts, asking by multicast, then bothering UDP host caches, and then trying GWebCaches
            getHosts(now);
        }

        /**
         * Grant permisson to contact GWebCache scripts without waiting.
         * 
         * Resets nextAllowedFetchTime.
         * Do this after we regain our Internet connection.
         * That way, we'll be able to fetch from GWebCache scripts if we need to.
         */
        void resetFetchTime() {

            // Setting nextAllowedFetchTime to 0 will let us do it the next time we try to
            nextAllowedFetchTime = 0;
        }

        /**
         * Determines whether or not we need more hosts.
         * If our host list is empty, we need more, returns true.
         * 
         * @param now The time right now
         * @return    True if we need more hosts, false if we don't
         */
        private synchronized boolean needsHosts(long now) {

            // Synchronize on the HostCatcher object
            synchronized (HostCatcher.this) {

                // Return true if our list of hosts is empty and we need more
                return
                    getNumHosts() == 0 ||                              // If our list is empty, or
                    (!RouterService.isConnected() && _failures > 100); // We're still not connected, and we've been unable to make more than 100 connections
            }
        }

        /**
         * Fetches more hosts, updating the next allowed time to fetch.
         * 
         * @param now The time now
         */
        synchronized void getHosts(long now) {

            // Always try multicast first
            if (multicastFetch(now)) return; // multicastFetch() returns true if we sent out a multicast ping, leave now

            // If we were unable to try that way, contact UDP host caches
            if (udpHostCacheFetch(now)) return; // udpHostCacheFetch() returns true if it contacted some UDP host caches, leave now

            // If we don't know any UDP host caches to contact, bother the GWebCache scripts
            if (gwebCacheFetch(now)) return;

            /*
             * Nothing worked.
             * :-(
             */
        }

        /**
         * Makes a multicast ping packet, and has the multicast service send it.
         * 
         * @return True if we sent out a ping over multicast, false if we didn't.
         */
        private boolean multicastFetch(long now) {

            // If we've waited long enough to try multicast again, and connection settings allow it
            if (nextAllowedMulticastTime < now && !ConnectionSettings.DO_NOT_MULTICAST_BOOTSTRAP.getValue()) {

                // Make a multicast ping packet, and have the multicast service send it
                LOG.trace("Fetching via multicast");
                PingRequest pr = PingRequest.createMulticastPing();
                MulticastService.instance().send(pr);

                // Prevent us from doing this again until more than a minute from now
                nextAllowedMulticastTime = now + POST_MULTICAST_DELAY;
                
                // Report that we did it
                return true;
            }

            // No, we haven't waited long enough, or settings forbid trying
            return false;
        }

        /**
         * Contact the best 5 UDP host caches in our list.
         * Make a Gnutella ping packet in a UDP packet, and send it to them.
         * 
         * @param now The time now
         * @return    True if we sent a ping to some UDP host caches.
         *            False if the list of UDP host caches was empty so we didn't do anything.
         */
        private boolean udpHostCacheFetch(long now) {

            // Contact the best 5 UDP host caches in our list
            if (udpHostCache.fetchHosts()) {

                // Returns true if we've got some in our list, and sent a Gnutella ping message to them
                LOG.trace("Fetching via UDP");

                // Record the time 30 seconds from now, when we'll let ourselves start contacting GWebCaches if we're still not connected
                nextAllowedFetchTime = now + POST_UDP_DELAY;

                // Report true, we contacted some UDP host caches
                return true;
            }

            // Report false, we didn't have any UDP host caches to contact
            return false;
        }

        /**
         * Contact GWebCache scripts to get some IP addresses and port numbers of computers to try to connect to.
         * 
         * @param now The time now
         * @return    True if we contacted some GWebCache scripts, false if we didn't
         */
        private boolean gwebCacheFetch(long now) {

            // If we haven't waited long enough before bothring the GWebCache scripts again, do nothing and leave now
            if (now < nextAllowedFetchTime) return false;

            // Tell the GWebCache object to contact the scripts, and find out what state of that process it's in
            int ret = gWebCache.fetchEndpointsAsync();

            // Do something different depending on the state it reported
            switch (ret) {

            // It's scheduled an operation to contact the GWebCache scripts
            case BootstrapServerManager.FETCH_SCHEDULED:

                // Make the time we'll have to wait before trying this again 5 times longer than we just waited
                delay *= 5;
                nextAllowedFetchTime = now + delay;

                // Return true, the GWebCache object will contact a script to get more hosts
                if (LOG.isDebugEnabled()) LOG.debug("Fetching hosts.  Next allowed time: " + nextAllowedFetchTime);
                return true;

            // It didn't schedule a new fetch because it's doing one right now
            case BootstrapServerManager.FETCH_IN_PROGRESS:

                // Return true, the GWebCache object will contact a script to get more hosts
                LOG.debug("Tried to fetch, but was already fetching.");
                return true;

            // It can't contact any GWebCaches because we've disabled it
            case BootstrapServerManager.CACHE_OFF:

                // Return false, the GWebCache feature is turned off
                LOG.debug("Didn't fetch, gWebCache's turned off.");
                return false;

            // It didn't do anything because it already has gotten too many hosts back from GWebCaches
            case BootstrapServerManager.FETCHED_TOO_MANY:

                // Return false, we've already gotten too many hosts from GWebCache scripts
                LOG.debug("We've received a bunch of endpoints already, didn't fetch.");
                MessageService.showError("GWEBCACHE_FETCHED_TOO_MANY");
                return false;

            // It didn't do anything because we've already contacted each GWebCache we know about at least once
            case BootstrapServerManager.NO_CACHES_LEFT:

                // Return false, we've already bothered all the GWebCache scripts that we know
                LOG.debug("Already contacted each gWebCache, didn't fetch.");
                MessageService.showError("GWEBCACHE_NO_CACHES_LEFT");
                return false;

            // The GWebCache object can only return one of the states listed above
            default:

                // Somehow, it returned something else, throw an exception
                throw new IllegalArgumentException("invalid value: " + ret);
            }
        }
    }

    /*
     * Unit test: tests/com/.../gnutella/HostCatcherTest.java
     *            tests/com/.../gnutella/bootstrap/HostCatcherFetchTest.java
     */
}
