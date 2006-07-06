package com.limegroup.gnutella;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.settings.ApplicationSettings;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.settings.DHTSettings;
import com.limegroup.gnutella.settings.DownloadSettings;
import com.limegroup.gnutella.settings.UltrapeerSettings;
import com.limegroup.gnutella.settings.UploadSettings;
import com.limegroup.gnutella.statistics.BandwidthStat;
import com.limegroup.gnutella.util.CommonUtils;
import com.limegroup.gnutella.util.NetworkUtils;
import com.limegroup.gnutella.util.ThreadFactory;


/**
 * This class's primary functions is to run the timer that continually
 * checks the amount of bandwidth passed through upstream and downstream 
 * HTTP file transfers.  It records the maximum of the sum of these streams
 * to determine the node's bandwidth.
 * 
 * It then updates the UltrapeerCapable and DHTCapable status of this node
 * 
 */
public class NodeAssigner {
    
    private static final Log LOG = LogFactory.getLog(NodeAssigner.class);
    
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
    private static volatile boolean _isTooGoodUltrapeerToPassUp = false;

    /**
     * Variable for the last time we attempted to become an Ultrapeer.
     */
    private static long _lastUltrapeerAttempt = 0L;

    /**
     * Number of times we've tried to become an Ultrapeer.
     */
    private static int _ultrapeerTries = 0;
    
    /**
     * Wether or not the NodeAssigner is trying to promote this node to Ultrapeer.
     * 
     */
    private static boolean _willTryToBeUltrapeer;
    
    /**
     * Wether or not this node is "Hardcore" capable
     */
    private static boolean _isHardcoreCapable;

    /** 
     * Creates a new <tt>NodeAssigner</tt>. 
     *
     * @param uploadTracker the <tt>BandwidthTracker</tt> instance for 
     *                      tracking bandwidth used for uploads
     * @param downloadTracker the <tt>BandwidthTracker</tt> instance for
     *                        tracking bandwidth used for downloads
     * @param manager Reference to the ConnectionManager for this node
     */
    public NodeAssigner(final BandwidthTracker uploadTracker, 
             final BandwidthTracker downloadTracker,
             ConnectionManager manager) {
        _uploadTracker = uploadTracker;
        _downloadTracker = downloadTracker;  
        _manager = manager;
    }
    
    /**
     * Schedules a timer event to continually updates the upload and download
     * bandwidth used.  Non-blocking.
     * Router provides the schedule(..) method for the timing
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
        
        setHardcoreCapable();
        
        //check if became ultrapeer capable
        assignUltrapeerNode();
        //check if became DHT capable
        assignDHTNode();
    }
    
    /**
     * Returns wether or not a node is capable of handling a special
     * function such as beeing an ultrapeer or connecting to the dht
     * 
     */
    private static void setHardcoreCapable() {
        _isHardcoreCapable = 
        //Is upstream OR downstream high enough?
        ((_maxUpstreamBytesPerSec >= 
                UltrapeerSettings.MIN_UPSTREAM_REQUIRED.getValue() ||
         _maxDownstreamBytesPerSec >= 
                UltrapeerSettings.MIN_DOWNSTREAM_REQUIRED.getValue()) &&
        //AND I'm not a modem (in case estimate wrong)
        (ConnectionSettings.CONNECTION_SPEED.getValue() > SpeedConstants.MODEM_SPEED_INT) &&
        //AND am I not firewalled?
        ConnectionSettings.EVER_ACCEPTED_INCOMING.getValue() &&
        //AND I have accepted incoming messages over UDP
        RouterService.isGUESSCapable() &&
        //AND am I a capable OS?
        ULTRAPEER_OS &&
        //AND I do not have a private address
        !NetworkUtils.isPrivate());
    }
    
    /**
     * Sets EVER_ULTRAPEER_CAPABLE to true if this has the necessary
     * requirements for becoming a ultrapeer if needed, based on 
     * the node's bandwidth, operating system, firewalled status, 
     * uptime, etc.  Does not modify the property if the capabilities
     * are not met.  If the user has disabled ultrapeer support, 
     * sets EVER_ULTRAPEER_CAPABLE to false.
     * 
     * @return true if we are or will try to become an ultrapeer, false otherwise
     */
    private static void assignUltrapeerNode() {
        if (UltrapeerSettings.DISABLE_ULTRAPEER_MODE.getValue()) {
            UltrapeerSettings.EVER_ULTRAPEER_CAPABLE.setValue(false);
            _willTryToBeUltrapeer = false;
            return;
        }

        //if we're an ultrapeer, connect to the DHT in passive mode
        //or if we were allready connected active before, switch to passive mode
        if(RouterService.isSupernode()) {
            if(!RouterService.isDHTNode()) {
                RouterService.startDHT(false);
            } else if(RouterService.isActiveDHTNode()) {
                RouterService.setPassiveDHTNode(true);
            }
            return;
        }

        boolean isUltrapeerCapable = 
            (_isHardcoreCapable &&
            //AND is my average uptime OR current uptime high enough?
                    //TODO: GET Average connection uptime here! 
            (ApplicationSettings.AVERAGE_UPTIME.getValue() >= UltrapeerSettings.MIN_AVG_UPTIME.getValue() ||
             _currentUptime >= UltrapeerSettings.MIN_INITIAL_UPTIME.getValue()));
        
        if(LOG.isDebugEnabled()) {
            LOG.debug("Node is "+(isUltrapeerCapable?"":"NOT")+" ultrapeer capable");
        }

        long curTime = System.currentTimeMillis();

        // check if this node has such good values that we simply can't pass
        // it up as an Ultrapeer -- it will just get forced to be one
        _isTooGoodUltrapeerToPassUp = isUltrapeerCapable &&
            RouterService.acceptedIncomingConnection() &&
            (curTime - RouterService.getLastQueryTime() > 5*60*1000) &&
            (BandwidthStat.HTTP_UPSTREAM_BANDWIDTH.getAverage() < 1);

        if(LOG.isDebugEnabled()) {
            LOG.debug("Node is "+(_isTooGoodUltrapeerToPassUp?"":"NOT")+" to good to pass up");
        }
        
        // record new ultrapeer capable value.
        if(isUltrapeerCapable)
            UltrapeerSettings.EVER_ULTRAPEER_CAPABLE.setValue(true);

        if(_isTooGoodUltrapeerToPassUp && 
                shouldTryToBecomeAnUltrapeer(curTime) && 
                switchFromActiveDHTNode()) {
            
            if(LOG.isDebugEnabled()) {
                LOG.debug("Node WILL become an ultrapeer");
            }
            
            _ultrapeerTries++;
            _willTryToBeUltrapeer = true;
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
            ThreadFactory.startThread(ultrapeerRunner, "UltrapeerAttemptThread");
            return;
        } 
        
        if(LOG.isDebugEnabled()) {
            LOG.debug("Node will not try to become an ultrapeer");
        }
        _willTryToBeUltrapeer = false;
        //here: we are not an ultrapeer and will not try to connect as one
        //maybe a demotion from ultrapeer to leaf --> disconnect the DHT if it is the case
        if(RouterService.isPassiveDHTNode()) {
            RouterService.shutdownDHT();
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
        if(curTime - _lastUltrapeerAttempt < UltrapeerSettings.UP_RETRY_TIME.getValue()) {
            return false;
        }
        _lastUltrapeerAttempt = curTime;
        return true;
    }
    
    /**
     * If we are allready actively part of the DHT, switch to ultrapeer with a given 
     * (possibly biased) probability.
     * 
     * @return true if we switched, false otherwise
     */
    private static boolean switchFromActiveDHTNode() {
        if(RouterService.isActiveDHTNode() && DHTSettings.EXCLUDE_ULTRAPEERS.getValue()) {
            if(Math.random() < DHTSettings.DHT_TO_ULTRAPEER_PROBABILITY.getValue()){
                if(LOG.isDebugEnabled()) {
                    LOG.debug("Randomly switching from DHT node to ultrapeer!");
                }
                RouterService.setPassiveDHTNode(true);
                return true;
            } else {
                return false;
            }
        }else {
            return true;
        }
    }
    
    /**
     * Accessor for whether or not this machine has settings that are too good
     * to pass up for Ultrapeer election.
     *
     * @return <tt>true</tt> if this node has extremely good Ultrapeer settings,
     *  otherwise <tt>false</tt>
     */
    public static boolean isTooGoodUltrapeerToPassUp() {
        return _isTooGoodUltrapeerToPassUp;
    }
    
    /**
     * Sets EVER_DHT_CAPABLE to true if this node has the necessary
     * requirements for joining the DHT. DOES modify the property if the capabilities
     * are not met and disconnects the node from the DHT.  
     * If the user has disabled DHT support, sets EVER_DHT_CAPABLE to false.
     */
    private static void assignDHTNode() {
        
        //make sure that the node has had the time to try to connect as an ultrapeer
        Assert.that((DHTSettings.MIN_DHT_INITIAL_UPTIME.getValue() > 
                     UltrapeerSettings.MIN_CONNECT_TIME.getValue()), "Wrong minimum initial uptime");
        
        if (DHTSettings.DISABLE_DHT_USER.getValue() || DHTSettings.DISABLE_DHT_NETWORK.getValue()) {
            DHTSettings.DHT_CAPABLE.setValue(false);
            RouterService.shutdownDHT();
            return;
        }

        boolean isActiveDHTCapable = 
            (_isHardcoreCapable &&
            //AND is my average uptime AND current uptime high enough?
            (ApplicationSettings.AVERAGE_UPTIME.getValue() >= DHTSettings.MIN_DHT_AVG_UPTIME.getValue() &&
             _currentUptime >= DHTSettings.MIN_DHT_INITIAL_UPTIME.getValue()));
                     
        //don't give active capability to ultrapeers
        if(DHTSettings.EXCLUDE_ULTRAPEERS.getValue() &&
                (RouterService.isSupernode() || _willTryToBeUltrapeer)){
            isActiveDHTCapable = false;
        }
                     
        if(LOG.isDebugEnabled()) {
            LOG.debug("Node is "+(isActiveDHTCapable?"":"NOT")+" DHT capable");
        }

        DHTSettings.DHT_CAPABLE.setValue(isActiveDHTCapable);
        
        //Node is DHT capable AND is not an ultrapeer AND not allready trying to connect as UP
        if ((isActiveDHTCapable && acceptDHTNode()) || DHTSettings.FORCE_DHT_CONNECT.getValue()) {
            Runnable dhtInitializer = 
                new Runnable() {
                    public void run() {
                        RouterService.startDHT(true);
                    }
                };
            ThreadFactory.startThread(dhtInitializer, "dhtInitializeThread");
            return;
        } 

        // for now, disconnect node as soon as not anymore DHT capable
        if(RouterService.isActiveDHTNode()) { 
            RouterService.shutdownDHT();
        }
    }
    
    private static boolean acceptDHTNode() {
        return (Math.random() < DHTSettings.DHT_ACCEPT_PROBABILITY.getValue());
    }
}
