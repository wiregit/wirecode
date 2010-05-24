package com.limegroup.gnutella.dht.db;

import java.io.Closeable;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.collection.IdentityHashSet;
import org.limewire.concurrent.FutureEvent;
import org.limewire.concurrent.FutureEvent.Type;
import org.limewire.listener.EventListener;
import org.limewire.mojito2.KUID;
import org.limewire.mojito2.concurrent.DHTFuture;
import org.limewire.mojito2.entity.StoreEntity;
import org.limewire.mojito2.storage.DHTValue;
import org.limewire.mojito2.util.MaxStack;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.gnutella.dht2.DHTManager;

/**
 * 
 */
@Singleton
public class PublisherQueue implements Closeable {

    private static final Log LOG 
        = LogFactory.getLog(PublisherQueue.class);
    
    private static final int ALPHA = 4;
    
    private final Map<KUID, DHTValue> values 
        = new LinkedHashMap<KUID, DHTValue>();
    
    private final Set<DHTFuture<?>> futures 
        = new IdentityHashSet<DHTFuture<?>>();
    
    private final MaxStack stack;
    
    private final DHTManager manager;
    
    private boolean open = true;
    
    /**
     * 
     */
    @Inject
    public PublisherQueue(DHTManager manager) {
        this (manager, ALPHA);
    }
    
    /**
     * 
     */
    public PublisherQueue(DHTManager manager, int alpha) {
        this.manager = manager;
        this.stack = new MaxStack(alpha);
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
        
        stack.pop(count);
        
        Iterator<Entry<KUID, DHTValue>> it 
            = values.entrySet().iterator();
        
        while (it.hasNext()) {
            if (!stack.hasFree()) {
                break;
            }
            
            Entry<KUID, DHTValue> entry = it.next();
            it.remove();
            
            KUID key = entry.getKey();
            DHTValue value = entry.getValue();
            
            final DHTFuture<StoreEntity> future = manager.put(key, value);
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
