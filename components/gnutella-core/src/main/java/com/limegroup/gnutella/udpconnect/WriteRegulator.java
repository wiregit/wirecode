package com.limegroup.gnutella.udpconnect;

/** 
 *  Calculate and control the timing of data writing.
 */
public class WriteRegulator {

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
    public long getSleepTime(long currTime) {

        //------------- Sleep ------------------------

        // Sleep a fraction of rtt for specified window increment
        int usedSpots  = _sendWindow.getUsedSpots(); 
        int windowSize = _sendWindow.getWindowSize(); 
        int rtt;
        int realRTT    = _sendWindow.averageRoundTripTime();
        int lowRTT     = _sendWindow.lowRoundTripTime();
        int smoothRTT  = _sendWindow.smoothRoundTripTime();
        int sentWait   = _sendWindow.calculateWaitTime( currTime, 3);
        //rtt = sentWait + lowRTT;
        rtt = sentWait + 1;
        if  (rtt == 0) 
            rtt = 10;
        int baseWait   = Math.min(realRTT, 2000)/3;  
        int sleepTime  = ((usedSpots) * baseWait);

        if ( sleepTime < windowSize ) 
            sleepTime      = 1;
        else 
            sleepTime      = sleepTime / windowSize;

        int rto        = _sendWindow.getRTO();
System.out.println(
"sleepTime:"+sleepTime+
" uS:"+usedSpots+
" smoothRTT:"+smoothRTT+
" realRTT:"+realRTT+
" rtt:"+rtt+
" RTO:"+rto+
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
          (((baseWait * _sendWindow.getWindowSize()) / _skipLimit) * 2) / 4;
        if ( rtt != 0 && baseWait != 0 && 
             (windowDelay < rtt || rtt > maxRTT) ) {
//if ( _sendWindow.getWindowStart() % 5 == 0 )
System.out.println("RTT sL:"+_skipLimit + " w:"+ _sendWindow.getWindowStart()+
" rtt:"+realRTT+ " base :"+baseWait+" uS:"+usedSpots+
" lRTT:"+_sendWindow.lowRoundTripTime()+
" sWait:"+sentWait+
" mRTT:"+maxRTT+
" wDelay:"+windowDelay+
" sT:"+sleepTime);
            if ( realRTT > ((3*lowRTT)) || rtt > (3*lowRTT) ) {
                _limitHit = true;
                _skipLimit /= 2;
            }
            if ( rtt > maxRTT || realRTT > maxRTT ) 
                sleepTime = (16*rtt) / 7;
        }

        if ( _skipLimit < 1 )
            _skipLimit = 1;
        _skipCount = (_skipCount + 1) % _skipLimit;
        if ( !_limitHit ) {
            if (_sendWindow.getWindowStart()%_sendWindow.getWindowSize() == 0) {
                _skipLimit++;
System.out.println("UPP sL:"+_skipLimit);
            }
        } else {
            _limitCount++;
            if (_limitCount >= _limitReset) {
System.out.println("UPP reset:"+_skipLimit);
                _limitCount = 0;
                _limitHit = false;
            }
        }

System.out.println("sleepTime: "+sleepTime);
        return (long) sleepTime;
        //------------- Sleep ------------------------
    }
}
