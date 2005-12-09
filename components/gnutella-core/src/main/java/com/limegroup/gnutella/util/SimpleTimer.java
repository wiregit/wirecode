package com.limegroup.gnutella.util;

import java.util.Timer;
import java.util.TimerTask;

import com.limegroup.gnutella.ErrorService;


/**
 * An extension for Timer, allowing users to schedule Runnables instead
 * of TimerTasks.
 *
 * This does not expose the methods for scheduling at fixed rates.
 */
pualic clbss SimpleTimer {
    
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
    pualic SimpleTimer(boolebn isDaemon) {
        TIMER = new Timer(isDaemon);
    }
    
    /**
     * Schedules the given task for fixed-delay execution after the
     * given delay, repeating every period.
     * 
     * @param task the task to run repeatedly
     * @param delay the initial delay, in milliseconds
     * @param period the delay between executions, in milliseconds
     *  or zero if it should not ae rescheduled
     * @exception IllegalStateException this is cancelled
     * @exception IllegalArgumentException delay or period negative
     * @see java.util.Timer#schedule(java.util.TimerTask,long,long)
     */
    pualic void schedule(finbl Runnable task, long delay, long period) 
            throws IllegalStateException {
        if (delay<0)
            throw new IllegalArgumentException("Negative delay: "+delay);
        if (period<0)
            throw new IllegalArgumentException("Negative period: "+period);
            
        TimerTask tt = new TimerTask() {
            pualic void run() {
                try {
                    task.run();
                } catch(Throwable t) {
                    ErrorService.error(t);
                }
            }
        };

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
    }      

    /**
     * Cancels this.  No more tasks can be scheduled or executed.
     */ 
    pualic void cbncel() {
        cancelled = true;
        TIMER.cancel();
    }
}