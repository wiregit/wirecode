package org.limewire.mojito.concurrent2;

import java.lang.ref.WeakReference;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.limewire.concurrent.ExecutorsHelper;

/**
 * 
 */
public class ExecutorUtils {

    private static final long DEFAULT_PURGE_FREQUENCY = 30L * 1000L;
    
    private ExecutorUtils() {}
    
    /**
     * 
     */
    public static ScheduledThreadPoolExecutor newSingleThreadScheduledExecutor(String name) {
        return newSingleThreadScheduledExecutor(name, 
                DEFAULT_PURGE_FREQUENCY, TimeUnit.MILLISECONDS);
    }
    
    /**
     * 
     */
    public static ScheduledThreadPoolExecutor newSingleThreadScheduledExecutor(
            String name, long frequency, TimeUnit unit) {
        return newScheduledThreadPool(name, 1, frequency, unit);
    }
    
    /**
     * 
     */
    public static ScheduledThreadPoolExecutor newScheduledThreadPool(
            String name, int corePoolSize) {
        return newScheduledThreadPool(name, corePoolSize, 
                DEFAULT_PURGE_FREQUENCY, TimeUnit.MILLISECONDS);
    }
    
    /**
     * 
     */
    public static ScheduledThreadPoolExecutor newScheduledThreadPool(
            String name, int corePoolSize, long frequency, TimeUnit unit) {
        
        ThreadFactory threadFactory 
            = ExecutorsHelper.defaultThreadFactory(name);
        final ScheduledThreadPoolExecutor executor 
            = new ScheduledThreadPoolExecutor(corePoolSize, threadFactory);
        
        if (frequency != -1L) {
            Runnable task = new ManagedRunnable() {
                @Override
                protected void doRun() {
                    executor.purge();
                }
            };
            
            executor.scheduleWithFixedDelay(task, frequency, frequency, unit);
        }
        
        return executor;
    }
    
    /**
     * 
     */
    public static ThreadPoolExecutor newSingleThreadExecutor(String name) {
        return newSingleThreadExecutor(ExecutorsHelper.defaultThreadFactory(name));
    }
    
    /**
     * 
     */
    public static ThreadPoolExecutor newSingleThreadExecutor(ThreadFactory threadFactory) {
        return newSingleThreadExecutor(threadFactory, DEFAULT_PURGE_FREQUENCY, TimeUnit.MILLISECONDS);
    }
    
    /**
     * 
     */
    public static ThreadPoolExecutor newSingleThreadExecutor(
            String name, long frequency, TimeUnit unit) {
        return newSingleThreadExecutor(
                ExecutorsHelper.defaultThreadFactory(name), frequency, unit);
    }
    
    /**
     * 
     */
    public static ThreadPoolExecutor newSingleThreadExecutor(
            ThreadFactory threadFactory, long frequency, TimeUnit unit) {
        return new ManagedThreadPoolExecutor(1, 1,
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>(),
                threadFactory,
                frequency, unit);
    }
    
    /**
     * 
     */
    private static class ManagedThreadPoolExecutor extends ThreadPoolExecutor {
        
        private static final ScheduledThreadPoolExecutor EXECUTOR 
            = ExecutorUtils.newSingleThreadScheduledExecutor("ExecutorPurgeThread");
        
        private final ScheduledFuture<?> purgeFuture;
        
        public ManagedThreadPoolExecutor(int corePoolSize, 
                int maximumPoolSize, 
                long keepAliveTime,
                TimeUnit unit, 
                BlockingQueue<Runnable> workQueue, 
                ThreadFactory threadFactory, 
                long purgeFrequency, TimeUnit purgeUnit) {
            super(corePoolSize, maximumPoolSize, keepAliveTime, unit, 
                    workQueue, threadFactory);
            
            purgeFuture = createPurgeFuture(this, purgeFrequency, purgeUnit);
        }

        private static ScheduledFuture<?> createPurgeFuture(
                ThreadPoolExecutor executor, long frequency, TimeUnit unit) {
            
            ScheduledFuture<?> future = null;
            
            if (frequency != -1L) {
                
                final WeakReference<ThreadPoolExecutor> executorRef 
                    = new WeakReference<ThreadPoolExecutor>(executor);
                
                Runnable task = new Runnable() {
                    @Override
                    public void run() {
                        ThreadPoolExecutor executor = executorRef.get();
                        if (executor == null) {
                            throw new IllegalStateException();
                        }
                        
                        try {
                            executor.purge();
                        } catch (Exception err) {
                            ExceptionUtils.exceptionCaught(err);
                        }
                    }
                };
                
                future = EXECUTOR.scheduleWithFixedDelay(
                        task, frequency, frequency, unit);
            }
            
            return future;
        }
        
        @Override
        protected void terminated() {
            if (purgeFuture != null) {
                purgeFuture.cancel(true);
            }
            
            super.terminated();
        }
    }
}
