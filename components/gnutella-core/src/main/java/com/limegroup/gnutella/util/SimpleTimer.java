package com.limegroup.gnutella.util;

import com.limegroup.gnutella.Assert;
import com.limegroup.gnutella.ByteOrder;
import com.limegroup.gnutella.ActivityCallback;
import com.limegroup.gnutella.RouterService;
import com.sun.java.util.collections.Comparable;
import com.sun.java.util.collections.ArrayList;


/**
 * A simplified replacement for java.util.Timer.  Useful because java.util.Timer
 * is not available in Java 1.1.8 (which we must use for the Mac).<p>
 *
 * Like Timer, this uses a single thread to service all tasks.  Hence it assumes
 * that timer tasks do not block for too long, and it doesn't provide real-time
 * guarantees on scheduling.<p>
 *
 * Unlike Timer, it only support simple fixed-delay scheduling.  Also it has no
 * fancy provisions to clean up threads.  It also notifies ActivityCallback
 * of any uncaught exception while servicing tasks.<p> 
 */
public class SimpleTimer {
    /**
     * The queue of tasks to execute, sorted by execution time. 
     * LOCKING: obtain queue's monitor
     */
    private BinaryHeap /* if SimpleTimerTask */ _queue=
        new BinaryHeap(INITIAL_HEAP_SIZE, true);
    private static final int INITIAL_HEAP_SIZE=4;
    /** The Thread that excecutes tasks. */
    private TimerRunnerThread _runner;
    /** True if this is cancelled. */
    private volatile boolean _isCancelled=false;
    /** If non-null, used for internal errors. */
    private ActivityCallback _callback;

    /**
     * Creates a new active SimpleTimer.
     * @param isDaemon true if this' thread should be a daemon.
     */
    //public SimpleTimer(boolean isDaemon) {
	//  this(isDaemon);
    //}

    /**
     * Creates a new active SimpleTimer with a callback for internal errors.
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
     * @exception IllegalArgumentException delay or period negative
     * @see java.util.Timer#schedule(java.util.TimerTask,long,long)
     */
    public void schedule(Runnable task, long delay, long period) 
            throws IllegalStateException {
        if (_isCancelled)
            throw new IllegalStateException("Timer cancelled");
        if (delay<0)
            throw new IllegalArgumentException("Negative delay: "+delay);
        if (period<0)
            throw new IllegalArgumentException("Negative period: "+period);

        long now=System.currentTimeMillis();

		ActivityCallback callback = RouterService.getCallback();
        SimpleTimerTask ttask=new SimpleTimerTask(task,
                                                  period, now+delay, 
                                                  callback);
        synchronized(_queue) {
            Object discarded=_queue.insert(ttask);
            Assert.that(discarded==null, "heap didn't resize");
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
            try {
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
            } catch(Throwable t) {
                RouterService.error(t);
            }
        }

        /**
         * Waits for and removes timer task appropriate for execution.  Returns
         * null if none is yet ready; call waitForTask again in this case.
         * @exception InterruptedException this was interrupted while waiting
         */
        private SimpleTimerTask waitForTask() throws InterruptedException {
            synchronized(_queue) {
                if (_queue.isEmpty()) {
                    //a) No tasks.
                    _queue.wait(); 
                    return null;
                } else {
                    SimpleTimerTask task=(SimpleTimerTask)_queue.getMax();
                    long time=task.timeUntilExecution();
                    if (time>0) {
                        //b) Task not ready to run.  After waiting, do NOT
                        //return this task; another task may have been added in
                        //the process, terminating this wait prematurely.
                        _queue.wait(time);
                        return null;
                    } else {
                        //c) Task ready to run.  Remove it.
                        SimpleTimerTask task2=(SimpleTimerTask)_queue.extractMax();
                        Assert.that(task2==task, "Queue modified without lock");
                        return task;
                    }
                }
            }
        } //end waitForTask
    } //end TimerRunnerThread        

    /** Unit tests. */
    /*
    public static void main(String args[]) {
        SimpleTimerTester tester=new SimpleTimerTester();
        tester.test();
    }
    */
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
    /** @see SimpleTimer._callback. */
    private ActivityCallback _callback;

    SimpleTimerTask(Runnable task, 
                    long period, long nextTime, 
                    ActivityCallback callback) {
        this._task=task;
        this._period=period;
        this._nextTime=nextTime;
        this._callback=callback;
    }

    /**
     * Returns the time in milliseconds to wait until this should be run.
     * Returns a non-positive value if this should be run immediately.
     */
    public long timeUntilExecution() {
        return _nextTime - System.currentTimeMillis();
    }

    /**
     * Calls _task.run(), then updates _nextTime.  Catches any exceptions while
     * running task.  
     */
    public void runAndReschedule() {
        try {
            _task.run();
        } catch (Exception e) {
            if (_callback!=null)
                _callback.error(e);
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
/*
class SimpleTimerTester {
    private long T=100;

    void test() {
        testFirstFirst();
        testSecondFirst();
        testManyTasks();
        testDaemon();
    }

    //Tests when the first item schedule goes first
    void testFirstFirst() {
        SimpleTimer t=new SimpleTimer(false);  //not daemon: test thread dies
        sleep(T);    //make timer thread block
        TimerTestTask a=new TimerTestTask("a");
        TimerTestTask b=new TimerTestTask("b");
        long start=System.currentTimeMillis();
        t.schedule(a, 2*T, 2*T);
        sleep(T);
        t.schedule(b, 2*T, 3*T);
        sleep(8*T+T/2);
        t.cancel();
        sleep(3*T);  //to check that cancel really worked

        a.checkMatch(start+2*T, 4, 2*T);
        b.checkMatch(start+3*T, 3, 3*T);

        try {
            t.schedule(new TimerTestTask("c"), 0, T);
            Assert.that(false);
        } catch (IllegalStateException pass) { }
    }

    //Tests when the second item scheduled goes first
    void testSecondFirst() {
        SimpleTimer t=new SimpleTimer(false);  //not daemon: test thread dies
        TimerTestTask b=new TimerTestTask("b2", true);
        TimerTestTask a=new TimerTestTask("a2", true);
        long start=System.currentTimeMillis();
        t.schedule(b, 3*T, 3*T);
        sleep(T);
        t.schedule(a, T, 2*T);
        sleep(8*T+T/2);
        t.cancel();
        sleep(3*T);  //to check that cancel really worked

        a.checkMatch(start+2*T, 4, 2*T);
        b.checkMatch(start+3*T, 3, 3*T);
    }

    //Test the priority queue with many tasks
    void testManyTasks() {
        SimpleTimer t=new SimpleTimer(true);
        TimerTestTask[] tasks=new TimerTestTask[12];
        long start=System.currentTimeMillis();
        for (int i=0; i<tasks.length; i++) {
            tasks[i]=new TimerTestTask("T"+i);
            t.schedule(tasks[i], 0, 4*T);
        }

        sleep(5*T);
        t.cancel();

        for (int i=0; i<tasks.length; i++) {
            tasks[i].checkMatch(start, 2, 4*T);
        }
    }

    void testDaemon() {
        SimpleTimer t=new SimpleTimer(true);
        System.out.println("Make sure test terminates without hanging now.");
    }

    void sleep(long msecs) {
        try { Thread.sleep(msecs); } catch (InterruptedException ignored) { }
    }
}

class TimerTestTask implements Runnable {
    //The system times this was executed, as Long.
    private ArrayList _runs=new ArrayList();
    // Amount of allowed variation, in msecs.
    private static long FUDGE_FACTOR=40;
    private String _name;
    private boolean _throwException;

    TimerTestTask(String name) {
        this(name, false);
    }

    TimerTestTask(String name, boolean throwException) {
        this._name=name;
        this._throwException=throwException;
    }

    public void run() {
        long now=System.currentTimeMillis();
        //System.out.println(_name+" : "+now);
        _runs.add(new Long(now));
        if (_throwException) 
            throw new IndexOutOfBoundsException();
    }
    
    //Checks that this' execution times approximately match.
    //Runs must be 2 or greater
    void checkMatch(long start, int runs, long period) {
        Assert.that(_runs.size()==runs,
                    "Got "+_runs.size()+" times instead of "+runs);
        Assert.that(equal(start, get(0)), 
                    "Start times not equal: "+start+" vs. "+get(0));
        for (int i=1; i<runs; i++) {
            long actualPeriod=get(i)-get(i-1);
            Assert.that(equal(actualPeriod, period),
                        "Bad spacing in runs: "+actualPeriod+" vs. "+period);
        }
    }
        
    //Checks that a and b are approximately equal within the given fudge factor
    boolean equal(long a, long b) {
        return (b-FUDGE_FACTOR)<a && a<(b+FUDGE_FACTOR);
    }
    
    long get(int i) {
        return ((Long)_runs.get(i)).longValue();
    }
}
*/
