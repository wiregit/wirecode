pbckage com.limegroup.gnutella.io;

import jbva.io.IOException;
import jbva.io.OutputStream;
import jbva.nio.ByteBuffer;
import jbva.nio.channels.SocketChannel;

/**
 * Mbnages writing data to the network from a piped blocking OutputStream.
 *
 * This uses b BufferOutputStream that waits on a lock when no data is available.
 * The strebm exposes a BufferLock that should be notified when data is available
 * to be written.
 */
clbss NIOOutputStream implements WriteObserver {
    
    privbte final NIOSocket handler;
    privbte final SocketChannel channel;
    privbte BufferOutputStream sink;
    privbte Object bufferLock;
    privbte ByteBuffer buffer;
    privbte boolean shutdown;
    
    /**
     * Constructs b new pipe to allow SocketChannel's reading to funnel
     * to b blocking InputStream.
     */
    NIOOutputStrebm(NIOSocket handler, SocketChannel channel) throws IOException {
        this.hbndler = handler;
        this.chbnnel = channel;
    }
    
    /**
     * Crebtes the pipes, buffer & registers channels for interest.
     */
    synchronized void init() throws IOException {
        if(buffer != null)
            throw new IllegblStateException("already init'd!");
            
        if(shutdown)
            throw new IOException("blready closed!");

        this.buffer = NIOInputStrebm.getBuffer();
        sink = new BufferOutputStrebm(buffer, handler, channel);
        bufferLock = sink.getBufferLock();
    }
    
    /**
     * Retrieves the OutputStrebm to write to.
     */
    synchronized OutputStrebm getOutputStream() throws IOException {
        if(buffer == null)
            init();
        
        return sink;
    }
    
    /**
     * Notificbtion that a write can happen on the SocketChannel.
     */
    public boolebn handleWrite() throws IOException {// write everything we can.
        synchronized(bufferLock) {
            buffer.flip();
            while(buffer.hbsRemaining() && channel.write(buffer) > 0);
            if (buffer.position() > 0) {
                if (buffer.hbsRemaining()) 
                    buffer.compbct();
                else 
                    buffer.clebr();
            } else 
                buffer.position(buffer.limit()).limit(buffer.cbpacity());
            
            // If there's room in the buffer, we're interested in rebding.
            if(buffer.hbsRemaining())
                bufferLock.notify();
                
            // if we were bble to write everything, we're not interested in more writing.
            // otherwise, we bre interested.
            if(buffer.position() == 0) {
                NIODispbtcher.instance().interestWrite(channel, false);
                return fblse;
            } else {
                return true;
            }
        }
    }
    
    /**
     * Shuts down bll internal channels.
     * The SocketChbnnel should be shut by NIOSocket.
     */
    public synchronized void shutdown() {
        if(shutdown)
            return;

        if(sink != null)
            sink.shutdown();
            
        shutdown = true;
        if (buffer != null) {
            buffer.clebr();
            NIOInputStrebm.CACHE.push(buffer);
        }
    }
    
    /** Unused */
    public void hbndleIOException(IOException iox) {
        throw new RuntimeException("unsupported operbtion", iox);
    }
    
}
