package com.limegroup.gnutella.http;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.WritableByteChannel;

import com.limegroup.gnutella.io.IOState;
import com.limegroup.gnutella.statistics.Statistic;

public abstract class WriteHeadersIOState implements IOState {
    /** The outgoing buffer, if we've made it.  (Null if we haven't.) */
    private ByteBuffer outgoing;
    /** The stat to add data to. */
    private final Statistic stat; 

    /** Creates a new WriteHandshakeState using the given stat. */
    public WriteHeadersIOState(Statistic stat) {
        this.stat = stat;
    }

    /** Returns true. */
    public boolean isWriting() {
        return true;
    }

    /** Returns false. */
    public boolean isReading() {
        return false;
    }

    /**
     * Writes output to the channel.  This farms out the creation of the output
     * to the abstract method createOutgoingData().  That method will only be called once
     * to get the initial outgoing data.  Once all data has been written, the abstract
     * processWrittenHeaders() method will be called, so that subclasses can act upon
     * what they've just written.
     * 
     * This will return true if it needs to be called again to continue writing.
     * If it returns false, all data has been written and you can proceed to the next state.
     */
    public boolean process(Channel channel, ByteBuffer buffer) throws IOException {
        if(outgoing == null) {
            outgoing = createOutgoingData();
        }
        
        int written = ((WritableByteChannel)channel).write(outgoing);
        stat.addData(written);
        System.out.println("wrote: " + written + "(" + this + ")");
        
        if(!outgoing.hasRemaining()) {
            processWrittenHeaders();
            return false;
        } else {
            return true;
        }
    }
    
    /** Returns a ByteBuffer of data to write. */
    protected abstract ByteBuffer createOutgoingData() throws IOException;
    
    /** Processes the headers we wrote, after writing them.  May throw IOException if we need to disco. */
    protected abstract void processWrittenHeaders() throws IOException;
}
