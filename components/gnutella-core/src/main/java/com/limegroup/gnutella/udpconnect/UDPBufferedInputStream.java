package com.limegroup.gnutella.udpconnect;

import java.io.*;
import java.net.*;

/**
 *  Handle reading from a UDP Connection in the form of a stream.
 *  This class tries to minimize byte array allocations by using the
 *  data directly as it comes out of messages.
 */
public class UDPBufferedInputStream extends InputStream {

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
    public synchronized int read() throws IOException  {
        while (true) {
            // Try to fetch some data if necessary
            checkForData();

            if ( _activeChunk != null && _activeChunk.length > 0 ) {

                // return a byte of data
                _activeChunk.length--;
                return (_activeChunk.data[_activeChunk.start++] & 0xff);

            } else if ( _activeChunk == null && _processor.isConnected() ) {

                // Wait for some data to become available
                waitOnData();

            } else {

                // This connection is closed
                return -1;
            }

        }
    }

    /**
     * 
     */
    public synchronized int read(byte b[], int off, int len)
      throws IOException  {
        int origLen = len;
        int wlength;

        while (true) {
            // Try to fetch some data if necessary
            checkForData();

            if ( _activeChunk != null && _activeChunk.length > 0 ) {

                // Load some data
                wlength = Math.min(_activeChunk.length, len);
                System.arraycopy(b, off, 
                  _activeChunk.data, _activeChunk.start, wlength);
                len                 -= wlength;
                _activeChunk.start  += wlength;
                _activeChunk.length -= wlength;
                if ( len <= 0 ) 
                    return origLen;

            } else if ( _activeChunk == null && _processor.isConnected() ) {

                // Wait for some data to become available
                waitOnData();

            } else if ( origLen != len ){

                // Return whatever was available
                return(origLen - len);

            } else {

                // This connection is closed
                return 0;
            }

        }
    }

    /**
     *  Throw away n bytes of data.  I think I just wont do this for now.
     *  TODO: Needed?
     */
    public synchronized long skip(long n) {
        return 0;
    }

    /**
     *  Returns how many bytes I know are immediately available.
     *  This can be optimized later but these things never seem to be accurate.
     */
    public synchronized int available() {
        if ( _activeChunk == null )
            return 0;
        return _activeChunk.length;
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
    public synchronized void reset() {
    }

    /**
     *  This does nothing for now.
     */
    public void close() throws IOException {
    }

    /**
     *  If no pending data then try to get some.
     */
    private synchronized void checkForData() {
        if ( _activeChunk == null || _activeChunk.length <= 0 ) {
            _activeChunk = _processor.getIncomingChunk();
        }
    }

    /**
     *  Wait for a new chunk to become available.
     */
    private synchronized void waitOnData() throws SocketTimeoutException {
        try { 
            wait(_processor.getReadTimeout()); 
        } catch(InterruptedException e) {
            throw new SocketTimeoutException(); 
        } 
    }

    /**
     *  Package accessor for notifying readers that data is available.
     */
    synchronized void wakeup() {
        // Wakeup any read operation waiting for data
        notify();  
    }
}
