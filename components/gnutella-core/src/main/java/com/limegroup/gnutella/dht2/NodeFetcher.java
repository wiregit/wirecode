package com.limegroup.gnutella.dht2;

import java.io.Closeable;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.collection.Cancellable;
import org.limewire.concurrent.ExecutorsHelper;
import org.limewire.core.settings.ConnectionSettings;
import org.limewire.core.settings.DHTSettings;
import org.limewire.io.IpPort;
import org.limewire.io.IpPortImpl;
import org.limewire.io.NetworkUtils;

import com.google.inject.Provider;
import com.limegroup.gnutella.ConnectionServices;
import com.limegroup.gnutella.ExtendedEndpoint;
import com.limegroup.gnutella.HostCatcher;
import com.limegroup.gnutella.MessageListener;
import com.limegroup.gnutella.ReplyHandler;
import com.limegroup.gnutella.UDPPinger;
import com.limegroup.gnutella.UniqueHostPinger;
import com.limegroup.gnutella.dht2.DHTManager.DHTMode;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.PingReply;
import com.limegroup.gnutella.messages.PingRequestFactory;

/**
 * 
 */
class NodeFetcher implements Closeable {

    private static final Log LOG = LogFactory.getLog(NodeFetcher.class);
    
    private static ScheduledExecutorService EXECUTOR 
        = Executors.newSingleThreadScheduledExecutor(
            ExecutorsHelper.defaultThreadFactory("NodeFetcherThread"));
    
    private final Callback callback;

    private final ConnectionServices connectionServices;
    
    private final Provider<HostCatcher> hostCatcher;
    
    private final PingRequestFactory pingRequestFactory;
    
    private final Provider<UniqueHostPinger> uniqueHostPinger;
    
    private final Provider<UDPPinger> udpPinger;
    
    private final long frequency;
    
    private final TimeUnit unit;
    
    private final AtomicBoolean barrier 
        = new AtomicBoolean(false);
    
    private ScheduledFuture<?> future = null;
    
    private boolean open = true;
    
    public NodeFetcher(Callback callback, 
            ConnectionServices connectionServices,
            Provider<HostCatcher> hostCatcher,
            PingRequestFactory pingRequestFactory,
            Provider<UniqueHostPinger> uniqueHostPinger,
            Provider<UDPPinger> udpPinger) {
        this(callback, connectionServices, hostCatcher, 
                pingRequestFactory, uniqueHostPinger, udpPinger,
                DHTSettings.DHT_NODE_FETCHER_TIME.getValue(), 
                TimeUnit.MILLISECONDS);
    }
    
    public NodeFetcher(Callback callback, 
            ConnectionServices connectionServices,
            Provider<HostCatcher> hostCatcher,
            PingRequestFactory pingRequestFactory,
            Provider<UniqueHostPinger> uniqueHostPinger,
            Provider<UDPPinger> udpPinger,
            long frequency, TimeUnit unit) {
        
        this.callback = callback;
        this.connectionServices = connectionServices;
        this.hostCatcher = hostCatcher;
        this.pingRequestFactory = pingRequestFactory;
        this.uniqueHostPinger = uniqueHostPinger;
        this.udpPinger = udpPinger;
        
        this.frequency = frequency;
        this.unit = unit;
    }
    
    /**
     * 
     */
    public synchronized boolean isRunning() {
        return open && future != null && !future.isDone();
    }
    
    /**
     * 
     */
    public synchronized void start() {
        if (!open) {
            throw new IllegalStateException();
        }
        
        if (isRunning()) {
            return;
        }
        
        Runnable task = new Runnable() {
            @Override
            public void run() {
                process();
            }
        };
        
        long delay = (long)(frequency * Math.random());
        future = EXECUTOR.scheduleWithFixedDelay(
                task, delay, frequency, unit);
    }
    
    /**
     * 
     */
    public synchronized void stop() {
        if (future != null) {
            future.cancel(true);
        }
    }
    
    @Override
    public synchronized void close() {
        open = false;
        stop();
    }
    
    /**
     * 
     */
    private void process() {
        
        // Do nothing if we're not connected
        if (!connectionServices.isConnected()) {
            return;
        }
        
        List<ExtendedEndpoint> dhtHosts = 
            hostCatcher.get().getDHTSupportEndpoint(0);
        
        // See if we have any ACTIVE nodes in the HostCatcher. The list
        // is ordered by ACTIVE nodes first, we can therefore exit as
        // soon as we encounter a non-ACTIVE node.
        boolean haveActive = false;
        for (ExtendedEndpoint ep : dhtHosts) {
            if (ep.getDHTMode() != DHTMode.ACTIVE) {
                break;
            }
            
            haveActive = true;
            callback.addActiveNode(ep.getInetSocketAddress());
        }
        
        if (haveActive) {
            return;
        }
        
        if (barrier.get()) {
            return;
        }
        
        Message m = pingRequestFactory.createUDPingWithDHTIPPRequest();
        MessageCallback messageCallback = new MessageCallback();
        
        if (!dhtHosts.isEmpty()) { 
            // We don't have ACTIVE nodes but have nodes that support DHT
            uniqueHostPinger.get().rank(dhtHosts, 
                    messageCallback, messageCallback, m);
        } else {
            // Send the request to all hosts
            hostCatcher.get().sendMessageToAllHosts(m, 
                    messageCallback, messageCallback);
        }
    }
    
    /**
     * 
     */
    public boolean ping(SocketAddress address) {
        synchronized (this) {
            if (!open) {
                return false;
            }
        }
        
        if (!connectionServices.isConnected()) {
            return false;
        }   
        
        IpPort ipp = new IpPortImpl((InetSocketAddress) address);
        
        if (LOG.isDebugEnabled()) {
            LOG.debug("Requesting DHT hosts from host " + address);
        }
        
        if (!barrier.getAndSet(true)) {
            Message m = pingRequestFactory.createUDPingWithDHTIPPRequest();
            
            udpPinger.get().rank(Arrays.asList(ipp), 
                    new SingleMessageCallback(), null, m, -1);
            return true;
        }
        
        return false;
    }
    
    /**
     * 
     */
    private void processMessage(Message m) {
        if (!(m instanceof PingReply)) {
            return;
        }
        
        if (!isRunning()) {
            return;
        }
        
        PingReply reply = (PingReply) m;
        Collection<IpPort> list = reply.getPackedDHTIPPorts();
        
        if (ConnectionSettings.FILTER_CLASS_C.getValue()) {
            list = NetworkUtils.filterOnePerClassC(list);
        }
        
        if (LOG.isDebugEnabled()) {
            LOG.debug("Received ping reply from "+reply.getAddress());
        }
        
        for (IpPort ipp : list) {
            callback.addActiveNode(ipp.getInetSocketAddress());
        }
    }
    
    /**
     * 
     */
    private class MessageCallback implements MessageListener, Cancellable {
        
        private volatile boolean cancelled = false;

        @Override
        public void processMessage(Message m, ReplyHandler handler) {
            cancelled = true;
            NodeFetcher.this.processMessage(m);
        }

        @Override
        public void registered(byte[] guid) {
        }

        @Override
        public void unregistered(byte[] guid) {
        }

        @Override
        public boolean isCancelled() {
            return cancelled;
        }
    }
    
    /**
     * 
     */
    private class SingleMessageCallback extends MessageCallback {

        @Override
        public void processMessage(Message m, ReplyHandler handler) {
            barrier.set(false);
            super.processMessage(m, handler);
        }
        
        @Override
        public void unregistered(byte[] guid) {
            barrier.set(false);
            super.unregistered(guid);
        }
    }
    
    /**
     * 
     */
    public static interface Callback {
        
        /**
         * 
         */
        public void addActiveNode(SocketAddress address);
    }
}
