package com.limegroup.gnutella.udpconnect;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/** 
 *  Calculate and control the timing of data writing.
 */
public class WriteRegulator {

    private static final Log LOG =
      LogFactory.getLog(WriteRegulator.class);

    /** Don't adjust the skipping of sleeps until the window has initialized */
    private static final int MIN_START_WINDOW = 40;

    /** When the window space hits this size, it is low */
    private static final int LOW_WINDOW_SPACE = 6;

    private DataWindow _sendWindow;
    private int        _skipCount  = 0;
    private int        _skipLimit  = 2;
    private boolean    _limitHit   = false;
    private int        _limitCount = 0;
    private int        _limitReset = 400;

    public WriteRegulator( DataWindow sendWindow ) {
        _sendWindow = sendWindow;
    }

    /** 
     *  When a resend is required, scale down activity.
     */
    public void hitResendTimeout() {
        if ( !_limitHit || _limitCount >= 2 ) { 
            _limitHit = true;
            _skipLimit /= 2;
            _limitCount = 0;
        }
    }

    /** 
     *  When a resend is required, scale down activity.
     */
    public void hitZeroWindow() {
        if ( !_limitHit || _limitCount >= 2 ) { 
            _limitHit = true;
            _skipLimit /= 2;
            _limitCount = 0;
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
            if (_skipLimit < 50 &&
                windowStart%windowSize == 0  &&
                windowStart > MIN_START_WINDOW) {
                _skipLimit++;
            if(LOG.isDebugEnabled())  
                LOG.debug(" -- UPP sL:"+_skipLimit);
            }
        } else {
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
            sleepTime = 0;
        }

        return (long) sleepTime;
        //------------- Sleep ------------------------
    }
}
