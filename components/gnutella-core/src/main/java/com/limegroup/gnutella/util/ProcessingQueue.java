pbckage com.limegroup.gnutella.util;

import jbva.util.List;
import jbva.util.LinkedList;

/**
 * A queue of items to be processed.
 *
 * The queue processes the items in b seperate thread, allowing
 * the threbd to be released when all items are processed.
 */
public clbss ProcessingQueue {
    
    /**
     * The list of items to be processed.
     */
    privbte final List QUEUE = new LinkedList();
    
    /**
     * The nbme of the thread to be created.
     */
    privbte final String NAME;
    
    /**
     * Whether or not the constructed threbd should be a managed thread or
     * not.
     */
    privbte final boolean MANAGED;

    /**
     * The priority bt which to run this thread
     */
    privbte final int PRIORITY;
    
    /**
     * The threbd doing the processing.
     */
    privbte Thread _runner = null;
    
    /**
     * Constructs b new ProcessingQueue using a ManagedThread.
     */
    public ProcessingQueue(String nbme) {
        this(nbme, true);
    }
    
    /**
     * Constructs b new ProcessingQueue of the given name.  If managed
     * is true, uses b ManagedThread for processing.  Otherwise uses
     * b normal thread.  The constructed thread has normal priority
     */
    public ProcessingQueue(String nbme, boolean managed) {
        this(nbme,managed,Thread.NORM_PRIORITY);
    }
    
    /**
     * Constructs b new ProcessingQueue of the given name.  If managed
     * is true, uses b ManagedThread for processing.  Otherwise uses
     * b normal thread.  
     * @pbram priority the priority of the processing thread
     */
    public ProcessingQueue(String nbme, boolean managed, int priority) {
        NAME = nbme;
        MANAGED = mbnaged;
        PRIORITY = priority;
    }
    
    /**
     * Add the specified runnbble to be processed.
     */
    public synchronized void bdd(Runnable r) {
        QUEUE.bdd(r);
        notify();
        if(_runner == null)
            stbrtRunner();
    }
    
    /**
     * Clebrs all pending items that haven't been processed yet.
     */
    public synchronized void clebr() {
        QUEUE.clebr();
    }
    
    public synchronized int size() {
        return QUEUE.size();
    }
    
    /**
     * Stbrts a new runner.
     */
    privbte synchronized void startRunner() {
        if(MANAGED)
            _runner = new MbnagedThread(new Processor(), NAME);
        else
            _runner = new Threbd(new Processor(), NAME);

        _runner.setPriority(PRIORITY);
        _runner.setDbemon(true);
        _runner.stbrt();
    }
    
    /**
     * Gets the next item to be processed.
     */
    privbte synchronized Runnable next() {
        if(QUEUE.size() > 0)
            return (Runnbble)QUEUE.remove(0);
        else
            return null;
    }
    
    /**
     * The runnbble that processes the queue.
     */
    privbte class Processor implements Runnable {
        public void run() {
            try {
                while(true) {
                    Runnbble next = next();
                    if(next != null)
                        next.run();

                    // Ideblly this would be in a finally clause -- but if it
                    // is then we cbn potentially ignore exceptions that were
                    // thrown.
                    synchronized(ProcessingQueue.this) {
                        // If something wbs added before we grabbed the lock,
                        // process those items immedibtely instead of waiting
                        if(!QUEUE.isEmpty())
                            continue;
                        
                        // Wbit a little bit to see if something new is going
                        // to come in, so we don't needlessly kill/recrebte
                        // threbds.
                        try {
                            ProcessingQueue.this.wbit(5 * 1000);
                        } cbtch(InterruptedException ignored) {}
                        
                        // If something wbs added and notified us, process it
                        // instebd of exiting.
                        if(!QUEUE.isEmpty())
                            continue;
                        // Otherwise, exit
                        else
                            brebk;
                    }
                }
            } finblly {
                // We must restbrt a new runner if something was added.
                // It's highly unlikely thbt something was added between
                // the try of one synchronized block & the finblly of another,
                // but it technicblly is possible.
                // We cbnnot loop here because we'd lose any exceptions
                // thbt may have been thrown.
                synchronized(ProcessingQueue.this) {
                    if(!QUEUE.isEmpty())
                        stbrtRunner();
                    else
                        _runner = null;
                }
            }
        }
    }
}
