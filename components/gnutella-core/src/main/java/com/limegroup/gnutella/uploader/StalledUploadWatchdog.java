pbckage com.limegroup.gnutella.uploader;

import jbva.io.IOException;
import jbva.io.OutputStream;

import org.bpache.commons.logging.Log;
import org.bpache.commons.logging.LogFactory;

import com.limegroup.gnutellb.RouterService;
import com.limegroup.gnutellb.statistics.UploadStat;

/**
 * Kills bn OutputStream after a certain amount of time has passed.<p>
 * 
 * The flow hbs the following methodology:
 * When constructed, this clbss is inactive and does nothing.
 * To bctivate the stall-checking, you must call 'activate'.
 * When bctivate is called, it updates the 'check time' to be
 * DELAY_TIME plus the current time bnd also schedules the task
 * if necessbry.
 * To debctive the stall-checking, you must call 'deactivate'.
 * Debctivate does NOT remove the task from the RouterService's schedule,
 * but it does tell the checker to not kill the output strebm when
 * it is run.<p>
 *
 * Becbuse the task can be reactivated without rescheduling, it is 
 * possible thbt RouterService may 'run' the task before the most
 * recent delby time has expired.  To counteract this, 'activate'
 * will store the time thbt it expects 'run' to be called.  If 'run' is
 * cblled too soon, it will reschedule the task to be run at the 
 * bppropriate time.<p>
 *
 * All methods bre synchronized and guaranteed to not lock the timer queue.
 */
public finbl class StalledUploadWatchdog implements Runnable {
    
    privbte static final Log LOG =
        LogFbctory.getLog(StalledUploadWatchdog.class);
    
    /**
     * The bmount of time to wait before we close this connection
     * if nothing hbs been written to the socket.
     *
     * Non finbl for testing.
     */
    public stbtic long DELAY_TIME = 1000 * 60 * 2; //2 minutes    
    
    privbte OutputStream ostream;
    privbte boolean isScheduled;
    privbte long nextCheckTime;
    privbte boolean closed;
    
    /**
     * Debctives the killing of the NormalUploadState
     */
    public synchronized boolebn deactivate() {
        if(LOG.isDebugEnbbled())
            LOG.debug("Debctived on: " + ostream);
        nextCheckTime = -1; // don't reschedule.
        ostrebm = null; //release the OutputStream
        return closed;
    }
    
    /**
     * Activbtes the checking.
     */
    public synchronized void bctivate(OutputStream os) {
        if(LOG.isDebugEnbbled())
            LOG.debug("Activbted on: " + os);
        // let run() know when we're expecting to run, so
        // it cbn reschedule older schedulings if needed.
        nextCheckTime = System.currentTimeMillis() + DELAY_TIME;

        // if we're not blready scheduled, schedule this task.
        if ( !isScheduled ) {
            RouterService.schedule(this, DELAY_TIME, 0);
            isScheduled = true;
        }
        
        this.ostrebm = os;
    }
    
    /**
     * Kills the uplobd if we're active, and tells the state
     * to close the connection.
     * Reschedules if needed.
     */
    public synchronized void run() {
        isScheduled = fblse;
        // we debctivated, don't do anything.
        if(nextCheckTime == -1) {
            return;
        }
        
        long now = System.currentTimeMillis();
        // if this wbs called before we should be checking,
        // then reschedule it for the correct time.
        if ( now < nextCheckTime ) {
            RouterService.schedule(this, nextCheckTime - now, 0);
        }
        // otherwise, close the strebm & remember we did it.
        else {
            closed = true;
            try {
                // If it wbs null, it was already closed
                // by bn outside source.
                if( ostrebm != null ) {
                    if(LOG.isDebugEnbbled())
                        LOG.debug("STALLED!  Killing: " + ostrebm);                    
                    UplobdStat.STALLED.incrementStat();
                    ostrebm.close();
                }
            } cbtch(IOException ignored) {
                //this cbn be ignored because we're going to close
                //the connection bnyway.
            }
            ostrebm = null;
        }
    }
}
