package com.limegroup.gnutella.udpconnect;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


/**
 *  Handle writing to a udp connection via a stream.  Internally, the writes 
 *  are broken up into chunks of a convenient size for UDP packets.  Blocking 
 *  occurs when the internal list is full.  This means that the 
 *  UDPConnectionProcessor can't send the data currently.
 */
public class UDPBufferedOutputStream extends OutputStream {


    private static final Log LOG =
      LogFactory.getLog(UDPBufferedOutputStream.class);

    /**
     *  The maximum blocking time of a write.
     */
    private static final int FOREVER = 10 * 60 * 60 * 1000;

    /**
     * The list of buffered chunks.
     */
    private ArrayList chunks;

    /**
     * The current chunk getting written to.
     */
    private byte[]    activeChunk;

    /**
     *  The written progress on the activeChunk.
     */
    private int       activeCount;

    /**
     *  The reader of information coming into this output stream.
     */
    private UDPConnectionProcessor _processor;

    private boolean                _connectionActive;

    /**
     *  Creates an output stream front end to a UDP Connection.
     */
    public UDPBufferedOutputStream(UDPConnectionProcessor p) {
		_processor        = p;
        _connectionActive = true;
        chunks            = new ArrayList(5);
        allocateNewChunk();
    }

    /**
     *  Writes the specified byte to the activeChunk if room.
     *  Block if necessary.
     */
    public synchronized void write(int b) throws IOException {
		// If there was no data before this, then ensure a writer is awake
    	if ( _connectionActive && getPendingChunks() == 0 )
            _processor.wakeupWriteEvent();

        while (true) {
            if ( !_connectionActive ) 
                throw new IOException("Connection Closed");
            
			// If there is room within current chunk
            else if ( activeCount < UDPConnectionProcessor.DATA_CHUNK_SIZE ) {
				// Add to the current chunk
                activeChunk[activeCount] = (byte) b;
                activeCount++;
                return;
            } else {
				// If there is room for more chunks
                if ( chunks.size() < _processor.getChunkLimit() ) {
					// Allocate a new chunk
                    chunks.add(activeChunk);
                    allocateNewChunk();
                } else {
					// Wait for room for a new chunk
                    waitOnReader();

                    // Again, If there was no data before this, 
                    // then ensure a writer is awake
                    if ( getPendingChunks() == 0 )
                        _processor.wakeupWriteEvent();
                }
            }
        }
    }

    /**
     * Do a partial write from the byte array. Block if necessary.
     */
    public synchronized void write(byte b[], int off, int len) 
      throws IOException {

        if(LOG.isDebugEnabled())  {
            LOG.debug("writing len: "+len+" bytes");
        }
		
		int space;   // The space available within the active chunk
		int wlength; // The length of data to be written to the active chunk

		// If there was no data before this, then ensure a writer is awake
        if ( _connectionActive && getPendingChunks() == 0 )
			_processor.wakeupWriteEvent();

        while (true) {
            if ( !_connectionActive ) 
                throw new IOException("Connection Closed");

			// If there is room within current chunk
			else if ( activeCount < activeChunk.length ) {
				// Fill up the current chunk
				space   = activeChunk.length - activeCount;
				wlength = Math.min(space, len);
				System.arraycopy(b, off, activeChunk, activeCount, wlength);
				space       -= wlength;
				activeCount += wlength;
				// If the data length was less than the available space
				if ( space > 0 ) {
					return;
				}
				len         -= wlength;
				off         += wlength;
				// If the data length matched the space available
				if ( len <= 0 )
                	return;
            } else {
				// If there is room for more chunks
                if ( chunks.size() < _processor.getChunkLimit() ) {
					// Allocate a new chunk
                    chunks.add(activeChunk);
                    allocateNewChunk();
                } else {
					// Wait for room for a new chunk
                    waitOnReader();

                    // Again, If there was no data before this, 
                    // then ensure a writer is awake
                    if ( getPendingChunks() == 0 )
                        _processor.wakeupWriteEvent();
                }
            }
        }
    }

    /**
     *  Closing output stream has no effect. 
     */
    public synchronized void close() throws IOException {
        if (!_connectionActive)
            throw new IOException("already closed");
        _processor.close();
    }

    /**
     *  Flushing currently does nothing.
     *  TODO: If needed, it can wait for all data to be read.
     */
    public void flush() throws IOException {
    }

    /**
     *  Allocates a chunk for writing to and reset written amount.
     */
    private void allocateNewChunk() {
        activeChunk = new byte[UDPConnectionProcessor.DATA_CHUNK_SIZE];
        activeCount = 0;
    }

    /**
     *  Package accessor for retrieving and freeing up chunks of data.
     *  Returns null if no data.
     */
    synchronized Chunk getChunk() {
        Chunk rChunk;
        if ( chunks.size() > 0 ) {
            // Return the oldest chunk 
            rChunk        = new Chunk();
            rChunk.data   = (byte[]) chunks.remove(0);
            rChunk.start  = 0; // Keep this to zero here
            rChunk.length = rChunk.data.length;
        } else if (activeCount > 0) {
            // Return a partial chunk and allocate a fresh one
            rChunk        = new Chunk();
            rChunk.data   = activeChunk;
            rChunk.start  = 0; // Keep this to zero here
            rChunk.length = activeCount;
            allocateNewChunk();
        } else {
            // If no data currently, return null
            return null;
        }
        // Wakeup any write operation waiting for space
        notify();

        return rChunk;
    }

    /**
     *  Wait for some chunks to be read
     */
    private void waitOnReader() throws IOException {
        try { wait(FOREVER); } catch(InterruptedException e) {}

        if ( !_connectionActive )
            throw new IOException("Connection Closed");
    }

    /**
     *  Package accessor for erroring out and waking up any further activity.
     */
    synchronized void connectionClosed() {
        LOG.debug("connection closed");
        _connectionActive=false;
		notify();
    }

    /**
     *  Return how many pending chunks are waiting.
     */
    synchronized int getPendingChunks() {
		// Add the number of list blocks
		int count = chunks.size();
		
		// Add one for the current block if data available.
		if (activeCount > 0)
			count++;

		return count;
    }
}
