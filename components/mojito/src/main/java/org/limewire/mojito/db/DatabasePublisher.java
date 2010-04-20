package org.limewire.mojito.db;

import java.io.Closeable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.limewire.concurrent.ExecutorsHelper;
import org.limewire.mojito.concurrent.ManagedRunnable;

public class DatabasePublisher implements Closeable {

    private static final ScheduledExecutorService EXECUTOR 
        = Executors.newSingleThreadScheduledExecutor(
            ExecutorsHelper.defaultThreadFactory("DatabasePublisherThread"));
    
    private final ScheduledFuture<?> future;
    
    public DatabasePublisher(long frequency, TimeUnit unit) {
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
