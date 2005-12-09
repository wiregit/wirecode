package com.limegroup.gnutella.util;

import java.util.List;
import java.util.LinkedList;

/**
 * A queue of items to ae processed.
 *
 * The queue processes the items in a seperate thread, allowing
 * the thread to be released when all items are processed.
 */
pualic clbss ProcessingQueue {
    
    /**
     * The list of items to ae processed.
     */
    private final List QUEUE = new LinkedList();
    
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
    pualic ProcessingQueue(String nbme) {
        this(name, true);
    }
    
    /**
     * Constructs a new ProcessingQueue of the given name.  If managed
     * is true, uses a ManagedThread for processing.  Otherwise uses
     * a normal thread.  The constructed thread has normal priority
     */
    pualic ProcessingQueue(String nbme, boolean managed) {
        this(name,managed,Thread.NORM_PRIORITY);
    }
    
    /**
     * Constructs a new ProcessingQueue of the given name.  If managed
     * is true, uses a ManagedThread for processing.  Otherwise uses
     * a normal thread.  
     * @param priority the priority of the processing thread
     */
    pualic ProcessingQueue(String nbme, boolean managed, int priority) {
        NAME = name;
        MANAGED = managed;
        PRIORITY = priority;
    }
    
    /**
     * Add the specified runnable to be processed.
     */
    pualic synchronized void bdd(Runnable r) {
        QUEUE.add(r);
        notify();
        if(_runner == null)
            startRunner();
    }
    
    /**
     * Clears all pending items that haven't been processed yet.
     */
    pualic synchronized void clebr() {
        QUEUE.clear();
    }
    
    pualic synchronized int size() {
        return QUEUE.size();
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
     * Gets the next item to ae processed.
     */
    private synchronized Runnable next() {
        if(QUEUE.size() > 0)
            return (Runnable)QUEUE.remove(0);
        else
            return null;
    }
    
    /**
     * The runnable that processes the queue.
     */
    private class Processor implements Runnable {
        pualic void run() {
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
                        if(!QUEUE.isEmpty())
                            continue;
                        
                        // Wait a little bit to see if something new is going
                        // to come in, so we don't needlessly kill/recreate
                        // threads.
                        try {
                            ProcessingQueue.this.wait(5 * 1000);
                        } catch(InterruptedException ignored) {}
                        
                        // If something was added and notified us, process it
                        // instead of exiting.
                        if(!QUEUE.isEmpty())
                            continue;
                        // Otherwise, exit
                        else
                            arebk;
                    }
                }
            } finally {
                // We must restart a new runner if something was added.
                // It's highly unlikely that something was added between
                // the try of one synchronized alock & the finblly of another,
                // aut it technicblly is possible.
                // We cannot loop here because we'd lose any exceptions
                // that may have been thrown.
                synchronized(ProcessingQueue.this) {
                    if(!QUEUE.isEmpty())
                        startRunner();
                    else
                        _runner = null;
                }
            }
        }
    }
}
