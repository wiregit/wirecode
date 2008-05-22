package com.limegroup.gnutella.dht.db;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.io.IpPortSet;
import org.limewire.mojito.KUID;
import org.limewire.mojito.concurrent.DHTFuture;
import org.limewire.mojito.concurrent.DHTFutureListener;
import org.limewire.mojito.result.StoreResult;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.dht.DHTEvent;
import com.limegroup.gnutella.dht.DHTEventListener;
import com.limegroup.gnutella.dht.DHTManager;
import com.limegroup.gnutella.dht.DHTEvent.Type;
import com.limegroup.gnutella.dht.util.KUIDUtils;
import com.limegroup.gnutella.settings.DHTSettings;

/**
 * The PushProxiesPublisher publishes Push Proxy information for 
 * the localhost in the DHT.
 * <p>
 * It implements {@link DHTEventListener} and starts publishing push proxies
 * once the DHT is bootstrapped. It only publishes stable configurations that 
 * have been stable for a certain amount of time specified by 
 * {@link DHTSettings#PUSH_PROXY_STABLE_PUBLISHING_INTERVAL}.
 * <p>
 * Note: For this class to work it must be registered as a listener to
 * {@link DHTManager} before it fires events.
 */
@Singleton
public class PushProxiesPublisher implements DHTEventListener {
    
    private static final Log LOG = LogFactory.getLog(PushProxiesPublisher.class);
    
    private volatile PushProxiesValue lastSeenValue;
    
    private volatile PushProxiesValue lastPublishedValue;
    
    private final PushProxiesValueFactory pushProxiesValueFactory;

    private final ScheduledExecutorService backgroundExecutor;

    private volatile ScheduledFuture publishingFuture;

    private final DHTManager dhtManager;
    
    /**
     * @param dhtManager just needed to hold a lock on it when sending queries
     */
    @Inject
    public PushProxiesPublisher(PushProxiesValueFactory pushProxiesValueFactory,
            @Named("backgroundExecutor") ScheduledExecutorService backgroundExecutor,
            DHTManager dhtManager) {
        this.pushProxiesValueFactory = pushProxiesValueFactory;
        this.backgroundExecutor = backgroundExecutor;
        this.dhtManager = dhtManager;
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
     */
    void publish() {
        PushProxiesValue valueToPublish = getValueToPublish();
        if (valueToPublish != null) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("publishing: " + valueToPublish);
            }
            GUID guid = new GUID(lastPublishedValue.getGUID());
            KUID primaryKey = KUIDUtils.toKUID(guid);
            DHTFuture<StoreResult> future = dhtManager.put(primaryKey, lastPublishedValue);
            if (LOG.isDebugEnabled() && future != null) {
                future.addDHTFutureListener(new DHTFutureListener<StoreResult>() {
                    public void handleCancellationException(CancellationException e) {
                        LOG.debug("cancelled", e);
                    }
                    public void handleExecutionException(ExecutionException e) {
                        LOG.debug("execution", e);
                    }
                    public void handleFutureSuccess(StoreResult result) {
                        LOG.debug("success: " + result);
                    }
                    public void handleInterruptedException(InterruptedException e) {
                        LOG.debug("interrupted", e);
                    }
                });
            }
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
        if (pushProxiesAreStable() && valueToPublishChangedSignificantly()) {
            lastPublishedValue = lastSeenValue;
            return lastPublishedValue;
        }
        return null;
    }
    
    /**
     * Returns true if there is a valid last seen value and it differs from
     * the last published value significantly in the set of push proxies.
     */
    boolean valueToPublishChangedSignificantly() {
        // no data or values the same
        if (lastSeenValue == null || lastSeenValue.equals(lastPublishedValue)) {
            return false;
        }
        // never published before
        if (lastPublishedValue == null) {
            return true;
        }
        // publish if fwt capabilities have changed
        if (lastSeenValue.getFwtVersion() != lastPublishedValue.getFwtVersion()) {
            return true;
        }
        // value has changed, if only one or less proxies are still the same
        // we republish
        IpPortSet old = new IpPortSet(lastPublishedValue.getPushProxies());
        old.retainAll(lastSeenValue.getPushProxies());
        return old.size() < 2;
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


    @Override
    public String toString() {
        StringBuilder buffer = new StringBuilder("PushProxiesPublisher: ");
        buffer.append(lastPublishedValue);
        return buffer.toString();
    }
    
    public synchronized void handleDHTEvent(DHTEvent event) {
        if (event.getType() == Type.CONNECTED) {
            LOG.debug("starting push proxy publishing");
            if (publishingFuture != null) {
                throw new IllegalStateException("should not have happened");
            }
            long interval = DHTSettings.PUSH_PROXY_STABLE_PUBLISHING_INTERVAL.getValue();
            long initialDelay = (long)(Math.random() * interval);
            // TODO instead of a polling approach, an event when push proxies have changed and should be updated might be nice
            publishingFuture = backgroundExecutor.scheduleAtFixedRate(new PublishingRunnable(), initialDelay, interval, TimeUnit.MILLISECONDS);
        } else if (event.getType() == Type.STOPPED) {
            LOG.debug("stopping push proxy publishing");
            if (publishingFuture != null) {
                publishingFuture.cancel(false);
                publishingFuture = null;
            }
        }
    }
    
    private class PublishingRunnable implements Runnable {
        
        public void run() {
            publish();
        }
    }
}
