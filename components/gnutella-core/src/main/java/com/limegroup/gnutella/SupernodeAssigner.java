package com.limegroup.gnutella;

import com.limegroup.gnutella.util.CommonUtils;
import javax.swing.Timer;
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
public final class SupernodeAssigner implements Runnable {

	/**
	 * Constant handle to the <tt>SettingsManager</tt> for accessing
	 * various properties.
	 */
	private final SettingsManager SETTINGS = SettingsManager.instance();

	/**
	 * Constant for the minimum number of upstream kbytes per second that 
	 * a node must be able to transfer in order to qualify as a supernode.
	 */
	private final int MINIMUM_REQUIRED_UPSTREAM_KBYTES_PER_SECOND = 10;

	/**
	 * Constant for the minimum number of downlstream kbytes per second that 
	 * a node must be able to transfer in order to qualify as a supernode.
	 */
	private final int MINIMUM_REQUIRED_DOWNSTREAM_KBYTES_PER_SECOND = 15;

	/**
	 * Constant for the minimum average uptime in seconds that a node must 
	 * have to qualify for supernode status.
	 */
	private final int MINIMUM_AVERAGE_UPTIME = 30 * 60; //1/2 hr

	/**
	 * Constant for the minimum current uptime in seconds that a node must 
	 * have to qualify for supernode status.
	 */
	private final int MINIMUM_CURRENT_UPTIME = 30 * 60; //1/2 hr

	/**
	 * Constant value for whether or not the operating system qualifies
	 * this node for supernode status.
	 */
	private boolean SUPERNODE_OS = CommonUtils.isSupernodeOS();
	
	/**
	 * Constant for the number of milliseconds between the timer's calls
	 * to its <tt>ActionListener</tt>s.
	 */
	private final int TIMER_DELAY = 2000;

	/**
	 * Constant for the number of seconds between the timer's calls
	 * to its <tt>ActionListener</tt>s.
	 */
	private final int TIMER_DELAY_IN_SECONDS = TIMER_DELAY/1000;

	/** 
	 * Constant handle to the <tt>Timer</tt> instance that handles
	 * data collection for upload and bandwidth data.
	 */
	private Timer _bandwidthTimer;

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
        this._manager = manager;
        _wasSupernodeCapable = _manager.isSupernode();
		ActionListener timerListener = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				collectBandwidthData();
			}
		};
		_bandwidthTimer = new Timer(TIMER_DELAY, timerListener);
    }
    
	/**
	 * Implements the <tt>Runnable</tt> interface.
	 * Starts the <tt>Timer</tt> that continually updates the upload
	 * and download bandwidth used.
	 */
    public void run() {
		_bandwidthTimer.start();
    }

	/**
	 * Sets EVER_SUPERNODE_CAPABLE to true if this has the necessary
	 * requirements for becoming a supernode if needed, based on 
	 * the node's bandwidth, operating system, firewalled status, 
	 * uptime, etc.  Does not modify the property if the capabilities
     * are not met.  If the user has disabled supernode support, 
     * sets EVER_SUPERNODE_CAPABLE to false.
	 */
	public void setSupernodeCapable() {
        if (SETTINGS.getDisableSupernodeMode()) {
            SETTINGS.setEverSupernodeCapable(false);
            return;
        }

        boolean isSupernodeCapable = 
            //Is upstream OR downstream high enough?
            (_maxUpstreamBytesPerSec >= 
                    MINIMUM_REQUIRED_UPSTREAM_KBYTES_PER_SECOND ||
             _maxDownstreamBytesPerSec >= 
                    MINIMUM_REQUIRED_DOWNSTREAM_KBYTES_PER_SECOND) &&
            //AND is my average uptime OR current uptime high enough?
            (SETTINGS.getAverageUptime() >= MINIMUM_AVERAGE_UPTIME ||
             _currentUptime >= MINIMUM_CURRENT_UPTIME) &&
            //AND am I not firewalled?
			SETTINGS.getEverAcceptedIncoming() &&
            //AND am I a capable OS?
			SUPERNODE_OS;
        
        // if this is supernode capable, make sure we record it
        if(isSupernodeCapable) SETTINGS.setEverSupernodeCapable(true);
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
		int newUpstreamBytesPerSec = 
            (int)_uploadTracker.getMeasuredBandwidth()
           +(int)_manager.getMeasuredUpstreamBandwidth();
		int newDownstreamBytesPerSec = 
            (int)_downloadTracker.getMeasuredBandwidth()
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
