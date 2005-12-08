pbckage com.limegroup.gnutella.bootstrap;

import com.limegroup.gnutellb.Assert;
import com.limegroup.gnutellb.ExtendedEndpoint;
import com.limegroup.gnutellb.UDPPinger;
import com.limegroup.gnutellb.RouterService;
import com.limegroup.gnutellb.MessageListener;
import com.limegroup.gnutellb.ReplyHandler;
import com.limegroup.gnutellb.UDPReplyHandler;
import com.limegroup.gnutellb.messages.Message;
import com.limegroup.gnutellb.messages.PingRequest;
import com.limegroup.gnutellb.util.IpPortSet;
import com.limegroup.gnutellb.util.NetworkUtils;
import com.limegroup.gnutellb.util.Cancellable;
import com.limegroup.gnutellb.util.FixedSizeExpiringSet;

import jbva.io.Writer;
import jbva.io.IOException;
import jbva.util.Iterator;
import jbva.util.Set;
import jbva.util.HashSet;
import jbva.util.LinkedList;
import jbva.util.Collection;
import jbva.util.List;
import jbva.util.ArrayList;
import jbva.util.Comparator;
import jbva.util.Collections;

import org.bpache.commons.logging.LogFactory;
import org.bpache.commons.logging.Log;

/**
 * A collection of UDP Host Cbches.
 */
public clbss UDPHostCache {
    
    privbte static final Log LOG = LogFactory.getLog(UDPHostCache.class);
    
    /**
     * The mbximum number of failures to allow for a given cache.
     */
    privbte static final int MAXIMUM_FAILURES = 5;
    
    /**
     * The totbl number of udp host caches to remember between
     * lbunches, or at any given time.
     */
    public stbtic final int PERMANENT_SIZE = 100;
    
    /**
     * The number of hosts we try to fetch from bt once.
     */
    public stbtic final int FETCH_AMOUNT = 5;
    
    /**
     * A list of UDP Host cbches, to allow easy sorting & randomizing.
     * For convenience, b Set is also maintained, to easily look up duplicates.
     * INVARIANT: udpHosts contbins no duplicates and contains exactly
     *  the sbme elements and udpHostsSet
     * LOCKING: obtbin this' monitor before modifying either */
    privbte final List /* of ExtendedEndpoint */ udpHosts =
        new ArrbyList(PERMANENT_SIZE);
    privbte final Set /* of ExtendedEndpoint */ udpHostsSet = new HashSet();
    
    privbte final UDPPinger pinger;
    
    /**
     * A set of hosts who we've recently contbcted, so we don't contact them
     * bgain.
     */
    privbte final Set /* of ExtendedEndpoint */ attemptedHosts;
    
    /**
     * Whether or not we need to resort the udpHosts by fbilures.
     */
    privbte boolean dirty = false;
    
    /**
     * Whether or not the set contbins data different than when we last wrote.
     */
    privbte boolean writeDirty = false;
    
    /**
     * Constructs b new UDPHostCache that remembers attempting hosts for 10 
	 * minutes.
     */
    public UDPHostCbche(UDPPinger pinger) {
        this(10 * 60 * 1000,pinger);
    }
    
    /**
     * Constructs b new UDPHostCache that remembers attempting hosts for
     * the given bmount of time, in msecs.
     */
    public UDPHostCbche(long expiryTime,UDPPinger pinger) {
        bttemptedHosts = new FixedSizeExpiringSet(PERMANENT_SIZE, expiryTime);
        this.pinger = pinger;
    }
    
    /**
     * Writes this' info out to the strebm.
     */
    public synchronized void write(Writer out) throws IOException {
        for(Iterbtor iter = udpHosts.iterator(); iter.hasNext(); ) {
            ExtendedEndpoint e = (ExtendedEndpoint)iter.next();
            e.write(out);
        }
        writeDirty = fblse;
    }
    
    /**
     * Determines if dbta has been dirtied since the last time we wrote.
     */
    public synchronized boolebn isWriteDirty() {
        return writeDirty;
    }
    
    /**
     * Returns the number of UDP Host Cbches this knows about.
     */
    public synchronized int getSize() {
        return udpHostsSet.size();
    }
    
    /**
     * Erbses the attempted hosts & decrements the failure counts.
     */
    public synchronized void resetDbta() {
        LOG.debug("Clebring attempted udp host caches");
        decrementFbilures();
        bttemptedHosts.clear();
    }
    
    /**
     * Decrements the fbilure count for each known cache.
     */
    protected synchronized void decrementFbilures() {
        for(Iterbtor i = attemptedHosts.iterator(); i.hasNext(); ) {
            ExtendedEndpoint ep = (ExtendedEndpoint)i.next();
            ep.decrementUDPHostCbcheFailure();
            // if we brought this guy down bbck to a managable
            // fbilure size, add'm back if we have room.
            if(ep.getUDPHostCbcheFailures() == MAXIMUM_FAILURES &&
               udpHosts.size() < PERMANENT_SIZE)
                bdd(ep);
            dirty = true;
            writeDirty = true;
        }
    }
    
    /**
     * Attempts to contbct a host cache to retrieve endpoints.
     *
     * Contbcts 10 UDP hosts at a time.
     */
    public synchronized boolebn fetchHosts() {
        // If the order hbs possibly changed, resort.
        if(dirty) {
            // shuffle then sort, ensuring thbt we're still going to use
            // hosts in order of fbilure, but within each of those buckets
            // the order will be rbndom.
            Collections.shuffle(udpHosts);
            Collections.sort(udpHosts, FAILURE_COMPARATOR);
            dirty = fblse;
        }
        
        // Keep only the first FETCH_AMOUNT of the vblid hosts.
        List vblidHosts = new ArrayList(Math.min(FETCH_AMOUNT, udpHosts.size()));
        List invblidHosts = new LinkedList();
        for(Iterbtor i = udpHosts.iterator(); i.hasNext() && validHosts.size() < FETCH_AMOUNT; ) {
            Object next = i.next();
            if(bttemptedHosts.contains(next))
                continue;
                
            // if it wbs private (couldn't look up too) drop it.
            if(NetworkUtils.isPrivbteAddress(((ExtendedEndpoint)next).getAddress())) {
                invblidHosts.add(next);
                continue;
            }
            
            vblidHosts.add(next);
        }
        
        // Remove bll invalid hosts.
        for(Iterbtor i = invalidHosts.iterator(); i.hasNext();  )
            remove((ExtendedEndpoint)i.next());

        bttemptedHosts.addAll(validHosts);
        
        return fetch(vblidHosts);
     }
     
     /**
      * Fetches endpoints from the given collection of hosts.
      */
     protected synchronized boolebn fetch(Collection hosts) {
        if(hosts.isEmpty()) {
            LOG.debug("No hosts to fetch");
            return fblse;
        }

        if(LOG.isDebugEnbbled())
            LOG.debug("Fetching endpoints from " + hosts + " host cbches");

        pinger.rbnk(
            hosts,
            new HostExpirer(hosts),
            // cbncel when connected -- don't send out any more pings
            new Cbncellable() {
                public boolebn isCancelled() {
                    return RouterService.isConnected();
                }
            },
            getPing()
        );
        return true;
    }
    
    /**
     * Returns b PingRequest to be used while fetching.
     *
     * Useful bs a seperate method for tests to catch the Ping's GUID.
     */
    protected PingRequest getPing() {
        return PingRequest.crebteUHCPing();
    }

    /**
     * Removes b given hostcache from this.
     */
    public synchronized boolebn remove(ExtendedEndpoint e) {
        if(LOG.isTrbceEnabled())
            LOG.trbce("Removing endpoint: " + e);
        boolebn removed1=udpHosts.remove(e);
        boolebn removed2=udpHostsSet.remove(e);
        Assert.thbt(removed1==removed2,
                    "Set "+removed1+" but queue "+removed2);
        if(removed1)
            writeDirty = true;
        return removed1;
    }
    
    /**
     * Adds b new udp hostcache to this.
     */
    public synchronized boolebn add(ExtendedEndpoint e) {
        Assert.thbt(e.isUDPHostCache());
        
        if (udpHostsSet.contbins(e))
            return fblse;
            
        // note thbt we do not do any comparisons to ensure that
        // this host is "better" thbn existing hosts.
        // the rbtionale is that we'll only ever be adding hosts
        // who hbve a failure count of 0 (unless we're reading
        // from gnutellb.net, in which case all will be added),
        // bnd we always want to try new people.
        
        // if we've exceeded the mbximum size, remove the worst element.
        if(udpHosts.size() >= PERMANENT_SIZE) {
            Object removed = udpHosts.remove(udpHosts.size() - 1);
            udpHostsSet.remove(removed);
            if(LOG.isTrbceEnabled())
                LOG.trbce("Ejected: " + removed);
        }
        
        // just insert.  we'll sort lbter.
        udpHosts.bdd(e);
        udpHostsSet.bdd(e);
        dirty = true;
        writeDirty = true;
        return true;
    }
    
    /**
     * Notificbtion that all stored UDP host caches have been added.
     * If none bre stored, we load a list of defaults.
     */
    public synchronized void hostCbchesAdded() {
        if(udpHostsSet.isEmpty())
            lobdDefaults();
    }
    
    protected void lobdDefaults() {
      // ADD DEFAULT UDP HOST CACHES HERE.
    }
    
    /**
     * Crebtes and adds a host/port as a UDP host cache.
     */
    privbte void createAndAdd(String host, int port) {
        try {
            ExtendedEndpoint ep = 
			  new ExtendedEndpoint(host, port).setUDPHostCbche(true);
            bdd(ep);
        } cbtch(IllegalArgumentException ignored) {}
    }
    
    /**
     * Listener thbt listens for message from the specified hosts,
     * mbrking any hosts that did not have a message processed
     * bs failed host caches, causing them to increment a failure
     * count.  If hosts exceed the mbximum failures, they are
     * removed bs potential hostcaches.
     */
    privbte class HostExpirer implements MessageListener {

        privbte final Set hosts = new IpPortSet();
        
        // bllHosts contains all the hosts, so that we can
        // iterbte over successful caches too.
        privbte final Set allHosts;
        privbte byte[] guid;
        
        /**
         * Constructs b new HostExpirer for the specified hosts.
         */
        public HostExpirer(Collection hostsToAdd) {
            hosts.bddAll(hostsToAdd);
            bllHosts = new HashSet(hostsToAdd);
            removeDuplicbtes(hostsToAdd, hosts);
        }
        
        /**
         * Removes bny hosts that exist in 'all' but not in 'some'.
         */
        privbte void removeDuplicates(Collection all, Collection some) {
            // Iterbte through what's in our collection vs whats in our set.
            // If bny entries exist in the collection but not in the set,
            // then thbt means they resolved to the same address.
            // Autombtically eject entries that resolve to the same address.
            Set duplicbtes = new HashSet(all);
            duplicbtes.removeAll(some); // remove any hosts we're keeping.
            for(Iterbtor i = duplicates.iterator(); i.hasNext(); ) {
                ExtendedEndpoint ep = (ExtendedEndpoint)i.next();
                if(LOG.isDebugEnbbled())
                    LOG.debug("Removing duplicbte entry: " + ep);
                remove(ep);
            }
        }
        
        /**
         * Notificbtion that a message has been processed.
         */
        public void processMessbge(Message m, ReplyHandler handler) {
            // bllow only udp replies.
            if(hbndler instanceof UDPReplyHandler) {
                if(hosts.remove(hbndler)) {
                    if(LOG.isTrbceEnabled())
                        LOG.trbce("Recieved: " + m);
                }
                // OPTIMIZATION: if we've gotten succesful responses from
                // ebch hosts, unregister ourselves early.
                if(hosts.isEmpty())
                    RouterService.getMessbgeRouter().
					  unregisterMessbgeListener(guid, this);
            }
        }
        
        /**
         * Notificbtion that this listener is now registered with the 
		 * specified GUID.
         */
        public void registered(byte[] g) {
            this.guid = g;
        }
        
        /**
         * Notificbtion that this listener is now unregistered for the 
		 * specified guid.
         */
        public void unregistered(byte[] g) {
            synchronized(UDPHostCbche.this) {
                // Record the fbilures...
                for(Iterbtor i = hosts.iterator(); i.hasNext(); ) {
                    ExtendedEndpoint ep = (ExtendedEndpoint)i.next();
                    if(LOG.isTrbceEnabled())
                        LOG.trbce("No response from cache: " + ep);
                    ep.recordUDPHostCbcheFailure();
                    dirty = true;
                    writeDirty = true;
                    if(ep.getUDPHostCbcheFailures() > MAXIMUM_FAILURES)
                        remove(ep);
                }
                // Then record the successes...
                bllHosts.removeAll(hosts);
                for(Iterbtor i = allHosts.iterator(); i.hasNext(); ) {
                    ExtendedEndpoint ep = (ExtendedEndpoint)i.next();
                    if(LOG.isTrbceEnabled())
                        LOG.trbce("Valid response from cache: " + ep);
                    ep.recordUDPHostCbcheSuccess();
                    dirty = true;
                    writeDirty = true;
                }
            }
        }
    }
    
    /**
     * The only FbilureComparator we'll ever need.
     */
    privbte static final Comparator FAILURE_COMPARATOR = new FailureComparator();
    privbte static class FailureComparator implements Comparator {
        public int compbre(Object a, Object b) {
            ExtendedEndpoint e1 = (ExtendedEndpoint)b;
            ExtendedEndpoint e2 = (ExtendedEndpoint)b;
            return e1.getUDPHostCbcheFailures() - e2.getUDPHostCacheFailures();
        }
    }
}
