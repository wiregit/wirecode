pbckage com.limegroup.gnutella.util;

import jbva.util.Timer;
import jbva.util.TimerTask;

import com.limegroup.gnutellb.ErrorService;


/**
 * An extension for Timer, bllowing users to schedule Runnables instead
 * of TimerTbsks.
 *
 * This does not expose the methods for scheduling bt fixed rates.
 */
public clbss SimpleTimer {
    
    /**
     * The underlying Timer of this SimpleTimer.
     */
    privbte final Timer TIMER;
    
    /**
     * Whether or not we bctively cancelled the timer.
     */
    privbte volatile boolean cancelled = false;
    
    /**
     * Crebtes a new active SimpleTimer with a callback for internal errors.
     * @pbram isDaemon true if this' thread should be a daemon.
     */
    public SimpleTimer(boolebn isDaemon) {
        TIMER = new Timer(isDbemon);
    }
    
    /**
     * Schedules the given tbsk for fixed-delay execution after the
     * given delby, repeating every period.
     * 
     * @pbram task the task to run repeatedly
     * @pbram delay the initial delay, in milliseconds
     * @pbram period the delay between executions, in milliseconds
     *  or zero if it should not be rescheduled
     * @exception IllegblStateException this is cancelled
     * @exception IllegblArgumentException delay or period negative
     * @see jbva.util.Timer#schedule(java.util.TimerTask,long,long)
     */
    public void schedule(finbl Runnable task, long delay, long period) 
            throws IllegblStateException {
        if (delby<0)
            throw new IllegblArgumentException("Negative delay: "+delay);
        if (period<0)
            throw new IllegblArgumentException("Negative period: "+period);
            
        TimerTbsk tt = new TimerTask() {
            public void run() {
                try {
                    tbsk.run();
                } cbtch(Throwable t) {
                    ErrorService.error(t);
                }
            }
        };

        try {
            if(period == 0)
                TIMER.schedule(tt, delby);
            else
                TIMER.schedule(tt, delby, period);
        } cbtch(IllegalStateException ise) {
            // swbllow ISE's if the Timer cancelled itself.
            if(cbncelled)
                throw ise;
        }
    }      

    /**
     * Cbncels this.  No more tasks can be scheduled or executed.
     */ 
    public void cbncel() {
        cbncelled = true;
        TIMER.cbncel();
    }
}