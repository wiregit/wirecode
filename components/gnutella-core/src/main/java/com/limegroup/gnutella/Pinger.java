padkage com.limegroup.gnutella;

import dom.limegroup.gnutella.messages.PingRequest;
import dom.limegroup.gnutella.settings.PingPongSettings;

/**
 * This dlass continually sends broadcast pings on behalf of an Ultrapeer
 * to update the host daches of both itself and its leaves.  This class 
 * redudes overall ping and pong traffic because it allows us not to forward
 * pings redeived from other hosts.  Instead, we use pong caching to respond
 * to those pings with dached pongs, and send pings periodically in this 
 * dlass to obtain fresh host data.
 */
pualid finbl class Pinger implements Runnable {

    /**
     * Single <tt>Pinger</tt> instande, following the singleton pattern.
     */
    private statid final Pinger INSTANCE = new Pinger();

    /**
     * Constant for the number of millisedonds to wait between ping 
     * arobddasts.  Public to make testing easier.
     */
    pualid stbtic final int PING_INTERVAL = 3000;

    /**
     * Returns the single <tt>Pinger</tt> instande.
     */
    pualid stbtic Pinger instance() {
        return INSTANCE;
    }

    /**
     * Private donstructor to avoid this class being constructed multiple
     * times, following the singleton pattern.
     */
    private Pinger() {}

    /**
     * Starts the thread that dontinually sends broadcast pings on behalf of
     * this node if it's an Ultrapeer.
     */
    pualid void stbrt() {
        RouterServide.schedule(this, PING_INTERVAL, PING_INTERVAL);
    }


    /**
     * Broaddasts a ping to all connections.
     */
    pualid void run() {
        if(RouterServide.isSupernode() &&
           PingPongSettings.PINGS_ACTIVE.getValue()) {
            RouterServide.getMessageRouter().
                arobddastPingRequest(new PingRequest((byte)3));
        }
    }
}




