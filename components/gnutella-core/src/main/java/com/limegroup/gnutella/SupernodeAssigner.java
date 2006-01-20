
// Commented for the Learning branch

package com.limegroup.gnutella;

import com.limegroup.gnutella.settings.ApplicationSettings;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.settings.DownloadSettings;
import com.limegroup.gnutella.settings.UltrapeerSettings;
import com.limegroup.gnutella.settings.UploadSettings;
import com.limegroup.gnutella.statistics.BandwidthStat;
import com.limegroup.gnutella.util.CommonUtils;
import com.limegroup.gnutella.util.ManagedThread;
import com.limegroup.gnutella.util.NetworkUtils;

/**
 * On the Gnutella network, there is no central server to tell us if we should be an ultrapeer or not.
 * And, groups of computers don't get together to decide which one of them should be an ultrapeer.
 * It's actually a lot simpler than that.
 * We, all by ourselves, look at how fast our Internet connection is and how long we've been online.
 * If we have pretty good numbers, we decide that we are ultrapeer capable.
 * We start connecting to computers with "X-Ultrapeer: true".
 * If they accept us as an ultrapeer, we are one.
 * 
 * When the program runs, RouterService.start() makes a new SupernodeAssigner object and calls start() on it.
 * The start() method schedules the RouterService to call collectBandwidthData() every second.
 * collectBandwidthData() measures our current speeds uploading and downloading file and Gnutella packet data.
 * 
 * collectBandwidthData() also calls setUltrapeerCapable(), the method that decides if we would make a good ultrapeer.
 * Here's what we look for:
 * An upload speed of 10 KB/s or more or a download speed of 20 KB/s or more.
 * The user did not indicate we're on a modem in settings.
 * An average online time of more than an hour, or a current time online of more than 2 hours.
 * Remote computers can connect to us on TCP and UDP.
 * We're not running on Windows 95, 98, Me, or NT 4.0.
 * 
 * Once we pass these tests, setUltrapeerCapable() sets EVER_ULTRAPEER_CAPABLE to true.
 * Now, ConnectionManager.isSupernodeCapable() will start returning true.
 */
public final class SupernodeAssigner {

	/**
     * True if the operating system we're running on is good enough for us to be an ultrapeer.
     * False if it's Windows 95, 98, Me, or NT 4.0.
	 */
	private static final boolean ULTRAPEER_OS = CommonUtils.isUltrapeerOS(); // Calls System.getProperty("os.name")

	/** 1 second in milliseconds, we'll make a thread that calls collectBandwidthData() every second. */
	public static final int TIMER_DELAY = 1000;

	/** 1 second, the thread will call collectBandwidthData() every second. */
	private static final int TIMER_DELAY_IN_SECONDS = TIMER_DELAY / 1000; // Convert milliseconds to seconds

    /**
     * The object that will keep track of how fast the program is uploading file data.
     * We'll save the program's UploadManger object here, which supports the BandwidthTracker interface.
     * We'll tell it to measure its bandwidth repeatedly, and then ask it how fast it's been uploading data.
	 */
	private static BandwidthTracker _uploadTracker;

    /**
     * The object that will keep track of how fast the program is downloading file data.
     * We'll save the program's DownloadManager object here, which supports the BandwidthTracker interface.
     * We'll tell it to measure its bandwidth repeatedly, and then ask it how fast it's been uploading data.
	 */
	private static BandwidthTracker _downloadTracker;

    /** A reference to the ConnectionManager object that keeps the list of all our Gnutella connections. */
    private static ConnectionManager _manager;

	/**
     * How many seconds the program has been running.
     * 
     * The RouterService has a thread call collectBandwidthData() every second.
     * collectBandwidthData() increments this count.
     * So, _currentUptime counts how many seconds the program has been running, and how many times collectBandwidthData() has ran.
	 */
	private static long _currentUptime = 0; // When the program starts, it's been running 0 seconds

	/**
     * The most bytes we've ever uploaded in a second.
     * We keep _maxUpstreamBytesPerSec in synch with the MAX_UPLOAD_BYTES_PER_SEC setting.
     * We save this value in settings so it can be the fastest speed we've ever witnessed over the history of the program running on this computer.
	 */
	private static int _maxUpstreamBytesPerSec = UploadSettings.MAX_UPLOAD_BYTES_PER_SEC.getValue();

	/**
     * The most bytes we've ever downloaded in a second.
     * We keep _maxDownstreamBytesPerSec in sync with the MAX_DOWNLOAD_BYTES_PER_SEC setting.
     * We save this value in settings so it can be the fastest speed we've ever witnessed over the history of the program running on this computer.
	 */
	private static int _maxDownstreamBytesPerSec = DownloadSettings.MAX_DOWNLOAD_BYTES_PER_SEC.getValue();

	/**
     * If we pass every test in isUltrapeerCapable(), _isTooGoodToPassUp is true.
     * isUltrapeerCapable() makes a "UltrapeerAttemptThread" that calls ConnectionManager.tryToBecomeAnUltrapeer() now.
     * ConnectionManager.allowLeafDemotion() doesn't.
	 */
	private static volatile boolean _isTooGoodToPassUp = false;

	/**
     * The last time shouldTryToBecomeAnUltrapeer() returned true.
     * shouldTryToBecomeAnUltrapeer() uses this time to only return true once every 3 hours.
	 */
	private static long _lastAttempt = 0L; // Initialize to 0 so shouldTryToBecomeAnUltrapeer() will return true the first time setUltrapeerCapable() calls it

	/**
     * The number of times we've tried to become an ultrapeer.
     * We only try to do this once every 3 hours.
     * 
     * Trying to become an ultrapeer looks like this.
     * First, we decide by ourselves that our Internet connection is fast enough and we've been online long enough that we'd make a good ultrapeer.
     * When we connect to a remote computer, we present ourselves as an ultrapeer, saying "X-Ultrapeer: true".
     * The remote computer accepts us as a fellow ultrapeer, or joins the network under us as our leaf.
	 */
	private static int _ultrapeerTries = 0;

    /**
     * Make the new SupernodeAssigner object.
     * RouterService.start() calls this when the program runs.
     * It passes in references to 3 important objects.
     * 
     * @param uploadTracker   The UploadManager object, which supports the BandwidthTracker interface, and will keep track of how fast we upload file data
     * @param downloadTracker The DownloadManager object, which supports the BandwidthTracker interface, and will keep track of how fast we download file data
     * @param manager         The ConnectionManager object, which maintains the Gnutella connections
	 */
    public SupernodeAssigner(final BandwidthTracker uploadTracker, final BandwidthTracker downloadTracker, ConnectionManager manager) {

        // Save references to the given objects
		_uploadTracker   = uploadTracker;
		_downloadTracker = downloadTracker;
        _manager         = manager;
    }

	/**
     * Schedules the RouterService to call collectBandwidthData() once a second.
     * Right after RouterService.start() creates the SupernodeAssigner, it calls this start() method on it.
     */
    public void start() {

        // Make a new object named task of a class that doesn't have a name and we'll define right here
        Runnable task = new Runnable() { // The class implements the Runnable interface, requiring it to have a run method

            // The RouterService will have a thread call this run() method
            public void run() {
                
                try {
                    
                    // Call the static collectBandwidthData() method in this class
                    collectBandwidthData();

                // Have the ErrorService log any exceptions, and keep going
                } catch (Throwable t) { ErrorService.error(t); }
            }
        };

        // Have the RouterService call collectBandwidthData() every second
        RouterService.schedule(task, 0, TIMER_DELAY); // Give schedule() a delay time of 1 second
    }

	/**
     * Looks at our Internet speed, connectability, and online time, and makes isSupernodeCapable() start returning true.
     * 
     * Looks at how fast our Internet connections is and how long we've been online.
     * If these are good, sets EVER_ULTRAPEER_CAPABLE to true, making ConnectionManager.isSupernodeCapable() start returning true.
     * If we pass every test, sets _isTooGoodToPassUp to true, and starts a "" thread which calls ConnectionManager.tryToBecomeAnUltrapeer() right now.
     * 
     * EVER_ULTRAPEER_CAPABLE is a setting, so it's value lasts between times the programs runs.
     * If we were ultrapeer capable yesterday, we're likely on the same computer and Internet connection, so we're ultrapeer capable now too.
	 */
	private static void setUltrapeerCapable() {

        // If settings prevent us from becoming an ultrapeer
        if (UltrapeerSettings.DISABLE_ULTRAPEER_MODE.getValue()) {

            // Record in settings that we haven't yet found our computer and Internet connection to be fast enough for us to be an ultrapeer
			UltrapeerSettings.EVER_ULTRAPEER_CAPABLE.setValue(false);
            return;
        }

        // If we're already an ultrapeer, we don't have to run the tests, leave now
        if (RouterService.isSupernode()) return;

        /*
         * Now we're going to set two booleans: isUltrapeerCapable, and isTooGoodToPassUp.
         * For both, we'll see if we have a fast Internet connection, we've been online a long time, and we're externally contactable.
         * 
         * If we pass a basic set of tests, isUltrapeerCapable will be true.
         * If we pass those and then even more tests, isTooGoodToPassUp will be true.
         * 
         * If isTooGoodToPassUp is true, we'll be forced into ultrapeer mode.
         */

        // We are capable of being an ultrapeer on the Gnutella network if all of the following things are true
        boolean isUltrapeerCapable =

            /* We have a fast Internet connection. */

            // Our upstream or downstream bandwidth is high enough
            (_maxUpstreamBytesPerSec >= UltrapeerSettings.MIN_UPSTREAM_REQUIRED.getValue() ||         // Our upload speed is 10 KB/s or more
                _maxDownstreamBytesPerSec >= UltrapeerSettings.MIN_DOWNSTREAM_REQUIRED.getValue()) && // Our download speed is 20 KB/s or more

            // The user indicated something faster than a modem in the startup wizard and connection settings
            (ConnectionSettings.CONNECTION_SPEED.getValue() > SpeedConstants.MODEM_SPEED_INT) &&

            /* We won't go offline right away. */

            // The programs runs for more than an hour on average, or we've been running for more than 2 hours right now
            (ApplicationSettings.AVERAGE_UPTIME.getValue() >= UltrapeerSettings.MIN_AVG_UPTIME.getValue() || // On average, the user runs the program for an hour or more
                _currentUptime >= UltrapeerSettings.MIN_INITIAL_UPTIME.getValue()) &&                        // We've been running for more than 2 hours

            /* Remote computers can connect to us. */

            // When we asked an ultrapeer to connect back to our TCP listening socket it worked, proving that we're externally contactable on the Internet
			ConnectionSettings.EVER_ACCEPTED_INCOMING.getValue() &&

            // Remote computers can send us UDP packets
            RouterService.isGUESSCapable() &&

            /* We are running on a modern operating system. */

            // We're not running on Windows 95, 98, Me, or NT 4.0
			ULTRAPEER_OS &&

            /* Another one related to remote computers being able to connect to us. */

            // The IP address we've been telling remote computers is a real Internet IP address, not just a LAN one
			!NetworkUtils.isPrivate();

        // Get the time right now
		long curTime = System.currentTimeMillis();

        // If all of those things are true and some more here, we're so good we're too good to pass up
		_isTooGoodToPassUp =

            // We have the speed, time online, external reach, and operating system we need to be an ultrapeer
            isUltrapeerCapable &&

            // A connect back check worked, we already checked for this above
            RouterService.acceptedIncomingConnection() &&

            // We last sent a query more than 5 minutes ago, indicating the user isn't here typing in searches rigth now
			(curTime - RouterService.getLastQueryTime() > 5 * 60 * 1000) &&

            // On average, we're uploading file data at a rate less than 1 KB/s
			(BandwidthStat.HTTP_UPSTREAM_BANDWIDTH.getAverage() < 1);

            /*
             * Good ultrapeers have fast upload speed, but don't actually upload a lot of file data.
             * In fact, they ideally aren't even sharing any files.
             * We want them to use all their performance for Gnutella connections, not file transfer.
             */

        // We've found our computer and Internet connection fast enough for us to be an ultrapeer, record that we passed the test in settings
        if (isUltrapeerCapable) UltrapeerSettings.EVER_ULTRAPEER_CAPABLE.setValue(true);

        /*
         * With the EVER_ULTRAPEER_CAPABLE setting set to true, ConnectionManager.isSupernodeCapable() will start returning true.
         */

        // If we passed every test and this is our first try or our first in 3 hours, make a thread to try to become an ultrapeer right now
		if(_isTooGoodToPassUp && shouldTryToBecomeAnUltrapeer(curTime)) {

            // We're going to try to join the Gnutella network as an ultrapeer
			_ultrapeerTries++;
            
            /*
             * Now we're going to try to become an ultrapeer.
             * We'll start telling new remote computers "X-Ultrapeer: true".
             * They may try to get us to be their leaf, by saying "X-Ultrapeer-Needed: false".
             * We'll ignore this guidance a certain number of times before giving in and being a leaf.
             * 
             * If this is our first attempt to connect as an ultrapeer, we'll ignore 4 demotes.
             * 3 hours later, we'll ignore 8 demotes
             */

            // Calculate how many times we'll ignore an ultrapeer that tells us to be a leaf
			final int demotes = 4 * _ultrapeerTries; // 4 the first them, then 8, 12, 16

            // Define a run() method here that we'll make a thread to run
			Runnable ultrapeerRunner = new Runnable() {

                // A new "UltrapeerAttemptThread" will run this method
			    public void run() {

                    // Have the thread call ConnectionManager.tryToBecomeAnUltrapeer()
			        RouterService.getConnectionManager().tryToBecomeAnUltrapeer(demotes); // Pass it the number of times we'll ignore being demoted
			    }
			};

            // Start a new thread named "UltrapeerAttemptThread" on the run() method right now
			Thread ultrapeerThread = new ManagedThread( // Make a new LimeWire ManagedThread object, which extends Thread
                ultrapeerRunner,                        // It will run the run() method above
                "UltrapeerAttemptThread");              // Name it "UltrapeerAttemptThread"
			ultrapeerThread.setDaemon(true);            // Let the Java virtual machine exit even if this thread is still running
			ultrapeerThread.start();                    // Start it on run() now
		}
	}

	/**
     * Returns true the first time you call it, and then only once every 3 hours.
     * Determines whether or not we should try again to become an ultrapeer.
     * 
     * @param curTime The time right now
     * @return        False if it hasn't been 3 hours yet, and we should stay in our role as a leaf.
     *                True if it has been 3 hours, and we should see if the network will accept us as an ultrapeer.
	 */
	private static boolean shouldTryToBecomeAnUltrapeer(long curTime) {

        /*
         * _lastAttempt is the time this method last returned true.
         * curTime - _lastAttempt is the time since then.
         * If it hasn't been UP_RETRY_TIME 3 hours yet, return false.
         * If it has, set _lastAttempt to now, and return true.
         */

        // If we returned true less than 3 hours ago, return false
        if (curTime - _lastAttempt < UltrapeerSettings.UP_RETRY_TIME.getValue()) return false;

        // This is the first time or it's been more than 3 hours, record the time and return true
		_lastAttempt = curTime;
		return true;
	}

	/**
     * True if our Internet speed and online time would make us an excellent ultrapeer.
     * 
     * ConnectionManager.allowLeafDemotion() calls this when a remote computer is telling us to drop down to leaf mode.
     * If setUltrapeerCapable() found us too good to pass up, allowLeafDemotion() won't allow it.
     * 
     * @return True if our Internet speed and online time would make us a very good ultrapeer
	 */
	public static boolean isTooGoodToPassUp() {

        // setUltrapeerCapable() made this true if we passed every test
		return _isTooGoodToPassUp;
	}

	/**
     * Gets our current transfer speed, and sees if our Internet connection is fast enough for us to be an ultrapeer.
     * The RouterService has a thread that calls this method once every second as the program runs.
     * 
     * Totals how fast we're uploading and downloading file data and Gnutella packets.
     * Saves new record speeds in settings.
     * Calls setUltrapeerCapable() to see if our new speed records make us ultrapeer material.
	 */
	private static void collectBandwidthData() {

        // Count that the program has been running one more second
		_currentUptime += TIMER_DELAY_IN_SECONDS;

        // Tell the UploadManager, DownloadManager, and ConnectionManager objects to measure their transfer speeds right now
        _uploadTracker.measureBandwidth();
        _downloadTracker.measureBandwidth();
        _manager.measureBandwidth();

        // Find out what our file upload speed is right now
        float bandwidth = 0;
        try {

            // Ask the UploadManager object how fast it's uploading file data right now
            bandwidth = _uploadTracker.getMeasuredBandwidth(); // Gets the speed in KB/s

        // The UploadManager doesn't have enough data to calculate a speed accurately, assume it's stopped
        } catch (InsufficientDataException ide) { bandwidth = 0; }

        // Add the speed we're sending Gnutella packet data
		int newUpstreamBytesPerSec = (int)bandwidth + (int)_manager.getMeasuredUpstreamBandwidth();

        // Find out what our file download speed is right now
        bandwidth = 0;
        try {

            // Ask the DownloadManager object how fast it's downloading file data right now
            bandwidth = _downloadTracker.getMeasuredBandwidth(); // Gets the speed in KB/s

        // The DownloadManager doesn't have enough data to calculate a speed accurately, assume it's stopped
        } catch (InsufficientDataException ide) { bandwidth = 0; }

        // Add the speed we're receiving Gnutella packet data
		int newDownstreamBytesPerSec = (int)bandwidth + (int)_manager.getMeasuredDownstreamBandwidth();

        // If we're sending data faster than we've ever recorded
		if (newUpstreamBytesPerSec > _maxUpstreamBytesPerSec) {

            // Save the new highest speed in settings
			_maxUpstreamBytesPerSec = newUpstreamBytesPerSec;
			UploadSettings.MAX_UPLOAD_BYTES_PER_SEC.setValue(_maxUpstreamBytesPerSec);
		}

        // If we're receiving data faster than we've ever recorded
  		if (newDownstreamBytesPerSec > _maxDownstreamBytesPerSec) {

            // Save the new highest speed in settings
			_maxDownstreamBytesPerSec = newDownstreamBytesPerSec;
  			DownloadSettings.MAX_DOWNLOAD_BYTES_PER_SEC.setValue(_maxDownstreamBytesPerSec);
  		}

        // See if new faster recorded speeds are fast enough to make us able to act as an ultrapeer on the Gnutella network
        setUltrapeerCapable();
	}
}
