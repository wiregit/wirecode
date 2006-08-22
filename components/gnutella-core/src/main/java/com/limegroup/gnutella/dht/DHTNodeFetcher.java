package com.limegroup.gnutella.dht;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.ExtendedEndpoint;
import com.limegroup.gnutella.MessageListener;
import com.limegroup.gnutella.ReplyHandler;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.PingReply;
import com.limegroup.gnutella.messages.PingRequest;
import com.limegroup.gnutella.settings.DHTSettings;
import com.limegroup.gnutella.util.Cancellable;
import com.limegroup.gnutella.util.IpPort;
import com.limegroup.gnutella.util.IpPortImpl;

/**
 * This class takes care of fetching DHT hosts from the Gnutella network. 

 * Implicitely, this will also propagate the knowledge through the network,
 * as the MessageRouter forwards pongs to the leafs!
 * 
 * First tries to get active nodes directly from the HostCatcher. if that fails, 
 * it tries to send UDP pings to nodes who support the DHT. If that fails too, it
 * sends UDP pings to all nodes in the HostCatcher.
 * 
 * This uses the ranker from the HostCatcher class, which it cancels
 * when the <tt>AbstractDHTController</tt> is able to bootstrap. 
 * 
 * This class can also start a timer task to periodically requests hosts until 
 * the manager is able to bootstrap. TODO: ability to cancel timer task
 *
 */
public class DHTNodeFetcher {
    
    private static final Log LOG = LogFactory.getLog(DHTNodeFetcher.class);
    
    private final DHTBootstrapper bootstrapper;
    
    private volatile long lastRequest = 0L;
    
    private TimedFetcher fetcher = null;
    
    public DHTNodeFetcher(DHTBootstrapper bootstrapper) {
        this.bootstrapper = bootstrapper;
    }
    
    /**
     * Requests active DHT hosts from the Gnutella network. This method has to be 
     * synchronized because it can be called either directly by the manager
     * or by the timer task.
     */
    public synchronized void requestDHTHosts() {
        
        LOG.debug("Requesting DHT hosts");
        
        if(!RouterService.isConnected()) {
            return;
        }
        
        //TODO: min version: for now, 0
        List<ExtendedEndpoint> dhtHosts = 
            RouterService.getHostCatcher().getDHTSupportEndpoint(0);
        
        //first see if we have any active dht node in our HostCatcher and add them all 
        //to the bootstrapper.
        //The list is ordered by Active nodes first so as soon as we get an inactive
        //node we can exit the loop.
        boolean haveActive = false;
        for(ExtendedEndpoint ep : dhtHosts) {
            if(ep.getDHTMode().isActive()) {
                
                if(LOG.isDebugEnabled()){
                    LOG.debug("Adding active host from HostCatcher: "+ ep.getAddress());
                }
                
                haveActive = true;
                bootstrapper.addBootstrapHost(new InetSocketAddress(ep.getAddress(), ep.getPort()));
            } else {
                break;
            }
        }
        
        if(haveActive) { //we have added active hosts already - no need to request
            return;
        }
        
        //We don't have active hosts --> send UDP pings
        long now = System.currentTimeMillis();
        if(now - lastRequest < DHTSettings.DHT_NODE_FETCHER_TIME.getValue()) {
            return;
        }
        
        lastRequest = now;
        Message m = PingRequest.createUDPingWithDHTIPPRequest();
        
        if(!dhtHosts.isEmpty()) { 
            
            LOG.debug("Sending ping to dht capable hosts");
            
            //we don't have active hosts but have hosts that support dht
            RouterService.getHostCatcher().sendMessage(m, dhtHosts, 
                    new DHTNodesRequestListener(), new UDPPingCanceller());
        } else {
            
            LOG.debug("Sending ping to all hosts");
            
            //send to all hosts
            RouterService.getHostCatcher().sendMessage(m, 
                    new DHTNodesRequestListener(), new UDPPingCanceller());
        }
    }
    
    public void requestDHTHosts(SocketAddress hostAddress) {
        
        LOG.debug("Requesting DHT hosts from host " + hostAddress);
        
        if(!RouterService.isConnected()) {
            return;
        }   
        
        if(!(hostAddress instanceof InetSocketAddress)) {
            return;
        }
        
        IpPort ipp = new IpPortImpl((InetSocketAddress)hostAddress);
        
        Message m = PingRequest.createUDPingWithDHTIPPRequest();
        RouterService.getHostCatcher().sendMessage(m, Arrays.asList(ipp), 
                new DHTNodesRequestListener(), new UDPPingCanceller());
    }
    
    public void startTimerTask() {
        fetcher = new TimedFetcher();
        long fetcherTime = DHTSettings.DHT_NODE_FETCHER_TIME.getValue();
        long initialFetch = (long) (Math.random() * fetcherTime);
        RouterService.schedule(fetcher, initialFetch, fetcherTime);
    }
    
    private class TimedFetcher implements Runnable {
        public void run() {
            if (!bootstrapper.isWaitingForNodes()) {
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
            boolean cancel = (delay > DHTSettings.MAX_NODE_FETCHER_TIME.getValue())
                                        || !RouterService.isConnected()
                                        || (!bootstrapper.isWaitingForNodes());
            if(cancel && LOG.isDebugEnabled()){
                LOG.debug("Cancelling UDP ping after "+delay+" ms");
            }
            return cancel;
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

            if(LOG.isDebugEnabled()) {
                LOG.debug("Received ping reply from "+reply.getAddress());
            }
            
            for (IpPort ipp : l) {
                bootstrapper.addBootstrapHost(
                        new InetSocketAddress(ipp.getInetAddress(), ipp.getPort()));
            }
        }
        public void registered(byte[] guid) {}
        public void unregistered(byte[] guid) {}
    }
}
