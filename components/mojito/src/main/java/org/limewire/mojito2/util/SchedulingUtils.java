package org.limewire.mojito2.util;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.limewire.mojito2.concurrent.DefaultThreadFactory;

/**
 * An utility class to create {@link ScheduledThreadPoolExecutor}s and
 * schedule {@link Runnable}s on an internal {@link ScheduledExecutorService}.
 */
public class SchedulingUtils {

    private static final long PURGE = 30L * 1000L;
    
    private static ScheduledExecutorService SCHEDULED_EXECUTOR 
        = newSingleThreadScheduledExecutor("ExecutorUtilsThread");
    
    private SchedulingUtils() {}
    
    /**
     * Creates and returns a {@link ScheduledThreadPoolExecutor}
     */
    public static ScheduledThreadPoolExecutor newSingleThreadScheduledExecutor(String name) {
        return newSingleThreadScheduledExecutor(new DefaultThreadFactory(name));
    }
    
    /**
     * Creates and returns a {@link ScheduledThreadPoolExecutor}
     */
    public static ScheduledThreadPoolExecutor newSingleThreadScheduledExecutor(
            ThreadFactory factory) {
        return newScheduledThreadPool(1, factory);
    }
    
    /**
     * Creates and returns a {@link ScheduledThreadPoolExecutor}
     */
    public static ScheduledThreadPoolExecutor newScheduledThreadPool(
            int corePoolSize, ThreadFactory factory) {
        
        final ScheduledThreadPoolExecutor executor 
            = (ScheduledThreadPoolExecutor)Executors
                .newScheduledThreadPool(corePoolSize, factory);
        //
        // NOTE: ScheduledThreadPoolExecutors may cause Memory-Leaks
        // if tasks are being scheduled with long delays/intervals
        // and these tasks are being canceled. They remain in the 
        // queue until their time for execution has arrived! For
        // more information see the following blog-post.
        //
        // http://www.kapsi.de/blog/2009/04/25/canceling-scheduledfutures-memory-leak
        //
        Runnable task = new Runnable() {
            @Override
            public void run() {
                executor.purge();
            }
        };
        
        executor.scheduleWithFixedDelay(task, 
                PURGE, PURGE, TimeUnit.MILLISECONDS);
        
        return executor;
    }
    
    /**
     * Schedules the given {@link Runnable}
     */
    public static ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, 
            long initialDelay, long delay, TimeUnit unit) {
        return SCHEDULED_EXECUTOR.scheduleWithFixedDelay(
                task, initialDelay, delay, unit);
    }
}
