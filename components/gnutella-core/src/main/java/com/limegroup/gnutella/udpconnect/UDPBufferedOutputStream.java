package com.limegroup.gnutella.udpconnect;

import java.io.*;
import com.sun.java.util.collections.*;

/**
 *  Handle writing to a udp connection via a stream.  Internally, the writes 
 *  are broken up into chunks of a convenient size for UDP packets.  Blocking 
 *  occurs when the internal list is full.  This means that the 
 *  UDPConnectionProcessor can't send the data currently.
 */
public class UDPBufferedOutputStream extends OutputStream {

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

    /**
     *  Creates an output stream front end to a UDP Connection.
     */
    public UDPBufferedOutputStream(UDPConnectionProcessor p) {
		_processor    = p;
        chunks        = new ArrayList(5);
        allocateNewChunk();
    }

    /**
     *  Writes the specified byte to the activeChunk if room.
     *  Block if necessary.
     */
    public synchronized void write(int b) {
        while (true) {
			// If there is room within current chunk
            if ( activeCount < UDPConnectionProcessor.DATA_CHUNK_SIZE ) {
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
                    try { wait(FOREVER); } catch(InterruptedException e) {}
                }
            }
        }
    }

    /**
     * Do a partial write from the byte array. Block if necessary.
     */
    public synchronized void write(byte b[], int off, int len) {
		
		int space;   // The space available within the active chunk
		int wlength; // The length of data to be written to the active chunk

        while (true) {
			// If there is room within current chunk
			if ( activeCount < activeChunk.length ) {
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
                    try { wait(FOREVER); } catch(InterruptedException e) {}
                }
            }
        }
    }

    /**
     * Closing output stream has no effect. 
     */
    public void close() throws IOException {
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
		Chunk rChunk = new Chunk();
		if ( chunks.size() > 0 ) {
			// Return the oldest chunk 
			rChunk.chunk = (byte[]) chunks.remove(0);
			rChunk.count = rChunk.chunk.length;
		} else if (activeCount > 0) {
			// Return a partial chunk and allocate a fresh one
			rChunk.chunk = activeChunk;
			rChunk.count = activeCount;
    		allocateNewChunk();
		} else {
			// If no data currently, return null
			return null;
		}
		// Wakeup any write operation waiting for space
		notify();
		return rChunk;
    }
}
