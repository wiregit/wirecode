package com.limegroup.gnutella.util;

import java.util.List;
import java.util.Vector;

/**
 * A queue of items to be processed.
 *
 * The queue processes the items in a seperate thread, allowing
 * the thread to be released when all items are processed.
 */
public class ProcessingQueue {
    
    /**
     * The list of items to be processed.
     */
    private final List QUEUE = new Vector();
    
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
     * a normal thread.
     */
    public ProcessingQueue(String name, boolean managed) {
        NAME = name;
        MANAGED = managed;
    }
    
    /**
     * Add the specified runnable to be processed.
     */
    public synchronized void add(Runnable r) {
        QUEUE.add(r);
        notify();
        if(_runner == null)
            startRunner();
    }
    
    /**
     * Starts a new runner.
     */
    private synchronized void startRunner() {
        if(MANAGED)
            _runner = new ManagedThread(new Processor(), NAME);
        else
            _runner = new Thread(new Processor(), NAME);

        _runner.setDaemon(true);
        _runner.start();
    }
    
    /**
     * The runnable that processes the queue.
     */
    private class Processor implements Runnable {
        public void run() {
            try {
                while(true) {
                    while(QUEUE.size() > 0) {
                        Runnable next = (Runnable)QUEUE.remove(0);
                        next.run();
                    }

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
                    if(!QUEUE.isEmpty())
                        startRunner();
                    else
                        _runner = null;
                }
            }
        }
    }
}
