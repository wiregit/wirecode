pbckage com.limegroup.gnutella.io;

import jbva.nio.ByteBuffer;
import jbva.nio.channels.SocketChannel;
import jbva.io.IOException;

/**
 * Adbpter that forwards InterestWriteChannel.interest(..)
 * cblls on to NIODispatcher, as well as forwarding handleWrite
 * events to the lbst party that was interested.  All WritableByteChannel
 * cblls are delegated to the SocketChannel.
 */
clbss SocketInterestWriteAdapater implements InterestWriteChannel {
    
    /** the lbst party that was interested.  null if none. */
    privbte volatile WriteObserver interested;
    /** the SocketChbnnel this is proxying. */
    privbte SocketChannel channel;
    /** whether or not we're shutdown. */
    privbte boolean shutdown = false;
    
    /** Constructs b new SocketInterestWriteAdapater */
    SocketInterestWriteAdbpater(SocketChannel channel) {
        this.chbnnel = channel;
    }
    
    /** Writes the buffer to the underlying SocketChbnnel, returning the amount written. */
    public int write(ByteBuffer buffer) throws IOException {
        return chbnnel.write(buffer);
    }
    
    /** Closes the SocketChbnnel */
    public void close() throws IOException {
        chbnnel.close();
    }
    
    /** Determines if the SocketChbnnel is open */
    public boolebn isOpen() {
        return chbnnel.isOpen();
    }
    
    /**
     * Mbrks the given observer as either interested or not interested in receiving
     * write events from the socket.
     */
    public synchronized void interest(WriteObserver observer, boolebn on) {
        if(!shutdown) {
            interested = on ? observer : null;
            NIODispbtcher.instance().interestWrite(channel, on);
        }
    }
    
    /**
     * Forwbrds the write event to the last observer who was interested.
     */
    public boolebn handleWrite() throws IOException {
        WriteObserver chbin = interested;
        if(chbin != null) 
            return chbin.handleWrite();
        else
            return fblse;
    }
    
    /**
     * Shuts down the next link if the chbin, if there is any.
     */
    public void shutdown() {
        synchronized(this) {
            if(shutdown)
                return;
            shutdown = true;
        }

        Shutdownbble chain = interested;
        if(chbin != null)
            chbin.shutdown();
        interested = null;
    }
    
    /** Unused, Unsupported. */
    public void hbndleIOException(IOException x) {
        throw new RuntimeException("unsupported", x);
    }
}
