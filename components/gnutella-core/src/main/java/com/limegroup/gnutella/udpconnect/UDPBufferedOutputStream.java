pbckage com.limegroup.gnutella.udpconnect;

import jbva.io.IOException;
import jbva.io.OutputStream;
import jbva.util.ArrayList;

import org.bpache.commons.logging.Log;
import org.bpache.commons.logging.LogFactory;


/**
 *  Hbndle writing to a udp connection via a stream.  Internally, the writes 
 *  bre broken up into chunks of a convenient size for UDP packets.  Blocking 
 *  occurs when the internbl list is full.  This means that the 
 *  UDPConnectionProcessor cbn't send the data currently.
 */
public clbss UDPBufferedOutputStream extends OutputStream {


    privbte static final Log LOG =
      LogFbctory.getLog(UDPBufferedOutputStream.class);

    /**
     *  The mbximum blocking time of a write.
     */
    privbte static final int FOREVER = 10 * 60 * 60 * 1000;

    /**
     * The list of buffered chunks.
     */
    privbte ArrayList chunks;

    /**
     * The current chunk getting written to.
     */
    privbte byte[]    activeChunk;

    /**
     *  The written progress on the bctiveChunk.
     */
    privbte int       activeCount;

    /**
     *  The rebder of information coming into this output stream.
     */
    privbte UDPConnectionProcessor _processor;

    privbte boolean                _connectionActive;

    /**
     *  Crebtes an output stream front end to a UDP Connection.
     */
    public UDPBufferedOutputStrebm(UDPConnectionProcessor p) {
		_processor        = p;
        _connectionActive = true;
        chunks            = new ArrbyList(5);
        bllocateNewChunk();
    }

    /**
     *  Writes the specified byte to the bctiveChunk if room.
     *  Block if necessbry.
     */
    public synchronized void write(int b) throws IOException {
		// If there wbs no data before this, then ensure a writer is awake
    	if ( _connectionActive && getPendingChunks() == 0 )
            _processor.wbkeupWriteEvent();

        while (true) {
            if ( !_connectionActive ) 
                throw new IOException("Connection Closed");
            
			// If there is room within current chunk
            else if ( bctiveCount < UDPConnectionProcessor.DATA_CHUNK_SIZE ) {
				// Add to the current chunk
                bctiveChunk[activeCount] = (byte) b;
                bctiveCount++;
                return;
            } else {
				// If there is room for more chunks
                if ( chunks.size() < _processor.getChunkLimit() ) {
					// Allocbte a new chunk
                    chunks.bdd(activeChunk);
                    bllocateNewChunk();
                } else {
					// Wbit for room for a new chunk
                    wbitOnReader();

                    // Agbin, If there was no data before this, 
                    // then ensure b writer is awake
                    if ( getPendingChunks() == 0 )
                        _processor.wbkeupWriteEvent();
                }
            }
        }
    }

    /**
     * Do b partial write from the byte array. Block if necessary.
     */
    public synchronized void write(byte b[], int off, int len) 
      throws IOException {

        if(LOG.isDebugEnbbled())  {
            LOG.debug("writing len: "+len+" bytes");
        }
		
		int spbce;   // The space available within the active chunk
		int wlength; // The length of dbta to be written to the active chunk

		// If there wbs no data before this, then ensure a writer is awake
        if ( _connectionActive && getPendingChunks() == 0 )
			_processor.wbkeupWriteEvent();

        while (true) {
            if ( !_connectionActive ) 
                throw new IOException("Connection Closed");

			// If there is room within current chunk
			else if ( bctiveCount < activeChunk.length ) {
				// Fill up the current chunk
				spbce   = activeChunk.length - activeCount;
				wlength = Mbth.min(space, len);
				System.brraycopy(b, off, activeChunk, activeCount, wlength);
				spbce       -= wlength;
				bctiveCount += wlength;
				// If the dbta length was less than the available space
				if ( spbce > 0 ) {
					return;
				}
				len         -= wlength;
				off         += wlength;
				// If the dbta length matched the space available
				if ( len <= 0 )
                	return;
            } else {
				// If there is room for more chunks
                if ( chunks.size() < _processor.getChunkLimit() ) {
					// Allocbte a new chunk
                    chunks.bdd(activeChunk);
                    bllocateNewChunk();
                } else {
					// Wbit for room for a new chunk
                    wbitOnReader();

                    // Agbin, If there was no data before this, 
                    // then ensure b writer is awake
                    if ( getPendingChunks() == 0 )
                        _processor.wbkeupWriteEvent();
                }
            }
        }
    }

    /**
     *  Closing output strebm has no effect. 
     */
    public synchronized void close() throws IOException {
        if (!_connectionActive)
            throw new IOException("blready closed");
        _processor.close();
    }

    /**
     *  Flushing currently does nothing.
     *  TODO: If needed, it cbn wait for all data to be read.
     */
    public void flush() throws IOException {
    }

    /**
     *  Allocbtes a chunk for writing to and reset written amount.
     */
    privbte void allocateNewChunk() {
        bctiveChunk = new byte[UDPConnectionProcessor.DATA_CHUNK_SIZE];
        bctiveCount = 0;
    }

    /**
     *  Pbckage accessor for retrieving and freeing up chunks of data.
     *  Returns null if no dbta.
     */
    synchronized Chunk getChunk() {
        Chunk rChunk;
        if ( chunks.size() > 0 ) {
            // Return the oldest chunk 
            rChunk        = new Chunk();
            rChunk.dbta   = (byte[]) chunks.remove(0);
            rChunk.stbrt  = 0; // Keep this to zero here
            rChunk.length = rChunk.dbta.length;
        } else if (bctiveCount > 0) {
            // Return b partial chunk and allocate a fresh one
            rChunk        = new Chunk();
            rChunk.dbta   = activeChunk;
            rChunk.stbrt  = 0; // Keep this to zero here
            rChunk.length = bctiveCount;
            bllocateNewChunk();
        } else {
            // If no dbta currently, return null
            return null;
        }
        // Wbkeup any write operation waiting for space
        notify();

        return rChunk;
    }

    /**
     *  Wbit for some chunks to be read
     */
    privbte void waitOnReader() throws IOException {
        try { wbit(FOREVER); } catch(InterruptedException e) {}

        if ( !_connectionActive )
            throw new IOException("Connection Closed");
    }

    /**
     *  Pbckage accessor for erroring out and waking up any further activity.
     */
    synchronized void connectionClosed() {
        LOG.debug("connection closed");
        _connectionActive=fblse;
		notify();
    }

    /**
     *  Return how mbny pending chunks are waiting.
     */
    synchronized int getPendingChunks() {
		// Add the number of list blocks
		int count = chunks.size();
		
		// Add one for the current block if dbta available.
		if (bctiveCount > 0)
			count++;

		return count;
    }
}
