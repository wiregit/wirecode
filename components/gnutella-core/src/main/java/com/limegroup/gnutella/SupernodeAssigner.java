package com.limegroup.gnutella;

import com.limegroup.gnutella.util.CommonUtils;
import com.limegroup.gnutella.settings.*;
import java.awt.event.*;

/**
 * This class determines whether or not this node has all of the necessary
 * characteristics for it to become a supernode if necessary.  The criteria
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
	 * Constant handle to the <tt>SettingsManager</tt> for accessing
	 * various properties.
	 */
	private final SettingsManager SETTINGS = SettingsManager.instance();

	/**
	 * Constant for the minimum number of upstream kbytes per second that 
	 * a node must be able to transfer in order to qualify as a supernode.
	 */
	private final int MINIMUM_REQUIRED_UPSTREAM_KBYTES_PER_SECOND = 15;

	/**
	 * Constant for the minimum number of downlstream kbytes per second that 
	 * a node must be able to transfer in order to qualify as a supernode.
	 */
	private final int MINIMUM_REQUIRED_DOWNSTREAM_KBYTES_PER_SECOND = 20;

	/**
	 * Constant for the minimum average uptime in seconds that a node must 
	 * have to qualify for supernode status.
	 */
	private final int MINIMUM_AVERAGE_UPTIME = 60 * 60; //1 hr

	/**
	 * Constant for the minimum current uptime in seconds that a node must 
	 * have to qualify for supernode status.
	 */
	private final int MINIMUM_CURRENT_UPTIME = 120 * 60; //2 hr

	/**
	 * Constant for the minimum current uptime in seconds that a node must 
	 * have for us to attempt to switch them from a leaf to an Ultrapeer
	 */
	private final int MINIMUM_CURRENT_UPTIME_FORCE = 150 * 60; //2.5 hr

	/**
	 * Constant value for whether or not the operating system qualifies
	 * this node for Ultrapeer status.
	 */
	private boolean SUPERNODE_OS = CommonUtils.isSupernodeOS();
	
	/**
	 * Constant for the number of milliseconds between the timer's calls
	 * to its <tt>Runnable</tt>s.
	 */
	public static final int TIMER_DELAY = 1000;

	/**
	 * Constant for the number of seconds between the timer's calls
	 * to its <tt>Runnable</tt>s.
	 */
	private final int TIMER_DELAY_IN_SECONDS = TIMER_DELAY/1000;

    /**
	 * A <tt>BandwidthTracker</tt> instance for keeping track of the 
	 * upload bandwidth used for file uploads.
	 */
	private BandwidthTracker _uploadTracker;

    /**
	 * A <tt>BandwidthTracker</tt> instance for keeping track of the 
	 * download bandwidth used for file downloads.
	 */
	private BandwidthTracker _downloadTracker;
    
    /**
     * A reference to the Connection Manager
     */
    private ConnectionManager _manager;

	/**
	 * Variable for the current uptime of this node.
	 */
	private long _currentUptime = 0;

	/**
	 * Variable for the maximum number of bytes per second transferred 
	 * downstream over the history of the application.
	 */
	private int _maxUpstreamBytesPerSec =
        SETTINGS.getMaxUpstreamBytesPerSec();

	/**
	 * Variable for the maximum number of bytes per second transferred 
	 * upstream over the history of the application.
	 */
	private int _maxDownstreamBytesPerSec = 
        SETTINGS.getMaxDownstreamBytesPerSec();
    
    /**
     * True, if the last time we evaluated the node for supernode capability, 
     * it came out as supernode capable. False, otherwise
     */
    private volatile boolean _wasSupernodeCapable;

	/**
	 * Variable for whether or not this node has such good values that it is too
	 * good to pass up for becoming an Ultrapeer.
	 */
	private static volatile boolean _isTooGoodToPassUp = false;

	/**
	 * Variable for the last time we attempted to become an Ultrapeer.
	 */
	private long _lastAttempt = 0L;

	/**
	 * Number of times we've tried to become an Ultrapeer.
	 */
	private int _ultrapeerTries = 0;

	/**
	 * Constant for the amount of time to wait between attempts to become an 
	 * Ultrapeer.
	 */
	private int RETRY_TIME = 90*60*1000; // 90 minutes

    /** 
	 * Creates a new <tt>SupernodeAssigner</tt>. 
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
        _wasSupernodeCapable = _manager.isSupernode();
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
                    RouterService.error(t);
                }
            }
        };            
        RouterService.schedule(task, 0, TIMER_DELAY);
    }

	/**
	 * Sets EVER_SUPERNODE_CAPABLE to true if this has the necessary
	 * requirements for becoming a supernode if needed, based on 
	 * the node's bandwidth, operating system, firewalled status, 
	 * uptime, etc.  Does not modify the property if the capabilities
     * are not met.  If the user has disabled supernode support, 
     * sets EVER_SUPERNODE_CAPABLE to false.
	 */
	private void setSupernodeCapable() {
        if (UltrapeerSettings.DISABLE_ULTRAPEER_MODE.getValue()) {
			UltrapeerSettings.EVER_ULTRAPEER_CAPABLE.setValue(false);
            return;
        }

        boolean isSupernodeCapable = 
            //Is upstream OR downstream high enough?
            (_maxUpstreamBytesPerSec >= 
                    MINIMUM_REQUIRED_UPSTREAM_KBYTES_PER_SECOND ||
             _maxDownstreamBytesPerSec >= 
                    MINIMUM_REQUIRED_DOWNSTREAM_KBYTES_PER_SECOND) &&
            //AND I'm not a modem (in case estimate wrong)
            (SETTINGS.getConnectionSpeed() > SpeedConstants.MODEM_SPEED_INT) &&
            //AND is my average uptime OR current uptime high enough?
            (SETTINGS.getAverageUptime() >= MINIMUM_AVERAGE_UPTIME ||
             _currentUptime >= MINIMUM_CURRENT_UPTIME) &&
            //AND am I not firewalled?
			ConnectionSettings.EVER_ACCEPTED_INCOMING.getValue() &&
            //AND I have accepted incoming messages over UDP
            //RouterService.isGUESSCapable() &&
            //AND am I a capable OS?
			SUPERNODE_OS;

		long curTime = System.currentTimeMillis();

		// check if this node has such good values that we simply can't pass
		// it up as an Ultrapeer -- it will just get forced to be one
		_isTooGoodToPassUp = 
			//are upstream and downstream high enough?
			(_maxUpstreamBytesPerSec >= 
			 MINIMUM_REQUIRED_UPSTREAM_KBYTES_PER_SECOND*2 &&
			 _maxDownstreamBytesPerSec >= 
			 MINIMUM_REQUIRED_DOWNSTREAM_KBYTES_PER_SECOND*2 &&
			 //AND I'm not a modem (in case estimate wrong)
			 (SETTINGS.getConnectionSpeed() > SpeedConstants.CABLE_SPEED_INT) &&
			 //AND is my average uptime OR current uptime high enough?
			 (SETTINGS.getAverageUptime() >= MINIMUM_AVERAGE_UPTIME ||
			  _currentUptime >= MINIMUM_CURRENT_UPTIME_FORCE) &&
			 //AND am I not firewalled?
			 ConnectionSettings.EVER_ACCEPTED_INCOMING.getValue() &&
			 //AND I have accepted incoming messages over UDP
			 //RouterService.isGUESSCapable() &&
			 //AND am I a capable OS?
			 SUPERNODE_OS &&
			 // and we haven't initiated a search in 5 minutes
			 (curTime - RouterService.getLastQueryTime() > 5*60*1000)
			 );

		// TODO:: add HTTP upload bandwidth used as a factor
		// TODO:: add a TEST for this class

        // if this is supernode capable, make sure we record it
        if(isSupernodeCapable) {
			UltrapeerSettings.EVER_ULTRAPEER_CAPABLE.setValue(true);
		}

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
			Thread ultrapeerThread = 
				new Thread(ultrapeerRunner, "UltrapeerAttemptThread");
			ultrapeerThread.setDaemon(true);
			ultrapeerThread.start();
		}
	}
	
	/**
	 * Checks whether or not we should try again to become an Ultrapeer.
	 *
	 * @param curTime the current time in milliseconds
	 * @return <tt>true</tt> if we should try again to become an Ultrapeer,
	 *  otherwise <tt>false</tt>
	 */
	private boolean shouldTryToBecomeAnUltrapeer(long curTime) {
		if(curTime - _lastAttempt < RETRY_TIME) {
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
	private void collectBandwidthData() {
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
			SETTINGS.setMaxUpstreamBytesPerSec(_maxUpstreamBytesPerSec);
		}
  		if(newDownstreamBytesPerSec > _maxDownstreamBytesPerSec) {
			_maxDownstreamBytesPerSec = newDownstreamBytesPerSec;
  			SETTINGS.setMaxDownstreamBytesPerSec(_maxDownstreamBytesPerSec);
  		}
    
        //check if became supernode capable
        setSupernodeCapable();
        
	}

}
