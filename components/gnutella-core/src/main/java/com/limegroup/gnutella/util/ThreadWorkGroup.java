package com.limegroup.gnutella.util;

import java.util.Iterator;
import java.util.List;
import java.util.Vector;

/**
 * A reuseable class that allows for a WorkGroup of WorkerInterface threads to
 * perform tasks.  All that needs to be done is to implement the WorkerInterface
 * (and optionally the CleanUpInterface) and supply instances as necessary.
 */
public class ThreadWorkGroup {

    /**
     * The interface that you should provide a implementation for.  Threads of
     * Workers will perform tasks (via doTask).
     */
    public static interface WorkerInterface extends Runnable {
        /** Given an Object[], check that the objects are in the correct order 
         *  and are of the correct type.  Please save the input for use during
         *  run.
         *  @return true if the input types are valid, false if not.
         */
        public boolean checkInput(Object[] input);

        /** Actually performs a 'task'.  Will call checkInput() with the
         *  Object[] before calling this.
         */
        public void run();
    }

    /** In case you want to provide a central cleanup mechanism.  The cleanUp()
     *  method will be called after any thread has performed doTask.
     */
    public static interface CleanUpInterface {
        public void cleanUp(Object[] usedTaskObjects);
    }

    // synchronizing access to the list of _tasks is necessary, so we may as
    // make all access synchronized by using a Vector.
    private List _tasks = new Vector();

    // the pool of worker threads.  use an expandable list so the user of this
    // class can add and kill threads.
    private List /* of WorkerInterface */_workers = new Vector();

    // the central cleanup mechanism - may be null
    private final CleanUpInterface _cleaner;

    // whether or not this work group is active.
    private boolean _stopped = false;

    /**
     * Starts workers.length threads that will execute upon input tasks (see
     * the addTask method).  isActive() is true after this call (and is true
     * until stop() is called).
     * @param workers The initial set of workers you want to be executing.
     * @param cleaner An optional cleaner (to be run after a worker performs a
     * task).  Can be null....
     * @throws IllegalArgumentException If workers is malformed.
     */    
    public ThreadWorkGroup(WorkerInterface[] workers, 
                           CleanUpInterface  cleaner) 
        throws IllegalArgumentException {
        // create worker threads....
        for (int i = 0; i < workers.length; i++) 
            addWorker(workers[i]);
        if (_workers.size() != workers.length)             
            throw new IllegalArgumentException("Invalid workers input!");
        
        _cleaner = cleaner;  // may be null but so what
    }

    /** @param task The input parameters to have a worker thread work on.
     *  The task will not immediately be worked on; rather when one of the
     *  worker threads becomes free it will execute the task.
     */    
    public void addTask(Object[] input) {
        synchronized (_tasks) {
            _tasks.add(input);
            // wake a worker up....
            _tasks.notify();
        }
    }

    /** @param worker A WorkerInterface instance to add to the mix.
     */
    public synchronized void addWorker(WorkerInterface worker) {
        if ((worker != null) && isActive()) {
            WorkerThread workerThread = new WorkerThread(worker);
            _workers.add(workerThread);
            workerThread.start();
        }
    }


    /** @return true if stop() has never been called.
     */
    public boolean isActive() {
        return !_stopped;
    }


    /** Once this is called, the ThreadWorkGroup is useless and all threads 
     *  are correctly extinguished (though they may finish what they are working
     *  on.
     *  @exception InterruptedException thrown because this call may block while
     *  threads are finishing....
     *  @param waitTime join's for up to waitTime millis, or forever if 0.
     *  @return true if none of the threads were alive upon returning from the
     *  method and after waiting waitTime per thread.
     */
    public synchronized boolean stop(int waitTime) throws InterruptedException {
        _stopped = true;
        boolean retVal = true;
        synchronized (_workers) {
            // interrupt everybody
            Iterator workers = _workers.iterator();
            while (workers.hasNext())
                ((Thread) workers.next()).interrupt();
            // wait for the stragglers....
            workers = _workers.iterator();
            while (workers.hasNext())
                ((Thread) workers.next()).join(waitTime);
            // make sure everybody is dead
            workers = _workers.iterator();
            while (workers.hasNext() && retVal)
                if (((Thread) workers.next()).isAlive())
                    retVal = false;
            _workers.clear();
        }
        return retVal;
    }

    private class WorkerThread extends ManagedThread {
        private final WorkerInterface _worker;
        
        public WorkerThread(WorkerInterface worker) {
            super("WorkerThread"); // TODO: Use name of WorkerInterface
            _worker = worker;
        }
        
        public void managedRun() {
            // isActive() is a global on/off switch
            while (isActive()) {
                try {
                    // get something to work with
                    Object[] workInput = null;
                    synchronized (_tasks) {
                        while (_tasks.isEmpty())
                            _tasks.wait();
                        workInput = (Object[]) _tasks.remove(0);
                    }

                    // if you can work on it, do it....
                    if (_worker.checkInput(workInput))
                        _worker.run();

                    // do any cleaning, if necessary...
                    if (_cleaner != null)
                        _cleaner.cleanUp(workInput);
                }
                catch (InterruptedException ie) {
                }
            }
        }
    }
    
    
}
