package com.limegroup.gnutella.util;

/** 
 * Limits throughput of a stream to at most N bytes per T seconds.  Mutable and
 * thread-safe.<p>
 * 
 * In the following example, <tt>throttle</tt> is used to send the contents of
 * <tt>buf</tt> to <tt>out</tt> at no more than <tt>N/T</tt> bytes per second:
 * <pre>
 *      BandwidthThrottle throttle=new BandwidthThrottle(N, T);
 *      OutputStream out=...;
 *      byte[] buf=...;
 *      for (int i=0; i<buf.length; ) {
 *          int allowed=throttle.request(buf.length-i);
 *          out.write(buf, i, allowed);
 *          i+=allowed;
 *      }
 * </pre>
 *
 * This class works by allowing exactly N bytes to be sent every T seconds.  If
 * the number of bytes for a given window have been exceeded, subsequent calls
 * to request(..) will block.  The default value of T is 100 milliseconds.
 * Smaller window values T allow fairer bandwidth sharing and less noticeable
 * pauses but may decrease efficiency slightly.<p>
 *
 * Note that throttles are <i>not</i> cumulative.  In the future, this may allow
 * enable fancier control.  Also, BandwidthThrottle may be able delegate to
 * other throttles.  This would allow, for example, a 15 KB/s Gnutella messaging
 * throttle, with no more than 10 KB/s devoted to uploads.<p>
 * 
 * This implementation is based on the <a href="http://cvs.sourceforge.net/cgi-bin/viewcvs.cgi/freenet/freenet/src/freenet/support/io/Bandwidth.java">Bandwidth</a> 
class from 
 * the Freenet project.  It has been simplified and better documented.<p> 
*/
public class BandwidthThrottle {
    /** The number of windows per second. */
    private static final int TICKS_PER_SECOND = 10;
    /** The value of T, in milliseconds. */
    private static final int MILLIS_PER_TICK = 1000 / TICKS_PER_SECOND;

    /** The bytes to send per tick.  Modified by setThrottle. */
    private volatile int _bytesPerTick; 

    /** The number of bytes remaining in this window. */
    private int _availableBytes; 
    /** The system time when the window is reset so more bytes can be sent. */
    private long _nextTickTime; 

    /**
     * Creates a new bandwidth throttle at the given throttle rate.
     * The default windows size T is used.  The bytes per windows N
     * is calculated from bytesPerSecond.
     *
     * @param bytesPerSecond the limits in bytes (not bits!) per second
     * (not milliseconds!)
     */    
    public BandwidthThrottle(float bytesPerSecond) {
        setRate(bytesPerSecond);
    }

    /**
     * Sets the throttle to the given throttle rate.  The default windows size
     * T is used.  The bytes per windows N is calculated from bytesPerSecond.
     *
     * @param bytesPerSecond the limits in bytes (not bits!) per second
     * (not milliseconds!)  
     */
    public void setRate(float bytesPerSecond) {
        _bytesPerTick = (int)((float)bytesPerSecond / TICKS_PER_SECOND);
    }

    /**
     * Blocks until the caller can send at least one byte without violating
     * bandwidth constraints.  Records the number of byte sent.
     *
     * @param desired the number of bytes the caller would like to send
     * @return the number of bytes the sender is expected to send, which
     *  is always greater than one and less than or equal to desired
     */
    synchronized public int request(int desired) {
        waitForBandwidth();
        int result = Math.min(desired, _availableBytes);
        _availableBytes -= result;
        return result;
    }
    
    /** Waits until data is _availableBytes. */
    private void waitForBandwidth() {
        for (;;) {
            long now = System.currentTimeMillis();
            updateWindow(now);
            if (_availableBytes != 0)
                break;
            try {
                Thread.currentThread().sleep(_nextTickTime - now);
            } catch (InterruptedException e) {  //TODO: propogate
            }
        }
    }
    
    /** Updates _availableBytes and _nextTickTime if possible. */
    private void updateWindow(long now) {
        if (now >= _nextTickTime) {
            _availableBytes = _bytesPerTick;
            _nextTickTime = now + MILLIS_PER_TICK;
        }
    }

    //Tests: see core/com/.../tests/BandwidthThrottleTest
}
