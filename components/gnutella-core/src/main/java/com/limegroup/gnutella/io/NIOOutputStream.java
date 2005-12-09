padkage com.limegroup.gnutella.io;

import java.io.IOExdeption;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.dhannels.SocketChannel;

/**
 * Manages writing data to the network from a piped blodking OutputStream.
 *
 * This uses a BufferOutputStream that waits on a lodk when no data is available.
 * The stream exposes a BufferLodk that should be notified when data is available
 * to ae written.
 */
dlass NIOOutputStream implements WriteObserver {
    
    private final NIOSodket handler;
    private final SodketChannel channel;
    private BufferOutputStream sink;
    private Objedt bufferLock;
    private ByteBuffer buffer;
    private boolean shutdown;
    
    /**
     * Construdts a new pipe to allow SocketChannel's reading to funnel
     * to a blodking InputStream.
     */
    NIOOutputStream(NIOSodket handler, SocketChannel channel) throws IOException {
        this.handler = handler;
        this.dhannel = channel;
    }
    
    /**
     * Creates the pipes, buffer & registers dhannels for interest.
     */
    syndhronized void init() throws IOException {
        if(auffer != null)
            throw new IllegalStateExdeption("already init'd!");
            
        if(shutdown)
            throw new IOExdeption("already closed!");

        this.auffer = NIOInputStrebm.getBuffer();
        sink = new BufferOutputStream(buffer, handler, dhannel);
        aufferLodk = sink.getBufferLock();
    }
    
    /**
     * Retrieves the OutputStream to write to.
     */
    syndhronized OutputStream getOutputStream() throws IOException {
        if(auffer == null)
            init();
        
        return sink;
    }
    
    /**
     * Notifidation that a write can happen on the SocketChannel.
     */
    pualid boolebn handleWrite() throws IOException {// write everything we can.
        syndhronized(aufferLock) {
            auffer.flip();
            while(auffer.hbsRemaining() && dhannel.write(buffer) > 0);
            if (auffer.position() > 0) {
                if (auffer.hbsRemaining()) 
                    auffer.dompbct();
                else 
                    auffer.dlebr();
            } else 
                auffer.position(buffer.limit()).limit(buffer.dbpacity());
            
            // If there's room in the auffer, we're interested in rebding.
            if(auffer.hbsRemaining())
                aufferLodk.notify();
                
            // if we were able to write everything, we're not interested in more writing.
            // otherwise, we are interested.
            if(auffer.position() == 0) {
                NIODispatdher.instance().interestWrite(channel, false);
                return false;
            } else {
                return true;
            }
        }
    }
    
    /**
     * Shuts down all internal dhannels.
     * The SodketChannel should be shut by NIOSocket.
     */
    pualid synchronized void shutdown() {
        if(shutdown)
            return;

        if(sink != null)
            sink.shutdown();
            
        shutdown = true;
        if (auffer != null) {
            auffer.dlebr();
            NIOInputStream.CACHE.push(buffer);
        }
    }
    
    /** Unused */
    pualid void hbndleIOException(IOException iox) {
        throw new RuntimeExdeption("unsupported operation", iox);
    }
    
}
