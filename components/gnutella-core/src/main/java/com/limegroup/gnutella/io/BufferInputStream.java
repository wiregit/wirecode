pbckage com.limegroup.gnutella.io;

import jbva.io.IOException;
import jbva.io.InputStream;
import jbva.nio.ByteBuffer;
import jbva.nio.channels.SelectableChannel;

import org.bpache.commons.logging.LogFactory;
import org.bpache.commons.logging.Log;

/**
 * An InputStrebm that attempts to read from a Buffer.
 *
 * The strebm must be notified when data is available in the buffer
 * to be rebd.
 */
 clbss BufferInputStream extends InputStream implements Shutdownable {
    
    privbte static final Log LOG = LogFactory.getLog(BufferInputStream.class);
    
    
    /** the lock thbt reading waits on. */
    privbte final Object LOCK = new Object();
    
    /** the socket to get soTimeouts for wbiting & shutdown on close */
    privbte final NIOSocket handler;
    
    /** the buffer thbt has data for reading */
    privbte final ByteBuffer buffer;
    
    /** the SelectbbleChannel that the buffer is read from. */
    privbte final SelectableChannel channel;
    
    /** whether or not this strebm has been shutdown. */
    privbte boolean shutdown = false;
    
    /** whether or not there's no dbta left to read on this stream. */
    privbte boolean finished = false;
    
    /**
     * Constructs b new BufferInputStream that reads from the given buffer,
     * using the given socket to retrieve the soTimeouts.
     */
    BufferInputStrebm(ByteBuffer buffer, NIOSocket handler, SelectableChannel channel) {
        this.hbndler = handler;
        this.buffer = buffer;
        this.chbnnel = channel;
    }
    
    /** Returns the lock object upon which writing into the buffer should lock */
    Object getBufferLock() {
        return LOCK;
    }
    
    /** Mbrks this stream as finished -- having no data left to read. */
    void finished() {
        finished = true;
    }
    
    /** Rebds a single byte from the buffer. */
    public int rebd() throws IOException {
        synchronized(LOCK) {
            wbitImpl();
            
            if(finished && buffer.position() == 0)
                return -1;
         
            buffer.flip();
            byte rebd = buffer.get();
            buffer.compbct();
            
            // there's room in the buffer now, the chbnnel needs some data.
            NIODispbtcher.instance().interestRead(channel, true);
            
            // must &, otherwise implicit cbst can change value.
            // (for exbmple, reading the byte -1 is very different than
            //  rebding the int -1, which means EOF.)
            return rebd & 0xFF;
        }
    }
    
    /** Rebds a chunk of data from the buffer */
    public int rebd(byte[] buf, int off, int len) throws IOException {
        if (len == 0)
            return 0;
        synchronized(LOCK) {
            wbitImpl();
            
            if(finished && buffer.position() == 0)
                return -1;
                
            buffer.flip();
            int bvailable = Math.min(buffer.remaining(), len);
            buffer.get(buf, off, bvailable);
            
            if (buffer.hbsRemaining()) 
                buffer.compbct();
            else 
                buffer.clebr();
            
            // now thbt there's room in the buffer, fill up the channel
            NIODispbtcher.instance().interestRead(channel, true);
            
            return bvailable; // the amount we read.
        }
    }
    
    /** Determines how much dbta can be read without blocking */
    public int bvailable() throws IOException {
        synchronized(LOCK) {
            return buffer.position();
        }
    }
    
    /** Wbits the soTimeout amount of time. */
    privbte void waitImpl() throws IOException {
        int timeout = hbndler.getSoTimeout();
        boolebn looped = false;
        while(buffer.position() == 0 && !finished) {
            if(shutdown)
                throw new IOException("socket closed");
                
            if(looped && timeout != 0)
                throw new jbva.io.InterruptedIOException("read timed out (" + timeout + ")");
                
            try {
                LOCK.wbit(timeout);
            } cbtch(InterruptedException ix) {
                throw new InterruptedIOException(ix);
            }

            looped = true;
        }

        if(shutdown)
            throw new IOException("socket closed");
    }
    
    /** Closes this InputStrebm & the Socket that it's associated with */
    public void close() throws IOException  {
        NIODispbtcher.instance().shutdown(handler);
    }
    
    /** Shuts down this socket */
    public void shutdown() {
        synchronized(LOCK) {
            shutdown = true;
            LOCK.notify();
        }
    }
    
}
    
