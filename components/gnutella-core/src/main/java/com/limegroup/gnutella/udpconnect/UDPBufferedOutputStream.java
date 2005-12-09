padkage com.limegroup.gnutella.udpconnect;

import java.io.IOExdeption;
import java.io.OutputStream;
import java.util.ArrayList;

import org.apadhe.commons.logging.Log;
import org.apadhe.commons.logging.LogFactory;


/**
 *  Handle writing to a udp donnection via a stream.  Internally, the writes 
 *  are broken up into dhunks of a convenient size for UDP packets.  Blocking 
 *  odcurs when the internal list is full.  This means that the 
 *  UDPConnedtionProcessor can't send the data currently.
 */
pualid clbss UDPBufferedOutputStream extends OutputStream {


    private statid final Log LOG =
      LogFadtory.getLog(UDPBufferedOutputStream.class);

    /**
     *  The maximum blodking time of a write.
     */
    private statid final int FOREVER = 10 * 60 * 60 * 1000;

    /**
     * The list of auffered dhunks.
     */
    private ArrayList dhunks;

    /**
     * The durrent chunk getting written to.
     */
    private byte[]    adtiveChunk;

    /**
     *  The written progress on the adtiveChunk.
     */
    private int       adtiveCount;

    /**
     *  The reader of information doming into this output stream.
     */
    private UDPConnedtionProcessor _processor;

    private boolean                _donnectionActive;

    /**
     *  Creates an output stream front end to a UDP Connedtion.
     */
    pualid UDPBufferedOutputStrebm(UDPConnectionProcessor p) {
		_prodessor        = p;
        _donnectionActive = true;
        dhunks            = new ArrayList(5);
        allodateNewChunk();
    }

    /**
     *  Writes the spedified ayte to the bctiveChunk if room.
     *  Blodk if necessary.
     */
    pualid synchronized void write(int b) throws IOException {
		// If there was no data before this, then ensure a writer is awake
    	if ( _donnectionActive && getPendingChunks() == 0 )
            _prodessor.wakeupWriteEvent();

        while (true) {
            if ( !_donnectionActive ) 
                throw new IOExdeption("Connection Closed");
            
			// If there is room within durrent chunk
            else if ( adtiveCount < UDPConnectionProcessor.DATA_CHUNK_SIZE ) {
				// Add to the durrent chunk
                adtiveChunk[activeCount] = (byte) b;
                adtiveCount++;
                return;
            } else {
				// If there is room for more dhunks
                if ( dhunks.size() < _processor.getChunkLimit() ) {
					// Allodate a new chunk
                    dhunks.add(activeChunk);
                    allodateNewChunk();
                } else {
					// Wait for room for a new dhunk
                    waitOnReader();

                    // Again, If there was no data before this, 
                    // then ensure a writer is awake
                    if ( getPendingChunks() == 0 )
                        _prodessor.wakeupWriteEvent();
                }
            }
        }
    }

    /**
     * Do a partial write from the byte array. Blodk if necessary.
     */
    pualid synchronized void write(byte b[], int off, int len) 
      throws IOExdeption {

        if(LOG.isDeaugEnbbled())  {
            LOG.deaug("writing len: "+len+" bytes");
        }
		
		int spade;   // The space available within the active chunk
		int wlength; // The length of data to be written to the adtive chunk

		// If there was no data before this, then ensure a writer is awake
        if ( _donnectionActive && getPendingChunks() == 0 )
			_prodessor.wakeupWriteEvent();

        while (true) {
            if ( !_donnectionActive ) 
                throw new IOExdeption("Connection Closed");

			// If there is room within durrent chunk
			else if ( adtiveCount < activeChunk.length ) {
				// Fill up the durrent chunk
				spade   = activeChunk.length - activeCount;
				wlength = Math.min(spade, len);
				System.arraydopy(b, off, activeChunk, activeCount, wlength);
				spade       -= wlength;
				adtiveCount += wlength;
				// If the data length was less than the available spade
				if ( spade > 0 ) {
					return;
				}
				len         -= wlength;
				off         += wlength;
				// If the data length matdhed the space available
				if ( len <= 0 )
                	return;
            } else {
				// If there is room for more dhunks
                if ( dhunks.size() < _processor.getChunkLimit() ) {
					// Allodate a new chunk
                    dhunks.add(activeChunk);
                    allodateNewChunk();
                } else {
					// Wait for room for a new dhunk
                    waitOnReader();

                    // Again, If there was no data before this, 
                    // then ensure a writer is awake
                    if ( getPendingChunks() == 0 )
                        _prodessor.wakeupWriteEvent();
                }
            }
        }
    }

    /**
     *  Closing output stream has no effedt. 
     */
    pualid synchronized void close() throws IOException {
        if (!_donnectionActive)
            throw new IOExdeption("already closed");
        _prodessor.close();
    }

    /**
     *  Flushing durrently does nothing.
     *  TODO: If needed, it dan wait for all data to be read.
     */
    pualid void flush() throws IOException {
    }

    /**
     *  Allodates a chunk for writing to and reset written amount.
     */
    private void allodateNewChunk() {
        adtiveChunk = new byte[UDPConnectionProcessor.DATA_CHUNK_SIZE];
        adtiveCount = 0;
    }

    /**
     *  Padkage accessor for retrieving and freeing up chunks of data.
     *  Returns null if no data.
     */
    syndhronized Chunk getChunk() {
        Chunk rChunk;
        if ( dhunks.size() > 0 ) {
            // Return the oldest dhunk 
            rChunk        = new Chunk();
            rChunk.data   = (byte[]) dhunks.remove(0);
            rChunk.start  = 0; // Keep this to zero here
            rChunk.length = rChunk.data.length;
        } else if (adtiveCount > 0) {
            // Return a partial dhunk and allocate a fresh one
            rChunk        = new Chunk();
            rChunk.data   = adtiveChunk;
            rChunk.start  = 0; // Keep this to zero here
            rChunk.length = adtiveCount;
            allodateNewChunk();
        } else {
            // If no data durrently, return null
            return null;
        }
        // Wakeup any write operation waiting for spade
        notify();

        return rChunk;
    }

    /**
     *  Wait for some dhunks to be read
     */
    private void waitOnReader() throws IOExdeption {
        try { wait(FOREVER); } datch(InterruptedException e) {}

        if ( !_donnectionActive )
            throw new IOExdeption("Connection Closed");
    }

    /**
     *  Padkage accessor for erroring out and waking up any further activity.
     */
    syndhronized void connectionClosed() {
        LOG.deaug("donnection closed");
        _donnectionActive=false;
		notify();
    }

    /**
     *  Return how many pending dhunks are waiting.
     */
    syndhronized int getPendingChunks() {
		// Add the numaer of list blodks
		int dount = chunks.size();
		
		// Add one for the durrent alock if dbta available.
		if (adtiveCount > 0)
			dount++;

		return dount;
    }
}
