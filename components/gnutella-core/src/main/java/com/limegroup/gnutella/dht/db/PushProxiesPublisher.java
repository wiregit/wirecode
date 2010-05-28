package com.limegroup.gnutella.dht.db;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.limewire.core.settings.DHTSettings;
import org.limewire.inspection.InspectablePrimitive;
import org.limewire.io.GUID;
import org.limewire.io.IpPortSet;
import org.limewire.mojito.KUID;
import org.limewire.mojito.storage.DHTValue;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.gnutella.ApplicationServices;
import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.PushEndpointFactory;
import com.limegroup.gnutella.dht.DHTManager;
import com.limegroup.gnutella.dht.util.KUIDUtils;

/**
 * The {@link PushProxiesPublisher} publishes the localhost's
 * Push-Proxies to the DHT.
 */
@Singleton
public class PushProxiesPublisher extends Publisher {

    @InspectablePrimitive(value = "The number of values that have been published")
    private static final AtomicInteger PUBLISH_COUNT = new AtomicInteger();
    
    private static final int PROXY_THRESHOLD = 2;
    
    private final DHTManager manager;
    
    private final NetworkManager networkManager;
    
    private final ApplicationServices applicationServices;
    
    private final PushEndpointFactory pushEndpointFactory;
    
    private volatile long stableTime = -1L;
    
    private volatile long publishTime = -1L;
    
    private volatile PushProxiesValue pendingValue = null;
    
    private volatile PushProxiesValue publishedValue = null;
    
    private volatile long pendingTimeStamp = 0L;
    
    private volatile long publishedTimeStamp = 0L;
    
    /**
     * Creates a {@link PushProxiesPublisher}
     */
    @Inject
    public PushProxiesPublisher(DHTManager manager, 
            NetworkManager networkManager,
            ApplicationServices applicationServices,
            PushEndpointFactory pushEndpointFactory) {
        this(manager, networkManager, applicationServices, pushEndpointFactory,
                DHTSettings.PROXY_PUBLISHER_FREQUENCY.getTimeInMillis(), 
                TimeUnit.MILLISECONDS);
    }
    
    /**
     * Creates a {@link PushProxiesPublisher}
     */
    public PushProxiesPublisher(DHTManager manager, 
            NetworkManager networkManager,
            ApplicationServices applicationServices,
            PushEndpointFactory pushEndpointFactory,
            long frequency, TimeUnit unit) {
        super(frequency, unit);
        
        this.manager = manager;
        this.networkManager = networkManager;
        this.applicationServices = applicationServices;
        this.pushEndpointFactory = pushEndpointFactory;
    }
    
    /**
     * Sets the time a {@link PushProxiesValue} must stay stable (unchanged)
     * before it's being published to the DHT.
     */
    public void setStableProxiesTime(long time, TimeUnit unit) {
        stableTime = unit.toMillis(time);
    }
    
    /**
     * Returns the time a {@link PushProxiesValue} must stay stable (unchanged)
     * before it's being published to the DHT in the given {@link TimeUnit}.
     */
    public long getStableProxiesTime(TimeUnit unit) {
        long stableTime = this.stableTime;
        if (stableTime != -1L) {
            return unit.convert(stableTime, TimeUnit.MILLISECONDS);
        }
        
        return DHTSettings.STABLE_PROXIES_TIME.getTime(unit);
    }
    
    /**
     * Returns the time a {@link PushProxiesValue} must stay stable (unchanged)
     * before it's being published to the DHT in milliseconds.
     */
    public long getStableProxiesTimeInMillis() {
        return getStableProxiesTime(TimeUnit.MILLISECONDS);
    }
    
    /**
     * Sets the time a {@link PushProxiesValue} needs to be re-published.
     */
    public void setPublishProxiesTime(long time, TimeUnit unit) {
        publishTime = unit.toMillis(time);
    }
    
    /**
     * Returns the time a {@link PushProxiesValue} needs to be
     * re-published in the given {@link TimeUnit}.
     */
    public long getPublishProxiesTime(TimeUnit unit) {
        long publishTime = this.publishTime;
        if (publishTime != -1L) {
            return unit.convert(publishTime, TimeUnit.MILLISECONDS);
        }
        
        return DHTSettings.PUBLISH_PROXIES_TIME.getTime(unit);
    }
    
    /**
     * Returns the time a {@link PushProxiesValue} needs to be
     * re-published in milliseconds.
     */
    public long getPublishProxiesTimeInMillis() {
        return getPublishProxiesTime(TimeUnit.MILLISECONDS);
    }
    
    /**
     * Returns the currently pending {@link PushProxiesValue} or 
     * {@code null} if no value is pending for publication.
     */
    public PushProxiesValue getPendingValue() {
        return pendingValue;
    }
    
    /**
     * Returns the currently published {@link PushProxiesValue} or 
     * {@code null} if no value has been published.
     */
    public PushProxiesValue getPublishedValue() {
        return publishedValue;
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
        if (hasChangedTooMuch(value, pendingValue)) {
            pendingValue = value;
            pendingTimeStamp = System.currentTimeMillis();
        }
        
        // Check for how long the pending-value has been stable for.
        // Do not publish if it's less than the given threshold!
        long stableTime = System.currentTimeMillis() - pendingTimeStamp;
        if (stableTime < getStableProxiesTimeInMillis()) {
            return;
        }
        
        // Check the time since the last publishing and if it's less
        // than the threshold and the value hasn't changed ever since
        // then don't re-publish the value.
        long publishTime = System.currentTimeMillis() - publishedTimeStamp;
        if (publishTime < getPublishProxiesTimeInMillis() 
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
     * Returns {@code true} if the current value differs too much
     * from the pending value and needs to be replaced.
     */
    static boolean hasChangedTooMuch(PushProxiesValue value, 
            PushProxiesValue pendingValue) {
        
        // Publish if first call
        if (pendingValue == null) {
            return true;
        }
        
        // Publish if fwt capabilities have changed
        if (value.getFwtVersion() != pendingValue.getFwtVersion()) {
            return true;
        }
        
        IpPortSet old = new IpPortSet(pendingValue.getPushProxies());
        old.retainAll(value.getPushProxies());
        
        if (old.size() < PROXY_THRESHOLD) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Publishes the given {@link DHTValue} to the DHT
     */
    protected void publish(KUID key, DHTValue value) {
        manager.enqueue(key, value);
    }
}
