package com.limegroup.gnutella.udpconnect;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
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
    private ArrayList /* of ByteBuffer */ chunks;

    /**
     * The current chunk getting written to.
     */
    private ByteBuffer activeChunk;

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
            if ( !_connectionActive ) {
                throw new IOException("Connection Closed");
            } else if (activeChunk.remaining() > 0) {
                // If there is room within current chunk
                activeChunk.put((byte) b);
                return;
            } else if (chunks.size() < _processor.getChunkLimit()) {
                // If there is room for more chunks, allocate a new chunk
                chunks.add(activeChunk);
                allocateNewChunk();
            } else {
                // Wait for room for a new chunk
                waitOnReader();

                // Again, If there was no data before this,
                // then ensure a writer is awake
                if (getPendingChunks() == 0)
                    _processor.wakeupWriteEvent();
            }
        }
    }

    /**
     * Do a partial write from the byte array. Block if necessary.
     */
    public synchronized void write(byte b[], int off, int len) 
      throws IOException {

        if(LOG.isDebugEnabled())
            LOG.debug("writing len: "+len+" bytes");
		

		// If there was no data before this, then ensure a writer is awake
        if ( _connectionActive && getPendingChunks() == 0 )
			_processor.wakeupWriteEvent();

        while (true) {
            if ( !_connectionActive ) {
                throw new IOException("Connection Closed");
            } else if (activeChunk.remaining() > 0) {
                // If there is room within current chunk then
                // fill up the current chunk
                int available = Math.min(activeChunk.remaining(), len);
                activeChunk.put(b, off, available);
                len -= available;
                off += available;
                if(len == 0)
                    return;
            } else if (chunks.size() < _processor.getChunkLimit()) {
                // If there is room for more chunks, allocate a new chunk
                chunks.add(activeChunk);
                allocateNewChunk();
            } else {
                // Wait for room for a new chunk
                waitOnReader();

                // Again, If there was no data before this,
                // then ensure a writer is awake
                if (getPendingChunks() == 0)
                    _processor.wakeupWriteEvent();
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
        activeChunk = ByteBuffer.allocate(UDPConnectionProcessor.DATA_CHUNK_SIZE);
    }

    /**
     *  Package accessor for retrieving and freeing up chunks of data.
     *  Returns null if no data.
     */
    synchronized ByteBuffer getChunk() {
        ByteBuffer rChunk;
        if ( chunks.size() > 0 ) {
            // Return the oldest chunk 
            rChunk = (ByteBuffer)chunks.remove(0);
            rChunk.flip();
        } else if (activeChunk.position() > 0) {
            rChunk = activeChunk;
            rChunk.flip();
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
		if (activeChunk.position() > 0)
			count++;

		return count;
    }
}
