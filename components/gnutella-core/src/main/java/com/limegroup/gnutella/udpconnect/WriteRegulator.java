package com.limegroup.gnutella.udpconnect;

import com.sun.java.util.collections.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
/** 
 *  Calculate and control the timing of data writing.
 */
public class WriteRegulator {

    private static final Log LOG =
      LogFactory.getLog(WriteRegulator.class);

    /** Don't adjust the skipping of sleeps until the window has initialized */
    private static final int   MIN_START_WINDOW     = 40;

    /** When the window space hits this size, it is low */
    private static final int   LOW_WINDOW_SPACE     = 6;

    /** Cap the quick sending of blocks at this number */
    private static final int   MAX_SKIP_LIMIT       = 14;

    /** The expected failure rate at optimal throughput */
    private static final float TARGET_FAILURE_RATE  = 3f / 100f;


    private DataWindow _sendWindow;
    private int        _skipCount  = 0;
    private int        _skipLimit  = 2;
    private boolean    _limitHit   = false;
    private int        _limitCount = 0;
    private int        _limitReset = 200;  
    private int        _zeroCount  = 0;


    /** Keep track of how many successes/failures there are in 
        writing messages */
    private FailureTracker _tracker;
        

    public WriteRegulator( DataWindow sendWindow ) {
        _sendWindow = sendWindow;
        _tracker    = new FailureTracker();
    }

    /** 
     *  When a resend is required and the failure rate is too high, 
     *  scale down activity.
     */
    public void hitResendTimeout() {
        if ( (!_limitHit || _limitCount >= 10) &&
              _tracker.failureRate() > TARGET_FAILURE_RATE ) {
            _limitHit = true;
            _skipLimit /= 2;
            _limitCount = 0;
            _tracker.clearOldFailures();
System.out.println("hitResendTimeout _skipLimit = "+_skipLimit);
        }
    }

    /** 
     *  When the send window keeps getting hit, slow down activity.
     */
    public void hitZeroWindow() {
        _zeroCount++;
        if ( (!_limitHit || _limitCount >= 10) && _zeroCount > 4) { 
            // Doing nothing for now since this is irrelevent to the skipping
            //

            //_limitHit = true;
            //_skipLimit /= 2;
            //_limitCount = 0;
            _zeroCount = 0;
System.out.println("hitZeroWindow _skipLimit = "+_skipLimit);
        }
    }

    /** 
     *  Compute how long the sleep time should be before the next write.
     */
    public long getSleepTime(long currTime, int receiverWindowSpace) {

        //------------- Sleep ------------------------

        // Sleep a fraction of rtt for specified window increment
        int  usedSpots   = _sendWindow.getUsedSpots(); 
        int  windowSize  = _sendWindow.getWindowSize(); 
        long windowStart = _sendWindow.getWindowStart(); 
        int  rtt;
        int  realRTT     = _sendWindow.averageRoundTripTime();
        int  lowRTT      = _sendWindow.lowRoundTripTime();
        int  smoothRTT   = _sendWindow.smoothRoundTripTime();
        int  sentWait    = _sendWindow.calculateWaitTime( currTime, 3);
        //rtt = sentWait + lowRTT;
        rtt = sentWait + 1;
        if  (rtt == 0) 
            rtt = 10;
        int baseWait   = Math.min(realRTT, 2000)/3;  
        //
        // Want to ideally achieve a steady state location in writing and 
        // reading window.  Don't want to get too far ahead or too far behind
        //
        int sleepTime  = ((usedSpots+1) * baseWait);

        if ( receiverWindowSpace <= LOW_WINDOW_SPACE ) {
            sleepTime += 1;
            sleepTime = 7 * (LOW_WINDOW_SPACE + 1 - receiverWindowSpace) *
              sleepTime / 5;  
        }

        // Ensure the sleep time is fairly distributed
        if ( sleepTime < windowSize ) {
            double pct = (double) sleepTime / (double) windowSize;
            if ( Math.random() < pct )
                sleepTime      = 1;
            else
                sleepTime      = 0;
        } else {
            sleepTime      = sleepTime / windowSize;
        }

        int   rto        = _sendWindow.getRTO();
        float rttvar     = _sendWindow.getRTTVar();
        float srtt       = _sendWindow.getSRTT();

        if(LOG.isDebugEnabled())  
            LOG.debug(
              "sleepTime:"+sleepTime+
              " uS:"+usedSpots+
              " smoothRTT:"+smoothRTT+
              " realRTT:"+realRTT+
              " rtt:"+rtt+
              " RTO:"+rto+
              " RTTVar:"+rttvar+
              " srtt:"+srtt+
              " sL:"+_skipLimit);

        if ( _skipLimit < 1 )
            _skipLimit = 1;

        // Reset Timing if you are going to wait less than rtt or
        // RTT has elevated too much

        // Compute a max target RTT given the spikyness of traffic
        int maxRTT;
        if ( smoothRTT > ((5*lowRTT)/2) ) {
            maxRTT      = ((_sendWindow.lowRoundTripTime()*7) / 5);
        } else {
            maxRTT      = ((_sendWindow.lowRoundTripTime()*15) / 5);
        }
        int windowDelay = 
          (((baseWait * windowSize) / _skipLimit) * 2) / 4;

        // If our RTT time is going up, figure out what to do
        if ( rtt != 0 && baseWait != 0 && 
             (windowDelay < rtt || rtt > maxRTT) ) {
            if(LOG.isDebugEnabled())  
                LOG.debug(
                  " -- MAX EXCEED "+
                  " RTT sL:"+_skipLimit + " w:"+ windowStart+
                  " Rrtt:"+realRTT+ " base :"+baseWait+" uS:"+usedSpots+
                  " lRTT:"+_sendWindow.lowRoundTripTime()+
                  " sWait:"+sentWait+
                  " mRTT:"+maxRTT+
                  " wDelay:"+windowDelay+
                  " sT:"+sleepTime);

            // If we are starting to affect the RTT, 
            // then ratchet down the accelorator
            if ( realRTT > ((3*lowRTT)) || rtt > (3*lowRTT) ) {
                _limitHit = true;
                _skipLimit /= 2;
                if(LOG.isDebugEnabled())  
                    LOG.debug(
                      " -- LOWER SL "+
                      " rRTT:"+realRTT+
                      " lRTT:"+lowRTT+
                      " rtt:"+rtt+ 
                      " sL:"+_skipLimit);
            }

            // If we are majorly affecting the RTT, then slow down right now
            if ( rtt > maxRTT || realRTT > maxRTT ) {
                sleepTime = (16*rtt) / 7;
                if(LOG.isDebugEnabled())  
                    LOG.debug(
                      " -- UP SLEEP "+ 
                      " rtt:"+rtt+ 
                      " mRTT:"+maxRTT+
                      " rRTT:"+realRTT+
                      " sT:"+sleepTime);
            }
        }

        // Cycle through the accelerator states and enforced backoff
        if ( _skipLimit < 1 )
            _skipLimit = 1;
        _skipCount = (_skipCount + 1) % _skipLimit;

        if ( !_limitHit ) {
            // Bump up the skipLimit occasionally to see if we can handle it
            if (_skipLimit < MAX_SKIP_LIMIT    &&
                windowStart%windowSize == 0    &&
                windowStart > MIN_START_WINDOW &&
                _tracker.failureRate() < TARGET_FAILURE_RATE ) {
System.out.println("up _skipLimit = "+_skipLimit);
                _skipLimit++;
            if(LOG.isDebugEnabled())  
                LOG.debug(" -- UPP sL:"+_skipLimit);
            }
        } else {
            // Wait before trying to be aggressive again
            _limitCount++;
            if (_limitCount >= _limitReset) {
                if(LOG.isDebugEnabled())  
                    LOG.debug(" -- UPP reset:"+_skipLimit);
                _limitCount = 0;
                _limitHit = false;
            }
        }

        // Readjust the sleepTime to zero if the connection can handle it
        if ( _skipCount != 0 && 
             rtt < maxRTT && 
             receiverWindowSpace > LOW_WINDOW_SPACE )  {
System.out.println("_skipLimit = "+_skipLimit);
            sleepTime = 0;
        }

        return (long) sleepTime;
        //------------- Sleep ------------------------
    }


    /** 
     * Record a message success 
     */
    public void addMessageSuccess() {
        _tracker.addSuccess();
    }

    /** 
     * Record a message failure 
     */
    public void addMessageFailure() {
        _tracker.addFailure();
    }

    /**
     *  Keep track of successes and failures at a discrete level
     */
    class FailureBlock {

        /** How many entries are in the block */
        public int count;

        /** How many successes are in the block */
        public int success;

        /** How many failures are in the block */
        public int failure;

        public FailureBlock() {
            count   = 0;
            success = 0;
            failure = 0;
        }
    }

    /**
     *  Keep track of overall successes and failures 
     */
    class FailureTracker {

        /** The number of discrete success/failure blocks to keep */
        private static final int MAX_BLOCKS       = 5;

        /** The number of discrete success/failures per block */
        private static final int NUMBER_PER_BLOCK = 20;


        /** The list of success/failures stored as discrete aging blocks */
        private ArrayList blocks = new ArrayList(MAX_BLOCKS+1);

        /**
         * Add one to the successful count
         */
        public void addSuccess() {
            FailureBlock block = ensureSpace(); 

            block.count++;
            block.success++;
        }

        /**
         * Add one to the failure count
         */
        public void addFailure() {
            FailureBlock block = ensureSpace(); 

            block.count++;
            block.failure++;
        }

        /**
         * Clear out old failures to give new rate a chance. This should clear
         * out a clump of failures more quickly.
         */
        public void clearOldFailures() {
            for (int i = 0; i < 50; i++)
                addSuccess();
        }

        /**
         * Compute the failure rate of last 100 blocks once up and running
         */
        public float failureRate() {
            FailureBlock block;
            int          count   = 0; 
            int          failure = 0; 
            
            for (int i = 0; i < blocks.size(); i++) {
                block = (FailureBlock) blocks.get(i);
                count   += block.count;
                failure += block.failure;
            }

            if ( count == 0 )
                count = 1;

            return ((float) failure) / ((float) count);
        }

        /**
         * Make sure there is space for an addition and 
         * return the active block
         */
        private FailureBlock ensureSpace() {
            FailureBlock block;

            // If nothing, initialize
            if ( blocks.size() == 0 ) {
                block = new FailureBlock();
                blocks.add(0, block);
                return block;
            }

            // Check latest block for room
            block = (FailureBlock) blocks.get(0);

            // Add new block if current is full
            if ( block.count >= NUMBER_PER_BLOCK ) {
                block = new FailureBlock();
                blocks.add(0, block);

                // Remove oldest block if too many
                if ( blocks.size() > MAX_BLOCKS )
                    blocks.remove(MAX_BLOCKS);
            }

            return block;
        }
    }
}
