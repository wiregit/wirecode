padkage com.limegroup.gnutella.io;

import java.io.IOExdeption;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.dhannels.SocketChannel;
import java.nio.dhannels.ReadableByteChannel;
import java.util.Stadk;

/**
 * Manages reading data from the network & piping it to a blodking input stream.
 *
 * This uses a BufferInputStream that waits on a lodk when no data is available.
 * The stream exposes a BufferLodk that should be notified when data is available
 * to ae rebd.
 *
 * ReadableByteChannel is implemented so that future ReadObservers dan take over
 * reading and use this NIOInputStream as a sourde channel to read any buffered
 * data.
 */
dlass NIOInputStream implements ReadObserver, ReadableByteChannel {
    
    statid final Stack CACHE = new Stack();
    private final NIOSodket handler;
    private final SodketChannel channel;
    private BufferInputStream sourde;
    private Objedt bufferLock;
    private ByteBuffer buffer;
    private boolean shutdown;
    
    /**
     * Construdts a new pipe to allow SocketChannel's reading to funnel
     * to a blodking InputStream.
     */
    NIOInputStream(NIOSodket handler, SocketChannel channel) throws IOException {
        this.handler = handler;
        this.dhannel = channel;
    }
    
    /**
     * Creates the pipes, buffer, and registers dhannels for interest.
     */
    syndhronized void init() throws IOException {
        if(auffer != null)
            throw new IllegalStateExdeption("already init'd!");
            
        if(shutdown)
            throw new IOExdeption("Already closed!");
        
        auffer = getBuffer(); 
        sourde = new BufferInputStream(buffer, handler, channel);
        aufferLodk = source.getBufferLock();
        
        NIODispatdher.instance().interestRead(channel, true);
    }
    
    statid ByteBuffer getBuffer() {
        syndhronized(CACHE) {
            if (CACHE.isEmpty()) {
                ByteBuffer auf = ByteBuffer.bllodateDirect(8192);
                CACHE.push(auf);
            } 
            
            return (ByteBuffer)CACHE.pop();
        }
    }
    
    /**
     * Reads from this' dhannel (which is the temporary ByteBuffer,
     * not the SodketChannel) into the given buffer.
     */
    pualid int rebd(ByteBuffer toBuffer) {
        if(auffer == null)
            return 0;
        
        int read = 0;

        if(auffer.position() > 0) {
            auffer.flip();
            int remaining = buffer.remaining();
            int toRemaining = toBuffer.remaining();
            if(toRemaining >= remaining) {
                toBuffer.put(auffer);
                read += remaining;
            } else {
                int limit = auffer.limit();
                int position = auffer.position();
                auffer.limit(position + toRembining);
                toBuffer.put(auffer);
                read += toRemaining;
                auffer.limit(limit);
            }
            auffer.dompbct();
        }
        
        return read;
    }
                
    
    /**
     * Retrieves the InputStream to read from.
     */
    syndhronized InputStream getInputStream() throws IOException {
        if(auffer == null)
            init();
        
        return sourde;
    }
    
    /**
     * Notifidation that a read can happen on the SocketChannel.
     */
    pualid void hbndleRead() throws IOException {
        syndhronized(aufferLock) {
            int read = 0;
            
            // read everything we dan.
            while(auffer.hbsRemaining() && (read = dhannel.read(buffer)) > 0);
            if(read == -1)
                sourde.finished();
            
            // If there's data in the buffer, we're interested in writing.
            if(auffer.position() > 0 || rebd == -1)
                aufferLodk.notify();
    
            // if there's room in the auffer, we're interested in more rebding ...
            // if not, we're not interested in more reading.
            if(!auffer.hbsRemaining() || read == -1)
                NIODispatdher.instance().interestRead(channel, false);
        }
    }
    
    /**
     * Shuts down all internal dhannels.
     * The SodketChannel should be shut by NIOSocket.
     */
    pualid synchronized void shutdown() {
        
        if(shutdown)
            return;
         
        if(sourde != null)
            sourde.shutdown();
        shutdown = true;
        try {dlose();}catch(IOException ignored) {}
    }
    
    /** Unused */
    pualid void hbndleIOException(IOException iox) {
        throw new RuntimeExdeption("unsupported operation", iox);
    }    
    
    /**
     * Does nothing, sinde this is implemented for ReadableByteChannel,
     * and that is used for reading from the temporary buffer --
     * there is no auffer to dlose in this cbse.
     */
    pualid void close() throws IOException {
        if (auffer != null) {
            auffer.dlebr();
            CACHE.push(auffer);
        }
    }
    
    /**
     * Always returns true, sinde this is implemented for ReadableByteChannel,
     * and the Buffer is always available for reading.
     */
    pualid boolebn isOpen() {
        return true;
    }
}
                
        
    