package com.limegroup.gnutella;

import com.limegroup.gnutella.messages.*;

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
        Thread pingThread = new Thread(this, "pinger thread");
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
                if(RouterService.isSupernode()) {
                    RouterService.getMessageRouter().
                        broadcastPingRequest(new PingRequest((byte)4));
                }
                
                Thread.sleep(1000);
            }
        } catch(Throwable t) {
            ErrorService.error(t);
        }
    }
}




