package org.limewire.mojito.db;

import java.io.Closeable;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.concurrent.ExecutorsHelper;
import org.limewire.concurrent.FutureEvent;
import org.limewire.concurrent.FutureEvent.Type;
import org.limewire.inspection.InspectablePrimitive;
import org.limewire.listener.EventListener;
import org.limewire.mojito.MojitoDHT2;
import org.limewire.mojito.concurrent.ManagedRunnable;
import org.limewire.mojito.entity.NodeEntity;
import org.limewire.mojito.routing.Contact;
import org.limewire.mojito.util.EventUtils;
import org.limewire.security.SecurityToken;

/**
 * 
 */
public class DatabasePublisher implements Closeable {

    private static final Log LOG 
        = LogFactory.getLog(DatabasePublisher.class);
    
    @InspectablePrimitive(value = "The number of files that have been published")
    private static final AtomicInteger PUBLISH_COUNT = new AtomicInteger();
    
    private static final ScheduledExecutorService EXECUTOR 
        = Executors.newSingleThreadScheduledExecutor(
            ExecutorsHelper.defaultThreadFactory("DatabasePublisherThread"));
    
    private final MojitoDHT2 dht;
    
    private final DatabasePublisherConfig config;
    
    private final ScheduledFuture<?> future;
    
    /**
     * 
     */
    private final AtomicBoolean barrier 
        = new AtomicBoolean(true);
    
    /**
     * 
     */
    private StoreTask storeTask = null;
    
    public DatabasePublisher(MojitoDHT2 dht, 
            DatabasePublisherConfig config,
            long frequency, TimeUnit unit) {
        
        this.dht = dht;
        this.config = config;
        
        Runnable task = new ManagedRunnable() {
            @Override
            protected void doRun() {
                process();
            }
        };
        
        future = EXECUTOR.scheduleWithFixedDelay(
                task, frequency, frequency, unit);
    }
    
    @Override
    public synchronized void close() {
        if (future != null) {
            future.cancel(true);
        }
        
        if (storeTask != null) {
            storeTask.close();
        }
    }
    
    private synchronized void process() {
        if (barrier.getAndSet(false)) {
            Runnable callback = new Runnable() {
                @Override
                public void run() {
                    barrier.set(true);
                }
            };
            
            storeTask = new StoreTask(dht, callback, timeout, unit);
        }
    }
    
    private static class StoreTask implements Closeable {
        
        private final EventListener<FutureEvent<NodeEntity>> listener 
                = new EventListener<FutureEvent<NodeEntity>>() {
            @Override
            public void handleEvent(FutureEvent<NodeEntity> event) {
                if (event.getType() == Type.SUCCESS) {
                    doStore(event.getResult());
                } else {
                    doNext();
                }
            }
        };
        
        private final EventListener<FutureEvent<NodeEntity>> listener2 
                = new EventListener<FutureEvent<NodeEntity>>() {
            @Override
            public void handleEvent(FutureEvent<NodeEntity> event) {
                doNext();
            }
        };

        protected final MojitoDHT2 dht;
        
        protected final Runnable callback;
        
        protected final long timeout;
        
        protected final TimeUnit unit;
        
        private final Iterator<Storable> storables;
        
        protected volatile boolean open = true;
        
        public StoreTask(MojitoDHT2 dht, Runnable callback, 
                long timeout, TimeUnit unit) {
            
            this.dht = dht;
            this.callback = callback;
            this.timeout = timeout;
            this.unit = unit;
            
            StorableModelManager modelManager 
                = dht.getStorableModelManager();
            
            storables = modelManager.getStorables().iterator();
        }
        
        @Override
        public void close() {
            open = false;
        }
        
        private void doStore(NodeEntity entity) {
            if (!open) {
                EventUtils.fireEvent(callback);
                return;
            }
            
            Entry<Contact, SecurityToken>[] contacts 
                = entity.getContacts();
            
            
        }

        private void doNext() {
            if (!open || !storables.hasNext()) {
                EventUtils.fireEvent(callback);
                return;
            }
            
            // DO STORE
        }
        
        
    }
}
