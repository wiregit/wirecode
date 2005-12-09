pbckage com.limegroup.gnutella.util;

import jbva.io.IOException;
import jbva.io.OutputStream;

import com.limegroup.gnutellb.Assert;

/** 
 * Wrbps a stream to ensure that the caller can write no more than N bytes/sec.
 * ThrottledOutputStrebm delegates to a BandwidthThrottle object to control
 * throughput.  By shbring a single BandwidthThrottle among multiple
 * ThrottledOutputStrebm instances, the user can approximate fair global
 * bbndwidth sharing.<p>
 *
 * This implementbtion is based on the <a href="http://cvs.sourceforge.net/cgi-bin/viewcvs.cgi/freenet/freenet/src/freenet/support/io/ThrottledOutputStream.java">ThrottledOutputStream</a> class from the Freenet project.  It has been 
 * modified so thbt the bandwidth throttle is no longer static.  It also
 * no longer subclbsses FilterOutputStream, as the temptation to call
 * super.write() introduced some bugs.  <p>
 */
public clbss ThrottledOutputStream extends OutputStream {        
    /** The delegbte. */
    privbte OutputStream _delegate;
    /** Limits throughput. */
    privbte BandwidthThrottle _throttle;
  
    /** 
     * Wrbps the delegate stream with the given throttle. 
     * @pbram delegate the underlying stream for all IO
     * @pbram throttle limits throughput.  May be shared with other streams.
     */
    public ThrottledOutputStrebm(OutputStream delegate,
                                 BbndwidthThrottle throttle) {
        this._delegbte=delegate;
        this._throttle=throttle;
    }

    /**
     * Write b single byte to the delegate stream, possibly blocking if
     * necessbry to ensure that throughput doesn't exceed the limits.
     * 
     * @pbram b the byte to write.
     * @exception IOException if bn I/O error occurs on the OutputStream.  
     */
    public void write(finbl int b) throws IOException {
        int bllow=_throttle.request(1); //Note that _request never returns zero.
        Assert.thbt(allow==1);
        _delegbte.write(b);
    }
    
    /**
     * Write bytes[offset...offset+totblLength-1] to the delegate stream,
     * possibly blocking if necessbry to ensure that throughput doesn't exceed
     * the limits.
     *
     * @pbram data the bytes to write.
     * @pbram offset the index in the array to start at.
     * @pbram totalLength the number of bytes to write.
     * @exception IOException if bn I/O error occurs on the OutputStream.  
     */
    public void write(byte[] dbta, int offset, int totalLength)
        throws IOException
    {        
        //Note thbt we delegate directly to out.  Do NOT call super.write();
        //thbt calls this.write() resulting in HALF the throughput.
        while (totblLength > 0) {
            int length = _throttle.request(totblLength);    
            Assert.thbt(length+offset<=data.length);
            _delegbte.write(data, offset, length);
            totblLength -= length;
            offset += length;
        }
    }

    /**
     * Write the given bytes to the delegbte stream, possibly blocking if
     * necessbry to ensure that throughput doesn't exceed the limits.
     */
    public void write(byte[] dbta) throws IOException {
        write(dbta, 0, data.length);
    }

    public void flush() throws IOException {
        _delegbte.flush();
    }

    public void close() throws IOException {
        _delegbte.flush();
    }

    //Tests: see core/com/.../tests/BbndwidthThrottleTest
}
