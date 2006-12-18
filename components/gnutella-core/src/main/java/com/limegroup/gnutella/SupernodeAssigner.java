package com.limegroup.gnutella;

import com.limegroup.gnutella.settings.ApplicationSettings;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.settings.DownloadSettings;
import com.limegroup.gnutella.settings.UltrapeerSettings;
import com.limegroup.gnutella.settings.UploadSettings;
import com.limegroup.gnutella.statistics.BandwidthStat;
import com.limegroup.gnutella.util.CommonUtils;
import com.limegroup.gnutella.util.NetworkUtils;
import com.limegroup.gnutella.util.ThreadExecutor;

/**
 * This class determines whether or not this node has all of the necessary
 * characteristics for it to become a ultrapeer if necessary.  The criteria
 * uses include the node's upload and download bandwidth, the operating
 * system, the node's firewalled status, the average uptime, the current 
 * uptime, etc. <p>
 *
 * One of this class's primary functions is to run the timer that continually
 * checks the amount of bandwidth passed through upstream and downstream 
 * HTTP file transfers.  It records the maximum of the sum of these streams
 * to determine the node's bandwidth.
 */
//2345678|012345678|012345678|012345678|012345678|012345678|012345678|012345678|
public final class SupernodeAssigner {

	/**
	 * Constant value for whether or not the operating system qualifies
	 * this node for Ultrapeer status.
	 */
	private static final boolean ULTRAPEER_OS = CommonUtils.isUltrapeerOS();
	
	/**
	 * Constant for the number of milliseconds between the timer's calls
	 * to its <tt>Runnable</tt>s.
	 */
	public static final int TIMER_DELAY = 1000;

	/**
	 * Constant for the number of seconds between the timer's calls
	 * to its <tt>Runnable</tt>s.
	 */
	private static final int TIMER_DELAY_IN_SECONDS = TIMER_DELAY/1000;

    /**
	 * A <tt>BandwidthTracker</tt> instance for keeping track of the 
	 * upload bandwidth used for file uploads.
	 */
	private static BandwidthTracker _uploadTracker;

    /**
	 * A <tt>BandwidthTracker</tt> instance for keeping track of the 
	 * download bandwidth used for file downloads.
	 */
	private static BandwidthTracker _downloadTracker;
    
    /**
     * A reference to the Connection Manager
     */
    private static ConnectionManager _manager;

	/**
	 * Variable for the current uptime of this node.
	 */
	private static long _currentUptime = 0;

	/**
	 * Variable for the maximum number of bytes per second transferred 
	 * downstream over the history of the application.
	 */
	private static int _maxUpstreamBytesPerSec =
        UploadSettings.MAX_UPLOAD_BYTES_PER_SEC.getValue();

	/**
	 * Variable for the maximum number of bytes per second transferred 
	 * upstream over the history of the application.
	 */
	private static int _maxDownstreamBytesPerSec = 
        DownloadSettings.MAX_DOWNLOAD_BYTES_PER_SEC.getValue();
    

	/**
	 * Variable for whether or not this node has such good values that it is too
	 * good to pass up for becoming an Ultrapeer.
	 */
	private static volatile boolean _isTooGoodToPassUp = false;

	/**
	 * Variable for the last time we attempted to become an Ultrapeer.
	 */
	private static long _lastAttempt = 0L;

	/**
	 * Number of times we've tried to become an Ultrapeer.
	 */
	private static int _ultrapeerTries = 0;

    /** 
	 * Creates a new <tt>UltrapeerAssigner</tt>. 
	 *
	 * @param uploadTracker the <tt>BandwidthTracker</tt> instance for 
	 *                      tracking bandwidth used for uploads
	 * @param downloadTracker the <tt>BandwidthTracker</tt> instance for
	 *                        tracking bandwidth used for downloads
     * @param manager Reference to the ConnectionManager for this node
	 */
    public SupernodeAssigner(final BandwidthTracker uploadTracker, 
							 final BandwidthTracker downloadTracker,
                             ConnectionManager manager) {
		_uploadTracker = uploadTracker;
		_downloadTracker = downloadTracker;  
        _manager = manager;
    }
    
	/**
	 * Schedules a timer event to continually updates the upload and download
	 * bandwidth used.  Non-blocking.
     * @param router provides the schedule(..) method for the timing
     */
    public void start() {
        Runnable task=new Runnable() {
            public void run() {
                try {
                    collectBandwidthData();
                } catch(Throwable t) {
                    ErrorService.error(t);
                }
            }
        };            
        RouterService.schedule(task, 0, TIMER_DELAY);
    }

	/**
	 * Sets EVER_ULTRAPEER_CAPABLE to true if this has the necessary
	 * requirements for becoming a ultrapeer if needed, based on 
	 * the node's bandwidth, operating system, firewalled status, 
	 * uptime, etc.  Does not modify the property if the capabilities
     * are not met.  If the user has disabled ultrapeer support, 
     * sets EVER_ULTRAPEER_CAPABLE to false.
	 */
	private static void setUltrapeerCapable() {
        if (UltrapeerSettings.DISABLE_ULTRAPEER_MODE.getValue()) {
			UltrapeerSettings.EVER_ULTRAPEER_CAPABLE.setValue(false);
            return;
        }

        // Ignore this check if we're already an Ultrapeer -- 
        // we already know we're Ultrapeer-capable in that
        // case
        if(RouterService.isSupernode())
            return;

        boolean isUltrapeerCapable = 
            //Is upstream OR downstream high enough?
            (_maxUpstreamBytesPerSec >= 
                    UltrapeerSettings.MIN_UPSTREAM_REQUIRED.getValue() ||
             _maxDownstreamBytesPerSec >= 
                    UltrapeerSettings.MIN_DOWNSTREAM_REQUIRED.getValue()) &&
            //AND I'm not a modem (in case estimate wrong)
            (ConnectionSettings.CONNECTION_SPEED.getValue() > SpeedConstants.MODEM_SPEED_INT) &&
            //AND is my average uptime OR current uptime high enough?
            (ApplicationSettings.AVERAGE_UPTIME.getValue() >= UltrapeerSettings.MIN_AVG_UPTIME.getValue() ||
             _currentUptime >= UltrapeerSettings.MIN_INITIAL_UPTIME.getValue()) &&
            //AND am I not firewalled?
			ConnectionSettings.EVER_ACCEPTED_INCOMING.getValue() &&
            //AND I have accepted incoming messages over UDP
            RouterService.isGUESSCapable() &&
            //AND am I a capable OS?
			ULTRAPEER_OS &&
			//AND I do not have a private address
			!NetworkUtils.isPrivate();

		long curTime = System.currentTimeMillis();

		// check if this node has such good values that we simply can't pass
		// it up as an Ultrapeer -- it will just get forced to be one
		_isTooGoodToPassUp = isUltrapeerCapable &&
            RouterService.acceptedIncomingConnection() &&
			(curTime - RouterService.getLastQueryTime() > 5*60*1000) &&
			(BandwidthStat.HTTP_UPSTREAM_BANDWIDTH.getAverage() < 1);

        // record new ultrapeer capable value.
        if(isUltrapeerCapable)
            UltrapeerSettings.EVER_ULTRAPEER_CAPABLE.setValue(true);

		if(_isTooGoodToPassUp && shouldTryToBecomeAnUltrapeer(curTime)) {
			_ultrapeerTries++;

			// try to become an Ultrapeer -- how persistent we are depends on
			// how many times we've tried, and so how long we've been
			// running for
			final int demotes = 4 * _ultrapeerTries;
			Runnable ultrapeerRunner = 
				new Runnable() {
					public void run() {
						RouterService.getConnectionManager().tryToBecomeAnUltrapeer(demotes);
					}
				};
            ThreadExecutor.startThread(ultrapeerRunner, "UltrapeerAttemptThread");
		}
	}
	
	/**
	 * Checks whether or not we should try again to become an Ultrapeer.
	 *
	 * @param curTime the current time in milliseconds
	 * @return <tt>true</tt> if we should try again to become an Ultrapeer,
	 *  otherwise <tt>false</tt>
	 */
	private static boolean shouldTryToBecomeAnUltrapeer(long curTime) {
		if(curTime - _lastAttempt < UltrapeerSettings.UP_RETRY_TIME.getValue()) {
			return false;
		}
		_lastAttempt = curTime;
		return true;
	}

	/**
	 * Accessor for whether or not this machine has settings that are too good
	 * to pass up for Ultrapeer election.
	 *
	 * @return <tt>true</tt> if this node has extremely good Ultrapeer settings,
	 *  otherwise <tt>false</tt>
	 */
	public static boolean isTooGoodToPassUp() {
		return _isTooGoodToPassUp;
	}

	/**
	 * Collects data on the bandwidth that has been used for file uploads
	 * and downloads.
	 */
	private static void collectBandwidthData() {
		_currentUptime += TIMER_DELAY_IN_SECONDS;
        _uploadTracker.measureBandwidth();
        _downloadTracker.measureBandwidth();
        _manager.measureBandwidth();
        float bandwidth = 0;
        try {
            bandwidth = _uploadTracker.getMeasuredBandwidth();
        }catch(InsufficientDataException ide) {
            bandwidth = 0;
        }
		int newUpstreamBytesPerSec = 
            (int)bandwidth
           +(int)_manager.getMeasuredUpstreamBandwidth();
        bandwidth = 0;
        try {
            bandwidth = _downloadTracker.getMeasuredBandwidth();
        } catch (InsufficientDataException ide) {
            bandwidth = 0;
        }
		int newDownstreamBytesPerSec = 
            (int)bandwidth
           +(int)_manager.getMeasuredDownstreamBandwidth();
		if(newUpstreamBytesPerSec > _maxUpstreamBytesPerSec) {
			_maxUpstreamBytesPerSec = newUpstreamBytesPerSec;
			UploadSettings.MAX_UPLOAD_BYTES_PER_SEC.setValue(_maxUpstreamBytesPerSec);
		}
  		if(newDownstreamBytesPerSec > _maxDownstreamBytesPerSec) {
			_maxDownstreamBytesPerSec = newDownstreamBytesPerSec;
  			DownloadSettings.MAX_DOWNLOAD_BYTES_PER_SEC.setValue(_maxDownstreamBytesPerSec);
  		}
    
        //check if became ultrapeer capable
        setUltrapeerCapable();
        
	}

}
