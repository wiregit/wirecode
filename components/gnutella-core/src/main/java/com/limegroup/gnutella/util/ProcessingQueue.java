package com.limegroup.gnutella.util;

import java.util.LinkedList;
import java.util.List;

/**
 * A queue of items to be processed.
 *
 * The queue processes the items in a seperate thread, allowing
 * the thread to be released when all items are processed.
 *
 * Runnables are processed sequentially, one after the other.  
 */
public class ProcessingQueue implements ThreadPool {
    
    /**
     * The list of items to be processed.
     */
    private final List<Runnable> QUEUE = new LinkedList<Runnable>();
    
    /**
     * The name of the thread to be created.
     */
    private final String NAME;
    
    /**
     * Whether or not the constructed thread should be a managed thread or
     * not.
     */
    private final boolean MANAGED;

    /**
     * The priority at which to run this thread
     */
    private final int PRIORITY;
    
    /**
     * The thread doing the processing.
     */
    private Thread _runner = null;
    
    /**
     * Constructs a new ProcessingQueue using a ManagedThread.
     */
    public ProcessingQueue(String name) {
        this(name, true);
    }
    
    /**
     * Constructs a new ProcessingQueue of the given name.  If managed
     * is true, uses a ManagedThread for processing.  Otherwise uses
     * a normal thread.  The constructed thread has normal priority
     */
    public ProcessingQueue(String name, boolean managed) {
        this(name,managed,Thread.NORM_PRIORITY);
    }
    
    /**
     * Constructs a new ProcessingQueue of the given name.  If managed
     * is true, uses a ManagedThread for processing.  Otherwise uses
     * a normal thread.  
     * @param priority the priority of the processing thread
     */
    public ProcessingQueue(String name, boolean managed, int priority) {
        NAME = name;
        MANAGED = managed;
        PRIORITY = priority;
    }
    
    /**
     * Add the specified runnable to be processed.
     */
    public synchronized void add(Runnable r) {
        QUEUE.add(r);
        notifyAndStart();
    }
    
    protected synchronized void notifyAndStart() {
    	notify();
    	if(_runner == null)
    		startRunner();
    }
    
    /**
     * Clears all pending items that haven't been processed yet.
     */
    public synchronized void clear() {
        QUEUE.clear();
    }
    
    public synchronized int size() {
        return QUEUE.size();
    }
    
    /**
     * Adds the specified runnable to be processed.
     */
    public synchronized void invokeLater(Runnable r) {
        add(r);
    }
    
    /**
     * Starts a new runner.
     */
    private synchronized void startRunner() {
        if(MANAGED)
            _runner = new ManagedThread(new Processor(), NAME);
        else
            _runner = new Thread(new Processor(), NAME);

        _runner.setPriority(PRIORITY);
        _runner.setDaemon(true);
        _runner.start();
    }
    
    /**
     * Gets the next item to be processed.
     */
    protected synchronized Runnable next() {
        if(QUEUE.size() > 0)
            return QUEUE.remove(0);
        else
            return null;
    }
    
    protected synchronized boolean moreTasks() {
    	return !QUEUE.isEmpty();
    }
    
    /**
     * The runnable that processes the queue.
     */
    private class Processor implements Runnable {
        public void run() {
            try {
                while(true) {
                    Runnable next = next();
                    if(next != null)
                        next.run();

                    // Ideally this would be in a finally clause -- but if it
                    // is then we can potentially ignore exceptions that were
                    // thrown.
                    synchronized(ProcessingQueue.this) {
                        // If something was added before we grabbed the lock,
                        // process those items immediately instead of waiting
                        if(moreTasks())
                            continue;
                        
                        // Wait a little bit to see if something new is going
                        // to come in, so we don't needlessly kill/recreate
                        // threads.
                        try {
                            ProcessingQueue.this.wait(5 * 1000);
                        } catch(InterruptedException ignored) {}
                        
                        // If something was added and notified us, process it
                        // instead of exiting.
                        if(moreTasks())
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
                synchronized(ProcessingQueue.this) {
                    if(moreTasks())
                        startRunner();
                    else
                        _runner = null;
                }
            }
        }
    }
}
