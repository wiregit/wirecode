
// Commented for the Learning branch

package com.limegroup.gnutella;

import com.limegroup.gnutella.messages.PingRequest;
import com.limegroup.gnutella.settings.PingPongSettings;

/**
 * Make a ping and send it to all our connections every 3 seconds.
 * Only do this when we're an ultrapeer.
 * 
 * This class continually sends broadcast pings on behalf of an Ultrapeer
 * to update the host caches of both itself and its leaves. This class
 * reduces overall ping and pong traffic because it allows us not to forward
 * pings received from other hosts.  Instead, we use pong caching to respond
 * to those pings with cached pongs, and send pings periodically in this
 * class to obtain fresh host data.
 */
public final class Pinger implements Runnable {

    /** The program's single Pinger object. */
    private static final Pinger INSTANCE = new Pinger();

    /** 3000 milliseconds, As an ultrapeer, we'll ping all our connections every 3 seconds. */
    public static final int PING_INTERVAL = 3000;

    /**
     * Access the Pinger.
     * 
     * @return The program's single Pinger object.
     */
    public static Pinger instance() {

        // Return a reference to the object
        return INSTANCE;
    }

    /** Private constructor so only this class can make the Pinger object. */
    private Pinger() {}

    /**
     * Schedule the pinger to ping our connections every 3 seconds when we're an ultrapeer.
     * RouterService.start() calls this.
     */
    public void start() {

        // Schedule this object with the RouterService to have a thread call run() every 3 seconds
        RouterService.schedule(this, PING_INTERVAL, PING_INTERVAL); // Call first 3 seconds from now, then every 3 seconds after that
    }

    /**
     * As an ultrapeer, broadcast a ping to all our connections.
     * The RouterService has a thread call this every 3 seconds.
     */
    public void run() {

        // Only do this if we're an ultrapeer
        if (RouterService.isSupernode() &&              // We're an ultrapeer, and
            PingPongSettings.PINGS_ACTIVE.getValue()) { // Settings enable pinging

            // Make a new ping, and give it to MessageRouter.broadcastPingRequest
            RouterService.getMessageRouter().broadcastPingRequest(new PingRequest((byte)3)); // Give it a TTL of 3
        }
    }
}
