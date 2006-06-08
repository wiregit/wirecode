package com.limegroup.gnutella.uploader;

import java.io.OutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.io.Shutdownable;
import com.limegroup.gnutella.statistics.UploadStat;
import com.limegroup.gnutella.util.IOUtils;

/**
 * Kills an OutputStream after a certain amount of time has passed.<p>
 * 
 * The flow has the following methodology:
 * When constructed, this class is inactive and does nothing.
 * To activate the stall-checking, you must call 'activate'.
 * When activate is called, it updates the 'check time' to be
 * DELAY_TIME plus the current time and also schedules the task
 * if necessary.
 * To deactive the stall-checking, you must call 'deactivate'.
 * Deactivate does NOT remove the task from the RouterService's schedule,
 * but it does tell the checker to not kill the output stream when
 * it is run.<p>
 *
 * Because the task can be reactivated without rescheduling, it is 
 * possible that RouterService may 'run' the task before the most
 * recent delay time has expired.  To counteract this, 'activate'
 * will store the time that it expects 'run' to be called.  If 'run' is
 * called too soon, it will reschedule the task to be run at the 
 * appropriate time.<p>
 *
 * All methods are synchronized and guaranteed to not lock the timer queue.
 */
public final class StalledUploadWatchdog implements Runnable {
    
    private static final Log LOG =
        LogFactory.getLog(StalledUploadWatchdog.class);
    
    /**
     * The amount of time to wait before we close this connection
     * if nothing has been written to the socket.
     *
     * Non final for testing.
     */
    public static long DELAY_TIME = 1000 * 60 * 2; //2 minutes    
    
    private Shutdownable shutdownable;
    private boolean isScheduled;
    private long nextCheckTime;
    private boolean closed;
    
    private final long delayTime;
    
    private OStreamWrap osWrap;
    
    public StalledUploadWatchdog() {
    	this(DELAY_TIME);
    }
    
    public StalledUploadWatchdog(long delayTime) {
    	this.delayTime = delayTime;
    }
    
    /**
     * Deactives the killing of the NormalUploadState
     */
    public synchronized boolean deactivate() {
        if(LOG.isDebugEnabled())
            LOG.debug("Deactived on: " + shutdownable);
        nextCheckTime = -1; // don't reschedule.
        shutdownable = null; //release the resource
        return closed;
    }
    
    /**
     * Activates the checking.
     */
    public synchronized void activate(Shutdownable shutdownable) {
        if(LOG.isDebugEnabled())
            LOG.debug("Activated on: " + shutdownable);
        // let run() know when we're expecting to run, so
        // it can reschedule older schedulings if needed.
        nextCheckTime = System.currentTimeMillis() + delayTime;

        // if we're not already scheduled, schedule this task.
        if ( !isScheduled ) {
            RouterService.schedule(this, delayTime, 0);
            isScheduled = true;
        }
        
        this.shutdownable = shutdownable;
    }
    
    public synchronized void activate(OutputStream os) {
    	if (osWrap == null)
    		osWrap = new OStreamWrap();
    	osWrap.setOstream(os);
    	activate(osWrap);
    }
    
    /**
     * Kills the upload if we're active, and tells the state
     * to close the connection.
     * Reschedules if needed.
     */
    public synchronized void run() {
        isScheduled = false;
        // we deactivated, don't do anything.
        if(nextCheckTime == -1) {
            return;
        }
        
        long now = System.currentTimeMillis();
        // if this was called before we should be checking,
        // then reschedule it for the correct time.
        if ( now < nextCheckTime ) {
            RouterService.schedule(this, nextCheckTime - now, 0);
        }
        // otherwise, close the stream & remember we did it.
        else {
        	closed = true;
        	// If it was null, it was already closed
        	// by an outside source.
        	if( shutdownable != null ) {
        		if(LOG.isDebugEnabled())
        			LOG.debug("STALLED!  Killing: " + shutdownable);                    
        		UploadStat.STALLED.incrementStat();
        		shutdownable.shutdown();
        	}
        	shutdownable = null;
        }
    }
    
    private static class OStreamWrap implements Shutdownable {
    	private OutputStream ostream;
    	void setOstream(OutputStream ostream) {
    		this.ostream = ostream;
    	}
    	public void shutdown() {
    		IOUtils.close(ostream);
    		ostream = null; 
            //exceptions can be ignored because we're going to close
            //the connection anyway.
    	}
    }
}
