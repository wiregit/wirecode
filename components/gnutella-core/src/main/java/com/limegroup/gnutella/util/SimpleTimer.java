package com.limegroup.gnutella.util;

import com.limegroup.gnutella.Assert;
import com.limegroup.gnutella.ByteOrder;
import com.sun.java.util.collections.Comparable;


/**
 * A simplified replacement for java.util.Timer.  Useful because java.util.Timer
 * is not available in Java 1.1.8 (which we must use for the Mac).<p>
 *
 * Like Timer, this uses a single thread to service all tasks.  Hence it assumes
 * that timer tasks do not block for too long, and it doesn't provide real-time
 * guarantees on scheduling.<p>
 *
 * Unlike Timer, it only support simple fixed-delay scheduling.  Also it has no
 * fancy provisions to clean up threads.<p> 
 */
public class SimpleTimer {
    /**
     * The queue of tasks to execute, sorted by execution time. 
     * LOCKING: obtain queue's monitor
     */
    private BinaryHeap /* SimpleTimerTask */ _queue=new BinaryHeap(HEAP_SIZE);
    private static final int HEAP_SIZE=10;  //TODO: make dynamic
    /** The Thread that excecutes tasks. */
    private TimerRunnerThread _runner;
    /** True if this is cancelled. */
    private volatile boolean _isCancelled=false;

    /**
     * Creates a new active SimpleTimer.
     * @param isDaemon true if this' thread should be a daemon.
     */
    public SimpleTimer(boolean isDaemon) {
        _runner=new TimerRunnerThread(isDaemon);
        _runner.start();
    }
    
    /**
     * Schedules the given task for fixed-delay execution after the
     * given delay, repeating every period.
     * 
     * @param task the task to run repeatedly
     * @param delay the initial delay, in milliseconds
     * @param period the delay between executions, in milliseconds
     * @exception IllegalStateException this is cancelled
     * @see java.util.Timer#schedule(java.util.TimerTask,long,long)
     */
    public void schedule(Runnable task, long delay, long period) 
            throws IllegalStateException {
        if (_isCancelled)
            throw new IllegalStateException("Timer cancelled");

        long now=System.currentTimeMillis();
        SimpleTimerTask ttask=new SimpleTimerTask(task, period, now+delay);
        synchronized(_queue) {
            Object discarded=_queue.insert(ttask);
            Assert.that(discarded==null, "TODO: resize heap");
            _queue.notify();
        }
    }      

    /**
     * Cancels this.  No more tasks can be scheduled or executed.
     */ 
    public void cancel() {
        _isCancelled=true;
        _runner.interrupt();
    }

    /**
     * Thread responsible for servicing all tasks.
     */
    private class TimerRunnerThread extends Thread {
        TimerRunnerThread(boolean isDaemon) {
            setDaemon(isDaemon);
        }

        /** Repeatedly runs tasks from queue as appropriate. */
        public void run() {
            while (! _isCancelled) {
                //1. Wait for runnable task.
                SimpleTimerTask ttask=null;
                try {
                    ttask=waitForTask();
                    if (ttask==null)
                        continue;
                } catch (InterruptedException e) {
                    Assert.that(_isCancelled, "Interrupted without cancel");
                    return;
                }
                
                //2. Run task WITHOUT HOLDING LOCK, then reschedule.
                ttask.runAndReschedule();
                synchronized(_queue) {
                    _queue.insert(ttask);
                }
            }
        }

        /**
         * Waits for a timer task appropriate for execution.  Returns null if
         * none is yet ready; call waitForTask again in this case.  
         * @exception InterruptedException this was interrupted while waiting
         */
        private SimpleTimerTask waitForTask() throws InterruptedException {
            synchronized(_queue) {
                if (_queue.isEmpty()) {
                    //a) No tasks.
                    _queue.wait(); 
                    return null;
                } else {
                    SimpleTimerTask task=(SimpleTimerTask)_queue.extractMax();
                    long time=task.timeUntilExecution();
                    if (time>0) {
                        //b) Task not ready to run.  After waiting, do NOT
                        //return this task; another task may have been added in
                        //the process, terminating this wait prematurely.
                        _queue.wait(time);
                        return null;
                    } else {
                        //c) Task ready to run.
                        return task;
                    }
                }
            }
        } //end waitForTask
    } //end TimerRunnerThread        

    /** Unit tests. */
    public static void main(String args[]) {
        SimpleTimerTester tester=new SimpleTimerTester();
        tester.test();
    }
} 



/**
 * A timed task to repeat.
 */
class SimpleTimerTask implements Comparable {
    /** The actual task to run. */
    private final Runnable _task;
    /** The milliseconds between tasks. */
    private final long _period;
    /** The system time this should next be executed, in milliseconds. */
    private long _nextTime;

    SimpleTimerTask(Runnable task, long period, long nextTime) {
        this._task=task;
        this._period=period;
        this._nextTime=nextTime;
    }

    /**
     * Returns the time in milliseconds to wait until this should be run.
     * Returns a non-positive value if this should be run immediately.
     */
    public long timeUntilExecution() {
        return System.currentTimeMillis() - _nextTime;
    }

    /**
     * Calls _task.run(), then updates _nextTime.  Catches any exceptions while
     * running task.  
     */
    public void runAndReschedule() {
        try {
            _task.run();
        } catch (Exception e) {
            //TODO: notify of internal error
        }
        _nextTime=System.currentTimeMillis()+_period;
    }

    /** 
     * Compares this' next execution time with another task's.
     *
     * @param x another SimpleTimerTask
     * @return a positive number if this._nextTime<x._nextTime (i.e., this
     *  should be executed before x), a negative number if
     *  x._nextTime<this._nextTime (i.e., this should be executed after x), or
     *  zero if this._nextTime==x._nextTime.
     * @exception ClassCastException x not a SimpleTimerTask 
     */
    public int compareTo(Object x) {
        long ret=((SimpleTimerTask)x)._nextTime - this._nextTime;
        return ByteOrder.long2int(ret);  //be careful downcasting
    }
}


/** Unit tests for SimpleTimer. */
class SimpleTimerTester {
    public void test() {

    }
}
