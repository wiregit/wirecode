
// Commented for the Learning branch

package com.limegroup.gnutella;

import java.util.Collection;
import java.util.Iterator;

import com.limegroup.gnutella.messages.PingRequest;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.util.Cancellable;
import com.limegroup.gnutella.util.IpPort;
import com.limegroup.gnutella.util.ProcessingQueue;

import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

/**
 * Send a Gnutella ping packet to a list of IP addresses using UDP.
 */
public class UDPPinger {

    /** We can write lines of text to this debugging log to see how the program acts while it is running. */
    private static final Log LOG = LogFactory.getLog(UDPPinger.class);

    /**
     * A ProcessingQueue we'll use to have another thread run some code here.
     * 
     * A ProcessingQueue object runs code for us in a separate thread.
     * We make one named QUEUE, and then call QUEUE.add(r), where r is an object that has a run() method.
     * The ProcessingQueue starts a separate thread named "UDPHostRanker" which calls the object's run() method.
     * 
     * This member is protected because UniqueHostPinger, which extends UDPPinger, also uses it.
     */
    protected static final ProcessingQueue QUEUE = new ProcessingQueue("UDPHostRanker");

    /** 20 seconds, expire a message listener after 20 seconds. */
    public static int LISTEN_EXPIRE_TIME = 20 * 1000;

    /** Half a second, send a ping every half second. */
    private static final long SEND_INTERVAL = 500;

    /** 15, we'll send up to 15 pings in each half second. */
    private static final int MAX_SENDS = 15;

    /** The number of UDP packets we've sent in the past half second. */
    private static int _sentAmount;

    /** The time we last sent a UDP packet. */
    private static long _lastSentTime;

    /**
     * Not used.
     * Send a Gnutella ping packet to a list of IP addresses and port numbers using UDP.
     * 
     * @param hosts A list of remote computers to send the Gnutella message
     */
    public void rank(Collection hosts) {

        // Call the other method with null for the features we're not using
        rank(hosts, null, null, null);
    }

    /**
     * Not used.
     * Send a Gnutella ping packet to a list of IP addresses and port numbers using UDP.
     * 
     * @param hosts   A list of remote computers to send the Gnutella message
     * @param message The Gnutella message to send to all the computers
     */
    public void rank(Collection hosts, Message message) {

        // Call the other method with null for the features we're not using
        rank(hosts, null, null, message);
    }

    /**
     * Send a Gnutella ping packet to a list of IP addresses and port numbers using UDP.
     * HostCatcher.rank(Collection) calls this.
     * 
     * @param hosts     A list of remote computers to send the Gnutella message
     * @param canceller We'll call canceller.isCancelled() to see if another part of the program has cancelled this request
     */
    public void rank(Collection hosts, Cancellable canceller) {

        // Call the other method with null for the features we're not using
        rank(hosts, null, canceller, null);
    }

    /**
     * Not used.
     * Send a Gnutella ping packet to a list of IP addresses and port numbers using UDP.
     * 
     * @param hosts    A list of remote computers to send the Gnutella message
     * @param listener An object that wants to know when we get a response related to the message, or null to not do this
     */
    public void rank(Collection hosts, MessageListener listener) {

        // Call the other method with null for the features we're not using
        rank(hosts, listener, null, null);
    }
    
    /**
     * Not used.
     * Send a Gnutella ping packet to a list of IP addresses and port numbers using UDP.
     * 
     * @param hosts     A list of remote computers to send the Gnutella message
     * @param listener  An object that wants to know when we get a response related to the message, or null to not do this
     * @param canceller We'll call canceller.isCancelled() to see if another part of the program has cancelled this request
     */
    public void rank(Collection hosts, MessageListener listener, Cancellable canceller) {

        // Call the other method with null for the features we're not using
        rank(hosts, listener, canceller, null);
    }

    /**
     * Send a Gnutella ping packet to a list of IP addresses and port numbers using UDP.
     * PingRanker.pingNewHosts() calls this.
     * UDPHostCache.fetch(Collection) calls this.
     * PingRanker.pingProxies(RemoteFileDesc) calls this.
     * A call from HostCatcher.rank(Collection) leads here.
     * ConnectionChecker.udpIsDead() calls this.
     * 
     * @param hosts     A list of remote computers to send the Gnutella message
     * @param listener  An object that wants to know when we get a response related to the message, or null to not do this
     * @param canceller We'll call canceller.isCancelled() to see if another part of the program has cancelled this request
     * @param message   The Gnutella message to send to all the computers
     */
    public void rank(final Collection hosts, final MessageListener listener, Cancellable canceller, final Message message) {

        // Make sure the caller actually gave us some IP addresses and port numbers in the Collection object named hosts
        if (hosts == null) throw new NullPointerException("null hosts not allowed");

        // If the caller didn't give us an object named canceller that we can call canceller.isCancelled() on
        if (canceller == null) {

            // Make a new object that supports the Cancellable interface right here
            canceller = new Cancellable() {

                // To implement the Cancellable interface we have to write an isCancelled() method
                public boolean isCancelled() {

                    // Calling canceller.isCancelled() always returns, this object is not cancelled and no one can cancel it
                    return false;
                }
            };
        }

        /*
         * Make a new SenderBundle object which wraps together the 4 parameters.
         * Add it to the ProcessingQueue named QUEUE.
         * The queue will make a thread called "UDPHostRanker", which will call run() on the SenderBundle.
         * That run() method has one line of code: send(hosts, listener, canceller, message).
         * The send() method sends the Gnutella message to all the computers in the host list, careful to not go too fast.
         */

        // Have the "UDPHostRanker" thread send the message to all the computers in the host list
        QUEUE.add(new SenderBundle(hosts, listener, canceller, message));
    }

    /**
     * When the "UDPHostRanker" thread calls this method, it waits here for the UDPService to start listening.
     * If it waits for more than 6 seconds, it gives up and returns false.
     * 
     * @param canceller An object we can call isCancelled() on to see if the program still wants us to do this
     * @return          True if the UDPService started listening, false if we waited for 6 seconds and gave up
     */
    private boolean waitForListening(Cancellable canceller) {

        // Loop until the UDPService is ready, we've waited more than 6 seconds for it, or another part of the program cancelled this request
        int waits = 0;
        while (!UDPService.instance().isListening() && waits < 10 && !canceller.isCancelled()) {

            try {

                // Have the "UDPHostRanker" thread sleep here for 0.6 seconds
                Thread.sleep(600);

            // An InterruptedException should never happen
            } catch (InterruptedException e) { ErrorService.error(e); }

            // Count that we waited one more time
            waits++;
        }

        // Return false if we stopped waiting because 6 seconds ran out
        return waits < 10;
    }

    /**
     * Send a Gnutella message in a UDP packet to a list of remote computers.
     * Sleeps to keep from sending the messages too fast.
     * 
     * @param hosts     A list of IP address and port numbers, we'll send the Gnutella packet to each of these computers
     * @param listener  An object that wants to know when we get a response about one of these packets
     * @param canceller We can call canceller.isCancelled() to see if the caller doesn't want us to do this anymore
     * @param message   The Gnutella packet to send to all the comptuers
     */
    protected void send(Collection hosts, final MessageListener listener, Cancellable canceller, Message message) {

        // Wait for up to 6 seconds for the UDPService to start listening
        if (!waitForListening(canceller)) return; // It never did, leave now without trying to send anything

        // If the caller didn't pass us a Gnutella packet, make a new UDP ping Gnutella packet
        if (message == null) message = PingRequest.createUDPPing();

        // If the caller gave us a MessageListener, register it with the message's GUID so the program will call the listener when (do)
        final byte[] messageGUID = message.getGUID(); // Read the message's GUID
        if (listener != null) RouterService.getMessageRouter().registerMessageListener(messageGUID, listener);

        // Send the Gnutella message to each IP address and port number in the hosts list
        Iterator iter = hosts.iterator();
        while (iter.hasNext() && !canceller.isCancelled()) sendSingleMessage((IpPort)iter.next(), message);

        // If we registered a MessageListener, we have to make sure the registration doesn't last forever
        if (listener != null) {

            /*
             * Now schedule a runnable that will remove the mapping for the GUID
             * of the above message after 20 seconds so that we don't store it
             * indefinitely in memory for no reason.
             */

            // Make a new object named udpMessagePurger that just has a run() method with some code in it
            Runnable udpMessagePurger = new Runnable() {

                // This is the code the RouterService will run 20 seconds from now
                public void run() {

                    // Remove the registration we created for this packet
                    RouterService.getMessageRouter().unregisterMessageListener(messageGUID, listener);
                }
            };

            // Have the router service run that code 20 seconds from now
            RouterService.schedule(udpMessagePurger, LISTEN_EXPIRE_TIME, 0);
        }
    }

    /**
     * Send a Gnutella message in a UDP packet to an IP address and port number of a remote computer on the Internet.
     * Sleeps to keep from sending messages too quickly.
     * 
     * @param host    The IP address and port number of a remote computer
     * @param message A Gnutella packet
     */
    protected void sendSingleMessage(IpPort host, Message message) {

        // Find out what time it is now
        long now = System.currentTimeMillis();

        // It's been more than half a second since we last sent a ping
        if (now > _lastSentTime + SEND_INTERVAL) {

            // Zero our count of how many pings we've sent in this half-second
            _sentAmount = 0;

        // We're still within the same half-second, and we've reached our maximum of 15 pings each half second
        } else if (_sentAmount == MAX_SENDS) {

            try {

                // Have the "UDPHostRanker" thread sleep here for half a second, that will definitely put us into the next half-second time period
                Thread.sleep(SEND_INTERVAL);
                now = System.currentTimeMillis(); // Find out what time it is when we wake up

            // If another thread calls interrupt(), ignore it and keep going
            } catch (InterruptedException ignored) {}

            // Zero our count of how many pings we've sent in this half-second
            _sentAmount = 0;
        }

        /*
         * We've waited long enough to send one more ping packet
         */

        // Make a note in the log that we're going to send a ping packet to this remote computer
        if (LOG.isTraceEnabled()) LOG.trace("Sending to " + host + ": " + message.getClass() + " " + message);

        // Have the UDPService object send it
        UDPService.instance().send(message, host);

        // Record that we sent one more packet right now
        _sentAmount++;
        _lastSentTime = now;
    }

    /**
     * A SenderBundle object wraps a single Gnutella packet and the many IP addresses and port numbers we'll send it to.
     * It implements the Runnable interface, requiring it to have a run() method.
     * When another thread calls the run() method, code in the UDPPinger class loops through the addresses and sends the packet to each computer.
     */
    private class SenderBundle implements Runnable {

        /** A list of IP addresses and port numbers of remote computers we'll send this Gnutella packet. */
        private final Collection hosts;

        /** An object that wants to know when we receive a response related to the Gnutella packet. */
        private final MessageListener listener;

        /** We'll call canceller.isCancelled() to see if the program doesn't want us to do this anymore. */
        private final Cancellable canceller;

        /** The Gnutella packet we'll send all the computers. */
        private final Message message;

        /**
         * Make a new SenderBundle object.
         * 
         * @param hosts     A list of IP addresses and port numbers of remote computers we'll send this Gnutella packet
         * @param listener  An object that wants to know when we receive a response related to the Gnutella packet
         * @param canceller We'll call canceller.isCancelled() to see if the program doesn't want us to do this anymore
         * @param message   The Gnutella packet we'll send all the computers
         */
        public SenderBundle(Collection hosts, MessageListener listener, Cancellable canceller, Message message) {

            // Save all the given objects
            this.hosts     = hosts;
            this.listener  = listener;
            this.canceller = canceller;
            this.message   = message;
        }

        /**
         * Send the Gnutella message to the list of remote computers.
         * A SenderBundle object is Runnable, so here is its run() method.
         * A separate thread calls this, so it's OK that it sleeps to not send packets too fast.
         */
        public void run() {

            // Send the Gnutella message to the list of remote computers
            send(hosts, listener, canceller, message);
        }
    }
}
