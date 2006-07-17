package com.limegroup.gnutella.dht;

import java.net.InetSocketAddress;
import java.util.List;

import com.limegroup.gnutella.MessageListener;
import com.limegroup.gnutella.ReplyHandler;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.PingReply;
import com.limegroup.gnutella.messages.PingRequest;
import com.limegroup.gnutella.settings.DHTSettings;
import com.limegroup.gnutella.util.Cancellable;
import com.limegroup.gnutella.util.IpPort;

/**
 * This class takes care of fetching DHT hosts from the Gnutella network 
 * using UDP pings. It uses the ranker from the HostCatcher class, which it cancels
 * when the <tt>AbstractDHTController</tt> is able to bootstrap. 
 * 
 * This class can also start a timer task to periodically requests hosts until 
 * the manager is able to bootstrap. TODO: ability to cancel timer task
 *
 */
public class DHTNodeFetcher {
    
    private static final long FETCH_DELAY = DHTSettings.DHT_NODE_FETCHER_TIME.getValue();
    
    private final DHTController controller;
    
    private long lastRequest = 0L;
    
    private TimedFetcher fetcher = null;
    
    public DHTNodeFetcher(DHTController controller) {
        this.controller = controller;
    }
    
    /**
     * Requests active DHT hosts from the Gnutella network. This method has to be 
     * synchronized because it can be called either directly by the manager
     * or by the timer task.
     */
    public synchronized void requestDHTHosts() {
        if(!RouterService.isConnected()) {
            return;
        }
        
        long now = System.currentTimeMillis();
        if(now - lastRequest < FETCH_DELAY) {
            return;
        }
        
        lastRequest = now;
        Message m = PingRequest.createUDPingWithDHTIPPRequest();
        RouterService.getHostCatcher().sendMessage(m, new DHTNodesRequestListener(), new UDPPingCanceller());
    }
    
    public void startTimerTask() {
        fetcher = new TimedFetcher();
        RouterService.schedule(fetcher, FETCH_DELAY, FETCH_DELAY);
    }
    
    private class TimedFetcher implements Runnable {
        public void run() {
            if (!controller.isWaiting()) {
                return;
            }
            requestDHTHosts();
        }
    }
    
    private class UDPPingCanceller implements Cancellable{
        /** Cancels the HostCatcher pings **/
        public boolean isCancelled() {
            long delay = System.currentTimeMillis() - lastRequest;
            //stop when not waiting anymore OR when not connected to the Gnutella network 
            //OR timeout
            return (!controller.isWaiting() 
                    || !RouterService.isConnected()
                    || (delay > DHTSettings.MAX_NODE_FETCHER_TIME.getValue()));
        }
    }
    
    private class DHTNodesRequestListener implements MessageListener{
        /** Response to our UDP ping **/
        public void processMessage(Message m, ReplyHandler handler) {
            if(!(m instanceof PingReply)) {
                return;
            }
            
            PingReply reply = (PingReply) m;
            List<IpPort> l = reply.getPackedDHTIPPorts();
            
            for (IpPort ipp : l) {
                controller.addBootstrapHost(new InetSocketAddress(ipp.getInetAddress(), ipp.getPort()));
            }
        }
        public void registered(byte[] guid) {}
        public void unregistered(byte[] guid) {}
    }
}
