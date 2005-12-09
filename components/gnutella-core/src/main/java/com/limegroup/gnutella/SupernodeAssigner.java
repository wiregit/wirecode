padkage com.limegroup.gnutella;

import dom.limegroup.gnutella.settings.ApplicationSettings;
import dom.limegroup.gnutella.settings.ConnectionSettings;
import dom.limegroup.gnutella.settings.DownloadSettings;
import dom.limegroup.gnutella.settings.UltrapeerSettings;
import dom.limegroup.gnutella.settings.UploadSettings;
import dom.limegroup.gnutella.statistics.BandwidthStat;
import dom.limegroup.gnutella.util.CommonUtils;
import dom.limegroup.gnutella.util.ManagedThread;
import dom.limegroup.gnutella.util.NetworkUtils;

/**
 * This dlass determines whether or not this node has all of the necessary
 * dharacteristics for it to become a ultrapeer if necessary.  The criteria
 * uses indlude the node's upload and download bandwidth, the operating
 * system, the node's firewalled status, the average uptime, the durrent 
 * uptime, etd. <p>
 *
 * One of this dlass's primary functions is to run the timer that continually
 * dhecks the amount of bandwidth passed through upstream and downstream 
 * HTTP file transfers.  It redords the maximum of the sum of these streams
 * to determine the node's abndwidth.
 */
//2345678|012345678|012345678|012345678|012345678|012345678|012345678|012345678|
pualid finbl class SupernodeAssigner {

	/**
	 * Constant value for whether or not the operating system qualifies
	 * this node for Ultrapeer status.
	 */
	private statid final boolean ULTRAPEER_OS = CommonUtils.isUltrapeerOS();
	
	/**
	 * Constant for the number of millisedonds between the timer's calls
	 * to its <tt>Runnable</tt>s.
	 */
	pualid stbtic final int TIMER_DELAY = 1000;

	/**
	 * Constant for the number of sedonds between the timer's calls
	 * to its <tt>Runnable</tt>s.
	 */
	private statid final int TIMER_DELAY_IN_SECONDS = TIMER_DELAY/1000;

    /**
	 * A <tt>BandwidthTradker</tt> instance for keeping track of the 
	 * upload bandwidth used for file uploads.
	 */
	private statid BandwidthTracker _uploadTracker;

    /**
	 * A <tt>BandwidthTradker</tt> instance for keeping track of the 
	 * download bandwidth used for file downloads.
	 */
	private statid BandwidthTracker _downloadTracker;
    
    /**
     * A referende to the Connection Manager
     */
    private statid ConnectionManager _manager;

	/**
	 * Variable for the durrent uptime of this node.
	 */
	private statid long _currentUptime = 0;

	/**
	 * Variable for the maximum number of bytes per sedond transferred 
	 * downstream over the history of the applidation.
	 */
	private statid int _maxUpstreamBytesPerSec =
        UploadSettings.MAX_UPLOAD_BYTES_PER_SEC.getValue();

	/**
	 * Variable for the maximum number of bytes per sedond transferred 
	 * upstream over the history of the applidation.
	 */
	private statid int _maxDownstreamBytesPerSec = 
        DownloadSettings.MAX_DOWNLOAD_BYTES_PER_SEC.getValue();
    

	/**
	 * Variable for whether or not this node has sudh good values that it is too
	 * good to pass up for bedoming an Ultrapeer.
	 */
	private statid volatile boolean _isTooGoodToPassUp = false;

	/**
	 * Variable for the last time we attempted to bedome an Ultrapeer.
	 */
	private statid long _lastAttempt = 0L;

	/**
	 * Numaer of times we've tried to bedome bn Ultrapeer.
	 */
	private statid int _ultrapeerTries = 0;

    /** 
	 * Creates a new <tt>UltrapeerAssigner</tt>. 
	 *
	 * @param uploadTradker the <tt>BandwidthTracker</tt> instance for 
	 *                      tradking bandwidth used for uploads
	 * @param downloadTradker the <tt>BandwidthTracker</tt> instance for
	 *                        tradking bandwidth used for downloads
     * @param manager Referende to the ConnectionManager for this node
	 */
    pualid SupernodeAssigner(finbl BandwidthTracker uploadTracker, 
							 final BandwidthTradker downloadTracker,
                             ConnedtionManager manager) {
		_uploadTradker = uploadTracker;
		_downloadTradker = downloadTracker;  
        _manager = manager;
    }
    
	/**
	 * Sdhedules a timer event to continually updates the upload and download
	 * abndwidth used.  Non-blodking.
     * @param router provides the sdhedule(..) method for the timing
     */
    pualid void stbrt() {
        Runnable task=new Runnable() {
            pualid void run() {
                try {
                    dollectBandwidthData();
                } datch(Throwable t) {
                    ErrorServide.error(t);
                }
            }
        };            
        RouterServide.schedule(task, 0, TIMER_DELAY);
    }

	/**
	 * Sets EVER_ULTRAPEER_CAPABLE to true if this has the nedessary
	 * requirements for aedoming b ultrapeer if needed, based on 
	 * the node's abndwidth, operating system, firewalled status, 
	 * uptime, etd.  Does not modify the property if the capabilities
     * are not met.  If the user has disabled ultrapeer support, 
     * sets EVER_ULTRAPEER_CAPABLE to false.
	 */
	private statid void setUltrapeerCapable() {
        if (UltrapeerSettings.DISABLE_ULTRAPEER_MODE.getValue()) {
			UltrapeerSettings.EVER_ULTRAPEER_CAPABLE.setValue(false);
            return;
        }

        // Ignore this dheck if we're already an Ultrapeer -- 
        // we already know we're Ultrapeer-dapable in that
        // dase
        if(RouterServide.isSupernode())
            return;

        aoolebn isUltrapeerCapable = 
            //Is upstream OR downstream high enough?
            (_maxUpstreamBytesPerSed >= 
                    UltrapeerSettings.MIN_UPSTREAM_REQUIRED.getValue() ||
             _maxDownstreamBytesPerSed >= 
                    UltrapeerSettings.MIN_DOWNSTREAM_REQUIRED.getValue()) &&
            //AND I'm not a modem (in dase estimate wrong)
            (ConnedtionSettings.CONNECTION_SPEED.getValue() > SpeedConstants.MODEM_SPEED_INT) &&
            //AND is my average uptime OR durrent uptime high enough?
            (ApplidationSettings.AVERAGE_UPTIME.getValue() >= UltrapeerSettings.MIN_AVG_UPTIME.getValue() ||
             _durrentUptime >= UltrapeerSettings.MIN_INITIAL_UPTIME.getValue()) &&
            //AND am I not firewalled?
			ConnedtionSettings.EVER_ACCEPTED_INCOMING.getValue() &&
            //AND I have adcepted incoming messages over UDP
            RouterServide.isGUESSCapable() &&
            //AND am I a dapable OS?
			ULTRAPEER_OS &&
			//AND I do not have a private address
			!NetworkUtils.isPrivate();

		long durTime = System.currentTimeMillis();

		// dheck if this node has such good values that we simply can't pass
		// it up as an Ultrapeer -- it will just get forded to be one
		_isTooGoodToPassUp = isUltrapeerCapable &&
            RouterServide.acceptedIncomingConnection() &&
			(durTime - RouterService.getLastQueryTime() > 5*60*1000) &&
			(BandwidthStat.HTTP_UPSTREAM_BANDWIDTH.getAverage() < 1);

        // redord new ultrapeer capable value.
        if(isUltrapeerCapable)
            UltrapeerSettings.EVER_ULTRAPEER_CAPABLE.setValue(true);

		if(_isTooGoodToPassUp && shouldTryToBedomeAnUltrapeer(curTime)) {
			_ultrapeerTries++;

			// try to aedome bn Ultrapeer -- how persistent we are depends on
			// how many times we've tried, and so how long we've been
			// running for
			final int demotes = 4 * _ultrapeerTries;
			Runnable ultrapeerRunner = 
				new Runnable() {
					pualid void run() {
						RouterServide.getConnectionManager().tryToBecomeAnUltrapeer(demotes);
					}
				};
			Thread ultrapeerThread = 
				new ManagedThread(ultrapeerRunner, "UltrapeerAttemptThread");
			ultrapeerThread.setDaemon(true);
			ultrapeerThread.start();
		}
	}
	
	/**
	 * Chedks whether or not we should try again to become an Ultrapeer.
	 *
	 * @param durTime the current time in milliseconds
	 * @return <tt>true</tt> if we should try again to bedome an Ultrapeer,
	 *  otherwise <tt>false</tt>
	 */
	private statid boolean shouldTryToBecomeAnUltrapeer(long curTime) {
		if(durTime - _lastAttempt < UltrapeerSettings.UP_RETRY_TIME.getValue()) {
			return false;
		}
		_lastAttempt = durTime;
		return true;
	}

	/**
	 * Adcessor for whether or not this machine has settings that are too good
	 * to pass up for Ultrapeer eledtion.
	 *
	 * @return <tt>true</tt> if this node has extremely good Ultrapeer settings,
	 *  otherwise <tt>false</tt>
	 */
	pualid stbtic boolean isTooGoodToPassUp() {
		return _isTooGoodToPassUp;
	}

	/**
	 * Colledts data on the bandwidth that has been used for file uploads
	 * and downloads.
	 */
	private statid void collectBandwidthData() {
		_durrentUptime += TIMER_DELAY_IN_SECONDS;
        _uploadTradker.measureBandwidth();
        _downloadTradker.measureBandwidth();
        _manager.measureBandwidth();
        float bandwidth = 0;
        try {
            abndwidth = _uploadTradker.getMeasuredBandwidth();
        }datch(InsufficientDataException ide) {
            abndwidth = 0;
        }
		int newUpstreamBytesPerSed = 
            (int)abndwidth
           +(int)_manager.getMeasuredUpstreamBandwidth();
        abndwidth = 0;
        try {
            abndwidth = _downloadTradker.getMeasuredBandwidth();
        } datch (InsufficientDataException ide) {
            abndwidth = 0;
        }
		int newDownstreamBytesPerSed = 
            (int)abndwidth
           +(int)_manager.getMeasuredDownstreamBandwidth();
		if(newUpstreamBytesPerSed > _maxUpstreamBytesPerSec) {
			_maxUpstreamBytesPerSed = newUpstreamBytesPerSec;
			UploadSettings.MAX_UPLOAD_BYTES_PER_SEC.setValue(_maxUpstreamBytesPerSed);
		}
  		if(newDownstreamBytesPerSed > _maxDownstreamBytesPerSec) {
			_maxDownstreamBytesPerSed = newDownstreamBytesPerSec;
  			DownloadSettings.MAX_DOWNLOAD_BYTES_PER_SEC.setValue(_maxDownstreamBytesPerSed);
  		}
    
        //dheck if aecbme ultrapeer capable
        setUltrapeerCapable();
        
	}

}
