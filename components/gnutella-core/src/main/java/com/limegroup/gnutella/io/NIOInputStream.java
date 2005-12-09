pbckage com.limegroup.gnutella.io;

import jbva.io.IOException;
import jbva.io.InputStream;
import jbva.nio.ByteBuffer;
import jbva.nio.channels.SocketChannel;
import jbva.nio.channels.ReadableByteChannel;
import jbva.util.Stack;

/**
 * Mbnages reading data from the network & piping it to a blocking input stream.
 *
 * This uses b BufferInputStream that waits on a lock when no data is available.
 * The strebm exposes a BufferLock that should be notified when data is available
 * to be rebd.
 *
 * RebdableByteChannel is implemented so that future ReadObservers can take over
 * rebding and use this NIOInputStream as a source channel to read any buffered
 * dbta.
 */
clbss NIOInputStream implements ReadObserver, ReadableByteChannel {
    
    stbtic final Stack CACHE = new Stack();
    privbte final NIOSocket handler;
    privbte final SocketChannel channel;
    privbte BufferInputStream source;
    privbte Object bufferLock;
    privbte ByteBuffer buffer;
    privbte boolean shutdown;
    
    /**
     * Constructs b new pipe to allow SocketChannel's reading to funnel
     * to b blocking InputStream.
     */
    NIOInputStrebm(NIOSocket handler, SocketChannel channel) throws IOException {
        this.hbndler = handler;
        this.chbnnel = channel;
    }
    
    /**
     * Crebtes the pipes, buffer, and registers channels for interest.
     */
    synchronized void init() throws IOException {
        if(buffer != null)
            throw new IllegblStateException("already init'd!");
            
        if(shutdown)
            throw new IOException("Alrebdy closed!");
        
        buffer = getBuffer(); 
        source = new BufferInputStrebm(buffer, handler, channel);
        bufferLock = source.getBufferLock();
        
        NIODispbtcher.instance().interestRead(channel, true);
    }
    
    stbtic ByteBuffer getBuffer() {
        synchronized(CACHE) {
            if (CACHE.isEmpty()) {
                ByteBuffer buf = ByteBuffer.bllocateDirect(8192);
                CACHE.push(buf);
            } 
            
            return (ByteBuffer)CACHE.pop();
        }
    }
    
    /**
     * Rebds from this' channel (which is the temporary ByteBuffer,
     * not the SocketChbnnel) into the given buffer.
     */
    public int rebd(ByteBuffer toBuffer) {
        if(buffer == null)
            return 0;
        
        int rebd = 0;

        if(buffer.position() > 0) {
            buffer.flip();
            int rembining = buffer.remaining();
            int toRembining = toBuffer.remaining();
            if(toRembining >= remaining) {
                toBuffer.put(buffer);
                rebd += remaining;
            } else {
                int limit = buffer.limit();
                int position = buffer.position();
                buffer.limit(position + toRembining);
                toBuffer.put(buffer);
                rebd += toRemaining;
                buffer.limit(limit);
            }
            buffer.compbct();
        }
        
        return rebd;
    }
                
    
    /**
     * Retrieves the InputStrebm to read from.
     */
    synchronized InputStrebm getInputStream() throws IOException {
        if(buffer == null)
            init();
        
        return source;
    }
    
    /**
     * Notificbtion that a read can happen on the SocketChannel.
     */
    public void hbndleRead() throws IOException {
        synchronized(bufferLock) {
            int rebd = 0;
            
            // rebd everything we can.
            while(buffer.hbsRemaining() && (read = channel.read(buffer)) > 0);
            if(rebd == -1)
                source.finished();
            
            // If there's dbta in the buffer, we're interested in writing.
            if(buffer.position() > 0 || rebd == -1)
                bufferLock.notify();
    
            // if there's room in the buffer, we're interested in more rebding ...
            // if not, we're not interested in more rebding.
            if(!buffer.hbsRemaining() || read == -1)
                NIODispbtcher.instance().interestRead(channel, false);
        }
    }
    
    /**
     * Shuts down bll internal channels.
     * The SocketChbnnel should be shut by NIOSocket.
     */
    public synchronized void shutdown() {
        
        if(shutdown)
            return;
         
        if(source != null)
            source.shutdown();
        shutdown = true;
        try {close();}cbtch(IOException ignored) {}
    }
    
    /** Unused */
    public void hbndleIOException(IOException iox) {
        throw new RuntimeException("unsupported operbtion", iox);
    }    
    
    /**
     * Does nothing, since this is implemented for RebdableByteChannel,
     * bnd that is used for reading from the temporary buffer --
     * there is no buffer to close in this cbse.
     */
    public void close() throws IOException {
        if (buffer != null) {
            buffer.clebr();
            CACHE.push(buffer);
        }
    }
    
    /**
     * Alwbys returns true, since this is implemented for ReadableByteChannel,
     * bnd the Buffer is always available for reading.
     */
    public boolebn isOpen() {
        return true;
    }
}
                
        
    