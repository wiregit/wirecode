package com.limegroup.gnutella.dht;

import java.io.Closeable;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.collection.Cancellable;
import org.limewire.core.settings.ConnectionSettings;
import org.limewire.core.settings.DHTSettings;
import org.limewire.io.IpPort;
import org.limewire.io.IpPortImpl;
import org.limewire.io.NetworkUtils;
import org.limewire.mojito.util.EventUtils;
import org.limewire.mojito.util.SchedulingUtils;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.limegroup.gnutella.ConnectionServices;
import com.limegroup.gnutella.ExtendedEndpoint;
import com.limegroup.gnutella.HostCatcher;
import com.limegroup.gnutella.MessageListener;
import com.limegroup.gnutella.ReplyHandler;
import com.limegroup.gnutella.UDPPinger;
import com.limegroup.gnutella.UniqueHostPinger;
import com.limegroup.gnutella.dht.DHTManager.DHTMode;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.PingReply;
import com.limegroup.gnutella.messages.PingRequestFactory;

/**
 * The {@link NodeFetcher} sends Gnutella PINGs (with special flags)
 * and receives some DHT nodes in return.
 */
public class NodeFetcher implements Closeable {

    private static final Log LOG 
        = LogFactory.getLog(NodeFetcher.class);
    
    private final List<NodeFetcherListener> listeners 
        = new CopyOnWriteArrayList<NodeFetcherListener>();
    
    private final ConnectionServices connectionServices;
    
    private final Provider<HostCatcher> hostCatcher;
    
    private final PingRequestFactory pingRequestFactory;
    
    private final Provider<UniqueHostPinger> uniqueHostPinger;
    
    private final Provider<UDPPinger> udpPinger;
    
    private final long frequency;
    
    private final TimeUnit unit;
    
    private final AtomicBoolean barrier 
        = new AtomicBoolean(false);
    
    private volatile long expireTimeInMillis = -1L;
    
    private ScheduledFuture<?> future = null;
    
    private boolean open = true;
    
    @Inject
    public NodeFetcher(ConnectionServices connectionServices,
            Provider<HostCatcher> hostCatcher,
            PingRequestFactory pingRequestFactory,
            Provider<UniqueHostPinger> uniqueHostPinger,
            Provider<UDPPinger> udpPinger) {
        this(connectionServices, hostCatcher, 
                pingRequestFactory, uniqueHostPinger, udpPinger,
                DHTSettings.DHT_NODE_FETCHER_TIME.getTimeInMillis(), 
                TimeUnit.MILLISECONDS);
    }
    
    public NodeFetcher(ConnectionServices connectionServices,
            Provider<HostCatcher> hostCatcher,
            PingRequestFactory pingRequestFactory,
            Provider<UniqueHostPinger> uniqueHostPinger,
            Provider<UDPPinger> udpPinger,
            long frequency, TimeUnit unit) {
        
        this.connectionServices = connectionServices;
        this.hostCatcher = hostCatcher;
        this.pingRequestFactory = pingRequestFactory;
        this.uniqueHostPinger = uniqueHostPinger;
        this.udpPinger = udpPinger;
        
        this.frequency = frequency;
        this.unit = unit;
    }
    
    /**
     * Sets the expiration time.
     */
    public void setExpireTime(long time, TimeUnit unit) {
        if (time >= Integer.MAX_VALUE) {
            throw new IllegalArgumentException("time=" + time);
        }
        
        this.expireTimeInMillis = (int)(time >= 0L ? unit.toMillis(time) : -1);
    }
    
    /**
     * Returns the expire time in the given {@link TimeUnit}.
     */
    public long getExpireTime(TimeUnit unit) {
        return unit.convert(expireTimeInMillis, TimeUnit.MILLISECONDS);
    }
    
    /**
     * Returns the expire time in milliseconds.
     */
    public long getExpireTimeInMillis() {
        return getExpireTime(TimeUnit.MILLISECONDS);
    }
    
    /**
     * Returns {@code true} if the {@link NodeFetcher} is running.
     */
    public synchronized boolean isRunning() {
        return open && future != null && !future.isDone();
    }
    
    /**
     * Starts the {@link NodeFetcher}.
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
        future = SchedulingUtils.scheduleWithFixedDelay(
                task, delay, frequency, unit);
    }
    
    /**
     * Stops the {@link NodeFetcher}.
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
     * The main process loop.
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
            fireActiveNode(ep.getInetSocketAddress());
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
     * Sends a PING to the given {@link SocketAddress}.
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
                    new SingleMessageCallback(), null, m, (int)expireTimeInMillis);
            return true;
        }
        
        return false;
    }
    
    /**
     * Processes the given {@link Message}.
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
            fireActiveNode(ipp.getInetSocketAddress());
        }
    }
    
    /**
     * A callback class for the {@link UDPPinger}.
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
     * A callback class for the {@link UDPPinger}.
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
     * Adds a {@link NodeFetcherListener}.
     */
    public void addNodeFetcherListener(NodeFetcherListener listener) {
        listeners.add(listener);
    }
    
    /**
     * Fires an event with the given {@link SocketAddress}.
     */
    protected void fireActiveNode(final SocketAddress address) {
        Runnable event = new Runnable() {
            @Override
            public void run() {
                for (NodeFetcherListener listener : listeners) {
                    listener.handleActiveNode(address);
                }
            }
        };
        
        EventUtils.fireEvent(event);
    }
    
    /**
     * A callback interface for the {@link NodeFetcher}.
     */
    public static interface NodeFetcherListener {
        
        /**
         * Called for each {@link SocketAddress} that has been fetched from
         * the Gnutella Network.
         */
        public void handleActiveNode(SocketAddress address);
    }
}
