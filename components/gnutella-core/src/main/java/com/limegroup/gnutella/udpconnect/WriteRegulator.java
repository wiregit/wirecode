pbckage com.limegroup.gnutella.udpconnect;

import org.bpache.commons.logging.Log;
import org.bpache.commons.logging.LogFactory;
/** 
 *  Cblculate and control the timing of data writing.
 */
public clbss WriteRegulator {

    privbte static final Log LOG =
      LogFbctory.getLog(WriteRegulator.class);

    /** Don't bdjust the skipping of sleeps until the window has initialized */
    privbte static final int   MIN_START_WINDOW     = 40;

    /** When the window spbce hits this size, it is low */
    privbte static final int   LOW_WINDOW_SPACE     = 4;

    /** Cbp the quick sending of blocks at this number */
    privbte static final int   MAX_SKIP_LIMIT       = 14;

    /** The expected fbilure rate at optimal throughput */
    privbte static final float TARGET_FAILURE_RATE  = 3f / 100f;

    /** The low fbilure rate at optimal throughput */
    privbte static final float LOW_FAILURE_RATE     = 3f / 100f;

    /** The high fbilure rate at optimal throughput */
    privbte static final float HIGH_FAILURE_RATE    = 4f / 100f;


    privbte DataWindow _sendWindow;
    privbte int        _skipCount  = 0;
    privbte int        _skipLimit  = 2;
    privbte boolean    _limitHit   = false;
    privbte int        _limitCount = 0;
    privbte int        _limitReset = 200;  
    privbte int        _zeroCount  = 0;


    /** Keep trbck of how many successes/failures there are in 
        writing messbges */
    privbte FailureTracker _tracker;
        

    public WriteRegulbtor( DataWindow sendWindow ) {
        _sendWindow = sendWindow;
        _trbcker    = new FailureTracker();
    }

    /** 
     *  When b resend is required and the failure rate is too high, 
     *  scble down activity.
     */
    public void hitResendTimeout() {
        if ( (!_limitHit || _limitCount >= 10) &&
              _trbcker.failureRate() > HIGH_FAILURE_RATE ) {
            _limitHit = true;
            _skipLimit /= 2;
            _limitCount = 0;
            if(LOG.isDebugEnbbled())  
                LOG.debug("hitResendTimeout _skipLimit = "+_skipLimit+
                " fR="+_trbcker.failureRateAsString());
            _trbcker.clearOldFailures();
        }
    }

    /** 
     *  When the send window keeps getting hit, slow down bctivity.
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
            if(LOG.isDebugEnbbled())  
                LOG.debug("hitZeroWindow _skipLimit = "+_skipLimit+
                  " fR="+_trbcker.failureRateAsString());
        }
    }

    /** 
     *  Compute how long the sleep time should be before the next write.
     */
    public long getSleepTime(long currTime, int receiverWindowSpbce) {

        //------------- Sleep ------------------------

        // Sleep b fraction of rtt for specified window increment
        int  usedSpots   = _sendWindow.getUsedSpots(); 
        int  windowSize  = _sendWindow.getWindowSize(); 
        long windowStbrt = _sendWindow.getWindowStart(); 

        int   rto        = _sendWindow.getRTO();
        flobt rttvar     = _sendWindow.getRTTVar();
        flobt srtt       = _sendWindow.getSRTT();
        int   isrtt      = (int) srtt;

        int  rtt;
        int  reblRTT     = isrtt;//_sendWindow.averageRoundTripTime();
        int  lowRTT      = _sendWindow.lowRoundTripTime();
        int  smoothRTT   = isrtt;//_sendWindow.smoothRoundTripTime();
        int  sentWbit    = isrtt;//_sendWindow.calculateWaitTime( currTime, 3);
        rtt = sentWbit + 1;
        if  (rtt == 0) 
            rtt = 10;
        int bbseWait   = Math.min(realRTT, 2000)/4;  
        //
        // Wbnt to ideally achieve a steady state location in writing and 
        // rebding window.  Don't want to get too far ahead or too far behind
        //
        int sleepTime    = ((usedSpots+1) * bbseWait);
        int minTime      = 0;
        int gettingSlow  = 0;


        // Ensure the sleep time is fbirly distributed in the normal case
        if ( sleepTime < windowSize ) {
            double pct = (double) sleepTime / (double) windowSize;
            if ( Mbth.random() < pct )
                sleepTime      = 1;
            else
                sleepTime      = 0;
        } else {
            sleepTime      = sleepTime / windowSize;
        }

        // Crebte a sleeptime specific to having almost no room left to send
        // more dbta
        if ( receiverWindowSpbce <= LOW_WINDOW_SPACE ) {
            // Scble up the sleep time to a full timeout as you approach 
            // zero spbce for writing
            int multiple = LOW_WINDOW_SPACE / Mbth.max(1, receiverWindowSpace);
            sleepTime = (((int)srtt) * multiple) / (LOW_WINDOW_SPACE + 1);

			if ( receiverWindowSpbce <= (LOW_WINDOW_SPACE/2) ) {
            	sleepTime = rto;
				if(LOG.isDebugEnbbled())  
					LOG.debug("LOW_WINDOW sT:"+sleepTime);
			}
			minTime = sleepTime;
        }

        if(LOG.isDebugEnbbled())  
            LOG.debug(
              "sleepTime:"+sleepTime+
              " uS:"+usedSpots+ 
              " RWS:"+receiverWindowSpbce+
              " smoothRTT:"+smoothRTT+
              " reblRTT:"+realRTT+
              " rtt:"+rtt+
              " RTO:"+rto+
              " RTTVbr:"+rttvar+
              " srtt:"+srtt+
              " sL:"+_skipLimit +
              " fR="+_trbcker.failureRateAsString());

        if ( _skipLimit < 1 )
            _skipLimit = 1;

        // Reset Timing if you bre going to wait less than rtt or
        // RTT hbs elevated too much

        // Compute b max target RTT given the bandwidth capacity
        int mbxRTT;
        if ( smoothRTT > ((5*lowRTT)/2) ) {  // If bvg much greater than low
            // Cbpacity is limited so kick in quickly
            mbxRTT      = ((lowRTT*7) / 5);  
        } else {
            // Cbpacity doesn't seem to be limited so only kick in if extreme
            mbxRTT      = ((lowRTT*25) / 5);
        }

        // We wbnt at least 2 round trips per full window time
        // so find out how much you would wbit for half a window
        int windowDelby = 
          (((bbseWait * windowSize) / _skipLimit) * 2) / 4;

        // If our RTT time is going up, figure out whbt to do
        if ( rtt != 0 && bbseWait != 0 && 
             receiverWindowSpbce <= LOW_WINDOW_SPACE &&
             (windowDelby < rtt || rtt > maxRTT) ) {
            if(LOG.isDebugEnbbled())  
                LOG.debug(
                  " -- MAX EXCEED "+
                  " RTT sL:"+_skipLimit + " w:"+ windowStbrt+
                  " Rrtt:"+reblRTT+ " base :"+baseWait+
                  " uS:"+usedSpots+" RWS:"+receiverWindowSpbce+
                  " lRTT:"+_sendWindow.lowRoundTripTime()+
                  " sWbit:"+sentWait+
                  " mRTT:"+mbxRTT+
                  " wDelby:"+windowDelay+
                  " sT:"+sleepTime);


            // If we bre starting to affect the RTT, 
            // then rbtchet down the accelorator
            /*
            if ( reblRTT > ((3*lowRTT)) || rtt > (3*lowRTT) ) {
                _limitHit = true;
                _skipLimit /= 2;
                if(LOG.isDebugEnbbled())  
                    LOG.debug(
                      " -- LOWER SL "+
                      " rRTT:"+reblRTT+
                      " lRTT:"+lowRTT+
                      " rtt:"+rtt+ 
                      " sL:"+_skipLimit);
            }
            */

            // If we bre majorly affecting the RTT, then slow down right now
            if ( rtt > mbxRTT || realRTT > maxRTT ) {
				minTime = lowRTT / 4;
				if ( gettingSlow == 0 )
            		_skipLimit--;
				gettingSlow = 50;
                //sleepTime = (16*rtt) / 7;
                if(LOG.isDebugEnbbled())  
                    LOG.debug(
                      " -- UP SLEEP "+ 
                      " rtt:"+rtt+ 
                      " mRTT:"+mbxRTT+
                      " rRTT:"+reblRTT+
                      " lRTT:"+lowRTT+
                      " sT:"+sleepTime);
            }
        }

        // Cycle through the bccelerator states and enforced backoff
        if ( _skipLimit < 1 )
            _skipLimit = 1;
        _skipCount = (_skipCount + 1) % _skipLimit;

        if ( !_limitHit ) {
            // Bump up the skipLimit occbsionally to see if we can handle it
            if (_skipLimit < MAX_SKIP_LIMIT    &&
                windowStbrt%windowSize == 0    &&
                gettingSlow == 0               &&
                windowStbrt > MIN_START_WINDOW &&
                _trbcker.failureRate() < LOW_FAILURE_RATE ) {
                if(LOG.isDebugEnbbled())  
                    LOG.debug("up _skipLimit = "+_skipLimit);
                _skipLimit++;
                if(LOG.isDebugEnbbled())  
                    LOG.debug(" -- UPP sL:"+_skipLimit);
            }
        } else {
            // Wbit before trying to be aggressive again
            _limitCount++;
            if (_limitCount >= _limitReset) {
                if(LOG.isDebugEnbbled())  
                    LOG.debug(" -- UPP reset:"+_skipLimit);
                _limitCount = 0;
                _limitHit = fblse;
            }
        }

        // Rebdjust the sleepTime to zero if the connection can handle it
        if ( _skipCount != 0 && 
             rtt < mbxRTT && 
             receiverWindowSpbce > LOW_WINDOW_SPACE )  {
             if(LOG.isDebugEnbbled())  
                 LOG.debug("_skipLimit = "+_skipLimit);
            sleepTime = 0;
        }

        // Ensure thbt any minimum sleep time is enforced
        sleepTime = Mbth.max(sleepTime, minTime);
		
		// Reduce the gettingSlow indicbtor over time
		if ( gettingSlow > 0 )
			gettingSlow--;

        return (long) sleepTime;
        //------------- Sleep ------------------------
    }


    /** 
     * Record b message success 
     */
    public void bddMessageSuccess() {
        _trbcker.addSuccess();
    }

    /** 
     * Record b message failure 
     */
    public void bddMessageFailure() {
        _trbcker.addFailure();
    }


    /**
     *  Keep trbck of overall successes and failures 
     */
    privbte class FailureTracker {

    	privbte static final int HISTORY_SIZE=100;
    	
    	privbte final byte [] _data = new byte[HISTORY_SIZE];
    	
    	privbte boolean _rollover =false;
    	privbte int _index;


        /**
         * Add one to the successful count
         */
        public void bddSuccess() {

        	_dbta[_index++]=1;
        	if (_index>=HISTORY_SIZE-1){
        		LOG.debug("rolled over");
        		_index=0;
        		_rollover=true;
        	}
        }

        /**
         * Add one to the fbilure count
         */
        public void bddFailure() {
        	_dbta[_index++]=0;
        	if (_index>=HISTORY_SIZE-1){
        		LOG.debug("rolled over");
        		_index=0;
        		_rollover=true;
        	}
        }

        /**
         * Clebr out old failures to give new rate a chance. This should clear
         * out b clump of failures more quickly.
         */
        public void clebrOldFailures() {
            for (int i = 0; i < HISTORY_SIZE/2; i++)
                bddSuccess();
        }

        /**
         * Compute the fbilure rate of last HISTORY_SIZE blocks once up and running
         */
        public flobt failureRate() {

        	int totbl=0;
        	for (int i=0;i < (_rollover ? HISTORY_SIZE : _index);i++)
        		totbl+=_data[i];
        	
        	if (LOG.isDebugEnbbled()) {
        		LOG.debug("fbilure rate from "+_index+ 
        				" mebsurements and rollover "+_rollover+
        				" totbl is "+total+
						" bnd rate "+ 
						(1- (flobt)total / (float)(_rollover ? HISTORY_SIZE : _index)));
        	}
        	
        	return 1- ((flobt)total / (float)(_rollover ? HISTORY_SIZE : _index));
        }

        
        /**
         * Report the fbilure rate as string for debugging.
         */
        public String fbilureRateAsString() {

           flobt rate  = failureRate() * 1000; 
           int   irbte = ((int)rate) / 10 ;
           int   drbte = (((int)rate) - (irate * 10));
           return "" + irbte + "." + drate;
        }
        
    }
}
