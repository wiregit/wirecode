package org.limewire.mojito.db;

import java.io.Closeable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.concurrent.ExecutorsHelper;
import org.limewire.inspection.InspectablePrimitive;
import org.limewire.mojito.MojitoDHT2;
import org.limewire.mojito.concurrent.ManagedRunnable;
import org.limewire.mojito.routing.RouteTable;

/**
 * 
 */
public class DatabaseCleaner2 implements Closeable {

    private static final Log LOG 
        = LogFactory.getLog(DatabaseCleaner2.class);
    
    @InspectablePrimitive(value = "Expired Value Count")
    private static final AtomicInteger EXPIRED_COUNT = new AtomicInteger();
    
    private static final ScheduledExecutorService EXECUTOR 
        = Executors.newSingleThreadScheduledExecutor(
            ExecutorsHelper.defaultThreadFactory("DatabaseCleanerThread"));
    
    private final EvictorManager evictorManager = new EvictorManager();
    
    private final RouteTable routeTable;
    
    private final Database database;
    
    private final ScheduledFuture<?> future;
    
    /**
     * 
     */
    public DatabaseCleaner2(MojitoDHT2 dht, long frequency, TimeUnit unit) {
        this(dht.getRouteTable(), dht.getDatabase(), frequency, unit);
    }
    
    /**
     * 
     */
    public DatabaseCleaner2(RouteTable routeTable, 
            Database database, long frequency, TimeUnit unit) {
        
        this.routeTable = routeTable;
        this.database = database;
        
        Runnable task = new ManagedRunnable() {
            @Override
            protected void doRun() {
                process();
            }
        };
        
        future = EXECUTOR.scheduleWithFixedDelay(
                task, frequency, frequency, unit);
    }
    
    public EvictorManager getEvictorManager() {
        return evictorManager;
    }
    
    @Override
    public void close() {
        if (future != null) {
            future.cancel(true);
        }
    }
    
    private void process() {
        synchronized (database) {
            for (DHTValueEntity entity : database.values()) {
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
