package com.limegroup.gnutella;

import com.limegroup.gnutella.messages.PingRequest;
import com.limegroup.gnutella.settings.PingPongSettings;
import com.limegroup.gnutella.util.ManagedThread;

/**
 * This class continually sends broadcast pings on behalf of an Ultrapeer
 * to update the host caches of both itself and its leaves.  This class 
 * reduces overall ping and pong traffic because it allows us not to forward
 * pings received from other hosts.  Instead, we use pong caching to respond
 * to those pings with cached pongs, and send pings periodically in this 
 * class to obtain fresh host data.
 */
public final class Pinger implements Runnable {

    /**
     * Single <tt>Pinger</tt> instance, following the singleton pattern.
     */
    private static final Pinger INSTANCE = new Pinger();

    /**
     * Constant for the number of milliseconds to wait between ping 
     * broadcasts.  Public to make testing easier.
     */
    public static final int PING_INTERVAL = 3000;

    /**
     * Returns the single <tt>Pinger</tt> instance.
     */
    public static Pinger instance() {
        return INSTANCE;
    }

    /**
     * Private constructor to avoid this class being constructed multiple
     * times, following the singleton pattern.
     */
    private Pinger() {}

    /**
     * Starts the thread that continually sends broadcast pings on behalf of
     * this node if it's an Ultrapeer.
     */
    public void start() {
        Thread pingThread = new ManagedThread(this, "pinger thread");
        pingThread.setDaemon(true);
        pingThread.start();
    }


    /**
     * Implements the <tt>Runnable</tt> interface.  Periodically sends broadcast
     * pings along all connections.
     */
    public void run() {
        try {
            while(true) {
                if(RouterService.isSupernode() &&
                   PingPongSettings.PINGS_ACTIVE.getValue()) {
                    RouterService.getMessageRouter().
                        broadcastPingRequest(new PingRequest((byte)3));
                }
                
                Thread.sleep(PING_INTERVAL);
            }
        } catch(Throwable t) {
            ErrorService.error(t);
        }
    }
}




