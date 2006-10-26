package com.limegroup.gnutella.util;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.limegroup.gnutella.ErrorService;


/**
 * An extension for Timer, allowing users to schedule Runnables instead
 * of TimerTasks.
 *
 * This does not expose the methods for scheduling at fixed rates.
 */
public class SimpleTimer implements SchedulingThreadPool{
    
    /** Timer to be shared. */
    private static final SimpleTimer sharedTimer = new SimpleTimer(true);
    
    /** Returns a timer that can be shared amongst everything. */
    public static SimpleTimer sharedTimer() {
        return sharedTimer;
    }
    
    /**
     * The underlying Timer of this SimpleTimer.
     */
    private final Timer TIMER;
    
    /**
     * Whether or not we actively cancelled the timer.
     */
    private volatile boolean cancelled = false;
    
    /**
     * Creates a new active SimpleTimer with a callback for internal errors.
     * @param isDaemon true if this' thread should be a daemon.
     */
    public SimpleTimer(boolean isDaemon) {
        TIMER = new Timer(isDaemon);
    }
    
    /**
     * Schedules the given task for fixed-delay execution after the
     * given delay, repeating every period.
     * 
     * @param task the task to run repeatedly
     * @param delay the initial delay, in milliseconds
     * @param period the delay between executions, in milliseconds
     *  or zero if it should not be rescheduled
     * @exception IllegalStateException this is cancelled
     * @exception IllegalArgumentException delay or period negative
     * @see java.util.Timer#schedule(java.util.TimerTask,long,long)
     */
    public TimerTask schedule(final Runnable task, long delay, long period) 
            throws IllegalStateException {
        if (delay<0)
            throw new IllegalArgumentException("Negative delay: "+delay);
        if (period<0)
            throw new IllegalArgumentException("Negative period: "+period);
            
        MyTimerTask tt = new MyTimerTask(new Runnable(){
            public void run() {
                try {
                    task.run();
                } catch(Throwable t) {
                    ErrorService.error(t);
                }
            }
        });

        try {
            if(period == 0)
                TIMER.schedule(tt, delay);
            else
                TIMER.schedule(tt, delay, period);
        } catch(IllegalStateException ise) {
            // swallow ISE's if the Timer cancelled itself.
            if(cancelled)
                throw ise;
        }
        
        return tt;
    }      

    public void invokeLater(Runnable r) {
    	schedule(r, 0, 0);
    }
    
    public Future invokeLater(Runnable r, long delay) {
    	MyTimerTask tt = (MyTimerTask)schedule(r, delay, 0);
    	return new TTFuture(tt);
    }
    
    /**
     * Cancels this.  No more tasks can be scheduled or executed.
     */ 
    public void cancel() {
        cancelled = true;
        TIMER.cancel();
    }
    
    private class MyTimerTask extends TimerTask {
    	volatile boolean done;
    	volatile Runnable r; 
    	MyTimerTask(Runnable r) {
    		this.r = r;
    	}
    	
    	public boolean cancel() {
    		boolean ret = super.cancel();
    		r = null;
    		return ret;
    	}
    	
    	public void run (){
    		Runnable toRun = r;
    		if (toRun == null)
    			return;
    		try {
    			toRun.run();
    		} finally  {
    			done = true;
    		}
    	}
    }
    
    private class TTFuture implements Future {
    	final MyTimerTask task;
    	volatile boolean cancelled;
    	TTFuture(MyTimerTask task) {
    		this.task = task;
    	}
		public boolean cancel(boolean mayInterruptIfRunning) {
			return task.cancel();
		}
		public Object get() throws InterruptedException, ExecutionException {
			return null;
		}
		public Object get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
			return null;
		}
		public boolean isCancelled() {
			return task.r == null;
		}
		public boolean isDone() {
			return task.done;
		}
    }
}