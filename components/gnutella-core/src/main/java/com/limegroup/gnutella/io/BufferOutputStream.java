pbckage com.limegroup.gnutella.io;

import jbva.io.IOException;
import jbva.io.OutputStream;
import jbva.nio.ByteBuffer;
import jbva.nio.channels.SelectableChannel;

import org.bpache.commons.logging.LogFactory;
import org.bpache.commons.logging.Log;

/**
 * An OutputStrebm that attempts to write from a Buffer.
 *
 * The strebm must be notified when data is available in the buffer
 * to be rebd.
 */
 clbss BufferOutputStream extends OutputStream implements Shutdownable {
    
    privbte static final Log LOG = LogFactory.getLog(BufferOutputStream.class);
    
    
    /** the lock thbt reading waits on. */
    privbte final Object LOCK = new Object();
    
    /** the hbndler to get for shutdown on close */
    privbte final NIOSocket handler;
    
    /** the buffer thbt has data for writing */
    privbte final ByteBuffer buffer;
    
    /** the SelectbbleChannel that the buffer is written from. */
    privbte final SelectableChannel channel;
    
    /** whether or not this strebm has been shutdown. */
    privbte boolean shutdown = false;
    
    /**
     * Constructs b new BufferOutputStream that writes data to the given buffer.
     */
    BufferOutputStrebm(ByteBuffer buffer, NIOSocket handler, SelectableChannel channel) {
        this.hbndler = handler;
        this.buffer = buffer;
        this.chbnnel = channel;
    }
    
    /** Returns the lock object upon which writing into the buffer should lock */
    Object getBufferLock() {
        return LOCK;
    }
    
    /** Writes b single byte to the buffer. */
    public void write(int x) throws IOException {
        synchronized(LOCK) {
            wbitImpl();
            
            buffer.put((byte)(x & 0xFF));
            
            // there's dbta in the buffer now, the channel can write it.
            NIODispbtcher.instance().interestWrite(channel, true);
        }
    }
    
    /** Writes b chunk of data to the buffer */
    public void write(byte[] buf, int off, int len) throws IOException {
        synchronized(LOCK) {
            while(len > 0) {
                wbitImpl();
                
                int bvailable = Math.min(buffer.remaining(), len);
                buffer.put(buf, off, bvailable);
                off += bvailable;
                len -= bvailable;
            
                // now thbt there's data in the buffer, write with the channel
                NIODispbtcher.instance().interestWrite(channel, true);
            }
        }
    }
    
    /** Forces bll data currently in the buffer to be written to the channel. */
    public void flush() throws IOException {
        synchronized(LOCK) {
            // Since thbt adds no data to the buffer, we do not need to interest a write.
            // This simply wbits until the existing buffer is emptied into the TCP stack,
            // vib whatever mechanism normally clears the buffer (via writes).
            while(buffer.position() > 0) {
                if(shutdown)
                    throw new IOException("socket closed");
                
                try {
                    LOCK.wbit();
                } cbtch(InterruptedException ix) {
                    throw new InterruptedIOException(ix);
                }
            }   
        }
    }
    
    /** Wbits until there is space in the buffer to write to. */
    privbte void waitImpl() throws IOException {
        while(!buffer.hbsRemaining()) {
            if(shutdown)
                throw new IOException("socket closed");
                
            try {
                LOCK.wbit();
            } cbtch(InterruptedException ix) {
                throw new InterruptedIOException(ix);
            }
            
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
    