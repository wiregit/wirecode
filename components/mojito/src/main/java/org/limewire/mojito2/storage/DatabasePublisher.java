package org.limewire.mojito2.storage;

import java.io.Closeable;
import java.util.Collection;
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
import org.limewire.mojito2.DHT;
import org.limewire.mojito2.concurrent.DHTFuture;
import org.limewire.mojito2.concurrent.ManagedRunnable;
import org.limewire.mojito2.entity.StoreEntity;
import org.limewire.mojito2.settings.StoreSettings;
import org.limewire.mojito2.util.EventUtils;

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
    
    private final DHT dht;
    
    private final Config config;
    
    private final long frequency;
    
    private final TimeUnit unit;
    
    /**
     * 
     */
    private final AtomicBoolean active 
        = new AtomicBoolean(false);
    
    /**
     * 
     */
    private ScheduledFuture<?> future;
    
    /**
     * 
     */
    private StoreTask storeTask = null;
    
    /**
     * 
     */
    private boolean open = true;
    
    /**
     * 
     */
    public DatabasePublisher(DHT dht, long frequency, TimeUnit unit) {
        this(dht, new Config(), frequency, unit);
    }
    
    /**
     * 
     */
    public DatabasePublisher(DHT dht, Config config,
            long frequency, TimeUnit unit) {
        
        this.dht = dht;
        this.config = config;
        this.frequency = frequency;
        this.unit = unit;
    }
    
    public synchronized void start() {
        if (!open) {
            throw new IllegalStateException();
        }
        
        if (future != null && !future.isDone()) {
            return;
        }
        
        Runnable task = new ManagedRunnable() {
            @Override
            protected void doRun() {
                if (dht.isReady()) {
                    process();
                }
            }
        };
        
        active.set(false);
        future = EXECUTOR.scheduleWithFixedDelay(
                task, frequency, frequency, unit);
    }
    
    public synchronized void stop() {
        if (future != null) {
            future.cancel(true);
        }
        
        if (storeTask != null) {
            storeTask.close();
        }
    }
    
    @Override
    public synchronized void close() {
        open = false;
        stop();
    }
    
    private synchronized void process() {
        if (!active.getAndSet(true)) {
            Runnable callback = new Runnable() {
                @Override
                public void run() {
                    active.set(false);
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
        
        private final DHT dht;
        
        private final Runnable callback;
        
        private final long timeout;
        
        private final TimeUnit unit;
        
        private final Iterator<Storable> storables;
        
        private volatile boolean open = true;
        
        private volatile DHTFuture<StoreEntity> future = null;
        
        public StoreTask(DHT dht, Runnable callback, 
                long timeout, TimeUnit unit) {
            
            this.dht = dht;
            this.callback = callback;
            this.timeout = timeout;
            this.unit = unit;
            
            StorableModelManager modelManager 
                = dht.getStorableModelManager();
            
            Collection<Storable> elements 
                = modelManager.getStorables();
            
            storables = elements.iterator();
            
            doNext();
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
            
            final Storable storable = storables.next();
            
            future = dht.put(storable, timeout, unit);
            future.addFutureListener(listener);
            
            future.addFutureListener(
                    new EventListener<FutureEvent<StoreEntity>>() {
                @Override
                public void handleEvent(FutureEvent<StoreEntity> event) {
                    StoreEntity entity = event.getResult();
                    
                    if (entity != null) {
                        storable.handleStoreResult(entity);
                    }
                }
            });
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
