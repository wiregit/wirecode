package com.limegroup.gnutella.bootstrap;

import com.limegroup.gnutella.Assert;
import com.limegroup.gnutella.ExtendedEndpoint;
import com.limegroup.gnutella.UDPHostRanker;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.util.NetworkUtils;
import com.limegroup.gnutella.util.Cancellable;
import com.limegroup.gnutella.util.FixedSizeExpiringSet;
import com.limegroup.gnutella.util.FixedsizePriorityQueue;

import java.io.Writer;
import java.io.IOException;
import java.util.Iterator;
import java.util.Set;
import java.util.HashSet;

import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

/**
 * A collection of UDP Host Caches.
 */
public class UDPHostCache {
    
    private static final Log LOG = LogFactory.getLog(UDPHostCache.class);
    
    /**
     * The total number of udp host caches to remember between
     * launches, or at any given time.
     */
    private static final int PERMANENT_SIZE = 100;
    
    /**
     * A sorted (by uptime) list of UDP caches.
     * This is sorted by list so that the permanent caching (to/from disk)
     * keeps the caches with the highest uptime.
     * For convenience, a Set is also maintained, to easily look up
     * duplicates.
     * INVARIANT: udpHosts contains no duplicates and contains exactly
     *  the same elements and udpHostsSet
     * LOCKING: obtain this' monitor before modifying either */
    private FixedsizePriorityQueue /* of ExtendedEndpoint */ udpHosts =
        new FixedsizePriorityQueue(ExtendedEndpoint.priorityComparator(),
                                   PERMANENT_SIZE);
    private Set /* of ExtendedEndpoint */ udpHostsSet =new HashSet();
    
    /**
     * A set of hosts who we've recently contacted, so we don't contact them
     * again.
     */
    private Set /* of ExtendedEndpoint */ attemptedHosts =
        new FixedSizeExpiringSet(PERMANENT_SIZE, 10 * 60 * 1000);
    
    /**
     * Writes this' info out to the stream.
     */
    public synchronized void write(Writer out) throws IOException {
        for(Iterator iter = udpHosts.iterator(); iter.hasNext(); ) {
            ExtendedEndpoint e = (ExtendedEndpoint)iter.next();
            e.write(out);
        }
    }
    
    /**
     * Erases the attempted hosts.
     */
    public synchronized void resetData() {
        LOG.debug("Clearing attempted udp host caches");
        attemptedHosts.clear();
    }
    
    /**
     * Attempts to contact a host cache to retrieve endpoints.
     */
    public synchronized boolean fetchHosts() {
        Set validHosts = new HashSet(udpHostsSet);
        validHosts.removeAll(attemptedHosts);
        if(validHosts.isEmpty()) {
            LOG.warn("No UDP Host Caches to fetch.");
            return false;
        }

        LOG.debug("Fetching hosts via UDP Host Cache");
        attemptedHosts.addAll(validHosts);        
        UDPHostRanker.rank(
            validHosts,
            null,
            // cancel when connected -- don't send out any more pings
            new Cancellable() {
                public boolean isCancelled() {
                    return RouterService.isConnected();
                }
            }
        );
        return true;
    }       
    
    /**
     * Adds a new udp hostcache to this.
     */
    public boolean add(ExtendedEndpoint e) {
        Assert.that(e.isUDPHostCache());
        
        if (NetworkUtils.isPrivateAddress(e.getInetAddress()))
            return false;
        if (udpHostsSet.contains(e))
            return false;
        
        Object removed=udpHosts.insert(e);
        if (removed!=e) {
            //Was actually added...
            udpHostsSet.add(e);
            if (removed!=null)
                //...and something else was removed.
                udpHostsSet.remove(removed);
            return true;
        } else {
            //Uptime not good enough to add.  (Note that this is 
            //really just an optimization of the above case.)
            return false;
        }
    }
    
    /**
     * Notification that all stored UDP host caches have been added.
     * If none are stored, we load a list of defaults.
     */
    public synchronized void hostCachesAdded() {
        if(udpHostsSet.isEmpty())
            loadDefaults();
    }
    
    private void loadDefaults() {}
    
}