package com.limegroup.gnutella.bootstrap;

import com.limegroup.gnutella.Assert;
import com.limegroup.gnutella.ExtendedEndpoint;
import com.limegroup.gnutella.UDPHostRanker;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.messages.PingRequest;
import com.limegroup.gnutella.util.NetworkUtils;
import com.limegroup.gnutella.util.Cancellable;
import com.limegroup.gnutella.util.FixedSizeExpiringSet;
import com.limegroup.gnutella.util.FixedsizePriorityQueue;

import java.io.Writer;
import java.io.IOException;
import java.util.Iterator;
import java.util.Set;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Collection;
import java.util.List;
import java.util.ArrayList;

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
    public static final int PERMANENT_SIZE = 100;
    
    /**
     * A sorted (by uptime) list of UDP caches.
     * This is sorted by list so that the permanent caching (to/from disk)
     * keeps the caches with the highest uptime.
     * For convenience, a Set is also maintained, to easily look up
     * duplicates.
     * INVARIANT: udpHosts contains no duplicates and contains exactly
     *  the same elements and udpHostsSet
     * LOCKING: obtain this' monitor before modifying either */
    private final FixedsizePriorityQueue /* of ExtendedEndpoint */ udpHosts =
        new FixedsizePriorityQueue(ExtendedEndpoint.priorityComparator(),
                                   PERMANENT_SIZE);
    private final Set /* of ExtendedEndpoint */ udpHostsSet =new HashSet();
    
    /**
     * A set of hosts who we've recently contacted, so we don't contact them
     * again.
     */
    private final Set /* of ExtendedEndpoint */ attemptedHosts;
    
    /**
     * Constructs a new UDPHostCache that remembers attempting hosts for 10 minutes.
     */
    public UDPHostCache() {
        this(10 * 60 * 1000);
    }
    
    /**
     * Constructs a new UDPHostCache that remembers attempting hosts for
     * the given amount of time, in msecs.
     */
    public UDPHostCache(long expiryTime) {
        attemptedHosts = new FixedSizeExpiringSet(PERMANENT_SIZE, expiryTime);
    }
    
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
     * Returns the number of UDP Host Caches this knows about.
     */
    public synchronized int getSize() {
        return udpHostsSet.size();
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
     *
     * Contacts 10 UDP hosts at a time.
     */
    public synchronized boolean fetchHosts() {
        LinkedList temp = new LinkedList();
        // add caches so they're ordered best -> worst
        for(Iterator i = udpHosts.iterator(); i.hasNext(); ) {
            Object next = i.next();
            if(!attemptedHosts.contains(next))
                temp.addFirst(next);
        }
        
        // Keep only the first 10 of the valid hosts.
        // Note that we had to add all possible ones first, 
        // 'cause udpHosts.iterator() returns from worst -> best
        List validHosts = new ArrayList(Math.min(10, temp.size()));
        for(Iterator i = temp.iterator(); i.hasNext(); ){
            validHosts.add(i.next());
            if(validHosts.size() == 10)
                break;
        }

        attemptedHosts.addAll(validHosts);
        
        return fetch(validHosts);
     }
     
     /**
      * Fetches endpoints from the given collection of hosts.
      */
     protected synchronized boolean fetch(Collection hosts) {
        if(hosts.isEmpty()) {
            LOG.debug("No hosts to fetch");
            return false;
        }

        if(LOG.isDebugEnabled())
            LOG.debug("Fetching endpoints from " + hosts + " host caches");

        UDPHostRanker.rank(
            hosts,
            null,
            // cancel when connected -- don't send out any more pings
            new Cancellable() {
                public boolean isCancelled() {
                    return RouterService.isConnected();
                }
            },
            PingRequest.createUDPPing()
        );
        return true;
    }       
    
    /**
     * Adds a new udp hostcache to this.
     */
    public synchronized boolean add(ExtendedEndpoint e) {
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
    
    protected void loadDefaults() {
      // ADD DEFAULT UDP HOST CACHES HERE.
    }
    
}