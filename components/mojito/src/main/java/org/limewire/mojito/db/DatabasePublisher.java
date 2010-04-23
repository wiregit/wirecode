package org.limewire.mojito.db;

import java.io.Closeable;
import java.util.Iterator;
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
import org.limewire.inspection.InspectablePrimitive;
import org.limewire.listener.EventListener;
import org.limewire.mojito.MojitoDHT2;
import org.limewire.mojito.concurrent.DHTFuture;
import org.limewire.mojito.concurrent.ManagedRunnable;
import org.limewire.mojito.entity.StoreEntity;
import org.limewire.mojito.settings.StoreSettings;
import org.limewire.mojito.util.EventUtils;

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
    
    private final Config config;
    
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
    
    public DatabasePublisher(MojitoDHT2 dht, Config config,
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
            
            long timeout = config.getStoreTimeoutInMillis();
            storeTask = new StoreTask(dht, callback, 
                    timeout, TimeUnit.MILLISECONDS);
        }
    }
    
    /**
     * 
     */
    private static class StoreTask implements Closeable {
        
        private final EventListener<FutureEvent<StoreEntity>> listener 
                = new EventListener<FutureEvent<StoreEntity>>() {
            @Override
            public void handleEvent(FutureEvent<StoreEntity> event) {
                doNext();
            }
        };
        
        private final MojitoDHT2 dht;
        
        private final Runnable callback;
        
        private final long timeout;
        
        private final TimeUnit unit;
        
        private final Iterator<Storable> storables;
        
        private volatile boolean open = true;
        
        private volatile DHTFuture<StoreEntity> future = null;
        
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
            
            DHTFuture<StoreEntity> future = this.future;
            if (future != null) {
                future.cancel(true);
            }
        }
        
        private void doNext() {
            if (!open || !storables.hasNext()) {
                EventUtils.fireEvent(callback);
                return;
            }
            
            Storable storable = storables.next();
            
            future = dht.put(storable, timeout, unit);
            future.addFutureListener(listener);
        }
    }
    
    /**
     * 
     */
    public static class Config {
        
        private volatile long storeTimeout 
            = StoreSettings.STORE_TIMEOUT.getValue();
        
        public Config() {
            
        }
        
        /**
         * 
         */
        public long getStoreTimeout(TimeUnit unit) {
            return unit.convert(storeTimeout, TimeUnit.MILLISECONDS);
        }
        
        /**
         * 
         */
        public long getStoreTimeoutInMillis() {
            return getStoreTimeout(TimeUnit.MILLISECONDS);
        }
        
        /**
         * 
         */
        public void setStoreTimeout(long timeout, TimeUnit unit) {
            this.storeTimeout = unit.toMillis(timeout);
        }
    }
}
