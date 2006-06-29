package com.limegroup.gnutella.util;

import java.util.LinkedList;
import java.util.List;

/**
 * A pool of Runnables that are processed asynchronously.
 */
public class DefaultThreadPool implements ThreadPool {
    
    /**
     * The list of items to be processed.
     */
    private final List<Runnable> QUEUE = new LinkedList<Runnable>();
    
    /**
     * The name of the threads to be created.
     */
    private final String NAME;
    
    /**
     * Whether or not the constructed thread should be a managed thread or not.
     */
    private final boolean MANAGED;
    
    /**
     * The maximum number of threads to allow running concurrently.
     */
    private final int MAX_THREADS;
    
    /**
     * The current number of ACTIVE threads.
     */
    private int _activeRunners;
    
    /**
     * Constructs a new DefaultThreadPool using ManagedThreads.
     * The pool will allow unlimited threads to be created for processing.
     */
    public DefaultThreadPool(String name) {
        this(name, true);
    }
    
    /**
     * Constructs a new DefaultThreadPool using ManagedThreads.
     * The pool will allow maxThreads threads to be created for processing.
     */
    public DefaultThreadPool(String name, int maxThreads) {
        this(name, true, maxThreads);
    }    
    
    /**
     * Constructs a new DefaultThreadPool which will creates Threads of the given name.
     * If managed is true, uses a ManagedThread for processing.  Otherwise uses
     * a normal thread.  The pool will allow unlimited threads to be created for processing.
     */
    public DefaultThreadPool(String name, boolean managed) {
        this(name, managed, Integer.MAX_VALUE);
    }
    
    /**
     * Constructs a new DefaultThreadPool which will creates Threads of the given name.
     * If managed is true, uses a ManagedThread for processing.  Otherwise uses
     * a normal thread.  The pool will allow maxThreads threads to be created for processing.
     */
    public DefaultThreadPool(String name, boolean managed, int maxThreads) {
        NAME = name;
        MANAGED = managed;
        MAX_THREADS = maxThreads;
    }
    
    /**
     * Adds the specified runnable to be processed.
     */
    public void invokeLater(Runnable r) {
        synchronized(this) {
            QUEUE.add(r);
            notifyAll();
        }
        
        // release lock to allow inactive thread to acquire & loop.
        Thread.yield();
        
        synchronized(this) {
            startRunnerIfPossibleAndNecessary();
        }
    }
    
    /**
     * Starts a new runner if a new active thread is allowed to be created.
     */
    private synchronized void startRunnerIfPossibleAndNecessary() {
        if(!QUEUE.isEmpty() && _activeRunners < MAX_THREADS) {
            Thread runner;
            if(MANAGED)
                runner = new ManagedThread(new Processor(), NAME);
            else
                runner = new Thread(new Processor(), NAME);
    
            runner.setDaemon(true);
            
            _activeRunners++;
            runner.start();
        }
    }
    
    /**
     * Gets the next item to process.
     */
    private synchronized Runnable next() {
        if(!QUEUE.isEmpty())
            return QUEUE.remove(0);
        else
            return null;
    }
    
    /**
     * A runnable that processes the queue.
     */
    private class Processor implements Runnable {
        public void run() {
            try {
                Runnable next = next();
                while(true) {
                    if(next != null)
                        next.run();

                    // Ideally this would be in a finally clause -- but if it
                    // is then we can potentially ignore exceptions that were
                    // thrown.
                    synchronized(DefaultThreadPool.this) {
                        // If something was added before we grabbed the lock,
                        // process those items immediately instead of waiting
                        next = next();
                        if(next != null)
                            continue;
                        
                        // Wait a little bit to see if something new is going
                        // to come in, so we don't needlessly kill/recreate
                        // threads.
                        try {
                            DefaultThreadPool.this.wait(5 * 1000);
                        } catch(InterruptedException ignored) {}
                        
                        // If something was added and notified us, process it
                        // instead of exiting.
                        next = next();
                        if(next != null)
                            continue;
                        // Otherwise, exit
                        else
                            break;
                    }
                }
            } finally {
                // We must restart a new runner if something was added.
                // It's highly unlikely that something was added between
                // the try of one synchronized block & the finally of another,
                // but it technically is possible.
                // We cannot loop here because we'd lose any exceptions
                // that may have been thrown.
                synchronized(DefaultThreadPool.this) {
                    _activeRunners--;
                    startRunnerIfPossibleAndNecessary();
                }
            }
        }
    }
}
