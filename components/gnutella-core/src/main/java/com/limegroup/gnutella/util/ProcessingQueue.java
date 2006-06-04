
// Commented for the Learning branch

package com.limegroup.gnutella.util;

import java.util.List;
import java.util.LinkedList;

/**
 * Use a ProcessingQueue to have a separate thread call run() on objects you give it.
 * 
 * Make a new ProcessingQueue p.
 * Call p.add(r), where r is an object that has a run() method.
 * The ProcessingQueue will make a thread that calls the run() method.
 * 
 * This lets you continue immediately, and have a separate thread perform some operations which may be many or slow.
 * 
 * A ProcessingQueue object makes a separate thread to call run() on the objects.
 * When it runs out of objects, it lets the thread exit.
 * If the program calls p.add(r) again, it makes a new thread with the same name as the one before.
 * 
 * When you create a thread in Java, you can give it a name.
 * These names are just helpful labels, they do not uniquely identify the thread.
 * You can make a bunch of threads that all have no name, or you can make several threads that all have the same name.
 * 
 * The ProcessingQueue constructor takes a String, and uses it to name the ProcessingQueue's thread.
 * If you make two ProcessingQueue objects with the same name, you've created two completely separate objects.
 * They will each create their own thread, and operate seprately from one another.
 */
public class ProcessingQueue {

    /**
     * The list of items this ProcessingQueue will have a thread process.
     * Call add(r) to pass an object r, which is Runnable and has a run() method.
     * This ProcessingQueue will make a thread which will call the run() method soon afterwards.
     */
    private final List QUEUE = new LinkedList();

    /** The name of the thread this ProcessingQueue is using. */
    private final String NAME;

    /** True to make a LimeWire managed thread, false to use a regular Java thread. */
    private final boolean MANAGED;

    /** The priority we'll run the thread. */
    private final int PRIORITY;

    /** The thread that does the processing. */
    private Thread _runner = null;

    /**
     * Make a new ProcessingQueue which will make a new managed thread with the given name.
     * 
     * @param name The name we'll give the thread we'll make to process the items
     */
    public ProcessingQueue(String name) {

        // Call the next constructor, giving it true to have it make a LimeWire ManagedThread instead of a regular Java thread
        this(name, true);
    }

    /**
     * Make a new ProcessingQueue which will make a new managed or regular thread with the given name.
     * 
     * @param name    The name we'll give the thread we'll make to process the items
     * @param managed True to make a LimeWire ManagedThread, false to just make a regular Java thread
     */
    public ProcessingQueue(String name, boolean managed) {

        // Call the next constructor, giving it the default thread priority
        this(name, managed, Thread.NORM_PRIORITY);
    }

    /**
     * Make a new ProcessingQueue which will make a new managed or regular thread with the given name and priority.
     * 
     * @param name     The name we'll give the thread we'll make to process the items
     * @param managed  True to make a LimeWire ManagedThread, false to just make a regular Java thread
     * @param priority The priority we'll run the thread at
     */
    public ProcessingQueue(String name, boolean managed, int priority) {

        // Save the given values in this ProcessingQueue object
        NAME     = name;
        MANAGED  = managed;
        PRIORITY = priority;
    }

    /**
     * Add an object with a run() method that this ProcessingQueue will have a separate thread run later.
     * 
     * @param r An object our thread can call run() on
     */
    public synchronized void add(Runnable r) {

        // Add the given object to our list of them, our thread will call its run() method later
        QUEUE.add(r);                       // Add the Runnable object to the queue
        notify();                           // Wake up our thread waiting in Processor.run()
        if (_runner == null) startRunner(); // If this ProcessingQueue object hasn't made its thread yet, do it
    }

    /**
     * Clears all the pending objects we've added but the thread hasn't had a chance to call run() on yet.
     */
    public synchronized void clear() {

        // Empty all the objects from the queue even though our thread hasn't called their run() methods yet
        QUEUE.clear();
    }

    /**
     * Find out how many objects this ProcessingQueue has to call run() on.
     * The ProcessingQueue calls run() on all the objects we give it right away, so this number should never be high.
     * 
     * @return The number of objects waiting in the queue for the thread to call run() on
     */
    public synchronized int size() {

        // Objects wait in queue for the thread to call their run() method
        return QUEUE.size();
    }

    /**
     * Make a new thread which calls Processor.run() and exits.
     */
    private synchronized void startRunner() {

        // Make a new LimeWire ManagedThread or Java Thread
        if (MANAGED) _runner = new ManagedThread(new Processor(), NAME); // Make a new Processor object, the thread will run Processor.run()
        else         _runner = new Thread(new Processor(), NAME);

        // Have it call Processor.run() once, and then exit
        _runner.setPriority(PRIORITY); // Set the thread's priority
        _runner.setDaemon(true);       // Mark our new thread as a daemon thread, which won't keep the program running even if it still is
        _runner.start();               // Have the thread call Processor.run(), calling it on the object we made above
    }

    /**
     * Get the next Runnable object the thread should call run() on.
     * 
     * @return The first Runnable object in the queue, or null if it's empty
     */
    private synchronized Runnable next() {

        // Remove and return a Runnable object that the program added to queue by calling add(r)
        if (QUEUE.size() > 0) return (Runnable)QUEUE.remove(0); // Remove the item at index 0, the start
        else return null; // We're out of Runnable objects
    }

    /**
     * The threads this ProcessingQueue object makes run the run() method in this nested class named Processor.
     * 
     * There are two different kinds of objects here that both have methods named run().
     * startRunner() makes a Processor object, and _runner.start() calls Processor.run().
     * add(r) adds an object to queue, and next.run() here is the line of code that calls run() on it.
     */
    private class Processor implements Runnable {

        /** When startRunner() above calls _runner.start(), the new thread it made calls this run() method, then exits. */
        public void run() {

            try {

                // Loop until we run out of objects in queue to call run() on
                while (true) {

                    // Get a Runnable object from queue that add(r) put there, and call run() on it
                    Runnable next = next(); // This removes the object from queue so we will only call run() on it once
                    if (next != null) next.run();

                    /*
                     * Ideally this would be in a finally clause -- but if it
                     * is then we can potentially ignore exceptions that were
                     * thrown.
                     */

                    // Only let one thread enter this block or any of the synchronized methods above
                    synchronized (ProcessingQueue.this) {

                        /*
                         * While we were waiting to enter this synchronized block, another thread may have added a Runnable item to the queue
                         */

                        // If something was added before we grabbed the lock, process those items immediately instead of waiting
                        if (!QUEUE.isEmpty()) continue; // If this happened, go back to the start of the while loop to run those items

                        /*
                         * Wait a little bit to see if something new is going
                         * to come in, so we don't needlessly kill/recreate
                         * threads.
                         */

                        try {

                            // Have this thread wait for 5 seconds, or until add() above calls notify()
                            ProcessingQueue.this.wait(5 * 1000); // Call wait on this ProcessingQueue object so the call to notify() in add() above will wake it up

                        } catch (InterruptedException ignored) {} // If something else calls interrupt(), just ignore it

                        // If the add() method added a Runnable object and woke us up, queue won't be empty
                        if (!QUEUE.isEmpty()) {

                            // Return to the top of the while loop to remove and run the new items
                            continue;

                        // We waited for 5 seconds without a new object to run
                        } else {

                            // Leave the while loop, this thread's job is done
                            break;
                        }
                    }
                }

            } finally {

                /*
                 * We must restart a new runner if something was added.
                 * It's highly unlikely that something was added between
                 * the try of one synchronized block & the finally of another,
                 * but it technically is possible.
                 * We cannot loop here because we'd lose any exceptions
                 * that may have been thrown.
                 */

                // Only let one thread enter this block or any of the synchronized methods above
                synchronized (ProcessingQueue.this) {

                    // If add(r) gave queue a new object to run, have startRunner() start a new thread
                    if (!QUEUE.isEmpty()) startRunner();
                    else                  _runner = null; // Otherwise, remove our reference to this thread
                }
            }

            // When this thread exits the run() method, it ends
        }
    }
}
