package com.limegroup.gnutella.bootstrap;

import com.limegroup.gnutella.Assert;
import com.limegroup.gnutella.ExtendedEndpoint;
import com.limegroup.gnutella.UDPHostRanker;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.MessageListener;
import com.limegroup.gnutella.ReplyHandler;
import com.limegroup.gnutella.UDPReplyHandler;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.PingRequest;
import com.limegroup.gnutella.util.NetworkUtils;
import com.limegroup.gnutella.util.Cancellable;
import com.limegroup.gnutella.util.FixedSizeExpiringSet;
import com.limegroup.gnutella.util.FixedsizePriorityQueue;
import com.limegroup.gnutella.util.IpPort;

import java.io.Writer;
import java.io.IOException;
import java.util.Iterator;
import java.util.Set;
import java.util.HashSet;
import java.util.TreeSet;
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
     * The maximum number of failures to allow for a given cache.
     */
    private static final int MAXIMUM_FAILURES = 5;
    
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
    private final Set /* of ExtendedEndpoint */ udpHostsSet = new HashSet();
    
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
        for(Iterator i = temp.iterator(); i.hasNext() && validHosts.size() < 10; )
            validHosts.add(i.next());

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
            new HostExpirer(hosts),
            // cancel when connected -- don't send out any more pings
            new Cancellable() {
                public boolean isCancelled() {
                    return RouterService.isConnected();
                }
            },
            getPing()
        );
        return true;
    }
    
    /**
     * Returns a PingRequest to be used while fetching.
     *
     * Useful as a seperate method for tests to catch the Ping's GUID.
     */
    protected PingRequest getPing() {
        return PingRequest.createUDPPing();
    }

    /**
     * Removes a given hostcache from this.
     */
    public synchronized boolean remove(ExtendedEndpoint e) {
        if(LOG.isTraceEnabled())
            LOG.trace("Removing endpoint: " + e);
        boolean removed1=udpHosts.remove(e);
        boolean removed2=udpHostsSet.remove(e);
        Assert.that(removed1==removed2,
                    "Set "+removed1+" but queue "+removed2);
        return removed1;
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
    
    /**
     * Listener that listens for message from the specified hosts,
     * marking any hosts that did not have a message processed
     * as failed host caches, causing them to increment a failure
     * count.  If hosts exceed the maximum failures, they are
     * removed as potential hostcaches.
     */
    private class HostExpirer implements MessageListener {
        // note that this MUST use IpPort.COMPARATOR to efficiently
        // look up ReplyHandlers vs ExtendedEndpoints
        // this is emptied as messages are processed from hosts,
        // to allow us to record failures as time passes.
        private final Set hosts = new TreeSet(IpPort.COMPARATOR);
        // allHosts contains all the hosts, so that we can
        // iterate over successful caches too.
        private final Set allHosts;
        private byte[] guid;
        
        /**
         * Constructs a new HostExpirer for the specified hosts.
         */
        public HostExpirer(Collection hosts) {
            this.hosts.addAll(hosts);
            allHosts = new HashSet(hosts);
        }
        
        /**
         * Notification that a message has been processed.
         */
        public void processMessage(Message m, ReplyHandler handler) {
            // allow only udp replies.
            if(handler instanceof UDPReplyHandler) {
                hosts.remove(handler);
                // OPTIMIZATION: if we've gotten succesful responses from
                // each hosts, unregister ourselves early.
                if(hosts.isEmpty())
                    RouterService.getMessageRouter().unregisterMessageListener(guid, this);
            }
        }
        
        /**
         * Notification that this listener is now registered with the specified GUID.
         */
        public void registered(byte[] g) {
            this.guid = g;
        }
        
        /**
         * Notification that this listener is now unregistered for the specified guid.
         */
        public void unregistered(byte[] g) {
            // Record the failures...
            for(Iterator i = hosts.iterator(); i.hasNext(); ) {
                ExtendedEndpoint ep = (ExtendedEndpoint)i.next();
                if(LOG.isTraceEnabled())
                    LOG.trace("No response from cache: " + ep);
                ep.recordUDPHostCacheFailure();
                if(ep.getUDPHostCacheFailures() > MAXIMUM_FAILURES)
                    remove(ep);
            }
            // Then record the successes...
            allHosts.removeAll(hosts);
            for(Iterator i = allHosts.iterator(); i.hasNext(); ) {
                ExtendedEndpoint ep = (ExtendedEndpoint)i.next();
                if(LOG.isTraceEnabled())
                    LOG.trace("Valid response from cache: " + ep);
                ep.recordUDPHostCacheSuccess();
            }
        }
    }
}