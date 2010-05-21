package com.limegroup.gnutella.dht.db;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.limewire.core.settings.DHTSettings;
import org.limewire.io.Connectable;
import org.limewire.io.ConnectableImpl;
import org.limewire.io.GUID;
import org.limewire.io.IpPort;
import org.limewire.io.IpPortImpl;
import org.limewire.mojito2.KUID;
import org.limewire.mojito2.storage.DHTValue;
import org.limewire.net.address.StrictIpPortSet;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.gnutella.ApplicationServices;
import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.PushEndpoint;
import com.limegroup.gnutella.PushEndpointFactory;
import com.limegroup.gnutella.dht.util.KUIDUtils;

/**
 * The {@link PushProxiesPublisher2} publishes the localhost's
 * Push-Proxies to the DHT.
 */
@Singleton
public class PushProxiesPublisher2 extends Publisher {

    private final PublisherQueue queue;
    
    private final NetworkManager networkManager;
    
    private final ApplicationServices applicationServices;
    
    private final PushEndpointFactory pushEndpointFactory;
    
    private volatile PushProxiesValue2 pendingValue = null;
    
    private volatile PushProxiesValue2 publishedValue = null;
    
    private volatile long pendingTimeStamp = 0L;
    
    private volatile long publishedTimeStamp = 0L;
    
    /**
     * Creates a {@link PushProxiesPublisher2}
     */
    @Inject
    public PushProxiesPublisher2(PublisherQueue queue, 
            NetworkManager networkManager,
            ApplicationServices applicationServices,
            PushEndpointFactory pushEndpointFactory) {
        this(queue, networkManager, applicationServices, pushEndpointFactory,
                DHTSettings.PROXY_PUBLISHER_FREQUENCY.getTimeInMillis(), 
                TimeUnit.MILLISECONDS);
    }
    
    /**
     * Creates a {@link PushProxiesPublisher2}
     */
    public PushProxiesPublisher2(PublisherQueue queue, 
            NetworkManager networkManager,
            ApplicationServices applicationServices,
            PushEndpointFactory pushEndpointFactory,
            long frequency, TimeUnit unit) {
        super(frequency, unit);
        
        this.queue = queue;
        this.networkManager = networkManager;
        this.applicationServices = applicationServices;
        this.pushEndpointFactory = pushEndpointFactory;
    }
    
    /**
     * Tries to publish the localhost's Push-Proxies to the DHT
     */
    @Override
    protected void publish() {
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
        
        // Check if the current value is equal to the pending-value.
        // If *NOT* then replace the pending-value with the current
        // value and reset the time stamp. The idea is to publish 
        // only stable configurations to the DHT.
        if (!value.equals(pendingValue)) {
            pendingValue = value;
            pendingTimeStamp = System.currentTimeMillis();
        }
        
        // Check for how long the pending-value has been stable for.
        // Do not publish if it's less than the given threshold!
        long stableTime = System.currentTimeMillis() - pendingTimeStamp;
        if (stableTime < DHTSettings.STABLE_PROXIES_TIME.getTimeInMillis()) {
            return;
        }
        
        // Check the time since the last publishing and if it's less
        // than the threshold and the value hasn't changed ever since
        // then don't re-publish the value.
        long publishTime = System.currentTimeMillis() - publishedTimeStamp;
        if (publishTime < DHTSettings.PUBLISH_PROXIES_TIME.getTimeInMillis() 
                && pendingValue.equals(publishedValue)) {
            return;
        }
        
        KUID key = KUIDUtils.toKUID(new GUID(guid));
        publishedValue = pendingValue;
        
        publish(key, publishedValue.serialize());
        publishedTimeStamp = System.currentTimeMillis();
    }
    
    /**
     * Publishes the given {@link DHTValue} to the DHT
     */
    protected void publish(KUID key, DHTValue value) {
        queue.put(key, value);
    }
    
    /**
     * Extracts and returns localhost's Push-Proxies.
     */
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
