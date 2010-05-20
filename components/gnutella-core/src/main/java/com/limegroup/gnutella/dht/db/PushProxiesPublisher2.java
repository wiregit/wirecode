package com.limegroup.gnutella.dht.db;

import java.io.Closeable;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.limewire.concurrent.ExecutorsHelper;
import org.limewire.core.settings.DHTSettings;
import org.limewire.io.Connectable;
import org.limewire.io.ConnectableImpl;
import org.limewire.io.GUID;
import org.limewire.io.IpPort;
import org.limewire.io.IpPortImpl;
import org.limewire.mojito2.KUID;
import org.limewire.net.address.StrictIpPortSet;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.gnutella.ApplicationServices;
import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.PushEndpoint;
import com.limegroup.gnutella.PushEndpointFactory;
import com.limegroup.gnutella.dht.util.KUIDUtils;

@Singleton
public class PushProxiesPublisher2 implements Closeable {

    private static final ScheduledExecutorService EXECUTOR 
        = Executors.newSingleThreadScheduledExecutor(
            ExecutorsHelper.defaultThreadFactory(
                "PushProxiesPublisherThread"));
    
    private final PublisherQueue queue;
    
    private final NetworkManager networkManager;
    
    private final ApplicationServices applicationServices;
    
    private final PushEndpointFactory pushEndpointFactory;
    
    private final long frequency;
    
    private final TimeUnit unit;
    
    private boolean open = true;
    
    private ScheduledFuture<?> future;
    
    private volatile PushProxiesValue2 existing = null;
    
    private volatile long timeStamp = 0L;
    
    @Inject
    public PushProxiesPublisher2(PublisherQueue queue, 
            NetworkManager networkManager,
            ApplicationServices applicationServices,
            PushEndpointFactory pushEndpointFactory) {
        this(queue, networkManager, applicationServices, pushEndpointFactory,
                DHTSettings.PROXY_PUBLISHER_FREQUENCY.getTimeInMillis(), 
                TimeUnit.MILLISECONDS);
    }
    
    public PushProxiesPublisher2(PublisherQueue queue, 
            NetworkManager networkManager,
            ApplicationServices applicationServices,
            PushEndpointFactory pushEndpointFactory,
            long frequency, TimeUnit unit) {
        
        this.queue = queue;
        this.networkManager = networkManager;
        this.applicationServices = applicationServices;
        this.pushEndpointFactory = pushEndpointFactory;
        
        this.frequency = frequency;
        this.unit = unit;
    }
    
    public synchronized void start() {
        if (!open) {
            throw new IllegalStateException();
        }
        
        if (future != null && !future.isDone()) {
            return;
        }
        
        Runnable task = new Runnable() {
            @Override
            public void run() {
                publish();
            }
        };
        
        future = EXECUTOR.scheduleWithFixedDelay(
                task, frequency, frequency, unit);
    }
    
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
    
    private void publish() {
        if (!DHTSettings.PUBLISH_PUSH_PROXIES.getValue()) {
            return;
        }
        
        PushEndpoint endpoint = pushEndpointFactory.createForSelf();
        
        byte[] guid = applicationServices.getMyGUID();
        byte features = endpoint.getFeatures();
        int fwtVersion = endpoint.getFWTVersion();
        int port = endpoint.getPort();
        Set<? extends IpPort> proxies = getPushProxies(endpoint);
        
        PushProxiesValue2 value = new PushProxiesValue2.Impl(
                PushProxiesValue2.VERSION, guid, 
                features, fwtVersion, port, proxies);
        
        long time = System.currentTimeMillis() - timeStamp;
        if (value.equals(existing) && time < DHTSettings.PUSH_PROXY_STABLE_PUBLISHING_INTERVAL.getTimeInMillis()) {
            return;
        }
        
        existing = value;
        timeStamp = System.currentTimeMillis();
        
        KUID key = KUIDUtils.toKUID(new GUID(guid));
        queue.put(key, value.serialize());
    }
    
    private Set<? extends IpPort> getPushProxies(PushEndpoint endpoint) {
        if (networkManager.acceptedIncomingConnection()
                && networkManager.isIpPortValid()) {
            return new StrictIpPortSet<Connectable>(new ConnectableImpl(
                    new IpPortImpl(networkManager.getAddress(), 
                            networkManager.getPort()), 
                            networkManager.isIncomingTLSEnabled()));
        }
        
        return endpoint.getProxies();
    }
}
