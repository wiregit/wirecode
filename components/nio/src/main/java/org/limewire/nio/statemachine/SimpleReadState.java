package org.limewire.nio.statemachine;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;

/**
 * A reading state that reads all its data first, then validates it at the end.
 * It will also clear all data it read from the buffer.
 */
public abstract class SimpleReadState extends ReadState {
    /** The amount of data we expected to read. */
    private final int expect;
    /** The total amount of data we've read. */
    private int amountRead = 0;
    /** The ByteBuffer we'll be using. */
    private ByteBuffer buffer;
    
    public SimpleReadState(int expect) {
        this.expect = expect;
    }

    @Override
    protected boolean processRead(ReadableByteChannel channel, ByteBuffer scratchBuffer) throws IOException {
        if(buffer == null) {
            buffer = scratchBuffer.slice();
            buffer.limit(expect);
        }
        
        int read = 0;
        while(buffer.hasRemaining() && (read = channel.read(buffer)) > 0)
            amountRead += read;
        
        if(buffer.hasRemaining() && read == -1)
            throw new IOException("EOF");
        
        // if finished filling the buffer...
        if(!buffer.hasRemaining()) {
            validateBuffer(buffer);
            buffer.clear();
            return false;
        }
        
        // still need to read more
        return true;
    }

    public long getAmountProcessed() {
        return amountRead;
    }
    
    /** Validates the buffer, after it is filled.  Throw an IOException if there are errors. */
    public abstract void validateBuffer(ByteBuffer buffer) throws IOException;

}
