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
	 * Constant for the minimum number of upstream bytes per second that 
	 * a node must be able to transfer in order to qualify as a supernode.
	 */
	private final int MINIMUM_REQUIRED_UPSTREAM_BYTES_PER_SECOND = 10000;

	/**
	 * Constant for the minimum number of downlstream bytes per second that 
	 * a node must be able to transfer in order to qualify as a supernode.
	 */
	private final int MINIMUM_REQUIRED_DOWNSTREAM_BYTES_PER_SECOND = 15000;

	/**
	 * Constant for the minimum average uptime in seconds that a node must 
	 * have to qualify for supernode status.
	 */
	private final int MINIMUM_AVERAGE_UPTIME = 180 * 60;

	/**
	 * Constant for the minimum current uptime in seconds that a node must 
	 * have to qualify for supernode status.
	 */
	private final int MINIMUM_CURRENT_UPTIME = 9 * 60;    

	/**
	 * Constant value for whether or not the operating system qualifies
	 * this node for supernode status.
	 */
	private boolean SUPERNODE_OS = CommonUtils.isSupernodeOS();
	
	/**
	 * Constant <tt>boolean</tt> for whether or not this node should
	 * be considered firewalled.
	 */
	private boolean FIREWALLED = CommonUtils.isPrivateAddress() ||
		!SETTINGS.getEverAcceptedIncoming();
	
	/**
	 * Constant for the average uptime measured over the all of the
	 * times this node has been run.
	 */
	private final long AVERAGE_UPTIME = SETTINGS.getAverageUptime();

	/**
	 * Constant for the number of milliseconds between the timer's calls
	 * to its <tt>ActionListener</tt>s.
	 */
    private final int TIMER_DELAY = 10 * 60 * 1000; //10 minutes

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
    private volatile boolean _wasSupernodeCapable = SETTINGS.isSupernode();

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
	 * Returns whether or not this node meets all the necessary 
	 * requirements for becoming a supernode if needed, based on 
	 * the node's bandwidth, operating system, firewalled status, 
	 * uptime, etc.
	 *
	 * @return <tt>true</tt> if this node meets the requirements of 
	 *         a supernode, <tt>false</tt> otherwise
	 */
	public boolean isSupernodeCapable() {
        boolean isSupernodeCapable = 
            (((_maxUpstreamBytesPerSec >= 
            MINIMUM_REQUIRED_UPSTREAM_BYTES_PER_SECOND) ||
            (_maxDownstreamBytesPerSec >= 
            MINIMUM_REQUIRED_DOWNSTREAM_BYTES_PER_SECOND)) &&
            (AVERAGE_UPTIME >= MINIMUM_AVERAGE_UPTIME) &&
			(_currentUptime >= MINIMUM_CURRENT_UPTIME) &&
			(!FIREWALLED) &&
			(SUPERNODE_OS));
        
        // if this is supernode capable, make sure we record it
        if(isSupernodeCapable) SETTINGS.setEverSupernodeCapable(true);
        return isSupernodeCapable;
	}

	/**
	 * Collects data on the bandwidth that has been used for file uploads
	 * and downloads.
	 */
	private void collectBandwidthData() {
//		System.out.println("_maxDownstreamBytesPerSec: "+_maxDownstreamBytesPerSec);
//        System.out.println();
//        System.out.println("_maxUpstreamBytesPerSec: "+_maxUpstreamBytesPerSec);
//        System.out.println();
//        System.out.println("_currentUptime: " + _currentUptime);
//        System.out.println();
//        System.out.println("AVERAGE_UPTIME: " + AVERAGE_UPTIME);
//        System.out.println();
//        System.out.println("FIREWALLED: " + FIREWALLED);
//        System.out.println();
//        System.out.println("SUPERNODE_OS: " + SUPERNODE_OS);
//        System.out.println();
//        System.out.println("isSupernodeCapable: "+isSupernodeCapable());
		_currentUptime += TIMER_DELAY_IN_SECONDS;
        int newUpstreamBytes   = _uploadTracker.getNewBytesTransferred();
        int newDownstreamBytes = _downloadTracker.getNewBytesTransferred();
		int newUpstreamBytesPerSec = 
            newUpstreamBytes/TIMER_DELAY_IN_SECONDS;
		int newDownstreamBytesPerSec = 
            newDownstreamBytes/TIMER_DELAY_IN_SECONDS;
		if(newUpstreamBytesPerSec > _maxUpstreamBytesPerSec) {
			_maxUpstreamBytesPerSec = newUpstreamBytesPerSec;
			SETTINGS.setMaxUpstreamBytesPerSec(_maxUpstreamBytesPerSec);
		}
  		if(newDownstreamBytesPerSec > _maxDownstreamBytesPerSec) {
			_maxDownstreamBytesPerSec = newDownstreamBytesPerSec;
  			SETTINGS.setMaxDownstreamBytesPerSec(_maxDownstreamBytesPerSec);
  		}
        
        //check if the state changed
        boolean isSupernodeCapable = isSupernodeCapable();
        if((isSupernodeCapable != _wasSupernodeCapable) &&
            !SETTINGS.hasSupernodeOrClientnodeStatusForced() 
            && !SETTINGS.hasShieldedClientSupernodeConnection()){
                SETTINGS.setSupernodeMode(isSupernodeCapable);
                _manager.reconnect();
        }
            _wasSupernodeCapable = isSupernodeCapable;
	}

}
