package com.limegroup.gnutella.udpconnect;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *  Handle reading from a UDP Connection in the form of a stream.
 *  This class tries to minimize byte array allocations by using the
 *  data directly as it comes out of messages.
 */
public class UDPBufferedInputStream extends InputStream {

	
    private static final Log LOG =
        LogFactory.getLog(UDPBufferedInputStream.class);
    
    /**
     *  The maximum blocking time of a read.
     */
    private static final int FOREVER = 10 * 60 * 60 * 1000;

    /**
     *  A cached chunk of data that hasn't been completely written 
     *  to the stream.
     */
    protected Chunk _activeChunk;

    /**
     *  The reader of information coming into this output stream.
     */
    private UDPConnectionProcessor _processor;

    /**
     * Create the InputStream with a handle to the connection processor 
     * to be used as an input source.
     */
    public UDPBufferedInputStream(UDPConnectionProcessor p) {
        _processor   = p; 
        _activeChunk = null;
    }

    /**
     * Read the next byte of data from the input source.  As normal, return -1 
     * if there is no more data.
     */
    public int read() throws IOException  {
    	boolean timedOut=false;
    	
        synchronized(_processor) { // Lock on the ConnectionProcessor
            while (true) {
                // Try to fetch some data if necessary
                checkForData();

                if ( _activeChunk != null && _activeChunk.length > 0 ) {
                    // return a byte of data
                    _activeChunk.length--;
                    return (_activeChunk.data[_activeChunk.start++] & 0xff);

                } else if ( _activeChunk == null && _processor.isConnected() ) {

                    // Wait for some data to become available
                	if (timedOut) {
                		InterruptedIOException e = new InterruptedIOException();
                		
                		if (LOG.isDebugEnabled()) {
                			LOG.debug("read() timed out for timeout "+
                					_processor.getReadTimeout(),e);
                		}
                		
                		throw e;
                	}
                    waitOnData();
                    timedOut=true;
                } else {

                    // This connection is closed
                    return -1;
                }
            }
        }
    }

    /**
     * Just ensure that this call is passed to the detailed local read method.
     */
    public int read(byte b[]) throws IOException  {
        return read(b, 0, b.length);
    } 

    /**
     * Read the next len byte of data from the input source. Return how much
     * was really read.  
     */
    public int read(byte b[], int off, int len)
      throws IOException  {
        int origLen = len;
        int origOff = off;
        int wlength;
        boolean timedOut= false;
        
        synchronized(_processor) {  // Lock on the ConnectionProcessor
            while (true) {
                // Try to fetch some data if necessary
                checkForData();

                if ( _activeChunk != null && _activeChunk.length > 0 ) {
                	timedOut=false;

                    // Load some data
                    wlength = Math.min(_activeChunk.length, len);
                    System.arraycopy( _activeChunk.data, _activeChunk.start, 
                      b, off, wlength);
                    len                 -= wlength;
                    off                 += wlength;
                    _activeChunk.start  += wlength;
                    _activeChunk.length -= wlength;
                    if ( len <= 0 ) 
                        return origLen;

                } else if ( origLen != len ){

                    // Return whatever was available
                    return(origLen - len);

                } else if ( _activeChunk == null && _processor.isConnected() ) {

                    // Wait for some data to become available
                	if (timedOut) {
                		InterruptedIOException e = new InterruptedIOException();
                		
                		if (LOG.isDebugEnabled()) {
                			LOG.debug("read(byte [], int, int) timed out for timeout "+
                					_processor.getReadTimeout(),e);
                		}
                		
                		throw e;
                	}
                	
                    waitOnData();
                    timedOut=true;
                } else {

                    // This connection is closed
                    return -1;
                }

            }
        }
    }

    /**
     *  Throw away n bytes of data.  Return the true amount of bytes skipped.
     *  Note that I am downgrading the long input to an int.
     */
    public long skip(long n) 
      throws IOException  {
        int len     = (int) n;
        int origLen = len;
        int wlength;
        boolean timedOut=false;
        
        // Just like reading a chunk of data above but the bytes get ignored
        synchronized(_processor) {  // Lock on the ConnectionProcessor
            while (true) {
                // Try to fetch some data if necessary
                checkForData();

                if ( _activeChunk != null && _activeChunk.length > 0 ) {
                	timedOut=false;

                    // Load some data
                    wlength = Math.min(_activeChunk.length, len);
                    len                 -= wlength;
                    _activeChunk.start  += wlength;
                    _activeChunk.length -= wlength;
                    if ( len <= 0 ) 
                        return origLen;

                } else if ( origLen != len ){

                    // Return whatever was available
                    return(origLen - len);

                } else if ( _activeChunk == null && _processor.isConnected() ) {

                    // Wait for some data to become available
                	if (timedOut) {
                		InterruptedIOException e = new InterruptedIOException();
                		
                		if (LOG.isDebugEnabled()) {
                			LOG.debug("skip() timed out for timeout "+
                					_processor.getReadTimeout(),e);
                		}
                		
                		throw e;
                	}
                    waitOnData();
                    timedOut=true;
                } else {

                    // This connection is closed
                    return -1;
                }
            }
        }
    }

    /**
     *  Returns how many bytes I know are immediately available.
     *  This can be optimized later but these things never seem to be accurate.
     */
    public int available() {
        synchronized(_processor) {  // Lock on the ConnectionProcessor
            if ( _activeChunk == null )
                return 0;
            return _activeChunk.length;
        }
    }

    /**
     *  I hope that I don't need to support this.
     */
    public boolean markSupported() {
        return false;
    }

    /**
     *  I hope that I don't need to support this.
     */
    public void mark(int readAheadLimit) {
    }

    /**
     *  I hope that I don't need to support this.
     */
    public void reset() {
    }

    /**
     *  This does nothing for now.
     */
    public void close() throws IOException {
        _processor.close();
    }

    /**
     *  If no pending data then try to get some.
     */
    private void checkForData() {
        synchronized(_processor) {  // Lock on the ConnectionProcessor
            if ( _activeChunk == null || _activeChunk.length <= 0 ) {
                _activeChunk = _processor.getIncomingChunk();
            }
        }
    }

    /**
     *  Wait for a new chunk to become available.
     */
    private void waitOnData() throws InterruptedIOException {
        synchronized(_processor) {  // Lock on the ConnectionProcessor
            try { 
            	_processor.wait(_processor.getReadTimeout());
            } catch(InterruptedException e) {
                throw new InterruptedIOException(e.getMessage()); 
            } 
        }
    }

    /**
     *  Package accessor for notifying readers that data is available.
     */
    void wakeup() {
        synchronized(_processor) {  // Lock on the ConnectionProcessor
            // Wakeup any read operation waiting for data
            _processor.notify();  
        }
    }
}
