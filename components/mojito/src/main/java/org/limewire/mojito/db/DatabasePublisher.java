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
    
    private final ScheduledFuture<?> future;
    
    public DatabasePublisher(MojitoDHT2 dht, 
            long frequency, TimeUnit unit) {
        
        this.dht = dht;
        
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
    public void close() {
        if (future != null) {
            future.cancel(true);
        }
    }
    
    private void process() {
        
    }
}
