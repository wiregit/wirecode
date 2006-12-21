package com.limegroup.gnutella.util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/** A factory for ExecutorService, ThreadFactory and ScheduledExecutorService. */
public class ExecutorsHelper {
    
    /**
     * Creates a new 'ProcessingQueue'.
     * A ProcessingQueue is an ExecutorService that will
     * process all Runnables/Callables sequentially, creating
     * a thread for processing only when it needs it.
     * 
     * This kind of ExecutorService is ideal for long-lived tasks
     * that require processing rarely.
     * 
     * The threads will be created using a DefaultThreadFactory of
     * the given name.
     */
    public static ExecutorService newProcessingQueue(String name) {
        ThreadPoolExecutor tpe =  new ThreadPoolExecutor(1, 1,
                5L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>(),
                daemonThreadFactory(name));
        tpe.allowCoreThreadTimeOut(true);
        return Executors.unconfigurableExecutorService(tpe);
    }
    
    /**
     * Creates a new ThreadPool.
     * The pool is tuned to begin with 0 threads and maintain 0 threads,
     * although an unlimited number of threads will be created to handle
     * the tasks.  Each thread is set to linger for a short period of time,
     * ready to handle new tasks, before the thread terminates.
     */
    public static ExecutorService newThreadPool(String name) {
        return Executors.unconfigurableExecutorService(
                new ThreadPoolExecutor(0, Integer.MAX_VALUE,
                        5L, TimeUnit.SECONDS,
                        new SynchronousQueue<Runnable>(),
                        daemonThreadFactory(name)));
    }
    
    /**
     * Creates a new ThreadPool.
     * The pool is tuned to begin with 0 threads and maintain 0 threads,
     * although an unlimited number of threads will be created to handle
     * the tasks.  Each thread is set to linger for a short period of time,
     * ready to handle new tasks, before the thread terminates.
     */
    public static ExecutorService newThreadPool(ThreadFactory factory) {
        return Executors.unconfigurableExecutorService(
                new ThreadPoolExecutor(0, Integer.MAX_VALUE,
                        5L, TimeUnit.SECONDS,
                        new SynchronousQueue<Runnable>(),
                        factory));
    }
    
    /**
     * Creates a new ThreadPool with the maximum number of available threads.
     * Items added while no threads are available to process them will wait
     * until an executing item is finished and then be processed.
     */
    public static ExecutorService newFixedSizeThreadPool(int size, String name) {
        ThreadPoolExecutor tpe =  new ThreadPoolExecutor(1, size,
                5L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>(),
                daemonThreadFactory(name));
        tpe.allowCoreThreadTimeOut(true);
        return Executors.unconfigurableExecutorService(tpe);
    }
    
    
    /** Returns the default thread factory, using the given name. */
    public static ThreadFactory defaultThreadFactory(String name) {
        return new DefaultThreadFactory(name, false);
    }
    
    /** Returns the a thread factory of daemon threads, using the given name. */
    public static ThreadFactory daemonThreadFactory(String name) {
        return new DefaultThreadFactory(name, true);
    }

    /** A thread factory that can create threads with a name. */
    private static class DefaultThreadFactory implements ThreadFactory {
        /** The name created threads will use. */
        private final String name;
        /** Whether or not the created thread is a daemon thread. */
        private final boolean daemon;
        
        /** Constructs a thread factory that will created named threads. */
        public DefaultThreadFactory(String name, boolean daemon) {
            this.name = name;
            this.daemon = daemon;
        }
        
        public Thread newThread(Runnable r) {
            Thread t = new ManagedThread(r, name);
            if(daemon)
                t.setDaemon(true);
            return t;
        }
    }
    
}
