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
import java.util.Comparator;
import java.util.Collections;

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
     * The number of hosts we try to fetch from at once.
     */
    public static final int FETCH_AMOUNT = 5;
    
    /**
     * A list of UDP Host caches, to allow easy sorting & randomizing.
     * For convenience, a Set is also maintained, to easily look up duplicates.
     * INVARIANT: udpHosts contains no duplicates and contains exactly
     *  the same elements and udpHostsSet
     * LOCKING: obtain this' monitor before modifying either */
    private final List /* of ExtendedEndpoint */ udpHosts =
        new ArrayList(PERMANENT_SIZE);
    private final Set /* of ExtendedEndpoint */ udpHostsSet = new HashSet();
    
    /**
     * A set of hosts who we've recently contacted, so we don't contact them
     * again.
     */
    private final Set /* of ExtendedEndpoint */ attemptedHosts;
    
    /**
     * Whether or not we need to resort the udpHosts by failures.
     */
    private boolean dirty = false;
    
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
     * Erases the attempted hosts & decrements the failure counts.
     */
    public synchronized void resetData() {
        LOG.debug("Clearing attempted udp host caches");
        decrementFailures();
        attemptedHosts.clear();
    }
    
    /**
     * Decrements the failure count for each known cache.
     */
    protected synchronized void decrementFailures() {
        for(Iterator i = attemptedHosts.iterator(); i.hasNext(); ) {
            ExtendedEndpoint ep = (ExtendedEndpoint)i.next();
            ep.decrementUDPHostCacheFailure();
            // if we brought this guy down back to a managable
            // failure size, add'm back if we have room.
            if(ep.getUDPHostCacheFailures() == MAXIMUM_FAILURES &&
               udpHosts.size() < PERMANENT_SIZE)
                add(ep);
            dirty = true;
        }
    }
    
    /**
     * Attempts to contact a host cache to retrieve endpoints.
     *
     * Contacts 10 UDP hosts at a time.
     */
    public synchronized boolean fetchHosts() {
        // If the order has possibly changed, resort.
        if(dirty) {
            Collections.sort(udpHosts, FAILURE_COMPARATOR);
            dirty = false;
        }
        
        // Keep only the first FETCH_AMOUNT of the valid hosts.
        List validHosts =
            new ArrayList(Math.min(FETCH_AMOUNT, udpHosts.size()));
        for(Iterator i = udpHosts.iterator();
         i.hasNext() && validHosts.size() < FETCH_AMOUNT; ) {
            Object next = i.next();
            if(!attemptedHosts.contains(next))
                validHosts.add(next);
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
            
        // note that we do not do any comparisons to ensure that
        // this host is "better" than existing hosts.
        // the rationale is that we'll only ever be adding hosts
        // who have a failure count of 0 (unless we're reading
        // from gnutella.net, in which case all will be added),
        // and we always want to try new people.
        
        // if we've exceeded the maximum size, remove the worst element.
        if(udpHosts.size() >= PERMANENT_SIZE) {
            Object removed = udpHosts.remove(udpHosts.size() - 1);
            udpHostsSet.remove(removed);
        }
        
        // just insert him at the beginning.  we'll sort later.
        udpHosts.add(0, e);
        udpHostsSet.add(e);
        // we need to sort if this guy had any failures.
        // otherwise, the front of the list is the place
        // to go.
        if(e.getUDPHostCacheFailures() != 0)
            dirty = true;
        return true;
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
                if(hosts.remove(handler)) {
                    if(LOG.isTraceEnabled())
                        LOG.trace("Recieved: " + m);
                }
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
            synchronized(UDPHostCache.this) {
                // Record the failures...
                for(Iterator i = hosts.iterator(); i.hasNext(); ) {
                    ExtendedEndpoint ep = (ExtendedEndpoint)i.next();
                    if(LOG.isTraceEnabled())
                        LOG.trace("No response from cache: " + ep);
                    ep.recordUDPHostCacheFailure();
                    dirty = true;
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
                    dirty = true;
                }
            }
        }
    }
    
    /**
     * The only FailureComparator we'll ever need.
     */
    private static final Comparator FAILURE_COMPARATOR = new FailureComparator();
    private static class FailureComparator implements Comparator {
        public int compare(Object a, Object b) {
            ExtendedEndpoint e1 = (ExtendedEndpoint)a;
            ExtendedEndpoint e2 = (ExtendedEndpoint)b;
            return e1.getUDPHostCacheFailures() - e2.getUDPHostCacheFailures();
        }
    }
}