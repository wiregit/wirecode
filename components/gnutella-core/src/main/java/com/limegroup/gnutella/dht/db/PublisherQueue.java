package com.limegroup.gnutella.dht.db;

import java.io.Closeable;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.collection.IdentityHashSet;
import org.limewire.concurrent.ExecutorsHelper;
import org.limewire.concurrent.FutureEvent;
import org.limewire.concurrent.FutureEvent.Type;
import org.limewire.listener.EventListener;
import org.limewire.mojito2.DHT;
import org.limewire.mojito2.KUID;
import org.limewire.mojito2.concurrent.DHTFuture;
import org.limewire.mojito2.entity.StoreEntity;
import org.limewire.mojito2.storage.DHTValue;
import org.limewire.mojito2.util.MaxStack;

/**
 * 
 */
public class PublisherQueue implements Closeable {

    private static final Log LOG 
        = LogFactory.getLog(PublisherQueue.class);
    
    private static final int ALPHA = 4;
    
    private static final ExecutorService EXECUTOR 
        = Executors.newSingleThreadExecutor(
            ExecutorsHelper.defaultThreadFactory(
                "PublisherQueueThread"));
    
    private final Map<KUID, DHTValue> values 
        = new LinkedHashMap<KUID, DHTValue>();
    
    private final Set<DHTFuture<?>> futures 
        = new IdentityHashSet<DHTFuture<?>>();
    
    private final MaxStack stack;
    
    private final DHT dht;
    
    private final long timeout;
    
    private final TimeUnit unit;
    
    private boolean open = true;
    
    /**
     * 
     */
    public PublisherQueue(DHT dht, long timeout, TimeUnit unit) {
        this (dht, ALPHA, timeout, unit);
    }
    
    /**
     * 
     */
    public PublisherQueue(DHT dht, int alpha, long timeout, TimeUnit unit) {
        this.dht = dht;
        this.stack = new MaxStack(alpha);
        this.timeout = timeout;
        this.unit = unit;
    }
    
    @Override
    public synchronized void close() {
        open = false;
        values.clear();
        
        for (DHTFuture<?> future : futures) {
            future.cancel(true);
        }
        
        futures.clear();
    }
    
    /**
     * 
     */
    public synchronized boolean put(KUID key, DHTValue value) {
        if (!open) {
            return false;
        }
        
        values.put(key, value);
        doNext(0);
        
        return true;
    }
    
    /**
     * 
     */
    public synchronized void clear() {
        values.clear();
    }
    
    /**
     * 
     */
    public synchronized int size() {
        return values.size();
    }
    
    /**
     * 
     */
    public synchronized boolean isEmpty() {
        return values.isEmpty();
    }
    
    /**
     * 
     */
    private synchronized void doNext(final int count) {
        if (!open) {
            return;
        }
        
        Runnable task = new Runnable() {
            @Override
            public void run() {
                process(count);
            }
        };
        
        EXECUTOR.execute(task);
    }
    
    /**
     * 
     */
    private synchronized void process(int count) {
        if (!open) {
            return;
        }
        
        stack.pop(count);
        
        Iterator<Entry<KUID, DHTValue>> it 
            = values.entrySet().iterator();
        
        while (it.hasNext()) {
            if (!stack.hasFree()) {
                break;
            }
            
            Entry<KUID, DHTValue> entry = it.next();
            
            KUID key = entry.getKey();
            DHTValue value = entry.getValue();
            
            final DHTFuture<StoreEntity> future 
                = dht.put(key, value, timeout, unit);
            future.addFutureListener(new EventListener<FutureEvent<StoreEntity>>() {
                @Override
                public void handleEvent(FutureEvent<StoreEntity> event) {
                    synchronized (PublisherQueue.this) {
                        try {
                            futures.remove(future);
                            completed(event);
                        } finally {
                            doNext(1);
                        }
                    }
                }
            });
            
            futures.add(future);
            
            it.remove();
            stack.push();
        }
    }
    
    /**
     * 
     */
    protected void completed(FutureEvent<StoreEntity> event) {
        if (event.getType() == Type.EXCEPTION) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Failed To Publish", event.getException());
            }
        }
    }
}
