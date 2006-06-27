package com.limegroup.gnutella.dht;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ThreadFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.LifecycleEvent;
import com.limegroup.gnutella.LifecycleListener;
import com.limegroup.gnutella.MessageListener;
import com.limegroup.gnutella.ReplyHandler;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.PingReply;
import com.limegroup.gnutella.messages.PingRequest;
import com.limegroup.gnutella.messages.vendor.CapabilitiesVM;
import com.limegroup.gnutella.settings.DHTSettings;
import com.limegroup.gnutella.util.Cancellable;
import com.limegroup.gnutella.util.CommonUtils;
import com.limegroup.gnutella.util.IpPort;
import com.limegroup.gnutella.util.IpPortImpl;
import com.limegroup.gnutella.util.ManagedThread;
import com.limegroup.mojito.ContactNode;
import com.limegroup.mojito.KUID;
import com.limegroup.mojito.MojitoDHT;
import com.limegroup.mojito.event.BootstrapListener;

/**
 * The manager for the LimeWire Gnutella DHT. 
 * A node should connect to the DHT only if it has previously been designated as capable 
 * by the <tt>NodeAssigner</tt> or if it is forced to. 
 * Once the node is a DHT node, if <tt>EXCLUDE_ULTRAPEERS</tt> is set to true, 
 * it should no try to connect as an ultrapeer (@see RouterService.isExclusiveDHTNode()). 
 *
 * The NodeAssigner should be the only class to have the authority to 
 * initialize the DHT and connect to the network.
 * 
 * This manager can be in one of the four following states:
 * 1) not running.
 * 2) running and bootstrapping: the dht is trying to bootstrap.
 * 3) running and waiting: the dht has failed the bootstrap and is waiting for additional bootstrap hosts.
 * 3) running and bootstrapped.
 * 
 * The current implementation is dependant on the MojitoDHT. 
 */
public class LimeDHTManager implements LifecycleListener {
    
    private static final Log LOG = LogFactory.getLog(LimeDHTManager.class);
    
    /**
     * The file to persist this Mojito DHT
     */
    private static final File FILE = new File(CommonUtils.getUserSettingsDir(), "mojito.dat");
    
    /** The instance of the DHT */
    private MojitoDHT dht;

    private volatile boolean running = false;
    
    /**
     * A boolean to represent the state when we have failed last bootstrap
     * and are waiting for new bootstrap hosts
     */
    private volatile boolean waiting = false;
    
    /**
     * A list of DHT bootstrap hosts comming from the Gnutella network
     */
    private volatile LinkedList<SocketAddress> bootstrapHosts 
            = new LinkedList<SocketAddress>();
    
    private boolean isActive = false;
    
    private LimeDHTRoutingTable limeDHTRouteTable;
    
    public LimeDHTManager() {
        init();
    }
    
    public void init() {
        MojitoDHT mDHT = null;
        if (DHTSettings.PERSIST_DHT.getValue() && 
                FILE.exists() && FILE.isFile()) {
            try {
                FileInputStream in = new FileInputStream(FILE);
                mDHT = MojitoDHT.load(in);
                in.close();
            } catch (FileNotFoundException e) {
                LOG.error("FileNotFoundException", e);
            } catch (ClassNotFoundException e) {
                LOG.error("ClassNotFoundException", e);
            } catch (IOException e) {
                LOG.error("IOException", e);
            }
        }
        
        if (mDHT == null) {
            dht = new MojitoDHT("LimeMojitoDHT");
        } else {
            dht = mDHT;
        }
        
        limeDHTRouteTable = (LimeDHTRoutingTable) dht.setRoutingTable(LimeDHTRoutingTable.class);
        dht.setMessageDispatcher(LimeMessageDispatcherImpl.class);
        dht.setThreadFactory(new ThreadFactory() {
            public Thread newThread(Runnable runnable) {
                return new ManagedThread(runnable);
            }
        });
    }
    
    /**
     * Start the Mojito DHT and connects it to the network in either passive mode
     * or active mode if we are not firewalled.
     * The start preconditions are the following:
     * 1) if we want to actively connect: We are DHT_CAPABLE OR FORCE_DHT_CONNECT is true 
     * 2) We are not an ultrapeer while excluding ultrapeers from the active network
     * 3) We are not already connected or trying to bootstrap
     * 
     * @param forcePassive true to connect to the DHT in passive mode
     */
    public synchronized void start(boolean forcePassive) {
        if (running) {
            return;
        }
        isActive = !forcePassive;
        
        if(DHTSettings.DISABLE_DHT_NETWORK.getValue() 
                || DHTSettings.DISABLE_DHT_USER.getValue()) { 
            return;
        }
        
        //if we want to connect actively, we either shouldn't be an ultrapeer
        //or should be DHT capable
        if (!forcePassive && !DHTSettings.FORCE_DHT_CONNECT.getValue()) {
            
            if(!DHTSettings.DHT_CAPABLE.getValue() ||
               (RouterService.isSupernode() &&
               DHTSettings.EXCLUDE_ULTRAPEERS.getValue())) {
            
                if(LOG.isDebugEnabled()) {
                    LOG.debug("Cannot initialize DHT - node is not DHT capable or is an ultrapeer");
                }
                return;
            }
        }
        
        //set firewalled status
        dht.setFirewalled(forcePassive);
        
        if(LOG.isDebugEnabled()) {
            LOG.debug("Initializing the DHT");
        }
        
        try {
            InetAddress addr = InetAddress.getByAddress(RouterService.getAddress());
            int port = RouterService.getPort();
            dht.bind(new InetSocketAddress(addr, port));
            dht.start();
            running = true;
            bootstrap();
        } catch (IOException err) {
            LOG.error(err);
        }
    }
    
    /**
     * Tries to bootstrap of a list of bootstrap hosts. 
     * If bootstraping fails, the manager clears the list and 
     * puts itself in a waiting state until new hosts are added. 
     * 
     */
    private void bootstrap() {
        final LinkedList<SocketAddress> snapshot = new LinkedList<SocketAddress>();
        synchronized (bootstrapHosts) {
             snapshot.addAll(bootstrapHosts);
        }
        
        String[] simppHosts = DHTSettings.DHT_BOOTSTRAP_HOSTS.getValue();

        for (int i = 0; i < simppHosts.length; i++) {
            String hostString = simppHosts[i];
            int index = hostString.indexOf(":");
            if(index == hostString.length() -1 || index < 0)
                LOG.error(new UnknownHostException("invalid host: " + hostString));
            
            try {
                String host = hostString.substring(0, index);
                int port = Integer.parseInt(hostString.substring(index+1).trim());
                InetSocketAddress addr = new InetSocketAddress(host, port);
                snapshot.addFirst(addr);
            } catch(NumberFormatException nfe) {
                LOG.error(new UnknownHostException("invalid host: " + hostString));
            }
        }
        
        System.out.println("snapshot:" + snapshot);
        try {
            dht.bootstrap(snapshot, new BootstrapListener() {
                public void noBootstrapHost() {
                    synchronized (bootstrapHosts) {
                        bootstrapHosts.removeAll(snapshot);
                        if(!bootstrapHosts.isEmpty()) {
                            //hosts were added --> try again
                            bootstrap();
                        } else {
                            waiting = true;
                            //send UDPPings -- non blocking
                            sendRequestForDHTHosts();
                        }
                    }
                }
                public void phaseOneComplete(long time) {
                    waiting = false;
                }
                public void phaseTwoComplete(boolean foundNodes, long time) {
                    //Notify our connections that we are now a full DHT node 
                    CapabilitiesVM.reconstructInstance();
                    RouterService.getConnectionManager().sendUpdatedCapabilities();
                }
            });
        } catch (IOException err) {
            LOG.error("IOException", err);
        }
    }
    
    /**
     * Shuts down the dht and persists it
     * 
     */
    public synchronized void shutdown(){
        if (!running) 
            return;
        
        if(LOG.isDebugEnabled()) {
            LOG.debug("Shutting down DHT");
        }
        
        dht.stop();
        running = false;
        waiting = false;
        
        //Notify our connections that we disconnected
        CapabilitiesVM.reconstructInstance();
        RouterService.getConnectionManager().sendUpdatedCapabilities();
        
        try {
            if(DHTSettings.PERSIST_DHT.getValue()) {
                FileOutputStream out = new FileOutputStream(FILE);
                dht.store(out);
                out.close();
            }
        } catch (IOException err) {
            LOG.error("IOException", err);
        }
    }
    
    private void sendRequestForDHTHosts() {
        if(!RouterService.isConnected()) return;
        Message m = PingRequest.createUDPingWithDHTIPPRequest();
        RouterService.getHostCatcher().sendMessage(m, new DHTNodesRequestListener(), new UDPPingCanceller());
    }
    
    /**
     * Adds a host to the head of a list of boostrap hosts ordered by Most Recently Seen.
     * If the manager is waiting for hosts, this method tries to bootstrap 
     * immediately after.
     * 
     * @param hostAddress The SocketAddress of the new bootstrap host.
     */
    public synchronized void addBootstrapHost(SocketAddress hostAddress) {
        synchronized (bootstrapHosts) {
            //Keep bootstrap list small because it should be updated often
            if(bootstrapHosts.size() >= 10) {
                bootstrapHosts.removeLast();
            }
            
            //always put/replace the host to the head of the list
            bootstrapHosts.remove(hostAddress);
            bootstrapHosts.addFirst(hostAddress);
        }
        System.out.println("adding: "+hostAddress);
        System.out.println("waiting: "+waiting);
        if(waiting) {
            waiting = false;
            bootstrap();
        }
    }
    
    public synchronized void addLeafDHTNode(String host, int port) {
        if(!running) 
            return;
        InetSocketAddress addr = new InetSocketAddress(host, port);
        if(waiting) {
            addBootstrapHost(addr);
        } else {
            limeDHTRouteTable.addLeafDHTNode(addr);
        }
    }
    
    public synchronized void removeLeafDHTNode(String host, int port) {
        if(!running) 
            return;
        limeDHTRouteTable.removeLeafDHTNode(host, port);
    }
    
    public boolean isRunning() {
        return running;
    }
    
    public boolean isActiveNode() {
        return running & isActive;
    }
    
    public boolean isPassiveNode() {
        return running & !isActive;
    }
    
    /**
     * Sets the mode 
     * 
     * 
     * @param passive
     */
    public void setPassive(boolean passive) {
        boolean wasPassive = dht.isFirewalled();
        if((passive && wasPassive) || (!passive && !wasPassive)) 
            return; //no change
        
        boolean wasRunning = running;
        shutdown();
        
        //we are becoming active: load dht from last active session
        if(wasPassive && !passive) {
            DHTSettings.PERSIST_DHT.setValue(true);
            init();
        } 
        //we are becoming passive: start new DHT with new nodeID so that 
        //the node is not part of the DHT anymore
        else if(!wasPassive && passive) {
            DHTSettings.PERSIST_DHT.setValue(false);
            init();
        } 
        
        if(wasRunning)
            start(true);
    }

    /**
     * Shuts the DHT down if we got disconnected from the network.
     */
    public void handleLifecycleEvent(LifecycleEvent evt) {
        if(evt.isConnectedEvent()) {
            //protect against change of state
            if(DHTSettings.EXCLUDE_ULTRAPEERS.getValue() 
                    && RouterService.isSupernode()) {
                setPassive(true);
            }
            
            //we were waiting and had no connection to the gnutella network
            //now we do --> start sending UDP pings to request bootstrap nodes
            if(waiting) {
                sendRequestForDHTHosts();
            }
            return;
        } else if(evt.isDisconnectedEvent() || evt.isNoInternetEvent()) {
            if(running && !DHTSettings.FORCE_DHT_CONNECT.getValue()) {
                shutdown();
            }
        }
    }
    
    public MojitoDHT getMojitoDHT() {
        return dht;
    }
    
    public void addressChanged() {
        if(!running) 
            return;
        dht.stop(); //the node assigner will take care of restarting it!
    }
    
    public int getVersion() {
        return dht.getVersion();
    }
    
    public boolean isWaiting() {
        return waiting;
    }
    
    public Collection<IpPort> getDHTNodes(int numNodes){
        if(!running) {
            return Collections.emptyList();
        }
        
        List<IpPort> ipps = new ArrayList<IpPort>();
        Collection<ContactNode> nodes = limeDHTRouteTable.getMRSNodes(numNodes);
        KUID localNode = dht.getLocalNodeID();
        for(ContactNode cn : nodes) {
            if(!isActive && cn.getNodeID().equals(localNode)) {
                continue;
            }
            
            ipps.add(new IpPortContactNode(cn));
        }
        return ipps;
    }
    
    
    
    private class IpPortContactNode implements IpPort {
        
        private final InetAddress nodeAddress;
        
        private final int port;
        
        public IpPortContactNode(ContactNode node) {
            InetSocketAddress addr = (InetSocketAddress) node.getSocketAddress();
            this.nodeAddress = addr.getAddress();
            this.port = addr.getPort();
        }
        
        public String getAddress() {
            return nodeAddress.getHostAddress();
        }

        public InetAddress getInetAddress() {
            return nodeAddress;
        }

        public int getPort() {
            return port;
        }
    }
    
    private class UDPPingCanceller implements Cancellable{
        public boolean isCancelled() {
            //stop when not waiting anymore OR when not connected to the Gnutella network
            return (!waiting || !RouterService.isConnected());
        }
    }
    
    private class DHTNodesRequestListener implements MessageListener{
        public void processMessage(Message m, ReplyHandler handler) {
            if(!(m instanceof PingReply)) return;
            PingReply reply = (PingReply) m;
            List<IpPort> l = reply.getPackedIPPorts();
            
            for (IpPort ipp : l) {
                addBootstrapHost(new InetSocketAddress(ipp.getInetAddress(), ipp.getPort()));
            }
        }

        public void registered(byte[] guid) {}

        public void unregistered(byte[] guid) {}
    }

}
