package com.limegroup.gnutella.util;

import com.limegroup.gnutella.util.ManagedThread;

import com.sun.java.util.collections.List;
import com.sun.java.util.collections.Vector;

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
    Thread _runner = null;
    
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
        if(_runner == null) {
            startRunner();
        }
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
                while(QUEUE.size() > 0) {
                    Runnable next = (Runnable)QUEUE.remove(0);
                    next.run();
                }
            } finally {
                synchronized(ProcessingQueue.this) {
                    // If something was added before we grabbed the lock,
                    // restart the runner immediately.
                    if(!QUEUE.isEmpty())
                        startRunner();
                    // Otherwise, set the runner to be null so that the thread
                    // is restarted when a new item is added.
                    else
                        _runner = null;
                }
            }
        }
    }
}
