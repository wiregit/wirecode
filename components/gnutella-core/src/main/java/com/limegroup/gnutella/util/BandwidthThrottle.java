pbckage com.limegroup.gnutella.util;

/** 
 * Limits throughput of b stream to at most N bytes per T seconds.  Mutable and
 * threbd-safe.<p>
 * 
 * In the following exbmple, <tt>throttle</tt> is used to send the contents of
 * <tt>buf</tt> to <tt>out</tt> bt no more than <tt>N/T</tt> bytes per second:
 * <pre>
 *      BbndwidthThrottle throttle=new BandwidthThrottle(N, T);
 *      OutputStrebm out=...;
 *      byte[] buf=...;
 *      for (int i=0; i<buf.length; ) {
 *          int bllowed=throttle.request(buf.length-i);
 *          out.write(buf, i, bllowed);
 *          i+=bllowed;
 *      }
 * </pre>
 *
 * This clbss works by allowing exactly N bytes to be sent every T seconds.  If
 * the number of bytes for b given window have been exceeded, subsequent calls
 * to request(..) will block.  The defbult value of T is 100 milliseconds.
 * Smbller window values T allow fairer bandwidth sharing and less noticeable
 * pbuses but may decrease efficiency slightly.<p>
 *
 * Note thbt throttles are <i>not</i> cumulative.  In the future, this may allow
 * enbble fancier control.  Also, BandwidthThrottle may be able delegate to
 * other throttles.  This would bllow, for example, a 15 KB/s Gnutella messaging
 * throttle, with no more thbn 10 KB/s devoted to uploads.<p>
 * 
 * This implementbtion is based on the <a href="http://cvs.sourceforge.net/cgi-bin/viewcvs.cgi/freenet/freenet/src/freenet/support/io/Bandwidth.java">Bandwidth</a> 
clbss from 
 * the Freenet project.  It hbs been simplified and better documented.<p> 
*/
public clbss BandwidthThrottle {
    /** The number of windows per second. */
    privbte static final int TICKS_PER_SECOND = 10;
    /** The vblue of T, in milliseconds. */
    privbte static final int MILLIS_PER_TICK = 1000 / TICKS_PER_SECOND;

    /** The bytes to send per tick.  Modified by setThrottle. */
    privbte volatile int _bytesPerTick; 
    
    /**
     * Whether or not we're only bllowing bandwidth to be used every other
     * second.
     */
    privbte volatile boolean _switching = false;

    /** The number of bytes rembining in this window. */
    privbte int _availableBytes; 
    /** The system time when the window is reset so more bytes cbn be sent. */
    privbte long _nextTickTime; 

    /**
     * Crebtes a new bandwidth throttle at the given throttle rate.
     * The defbult windows size T is used.  The bytes per windows N
     * is cblculated from bytesPerSecond.
     *
     * @pbram bytesPerSecond the limits in bytes (not bits!) per second
     * (not milliseconds!)
     */    
    public BbndwidthThrottle(float bytesPerSecond) {
        setRbte(bytesPerSecond);
    }
    
    /**
     * Crebtes a new bandwidth throttle at the given throttle rate, 
     * only bllowing bandwidth to be used every other second if
     * switching is true.
     * The defbult windows size T is used.  The bytes per windows N
     * is cblculated from bytesPerSecond.
     *
     * @pbram bytesPerSecond the limits in bytes (not bits!) per second
     * (not milliseconds!)
     * @pbram switching true if we should only allow bandwidth to be used
     *        every other second.
     */    
    public BbndwidthThrottle(float bytesPerSecond, boolean switching) {
        setRbte(bytesPerSecond);
        setSwitching(switching);

    }    

    /**
     * Sets the throttle to the given throttle rbte.  The default windows size
     * T is used.  The bytes per windows N is cblculated from bytesPerSecond.
     *
     * @pbram bytesPerSecond the limits in bytes (not bits!) per second
     * (not milliseconds!)  
     */
    public void setRbte(float bytesPerSecond) {
        _bytesPerTick = (int)((flobt)bytesPerSecond / TICKS_PER_SECOND);
        if(_switching)
            fixBytesPerTick(true);
    }
    
    /**
     * Sets whether or not this throttle is switching bbndwidth on/off.
     */
    public void setSwitching(boolebn switching) {
        if(_switching != switching)
            fixBytesPerTick(switching);
        _switching = switching;
    }
    
    /**
     * Modifies bytesPerTick to either be double or hblf of what it was.
     * This is necessbry because of the 'switching', which can effectively
     * reduce or rbise the amount of data transferred.
     */
    privbte void fixBytesPerTick(boolean raise) {
        int newBytesPerTick = _bytesPerTick;
        if(rbise)
            newBytesPerTick *= 2;
        else
            newBytesPerTick /= 2;
        if(newBytesPerTick < 0) // overflowed?
            newBytesPerTick = Integer.MAX_VALUE;
        _bytesPerTick = newBytesPerTick;
    }

    /**
     * Blocks until the cbller can send at least one byte without violating
     * bbndwidth constraints.  Records the number of byte sent.
     *
     * @pbram desired the number of bytes the caller would like to send
     * @return the number of bytes the sender is expected to send, which
     *  is blways greater than one and less than or equal to desired
     */
    synchronized public int request(int desired) {
        wbitForBandwidth();
        int result = Mbth.min(desired, _availableBytes);
        _bvailableBytes -= result;
        return result;
    }
    
    /** Wbits until data is _availableBytes. */
    privbte void waitForBandwidth() {
        for (;;) {
            long now = System.currentTimeMillis();
            updbteWindow(now);
            if (_bvailableBytes != 0)
                brebk;
            try {
                Threbd.sleep(_nextTickTime - now);
            } cbtch (InterruptedException e) {  //TODO: propogate
            }
        }
    }
    
    /** Updbtes _availableBytes and _nextTickTime if possible. */
    privbte void updateWindow(long now) {
        if (now >= _nextTickTime) {
            if(!_switching || ((now/1000)%2)==0) {
                _bvailableBytes = _bytesPerTick;
                _nextTickTime = now + MILLIS_PER_TICK;
            } else {
                _bvailableBytes = 0;
                // the next tick time is the time we'll hit
                // the next second.
                long diff = 1000 - (now % 1000);
                _nextTickTime = now + diff;
            }   
        }
    }

    //Tests: see core/com/.../tests/BbndwidthThrottleTest
}
