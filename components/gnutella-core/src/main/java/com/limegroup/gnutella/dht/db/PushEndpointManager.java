package com.limegroup.gnutella.dht.db;

import java.io.Closeable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.concurrent.ExecutorsHelper;
import org.limewire.concurrent.FutureEvent;
import org.limewire.concurrent.FutureEvent.Type;
import org.limewire.core.settings.DHTSettings;
import org.limewire.io.GUID;
import org.limewire.io.IpPort;
import org.limewire.listener.EventListener;
import org.limewire.mojito.concurrent.DHTFuture;
import org.limewire.mojito.concurrent.DHTValueFuture;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.limegroup.gnutella.PushEndpoint;
import com.limegroup.gnutella.PushEndpointCache;

@Singleton
public class PushEndpointManager implements Closeable, PushEndpointService {

    private static final Log LOG 
        = LogFactory.getLog(PushEndpointManager.class);
    
    private static ScheduledExecutorService EXECUTOR 
        = Executors.newSingleThreadScheduledExecutor(
            ExecutorsHelper.defaultThreadFactory(
                "PushEndpointManagerThread"));
    
    private final EventListener<FutureEvent<PushEndpoint>> listener
            = new EventListener<FutureEvent<PushEndpoint>>() {
        @Override
        public void handleEvent(FutureEvent<PushEndpoint> event) {
            handlePushEndpoint(event);
        }
    };

    private final Map<GUID, FutureHandle> handles 
        = new HashMap<GUID, FutureHandle>();
    
    private final PushEndpointCache cache;
    
    private final PushEndpointService service;
    
    private volatile long frequency;
    
    private ScheduledFuture<?> purgeFuture = null;
    
    private boolean open = true;
    
    private volatile long cacheTime 
        = DHTSettings.PUSH_ENDPOINT_CACHE_TIME.getTimeInMillis();
    
    /**
     * Creates a {@link PushEndpointManager}
     */
    @Inject
    public PushEndpointManager(PushEndpointCache cache, 
            @Named("dhtPushEndpointFinder") PushEndpointService service) {
        this (cache, service, 
                DHTSettings.PUSH_ENDPOINT_PURGE_FREQUENCY.getTimeInMillis(), 
                TimeUnit.MILLISECONDS);
    }
    
    /**
     * Creates a {@link PushEndpointManager}
     */
    public PushEndpointManager(PushEndpointCache cache, 
            PushEndpointService service,
            long frequency, TimeUnit unit) {
        this.cache = cache;
        this.service = service;
        this.frequency = unit.toMillis(frequency);
    }
    
    @Override
    public synchronized void close() {
        open = false;
        
        if (purgeFuture != null) {
            purgeFuture.cancel(true);
        }
        
        handles.clear();
    }
    
    /**
     * Sets the purge frequency in the given {@link TimeUnit}.
     */
    public void setPurgeFrequency(long time, TimeUnit unit) {
        frequency = unit.toMillis(time);
    }
    
    /**
     * Returns the purge frequency in the given {@link TimeUnit}.
     */
    public long getPurgeFrequency(TimeUnit unit) {
        return unit.convert(frequency, TimeUnit.MILLISECONDS);
    }
    
    /**
     * Returns the purge frequency in milliseconds.
     */
    public long getPurgeFrequencyInMillis() {
        return getPurgeFrequency(TimeUnit.MILLISECONDS);
    }
    
    /**
     * Sets the current cache time in the given {@link TimeUnit}.
     */
    public void setCacheTime(long time, TimeUnit unit) {
        cacheTime = unit.toMillis(time);
    }
    
    /**
     * Returns the current cache time in the given {@link TimeUnit}.
     */
    public long getCacheTime(TimeUnit unit) {
        return unit.convert(cacheTime, TimeUnit.MILLISECONDS);
    }
    
    /**
     * Returns the current cache time in milliseconds
     */
    public long getCacheTimeInMillis() {
        return getCacheTime(TimeUnit.MILLISECONDS);
    }
    
    /**
     * Returns the number of cached {@link PushEndpoint}s
     */
    public synchronized int size() {
        return handles.size();
    }
    
    /**
     * Returns {@code true} if there are no cached {@link PushEndpoint}s.
     */
    public synchronized boolean isEmpty() {
        return handles.isEmpty();
    }
    
    @Override
    public DHTFuture<PushEndpoint> findPushEndpoint(GUID guid) {
        PushEndpoint endpoint = cache.getPushEndpoint(guid);
        if (endpoint != null && !endpoint.getProxies().isEmpty()) {
            return new DHTValueFuture<PushEndpoint>(endpoint);
        }
        
        if (!DHTSettings.ENABLE_PUSH_PROXY_QUERIES.getValue()) {
            return new DHTValueFuture<PushEndpoint>(
                    new IllegalStateException());
        }
        
        synchronized (this) {
            if (!open) {
                throw new IllegalStateException();
            }
            
            FutureHandle handle = handles.get(guid);
            if (handle != null) {
                return handle.future;
            }
            
            DHTFuture<PushEndpoint> future 
                = service.findPushEndpoint(guid);
            future.addFutureListener(listener);
            handles.put(guid, new FutureHandle(future));
            
            if (purgeFuture == null || purgeFuture.isDone()) {
                Runnable task = new Runnable() {
                    @Override
                    public void run() {
                        purge();
                    }
                };
                
                long frequency = this.frequency;
                purgeFuture = EXECUTOR.scheduleWithFixedDelay(
                        task, frequency, frequency, TimeUnit.MILLISECONDS);
            }
            
            return future;
        }
    }
    
    /**
     * A callback method that is being called for each {@link PushEndpoint}
     * {@link DHTFuture} that completes.
     */
    protected void handlePushEndpoint(FutureEvent<PushEndpoint> event) {
        if (event.getType() == Type.SUCCESS) {
            PushEndpoint endpoint = event.getResult();
            endpoint.updateProxies(true);
            
            IpPort externalAddress = endpoint.getValidExternalAddress();
            if (externalAddress != null) {
                cache.setAddr(endpoint.getClientGUID(), externalAddress);
            }
        }
    }
    
    /**
     * Removes all expired values from the cache
     */
    private synchronized void purge() {
        for (Iterator<FutureHandle> it 
                    = handles.values().iterator(); it.hasNext(); ) {
            
            FutureHandle handle = it.next();
            long time = System.currentTimeMillis() - handle.creationTime;
            
            if (time >= cacheTime) {
                handle.future.cancel(true);
                it.remove();
            }
        }
        
        boolean done = handles.isEmpty();
        
        if (done) {
            purgeFuture.cancel(true);
        }
        
        purged(done);
    }
    
    /**
     * Called by the purge {@link Thread} after each purge.
     */
    protected synchronized void purged(boolean done) {
    }
    
    /**
     * A handle that hold the creation time and the cached 
     * {@link PushEndpoint} {@link DHTFuture}.
     */
    private static class FutureHandle {
        
        private final long creationTime = System.currentTimeMillis();
        
        private final DHTFuture<PushEndpoint> future;
        
        public FutureHandle(DHTFuture<PushEndpoint> future) {
            this.future = future;
        }
    }
}
