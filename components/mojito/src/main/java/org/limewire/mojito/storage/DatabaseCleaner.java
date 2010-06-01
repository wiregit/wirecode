package org.limewire.mojito.storage;

import java.io.Closeable;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.inspection.InspectablePrimitive;
import org.limewire.mojito.concurrent.ManagedRunnable;
import org.limewire.mojito.routing.RouteTable;
import org.limewire.mojito.util.SchedulingUtils;

/**
 * The {@link DatabaseCleaner} is responsible for removing expired 
 * {@link Value}s from the {@link Database}.
 */
public class DatabaseCleaner implements Closeable {

    private static final Log LOG 
        = LogFactory.getLog(DatabaseCleaner.class);
    
    @InspectablePrimitive(value = "Expired Value Count")
    private static final AtomicInteger EXPIRED_COUNT = new AtomicInteger();
    
    private final EvictorManager evictorManager = new EvictorManager();
    
    private final RouteTable routeTable;
    
    private final Database database;
    
    private final long frequency;
    
    private final TimeUnit unit;
    
    private ScheduledFuture<?> future;
    
    private boolean open = true;
    
    /**
     * Creates a {@link DatabaseCleaner}.
     */
    public DatabaseCleaner(RouteTable routeTable, 
            Database database, long frequency, TimeUnit unit) {
        
        this.routeTable = routeTable;
        this.database = database;
        this.frequency = frequency;
        this.unit = unit;
    }
    
    /**
     * Returns the {@link EvictorManager}.
     */
    public EvictorManager getEvictorManager() {
        return evictorManager;
    }
    
    /**
     * Starts the {@link DatabaseCleaner}
     */
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
                process();
            }
        };
        
        future = SchedulingUtils.scheduleWithFixedDelay(
                task, frequency, frequency, unit);
    }
    
    /**
     * Stops the {@link DatabaseCleaner}
     */
    public synchronized void stop() {
        if (future != null) {
            future.cancel(true);
        }
    }

    @Override
    public synchronized void close() {
        open = false;
        stop();
    }
    
    private void process() {
        synchronized (database) {
            for (ValueTuple entity : database.values()) {
                if (evictorManager.isExpired(routeTable, entity)) {
                    if (LOG.isTraceEnabled()) {
                        LOG.trace(entity + " is expired!");
                    }
                    
                    database.remove(entity.getPrimaryKey(), 
                            entity.getSecondaryKey());
                    
                    EXPIRED_COUNT.incrementAndGet();
                }
            }
        }
    }
}
