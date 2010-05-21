package com.limegroup.gnutella.dht.db;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.limewire.core.settings.DHTSettings;
import org.limewire.inspection.InspectablePrimitive;
import org.limewire.io.GUID;
import org.limewire.mojito2.KUID;
import org.limewire.mojito2.storage.DHTValue;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.gnutella.ApplicationServices;
import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.PushEndpointFactory;
import com.limegroup.gnutella.dht.util.KUIDUtils;

/**
 * The {@link PushProxiesPublisher} publishes the localhost's
 * Push-Proxies to the DHT.
 */
@Singleton
public class PushProxiesPublisher extends Publisher {

    @InspectablePrimitive(value = "The number of values that have been published")
    private static final AtomicInteger PUBLISH_COUNT = new AtomicInteger();
    
    private final PublisherQueue queue;
    
    private final NetworkManager networkManager;
    
    private final ApplicationServices applicationServices;
    
    private final PushEndpointFactory pushEndpointFactory;
    
    private volatile PushProxiesValue pendingValue = null;
    
    private volatile PushProxiesValue publishedValue = null;
    
    private volatile long pendingTimeStamp = 0L;
    
    private volatile long publishedTimeStamp = 0L;
    
    /**
     * Creates a {@link PushProxiesPublisher}
     */
    @Inject
    public PushProxiesPublisher(PublisherQueue queue, 
            NetworkManager networkManager,
            ApplicationServices applicationServices,
            PushEndpointFactory pushEndpointFactory) {
        this(queue, networkManager, applicationServices, pushEndpointFactory,
                DHTSettings.PROXY_PUBLISHER_FREQUENCY.getTimeInMillis(), 
                TimeUnit.MILLISECONDS);
    }
    
    /**
     * Creates a {@link PushProxiesPublisher}
     */
    public PushProxiesPublisher(PublisherQueue queue, 
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
        
        PushProxiesValue value = new DefaultPushProxiesValue(networkManager, 
                applicationServices, pushEndpointFactory);
        
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
        
        KUID key = KUIDUtils.toKUID(new GUID(value.getGUID()));
        publishedValue = pendingValue;
        
        publish(key, publishedValue.serialize());
        publishedTimeStamp = System.currentTimeMillis();
        PUBLISH_COUNT.incrementAndGet();
    }
    
    /**
     * Publishes the given {@link DHTValue} to the DHT
     */
    protected void publish(KUID key, DHTValue value) {
        queue.put(key, value);
    }
}
