package com.limegroup.gnutella.dht.db;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.limewire.mojito.KUID;
import org.limewire.mojito.MojitoDHT;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.dht.DHTEvent;
import com.limegroup.gnutella.dht.DHTEventListener;
import com.limegroup.gnutella.dht.DHTEvent.Type;
import com.limegroup.gnutella.dht.util.KUIDUtils;
import com.limegroup.gnutella.settings.DHTSettings;

/**
 * The PushProxiesPublisher publishes Push Proxy information for 
 * the localhost in the DHT.
 * <p>
 * It implements {@link DHTEventListener} and starts publishing push proxies
 * once the DHT is bootstrapped. It only publishes stable configurations that 
 * have been stable for a minute
 */
@Singleton
public class PushProxiesPublisher implements DHTEventListener {
    
    private volatile PushProxiesValue lastSeenValue;
    
    private volatile PushProxiesValue lastPublishedValue;
    
    private final PushProxiesValueFactory pushProxiesValueFactory;

    private final ScheduledExecutorService backgroundExecutor;

    private volatile ScheduledFuture publishingFuture;
    
    @Inject
    public PushProxiesPublisher(PushProxiesValueFactory pushProxiesValueFactory,
            @Named("backgroundExecutor") ScheduledExecutorService backgroundExecutor) {
        this.pushProxiesValueFactory = pushProxiesValueFactory;
        this.backgroundExecutor = backgroundExecutor;
    }

    private PushProxiesValue getCurrentPushproxiesValue() {
        PushProxiesValue pushProxiesValueForSelf = pushProxiesValueFactory.createDHTValueForSelf();
        return pushProxiesValueForSelf.getPushProxies().isEmpty() ? null : createCopy(pushProxiesValueForSelf);
    }
    
    private static final PushProxiesValue createCopy(PushProxiesValue original) {
        return new PushProxiesValueImpl(original.getVersion(), original.getGUID(),
                original.getFeatures(), original.getFwtVersion(), original.getPort(), original.getPushProxies());
    }

    /**
     * Publishes push proxies if they are stable and have changed from the
     * last time they were published.
     * @param mojitoDHT 
     */
    void publish(MojitoDHT mojitoDHT) {
        PushProxiesValue valueToPublish = getValueToPublish();
        if (valueToPublish != null) {
            GUID guid = new GUID(lastPublishedValue.getGUID());
            KUID primaryKey = KUIDUtils.toKUID(guid);
            mojitoDHT.put(primaryKey, lastPublishedValue);
        }
    }
    
    /**
     * Returns value to publish or null if there is nothing to publish.
     * <p>
     * Has the side effect of storing the updating the last published value.
     * </p>
     */
    PushProxiesValue getValueToPublish() {
        // order is important to compare newest last seen value with last published value
        if (pushProxiesAreStable() && publishedValueChanged()) {
            lastPublishedValue = lastSeenValue;
            return lastPublishedValue;
        }
        return null;
    }
    
    /**
     * Returns true if there is a valid last seen value and it differs from
     * the last published value.
     */
    boolean publishedValueChanged() {
        return lastSeenValue != null && !lastSeenValue.equals(lastPublishedValue);
    }
    
    /**
     * Returns true if the current value of push proxies is the same as the last 
     * time this method was called.
     * <p>
     * Has the side effect of storing the last seen value.
     * </p> 
     */
    boolean pushProxiesAreStable() {
        PushProxiesValue previousValue = lastSeenValue;
        lastSeenValue = getCurrentPushproxiesValue();
        if (lastSeenValue == null) {
            return false;
        }
        return lastSeenValue.equals(previousValue);
    }


    public String toString() {
        StringBuilder buffer = new StringBuilder("PushProxiesPublisher: ");
        buffer.append(lastPublishedValue);
        return buffer.toString();
    }
    
    public void handleDHTEvent(DHTEvent event) {
        if (event.getType() == Type.CONNECTED) {
            if (publishingFuture != null) {
                throw new IllegalStateException("should not have happened");
            }
            long interval = DHTSettings.PUSH_PROXY_STABLE_PUBLISHING_INTERVAL.getValue();
            long initialDelay = (long)(Math.random() * interval);
            // TODO instead of a polling approach, an event when push proxies have changed and should be updated might be nice
            publishingFuture = backgroundExecutor.scheduleAtFixedRate(new PublishingRunnable(event.getDHTController().getMojitoDHT()), initialDelay, interval, TimeUnit.MILLISECONDS);
        } else if (event.getType() == Type.STOPPED) {
            if (publishingFuture != null) {
                publishingFuture.cancel(false);
                publishingFuture = null;
            }
        }
    }
    
    private class PublishingRunnable implements Runnable {
        
        private final MojitoDHT mojitoDHT;

        public PublishingRunnable(MojitoDHT mojitoDHT) {
            this.mojitoDHT = mojitoDHT;
        }

        public void run() {
            publish(mojitoDHT);
        }
    }
}
