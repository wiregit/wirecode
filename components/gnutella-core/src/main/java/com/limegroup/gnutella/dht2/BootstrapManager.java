package com.limegroup.gnutella.dht2;

import static org.limewire.mojito2.util.ExceptionUtils.getCause;

import java.io.Closeable;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.collection.FixedSizeLIFOSet;
import org.limewire.collection.FixedSizeLIFOSet.EjectionPolicy;
import org.limewire.concurrent.FutureEvent;
import org.limewire.core.settings.DHTSettings;
import org.limewire.listener.EventListener;
import org.limewire.mojito2.MojitoDHT;
import org.limewire.mojito2.concurrent.DHTFuture;
import org.limewire.mojito2.entity.BootstrapEntity;
import org.limewire.mojito2.entity.CollisionException;
import org.limewire.mojito2.entity.PingEntity;
import org.limewire.mojito2.routing.Contact;
import org.limewire.mojito2.settings.NetworkSettings;
import org.limewire.mojito2.util.EventUtils;
import org.limewire.util.ExceptionUtils;

import com.google.inject.Provider;
import com.limegroup.gnutella.ConnectionServices;
import com.limegroup.gnutella.HostCatcher;
import com.limegroup.gnutella.UDPPinger;
import com.limegroup.gnutella.UniqueHostPinger;
import com.limegroup.gnutella.messages.PingRequestFactory;

/**
 * 
 */
class BootstrapManager implements Closeable, NodeFetcher.Callback {

    private static final Log LOG 
        = LogFactory.getLog(BootstrapManager.class);
    
    /**
     * A list of DHT bootstrap hosts coming from the Gnutella network. 
     * Limit size to 50 for now.
     */
    private final Set<SocketAddress> addresses 
        = new FixedSizeLIFOSet<SocketAddress>(50, EjectionPolicy.FIFO);
    
    private final CollisionCallback callback;
    
    private final MojitoDHT dht;
    
    private final NodeFetcher nodeFetcher;
    
    private boolean open = true;
    
    private DHTFuture<PingEntity> pingFuture = null;
    
    private DHTFuture<BootstrapEntity> bootFuture = null;
    
    public BootstrapManager(CollisionCallback callback,
            MojitoDHT dht, 
            ConnectionServices connectionServices,
            Provider<HostCatcher> hostCatcher,
            PingRequestFactory pingRequestFactory,
            Provider<UniqueHostPinger> uniqueHostPinger,
            Provider<UDPPinger> udpPinger) {
        
        this.callback = callback;
        this.dht = dht;
        
        this.nodeFetcher = new NodeFetcher(this, 
                connectionServices, hostCatcher, 
                pingRequestFactory, uniqueHostPinger,
                udpPinger);
    }
    
    /**
     * 
     */
    public synchronized void start(Contact... contacts) {
        if (!open) {
            throw new IllegalStateException();
        }
        
        if (dht.isReady()) {
            return;
        }
        
        if (contacts == null || contacts.length == 0) {
            return;
        }
        
        stop();
        
        Contact src = dht.getLocalNode();
        long timeout = NetworkSettings.DEFAULT_TIMEOUT.getValue();
        
        pingFuture = dht.ping(src, contacts, timeout, TimeUnit.MILLISECONDS);
        pingFuture.addFutureListener(
                new EventListener<FutureEvent<PingEntity>>() {
            @Override
            public void handleEvent(FutureEvent<PingEntity> event) {
                onPong(event);
            }
        });
    }
    
    /**
     * 
     */
    public synchronized void stop() {
        if (nodeFetcher != null) {
            nodeFetcher.stop();
        }
        
        if (pingFuture != null) {
            pingFuture.cancel(true);
        }
        
        if (bootFuture != null) {
            bootFuture.cancel(true);
        }
    }
    
    /**
     * 
     */
    public synchronized void close() {
        open = false;
        
        nodeFetcher.close();
        stop();
    }
    
    /**
     * 
     */
    private synchronized void onPong(FutureEvent<PingEntity> event) {
        if (!open) {
            return;
        }
        
        try {
            switch (event.getType()) {
                case SUCCESS:
                    onPong(event.getResult());
                    break;
                case EXCEPTION:
                    onException(event.getException());
                    break;
                default:
                    stop();
                    break;
            }
        } catch (Throwable t) {
            ExceptionUtils.reportIfUnchecked(t);
            uncaughtException(t);
        }
    }
    
    /**
     * 
     */
    private synchronized void onPong(PingEntity entity) {
        // We got a PONG, stop the NodeFetcher!
        if (nodeFetcher != null) {
            nodeFetcher.stop();
        }
        
        Contact src = entity.getContact();
        
        bootFuture = dht.bootstrap(src);
        bootFuture.addFutureListener(
                new EventListener<FutureEvent<BootstrapEntity>>() {
            @Override
            public void handleEvent(FutureEvent<BootstrapEntity> event) {
                onBootstrap(event);
            }
        });
    }
    
    /**
     * 
     */
    private synchronized void onBootstrap(FutureEvent<BootstrapEntity> event) {
        if (!open) {
            return;
        }
        
        try {
            switch (event.getType()) {
                case SUCCESS:
                    onSuccess();
                    break;
                case EXCEPTION:
                    onException(event.getException());
                    break;
                default:
                    onComplete();
                    break;
            }
        } catch (Throwable t) {
            uncaughtException(t);
        }
    }
    
    /**
     * 
     */
    private void onSuccess() {
        onComplete();
    }
    
    /**
     * 
     */
    private void onComplete() {
        stop();
    }
    
    /**
     * 
     */
    private void uncaughtException(Throwable t) {
        ExceptionUtils.reportIfUnchecked(t);
        onException(t);
    }
    
    /**
     * 
     */
    private synchronized void onException(Throwable t) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Failed to bootstrap", t);
        }
        
        final CollisionException cause = getCause(t, CollisionException.class);
        
        if (cause != null) {
            Runnable event = new Runnable() {
                @Override
                public void run() {
                    callback.handleCollision(cause);
                }
            };
            
            EventUtils.fireEvent(event);
            return;
        }
        
        tryBootstrap();
    }
    
    /**
     * 
     */
    @Override
    public synchronized void addActiveNode(SocketAddress address) {
        if (!open) {
            return;
        }
        
        addresses.add(address);
        tryBootstrap();
    }
    
    public synchronized void addPassiveNode(SocketAddress address) {
        if (!open) {
            return;
        }
        
        nodeFetcher.ping(address);
    }
    
    /**
     * 
     */
    private synchronized void tryBootstrap() {
        
        if (dht.isBooting() || dht.isReady()) {
            return;
        }

        if (pingFuture != null && !pingFuture.isDone()) {
            return;
        }
        
        if (addresses.isEmpty()) {
            SocketAddress address = getSimppHost();
            if (address != null) {
                addresses.add(address);
            }
        }
        
        Iterator<SocketAddress> it = addresses.iterator();
        if (!it.hasNext()) {
            if (nodeFetcher != null) {
                nodeFetcher.start();
            }
            
            return;
        }
        
        SocketAddress address = it.next();
        it.remove();
        
        pingFuture = dht.ping(address);
        pingFuture.addFutureListener(
                new EventListener<FutureEvent<PingEntity>>() {
            @Override
            public void handleEvent(FutureEvent<PingEntity> event) {
                onPong(event);
            }
        });
    }
    
    private static SocketAddress getSimppHost() {
        String[] simppHosts = DHTSettings.DHT_BOOTSTRAP_HOSTS.get();
        List<SocketAddress> list = new ArrayList<SocketAddress>(simppHosts.length);

        for (String hostString : simppHosts) {
            int index = hostString.indexOf(":");
            if(index < 0 || index == hostString.length()-1) {
                if (LOG.isErrorEnabled()) {
                    LOG.error("invalid SIMPP host: " + hostString);
                }
                
                continue;
            }
            
            try {
                String host = hostString.substring(0, index);
                int port = Integer.parseInt(hostString.substring(index+1).trim());
                list.add(new InetSocketAddress(host, port));
            } catch(NumberFormatException nfe) {
                if (LOG.isErrorEnabled()) {
                    LOG.error("invalid host: " + hostString);
                }
            }
        }
        
        if (list.isEmpty()) {
            return null;
        }
        
        return list.get((int)(list.size() * Math.random()));
    }
    
    /**
     *
     */
    public static interface CollisionCallback {
        
        /**
         * 
         */
        public void handleCollision(CollisionException ex);
    }
}
