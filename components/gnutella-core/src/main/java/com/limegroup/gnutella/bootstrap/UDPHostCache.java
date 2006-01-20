
// Commented for the Learning branch

package com.limegroup.gnutella.bootstrap;

import com.limegroup.gnutella.Assert;
import com.limegroup.gnutella.ExtendedEndpoint;
import com.limegroup.gnutella.UDPPinger;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.MessageListener;
import com.limegroup.gnutella.ReplyHandler;
import com.limegroup.gnutella.UDPReplyHandler;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.PingRequest;
import com.limegroup.gnutella.util.IpPortSet;
import com.limegroup.gnutella.util.NetworkUtils;
import com.limegroup.gnutella.util.Cancellable;
import com.limegroup.gnutella.util.FixedSizeExpiringSet;

import java.io.Writer;
import java.io.IOException;
import java.util.Iterator;
import java.util.Set;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Collection;
import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;

import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

/**
 * A UDPHostCache object keeps a list of UDP host caches, and has a method to contact them to find IP addresses of Gnutella computers to connect to.
 * There is only one UDPHostCache object as the program runs.
 * 
 * A UDP host cache is a remote computer on the Internet running UDP host cache software.
 * UDP host caches are usually on Web servers that have fast Internet connections, total uptime, and externally reachable IP addresses that don't change.
 * 
 * You can send a UDP packet to a UDP host cache, and it will reply with another.
 * It will tell you the IP addresses of computers like you running Gnutella software.
 * Then, you can try to connect to them, and get connected to the Gnutella network.
 * 
 * A UDPHostCache object keeps a list of UDP host caches.
 * In the list, each UDP host cache is represented by an ExtendedEndpoint object.
 * The ExtendedEndpoint object holds the IP address and port number of the UDP host cache.
 * 
 * Code in this class can send a Gnutella ping packet over UDP to a handful of UDP host caches at a time.
 * As the program gets pong responses from the UDP host caches, it calls methods in the nested HostExpirer class.
 * After 20 seconds, the program calls HostExpirer.unregistered(guid).
 * Code there sees which host caches responded to us, and which didn't.
 * It updates the success and failure counts in the ExtendedEndpoint object, and saves the data to the gnutella.net file on the disk.
 * 
 * To send a ping packet over UDP to the next best 5 UDP host caches, call the fetchHosts() method.
 * HostCatcher.Bootstrapper.udpHostCacheFetch() does this.
 */
public class UDPHostCache {

    /** A log that we can write lines of text into as the code here runs. */
    private static final Log LOG = LogFactory.getLog(UDPHostCache.class);

    /** 5, if we're unable to connect to a UDP host cache more than 5 times, we won't keep it on our list. */
    private static final int MAXIMUM_FAILURES = 5;

    /** 100, the list of UDP host caches we keep is limited to hold 100. */
    public static final int PERMANENT_SIZE = 100;

    /** 5, we try to contact 5 UDP host caches at a time. */
    public static final int FETCH_AMOUNT = 5;

    /*
     * udpHosts and udpHostsSet hold the list of IP addresses and port numbers of UDP host caches we can try to use.
     * Both contain exactly the same elements.
     * There are no duplicates in the list.
     * 
     * There are 2 Java collections classes used here.
     * ArrayList is just a list, while HashSet makes sure there are no duplicates.
     * Neither ArrayList or HashSet are sorted.
     * We use the HashSet to notice duplicates, and quickly shuffle the ArrayList into random order.
     * 
     * We'll keep ExtendedEndpoint objects in both of these lists.
     * Lock on this UDPHostCache class before modifying these lists.
     */

    /** A list of 100 UDP host caches to use, each is an ExtendedEndpoint holding an IP address and port number. */
    private final List udpHosts = new ArrayList(PERMANENT_SIZE);
    /** The same list as udpHosts, but in a HashSet that can tell us if a new one is already in there. */
    private final Set udpHostsSet = new HashSet();

    /** A UDPPinger object we can use to send a Gnutella ping packet to a list of IP addresses and port numbers with UDP. */
    private final UDPPinger pinger;

    /**
     * A list of UDP host caches we've recently contacted.
     * We keep this list so we don't contact them soon after.
     * This Set holds ExtendedEndpoint objects, each with the IP address and port number of a UDP host cache.
     * 
     * This is a LimeWire FixedsizeExpiringSet which we're using through the Java Set interface.
     * It will automatically remove an ExtendedEndpoint 10 minutes after we add it.
     */
    private final Set attemptedHosts;

    /**
     * True if we need to sort the udpHosts list by how many times we've been unable to contact each one.
     * 
     * When we add a new ExtendedEndpoint, we don't add it in sorted order.
     * Rather, we just add it anywhere, and set dirty to true.
     * Before we need to use the list, if dirty is true, we sort it all then.
     */
    private boolean dirty = false;

    /** True if we've updated the udpHosts list, and need to save the changed data to the gnutella.net file. */
    private boolean writeDirty = false;

    /**
     * Make a new UDPHostCache object.
     * It will remember that it contacted a UDP host cache for 10 minuites to make sure it doesn't contact it again.
     * 
     * @param pinger A UDPPinger object that can send a Gnutella ping packet to a list of IP addresses
     */
    public UDPHostCache(UDPPinger pinger) {

        // Call the next constructor, giving in 10 minutes in milliseconds and the given pinger object
        this(10 * 60 * 1000, pinger);
    }

    /**
     * Make a new UDPHostCache object.
     * 
     * @param expiryTime After we contact a UDP host cache, we'll remember we contacted it for this long
     * @param pinger     A UDPPinger object that can send a Gnutella ping packet to a list of IP addresses
     */
    public UDPHostCache(long expiryTime, UDPPinger pinger) {

        // Make a new LimeWire FixedSizeExpiringSet, which keeps a unique list of up to 100 items, throwing each out 10 minutes after we add it
        attemptedHosts = new FixedSizeExpiringSet(
            PERMANENT_SIZE, // Make it hold up to 100 ExtendedEndpoint objects
            expiryTime);    // 10 minutes after we add an item, the FixedSizeExpiringSet will remove it

        // Save the UDPPinger
        this.pinger = pinger;
    }

    /**
     * Have this UDPHostCache object write the information in the ExtendedEndpoints its keeping to lines of text in the gnutella.net file.
     * 
     * @param out The object we'll write to, the output stream opened from the gnutella.net file
     */
    public synchronized void write(Writer out) throws IOException {

        // Loop through all the ExtendedEndpoint objects in our list of UDP host caches
        for (Iterator iter = udpHosts.iterator(); iter.hasNext(); ) {

            // Have the ExtendedEndpoint.write() method write the information about the UDP host cache into a line of text in the gnutella.net file
            ExtendedEndpoint e = (ExtendedEndpoint)iter.next();
            e.write(out);
        }

        // We saved the file, the udpHosts list in the memory of the running program matches the information in the gnutella.net file on the disk
        writeDirty = false;
    }

    /**
     * Determines if this UDPHostCache needs to save its list to the gnutella.net file.
     * This is similar to opening a document on your computer, making a change, and needing to press the Save button.
     * 
     * @return True if we saved the file, the udpHosts list in the memory of the running program matches the information in the gnutella.net file on the disk.
     *         False if we changed the udpHosts list and need to save it to the gnutella.net file.
     */
    public synchronized boolean isWriteDirty() {

        // Return the flag we've been setting to true when we make changes and false when we save
        return writeDirty;
    }

    /**
     * Get the number of UDP host caches that this UDPHostCache object is keeping in its list.
     * The list here only holds 100, so it won't be more than that.
     * 
     * @return The number of ExtendedEndpoint objects in the list
     */
    public synchronized int getSize() {

        // Get the size from the HashSet, which will be a little faster
        return udpHostsSet.size();
    }

    /**
     * Pulls UDP host caches that haven't failed too much back into the main list, and clears the list of UDP host caches we've contacted.
     * 
     * Loops through the UDP host caches we've contacted, decrementing their failure count.
     * Adds those that drop down to 5 failures back into our main list of them.
     * Clears the list of UDP host caches we've contacted.
     * 
     * HostCatcher.recoverHosts() calls this.
     */
    public synchronized void resetData() {

        LOG.debug("Clearing attempted udp host caches");
        decrementFailures();
        attemptedHosts.clear();
    }

    /**
     * Loops through the UDP host caches we've contacted, decrementing their failure count.
     * Adds those that drop down to 5 failures back into our main list of them.
     * Only resetData() above calls this.
     */
    protected synchronized void decrementFailures() {

        // Loop through our record of the UDP host caches we've contacted
        for (Iterator i = attemptedHosts.iterator(); i.hasNext(); ) {
            ExtendedEndpoint ep = (ExtendedEndpoint)i.next();

            // Record that we failed to contact this UDP host cache one fewer time (do)
            ep.decrementUDPHostCacheFailure();

            /*
             * if we brought this guy down back to a managable
             * failure size, add'm back if we have room.
             */

            // If that brings it back down to 5 and there's room in the main udpHosts list for this one, add it
            if (ep.getUDPHostCacheFailures() == MAXIMUM_FAILURES && udpHosts.size() < PERMANENT_SIZE) add(ep);
            dirty = true; // We need to save the list to gnutella.net

            // We changed the udpHosts list and need to save it to the gnutella.net file
            writeDirty = true;
        }
    }

    /**
     * Contact the best 5 UDP host caches in our list.
     * Makes a new HostExpirer which will get called when we get a response.
     * Keeps track of which UDP host caches we've contacted so we don't keep bothering the same ones.
     * 
     * HostCatcher.Bootstrapper.udpHostCacheFetch() calls this to actually talk to the UDP host caches.
     * 
     * @return True if we sent a ping to some UDP host caches.
     *         False if the list of UDP host caches was empty so we didn't do anything.
     */
    public synchronized boolean fetchHosts() {

        // We need to sort the udpHosts list before using it
        if (dirty) {

            /*
             * shuffle then sort, ensuring that we're still going to use
             * hosts in order of failure, but within each of those buckets
             * the order will be random.
             */

            // Shuffle and sort so within each group that has the same number of failures, the order will be different than it was before
            Collections.shuffle(udpHosts);                  // Shuffle the ExtendedEndpoint objects in udpHosts into a random order
            Collections.sort(udpHosts, FAILURE_COMPARATOR); // Sort the ExtendedEndpoint objects by how many times we've sent a ping and got no response
            dirty = false;                                  // Record that the list is sorted
        }

        // Loop through UDP host caches, the list is sorted so that we'll get the best ones first
        List validHosts = new ArrayList(Math.min(FETCH_AMOUNT, udpHosts.size())); // We'll hold the UDP host caches we're going to contact here
        List invalidHosts = new LinkedList(); // If we run into a UDP host cache in our list that has a LAN address, we'll put it here and then delete it
        for (Iterator i = udpHosts.iterator(); i.hasNext() && validHosts.size() < FETCH_AMOUNT; ) {
            Object next = i.next();

            // If we've contacted that one in the last 10 minutes, go back to the start of the loop to get another
            if (attemptedHosts.contains(next)) continue;

            // We got a LAN IP address
            if (NetworkUtils.isPrivateAddress(((ExtendedEndpoint)next).getAddress())) {

                // Add it to the invalid list and get the next one from the start of the loop
                invalidHosts.add(next);
                continue;
            }

            // We got a good one that we haven't contacted yet, add it to the valid list
            validHosts.add(next);
        }

        // In the loop, we found ExtendedEndpoint objects with LAN IP addresses, remove them from the udpHosts list
        for(Iterator i = invalidHosts.iterator(); i.hasNext(); ) remove((ExtendedEndpoint)i.next());

        // Add all these hosts to the attemptedHosts list, recording that we tried to contact them now
        attemptedHosts.addAll(validHosts);
        
        // Contact this group of UDP host caches
        return fetch(validHosts);
    }

    /**
     * Sends Gnutella ping packets over UDP to a list of UDP host caches.
     * 
     * @param hosts A list of ExtendedEndpoint objects with the IP addresses and port numbers of the UDP host caches we'll contact
     * @return      True if we sent a ping to some UDP host caches.
     *              False if the list of UDP host caches was empty so we didn't do anything.
     */
    protected synchronized boolean fetch(Collection hosts) {

        // The fetchHosts() method above didn't give us any UDP host cache addresses to contact
        if (hosts.isEmpty()) {

            // Record the error and leave now
            LOG.debug("No hosts to fetch");
            return false;
        }

        // Make a node in the debugging log that we're about to contact the UDP host caches
        if (LOG.isDebugEnabled()) LOG.debug("Fetching endpoints from " + hosts + " host caches");

        // Use UDP to send a Gnutella ping packet to the list of IP addresses and port numbers
        pinger.rank(

            // The list of IP addresses and port numbers to send the packet to
            hosts,

            // The UDPPinger will contact this new HostExpirer when the UDP host caches give us responses
            new HostExpirer(hosts),

            /*
             * cancel when connected -- don't send out any more pings
             */

            // Make a new unnamed Cancellable object and pass it to the UDPPinger
            new Cancellable() {

                // The UDPPinger will call this isCancelled() method to see if we still want to go ahead
                public boolean isCancelled() {

                    // If we're connected to the Gnutella network, we don't need to contact this batch of UDP host caches
                    return RouterService.isConnected(); // Return true to cancel the operation
                }
            },

            // Make a UDP host cache ping message to send
            getPing()
        );

        // Report that we sent the ping to some UDP host caches
        return true;
    }

    /**
     * Generates a Gnutella UDP host cache ping packet to send to UDP host caches.
     * This is a separate method so that test code can catch the ping's GUID.
     * 
     * @return A new Gnutella UDP host cache ping packet
     */
    protected PingRequest getPing() {

        // Make a new Gnutella UDP host cache ping packet, and return it
        return PingRequest.createUHCPing();
    }

    /**
     * Remove our record of a UDP host cache from the list of them this object keeps.
     * 
     * @param e The ExtendedEndpoint in the list to remove
     * @return  True if we removed e, false if not found
     */
    public synchronized boolean remove(ExtendedEndpoint e) {

        // Make a note in the log
        if (LOG.isTraceEnabled()) LOG.trace("Removing endpoint: " + e);

        /*
         * udpHosts is a Java ArrayList that we're using with the List interface.
         * udpHostsSet is a HashSet that we're using with the Set interface.
         * 
         * These remove(e) methods take an object in the list, and return a boolean.
         * Their signature is boolean remove(Object o).
         * ArrayList inherits this method from AbstractCollection, while HashSet has it.
         * 
         * remove(e) returns true if the collection contained e, false if it is not found.
         * The list and the set contain the same ExtendedEndpoint objects.
         * So, they should both either have it or it should be not found in both.
         * The Assert.that() method makes sure this happened.
         * 
         * Assert.that() is a LimeWire method.
         * It throws an runtime exception with the composed text if the first argument isn't true.
         */

        // Remove e from both the list and the set, and make sure it was removed from both or not found in either
        boolean removed1 = udpHosts.remove(e);
        boolean removed2 = udpHostsSet.remove(e);
        Assert.that(removed1 == removed2, "Set " + removed1 + " but queue " + removed2);

        // We changed the udpHosts list and need to save it to the gnutella.net file
        if (removed1) writeDirty = true;

        // Return true if we removed e, false if not found
        return removed1;
    }

    /**
     * Add the address of a new UDP host cache to the list of them this object keeps.
     * 
     * @param e Information about the UDP host cache in an ExtendedEndpoint object
     */
    public synchronized boolean add(ExtendedEndpoint e) {

        // Make sure the ExtendedEndpoint the caller gave us describes a UDP host cache, and not a remote Gnutella computer
        Assert.that(e.isUDPHostCache());

        // If we've already got it, do nothing and leave now
        if (udpHostsSet.contains(e)) return false;

        /*
         * note that we do not do any comparisons to ensure that
         * this host is "better" than existing hosts.
         * the rationale is that we'll only ever be adding hosts
         * who have a failure count of 0 (unless we're reading
         * from gnutella.net, in which case all will be added),
         * and we always want to try new people.
         */

        // If we already have 100 ExtendedEndpoint objects in the list, we'll have to remove one to add this new one
        if (udpHosts.size() >= PERMANENT_SIZE) {

            // Remove the last ExtendedEndpoint in the list
            Object removed = udpHosts.remove(udpHosts.size() - 1); // This isn't the worst one, it's just the last one
            udpHostsSet.remove(removed); // Remove it from udpHostsSet too so the two lists stay the same
            if (LOG.isTraceEnabled()) LOG.trace("Ejected: " + removed);
        }

        // Add the given ExtendedEndpoint to both lists
        udpHosts.add(e);
        udpHostsSet.add(e);
        dirty = true; // Mark that udpHosts isn't sorted anymore, we'll need to sort the list before we use it

        // We changed the udpHosts list and need to save it to the gnutella.net file
        writeDirty = true;
        return true;
    }

    /**
     * Adds default UDP host cache addresses if we didn't get any from the gnutella.net file.
     * 
     * HostCatcher.read() calls this after it reads in the contents of the gnutella.net file.
     * It calls here as a notification that all the UDP host caches from the gnutella.net file have been added.
     * If we don't have any, we'll load a list of defaults that the LimeWire developers inserted into the source code when they compiled the program.
     */
    public synchronized void hostCachesAdded() {

        // If we don't have any UDP host caches yet, bring in the defaults
        if (udpHostsSet.isEmpty()) loadDefaults();
    }

    /**
     * Adds default UDP host cache addresses to the list this UDPHostCache keeps.
     * Only hostCachesAdded() above calls this method.
     */
    protected void loadDefaults() {

        /*
         * The comment below looks like a regular comment, but it's more.
         * 
         * When the company LimeWire ships a new version of LimeWire, they use a build script to perform the necessary steps.
         * It compiles the source code, and generates the installers for the different platforms like Windows, Macintosh, and Linux.
         * Before it compiles the source code, it does a find and replace operation across all the source code files.
         * As part of that operation, it finds this comment, and replaces it with code.
         * The code makes new ExtendedEndpoint objects with the IP address and port number of current, good UDP host caches.
         */

      // ADD DEFAULT UDP HOST CACHES HERE.
    }

    /**
     * Add a UDP host cache to the list this UDPHostCache object keeps.
     * Makes a new ExtendedEndpoint with the given IP address and port number, and adds it to the list.
     * 
     * @param host The IP address of a UDP host cache, like "64.61.25.171"
     * @param port The port number of the UDP host cache
     */
    private void createAndAdd(String host, int port) {

        try {

            // Make a new ExtendedEndpoint object with the given IP address and port number, and mark it as holding the address of a UDP host cache
            ExtendedEndpoint ep = new ExtendedEndpoint(host, port).setUDPHostCache(true);

            // Add the new ExtendedEndpoint object to our list
            add(ep);

        } catch (IllegalArgumentException ignored) {}
    }

    /**
     * HostExpirer is a class that contains methods the message listening system calls as UDP host caches respond to our ping.
     * 
     * The UDPHostCache class gets 5 host caches from its list, and sends a ping packet to each one.
     * Then, code in this nested HostExpirer class gets called as the host caches reply with pong packets.
     * After 20 seconds of waiting, the unregistered() method gets called.
     * Code here records which UDP host caches responded to us, and which never did.
     * If this is the 5th time a UDP host cache hasn't gotten back to us, we remove it from our list.
     * 
     * HostExpirer implements the MessageListener interface.
     * This means it has methods like processMessage(m, handler), registered(guid), and unregistered(guid).
     * 
     * UDPPinger.rank() gets passed a new HostExpirer.
     * The message listening system calls processMessage(m, handler) when a UDP host cache gives us a pong message response.
     */
    private class HostExpirer implements MessageListener {

        /*
         * At the start, hosts and allHosts both list all the UDP host caches we're sending a ping to.
         * Each time a host cache sends us a pong in response, we remove it from the hosts list.
         * It's still in the allHosts list.
         * When we're done, hosts will contain just the UDP host caches that never got back to us.
         * allHosts will contain those and the successful caches, too.
         */

        /** The UDP host caches that haven't gotten back to us yet. */
        private final Set hosts = new IpPortSet();
        /** All the UDP host caches we sent the ping packet to. */
        private final Set allHosts;

        /** The GUID of the ping message we sent to the group of UDP host caches. */
        private byte[] guid;

        /**
         * Make a new HostExpirer for the group of UDP host caches we're about to contact.
         * 
         * @param hostsToAdd The list of UDP host caches we're going to contact in this batch
         */
        public HostExpirer(Collection hostsToAdd) {

            // Add them to hosts and allHosts
            hosts.addAll(hostsToAdd);
            allHosts = new HashSet(hostsToAdd);

            // Doesn't seem to do anything
            removeDuplicates(hostsToAdd, hosts);
        }

        /**
         * Removes any hosts that exist in all but not in some.
         * 
         * Only the HostExpirer constructor above calls this method.
         * It copied hostsToAdd into hosts which was empty, so both all and some are the same.
         * It looks like this function can never actually change anything. (do)
         * 
         * @param all  The group of UDP host caches we're going to contact
         * @param some The same group
         */
        private void removeDuplicates(Collection all, Collection some) {

            /*
             * Iterate through what's in our collection vs whats in our set.
             * If any entries exist in the collection but not in the set,
             * then that means they resolved to the same address.
             * Automatically eject entries that resolve to the same address.
             */

            // Determine which ExtendedEndpoints are duplicates (do)
            Set duplicates = new HashSet(all); // Make a HashSet from all, which will remove any duplicates
            duplicates.removeAll(some);        // Remove all of some from duplicates, which should leave it empty

            // Loop through any of the ExtendedEndpoint objects in duplicates that survived that process
            for (Iterator i = duplicates.iterator(); i.hasNext(); ) {
                ExtendedEndpoint ep = (ExtendedEndpoint)i.next();

                // Remove it from the main list the UDPHostCache object keeps
                if (LOG.isDebugEnabled()) LOG.debug("Removing duplicate entry: " + ep);
                remove(ep);
            }
        }

        /**
         * Notification that a UDP host cache has sent us a pong in response to our ping.
         * Removes the cache from the hosts list, but it's still in allHosts.
         * 
         * @param m       The message we received
         * @param handler This is a UDPReplyHandler, but it also is the ExtendedEndpoint of the UDP host cache we just heard from (do)
         */
        public void processMessage(Message m, ReplyHandler handler) {

            // Make sure this is a UDP reply
            if (handler instanceof UDPReplyHandler) {

                // Remove the host that sent us a response from the hosts list
                if (hosts.remove(handler)) { // How are the UDPReplyHandler and the ExtendedEndpoint the same thing? (do)

                    // If it was in there, make a note in the debugging log
                    if (LOG.isTraceEnabled()) LOG.trace("Recieved: " + m);
                }

                /*
                 * OPTIMIZATION: if we've gotten succesful responses from
                 * each hosts, unregister ourselves early.
                 */

                // Every host we contacted has sent us a response, unregister our entire message listener
                if (hosts.isEmpty()) RouterService.getMessageRouter().unregisterMessageListener(guid, this);
            }
        }

        /**
         * Notification that this listener is now registered with the specified GUID.
         * 
         * @param g The GUID of our message we're waiting for a response to and that we registered
         */
        public void registered(byte[] g) {

            // Save the guid in this HostExpirer object
            this.guid = g;
        }
        
        /**
         * Notification that this listener is now unregistered for the specified GUID.
         * This happens once we're done trying to contact this batch of UDP host caches.
         * Calls ep.recordUDPHostCacheFailure() and ep.recordUDPHostCacheSuccess() on the ExtendedEndpoint objects.
         * 
         * The router service calls this 20 seconds after the registration.
         * 
         * @param g The GUID of our message we're waiting for a response to, that we registered, and that we unregistered
         */
        public void unregistered(byte[] g) {

            // Only let one thread enter any of these synchronized blocks at a time
            synchronized (UDPHostCache.this) {

                /*
                 * Record the failures.
                 */

                // Loop for each UDP host cache we sent a ping packet and never got a response
                for (Iterator i = hosts.iterator(); i.hasNext(); ) {
                    ExtendedEndpoint ep = (ExtendedEndpoint)i.next();
                    if (LOG.isTraceEnabled()) LOG.trace("No response from cache: " + ep);
                    
                    // Record one more failure in the ExtendedEndpoint object itself
                    ep.recordUDPHostCacheFailure(); // This information even gets saved into the gnutella.net file

                    // We changed the information in the udpHosts list
                    dirty      = true; // We need to sort it again
                    writeDirty = true; // We need to save the new information to the gnutella.net file

                    // If we've tried to contact this UDP host cache 5 times and its never responded, remove it from the list the UDPHostCache object keeps
                    if (ep.getUDPHostCacheFailures() > MAXIMUM_FAILURES) remove(ep);
                }

                /*
                 * Then, record the successes.
                 * 
                 * hosts contains the caches that never got back to us.
                 * allHosts contains all those we contacted.
                 * Removing all the hosts from allHosts leaves allHosts with just the caches that replied to us.
                 */

                // Remove hosts, the failed caches, from allHosts, leaving just the ones that got back to us.
                allHosts.removeAll(hosts);

                // Loop through each UDP host cache that got back to us
                for (Iterator i = allHosts.iterator(); i.hasNext(); ) {
                    ExtendedEndpoint ep = (ExtendedEndpoint)i.next();
                    if(LOG.isTraceEnabled()) LOG.trace("Valid response from cache: " + ep);

                    // Record one more success in the ExtendedEndpoint object itself
                    ep.recordUDPHostCacheSuccess(); // This information even gets saved into the gnutella.net file

                    // We changed the information in the udpHosts list
                    dirty      = true; // We need to sort it again
                    writeDirty = true; // We need to save the new information to the gnutella.net file
                }
            }
        }
    }

    /**
     * A class that has a compare(a, b) method you can use to determine which of two UDP host caches we've been unable to contact more times.
     * fetchHosts() uses it to sort the udpHosts list by quality.
     */
    private static final Comparator FAILURE_COMPARATOR = new FailureComparator();

    /** A class to hold the compare(a, b) method. */
    private static class FailureComparator implements Comparator {

        /**
         * Compare two UDP host caches by how many times we've sent them a packet and gotten no response back.
         * 
         * @param a An ExtendedEndpoint that has the IP address and port number of a UDP host cache.
         * @param b Another ExtendedEndpoint that has information about a UDP host cache.
         * @return Positive if we've been unable to contact a more, meaning b is better.
         *         0 if we've been unable to contact both exactly the same number of times.
         *         Negative if we've been unable to contact b more, meaning a is better.
         */
        public int compare(Object a, Object b) {

            // Subtract their counts of how many times we've tried to contact them, and gotten no response
            ExtendedEndpoint e1 = (ExtendedEndpoint)a;
            ExtendedEndpoint e2 = (ExtendedEndpoint)b;
            return e1.getUDPHostCacheFailures() - e2.getUDPHostCacheFailures();
        }
    }
}
