package com.limegroup.gnutella.util;

import java.io.IOException;
import java.io.OutputStream;

import com.limegroup.gnutella.Assert;

/** 
 * Wraps a stream to ensure that the caller can write no more than N bytes/sec.
 * ThrottledOutputStream delegates to a BandwidthThrottle object to control
 * throughput.  By sharing a single BandwidthThrottle among multiple
 * ThrottledOutputStream instances, the user can approximate fair global
 * bandwidth sharing.<p>
 *
 * This implementation is based on the <a href="http://cvs.sourceforge.net/cgi-bin/viewcvs.cgi/freenet/freenet/src/freenet/support/io/ThrottledOutputStream.java">ThrottledOutputStream</a> class from the Freenet project.  It has been 
 * modified so that the bandwidth throttle is no longer static.  It also
 * no longer subclasses FilterOutputStream, as the temptation to call
 * super.write() introduced some bugs.  <p>
 */
public class ThrottledOutputStream extends OutputStream {        
    /** The delegate. */
    private OutputStream _delegate;
    /** Limits throughput. */
    private BandwidthThrottle _throttle;
  
    /** 
     * Wraps the delegate stream with the given throttle. 
     * @param delegate the underlying stream for all IO
     * @param throttle limits throughput.  May be shared with other streams.
     */
    public ThrottledOutputStream(OutputStream delegate,
                                 BandwidthThrottle throttle) {
        this._delegate=delegate;
        this._throttle=throttle;
    }

    /**
     * Write a single byte to the delegate stream, possibly blocking if
     * necessary to ensure that throughput doesn't exceed the limits.
     * 
     * @param b the byte to write.
     * @exception IOException if an I/O error occurs on the OutputStream.  
     */
    public void write(final int b) throws IOException {
        int allow=_throttle.request(1); //Note that _request never returns zero.
        Assert.that(allow==1);
        _delegate.write(b);
    }
    
    /**
     * Write bytes[offset...offset+totalLength-1] to the delegate stream,
     * possibly blocking if necessary to ensure that throughput doesn't exceed
     * the limits.
     *
     * @param data the bytes to write.
     * @param offset the index in the array to start at.
     * @param totalLength the number of bytes to write.
     * @exception IOException if an I/O error occurs on the OutputStream.  
     */
    public void write(byte[] data, int offset, int totalLength)
        throws IOException
    {        
        //Note that we delegate directly to out.  Do NOT call super.write();
        //that calls this.write() resulting in HALF the throughput.
        while (totalLength > 0) {
            int length = _throttle.request(totalLength);    
            Assert.that(length+offset<=data.length);
            _delegate.write(data, offset, length);
            totalLength -= length;
            offset += length;
        }
    }

    /**
     * Write the given bytes to the delegate stream, possibly blocking if
     * necessary to ensure that throughput doesn't exceed the limits.
     */
    public void write(byte[] data) throws IOException {
        write(data, 0, data.length);
    }

    public void flush() throws IOException {
        _delegate.flush();
    }

    public void close() throws IOException {
        _delegate.flush();
    }

    //Tests: see core/com/.../tests/BandwidthThrottleTest
}
